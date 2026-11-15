package com.nextunlocks;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import lombok.Builder;
import lombok.Value;
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
	}

	private static final Color[] GREENS = {
		new Color(51, 255, 102), new Color(160, 255, 170), new Color(30, 190, 80), Color.WHITE,
	};
	private static final Color[] HANDHELD = {
		new Color(15, 56, 15), new Color(48, 98, 48), new Color(139, 172, 15), new Color(155, 188, 15),
	};

	Frame frame;
	Color background;
	/** Null for none. */
	Color border;
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
	Color barBackground;
	Color barText;

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

	/** The theme for pinned cards from the config: the picked style, then the player's own changes. */
	static CardTheme pinned(NextUnlocksConfig config, Palette palette, CardTheme classic, int opacity)
	{
		CardTheme style = preset(config.pinStyle(), palette, classic);
		CardTheme.CardThemeBuilder theme = style.toBuilder();
		if (style != classic && !style.opaque)
		{
			Color b = style.background;
			theme.background(new Color(b.getRed(), b.getGreen(), b.getBlue(), b.getAlpha() * opacity / 100));
		}
		if (config.pinFont() != NextUnlocksConfig.CardFont.STYLE)
		{
			theme.font(config.pinFont());
		}
		if (config.pinBar() != NextUnlocksConfig.BarStyle.STYLE)
		{
			theme.bar(config.pinBar());
		}
		theme.levelUp(config.pinLevelUp().on(style.levelUp));
		theme.shadow(config.pinTextShadow().on(style.shadow));
		theme.scanlines(config.pinScanlines().on(style.scanlines));
		if (config.pinGlow() != NextUnlocksConfig.GlowMode.STYLE)
		{
			NextUnlocksConfig.GlowMode mode = config.pinGlow();
			theme.glow(mode == NextUnlocksConfig.GlowMode.OFF ? null : config.pinGlowColor())
				.glowBorder(mode == NextUnlocksConfig.GlowMode.BORDER || mode == NextUnlocksConfig.GlowMode.BOTH)
				.glowText(mode == NextUnlocksConfig.GlowMode.TEXT || mode == NextUnlocksConfig.GlowMode.BOTH);
		}
		theme.glowSize(config.pinGlowSize());
		if (config.pinCustomColors())
		{
			Color border = config.pinColorBorder();
			theme.background(config.pinColorBackground())
				.border(border.getAlpha() == 0 ? null : border)
				.innerBorder(style.innerBorder == null ? null : border)
				.skillName(config.pinColorSkillName())
				.levelLabel(config.pinColorLevelLabel())
				.levelValue(config.pinColorLevelValue())
				.xpLabel(config.pinColorXpLabel())
				.xpValue(config.pinColorXpValue())
				.nextLevelLabel(config.pinColorNextLevelLabel())
				.nextLevelValue(config.pinColorNextLevelValue())
				.remainingLabel(config.pinColorRemainingLabel())
				.remainingValue(config.pinColorRemainingValue())
				.nextUnlockLabel(config.pinColorNextUnlockLabel())
				.nextUnlockLevel(config.pinColorNextUnlockLevel())
				.nextUnlockXp(config.pinColorNextUnlockXp())
				.unlock(config.pinColorUnlock())
				.more(config.pinColorMore())
				.goalLabel(config.pinColorGoalLabel())
				.goalLevel(config.pinColorGoalLevel())
				.goalXp(config.pinColorGoalXp())
				.taskTitle(config.pinColorTaskTitle())
				.met(config.pinColorMet())
				.unmet(config.pinColorUnmet())
				.barFill(config.pinColorBarFill())
				.barBackground(config.pinColorBarBackground())
				.barText(config.pinColorBarText());
		}
		return theme.build();
	}

	static CardTheme preset(NextUnlocksConfig.CardStyle style, Palette palette, CardTheme classic)
	{
		switch (style)
		{
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
			default:
				return classic;
		}
	}

	/** Every text color from three: labels, values, and the accent for names and the next unlock. */
	private static CardThemeBuilder colors(Color label, Color value, Color accent, Palette palette)
	{
		return builder()
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
