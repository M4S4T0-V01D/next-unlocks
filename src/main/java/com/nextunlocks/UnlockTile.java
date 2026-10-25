package com.nextunlocks;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import net.runelite.client.ui.FontManager;

/** An inventory-style slot: an item or spell image with the unlock level drawn like a stack count. */
class UnlockTile extends JComponent
{
	static final int WIDTH = 36;
	static final int HEIGHT = 32;
	static final Color LEVEL_COLOR = new Color(255, 255, 0);
	private static final Color DIMMED_LEVEL_COLOR = new Color(150, 150, 150);
	private static final AlphaComposite DIMMED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f);

	private final String level;
	private BufferedImage image;
	private Color levelColor = LEVEL_COLOR;
	private boolean dimmed;

	UnlockTile(int level)
	{
		this.level = String.valueOf(level);
		setFont(FontManager.getRunescapeSmallFont());
		Dimension size = new Dimension(WIDTH, HEIGHT);
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
	}

	/** EDT. */
	void setImage(BufferedImage image)
	{
		this.image = image;
		repaint();
	}

	/** Greys the tile out, for an unlock that can't be used yet. */
	void setDimmed(boolean dimmed)
	{
		this.dimmed = dimmed;
		repaint();
	}

	void setLevelColor(Color levelColor)
	{
		this.levelColor = levelColor;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		if (isOpaque())
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.setFont(getFont());
		paint(g2, image, level, levelColor, dimmed, getWidth(), getHeight(), 1);
	}

	/**
	 * Draws the image, scaled, centered in a width x height slot at the origin, with the level in its
	 * top-left corner in the graphics' font. A dimmed tile has a faded image and a grey level.
	 */
	static void paint(Graphics2D g, BufferedImage image, String level, Color levelColor, boolean dimmed, int width, int height, double scale)
	{
		if (image != null)
		{
			int w = (int) Math.round(image.getWidth() * scale);
			int h = (int) Math.round(image.getHeight() * scale);
			Composite composite = g.getComposite();
			if (dimmed)
			{
				g.setComposite(DIMMED);
			}
			g.drawImage(image, (width - w) / 2, (height - h) / 2, w, h, null);
			g.setComposite(composite);
		}
		FontMetrics fm = g.getFontMetrics();
		int y = fm.getAscent() - 1;
		g.setColor(Color.BLACK);
		g.drawString(level, 2, y + 1);
		g.setColor(dimmed ? DIMMED_LEVEL_COLOR : levelColor);
		g.drawString(level, 1, y);
	}
}
