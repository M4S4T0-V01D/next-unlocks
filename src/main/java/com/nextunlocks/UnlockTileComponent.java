package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/** {@link UnlockTile} for overlays, such as the skills-tab tooltip. */
class UnlockTileComponent implements LayoutableRenderableEntity
{
	private final BufferedImage image;
	private final String level;
	private final Color levelColor;
	private final boolean dimmed;
	private final double scale;
	private final int width;
	private final int height;
	private final Rectangle bounds = new Rectangle();
	private Point preferredLocation = new Point();

	/** The image may still be loading (an item's AsyncBufferedImage); it's read on every render. */
	UnlockTileComponent(BufferedImage image, int level, Color levelColor, boolean dimmed, double scale)
	{
		this.image = image;
		this.level = String.valueOf(level);
		this.levelColor = levelColor;
		this.dimmed = dimmed;
		this.scale = scale;
		width = size(UnlockTile.WIDTH, scale);
		height = size(UnlockTile.HEIGHT, scale);
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
		UnlockTile.paint(graphics, image, level, levelColor, dimmed, width, height, scale);
		graphics.setFont(font);
		graphics.translate(-preferredLocation.x, -preferredLocation.y);
		bounds.setLocation(preferredLocation);
		bounds.setSize(width, height);
		return bounds.getSize();
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
