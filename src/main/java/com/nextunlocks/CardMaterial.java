package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/**
 * The line under an item goal's making level saying what the level is for: a tiny icon of the material, then its name
 * in smaller text, cut short with an ellipsis rather than wrapped.
 */
class CardMaterial implements LayoutableRenderableEntity
{
	private static final int INDENT = 6;
	private static final int GAP = 3;
	private static final String ELLIPSIS = "...";

	private final CardTheme theme;
	private final Font font;
	/** Null for none; an item image can still be loading, and is drawn as it is when painted. */
	private final BufferedImage icon;
	private final String text;
	private final Color color;
	private Point location = new Point();
	private Dimension preferred = new Dimension();
	private final Rectangle bounds = new Rectangle();
	/** The blurred text behind it, kept until the width changes. */
	private BufferedImage glow;
	private int glowWidth;

	CardMaterial(CardTheme theme, Font font, BufferedImage icon, String text, Color color)
	{
		this.theme = theme;
		this.font = font;
		this.icon = icon;
		this.text = text;
		this.color = color;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Font previous = graphics.getFont();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		Object interpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		graphics.setFont(font);
		if (theme.smooth())
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		FontMetrics metrics = graphics.getFontMetrics();
		int height = metrics.getHeight();
		int x = location.x + INDENT;
		int width = preferred.width;

		// the icon as tall as the text, keeping the item picture's shape
		int iconHeight = icon == null || icon.getHeight() <= 0 ? 0 : height;
		int iconWidth = iconHeight == 0 ? 0 : Math.max(1, icon.getWidth() * iconHeight / icon.getHeight());
		if (iconWidth > 0)
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.drawImage(icon, x, location.y, iconWidth, iconHeight, null);
			if (interpolation != null)
			{
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			}
			x += iconWidth + GAP;
		}
		int room = Math.max(1, location.x + width - x);
		String shown = fit(metrics, text, room);
		int baseline = location.y + (height + metrics.getAscent() - metrics.getDescent()) / 2;

		if (theme.isGlowText() && theme.getGlow() != null)
		{
			if (glow == null || glowWidth != width)
			{
				glow = Glow.paint(room, height, theme.getGlowSize(), g ->
				{
					g.setFont(font);
					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g.setColor(theme.getGlow());
					g.drawString(shown, 0, baseline - location.y);
				});
				glowWidth = width;
			}
			int pad = Glow.pad(theme.getGlowSize());
			graphics.drawImage(glow, x - pad, location.y - pad, null);
		}
		Color old = graphics.getColor();
		if (theme.isShadow())
		{
			graphics.setColor(Color.BLACK);
			graphics.drawString(shown, x + 1, baseline + 1);
		}
		graphics.setColor(color);
		graphics.drawString(shown, x, baseline);
		graphics.setColor(old);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
		graphics.setFont(previous);

		bounds.setLocation(location);
		bounds.setSize(width, height);
		return new Dimension(width, height);
	}

	/** The text, or as much of it as fits with an ellipsis after it. */
	static String fit(FontMetrics metrics, String text, int width)
	{
		if (metrics.stringWidth(text) <= width)
		{
			return text;
		}
		int end = text.length();
		while (end > 0 && metrics.stringWidth(text.substring(0, end).trim() + ELLIPSIS) > width)
		{
			end--;
		}
		return text.substring(0, end).trim() + ELLIPSIS;
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		location = position;
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		preferred = dimension;
	}
}
