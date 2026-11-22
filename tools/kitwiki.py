"""OSRS Wiki helpers for a plugin's data tools: polite requests, an on-disk cache and the queries the plugins use.
Load it by path from a tool in the same folder (python3 -I keeps tools/ off sys.path):

    import importlib.util, pathlib
    _spec = importlib.util.spec_from_file_location("kitwiki", pathlib.Path(__file__).resolve().parent / "kitwiki.py")
    wiki = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(wiki)
    wiki.setup("my-plugin data builder", pathlib.Path(__file__).resolve().parent / ".cache" / "npcs")

Wiki content is licensed CC BY-NC-SA 3.0 (https://oldschool.runescape.wiki); credit it in the README.
"""
import hashlib
import json
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
# The wiki asks for a descriptive User-Agent; set it with setup().
USER_AGENT = None
CACHE = None
DELAY = 0.3


def setup(user_agent, cache_dir, delay=0.3):
    global USER_AGENT, CACHE, DELAY
    USER_AGENT, CACHE, DELAY = user_agent, cache_dir, delay


def api(params):
    if not USER_AGENT:
        raise RuntimeError("call kitwiki.setup(user_agent, cache_dir) first")
    params = dict(params, format="json", formatversion=2)
    req = urllib.request.Request(f"{API}?{urllib.parse.urlencode(params)}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.load(resp)
    time.sleep(DELAY)
    return data


def cached(name, refresh, build):
    """The JSON in CACHE/name, or build() saved there. refresh=True rebuilds."""
    CACHE.mkdir(parents=True, exist_ok=True)
    path = CACHE / name
    if path.exists() and not refresh:
        return json.loads(path.read_text())
    result = build()
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(result))
    tmp.replace(path)
    return result


def key(titles):
    return hashlib.sha1("\n".join(titles).encode()).hexdigest()[:16]


def embeddedin(templates, refresh=False, namespace=0):
    """Titles of every page using any of the templates, e.g. ("Template:Infobox NPC",)."""
    def build():
        titles = set()
        for template in templates:
            params = {"action": "query", "list": "embeddedin", "eititle": template,
                      "einamespace": namespace, "eilimit": 500}
            while True:
                data = api(params)
                titles.update(p["title"] for p in data["query"]["embeddedin"])
                if "continue" not in data:
                    break
                params.update(data["continue"])
        return sorted(titles)
    return cached(f"pages-{key(list(templates))}.json", refresh, build)


def category(name, refresh=False):
    """Titles of the pages in a category, e.g. "Category:Quests"."""
    def build():
        titles = []
        params = {"action": "query", "list": "categorymembers", "cmtitle": name, "cmnamespace": 0, "cmlimit": 500}
        while True:
            data = api(params)
            titles += [p["title"] for p in data["query"]["categorymembers"]]
            if "continue" not in data:
                break
            params.update(data["continue"])
        return sorted(titles)
    return cached(f"cat-{key([name])}.json", refresh, build)


def wikitext(titles, refresh=False):
    """{title: wikitext}, 50 pages per request."""
    pages = {}
    for i in range(0, len(titles), 50):
        batch = titles[i:i + 50]

        def build():
            data = api({"action": "query", "prop": "revisions", "rvprop": "content",
                        "rvslots": "main", "titles": "|".join(batch)})
            return {p["title"]: p["revisions"][0]["slots"]["main"]["content"]
                    for p in data["query"]["pages"] if p.get("revisions")}
        pages.update(cached(f"wt-{key(batch)}.json", refresh, build))
        print(f"\r  wikitext {min(i + 50, len(titles))}/{len(titles)}", end="", flush=True)
    print()
    return pages


def intros(titles, refresh=False):
    """{title: plain-text intro}, 20 per request (the most the wiki gives for intros)."""
    pages = {}
    for i in range(0, len(titles), 20):
        batch = titles[i:i + 20]

        def build():
            data = api({"action": "query", "prop": "extracts", "explaintext": 1, "exintro": 1,
                        "exlimit": 20, "titles": "|".join(batch)})
            return {p["title"]: p.get("extract") or "" for p in data["query"]["pages"]}
        pages.update(cached(f"in-{key(batch)}.json", refresh, build))
        print(f"\r  intros {min(i + 20, len(titles))}/{len(titles)}", end="", flush=True)
    print()
    return pages


def full(title, refresh=False):
    """A whole page as plain text; the wiki gives only one at a time."""
    def build():
        data = api({"action": "query", "prop": "extracts", "explaintext": 1, "titles": title})
        return data["query"]["pages"][0].get("extract") or ""
    return cached(f"full-{key([title])}.json", refresh, build)


def module(title, refresh=False):
    """The source of a Lua data module page, e.g. "Module:Skill calc/Mining/data"; parse with luadata.parse."""
    return wikitext([title], refresh).get(title, "")


def images(names, folder, refresh=False):
    """Saves the wiki's File:<name> images (e.g. "Abyssal_whip.png") in folder and gives their paths. Files already
    there are kept, and the rest are looked up 50 per API request (imageinfo), so a re-run asks the wiki for nothing."""
    folder.mkdir(parents=True, exist_ok=True)
    paths = {name: folder / name for name in names}
    missing = [n for n, path in paths.items() if refresh or not path.exists()]
    for i in range(0, len(missing), 50):
        batch = missing[i:i + 50]
        data = api({"action": "query", "prop": "imageinfo", "iiprop": "url",
                    "titles": "|".join("File:" + n for n in batch)})
        query = data["query"]
        # the API answers with normalized titles ("File:Abyssal whip.png"); map them back to the names asked for
        asked = {"File:" + n: n for n in batch}
        asked.update({step["to"]: asked[step["from"]] for step in query.get("normalized", [])})
        for page in query["pages"]:
            info = page.get("imageinfo")
            if not info:
                print(f"  no file {page['title']}")
                continue
            req = urllib.request.Request(info[0]["url"], headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=60) as resp:
                tmp = paths[asked[page["title"]]].with_suffix(".tmp")
                tmp.write_bytes(resp.read())
                tmp.replace(paths[asked[page["title"]]])
            time.sleep(DELAY)
    return [paths[n] for n in names if paths[n].exists()]
