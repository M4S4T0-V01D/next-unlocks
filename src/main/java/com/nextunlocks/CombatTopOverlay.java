package com.nextunlocks;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/** The player's own combat level in a small plate at the top middle of the screen, in the combat level card's look. */
class CombatTopOverlay extends Overlay
{
	private final NextUnlocksPlugin plugin;
	private final NextUnlocksConfig config;
	/** Whether the plate was drawn last frame, for the right-click menu. Client thread. */
	private boolean shown;

	@Inject
	CombatTopOverlay(NextUnlocksPlugin plugin, NextUnlocksConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		shown = false;
		if (!config.combatTop())
		{
			return null;
		}
		LayoutableRenderableEntity plate = plugin.combatPlate();
		if (plate == null)
		{
			return null;
		}
		plate.setPreferredLocation(new Point(0, 0));
		Dimension size = plate.render(graphics);
		if (plate instanceof TooltipBox)
		{
			((TooltipBox) plate).bars().forEach(bar -> bar.renderBurst(graphics));
		}
		shown = true;
		return size;
	}

	/** Whether the plate is under a canvas point. Client thread. */
	boolean contains(int x, int y)
	{
		return shown && getBounds() != null && getBounds().contains(x, y);
	}
}
