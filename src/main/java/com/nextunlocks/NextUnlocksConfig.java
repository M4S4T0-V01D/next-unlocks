package com.nextunlocks;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
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

	@ConfigItem(
		keyName = "guideMessages",
		name = "Guide step messages",
		description = "Post a chat message when a step of a guide you follow is done, with the next step",
		position = 8
	)
	default boolean guideMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideCapWarnings",
		name = "Pure cap warnings",
		description = "Post a red chat warning when you gain XP in a skill a pure guide you follow keeps capped",
		position = 9
	)
	default boolean guideCapWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideCards",
		name = "Guide cards in game",
		description = "Show a card above the chatbox for each guide you follow, with its next steps",
		position = 9
	)
	default boolean guideCards()
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
		return true;
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

	@ConfigItem(
		keyName = "pinnedLevel",
		name = "Level on pinned cards",
		description = "Head pinned cards with the skill and your current level, boosted level first when boosted",
		position = 19,
		section = tooltipSection
	)
	default boolean pinnedLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pinnedGoal",
		name = "Goal bar on pinned cards",
		description = "Show the XP left and a bar in the skill's color for the skill's nearest goal on its pinned card",
		position = 20,
		section = tooltipSection
	)
	default boolean pinnedGoal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "combatTop",
		name = "Combat level at top",
		description = "Show your combat level and a bar to the next level at the top middle of the screen",
		position = 21,
		section = tooltipSection
	)
	default boolean combatTop()
	{
		return false;
	}

	/** Looks for the pinned cards, from the RuneLite default through the 90s to modern. */
	enum CardStyle
	{
		/** A look of its own for every skill, in the skill's colors; quest, diary and item cards stay classic. */
		SKILL("Each skill's own"),
		CLASSIC("Classic RuneLite"),
		ARCADE("90s arcade"),
		NEON("Neon synthwave"),
		TERMINAL("CRT terminal"),
		HANDHELD("Handheld (4 greens)"),
		WIN95("Windows 95"),
		MODERN("Modern"),
		MINIMAL("Minimal"),
		PARCHMENT("Parchment scroll"),
		STONE("RuneScape stone"),
		GILDED("Gilded"),
		GLASS("Frosted glass");

		private final String name;

		CardStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum CardFont
	{
		STYLE("Style default"),
		RUNESCAPE("RuneScape"),
		RUNESCAPE_BOLD("RuneScape bold"),
		RUNESCAPE_SMALL("RuneScape small"),
		SANS("Sans"),
		SANS_BOLD("Sans bold"),
		MONO("Monospaced");

		private final String name;

		CardFont(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum BarStyle
	{
		STYLE("Style default"),
		CLASSIC("Classic"),
		ARCADE("Arcade (shakes near 100%)"),
		SEGMENTED("Segmented blocks"),
		MODERN("Modern rounded");

		private final String name;

		BarStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum GlowMode
	{
		STYLE("Style default"),
		OFF("Off"),
		BORDER("Border"),
		TEXT("Text"),
		BOTH("Border and text");

		private final String name;

		GlowMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** A setting the style decides unless it is turned on or off here. */
	enum StyleToggle
	{
		STYLE("Style default"),
		ON("On"),
		OFF("Off");

		private final String name;

		StyleToggle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}

		boolean on(boolean style)
		{
			return this == STYLE ? style : this == ON;
		}
	}

	/** What flies off the bar while the skill gains xp. */
	enum XpEffect
	{
		STYLE("Style default"),
		AUTO("Skill's own"),
		OFF("Off"),
		SPARKS("Sparks"),
		EMBERS("Embers"),
		BUBBLES("Bubbles"),
		LEAVES("Leaves"),
		STARS("Stars"),
		PIXELS("Pixels"),
		RUNES("Runes"),
		HEARTS("Hearts"),
		COINS("Coins"),
		SMOKE("Smoke"),
		BOLTS("Lightning");

		private final String name;

		XpEffect(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** The shape of a card's frame; the names match the card's own frames. */
	enum FrameStyle
	{
		STYLE("Style default"),
		CLASSIC("Classic"),
		FLAT("Flat"),
		DOUBLE("Double"),
		BEVEL("Raised"),
		ROUNDED("Rounded"),
		NOTCHED("Cut corners"),
		RIVETED("Riveted"),
		BRACKETS("Corner brackets"),
		ORNATE("Ornate"),
		PIXEL("Pixel"),
		DASHED("Dashed"),
		SCALLOPED("Scalloped"),
		TICKET("Ticket");

		private final String name;

		FrameStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** Decorations drawn over the frame: corner pieces and things running along the edges. */
	enum Trim
	{
		STYLE("Style default"),
		AUTO("Skill's own"),
		OFF("None"),
		FILIGREE("Gold filigree"),
		PRINCESS("Princess"),
		VINES("Ivy vines"),
		ROSES("Rose vines"),
		THORNS("Thorny briar"),
		ANVILS("Anvils"),
		SWORDS("Crossed swords"),
		GEMS("Gems"),
		STARS("Twinkling stars"),
		HEARTS("Hearts"),
		SKULLS("Skulls"),
		SNOW("Snowflakes"),
		CHAINS("Chains"),
		ANCHOR("Anchor and chain"),
		NET("Fishing net"),
		LOGS("Logs and hatchet"),
		FLAMES("Flames"),
		HERBS("Herbs"),
		RUNES("Runes"),
		BONES("Bones"),
		ARROWS("Arrows and bow"),
		DRAGON_WEAPONS("Dragon weapons"),
		WHIP("Abyssal whip"),
		MINIMAP_DRAGONS("Minimap dragons");

		private final String name;

		Trim(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum TrimSize
	{
		SMALL("Small", 0.75),
		NORMAL("Normal", 1),
		LARGE("Large", 1.35);

		private final String name;
		final double scale;

		TrimSize(String name, double scale)
		{
			this.name = name;
			this.scale = scale;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** What drifts about inside a card all the time, behind the text. */
	enum Ambient
	{
		STYLE("Style default"),
		AUTO("Skill's own"),
		OFF("Off"),
		EMBERS("Embers"),
		BUBBLES("Bubbles"),
		SNOW("Snow"),
		FIREFLIES("Fireflies"),
		SPARKLES("Sparkles"),
		LEAVES("Leaves"),
		RUNES("Runes"),
		DUST("Dust");

		private final String name;

		Ambient(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** How a card's border moves. */
	enum BorderMotion
	{
		STYLE("Style default"),
		OFF("Off"),
		PULSE("Pulse"),
		RUNNER("Running light"),
		RAINBOW("Rainbow"),
		MARCH("Marching"),
		FLICKER("Flicker");

		private final String name;

		BorderMotion(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** How thick a card's border is, in pixels; none leaves it off. */
	enum BorderWidth
	{
		STYLE("Style default", -1),
		NONE("None", 0),
		THIN("Thin", 1),
		THICK("Thick", 2),
		HEAVY("Heavy", 3);

		private final String name;
		final int pixels;

		BorderWidth(String name, int pixels)
		{
			this.name = name;
			this.pixels = pixels;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** How round a card's corners are, in pixels. */
	enum Corner
	{
		STYLE("Style default", -1),
		SQUARE("Square", 0),
		SMALL("Small", 3),
		ROUND("Round", 6),
		ROUNDER("Rounder", 10);

		private final String name;
		final int radius;

		Corner(String name, int radius)
		{
			this.name = name;
			this.radius = radius;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** Space between a card's edge and its text, in pixels. */
	enum Padding
	{
		STYLE("Style default", -1),
		TIGHT("Tight", 2),
		NORMAL("Normal", 5),
		ROOMY("Roomy", 8);

		private final String name;
		final int pixels;

		Padding(String name, int pixels)
		{
			this.name = name;
			this.pixels = pixels;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigSection(
		name = "Pinned card style",
		description = "How the cards pinned above the chatbox look",
		position = 21
	)
	String pinStyleSection = "pinStyle";

	@ConfigItem(
		keyName = "pinStyle",
		name = "Style",
		description = "The look of pinned cards. The settings below can change parts of it",
		position = 100,
		section = pinStyleSection
	)
	default CardStyle pinStyle()
	{
		return CardStyle.SKILL;
	}

	@ConfigItem(
		keyName = "pinFont",
		name = "Font",
		description = "The font on pinned cards",
		position = 101,
		section = pinStyleSection
	)
	default CardFont pinFont()
	{
		return CardFont.STYLE;
	}

	@ConfigItem(
		keyName = "pinBar",
		name = "Progress bar",
		description = "The unlock progress bar on pinned cards. Arcade shakes and shifts color as it nears 100%",
		position = 102,
		section = pinStyleSection
	)
	default BarStyle pinBar()
	{
		return BarStyle.STYLE;
	}

	@ConfigItem(
		keyName = "pinLevelUp",
		name = "Level-up effect",
		description = "On a level-up the bar bursts into pixels; reaching the unlock first holds it at a flashing 100%",
		position = 103,
		section = pinStyleSection
	)
	default StyleToggle pinLevelUp()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinGlow",
		name = "Glow",
		description = "A soft glow around the card's border, its text, or both. Neon and CRT terminal glow by default",
		position = 104,
		section = pinStyleSection
	)
	default GlowMode pinGlow()
	{
		return GlowMode.STYLE;
	}

	@Alpha
	@ConfigItem(
		keyName = "pinGlowColor",
		name = "Glow color",
		description = "The color of the glow, when Glow is set to something other than the style default",
		position = 105,
		section = pinStyleSection
	)
	default Color pinGlowColor()
	{
		return new Color(64, 224, 255, 220);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "pinGlowSize",
		name = "Glow size",
		description = "How far the glow spreads, in pixels",
		position = 106,
		section = pinStyleSection
	)
	default int pinGlowSize()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "pinTextShadow",
		name = "Text shadow",
		description = "A dark shadow under the text",
		position = 107,
		section = pinStyleSection
	)
	default StyleToggle pinTextShadow()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinScanlines",
		name = "Scanlines",
		description = "Faint lines across the card, like an old CRT screen",
		position = 108,
		section = pinStyleSection
	)
	default StyleToggle pinScanlines()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinSkillIcon",
		name = "Skill icon",
		description = "The skill's icon before its name on pinned cards, as tall as the text",
		position = 108,
		section = pinStyleSection
	)
	default boolean pinSkillIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pinCustomColors",
		name = "Use my colors",
		description = "Color every word and number on pinned cards with the colors in 'Pinned card colors' instead of the style's",
		position = 109,
		section = pinStyleSection
	)
	default boolean pinCustomColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pinXpEffect",
		name = "XP effect",
		description = "What flies off the bar as you gain xp. Skill's own picks one for each skill, like embers for Firemaking and bubbles for Fishing",
		position = 110,
		section = pinStyleSection
	)
	default XpEffect pinXpEffect()
	{
		return XpEffect.STYLE;
	}

	@ConfigItem(
		keyName = "pinXpPopup",
		name = "XP popup",
		description = "The xp you just gained floats up off the card",
		position = 111,
		section = pinStyleSection
	)
	default StyleToggle pinXpPopup()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinXpPulse",
		name = "Bar pulse",
		description = "The bar lights up for a moment as you gain xp",
		position = 112,
		section = pinStyleSection
	)
	default StyleToggle pinXpPulse()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinBackgroundGradient",
		name = "Background gradient",
		description = "Shade the background from top to bottom, down to Background 2 under Pinned card colors when Use my colors is on",
		position = 113,
		section = pinStyleSection
	)
	default StyleToggle pinBackgroundGradient()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinBarGradient",
		name = "Bar gradient",
		description = "Run the bar from its fill color to Bar fill 2",
		position = 114,
		section = pinStyleSection
	)
	default StyleToggle pinBarGradient()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinShimmer",
		name = "Shimmer",
		description = "A light that sweeps across the card now and then",
		position = 115,
		section = pinStyleSection
	)
	default StyleToggle pinShimmer()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinHeaderBand",
		name = "Title band",
		description = "A band of color behind the card's first line, like a window's title bar",
		position = 116,
		section = pinStyleSection
	)
	default StyleToggle pinHeaderBand()
	{
		return StyleToggle.STYLE;
	}

	@ConfigItem(
		keyName = "pinBorderWidth",
		name = "Border width",
		description = "How thick the card's border is",
		position = 117,
		section = pinStyleSection
	)
	default BorderWidth pinBorderWidth()
	{
		return BorderWidth.STYLE;
	}

	@ConfigItem(
		keyName = "pinCorner",
		name = "Corners",
		description = "How round the card's corners are",
		position = 118,
		section = pinStyleSection
	)
	default Corner pinCorner()
	{
		return Corner.STYLE;
	}

	@ConfigItem(
		keyName = "pinPadding",
		name = "Padding",
		description = "Space between the card's edge and its text",
		position = 119,
		section = pinStyleSection
	)
	default Padding pinPadding()
	{
		return Padding.STYLE;
	}

	@ConfigItem(
		keyName = "pinFrame",
		name = "Frame",
		description = "The shape of the card's frame: cut corners, rivets, corner brackets, ornaments and more",
		position = 120,
		section = pinStyleSection
	)
	default FrameStyle pinFrame()
	{
		return FrameStyle.STYLE;
	}

	@ConfigItem(
		keyName = "pinBorderMotion",
		name = "Border motion",
		description = "Make the border pulse, flicker, cycle colors or run a light around the card",
		position = 121,
		section = pinStyleSection
	)
	default BorderMotion pinBorderMotion()
	{
		return BorderMotion.STYLE;
	}

	@ConfigItem(
		keyName = "pinAmbient",
		name = "Ambient",
		description = "Something that drifts about inside the card all the time, behind the text. Skill's own picks one for each skill",
		position = 122,
		section = pinStyleSection
	)
	default Ambient pinAmbient()
	{
		return Ambient.STYLE;
	}

	@ConfigItem(
		keyName = "pinTrim",
		name = "Trim",
		description = "Decorations over the frame: filigree, vines, anvils, swords, nets, flames, runes and more. Skill's own picks one to"
			+ " suit each skill. Goes with any frame",
		position = 123,
		section = pinStyleSection
	)
	default Trim pinTrim()
	{
		return Trim.AUTO;
	}

	@ConfigItem(
		keyName = "pinTrimSize",
		name = "Trim size",
		description = "How big the trim's corner pieces and edge decorations are",
		position = 124,
		section = pinStyleSection
	)
	default TrimSize pinTrimSize()
	{
		return TrimSize.NORMAL;
	}

	@ConfigItem(
		keyName = "pinTrimTint",
		name = "Trim in border color",
		description = "Draw the trim in the card's border color instead of its own gold, green, red and so on",
		position = 125,
		section = pinStyleSection
	)
	default StyleToggle pinTrimTint()
	{
		return StyleToggle.STYLE;
	}

	@ConfigSection(
		name = "Pinned card colors",
		description = "Each word and number on pinned cards; used when 'Use my colors' is on",
		position = 22,
		closedByDefault = true
	)
	String pinColorSection = "pinColors";

	@Alpha
	@ConfigItem(
		keyName = "pinColorBackground",
		name = "Background",
		description = "The card's background; its alpha sets how see-through it is",
		position = 200,
		section = pinColorSection
	)
	default Color pinColorBackground()
	{
		return new Color(70, 61, 50, 156);
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorBorder",
		name = "Border",
		description = "The card's border",
		position = 201,
		section = pinColorSection
	)
	default Color pinColorBorder()
	{
		return Palette.DEFAULT.getOutline();
	}

	@ConfigItem(
		keyName = "pinColorSkillName",
		name = "Skill name",
		description = "The skill's name at the top",
		position = 202,
		section = pinColorSection
	)
	default Color pinColorSkillName()
	{
		return Palette.DEFAULT.getNext();
	}

	@ConfigItem(
		keyName = "pinColorLevelLabel",
		name = "'Level' word",
		description = "The word 'Level' at the top",
		position = 203,
		section = pinColorSection
	)
	default Color pinColorLevelLabel()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorLevelValue",
		name = "Level number",
		description = "Your level at the top (boosted levels stay green or red)",
		position = 204,
		section = pinColorSection
	)
	default Color pinColorLevelValue()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorXpLabel",
		name = "'XP:' word",
		description = "The 'XP:' label",
		position = 205,
		section = pinColorSection
	)
	default Color pinColorXpLabel()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorXpValue",
		name = "XP number",
		description = "Your XP",
		position = 206,
		section = pinColorSection
	)
	default Color pinColorXpValue()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorNextLevelLabel",
		name = "'Next level:' words",
		description = "The 'Next level:' label",
		position = 207,
		section = pinColorSection
	)
	default Color pinColorNextLevelLabel()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorNextLevelValue",
		name = "Next level XP number",
		description = "The XP of the next level",
		position = 208,
		section = pinColorSection
	)
	default Color pinColorNextLevelValue()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorRemainingLabel",
		name = "'Remaining XP:' words",
		description = "The 'Remaining XP:' label",
		position = 209,
		section = pinColorSection
	)
	default Color pinColorRemainingLabel()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorRemainingValue",
		name = "Remaining XP number",
		description = "The XP left to the next level",
		position = 210,
		section = pinColorSection
	)
	default Color pinColorRemainingValue()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorNextUnlockLabel",
		name = "'Next unlock:' words",
		description = "The 'Next unlock:' label",
		position = 211,
		section = pinColorSection
	)
	default Color pinColorNextUnlockLabel()
	{
		return Palette.DEFAULT.getNext();
	}

	@ConfigItem(
		keyName = "pinColorNextUnlockLevel",
		name = "Next unlock level",
		description = "The level of the next unlock",
		position = 212,
		section = pinColorSection
	)
	default Color pinColorNextUnlockLevel()
	{
		return Palette.DEFAULT.getNext();
	}

	@ConfigItem(
		keyName = "pinColorNextUnlockXp",
		name = "Next unlock XP number",
		description = "The XP left to the next unlock",
		position = 213,
		section = pinColorSection
	)
	default Color pinColorNextUnlockXp()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorUnlock",
		name = "Unlocks",
		description = "The unlocks listed as text",
		position = 214,
		section = pinColorSection
	)
	default Color pinColorUnlock()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorMore",
		name = "'+N more'",
		description = "The count of unlocks that don't fit",
		position = 215,
		section = pinColorSection
	)
	default Color pinColorMore()
	{
		return Color.LIGHT_GRAY;
	}

	@ConfigItem(
		keyName = "pinColorGoalLabel",
		name = "'Goal:' word",
		description = "The 'Goal:' label",
		position = 216,
		section = pinColorSection
	)
	default Color pinColorGoalLabel()
	{
		return Palette.DEFAULT.getNext();
	}

	@ConfigItem(
		keyName = "pinColorGoalLevel",
		name = "Goal level",
		description = "The goal's level",
		position = 217,
		section = pinColorSection
	)
	default Color pinColorGoalLevel()
	{
		return Palette.DEFAULT.getNext();
	}

	@ConfigItem(
		keyName = "pinColorGoalXp",
		name = "Goal XP number",
		description = "The XP left to the goal",
		position = 218,
		section = pinColorSection
	)
	default Color pinColorGoalXp()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "pinColorTaskTitle",
		name = "Quest/diary name",
		description = "The name on quest and diary cards",
		position = 219,
		section = pinColorSection
	)
	default Color pinColorTaskTitle()
	{
		return Palette.DEFAULT.getNext();
	}

	@ConfigItem(
		keyName = "pinColorMet",
		name = "Requirement met",
		description = "Quest and diary requirements you have",
		position = 220,
		section = pinColorSection
	)
	default Color pinColorMet()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "pinColorUnmet",
		name = "Requirement not met",
		description = "Quest and diary requirements you don't have",
		position = 221,
		section = pinColorSection
	)
	default Color pinColorUnmet()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "pinColorBarFill",
		name = "Bar fill",
		description = "The filled part of the progress bar",
		position = 222,
		section = pinColorSection
	)
	default Color pinColorBarFill()
	{
		return Palette.DEFAULT.getProgress();
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorBarBackground",
		name = "Bar background",
		description = "The empty part of the progress bar",
		position = 223,
		section = pinColorSection
	)
	default Color pinColorBarBackground()
	{
		return Palette.DEFAULT.getProgressBackground();
	}

	@ConfigItem(
		keyName = "pinColorBarText",
		name = "Bar text",
		description = "The percentage on the progress bar",
		position = 224,
		section = pinColorSection
	)
	default Color pinColorBarText()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorBackground2",
		name = "Background 2",
		description = "The bottom of the background with Background gradient on; fully see-through for a darker shade of Background",
		position = 225,
		section = pinColorSection
	)
	default Color pinColorBackground2()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorBarFill2",
		name = "Bar fill 2",
		description = "The far end of the bar with Bar gradient on; fully see-through for a lighter shade of Bar fill",
		position = 226,
		section = pinColorSection
	)
	default Color pinColorBarFill2()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorGoalBar",
		name = "Goal bar",
		description = "The goal bar; fully see-through for the skill's color",
		position = 227,
		section = pinColorSection
	)
	default Color pinColorGoalBar()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorGoalBar2",
		name = "Goal bar 2",
		description = "The goal bar's far end, for a gradient; fully see-through for none",
		position = 228,
		section = pinColorSection
	)
	default Color pinColorGoalBar2()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "pinColorHeaderBand",
		name = "Title band",
		description = "The band behind the first line with Title band on",
		position = 229,
		section = pinColorSection
	)
	default Color pinColorHeaderBand()
	{
		return new Color(0, 0, 128);
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
		name = "Goals",
		description = "Unlocks set as goals, and what's one requirement away",
		position = 28
	)
	String goalSection = "goals";

	@ConfigItem(
		keyName = "goalInfoboxes",
		name = "Goal infoboxes",
		description = "Show an infobox with the XP left for each goal. Right-click an unlock in the side panel, or a skill, to set a goal",
		position = 28,
		section = goalSection
	)
	default boolean goalInfoboxes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "goalNotification",
		name = "Goal reached notification",
		description = "Notify when you reach a goal; a chat message is always posted",
		position = 29,
		section = goalSection
	)
	default Notification goalNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "tallyBank",
		name = "Banked on amount goals",
		description = "Show how many of an amount goal's item are in your bank, as of the last time it was open",
		position = 29,
		section = goalSection
	)
	default boolean tallyBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "almostThere",
		name = "Almost there",
		description = "List quests and diaries one skill level short, and unlocks you have the level for that only need a quest, in the side panel",
		position = 30,
		section = goalSection
	)
	default boolean almostThere()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recentUnlocks",
		name = "Recently unlocked",
		description = "List what your latest level-ups unlocked, and when, at the top of the side panel. Each skill's page keeps its full timeline either way",
		position = 30,
		section = goalSection
	)
	default boolean recentUnlocks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "itemMakingLevels",
		name = "Item goals: making levels",
		description = "Item cards also need the levels to make the item from scratch, as an ironman would, not just the ones to wield or wear it",
		position = 30,
		section = goalSection
	)
	default boolean itemMakingLevels()
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
		keyName = "colorChatLevel",
		name = "Message skill and level",
		description = "The skill and level (e.g. 'Smithing 10:') in the level-up message",
		position = 38,
		section = colorSection
	)
	default Color colorChatLevel()
	{
		return new Color(0xB4B4B4);
	}

	@ConfigItem(
		keyName = "colorChatUnlocks",
		name = "Message unlocks",
		description = "The unlocks listed in the level-up message",
		position = 39,
		section = colorSection
	)
	default Color colorChatUnlocks()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "chatShadow",
		name = "Message text shadow",
		description = "Draw a black shadow under the coloured parts of the level-up message, so light colours stay readable on the opaque chatbox",
		position = 40,
		section = colorSection
	)
	default boolean chatShadow()
	{
		return true;
	}

	@ConfigSection(
		name = "In-game menu",
		description = "A button on the minimap's rim that opens a menu of these settings, goals and pins in the game",
		position = 41
	)
	String menuSection = "menu";

	@ConfigItem(
		keyName = "minimapButton",
		name = "Minimap button",
		description = "Show the Next Unlocks button on the minimap's rim, which opens the in-game menu",
		position = 41,
		section = menuSection
	)
	default boolean minimapButton()
	{
		return true;
	}

	@Range(max = 359)
	@ConfigItem(
		keyName = "minimapButtonPosition",
		name = "Button position",
		description = "Where on the minimap's rim the button sits, in degrees clockwise from the right; 324 is just below "
			+ "VisionHD's menu button",
		position = 42,
		section = menuSection
	)
	default int minimapButtonAngle()
	{
		return 324;
	}

	@ConfigItem(
		keyName = "goals",
		name = "",
		description = "Unlocks set as goals, as SKILL|level|text, one per line",
		hidden = true
	)
	default String goals()
	{
		return "";
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
		keyName = "pinnedTasks",
		name = "",
		description = "Wiki titles of the quests and diaries pinned above the chatbox, one per line",
		hidden = true
	)
	default String pinnedTasks()
	{
		return "";
	}

	@ConfigItem(
		keyName = "goalStarts",
		name = "",
		description = "The XP each goal was set at, so its bar runs from there",
		hidden = true
	)
	default String goalStarts()
	{
		return "";
	}

	@ConfigItem(
		keyName = "combatGoalStart",
		name = "",
		description = "The exact combat level the combat goal was set at",
		hidden = true
	)
	default String combatGoalStart()
	{
		return "";
	}

	@ConfigItem(
		keyName = "pinnedCombat",
		name = "",
		description = "Whether the combat level card is pinned above the chatbox",
		hidden = true
	)
	default boolean pinnedCombat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "combatGoal",
		name = "",
		description = "The combat level to aim for on the combat level card, 0 for none",
		hidden = true
	)
	default int combatGoal()
	{
		return 0;
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

	@ConfigItem(
		keyName = "checkedOff",
		name = "",
		description = "Requirements of pinned cards checked off by hand, as key, tab, requirement, one per line",
		hidden = true
	)
	default String checkedOff()
	{
		return "";
	}

	@ConfigItem(
		keyName = "tallyBanked",
		name = "",
		description = "How many of each amount goal's item the bank held when last open, as item:amount, comma separated",
		hidden = true
	)
	default String tallyBanked()
	{
		return "";
	}

	@ConfigItem(
		keyName = "tallies",
		name = "",
		description = "Amount goals, as SKILL|item|target|count|name, one per line",
		hidden = true
	)
	default String tallies()
	{
		return "";
	}

	@ConfigItem(
		keyName = "skillLooks",
		name = "",
		description = "Each skill's own changes to the pinned card look, as SKILL|key=value;key=value, one per line",
		hidden = true
	)
	default String skillLooks()
	{
		return "";
	}

	@ConfigItem(
		keyName = "lookPresets",
		name = "",
		description = "Saved pinned card looks, as name|key=value;key=value, one per line",
		hidden = true
	)
	default String lookPresets()
	{
		return "";
	}

	@ConfigItem(
		keyName = "pinBase",
		name = "",
		description = "Whose built-in look Each skill's own uses: a skill's name, or empty for each card's own skill",
		hidden = true
	)
	default String pinBase()
	{
		return "";
	}
}
