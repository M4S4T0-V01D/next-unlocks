package com.nextunlocks;

import java.awt.Color;
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
