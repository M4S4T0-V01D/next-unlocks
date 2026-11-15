package com.nextunlocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Wraps the tooltip panel so it keeps its own background: RuneLite's tooltip overlay replaces the
 * background of any PanelComponent it is given with the default overlay color. Draws the theme's frame, header band,
 * scanlines and glow around the panel, and can be stretched taller than its content so boxes side by side line up.
 */
class TooltipBox implements LayoutableRenderableEntity
{
	private final PanelComponent panel;
	private final CardTheme theme;
	/** The blurred border behind the frame, kept until the size changes. */
	private BufferedImage glow;
	private Dimension glowSize;
	private Point location = new Point();
	/** Panel size from the last render, before stretching; the panel lays out from its previous frame too. */
	@Getter
	private Dimension last;
	/** Height to stretch to when the content is shorter; 0 for the content's own height. */
	@Setter
	private int minHeight;

	/** Rewrite the xp numbers and progress for new xp. */
	private final List<IntConsumer> xpUpdates;

	TooltipBox(PanelComponent panel, CardTheme theme, List<IntConsumer> xpUpdates)
	{
		this.panel = panel;
		this.theme = theme;
		this.xpUpdates = xpUpdates;
		int padding = theme.getPadding();
		panel.setBorder(new Rectangle(padding, padding, padding, padding));
	}

	/** Update the xp shown without rebuilding, so the box keeps its size and place. */
	void updateXp(int xp)
	{
		xpUpdates.forEach(u -> u.accept(xp));
	}

	/** The icon tiles inside, where they were drawn last. */
	List<UnlockTileComponent> tiles()
	{
		List<UnlockTileComponent> tiles = new ArrayList<>();
		collect(panel, tiles);
		return tiles;
	}

	/** The progress bars inside, if any. */
	List<PinBar> bars()
	{
		List<PinBar> bars = new ArrayList<>();
		for (LayoutableRenderableEntity child : panel.getChildren())
		{
			if (child instanceof PinBar)
			{
				bars.add((PinBar) child);
			}
		}
		return bars;
	}

	private static void collect(PanelComponent panel, List<UnlockTileComponent> tiles)
	{
		for (LayoutableRenderableEntity child : panel.getChildren())
		{
			if (child instanceof UnlockTileComponent)
			{
				tiles.add((UnlockTileComponent) child);
			}
			else if (child instanceof PanelComponent)
			{
				collect((PanelComponent) child, tiles);
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// the frame goes on first, sized to fit the stretched height, then the panel's content over it
		if (last != null && last.width > 0)
		{
			paintFrame(graphics, new Rectangle(location, new Dimension(last.width, Math.max(last.height, minHeight))));
		}
		panel.setBackgroundColor(null);
		Dimension size = panel.render(graphics);
		if (last == null && size.width > 0)
		{
			// first frame: nothing to size the frame by yet, so it goes over the content's height
			last = size;
			return render(graphics);
		}
		last = size;
		Dimension stretched = new Dimension(size.width, Math.max(size.height, minHeight));
		if (stretched.width > 0 && stretched.height > 0)
		{
			paintOver(graphics, new Rectangle(location, stretched));
		}
		return stretched;
	}

	/** Glow, background and header band, behind the content. */
	private void paintFrame(Graphics2D graphics, Rectangle box)
	{
		Color color = graphics.getColor();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Color border = theme.getBorder();
		if (theme.isGlowBorder() && theme.getGlow() != null && border != null)
		{
			if (glow == null || !box.getSize().equals(glowSize))
			{
				glow = Glow.paint(box.width, box.height, theme.getGlowSize(), g ->
				{
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(theme.getGlow());
					g.setStroke(new BasicStroke(2));
					g.draw(outline(new Rectangle(0, 0, box.width, box.height)));
				});
				glowSize = box.getSize();
			}
			int pad = Glow.pad(theme.getGlowSize());
			graphics.drawImage(glow, box.x - pad, box.y - pad, null);
		}

		switch (theme.getFrame())
		{
			case CLASSIC:
				BackgroundComponent back = new BackgroundComponent();
				back.setRectangle(box);
				back.setBackgroundColor(theme.getBackground());
				back.render(graphics);
				break;
			case ROUNDED:
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setColor(theme.getBackground());
				graphics.fill(new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, corner(), corner()));
				break;
			default:
				graphics.setColor(theme.getBackground());
				graphics.fillRect(box.x, box.y, box.width, box.height);
				break;
		}
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);

		Rectangle header = header();
		if (theme.getHeaderBand() != null && header != null)
		{
			int top = box.y + 2;
			int bottom = header.y + header.height + 1;
			if (bottom > top)
			{
				graphics.setColor(theme.getHeaderBand());
				graphics.fillRect(box.x + 2, top, box.width - 4, bottom - top);
			}
		}
		graphics.setColor(color);
	}

	/** Scanlines and borders, over the content. */
	private void paintOver(Graphics2D graphics, Rectangle box)
	{
		Color color = graphics.getColor();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Stroke stroke = graphics.getStroke();
		if (theme.isScanlines())
		{
			Shape clip = graphics.getClip();
			if (theme.getFrame() == CardTheme.Frame.ROUNDED)
			{
				graphics.clip(outline(box));
			}
			graphics.setColor(new Color(0, 0, 0, 40));
			for (int y = box.y + 1; y < box.y + box.height; y += 2)
			{
				graphics.drawLine(box.x, y, box.x + box.width - 1, y);
			}
			graphics.setClip(clip);
		}

		Color border = theme.getBorder();
		int right = box.x + box.width - 1;
		int bottom = box.y + box.height - 1;
		switch (theme.getFrame())
		{
			case BEVEL:
				// raised like a 95 window: light top and left, dark bottom and right, twice over
				graphics.setColor(Color.WHITE);
				graphics.drawLine(box.x, box.y, right - 1, box.y);
				graphics.drawLine(box.x, box.y, box.x, bottom - 1);
				graphics.setColor(Color.BLACK);
				graphics.drawLine(box.x, bottom, right, bottom);
				graphics.drawLine(right, box.y, right, bottom);
				graphics.setColor(new Color(223, 223, 223));
				graphics.drawLine(box.x + 1, box.y + 1, right - 2, box.y + 1);
				graphics.drawLine(box.x + 1, box.y + 1, box.x + 1, bottom - 2);
				graphics.setColor(new Color(128, 128, 128));
				graphics.drawLine(box.x + 1, bottom - 1, right - 1, bottom - 1);
				graphics.drawLine(right - 1, box.y + 1, right - 1, bottom - 1);
				break;
			case ROUNDED:
				if (border != null)
				{
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.setColor(border);
					graphics.draw(new RoundRectangle2D.Double(box.x + 0.5, box.y + 0.5, box.width - 1, box.height - 1, corner(), corner()));
				}
				break;
			default:
				if (border != null)
				{
					graphics.setColor(border);
					graphics.drawRect(box.x, box.y, box.width - 1, box.height - 1);
				}
				if (theme.getFrame() == CardTheme.Frame.DOUBLE && theme.getInnerBorder() != null)
				{
					graphics.setColor(theme.getInnerBorder());
					graphics.drawRect(box.x + 2, box.y + 2, box.width - 5, box.height - 5);
				}
				break;
		}
		graphics.setStroke(stroke);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
		graphics.setColor(color);
	}

	private int corner()
	{
		return theme.getCorner() * 2;
	}

	/** The frame's outline shape, for the glow and for clipping. */
	private Shape outline(Rectangle box)
	{
		if (theme.getFrame() == CardTheme.Frame.ROUNDED)
		{
			return new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, corner(), corner());
		}
		return box;
	}

	/** Where the title line was last drawn, if the card starts with one. */
	private Rectangle header()
	{
		List<LayoutableRenderableEntity> children = panel.getChildren();
		if (children.isEmpty())
		{
			return null;
		}
		LayoutableRenderableEntity first = children.get(0);
		if (first instanceof CardHeader || first instanceof CardLine && ((CardLine) first).isHeader())
		{
			Rectangle bounds = first.getBounds();
			return bounds == null || bounds.height == 0 ? null : bounds;
		}
		return null;
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
