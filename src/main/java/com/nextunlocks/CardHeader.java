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
import java.util.List;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/** A pinned quest or diary card's heading: its tab icon, then its name in a bigger font, wrapped to the card's width. */
class CardHeader implements LayoutableRenderableEntity
{
	private static final int GAP = 4;

	/** Null for none. */
	private final BufferedImage icon;
	private final String text;
	private final Color color;
	private final Font font;
	private final double scale;
	private final CardTheme theme;
	private final Rectangle bounds = new Rectangle();
	private Point preferredLocation = new Point();
	private Dimension preferredSize = new Dimension();
	/** The blurred title behind it, kept until the width changes. */
	private BufferedImage glow;
	private int glowWidth;

	CardHeader(BufferedImage icon, String text, Color color, Font font, double scale, CardTheme theme)
	{
		this.theme = theme;
		this.icon = icon;
		this.text = text;
		this.color = color;
		this.font = font;
		this.scale = scale;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int iconWidth = icon == null ? 0 : (int) Math.round(icon.getWidth() * scale);
		int iconHeight = icon == null ? 0 : (int) Math.round(icon.getHeight() * scale);
		int textX = iconWidth == 0 ? 0 : iconWidth + GAP;

		Font previous = graphics.getFont();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		graphics.setFont(font);
		if (theme.smooth())
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		FontMetrics metrics = graphics.getFontMetrics();
		List<String> lines = CardLine.wrap(metrics, text, Math.max(1, preferredSize.width - textX));
		int lineHeight = metrics.getHeight();
		int textHeight = lines.size() * lineHeight;
		int height = Math.max(iconHeight, textHeight);

		int x = preferredLocation.x;
		int y = preferredLocation.y;
		if (icon != null)
		{
			graphics.drawImage(icon, x, y + (height - iconHeight) / 2, iconWidth, iconHeight, null);
		}
		int top = (height - textHeight) / 2 + metrics.getAscent();
		if (theme.isGlowText() && theme.getGlow() != null)
		{
			if (glow == null || glowWidth != preferredSize.width)
			{
				int width = Math.max(1, preferredSize.width - textX);
				glow = Glow.paint(width, height, theme.getGlowSize(), g ->
				{
					g.setFont(font);
					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g.setColor(theme.getGlow());
					for (int[] o : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}})
					{
						paint(g, lines, o[0], top + o[1], lineHeight);
					}
				});
				glowWidth = preferredSize.width;
			}
			int pad = Glow.pad(theme.getGlowSize());
			graphics.drawImage(glow, x + textX - pad, y - pad, null);
		}
		Color old = graphics.getColor();
		if (theme.isShadow())
		{
			graphics.setColor(Color.BLACK);
			paint(graphics, lines, x + textX + 1, y + top + 1, lineHeight);
		}
		graphics.setColor(color);
		paint(graphics, lines, x + textX, y + top, lineHeight);
		graphics.setColor(old);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
		graphics.setFont(previous);

		bounds.setLocation(preferredLocation);
		bounds.setSize(preferredSize.width, height);
		return bounds.getSize();
	}

	private static void paint(Graphics2D graphics, List<String> lines, int x, int baseline, int lineHeight)
	{
		for (String line : lines)
		{
			graphics.drawString(line, x, baseline);
			baseline += lineHeight;
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
		preferredSize = dimension;
	}
}
