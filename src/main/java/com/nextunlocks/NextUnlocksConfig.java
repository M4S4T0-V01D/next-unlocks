package com.nextunlocks;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(NextUnlocksConfig.GROUP)
public interface NextUnlocksConfig extends Config
{
	String GROUP = "nextunlocks";

	enum MembersContent
	{
		MATCH_WORLD("Match world"),
		ALWAYS("Always"),
		NEVER("Never");

		private final String name;

		MembersContent(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigItem(
		keyName = "membersContent",
		name = "Members unlocks",
		description = "Show members-only unlocks. 'Match world' shows them only on members worlds",
		position = 1
	)
	default MembersContent membersContent()
	{
		return MembersContent.MATCH_WORLD;
	}

	@ConfigItem(
		keyName = "showQuests",
		name = "Quest requirements",
		description = "Show quests that need a level",
		position = 2
	)
	default boolean showQuests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDiaries",
		name = "Diary requirements",
		description = "Show achievement diaries that need a level",
		position = 3
	)
	default boolean showDiaries()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
		keyName = "levelsAhead",
		name = "Levels to show",
		description = "How many upcoming unlock levels the panel lists for a skill",
		position = 4
	)
	default int levelsAhead()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "levelUpMessage",
		name = "Level-up message",
		description = "Post a chat message listing what a new level unlocks",
		position = 5
	)
	default boolean levelUpMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countBoosts",
		name = "Count boosts",
		description = "Treat unlocks your boosted level reaches as reached, for things that accept boosts (not equipment or prayers)."
			+ " The panel lists them in green",
		position = 6
	)
	default boolean countBoosts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "greyOutLocked",
		name = "Grey out quest-locked",
		description = "Grey out unlocks that need a quest you haven't finished",
		position = 7
	)
	default boolean greyOutLocked()
	{
		return true;
	}

	@ConfigSection(
		name = "Skills tab tooltip",
		description = "The tooltip shown when hovering a skill in the skills tab",
		position = 10
	)
	String tooltipSection = "tooltip";

	@ConfigItem(
		keyName = "skillTooltip",
		name = "Show tooltip",
		description = "Show the next unlock when hovering a skill in the skills tab",
		position = 11,
		section = tooltipSection
	)
	default boolean skillTooltip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tooltipIconsOnly",
		name = "Icons only",
		description = "Show only the unlock icons, without the level line and names. Falls back to text when nothing at the next level has an icon",
		position = 12,
		section = tooltipSection
	)
	default boolean tooltipIconsOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tooltipXp",
		name = "Include game XP tooltip",
		description = "Hide the game's own skill XP tooltip and show its numbers in this one, so the two don't overlap",
		position = 13,
		section = tooltipSection
	)
	default boolean tooltipXp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tooltipProgress",
		name = "Progress bar",
		description = "Show progress from the last unlock level to the next",
		position = 14,
		section = tooltipSection
	)
	default boolean tooltipProgress()
	{
		return true;
	}

	@Range(min = 50, max = 200)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "tooltipIconSize",
		name = "Icon size",
		description = "Size of the unlock icons",
		position = 15,
		section = tooltipSection
	)
	default int tooltipIconSize()
	{
		return 100;
	}

	@Range(min = 10, max = 32)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "tooltipTextSize",
		name = "Text size",
		description = "Size of the tooltip text. 16 is the game's normal size",
		position = 16,
		section = tooltipSection
	)
	default int tooltipTextSize()
	{
		return 16;
	}

	@Range(max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "tooltipOpacity",
		name = "Background opacity",
		description = "Opacity of the tooltip background. Its color is RuneLite's overlay background color",
		position = 17,
		section = tooltipSection
	)
	default int tooltipOpacity()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "tooltipOutline",
		name = "Outline",
		description = "Draw an outline around the tooltip in the outline color",
		position = 18,
		section = tooltipSection
	)
	default boolean tooltipOutline()
	{
		return false;
	}

	@ConfigSection(
		name = "Skill guide",
		description = "Additions to the game's skill guide",
		position = 20
	)
	String guideSection = "guide";

	@ConfigItem(
		keyName = "guideTab",
		name = "Next Unlocks tab",
		description = "Add a Next Unlocks tab to the skill guide listing your upcoming unlocks for that skill",
		position = 21,
		section = guideSection
	)
	default boolean guideTab()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideHighlight",
		name = "Highlight next unlock",
		description = "Outline the rows of the next unlock level and color their level",
		position = 22,
		section = guideSection
	)
	default boolean guideHighlight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideFadeAhead",
		name = "Fade later unlocks",
		description = "Fade the rows past the next unlock level",
		position = 23,
		section = guideSection
	)
	default boolean guideFadeAhead()
	{
		return false;
	}

	@ConfigItem(
		keyName = "guideMarkings",
		name = "Boosted and quest-locked",
		description = "Color the levels of unlocks your boost reaches, and grey out quest-locked ones, following the boost and grey-out settings",
		position = 24,
		section = guideSection
	)
	default boolean guideMarkings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideProgress",
		name = "Progress bar",
		description = "Show progress to the next unlock above the skill guide list",
		position = 25,
		section = guideSection
	)
	default boolean guideProgress()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideScroll",
		name = "Scroll to next unlock",
		description = "Scroll the skill guide list to the next unlock when opening a tab",
		position = 26,
		section = guideSection
	)
	default boolean guideScroll()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideDimSections",
		name = "Dim hidden sections",
		description = "Dim the skill guide tabs of sections hidden in the panel",
		position = 27,
		section = guideSection
	)
	default boolean guideDimSections()
	{
		return true;
	}

	@ConfigSection(
		name = "Colors",
		description = "Colors in the tooltip and the skill guide",
		position = 30
	)
	String colorSection = "colors";

	@ConfigItem(
		keyName = "colorNext",
		name = "Next unlock",
		description = "The next unlock line in the tooltip, and the next unlock level in the skill guide",
		position = 31,
		section = colorSection
	)
	default Color colorNext()
	{
		return Palette.DEFAULT.getNext();
	}

	@Alpha
	@ConfigItem(
		keyName = "colorOutline",
		name = "Outline",
		description = "The outline of the next unlock rows in the skill guide, and of the tooltip when it has one",
		position = 32,
		section = colorSection
	)
	default Color colorOutline()
	{
		return Palette.DEFAULT.getOutline();
	}

	@ConfigItem(
		keyName = "colorProgress",
		name = "Progress bar",
		description = "The filled part of the progress bars",
		position = 33,
		section = colorSection
	)
	default Color colorProgress()
	{
		return Palette.DEFAULT.getProgress();
	}

	@Alpha
	@ConfigItem(
		keyName = "colorProgressBackground",
		name = "Progress bar background",
		description = "The empty part of the progress bars",
		position = 34,
		section = colorSection
	)
	default Color colorProgressBackground()
	{
		return Palette.DEFAULT.getProgressBackground();
	}

	@ConfigItem(
		keyName = "colorLevel",
		name = "Icon level",
		description = "The level number on unlock icons",
		position = 35,
		section = colorSection
	)
	default Color colorLevel()
	{
		return Palette.DEFAULT.getLevel();
	}

	@ConfigItem(
		keyName = "colorBoost",
		name = "Reached by boost",
		description = "Unlocks your boosted level reaches",
		position = 36,
		section = colorSection
	)
	default Color colorBoost()
	{
		return Palette.DEFAULT.getBoost();
	}

	@ConfigItem(
		keyName = "colorLocked",
		name = "Quest-locked",
		description = "Unlocks that need a quest you haven't finished",
		position = 37,
		section = colorSection
	)
	default Color colorLocked()
	{
		return Palette.DEFAULT.getLocked();
	}

	@ConfigItem(
		keyName = "pinnedSkills",
		name = "",
		description = "Skills whose tooltip is pinned above the chatbox, comma separated",
		hidden = true
	)
	default String pinnedSkills()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenSections",
		name = "",
		description = "Skill guide sections hidden in the panel and tooltips, as SKILL:Header, semicolon separated",
		hidden = true
	)
	default String hiddenSections()
	{
		return "";
	}
}
