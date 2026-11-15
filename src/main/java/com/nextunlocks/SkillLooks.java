package com.nextunlocks;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

/** Each skill's own card for the Each skill's own style: a base style recolored to the skill's feel, with its own xp effect. */
final class SkillLooks
{
	private static final class Row
	{
		final NextUnlocksConfig.CardStyle base;
		/** Null keeps the base style's. */
		final Color background;
		final Color border;
		final Color innerBorder;
		final Color accent;
		final Color fill;
		final NextUnlocksConfig.XpEffect effect;

		Row(NextUnlocksConfig.CardStyle base, Color background, Color border, Color innerBorder, Color accent, Color fill,
			NextUnlocksConfig.XpEffect effect)
		{
			this.base = base;
			this.background = background;
			this.border = border;
			this.innerBorder = innerBorder;
			this.accent = accent;
			this.fill = fill;
			this.effect = effect;
		}
	}

	private static final Map<Skill, Row> ROWS = new EnumMap<>(Skill.class);

	static
	{
		row(Skill.ATTACK, NextUnlocksConfig.CardStyle.STONE, rgb(0x2a1210), rgb(0x7a1c14), rgb(0xff5a4a), rgb(0xc0281e), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.STRENGTH, NextUnlocksConfig.CardStyle.STONE, rgb(0x0f2312), rgb(0x1f6b2a), rgb(0x5ee06a), rgb(0x2e9b3c), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.DEFENCE, NextUnlocksConfig.CardStyle.STONE, rgb(0x141c2a), rgb(0x6a86b0), rgb(0x9cc4ff), rgb(0x4a78c0), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.RANGED, NextUnlocksConfig.CardStyle.STONE, rgb(0x18240f), rgb(0x5a7a2a), rgb(0xb4e05a), rgb(0x6a9a2a), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.PRAYER, NextUnlocksConfig.CardStyle.GILDED, rgb(0x1c1a14), rgb(0xe8e0c0), rgb(0xfffbe0), rgb(0xf0e6b4), NextUnlocksConfig.XpEffect.STARS);
		row(Skill.HITPOINTS, NextUnlocksConfig.CardStyle.NEON, rgb(0x2a0810), rgb(0xff3050), rgb(0xff6070), rgb(0xe01030), NextUnlocksConfig.XpEffect.EMBERS);
		row(Skill.COOKING, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0xb03a10), rgb(0xd06020), NextUnlocksConfig.XpEffect.EMBERS);
		row(Skill.WOODCUTTING, NextUnlocksConfig.CardStyle.STONE, rgb(0x1f170c), rgb(0x6b4a23), rgb(0x8fd16a), rgb(0x4f8a2a), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.FLETCHING, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0x1f6b5a), rgb(0x2a8a72), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.FISHING, NextUnlocksConfig.CardStyle.GLASS, new Color(40, 110, 170, 120), new Color(150, 210, 255, 140), rgb(0x8fe0ff), rgb(0x3aa0e0), NextUnlocksConfig.XpEffect.BUBBLES);
		row(Skill.FIREMAKING, NextUnlocksConfig.CardStyle.NEON, rgb(0x1e0a02), rgb(0xff7a1a), rgb(0xffb030), rgb(0xff6010), NextUnlocksConfig.XpEffect.EMBERS);
		row(Skill.CRAFTING, NextUnlocksConfig.CardStyle.GILDED, rgb(0x201018), rgb(0xc07aa0), rgb(0xffb0d8), rgb(0xd070a0), NextUnlocksConfig.XpEffect.STARS);
		row(Skill.SMITHING, NextUnlocksConfig.CardStyle.STONE, rgb(0x1a1a1c), rgb(0x8a8a90), rgb(0xffa040), rgb(0xe06a1a), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.MINING, NextUnlocksConfig.CardStyle.STONE, rgb(0x1c1812), rgb(0x6a5a48), rgb(0xd0b890), rgb(0x8a7458), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.HERBLORE, NextUnlocksConfig.CardStyle.GLASS, new Color(20, 80, 30, 120), new Color(120, 220, 140, 140), rgb(0x9cf0a8), rgb(0x30b048), NextUnlocksConfig.XpEffect.BUBBLES);
		row(Skill.AGILITY, NextUnlocksConfig.CardStyle.MODERN, rgb(0x101a2a), null, rgb(0x60a0ff), rgb(0x3070e0), NextUnlocksConfig.XpEffect.PIXELS);
		row(Skill.THIEVING, NextUnlocksConfig.CardStyle.MODERN, rgb(0x1a1020), null, rgb(0xc080ff), rgb(0x8040c0), NextUnlocksConfig.XpEffect.STARS);
		row(Skill.SLAYER, NextUnlocksConfig.CardStyle.NEON, rgb(0x1a0606), rgb(0xa01010), rgb(0xff4040), rgb(0xb01818), NextUnlocksConfig.XpEffect.EMBERS);
		row(Skill.FARMING, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0x2e7a1e), rgb(0x3e9a2a), NextUnlocksConfig.XpEffect.LEAVES);
		ROWS.put(Skill.RUNECRAFT, new Row(NextUnlocksConfig.CardStyle.ARCADE, rgb(0x0c0a1a), rgb(0xe0c060), rgb(0x8060ff),
			rgb(0xffd860), rgb(0xa070ff), NextUnlocksConfig.XpEffect.RUNES));
		row(Skill.HUNTER, NextUnlocksConfig.CardStyle.PARCHMENT, rgb(0xd8c89a), null, rgb(0x6b4a23), rgb(0x8b6a33), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.CONSTRUCTION, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0x7a4a1a), rgb(0xa06a30), NextUnlocksConfig.XpEffect.PIXELS);
		row(Skill.SAILING, NextUnlocksConfig.CardStyle.GLASS, new Color(30, 100, 150, 120), new Color(170, 225, 255, 140), rgb(0xffe9a8), rgb(0x2e9ac4), NextUnlocksConfig.XpEffect.BUBBLES);
	}

	private SkillLooks()
	{
	}

	private static void row(Skill skill, NextUnlocksConfig.CardStyle base, Color background, Color border, Color accent, Color fill,
		NextUnlocksConfig.XpEffect effect)
	{
		ROWS.put(skill, new Row(base, background, border, null, accent, fill, effect));
	}

	private static Color rgb(int rgb)
	{
		return new Color(rgb);
	}

	/** What flies off the skill's card as it gains xp, for Skill's own. */
	static NextUnlocksConfig.XpEffect effect(Skill skill)
	{
		if (skill == Skill.MAGIC)
		{
			return NextUnlocksConfig.XpEffect.STARS;
		}
		Row row = ROWS.get(skill);
		return row == null ? NextUnlocksConfig.XpEffect.SPARKS : row.effect;
	}

	/** The skill's own card. */
	static CardTheme theme(Skill skill, Palette palette, CardTheme classic)
	{
		if (skill == Skill.MAGIC)
		{
			return magic(palette);
		}
		Row row = ROWS.get(skill);
		if (row == null)
		{
			// a skill newer than this list: the stone card in the skill's color
			Color color = Skills.color(skill);
			row = new Row(NextUnlocksConfig.CardStyle.STONE, null, null, null, CardTheme.shade(color, Color.WHITE, 0.3), color,
				NextUnlocksConfig.XpEffect.SPARKS);
		}
		CardTheme base = CardTheme.preset(row.base, palette, classic, null);
		CardTheme.CardThemeBuilder theme = base.toBuilder()
			.skillName(row.accent)
			.nextUnlockLabel(row.accent)
			.nextUnlockLevel(row.accent)
			.taskTitle(row.accent)
			.barFill(row.fill)
			.xpEffect(row.effect);
		if (row.background != null)
		{
			theme.background(row.background);
			if (base.getBackground2() != null)
			{
				theme.background2(CardTheme.shade(row.background, Color.BLACK, 0.35));
			}
		}
		if (row.border != null)
		{
			theme.border(row.border);
			if (base.getInnerBorder() != null)
			{
				theme.innerBorder(row.innerBorder != null ? row.innerBorder : CardTheme.shade(row.border, Color.BLACK, 0.4));
			}
			if (base.getGlow() != null)
			{
				theme.glow(new Color(row.border.getRed(), row.border.getGreen(), row.border.getBlue(), base.getGlow().getAlpha()));
			}
		}
		if (base.getBarFill2() != null)
		{
			theme.barFill2(CardTheme.shade(row.fill, Color.WHITE, 0.5));
		}
		return theme.build();
	}

	/** A raised window in dusky rose with a navy title band, cyan and yellow text and a starry burst as xp comes in. */
	private static CardTheme magic(Palette palette)
	{
		Color cyan = new Color(0x00fdd2);
		return CardTheme.colors(cyan, Color.YELLOW, cyan, palette)
			.frame(CardTheme.Frame.BEVEL)
			.background(new Color(0xb88c9e))
			.headerBand(new Color(0, 0, 128))
			.padding(5)
			.opaque(true)
			.scanlines(true)
			.font(NextUnlocksConfig.CardFont.RUNESCAPE_BOLD)
			.bar(NextUnlocksConfig.BarStyle.SEGMENTED)
			.levelUp(true)
			.levelLabel(cyan)
			.more(new Color(0x3a2a40))
			.goalLabel(cyan)
			.goalLevel(Color.YELLOW)
			.goalXp(Color.GREEN)
			.barFill(new Color(0xc86a28))
			.barBackground(new Color(0x383838))
			.goalBar(new Color(0x7d40ff))
			.goalBar2(cyan)
			.goalBarBackground(new Color(0x4d4d4d))
			.xpEffect(NextUnlocksConfig.XpEffect.STARS)
			.xpPopup(true)
			.xpPulse(true)
			.glowSize(3)
			.build();
	}
}
