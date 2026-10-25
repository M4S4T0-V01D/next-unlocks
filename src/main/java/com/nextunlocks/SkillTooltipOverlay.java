package com.nextunlocks;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Adds the next unlock to the tooltip of the skill under the mouse in the skills tab. */
class SkillTooltipOverlay extends Overlay
{
	private final Client client;
	private final NextUnlocksPlugin plugin;
	private final NextUnlocksConfig config;
	private final TooltipManager tooltipManager;

	@Inject
	SkillTooltipOverlay(Client client, NextUnlocksPlugin plugin, NextUnlocksConfig config, TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.skillTooltip())
		{
			return null;
		}
		Skill skill = hoveredSkill();
		if (skill != null)
		{
			LayoutableRenderableEntity tooltip = plugin.tooltip(skill);
			if (tooltip != null)
			{
				tooltipManager.add(new Tooltip(tooltip));
			}
		}
		return null;
	}

	/** The skill under the mouse in the open skills tab, or null. Client thread. */
	Skill hoveredSkill()
	{
		Widget tab = client.getWidget(InterfaceID.Stats.UNIVERSE);
		if (tab == null || tab.isHidden())
		{
			return null;
		}
		Point mouse = client.getMouseCanvasPosition();
		for (Map.Entry<Skill, Integer> e : Skills.STATS_TAB.entrySet())
		{
			Widget w = client.getWidget(e.getValue());
			if (w != null && !w.isHidden() && w.getBounds().contains(mouse.getX(), mouse.getY()))
			{
				return e.getKey();
			}
		}
		return null;
	}
}
