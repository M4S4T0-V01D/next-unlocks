package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.PanelComponent;

/** The colors each guide style is drawn in, in the panel and on the in-game cards. */
final class GuideLooks
{
	/** How many upcoming steps a guide card lists. */
	static final int CARD_STEPS = 3;

	private GuideLooks()
	{
	}

	/**
	 * The in-game card for a followed guide: how far along it is, any broken pure caps, and the next steps, with what each
	 * still needs and the cap warnings for quests. Scroll is how many of the steps left to skip, so the mouse wheel can
	 * page through them.
	 */
	static TooltipBox card(GuideProgress progress, CardTheme theme, BufferedImage icon, int textSize, ToIntFunction<Skill> level,
		int scroll)
	{
		Guide guide = progress.getGuide();
		Font regular = FontManager.getRunescapeFont();
		Font font = theme.font(textSize);
		Font small = theme.font(Math.max(NextUnlocksPlugin.MATERIAL_MIN_SIZE, Math.round(textSize * NextUnlocksPlugin.MATERIAL_SIZE)));
		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		lines.add(new CardHeader(icon, guide.getName(), theme.getTaskTitle(), theme.titleFont(textSize, NextUnlocksPlugin.TASK_TITLE_GROWTH),
			(double) textSize / regular.getSize(), theme));
		boolean complete = progress.isComplete();
		lines.add(new CardLine(theme, font)
			.left(complete ? "Guide complete!" : guide.getCategory(), complete ? theme.getMet() : theme.getMore())
			.right(progress.done() + "/" + progress.total() + " steps", complete ? theme.getMet() : theme.getXpValue()));
		lines.add(new PinBar(theme, 0, Math.max(1, progress.total()), progress.done(), theme.getBarFill(), theme.getBarFill2(),
			theme.getBarBackground(), null));
		for (Map.Entry<Skill, Integer> broken : progress.getBroken().entrySet())
		{
			lines.add(new CardLine(theme, font)
				.left(broken.getKey().getName() + " cap broken", theme.getUnmet())
				.right("Lv " + broken.getValue() + ", cap " + guide.getCaps().get(broken.getKey()), theme.getUnmet()));
		}
		List<Guide.Step> left = progress.upcoming(Integer.MAX_VALUE);
		int first = clampScroll(scroll, left.size());
		for (Guide.Step step : left.subList(first, Math.min(left.size(), first + CARD_STEPS)))
		{
			String warning = progress.warning(step);
			lines.add(new CardLine(theme, font)
				.left(step.getTitle(), warning != null ? theme.getUnmet() : theme.getUnlock())
				.right(stepStatus(step, level), theme.getMore()));
			if (warning != null)
			{
				lines.add(new CardMaterial(theme, small, null, warning, NextUnlocksPlugin.materialShade(theme.getUnmet(), theme)));
			}
		}
		if (left.size() > CARD_STEPS)
		{
			lines.add(new CardMaterial(theme, small, null, "Steps " + (first + 1) + "-" + (first + CARD_STEPS) + " of "
				+ left.size() + ", scroll for more", theme.getMore()));
		}

		int width = (int) (ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize() * theme.widthScale(textSize))
			+ 2 * (theme.getPadding() - NextUnlocksPlugin.PANEL_PADDING);
		for (LayoutableRenderableEntity line : lines)
		{
			if (line instanceof CardLine)
			{
				width = Math.max(width, ((CardLine) line).minWidth() + 2 * theme.getPadding());
			}
		}
		card.setPreferredSize(new Dimension(width, 0));
		return new TooltipBox(card, theme, Collections.emptyList());
	}

	/** The steps to skip on a card with this many steps left, kept so the card always shows a full page. */
	static int clampScroll(int scroll, int left)
	{
		return Math.max(0, Math.min(scroll, left - CARD_STEPS));
	}

	/** What a step on a guide card still needs: the level against the target, or what kind of step it is. */
	static String stepStatus(Guide.Step step, ToIntFunction<Skill> level)
	{
		switch (step.getType())
		{
			case LEVEL:
			case UNLOCK:
				return level.applyAsInt(step.getSkill()) + "/" + step.getLevel();
			case QUEST:
				return "Quest";
			case DIARY:
				return "Diary";
			default:
				return "To do";
		}
	}

	/** The style's main color: the panel's accent stripe, and a card's trim. */
	static Color accent(Guide.Style style)
	{
		switch (style)
		{
			case STARTER:
				return new Color(0xd8b25a);
			case IRON:
				return new Color(0xa7adb4);
			case QUESTS:
				return new Color(0x5fa2e0);
			case PURE:
				return new Color(0xd23a32);
			default:
				return new Color(0x6fb24a);
		}
	}

	/** The in-game card's look: a parchment scroll for starters, riveted steel for irons, blood red for pures. */
	static CardTheme theme(Guide.Style style, Palette palette)
	{
		Color accent = accent(style);
		switch (style)
		{
			case STARTER:
				Color ink = new Color(43, 26, 10);
				return CardTheme.colors(new Color(110, 74, 34), ink, new Color(120, 70, 10), palette)
					.frame(CardTheme.Frame.ORNATE)
					.background(new Color(236, 218, 172))
					.background2(new Color(210, 184, 128))
					.border(new Color(150, 110, 30))
					.borderWidth(2)
					.innerBorder(accent)
					.padding(6)
					.opaque(true)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.CLASSIC)
					.more(new Color(120, 96, 64))
					.met(new Color(46, 107, 31))
					.unmet(new Color(160, 16, 16))
					.barFill(new Color(176, 128, 40))
					.barBackground(new Color(196, 172, 120))
					.shimmer(true)
					.glowSize(3)
					.build();
			case IRON:
				return CardTheme.colors(new Color(170, 176, 184), new Color(232, 236, 240), new Color(214, 220, 228), palette)
					.frame(CardTheme.Frame.RIVETED)
					.background(new Color(58, 62, 68, 245))
					.background2(new Color(32, 35, 40, 245))
					.border(new Color(18, 20, 23))
					.borderWidth(2)
					.innerBorder(new Color(120, 126, 134))
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE_BOLD)
					.bar(NextUnlocksConfig.BarStyle.SEGMENTED)
					.more(new Color(150, 156, 164))
					.met(new Color(120, 220, 120))
					.unmet(new Color(240, 110, 90))
					.barFill(accent)
					.barBackground(new Color(28, 30, 34))
					.glowSize(3)
					.build();
			case QUESTS:
				return CardTheme.colors(new Color(150, 180, 220), Color.WHITE, new Color(130, 190, 255), palette)
					.frame(CardTheme.Frame.DOUBLE)
					.background(new Color(14, 26, 48, 240))
					.background2(new Color(8, 14, 28, 240))
					.border(accent)
					.innerBorder(new Color(40, 70, 120))
					.headerBand(new Color(30, 60, 110, 200))
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.more(new Color(130, 150, 180))
					.barFill(accent)
					.barFill2(new Color(170, 220, 255))
					.barBackground(new Color(10, 20, 40))
					.ambient(NextUnlocksConfig.Ambient.RUNES)
					.glowSize(3)
					.build();
			case PURE:
				return CardTheme.colors(new Color(200, 150, 140), new Color(255, 236, 230), new Color(255, 70, 60), palette)
					.frame(CardTheme.Frame.NOTCHED)
					.background(new Color(40, 6, 6, 245))
					.background2(new Color(12, 2, 2, 245))
					.border(accent)
					.borderWidth(2)
					.corner(6)
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE_BOLD)
					.bar(NextUnlocksConfig.BarStyle.CLASSIC)
					.more(new Color(170, 120, 115))
					.met(new Color(120, 220, 120))
					.unmet(new Color(255, 80, 70))
					.barFill(new Color(190, 30, 24))
					.barBackground(new Color(30, 4, 4))
					.ambient(NextUnlocksConfig.Ambient.EMBERS)
					.borderMotion(NextUnlocksConfig.BorderMotion.PULSE)
					.glow(new Color(210, 40, 30, 80))
					.glowBorder(true)
					.glowSize(2)
					.build();
			default:
				Color bark = new Color(92, 62, 34);
				return CardTheme.colors(new Color(170, 200, 150), new Color(236, 246, 226), new Color(140, 220, 100), palette)
					.frame(CardTheme.Frame.DOUBLE)
					.background(new Color(20, 40, 18, 240))
					.background2(new Color(10, 24, 10, 240))
					.border(bark)
					.borderWidth(2)
					.innerBorder(new Color(60, 110, 50))
					.padding(6)
					.shadow(true)
					.font(NextUnlocksConfig.CardFont.RUNESCAPE)
					.bar(NextUnlocksConfig.BarStyle.MODERN)
					.more(new Color(140, 170, 125))
					.barFill(accent)
					.barFill2(new Color(180, 240, 120))
					.barBackground(new Color(14, 28, 12))
					.ambient(NextUnlocksConfig.Ambient.LEAVES)
					.glowSize(3)
					.build();
		}
	}
}
