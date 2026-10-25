#!/usr/bin/env python3
"""Builds src/main/resources/com/nextunlocks/unlocks.json from the OSRS Wiki
"<Skill>/Level up table" pages. Run with: python3 -I tools/fetch_unlocks.py

Entries that picture an item get its item ID ("item"), and spells and prayers get their
game sprite ID ("sprite"), so the panel can draw them with the game's own images. Sprite
IDs come from the named constants in the runelite-api jar in the Gradle cache, read with javap.

Wiki content is licensed CC BY-NC-SA 3.0 (https://oldschool.runescape.wiki).
"""
import datetime
import glob
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import time
import urllib.parse
import urllib.request

SKILLS = [
    "Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft",
    "Construction", "Hitpoints", "Agility", "Herblore", "Thieving", "Crafting",
    "Fletching", "Slayer", "Hunter", "Mining", "Smithing", "Fishing", "Cooking",
    "Firemaking", "Woodcutting", "Farming", "Sailing",
]
API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = "next-unlocks-runelite-plugin data builder"
OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/com/nextunlocks/unlocks.json"

PARAM_RE = re.compile(r"^\|(freeplay|members)(\d+)\s*=", re.M)
LINK_TEMPLATE_RE = re.compile(r"\{\{(?:plink|plinkp|ilink)\|([^}]*)\}\}", re.I)
SCP_RE = re.compile(r"\{\{SCP\|([^}|]+)(?:\|([^}|]*))?[^}]*\}\}", re.I)
WIKILINK_RE = re.compile(r"\[\[([^\]|]+)(?:\|([^\]]*))?\]\]")
FOOTNOTE_RE = re.compile(r"\{\{efn\|[^{}]*\}\}", re.I)
TEMPLATE_RE = re.compile(r"\{\{[^{}]*\}\}")
PLINK_RE = re.compile(r"\{\{(?:plink|plinkp|ilink)\|([^}]*)\}\}", re.I)
# sprite groups in net.runelite.api.gameval.SpriteID holding spell and prayer icons
SPRITE_GROUPS = ["Magicon", "Magicon2", "LunarMagicOn", "MagicNecroOn", "Prayeron"]


def fetch(skill):
    query = urllib.parse.urlencode({
        "action": "parse", "page": f"{skill}/Level up table", "prop": "wikitext",
        "format": "json", "formatversion": "2", "redirects": "1",
    })
    req = urllib.request.Request(f"{API}?{query}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.load(resp)["parse"]["wikitext"]


def first_target(raw):
    """Wiki page an entry points at: the first item/page link in it."""
    m = re.search(r"\{\{(?:plink|plinkp|ilink)\|([^}|]+)|\[\[([^\]|]+)", raw, re.I)
    if not m:
        return None
    target = (m.group(1) or m.group(2)).strip()
    return target[0].upper() + target[1:] if target else None


def picture(raw):
    """(page, picture name) of the first picture link in an entry, or None."""
    m = PLINK_RE.search(raw)
    if not m:
        return None
    parts = m.group(1).split("|")
    named = dict(p.split("=", 1) for p in parts[1:] if "=" in p)
    page = parts[0].strip()
    return page, named.get("pic", page).strip()


def link_template(m):
    parts = m.group(1).split("|")
    named = dict(p.split("=", 1) for p in parts[1:] if "=" in p)
    return named.get("txt", parts[0]).strip()


def scp(m):
    name, level = m.group(1).strip(), (m.group(2) or "").strip()
    if name.lower() in ("quest", "diary", "minigame", "combat"):
        return ""
    return f"{level} {name}" if level else ""


def to_plain(raw):
    text = FOOTNOTE_RE.sub("", raw)
    text = LINK_TEMPLATE_RE.sub(link_template, text)
    text = SCP_RE.sub(scp, text)
    text = WIKILINK_RE.sub(lambda m: (m.group(2) or m.group(1)).strip(), text)
    text = TEMPLATE_RE.sub("", text)
    text = re.sub(r"'{2,}", "", text)
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    # "Mine {{SCP|Mining|15}} [[Mining]]" leaves "15 Mining Mining"
    return re.sub(r"\b(\w+) \1\b", r"\1", text)


def classify(raw):
    low = raw.lower()
    if low.startswith("required for {{scp|quest}}"):
        return "QUEST"
    if low.startswith("required to complete {{scp|diary}}"):
        return "DIARY"
    return "UNLOCK"


def parse(wikitext):
    entries = []
    params = list(PARAM_RE.finditer(wikitext))
    for i, m in enumerate(params):
        end = params[i + 1].start() if i + 1 < len(params) else wikitext.find("\n}}", m.end())
        body = wikitext[m.end():end if end > 0 else None]
        members = m.group(1) == "members"
        level = int(m.group(2))
        lines = [l.rstrip() for l in body.splitlines() if l.startswith("*")]
        parent = None
        for line in lines:
            depth = len(line) - len(line.lstrip("*"))
            raw = line.lstrip("*").strip()
            if raw.count("}}") > raw.count("{{"):
                raw = raw[:raw.rfind("}}")].rstrip()  # last line runs into the table's closing braces
            if not raw:
                continue
            if depth > 1 and parent is not None:
                if parent["_raw"].endswith(":"):
                    # "Wield dragon weapons:" followed by one bullet per weapon
                    parent["_drop"] = True
                else:
                    parent["text"] += f" ({to_plain(raw)})"
                    continue
            entry = {
                "level": level,
                "members": members,
                "type": classify(raw),
                "text": to_plain(raw),
                "wiki": first_target(raw),
                "_raw": raw,
                "_pic": picture(raw),
            }
            entries.append(entry)
            if depth == 1:
                parent = entry
    return [e for e in entries if not e.get("_drop") and e["text"]]


def api(params):
    query = urllib.parse.urlencode({"format": "json", **params})
    req = urllib.request.Request(f"{API}?{query}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def norm(title):
    title = title.replace("_", " ").strip()
    return (title[0].upper() + title[1:]) if title else title


def item_ids():
    """Wiki page (and "Page#Version") -> item ID, from the infobox_item bucket."""
    ids, offset = {}, 0
    while True:
        query = (f"bucket('infobox_item').select('page_name','page_name_sub','item_id')"
                 f".limit(5000).offset({offset}).run()")
        rows = api({"action": "bucket", "query": query})["bucket"]
        for row in rows:
            item = next((int(i) for i in row.get("item_id") or [] if i.isdigit()), None)
            if item is None:
                continue
            page, sub = norm(row["page_name"]), norm(row.get("page_name_sub") or row["page_name"])
            ids.setdefault(sub, item)
            # a page with versions gets the first version listed, unless it has an unversioned row
            if sub == page:
                ids[page] = item
            else:
                ids.setdefault(page, item)
        if len(rows) < 5000:
            return ids
        offset += 5000
        time.sleep(0.5)


def redirects(titles):
    """Wiki title -> redirect target, for titles that are redirects."""
    out, titles = {}, sorted(titles)
    for i in range(0, len(titles), 50):
        data = api({"action": "query", "titles": "|".join(titles[i:i + 50]), "redirects": "1", "formatversion": "2"})
        query = data.get("query", {})
        steps = {norm(n["from"]): norm(n["to"]) for n in query.get("normalized", [])}
        for r in query.get("redirects", []):
            steps[norm(r["from"])] = norm(r["to"])
        for t in titles[i:i + 50]:
            target = t
            for _ in range(3):
                target = steps.get(target, target)
            if target != t:
                out[t] = target
        time.sleep(0.5)
    return out


def sprite_key(name):
    return re.sub(r"[^A-Z0-9]+", "_", name.upper()).strip("_")


def sprite_ids():
    """Spell/prayer name key -> sprite ID, or {} when the runelite-api jar or javap is missing."""
    jars = sorted(glob.glob(os.path.expanduser(
        "~/.gradle/caches/modules-2/files-2.1/net.runelite/runelite-api/*/*/runelite-api-*[0-9].jar")))
    java_home = os.environ.get("JAVA_HOME")
    javap = os.path.join(java_home, "bin", "javap") if java_home else shutil.which("javap")
    if not jars or not javap or not os.path.exists(javap):
        print("runelite-api jar or javap not found; spells and prayers get no icons", file=sys.stderr)
        return {}
    classes = [f"net.runelite.api.gameval.SpriteID${g}" for g in SPRITE_GROUPS]
    out = subprocess.run([javap, "-constants", "-cp", jars[-1], *classes],
                         capture_output=True, text=True, check=True).stdout
    ids = {}
    for name, value in re.findall(r"int ([A-Z][A-Z0-9_]*) = (\d+);", out):
        ids.setdefault(name, int(value))
    return ids


def add_icons(skills):
    entries = [e for es in skills.values() for e in es]
    items, sprites = item_ids(), sprite_ids()
    pages = {norm(e["_pic"][0]) for e in entries if e["_pic"]}
    pages |= {norm(e["wiki"]) for e in entries if e["wiki"]}
    moved = redirects({p for p in pages if p not in items})
    for e in entries:
        if e["type"] != "UNLOCK":
            continue
        if e["_pic"] and sprite_key(e["_pic"][1]) in sprites:
            e["sprite"] = sprites[sprite_key(e["_pic"][1])]
            continue
        for page in [e["_pic"][0] if e["_pic"] else None, e["wiki"]]:
            if not page:
                continue
            page = norm(page)
            for title in (page, moved.get(page), page.split("#")[0]):
                if title and title in items:
                    e["item"] = items[title]
                    break
            if "item" in e:
                break
    pictured = sum(1 for e in entries if "item" in e or "sprite" in e)
    print(f"icons: {pictured}/{len(entries)} entries")


def main():
    skills = {}
    for skill in SKILLS:
        try:
            entries = parse(fetch(skill))
        except Exception as e:  # keep going so one broken page does not block a refresh
            print(f"{skill}: failed ({e})", file=sys.stderr)
            continue
        skills[skill.upper()] = entries
        print(f"{skill}: {len(entries)} entries")
        time.sleep(0.5)
    if len(skills) != len(SKILLS):
        sys.exit("some skills failed; not writing output")
    add_icons(skills)
    skills = {k: [{f: v for f, v in e.items() if not f.startswith("_")} for e in es]
              for k, es in skills.items()}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "source": "https://oldschool.runescape.wiki (CC BY-NC-SA 3.0)",
        "fetched": datetime.date.today().isoformat(),
        "skills": skills,
    }
    OUT.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
