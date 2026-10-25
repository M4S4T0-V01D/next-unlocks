package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Wraps the tooltip panel so it keeps its own background: RuneLite's tooltip overlay replaces the
 * background of any PanelComponent it is given with the default overlay color. Can draw an outline over the
 * panel's own border.
 */
class TooltipBox implements LayoutableRenderableEntity
{
	private final PanelComponent panel;
	private final Color background;
	/** Null for none. */
	private final Color outline;

	TooltipBox(PanelComponent panel, Color background, Color outline)
	{
		this.panel = panel;
		this.background = background;
		this.outline = outline;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panel.setBackgroundColor(background);
		Dimension size = panel.render(graphics);
		if (outline != null && size.width > 0 && size.height > 0)
		{
			Rectangle bounds = panel.getBounds();
			Color color = graphics.getColor();
			graphics.setColor(outline);
			graphics.drawRect(bounds.x, bounds.y, size.width - 1, size.height - 1);
			graphics.setColor(color);
		}
		return size;
	}

	@Override
	public Rectangle getBounds()
	{
		return panel.getBounds();
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		panel.setPreferredLocation(position);
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		panel.setPreferredSize(dimension);
	}
}
