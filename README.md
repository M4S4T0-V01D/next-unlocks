<p align="center">
  <img src="screenshots/logo.png" alt="Next Unlocks logo" width="96" height="144">
</p>

<h1 align="center">Next Unlocks</h1>

<p align="center">See what your next levels unlock in every skill, straight from the game's own skill guide.</p>

<p align="center">
  <img src="screenshots/skillguide.png" alt="The skill guide's Next Unlocks tab">
</p>

## Features

### Skill guide
The game's skill guide gets a **Next Unlocks** tab listing what your next levels unlock, plus a progress bar to your next unlock above the list.
On the guide's own tabs, your next unlock is highlighted (and the ones after it can be faded), rows reached with a boost are marked green, quest-locked rows are greyed out, the list scrolls to your next unlock, and sections you've hidden are dimmed.
Each part can be turned off.

### Skills tab tooltip
<img src="screenshots/tooltip.png" alt="Skills tab tooltip" align="right">

Hover a skill to see its next unlock level, the XP left, a progress bar and the unlocks' icons, or only the icons.
It can take over the game's XP tooltip so the two don't overlap, and its icon size, text size and background opacity are adjustable.

Right-click a skill and choose **Pin** to keep its tooltip on screen above the chatbox. Right-click it again to unpin.
Pinned cards sit side by side at the same height, and their XP and progress update as you train.

<br clear="right">

<p align="center">
  <img src="screenshots/pin.png" alt="A pinned skill card above the chatbox">
</p>

### Side panel
<p align="center">
  <img src="screenshots/side-panel.png" alt="Side panel: Almost there and every skill's next unlocks" height="640">
  &nbsp;&nbsp;
  <img src="screenshots/side-panel2.png" alt="Side panel: one skill's Almost there list and next unlock levels" height="640">
</p>

- Every skill sorted by the closest upcoming unlock, or one skill's next few unlock levels.
- Items, spells and prayers show as their in-game icons with the level on them. Hover for the name and XP left, click to open it on the OSRS Wiki.
- Search any unlock across all skills, and see whether you have it yet.
- Hide whole skill guide sections per skill.
- **Goals**: right-click any unlock and pick **Set as goal**. Goals sit at the top of the panel with the XP left, and show as infoboxes; right-click the infobox to remove one. Reaching a goal posts a chat message and a notification, then clears it.
- **Almost there**: quests and achievement diaries where every skill requirement but one is met, closest first, and quests that are the only thing between you and unlocks you already have the level for. Click one to open it on the wiki. Diary completion isn't checked, so finished diaries can still show up.

### Also
- **Level-up message**: a chat message listing what the level you just reached unlocks, with the skill and level in grey and the unlocks in yellow by default, both changeable under Colors, with an optional text shadow.

  <img src="screenshots/chat.png" alt="Level-up chat message: New at Fishing 5: Karambwanji; Sardine; Required for Tai Bwo Wannai Trio">

- **Boosts**: unlocks your boosted level reaches count as reached for things the game lets you do boosted (skilling methods, spells, shortcuts, quests, diaries), but not equipment or prayers.
- **Quest-locked unlocks**: unlocks that need a quest you haven't finished are greyed out.
- **Filters**: members content (always, never, or match the world), quest and diary requirements, how many levels to list.

## Settings

<img src="screenshots/settings.png" alt="Settings" align="right">

- **General**: members unlocks, quest and diary requirements, levels to show, level-up message, boosts, greying out quest-locked unlocks.
- **Skills tab tooltip**: on/off, icons only, including the game's XP tooltip, progress bar, icon and text size, background opacity, outline.
- **Goals**: goal infoboxes, the goal-reached notification, the Almost there list.
- **Skill guide**: the Next Unlocks tab, highlighting, fading, markings, progress bar, scrolling, dimming hidden sections.
- **Colors**: next unlock, outline, progress bar and its background, level numbers, boost-reached and quest-locked, used by the tooltip, the side panel and the skill guide; plus the level-up message's skill/level and unlocks colors and its text shadow.

<br clear="right">

## Data

Unlocks come from the game's own skill guide, read from the client's cache, so their names and icons match the in-game guide.
Quest and diary requirements come from the OSRS Wiki "Level up table" pages, which ship with the plugin, so it makes no network requests; the wiki's unlocks are also used until the cache is loaded.
Refresh the wiki data with:

```
python3 -I tools/fetch_unlocks.py
```

Wiki content is licensed under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/); see https://oldschool.runescape.wiki.

## Development

```
./gradlew run     # starts RuneLite with the plugin loaded
./gradlew test
```
