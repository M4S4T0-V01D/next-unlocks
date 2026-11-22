#!/usr/bin/env python3
"""Fetches the wiki icons the README's rendered pictures use into tools/.cache/art, through the wiki API.
Run with: python3 -I tools/fetch_art.py [--refresh]

Icons already in the folder are kept, so only new ones are asked for. Then render with
PIN_ART=tools/.cache/art PIN_GALLERY=<dir> (PinPreviewTest) or GUIDE_ICONS=tools/.cache/art (GuideCardTest).

Wiki content is licensed CC BY-NC-SA 3.0 (https://oldschool.runescape.wiki).
"""
import importlib.util
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("kitwiki", HERE / "kitwiki.py")
wiki = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wiki)
wiki.setup("next-unlocks README renders (github.com/M4S4T0-V01D/next-unlocks)", HERE / ".cache" / "api")

# the item trims' icons, the showcase's unlock icons (PinPreviewTest) and the guide cards' icons (GuideCardTest)
NAMES = [
    "Abyssal_whip", "Dragon_claws", "Dragon_scimitar", "Dragon_longsword",
    "Ardougne_teleport_(tablet)", "Law_rune", "Abyssal_head", "Raw_trout", "Fly_fishing_rod", "Lobster", "Raw_lobster",
    "Mithril_platebody", "Mithril_bar", "Big_bones", "Holy_symbol", "Maple_logs", "Tinderbox", "Nature_rune",
    "Pure_essence", "Super_attack(4)", "Irit_leaf", "Yew_logs", "Rune_axe",
    "Bronze_longsword", "Ironman_helm", "Quest_point_icon", "Skull", "Stats_icon",
]

if __name__ == "__main__":
    saved = wiki.images([n + ".png" for n in NAMES], HERE / ".cache" / "art", "--refresh" in sys.argv)
    print(f"{len(saved)}/{len(NAMES)} icons in {HERE / '.cache' / 'art'}")
