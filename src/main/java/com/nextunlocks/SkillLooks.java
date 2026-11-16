package com.nextunlocks;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Each skill's own card for the Each skill's own style: a base style recolored to the skill's feel, with its own frame,
 * xp effect, ambient and border motion.
 */
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
		/** Null keeps the base style's. */
		CardTheme.Frame frame;
		/** Null for none. */
		NextUnlocksConfig.Ambient ambient;
		/** Null for none. */
		NextUnlocksConfig.BorderMotion motion;

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
	/** The combat level card's own: dark iron in a gold frame, with a blood-red bar. */
	private static final Row COMBAT = new Row(NextUnlocksConfig.CardStyle.STONE, rgb(0x1c1414), rgb(0xb8892e), null, rgb(0xffd257),
		rgb(0xc0281e), NextUnlocksConfig.XpEffect.SPARKS);

	static
	{
		row(Skill.ATTACK, NextUnlocksConfig.CardStyle.STONE, rgb(0x2a1210), rgb(0x7a1c14), rgb(0xff5a4a), rgb(0xc0281e), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.STRENGTH, NextUnlocksConfig.CardStyle.STONE, rgb(0x0f2312), rgb(0x1f6b2a), rgb(0x5ee06a), rgb(0x2e9b3c), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.DEFENCE, NextUnlocksConfig.CardStyle.STONE, rgb(0x141c2a), rgb(0x6a86b0), rgb(0x9cc4ff), rgb(0x4a78c0), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.RANGED, NextUnlocksConfig.CardStyle.STONE, rgb(0x18240f), rgb(0x5a7a2a), rgb(0xb4e05a), rgb(0x6a9a2a), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.PRAYER, NextUnlocksConfig.CardStyle.GILDED, rgb(0x1c1a14), rgb(0xe8e0c0), rgb(0xfffbe0), rgb(0xf0e6b4), NextUnlocksConfig.XpEffect.STARS);
		row(Skill.HITPOINTS, NextUnlocksConfig.CardStyle.NEON, rgb(0x2a0810), rgb(0xff3050), rgb(0xff6070), rgb(0xe01030), NextUnlocksConfig.XpEffect.HEARTS);
		row(Skill.COOKING, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0xb03a10), rgb(0xd06020), NextUnlocksConfig.XpEffect.SMOKE);
		row(Skill.WOODCUTTING, NextUnlocksConfig.CardStyle.STONE, rgb(0x1f170c), rgb(0x6b4a23), rgb(0x8fd16a), rgb(0x4f8a2a), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.FLETCHING, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0x1f6b5a), rgb(0x2a8a72), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.FISHING, NextUnlocksConfig.CardStyle.GLASS, new Color(40, 110, 170, 120), new Color(150, 210, 255, 140), rgb(0x8fe0ff), rgb(0x3aa0e0), NextUnlocksConfig.XpEffect.BUBBLES);
		row(Skill.FIREMAKING, NextUnlocksConfig.CardStyle.NEON, rgb(0x1e0a02), rgb(0xff7a1a), rgb(0xffb030), rgb(0xff6010), NextUnlocksConfig.XpEffect.EMBERS);
		row(Skill.CRAFTING, NextUnlocksConfig.CardStyle.GILDED, rgb(0x201018), rgb(0xc07aa0), rgb(0xffb0d8), rgb(0xd070a0), NextUnlocksConfig.XpEffect.STARS);
		row(Skill.SMITHING, NextUnlocksConfig.CardStyle.STONE, rgb(0x1a1a1c), rgb(0x8a8a90), rgb(0xffa040), rgb(0xe06a1a), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.MINING, NextUnlocksConfig.CardStyle.STONE, rgb(0x1c1812), rgb(0x6a5a48), rgb(0xd0b890), rgb(0x8a7458), NextUnlocksConfig.XpEffect.SPARKS);
		row(Skill.HERBLORE, NextUnlocksConfig.CardStyle.GLASS, new Color(20, 80, 30, 120), new Color(120, 220, 140, 140), rgb(0x9cf0a8), rgb(0x30b048), NextUnlocksConfig.XpEffect.BUBBLES);
		row(Skill.AGILITY, NextUnlocksConfig.CardStyle.MODERN, rgb(0x101a2a), null, rgb(0x60a0ff), rgb(0x3070e0), NextUnlocksConfig.XpEffect.BOLTS);
		row(Skill.THIEVING, NextUnlocksConfig.CardStyle.MODERN, rgb(0x1a1020), null, rgb(0xc080ff), rgb(0x8040c0), NextUnlocksConfig.XpEffect.COINS);
		row(Skill.FARMING, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0x2e7a1e), rgb(0x3e9a2a), NextUnlocksConfig.XpEffect.LEAVES);
		ROWS.put(Skill.RUNECRAFT, new Row(NextUnlocksConfig.CardStyle.ARCADE, rgb(0x0c0a1a), rgb(0xe0c060), rgb(0x8060ff),
			rgb(0xffd860), rgb(0xa070ff), NextUnlocksConfig.XpEffect.RUNES));
		row(Skill.HUNTER, NextUnlocksConfig.CardStyle.PARCHMENT, rgb(0xd8c89a), null, rgb(0x6b4a23), rgb(0x8b6a33), NextUnlocksConfig.XpEffect.LEAVES);
		row(Skill.CONSTRUCTION, NextUnlocksConfig.CardStyle.PARCHMENT, null, null, rgb(0x7a4a1a), rgb(0xa06a30), NextUnlocksConfig.XpEffect.PIXELS);
		row(Skill.SAILING, NextUnlocksConfig.CardStyle.GLASS, new Color(30, 100, 150, 120), new Color(170, 225, 255, 140), rgb(0xffe9a8), rgb(0x2e9ac4), NextUnlocksConfig.XpEffect.BUBBLES);

		CardTheme.Frame notched = CardTheme.Frame.NOTCHED;
		CardTheme.Frame riveted = CardTheme.Frame.RIVETED;
		CardTheme.Frame brackets = CardTheme.Frame.BRACKETS;
		CardTheme.Frame ornate = CardTheme.Frame.ORNATE;
		CardTheme.Frame pixel = CardTheme.Frame.PIXEL;
		CardTheme.Frame dashed = CardTheme.Frame.DASHED;
		NextUnlocksConfig.Ambient embers = NextUnlocksConfig.Ambient.EMBERS;
		NextUnlocksConfig.Ambient bubbles = NextUnlocksConfig.Ambient.BUBBLES;
		NextUnlocksConfig.Ambient fireflies = NextUnlocksConfig.Ambient.FIREFLIES;
		NextUnlocksConfig.Ambient sparkles = NextUnlocksConfig.Ambient.SPARKLES;
		NextUnlocksConfig.Ambient leaves = NextUnlocksConfig.Ambient.LEAVES;
		NextUnlocksConfig.Ambient runes = NextUnlocksConfig.Ambient.RUNES;
		NextUnlocksConfig.Ambient dust = NextUnlocksConfig.Ambient.DUST;
		NextUnlocksConfig.BorderMotion pulse = NextUnlocksConfig.BorderMotion.PULSE;
		NextUnlocksConfig.BorderMotion runner = NextUnlocksConfig.BorderMotion.RUNNER;
		NextUnlocksConfig.BorderMotion march = NextUnlocksConfig.BorderMotion.MARCH;
		NextUnlocksConfig.BorderMotion flicker = NextUnlocksConfig.BorderMotion.FLICKER;
		fx(Skill.ATTACK, notched, null, pulse);
		fx(Skill.STRENGTH, riveted, null, pulse);
		fx(Skill.DEFENCE, riveted, null, runner);
		fx(Skill.RANGED, brackets, leaves, null);
		fx(Skill.PRAYER, ornate, sparkles, pulse);
		fx(Skill.HITPOINTS, null, null, pulse);
		fx(Skill.COOKING, null, embers, flicker);
		fx(Skill.WOODCUTTING, notched, leaves, null);
		fx(Skill.FLETCHING, brackets, dust, null);
		fx(Skill.FISHING, null, bubbles, null);
		fx(Skill.FIREMAKING, null, embers, flicker);
		fx(Skill.CRAFTING, ornate, sparkles, runner);
		fx(Skill.SMITHING, riveted, embers, flicker);
		fx(Skill.MINING, riveted, dust, null);
		fx(Skill.HERBLORE, null, bubbles, pulse);
		fx(Skill.AGILITY, brackets, null, runner);
		fx(Skill.THIEVING, dashed, dust, march);
		fx(Skill.FARMING, null, fireflies, null);
		fx(Skill.RUNECRAFT, pixel, runes, runner);
		fx(Skill.HUNTER, brackets, fireflies, null);
		fx(Skill.CONSTRUCTION, riveted, dust, null);
		fx(Skill.SAILING, null, bubbles, pulse);
		COMBAT.frame = notched;
		COMBAT.ambient = embers;
		COMBAT.motion = pulse;
	}

	private SkillLooks()
	{
	}

	private static void row(Skill skill, NextUnlocksConfig.CardStyle base, Color background, Color border, Color accent, Color fill,
		NextUnlocksConfig.XpEffect effect)
	{
		ROWS.put(skill, new Row(base, background, border, null, accent, fill, effect));
	}

	private static void fx(Skill skill, CardTheme.Frame frame, NextUnlocksConfig.Ambient ambient,
		NextUnlocksConfig.BorderMotion motion)
	{
		Row row = ROWS.get(skill);
		if (row != null)
		{
			row.frame = frame;
			row.ambient = ambient;
			row.motion = motion;
		}
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
		if (skill == Skill.SLAYER)
		{
			return NextUnlocksConfig.XpEffect.EMBERS;
		}
		Row row = ROWS.get(skill);
		return row == null ? NextUnlocksConfig.XpEffect.SPARKS : row.effect;
	}

	/** What drifts about inside the skill's card, for Skill's own. */
	static NextUnlocksConfig.Ambient ambient(Skill skill)
	{
		if (skill == Skill.MAGIC)
		{
			return NextUnlocksConfig.Ambient.SPARKLES;
		}
		if (skill == Skill.SLAYER)
		{
			return NextUnlocksConfig.Ambient.EMBERS;
		}
		Row row = ROWS.get(skill);
		return row == null || row.ambient == null ? NextUnlocksConfig.Ambient.DUST : row.ambient;
	}

	/** The skill's own card. */
	static CardTheme theme(Skill skill, Palette palette, CardTheme classic)
	{
		if (skill == Skill.MAGIC)
		{
			return magic(palette);
		}
		if (skill == Skill.SLAYER)
		{
			return slayer(palette, classic);
		}
		Row row = ROWS.get(skill);
		if (row == null)
		{
			// a skill newer than this list: the stone card in the skill's color
			Color color = Skills.color(skill);
			row = new Row(NextUnlocksConfig.CardStyle.STONE, null, null, null, CardTheme.shade(color, Color.WHITE, 0.3), color,
				NextUnlocksConfig.XpEffect.SPARKS);
		}
		return theme(row, palette, classic);
	}

	/** The combat level card's own. */
	static CardTheme combat(Palette palette, CardTheme classic)
	{
		return theme(COMBAT, palette, classic);
	}

	static NextUnlocksConfig.Ambient combatAmbient()
	{
		return COMBAT.ambient;
	}

	private static CardTheme theme(Row row, Palette palette, CardTheme classic)
	{
		CardTheme base = CardTheme.preset(row.base, palette, classic, null);
		CardTheme.CardThemeBuilder theme = base.toBuilder()
			.skillName(row.accent)
			.nextUnlockLabel(row.accent)
			.nextUnlockLevel(row.accent)
			.taskTitle(row.accent)
			.barFill(row.fill)
			.xpEffect(row.effect)
			.ambient(row.ambient)
			.borderMotion(row.motion);
		if (row.frame != null)
		{
			theme.frame(row.frame);
		}
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

	/** A scroll gone dark: near-black parchment in a blood-red frame, with red and dark tan writing. */
	private static CardTheme slayer(Palette palette, CardTheme classic)
	{
		Color red = new Color(0xc8282a);
		Color tan = new Color(0xa88a62);
		Color light = new Color(0xd2bc94);
		return CardTheme.preset(NextUnlocksConfig.CardStyle.PARCHMENT, palette, classic, null).toBuilder()
			.background(new Color(0x262322))
			.background2(new Color(0x141212))
			.border(new Color(0x8c1414))
			.innerBorder(new Color(0x4a1010))
			.skillName(red)
			.levelLabel(tan)
			.levelValue(light)
			.xpLabel(tan)
			.xpValue(light)
			.nextLevelLabel(tan)
			.nextLevelValue(light)
			.remainingLabel(tan)
			.remainingValue(light)
			.nextUnlockLabel(tan)
			.nextUnlockLevel(red)
			.nextUnlockXp(light)
			.unlock(light)
			.locked(new Color(0x6a5e52))
			.more(new Color(0x8a7a66))
			.goalLabel(tan)
			.goalLevel(red)
			.goalXp(light)
			.taskTitle(red)
			.met(new Color(0x6aa050))
			.unmet(red)
			.barFill(new Color(0xa01818))
			.barBackground(new Color(0x3a3230))
			.barText(light)
			.xpEffect(NextUnlocksConfig.XpEffect.EMBERS)
			.ambient(NextUnlocksConfig.Ambient.EMBERS)
			.borderMotion(NextUnlocksConfig.BorderMotion.PULSE)
			.build();
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
