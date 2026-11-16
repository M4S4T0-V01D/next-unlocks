package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import lombok.Getter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/** {@link UnlockTile} for overlays, such as the skills-tab tooltip. */
class UnlockTileComponent implements LayoutableRenderableEntity
{
	/** Room around the icon inside a slot, each side, before scaling. */
	static final int SLOT_PADDING = 2;
	private static final Color SLOT_SHADE = new Color(0, 0, 0, 90);
	private static final Color SLOT_LIGHT = new Color(255, 255, 255, 28);

	private final BufferedImage image;
	/** What the icon unlocks, shown when it's hovered on a pinned card. */
	@Getter
	private final String name;
	/** The level drawn in the corner, or null for none. */
	private final String level;
	private final Color levelColor;
	private final boolean dimmed;
	private final double scale;
	private int width;
	private int height;
	/** The slot drawn behind the icon on pinned cards, or null for the bare icon. */
	private Color slotFill;
	private Color slotEdge;
	private int slotCorner;
	private final Rectangle bounds = new Rectangle();
	private Point preferredLocation = new Point();

	/** The image may still be loading (an item's AsyncBufferedImage); it's read on every render. */
	UnlockTileComponent(BufferedImage image, String name, Integer level, Color levelColor, boolean dimmed, double scale)
	{
		this.image = image;
		this.name = name;
		this.level = level == null ? null : String.valueOf(level);
		this.levelColor = levelColor;
		this.dimmed = dimmed;
		this.scale = scale;
		width = width(scale, false);
		height = height(scale, false);
	}

	/** Sit the icon in a slot, like an inventory's, in the card's colors. The edge may be null. */
	UnlockTileComponent slot(Color fill, Color edge, int corner)
	{
		slotFill = fill;
		slotEdge = edge;
		slotCorner = corner;
		width = width(scale, true);
		height = height(scale, true);
		return this;
	}

	static int width(double scale, boolean slot)
	{
		return size(UnlockTile.WIDTH + (slot ? 2 * SLOT_PADDING : 0), scale);
	}

	static int height(double scale, boolean slot)
	{
		return size(UnlockTile.HEIGHT + (slot ? 2 * SLOT_PADDING : 0), scale);
	}

	static int size(int base, double scale)
	{
		return (int) Math.round(base * scale);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.translate(preferredLocation.x, preferredLocation.y);
		Font font = graphics.getFont();
		Font small = FontManager.getRunescapeSmallFont();
		graphics.setFont(scale == 1 ? small : small.deriveFont((float) Math.max(8, Math.round(small.getSize2D() * scale))));
		if (slotFill != null)
		{
			paintSlot(graphics);
			int inset = size(SLOT_PADDING, scale);
			graphics.translate(inset, inset);
			UnlockTile.paint(graphics, image, level, levelColor, dimmed, width - 2 * inset, height - 2 * inset, scale);
			graphics.translate(-inset, -inset);
		}
		else
		{
			UnlockTile.paint(graphics, image, level, levelColor, dimmed, width, height, scale);
		}
		graphics.setFont(font);
		graphics.translate(-preferredLocation.x, -preferredLocation.y);
		bounds.setLocation(preferredLocation);
		bounds.setSize(width, height);
		return bounds.getSize();
	}

	/** A sunken slot: the fill, a shade along the top and left, a glint along the bottom and right, then the edge. */
	private void paintSlot(Graphics2D g)
	{
		Object antialias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double arc = slotCorner * 2;
		g.setColor(slotFill);
		g.fill(new RoundRectangle2D.Double(0, 0, width, height, arc, arc));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		int c = slotCorner / 2;
		g.setColor(SLOT_SHADE);
		g.drawLine(1 + c, 1, width - 2 - c, 1);
		g.drawLine(1, 1 + c, 1, height - 2 - c);
		g.setColor(SLOT_LIGHT);
		g.drawLine(1 + c, height - 2, width - 2 - c, height - 2);
		g.drawLine(width - 2, 1 + c, width - 2, height - 2 - c);
		if (slotEdge != null)
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(slotEdge);
			g.draw(new RoundRectangle2D.Double(0.5, 0.5, width - 1, height - 1, arc, arc));
		}
		if (antialias != null)
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
		}
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		preferredLocation = position;
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		// fixed size
	}
}
