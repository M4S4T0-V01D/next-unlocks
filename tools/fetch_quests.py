"""Builds src/main/resources/com/nextunlocks/quests.json from the OSRS Wiki: every quest and miniquest with its
members flag, skill/quest point/combat requirements and the quests it needs first.

    python3 -I tools/fetch_quests.py [--refresh]

Wiki content is CC BY-NC-SA 3.0 (https://oldschool.runescape.wiki).
"""
import importlib.util
import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("kitwiki", HERE / "kitwiki.py")
wiki = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wiki)
wiki.setup("next-unlocks data builder (github.com/M4S4T0-V01D/next-unlocks)", HERE / ".cache" / "quests")

OUT = HERE.parent / "src/main/resources/com/nextunlocks/quests.json"
SKILLS = {"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Construction", "Hitpoints",
          "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Hunter", "Mining", "Smithing",
          "Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Sailing"}


def template(text, name):
    """{param: value} of the first {{name ...}} in text, honouring nested braces and links."""
    m = re.search(r"\{\{\s*" + re.escape(name) + r"\s*\|", text, re.I)
    if not m:
        return {}
    i, depth, start, parts = m.end(), 2, m.end(), []
    while i < len(text) and depth > 0:
        two = text[i:i + 2]
        if two in ("{{", "[["):
            depth += 2
            i += 2
            continue
        if two in ("}}", "]]"):
            depth -= 2
            i += 2
            continue
        if text[i] == "|" and depth == 2:
            parts.append(text[start:i])
            start = i + 1
        i += 1
    parts.append(text[start:i - 2])
    params = {}
    for p in parts:
        if "=" in p:
            k, v = p.split("=", 1)
            params[k.strip().lower()] = v.strip()
    return params


def plain(text):
    text = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", text)
    text = re.sub(r"\{\{[^{}]*\}\}", "", text)
    text = re.sub(r"'''?|<[^>]+>", "", text)
    return re.sub(r"\s+", " ", text).strip()


def main(refresh):
    quests = [t for t in wiki.category("Category:Quests", refresh) if "/" not in t]
    minis = [t for t in wiki.category("Category:Miniquests", refresh) if "/" not in t]
    names = set(quests) | set(minis)
    pages = wiki.wikitext(sorted(names), refresh)
    out = []
    for title in sorted(names):
        text = pages.get(title) or ""
        info = template(text, "Infobox Quest") or template(text, "Infobox Miniquest")
        details = template(text, "Quest details")
        if not info or not details or re.search(r"\{\{\s*Gone\b", text, re.I):
            continue
        req = details.get("requirements", "")
        skills, qp, combat = {}, 0, 0
        for skill, level in re.findall(r"\{\{\s*SCP\s*\|\s*([A-Za-z ]+?)\s*\|\s*(\d+)", req):
            skill, level = skill.strip().capitalize(), int(level)
            if skill in SKILLS:
                skills[skill.upper()] = max(level, skills.get(skill.upper(), 0))
            elif skill == "Quest":
                qp = max(qp, level)
            elif skill == "Combat":
                combat = max(combat, level)
        needs = []
        for link in re.findall(r"\[\[([^|\]#]+)", req):
            link = link.strip()
            if link in names and link != title and link not in needs:
                needs.append(link)
        entry = {
            "name": plain(info.get("name", title)) or title,
            "wiki": title,
            "members": info.get("members", "").lower().startswith("yes"),
        }
        if skills:
            entry["skills"] = skills
        if qp:
            entry["questPoints"] = qp
        if combat:
            entry["combat"] = combat
        if needs:
            entry["quests"] = needs
        out.append(entry)
    # Category:Quests also holds pages like "Quest points" that have no quest infobox
    kept = {e["wiki"] for e in out}
    for e in out:
        if "quests" in e:
            e["quests"] = [t for t in e["quests"] if t in kept]
            if not e["quests"]:
                del e["quests"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"source": "https://oldschool.runescape.wiki (CC BY-NC-SA 3.0)", "quests": out},
                              indent=0, ensure_ascii=False) + "\n")
    print(f"{len(out)} quests -> {OUT.relative_to(HERE.parent)}")


if __name__ == "__main__":
    main("--refresh" in sys.argv)
