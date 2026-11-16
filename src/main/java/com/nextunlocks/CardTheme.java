package com.nextunlocks;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;

/**
 * How a card looks: its frame, font, bar and the color of every word and number on it. The skills-tab tooltip is
 * always {@link #classic}; pinned cards use the style picked in the config, with the player's changes on top.
 */
@Value
@Builder(toBuilder = true)
class CardTheme
{
	enum Frame
	{
		/** RuneLite's own overlay box, with its lighter edge. */
		CLASSIC,
		FLAT,
		/** An outer and an inner border, like an arcade cabinet's screen. */
		DOUBLE,
		/** Raised grey edges, like an old desktop window. */
		BEVEL,
		ROUNDED,
		/** Corners cut off on the slant; the corner setting sizes the cut. */
		NOTCHED,
		/** A double frame bolted down at the corners and along the edges. */
		RIVETED,
		/** A faint edge with heavy brackets at the corners. */
		BRACKETS,
		/** A double frame with diamonds at the corners and the middle of the top and bottom. */
		ORNATE,
		/** Stepped corners, like an old console's text box. */
		PIXEL,
		DASHED,
	}

	private static final Color[] GREENS = {
		new Color(51, 255, 102), new Color(160, 255, 170), new Color(30, 190, 80), Color.WHITE,
	};
	private static final Color[] HANDHELD = {
		new Color(15, 56, 15), new Color(48, 98, 48), new Color(139, 172, 15), new Color(155, 188, 15),
	};

	Frame frame;
	Color background;
	/** The bottom of a top-to-bottom gradient behind the card; null for a flat background. */
	Color background2;
	/** Null for none. */
	Color border;
	/** In pixels. */
	int borderWidth;
	/** Inside the border on a double frame; null for none. */
	Color innerBorder;
	/** A title bar behind the card's first line, null for none. */
	Color headerBand;
	int corner;
	/** Space between the frame and the content. */
	int padding;
	/** The background ignores the opacity setting, for looks that only work solid. */
	boolean opaque;
	boolean scanlines;
	boolean shadow;
	NextUnlocksConfig.CardFont font;
	NextUnlocksConfig.BarStyle bar;
	/** Whether a reached unlock holds the bar at 100% and level-ups burst into pixels. */
	boolean levelUp;
	/** The burst's colors; null for a rainbow. */
	Color[] burst;
	/** Null for no glow. */
	Color glow;
	boolean glowBorder;
	boolean glowText;
	int glowSize;
	/** A light that sweeps across the card now and then. */
	boolean shimmer;
	/** What flies off the bar as the skill gains xp; AUTO for the skill's own, null or OFF for nothing. */
	NextUnlocksConfig.XpEffect xpEffect;
	/** The xp just gained floats up off the card. */
	boolean xpPopup;
	/** The bar lights up as xp comes in. */
	boolean xpPulse;
	/** The skill's icon before its name on the title line. */
	boolean skillIcon;
	/** What drifts about inside the card all the time; null for nothing. */
	NextUnlocksConfig.Ambient ambient;
	/** How the border moves; null for not at all. */
	NextUnlocksConfig.BorderMotion borderMotion;

	Color skillName;
	Color levelLabel;
	Color levelValue;
	Color xpLabel;
	Color xpValue;
	Color nextLevelLabel;
	Color nextLevelValue;
	Color remainingLabel;
	Color remainingValue;
	Color nextUnlockLabel;
	Color nextUnlockLevel;
	Color nextUnlockXp;
	Color unlock;
	Color locked;
	Color more;
	/** Null for the skill's own color. */
	Color goalLabel;
	/** Null for the skill's own color. */
	Color goalLevel;
	Color goalXp;
	Color taskTitle;
	Color met;
	Color unmet;
	Color barFill;
	/** The bar's far end, for a gradient along it; null for one color. */
	Color barFill2;
	Color barBackground;
	Color barText;
	/** Null for the skill's own color. */
	Color goalBar;
	/** The goal bar's far end, for a gradient; null for one color. */
	Color goalBar2;
	/** Null for the progress bar's background. */
	Color goalBarBackground;

	/** The look the plugin always had. */
	static CardTheme classic(Palette palette, Color background, Color outline)
	{
		return colors(Color.WHITE, Color.WHITE, palette.getNext(), palette)
			.frame(Frame.CLASSIC)
			.background(background)
			.border(outline)
			.padding(4)
			.shadow(true)
			.font(NextUnlocksConfig.CardFont.RUNESCAPE)
			.bar(NextUnlocksConfig.BarStyle.CLASSIC)
			.glowSize(3)
			.build();
	}

	/**
	 * The theme for a pinned card: the picked style, then the player's own changes. The skill is the card's, null for
	 * quests, diaries and items, and picks the look of Each skill's own and the effect of Skill's own.
	 */
	static CardTheme pinned(Look look, Palette palette, CardTheme classic, Skill skill)
	{
		return pinned(look, palette, classic, skill, false);
	}

	/** {@link #pinned}, for the combat level card when {@code combat}, whose Each skill's own is a look of its own. */
	static CardTheme pinned(Look look, Palette palette, CardTheme classic, Skill skill, boolean combat)
	{
		int opacity = look.get("tooltipOpacity");
		NextUnlocksConfig.CardStyle picked = look.get("pinStyle");
		Skill base = base(look.get("pinBase"), skill);
		CardTheme style = combat && base == null && picked == NextUnlocksConfig.CardStyle.SKILL ? SkillLooks.combat(palette, classic)
			: preset(picked, palette, classic, base);
		CardTheme.CardThemeBuilder theme = style.toBuilder();
		if (style != classic && !style.opaque)
		{
			theme.background(fade(style.background, opacity));
			theme.background2(style.background2 == null ? null : fade(style.background2, opacity));
		}
		NextUnlocksConfig.CardFont font = look.get("pinFont");
		if (font != NextUnlocksConfig.CardFont.STYLE)
		{
			theme.font(font);
		}
		NextUnlocksConfig.BarStyle bar = look.get("pinBar");
		if (bar != NextUnlocksConfig.BarStyle.STYLE)
		{
			theme.bar(bar);
		}
		theme.levelUp(look.<NextUnlocksConfig.StyleToggle>get("pinLevelUp").on(style.levelUp));
		theme.shadow(look.<NextUnlocksConfig.StyleToggle>get("pinTextShadow").on(style.shadow));
		theme.scanlines(look.<NextUnlocksConfig.StyleToggle>get("pinScanlines").on(style.scanlines));
		theme.skillIcon(look.<Boolean>get("pinSkillIcon"));
		NextUnlocksConfig.GlowMode mode = look.get("pinGlow");
		if (mode != NextUnlocksConfig.GlowMode.STYLE)
		{
			theme.glow(mode == NextUnlocksConfig.GlowMode.OFF ? null : look.get("pinGlowColor"))
				.glowBorder(mode == NextUnlocksConfig.GlowMode.BORDER || mode == NextUnlocksConfig.GlowMode.BOTH)
				.glowText(mode == NextUnlocksConfig.GlowMode.TEXT || mode == NextUnlocksConfig.GlowMode.BOTH);
		}
		theme.glowSize(look.get("pinGlowSize"));
		boolean custom = look.get("pinCustomColors");
		if (custom)
		{
			Color border = look.get("pinColorBorder");
			theme.background(look.get("pinColorBackground"))
				.border(border.getAlpha() == 0 ? null : border)
				.innerBorder(style.innerBorder == null ? null : border)
				.skillName(look.get("pinColorSkillName"))
				.levelLabel(look.get("pinColorLevelLabel"))
				.levelValue(look.get("pinColorLevelValue"))
				.xpLabel(look.get("pinColorXpLabel"))
				.xpValue(look.get("pinColorXpValue"))
				.nextLevelLabel(look.get("pinColorNextLevelLabel"))
				.nextLevelValue(look.get("pinColorNextLevelValue"))
				.remainingLabel(look.get("pinColorRemainingLabel"))
				.remainingValue(look.get("pinColorRemainingValue"))
				.nextUnlockLabel(look.get("pinColorNextUnlockLabel"))
				.nextUnlockLevel(look.get("pinColorNextUnlockLevel"))
				.nextUnlockXp(look.get("pinColorNextUnlockXp"))
				.unlock(look.get("pinColorUnlock"))
				.more(look.get("pinColorMore"))
				.goalLabel(look.get("pinColorGoalLabel"))
				.goalLevel(look.get("pinColorGoalLevel"))
				.goalXp(look.get("pinColorGoalXp"))
				.taskTitle(look.get("pinColorTaskTitle"))
				.met(look.get("pinColorMet"))
				.unmet(look.get("pinColorUnmet"))
				.barFill(look.get("pinColorBarFill"))
				.barBackground(look.get("pinColorBarBackground"))
				.barText(look.get("pinColorBarText"));
			Color goal = look.get("pinColorGoalBar");
			Color goal2 = look.get("pinColorGoalBar2");
			if (goal.getAlpha() > 0)
			{
				theme.goalBar(goal);
			}
			if (goal2.getAlpha() > 0)
			{
				theme.goalBar2(goal2);
			}
		}
		CardTheme colored = theme.build();

		// the switches that build on the final colors
		if (look.<NextUnlocksConfig.StyleToggle>get("pinBackgroundGradient").on(style.background2 != null))
		{
			Color own = custom ? look.<Color>get("pinColorBackground2") : null;
			theme.background2(own != null && own.getAlpha() > 0 ? own
				: colored.background2 != null && !custom ? colored.background2 : shade(colored.background, Color.BLACK, 0.5));
		}
		else
		{
			theme.background2(null);
		}
		if (look.<NextUnlocksConfig.StyleToggle>get("pinBarGradient").on(style.barFill2 != null))
		{
			Color own = custom ? look.<Color>get("pinColorBarFill2") : null;
			theme.barFill2(own != null && own.getAlpha() > 0 ? own
				: colored.barFill2 != null && !custom ? colored.barFill2 : shade(colored.barFill, Color.WHITE, 0.5));
		}
		else
		{
			theme.barFill2(null);
		}
		if (look.<NextUnlocksConfig.StyleToggle>get("pinHeaderBand").on(style.headerBand != null))
		{
			Color fallback = colored.border != null ? colored.border : colored.barFill;
			theme.headerBand(custom ? look.get("pinColorHeaderBand")
				: colored.headerBand != null ? colored.headerBand : shade(fallback, Color.BLACK, 0.4));
		}
		else
		{
			theme.headerBand(null);
		}
		theme.shimmer(look.<NextUnlocksConfig.StyleToggle>get("pinShimmer").on(style.shimmer));
		theme.xpPopup(look.<NextUnlocksConfig.StyleToggle>get("pinXpPopup").on(style.xpPopup));
		theme.xpPulse(look.<NextUnlocksConfig.StyleToggle>get("pinXpPulse").on(style.xpPulse));
		NextUnlocksConfig.XpEffect effect = look.get("pinXpEffect");
		if (effect == NextUnlocksConfig.XpEffect.STYLE)
		{
			effect = style.xpEffect;
		}
		if (effect == NextUnlocksConfig.XpEffect.AUTO)
		{
			effect = skill == null ? null : SkillLooks.effect(skill);
		}
		theme.xpEffect(effect == NextUnlocksConfig.XpEffect.OFF ? null : effect);
		NextUnlocksConfig.Ambient ambient = look.get("pinAmbient");
		if (ambient == NextUnlocksConfig.Ambient.STYLE)
		{
			ambient = style.ambient;
		}
		if (ambient == NextUnlocksConfig.Ambient.AUTO)
		{
			ambient = skill != null ? SkillLooks.ambient(skill) : combat ? SkillLooks.combatAmbient() : null;
		}
		theme.ambient(ambient == NextUnlocksConfig.Ambient.OFF ? null : ambient);
		NextUnlocksConfig.BorderMotion motion = look.get("pinBorderMotion");
		if (motion != NextUnlocksConfig.BorderMotion.STYLE)
		{
			theme.borderMotion(motion == NextUnlocksConfig.BorderMotion.OFF ? null : motion);
		}

		NextUnlocksConfig.BorderWidth width = look.get("pinBorderWidth");
		if (width == NextUnlocksConfig.BorderWidth.NONE)
		{
			theme.border(null).innerBorder(null);
		}
		else if (width != NextUnlocksConfig.BorderWidth.STYLE)
		{
			theme.borderWidth(width.pixels);
		}
		NextUnlocksConfig.Corner corner = look.get("pinCorner");
		if (corner != NextUnlocksConfig.Corner.STYLE)
		{
			theme.corner(corner.radius);
			if (corner.radius > 0 && (colored.frame == Frame.CLASSIC || colored.frame == Frame.FLAT || colored.frame == Frame.DOUBLE))
			{
				theme.frame(Frame.ROUNDED);
			}
			else if (corner.radius == 0 && colored.frame == Frame.ROUNDED)
			{
				theme.frame(Frame.FLAT);
			}
		}
		NextUnlocksConfig.FrameStyle frame = look.get("pinFrame");
		if (frame != NextUnlocksConfig.FrameStyle.STYLE)
		{
			theme.frame(Frame.valueOf(frame.name()));
			if (frame == NextUnlocksConfig.FrameStyle.ROUNDED && (corner == NextUnlocksConfig.Corner.STYLE ? colored.corner : corner.radius) == 0)
			{
				// rounded with square corners would only be flat
				theme.corner(NextUnlocksConfig.Corner.ROUND.radius);
			}
		}
		NextUnlocksConfig.Padding padding = look.get("pinPadding");
		if (padding != NextUnlocksConfig.Padding.STYLE)
		{
			theme.padding(padding.pixels);
		}
		return theme.build();
	}

	/** The skill whose look Each skill's own shows: the one picked for every card, or the card's own. */
	static Skill base(String picked, Skill skill)
	{
		if (picked != null && !picked.isEmpty())
		{
			try
			{
				return Skill.valueOf(picked);
			}
			catch (IllegalArgumentException e)
			{
				// a skill from a newer or older client; the card's own then
			}
		}
		return skill;
	}

	private static Color fade(Color c, int opacity)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() * opacity / 100);
	}

	/** Mixed toward another color, keeping the first one's see-through. */
	static Color shade(Color c, Color toward, double amount)
	{
		Color mixed = PinBar.mix(c, toward, amount);
		return new Color(mixed.getRed(), mixed.getGreen(), mixed.getBlue(), c.getAlpha());
	}

	/** A style's own look; Each skill's own needs the skill, and is classic without one. */
	static CardTheme preset(NextUnlocksConfig.CardStyle style, Palette palette, CardTheme classic, Skill skill)
	{
		switch (style)
		{
			case SKILL:
				return skill == null ? classic : SkillLooks.theme(skill, palette, classic);
			case ARCADE:
				return colors(new Color(64, 224, 255), Color.WHITE, new Color(255, 236, 64), palette)
					.frame(Frame.DOUBLE)
					.background(new Color(8, 6, 20, 240))
					.border(new Color(255, 40, 200))
					.innerBorder(new Color(40, 220, 255))
					.padding(6)
					.scanlines(true)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE_BOLD)
					.bar(NextUnlocksConfig.BarStyle.ARCADE)
					.levelUp(true)
					.nextUnlockLabel(new Color(255, 96, 208))
					.more(new Color(170, 150, 255))
					.met(new Color(96, 255, 96))
					.unmet(new Color(255, 70, 110))
					.barFill(new Color(255, 120, 40))
					.barBackground(new Color(34, 22, 66))
					.xpEffect(NextUnlocksConfig.XpEffect.PIXELS)
					.xpPopup(true)
					.xpPulse(true)
					.glowSize(3)
					.build();
			case NEON:
				Color pink = new Color(255, 70, 200);
				return colors(new Color(196, 164, 255), new Color(96, 240, 255), pink, palette)
					.frame(Frame.ROUNDED)
					.background(new Color(26, 8, 42, 240))
					.border(pink)
					.corner(6)
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.levelUp(true)
					.more(new Color(150, 120, 200))
					.met(new Color(96, 255, 190))
					.unmet(new Color(255, 90, 120))
					.barFill(pink)
					.barBackground(new Color(70, 24, 96))
					.xpEffect(NextUnlocksConfig.XpEffect.STARS)
					.xpPopup(true)
					.xpPulse(true)
					.glow(new Color(255, 60, 200, 200))
					.glowBorder(true)
					.glowText(true)
					.glowSize(3)
					.build();
			case TERMINAL:
				Color phosphor = new Color(51, 255, 102);
				return colors(new Color(40, 190, 80), new Color(120, 255, 150), phosphor, palette)
					.frame(Frame.FLAT)
					.background(new Color(0, 8, 2, 240))
					.border(phosphor)
					.padding(5)
					.scanlines(true)
					.font(NextUnlocksConfig.CardFont.MONO)
					.bar(NextUnlocksConfig.BarStyle.SEGMENTED)
					.levelUp(true)
					.burst(GREENS)
					.unlock(new Color(120, 255, 150))
					.more(new Color(30, 140, 60))
					.met(new Color(120, 255, 150))
					.unmet(new Color(255, 176, 0))
					.barFill(phosphor)
					.barBackground(new Color(0, 44, 14))
					.barText(new Color(210, 255, 210))
					.xpEffect(NextUnlocksConfig.XpEffect.PIXELS)
					.xpPopup(true)
					.glow(new Color(51, 255, 102, 110))
					.glowText(true)
					.glowSize(2)
					.build();
			case HANDHELD:
				return colors(HANDHELD[1], HANDHELD[0], HANDHELD[0], palette)
					.frame(Frame.DOUBLE)
					.background(HANDHELD[3])
					.border(HANDHELD[0])
					.innerBorder(HANDHELD[2])
					.padding(6)
					.opaque(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.SEGMENTED)
					.levelUp(true)
					.burst(HANDHELD)
					.locked(HANDHELD[2])
					.more(HANDHELD[1])
					.met(HANDHELD[0])
					.unmet(HANDHELD[1])
					.goalLabel(HANDHELD[0])
					.goalLevel(HANDHELD[0])
					.barFill(HANDHELD[0])
					.barBackground(HANDHELD[2])
					.barText(HANDHELD[3])
					.xpEffect(NextUnlocksConfig.XpEffect.PIXELS)
					.xpPopup(true)
					.glowSize(3)
					.build();
			case WIN95:
				Color navy = new Color(0, 0, 128);
				return colors(Color.BLACK, Color.BLACK, navy, palette)
					.frame(Frame.BEVEL)
					.background(new Color(192, 192, 192))
					.headerBand(navy)
					.padding(5)
					.opaque(true)
					.font(NextUnlocksConfig.CardFont.SANS)
					.bar(NextUnlocksConfig.BarStyle.SEGMENTED)
					.skillName(Color.WHITE)
					.levelLabel(Color.WHITE)
					.levelValue(Color.WHITE)
					.taskTitle(Color.WHITE)
					.locked(new Color(110, 110, 110))
					.more(new Color(64, 64, 64))
					.met(new Color(0, 128, 0))
					.unmet(new Color(176, 0, 0))
					.barFill(navy)
					.barBackground(Color.WHITE)
					.glowSize(3)
					.build();
			case MODERN:
				Color accent = new Color(100, 170, 255);
				return colors(new Color(150, 156, 170), Color.WHITE, accent, palette)
					.frame(Frame.ROUNDED)
					.background(new Color(22, 24, 30, 240))
					.border(new Color(255, 255, 255, 28))
					.corner(8)
					.padding(7)
					.font(NextUnlocksConfig.CardFont.SANS)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.skillName(Color.WHITE)
					.unlock(new Color(220, 224, 232))
					.more(new Color(120, 126, 140))
					.met(new Color(90, 210, 140))
					.unmet(new Color(240, 100, 100))
					.barFill(accent)
					.barBackground(new Color(255, 255, 255, 30))
					.glowSize(3)
					.build();
			case MINIMAL:
				return colors(new Color(200, 200, 200), Color.WHITE, palette.getNext(), palette)
					.frame(Frame.FLAT)
					.background(new Color(0, 0, 0, 60))
					.padding(3)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.glowSize(3)
					.build();
			case PARCHMENT:
				Color ink = new Color(43, 26, 10);
				return colors(new Color(110, 74, 34), ink, new Color(139, 26, 26), palette)
					.frame(Frame.DOUBLE)
					.background(new Color(232, 213, 166))
					.background2(new Color(206, 182, 128))
					.border(new Color(107, 74, 35))
					.innerBorder(new Color(165, 129, 78))
					.padding(6)
					.opaque(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.CLASSIC)
					.locked(new Color(150, 132, 100))
					.more(new Color(120, 96, 64))
					.met(new Color(46, 107, 31))
					.unmet(new Color(160, 16, 16))
					.goalXp(ink)
					.barFill(new Color(139, 90, 43))
					.barBackground(new Color(196, 172, 120))
					.xpEffect(NextUnlocksConfig.XpEffect.AUTO)
					.xpPopup(true)
					.glowSize(3)
					.build();
			case STONE:
				Color orange = new Color(255, 152, 31);
				return colors(orange, Color.WHITE, new Color(255, 255, 0), palette)
					.frame(Frame.DOUBLE)
					.background(new Color(62, 53, 41, 245))
					.background2(new Color(40, 34, 26, 245))
					.border(new Color(20, 17, 13))
					.borderWidth(2)
					.innerBorder(new Color(94, 84, 66))
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE_BOLD)
					.bar(NextUnlocksConfig.BarStyle.CLASSIC)
					.more(new Color(180, 168, 140))
					.barFill(new Color(0, 160, 0))
					.barBackground(new Color(140, 20, 20))
					.xpEffect(NextUnlocksConfig.XpEffect.AUTO)
					.xpPopup(true)
					.xpPulse(true)
					.glowSize(3)
					.build();
			case GILDED:
				Color gold = new Color(212, 175, 55);
				return colors(new Color(201, 168, 106), new Color(255, 243, 196), new Color(255, 215, 0), palette)
					.frame(Frame.DOUBLE)
					.background(new Color(30, 20, 6, 245))
					.background2(new Color(52, 36, 8, 245))
					.border(gold)
					.borderWidth(2)
					.innerBorder(new Color(138, 109, 31))
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE_BOLD)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.more(new Color(160, 136, 90))
					.barFill(gold)
					.barFill2(new Color(255, 240, 160))
					.barBackground(new Color(58, 42, 8))
					.shimmer(true)
					.xpEffect(NextUnlocksConfig.XpEffect.STARS)
					.xpPopup(true)
					.xpPulse(true)
					.glow(new Color(255, 215, 0, 90))
					.glowBorder(true)
					.glowSize(2)
					.build();
			case GLASS:
				return colors(new Color(220, 230, 245), Color.WHITE, new Color(160, 210, 255), palette)
					.frame(Frame.ROUNDED)
					.background(new Color(190, 215, 255, 80))
					.background2(new Color(90, 120, 170, 60))
					.border(new Color(255, 255, 255, 110))
					.corner(8)
					.padding(7)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.SANS)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.more(new Color(200, 210, 225))
					.barFill(new Color(140, 200, 255))
					.barFill2(new Color(230, 245, 255))
					.barBackground(new Color(255, 255, 255, 40))
					.shimmer(true)
					.xpEffect(NextUnlocksConfig.XpEffect.AUTO)
					.xpPulse(true)
					.glowSize(3)
					.build();
			default:
				return classic;
		}
	}

	/** Every text color from three: labels, values, and the accent for names and the next unlock. */
	static CardThemeBuilder colors(Color label, Color value, Color accent, Palette palette)
	{
		return builder()
			.borderWidth(1)
			.skillName(accent)
			.levelLabel(value)
			.levelValue(value)
			.xpLabel(label)
			.xpValue(value)
			.nextLevelLabel(label)
			.nextLevelValue(value)
			.remainingLabel(label)
			.remainingValue(value)
			.nextUnlockLabel(accent)
			.nextUnlockLevel(accent)
			.nextUnlockXp(value)
			.unlock(value)
			.locked(palette.getLocked())
			.more(Color.LIGHT_GRAY)
			.goalXp(value)
			.taskTitle(accent)
			.met(Color.GREEN)
			.unmet(Color.RED)
			.barFill(palette.getProgress())
			.barBackground(palette.getProgressBackground())
			.barText(Color.WHITE);
	}

	/** Whether the font is a smooth desktop one, which wants antialiasing, rather than the game's pixel fonts. */
	boolean smooth()
	{
		return font == NextUnlocksConfig.CardFont.SANS || font == NextUnlocksConfig.CardFont.SANS_BOLD
			|| font == NextUnlocksConfig.CardFont.MONO;
	}

	/** How much wider this theme's text runs than the game font's, so cards can grow to fit; never less than 1. */
	double widthScale(int textSize)
	{
		String sample = "Next unlock: 99 1,234,567 xp";
		FontRenderContext context = new FontRenderContext(null, smooth(), false);
		Font game = FontManager.getRunescapeFont().deriveFont((float) textSize);
		return Math.max(1, font(textSize).getStringBounds(sample, context).getWidth() / game.getStringBounds(sample, context).getWidth());
	}

	/** The text font, for a text size in the game font's points. */
	Font font(int textSize)
	{
		switch (font)
		{
			case RUNESCAPE_BOLD:
				return FontManager.getRunescapeBoldFont().deriveFont((float) textSize);
			case RUNESCAPE_SMALL:
				return FontManager.getRunescapeSmallFont().deriveFont((float) textSize);
			case SANS:
				return desktop(Font.SANS_SERIF, Font.PLAIN, textSize);
			case SANS_BOLD:
				return desktop(Font.SANS_SERIF, Font.BOLD, textSize);
			case MONO:
				return desktop(Font.MONOSPACED, Font.PLAIN, textSize);
			default:
				Font regular = FontManager.getRunescapeFont();
				return textSize == regular.getSize() ? regular : regular.deriveFont((float) textSize);
		}
	}

	/** The bold font for quest and diary names, grown by some points. */
	Font titleFont(int textSize, int growth)
	{
		switch (font)
		{
			case SANS:
			case SANS_BOLD:
				return desktop(Font.SANS_SERIF, Font.BOLD, textSize + growth);
			case MONO:
				return desktop(Font.MONOSPACED, Font.BOLD, textSize + growth);
			default:
				Font bold = FontManager.getRunescapeBoldFont();
				return bold.deriveFont((float) Math.round(bold.getSize2D() * textSize / FontManager.getRunescapeFont().getSize() + growth));
		}
	}

	/** Desktop fonts look bigger than the game's at the same size. */
	private static Font desktop(String name, int style, int textSize)
	{
		return new Font(name, style, 1).deriveFont((float) Math.round(textSize * 0.78f));
	}
}
