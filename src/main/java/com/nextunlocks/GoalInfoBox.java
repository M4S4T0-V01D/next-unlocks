package com.nextunlocks;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.function.ToIntFunction;
import lombok.Getter;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/** XP left to a goal. Rendered on the client thread. */
class GoalInfoBox extends InfoBox
{
	@Getter
	private final Goal goal;
	private final ToIntFunction<Skill> xp;
	private final Color color;

	GoalInfoBox(BufferedImage image, Plugin plugin, Goal goal, ToIntFunction<Skill> xp, Color color)
	{
		super(image, plugin);
		this.goal = goal;
		this.xp = xp;
		this.color = color;
	}

	/** Room for the box's text under the icon, so the xp isn't drawn over it. */
	private static final int SIZE = 32;
	private static final int ICON_HEIGHT = 21;

	/**
	 * The icon shrunk into the top of a square, with the bottom left empty: infoboxes draw their text at the
	 * bottom, so it sits under the icon instead of on it.
	 */
	static BufferedImage withTextRoom(BufferedImage icon)
	{
		BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		if (icon == null || icon.getWidth() <= 0 || icon.getHeight() <= 0)
		{
			return out;
		}
		double scale = Math.min(1, Math.min((double) SIZE / icon.getWidth(), (double) ICON_HEIGHT / icon.getHeight()));
		int w = Math.max(1, (int) Math.round(icon.getWidth() * scale));
		int h = Math.max(1, (int) Math.round(icon.getHeight() * scale));
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(icon, (SIZE - w) / 2, (ICON_HEIGHT - h) / 2, w, h, null);
		g.dispose();
		return out;
	}

	private long xpToGo()
	{
		return Math.max(0, Experience.getXpForLevel(goal.getLevel()) - xp.applyAsInt(goal.getSkill()));
	}

	@Override
	public String getText()
	{
		return NextUnlocksPanel.shortXp(xpToGo());
	}

	@Override
	public Color getTextColor()
	{
		return color;
	}

	@Override
	public String getTooltip()
	{
		return "Goal: " + goal.getText() + "</br>" + goal.getSkill().getName() + " " + goal.getLevel() + " - "
			+ NumberFormat.getIntegerInstance().format(xpToGo()) + " xp to go";
	}
}
