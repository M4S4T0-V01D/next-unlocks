package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Wraps the tooltip panel so it keeps its own background: RuneLite's tooltip overlay replaces the
 * background of any PanelComponent it is given with the default overlay color. Can draw an outline over the
 * panel's own border, and can be stretched taller than its content so boxes side by side line up.
 */
class TooltipBox implements LayoutableRenderableEntity
{
	private final PanelComponent panel;
	private final Color background;
	/** Null for none. */
	private final Color outline;
	private Point location = new Point();
	/** Panel size from the last render; the panel lays out from its previous frame too. */
	private Dimension last;
	/** Height to stretch to when the content is shorter; 0 for the content's own height. */
	@Setter
	private int minHeight;

	TooltipBox(PanelComponent panel, Color background, Color outline)
	{
		this.panel = panel;
		this.background = background;
		this.outline = outline;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// the background goes on first, sized to fit the stretched height, then the panel's content over it
		if (last != null && last.width > 0)
		{
			paintBackground(graphics, new Rectangle(location, new Dimension(last.width, Math.max(last.height, minHeight))));
		}
		panel.setBackgroundColor(null);
		Dimension size = panel.render(graphics);
		if (last == null && size.width > 0)
		{
			// first frame: nothing to size the background by yet, so it goes over the content's height
			last = size;
			return render(graphics);
		}
		last = size;
		Dimension stretched = new Dimension(size.width, Math.max(size.height, minHeight));
		if (outline != null && stretched.width > 0 && stretched.height > 0)
		{
			Color color = graphics.getColor();
			graphics.setColor(outline);
			graphics.drawRect(location.x, location.y, stretched.width - 1, stretched.height - 1);
			graphics.setColor(color);
		}
		return stretched;
	}

	private void paintBackground(Graphics2D graphics, Rectangle bounds)
	{
		BackgroundComponent back = new BackgroundComponent();
		back.setRectangle(bounds);
		back.setBackgroundColor(background);
		back.render(graphics);
	}

	@Override
	public Rectangle getBounds()
	{
		Rectangle bounds = panel.getBounds();
		return new Rectangle(bounds.x, bounds.y, bounds.width, Math.max(bounds.height, minHeight));
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		location = position;
		panel.setPreferredLocation(position);
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		panel.setPreferredSize(dimension);
	}
}
