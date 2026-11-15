"""Builds src/main/resources/com/nextunlocks/items.json from the OSRS Wiki: items with the levels and quests needed to
wield or wear them, and the levels needed to make them from scratch (the recipe, then each material's recipe or the
level that gathers it), for item goals.

    python3 -I tools/fetch_items.py [--refresh]

Wield levels come from the sentences in each equipment page's intro ("requires 55 Ranged to wield"), since the wiki
keeps them in no structured field; making levels come from the recipe bucket and unlocks.json. Run fetch_unlocks.py
and fetch_quests.py first.

Wiki content is CC BY-NC-SA 3.0 (https://oldschool.runescape.wiki).
"""
import datetime
import importlib.util
import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("kitwiki", HERE / "kitwiki.py")
wiki = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wiki)
wiki.setup("next-unlocks data builder (github.com/M4S4T0-V01D/next-unlocks)", HERE / ".cache" / "items")

RESOURCES = HERE.parent / "src/main/resources/com/nextunlocks"
OUT = RESOURCES / "items.json"
SKILLS = ["Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Construction", "Hitpoints",
          "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Hunter", "Mining", "Smithing",
          "Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Sailing"]
ALIASES = {"Runecrafting": "Runecraft", "Range": "Ranged", "Defense": "Defence", "HP": "Hitpoints"}
SKILL = r"(?:" + "|".join(SKILLS + list(ALIASES)) + r")\b"
SKILL_LIST = SKILL + r"(?:\s*(?:,\s*(?:and\s+)?|\s+and\s+|\s+or\s+)" + SKILL + r")*"
# "42 Attack, Strength and Defence", "level 75 Ranged and Defence", "60 in Runecraft"
LEVEL_FIRST = re.compile(r"\b(\d{1,2})\s+(?:in\s+)?(?:both\s+)?(" + SKILL_LIST + r")")
# "an Attack level of 70", "Ranged level of at least 70"
SKILL_FIRST = re.compile(r"\b(" + SKILL + r")\s+levels?\s+of\s+(?:at\s+least\s+)?(\d{1,2})\b")
# sentences about wielding, wearing or getting the item; the levels and quests in them are what the goal needs
WIELD = re.compile(r"\b(?:wield|wear|equip|worn|purchas|bought|buy)", re.I)
# what a level is for, from the words after it: wielding, or something else such as making or gathering
PURPOSE = re.compile(r"\bto\s+(?:be\s+able\s+to\s+)?(?:buy\s+and\s+)?(\w+)", re.I)
NOT_WIELD = {"make", "create", "craft", "smith", "fletch", "mine", "chop", "fish", "cut", "cook", "brew", "mix",
             "use", "build", "repair", "charge", "imbue", "assemble", "attach", "enchant", "string", "upgrade",
             "cast", "kill", "fight", "enter", "access", "catch", "pick", "plant", "harvest", "burn", "light",
             "smelt", "combine", "recharge", "restore", "boost", "train", "fill"}
GATHER = re.compile(r"^(?:Chop|Mine|Fish|Catch|Harvest|Pick|Gather|Collect|Cut|Grow)\b")
MAX_DEPTH = 6


def norm(title):
    title = title.replace("_", " ").strip()
    return (title[0].upper() + title[1:]) if title else title


def skill_names(text):
    return [ALIASES.get(s, s) for s in re.findall(SKILL, text)]


def bucket(name, fields):
    """Every row of a bucket, 5000 at a time."""
    def build():
        rows, offset = [], 0
        while True:
            select = ",".join(f"'{f}'" for f in fields)
            query = f"bucket('{name}').select({select}).limit(5000).offset({offset}).run()"
            batch = wiki.api({"action": "bucket", "query": query})["bucket"]
            rows += batch
            if len(batch) < 5000:
                return rows
            offset += 5000
    return wiki.cached(f"bucket-{name}.json", "--refresh" in sys.argv, build)


def sentences(text):
    return [s for s in re.split(r"(?<=[.!?])\s+(?=[A-Z])", text.replace("\n", " ")) if s]


def wield_levels(intro):
    """{skill: level} the intro says the item needs to wield, wear or buy it, and the sentences about that."""
    levels, said = {}, []
    for sentence in sentences(intro):
        if not WIELD.search(sentence):
            continue
        found = []  # (start, end, [skills], level)
        for m in LEVEL_FIRST.finditer(sentence):
            found.append((m.start(), m.end(), skill_names(m.group(2)), int(m.group(1))))
        for m in SKILL_FIRST.finditer(sentence):
            if not any(s <= m.start() < e for s, e, _, _ in found):
                found.append((m.start(), m.end(), skill_names(m.group(1)), int(m.group(2))))
        found.sort()
        # each level is for whatever the next "to <verb>" says; one with none inherits from the levels after it
        purposes = []
        for i, (start, end, _, _) in enumerate(found):
            stop = found[i + 1][0] if i + 1 < len(found) else len(sentence)
            m = PURPOSE.search(sentence, end, stop)
            verb = m.group(1).lower() if m else None
            purposes.append(None if verb is None else verb not in NOT_WIELD)
        wield = True
        for i in range(len(found) - 1, -1, -1):
            wield = purposes[i] if purposes[i] is not None else wield
            purposes[i] = wield
        for (_, _, skills, level), keep in zip(found, purposes):
            if not keep or level < 2:
                continue
            for skill in skills:
                levels[skill] = max(levels.get(skill, 0), level)
        said.append(sentence)
    return levels, said


def quests_in(said, quest_names):
    """Quests the wield sentences say must be completed ("...and have completed the Monkey Madness I quest"), longest
    names first so "Dragon Slayer II" isn't read as "...I"."""
    found = []
    for sentence in said:
        for name in quest_names:
            m = re.search(r"\b" + re.escape(name) + r"(?![\w'])", sentence)
            if not m:
                continue
            sentence = sentence[:m.start()] + " " * len(name) + sentence[m.end():]
            # only a quest close after "completed"/"completion of", and not "unless you have completed"
            before = sentence[max(0, m.start() - 60):m.start()]
            done = [d for d in re.finditer(r"\bcomplet\w*", before, re.I)]
            if done and not re.search(r"\b(?:unless|not|without|optional)\b", before[max(0, done[-1].start() - 25):], re.I):
                found.append(quest_names[name])
    return sorted(set(found))


def item_ids():
    """Page -> every item ID on it (all versions), from the infobox_item bucket."""
    ids = {}
    for row in bucket("infobox_item", ["page_name", "item_id"]):
        page = norm(row["page_name"])
        for i in row.get("item_id") or []:
            if str(i).isdigit():
                ids.setdefault(page, set()).add(int(i))
    return ids


def recipes():
    """Item name -> the recipe that needs the lowest top level: ({skill: level}, [material names])."""
    best = {}
    for row in bucket("recipe", ["page_name", "page_name_sub", "production_json"]):
        try:
            data = json.loads(row["production_json"])
        except (TypeError, ValueError):
            continue
        skills = {}
        for s in data.get("skills") or []:
            name = ALIASES.get(s.get("name"), s.get("name"))
            level = str(s.get("level", "")).strip()
            if name in SKILLS and level.isdigit():
                skills[name] = max(skills.get(name, 0), int(level))
        if not skills:
            continue
        materials = [norm(m["name"]) for m in data.get("materials") or [] if m.get("name")]
        output = data.get("output")
        names = {norm(row["page_name"])}
        if isinstance(output, dict) and output.get("name"):
            names.add(norm(output["name"]))
        rank = (max(skills.values()), len(skills))
        for name in names:
            if name not in best or rank < best[name][0]:
                best[name] = (rank, skills, materials)
    return {name: (skills, materials) for name, (_, skills, materials) in best.items()}


def gathering(unlocks):
    """Item page -> (skill, level) of the easiest unlock that gathers it, e.g. Ironwood logs -> Woodcutting 80."""
    by_id, by_page = {}, {}
    for skill, entries in unlocks.items():
        for e in entries:
            if e["type"] != "UNLOCK" or not GATHER.match(e["text"]):
                continue
            pick = (skill.title(), e["level"])
            for table, key in ((by_page, norm(e["wiki"].split("#")[0])), (by_id, e.get("item"))):
                if key and (key not in table or pick[1] < table[key][1]):
                    table[key] = pick
    return by_page, by_id


def wear_unlocks(unlocks):
    """Item page -> {skill: level} from unlocks.json's "Wield"/"Wear" lines that name a single item."""
    out = {}
    for skill, entries in unlocks.items():
        for e in entries:
            if e["type"] == "UNLOCK" and re.match(r"^(?:Wield|Wear|Equip)\b", e["text"]) and e.get("item"):
                page = norm(e["wiki"].split("#")[0])
                levels = out.setdefault(page, {})
                levels[skill.title()] = max(levels.get(skill.title(), 0), e["level"])
    return out


def make_levels(name, recipe, gather, ids, depth=0, seen=None):
    """{skill: (level, what it's for)} to make the item from scratch: its recipe, then its materials'."""
    seen = set() if seen is None else seen
    if name in seen or depth > MAX_DEPTH:
        return {}
    seen.add(name)
    out = {}

    def need(skill, level, what):
        if skill not in out or level > out[skill][0]:
            out[skill] = (level, what)

    by_page, by_id = gather
    pick = by_page.get(name) or next((by_id[i] for i in sorted(ids.get(name, ())) if i in by_id), None)
    if depth > 0 and pick:
        # a material that is gathered is taken as gathered, even when some recipe also makes it
        need(pick[0], pick[1], name)
    elif name in recipe:
        skills, materials = recipe[name]
        for skill, level in skills.items():
            need(skill, level, name)
        for material in materials:
            for skill, (level, what) in make_levels(material, recipe, gather, ids, depth + 1, seen).items():
                need(skill, level, what)
    return {skill: need for skill, need in out.items() if need[0] > 1}


def main():
    unlocks = json.loads((RESOURCES / "unlocks.json").read_text(encoding="utf-8"))["skills"]
    quests = json.loads((RESOURCES / "quests.json").read_text(encoding="utf-8"))["quests"]
    quest_names = {q["name"]: q["wiki"] for q in sorted(quests, key=lambda q: -len(q["name"]))}

    ids = item_ids()
    recipe = recipes()
    gather = gathering(unlocks)
    worn = wear_unlocks(unlocks)
    equipment = [norm(t) for t in wiki.embeddedin(("Template:Infobox Bonuses",), "--refresh" in sys.argv)]
    intro = wiki.intros(equipment, "--refresh" in sys.argv)

    items = []
    for page in sorted(set(equipment) | set(recipe)):
        if page not in ids:
            continue
        levels, said = wield_levels(intro.get(page, ""))
        for skill, level in worn.get(page, {}).items():
            levels[skill] = max(levels.get(skill, 0), level)
        make = make_levels(page, recipe, gather, ids)
        quest = quests_in(said, quest_names)
        if not levels and not make and not quest:
            continue
        entry = {"name": page, "ids": sorted(ids[page])}
        if levels:
            entry["wield"] = {s.upper(): l for s, l in sorted(levels.items(), key=lambda x: -x[1])}
        if quest:
            entry["quests"] = quest
        if make:
            # each with the material's item ID for its icon, or 0 when the wiki has no item page for it
            entry["make"] = [[s.upper(), l, what, min(ids.get(what, {0}))]
                             for s, (l, what) in sorted(make.items(), key=lambda x: -x[1][0])]
        items.append(entry)

    data = {
        "source": "https://oldschool.runescape.wiki (CC BY-NC-SA 3.0)",
        "fetched": datetime.date.today().isoformat(),
        "items": items,
    }
    OUT.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"wrote {OUT}: {len(items)} items, {sum('wield' in i for i in items)} with wield levels")


if __name__ == "__main__":
    main()
