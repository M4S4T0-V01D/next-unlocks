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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/**
 * A line of a card, laid out like RuneLite's LineComponent: text on the left, text on the right. Each side is made of
 * parts with their own color, so "XP:" and the number after it can differ, and the theme decides shadow and glow.
 */
class CardLine implements LayoutableRenderableEntity
{
	/** Room kept between the two sides. */
	private static final int GAP = 4;

	private static final class Part
	{
		private String text;
		private final Color color;

		private Part(String text, Color color)
		{
			this.text = text;
			this.color = color;
		}
	}

	private final CardTheme theme;
	private final Font font;
	/** The card's title line, which a theme's header band goes behind. */
	@Getter
	private final boolean header;
	private final List<Part> left = new ArrayList<>();
	private final List<Part> right = new ArrayList<>();
	private Point location = new Point();
	private Dimension preferred = new Dimension();
	private final Rectangle bounds = new Rectangle();

	/** The blurred text behind the line, kept until the text or width changes. */
	private BufferedImage glow;
	private String glowKey;

	CardLine(CardTheme theme, Font font)
	{
		this(theme, font, false);
	}

	CardLine(CardTheme theme, Font font, boolean header)
	{
		this.theme = theme;
		this.font = font;
		this.header = header;
	}

	CardLine left(String text, Color color)
	{
		left.add(new Part(text, color));
		return this;
	}

	CardLine right(String text, Color color)
	{
		right.add(new Part(text, color));
		return this;
	}

	/** Change the text of a right-hand part, by its place among them. */
	void setRight(int index, String text)
	{
		right.get(index).text = text;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Font previous = graphics.getFont();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		graphics.setFont(font);
		if (theme.smooth())
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		FontMetrics metrics = graphics.getFontMetrics();
		int lineHeight = metrics.getHeight();
		int width = preferred.width;
		int rightWidth = width(metrics, right);
		int room = right.isEmpty() ? width : width - rightWidth - GAP;

		// a single long left part wraps, like the game's tooltips; the right side stays on the first line
		List<String> wrapped = null;
		if (left.size() == 1 && width(metrics, left) > room)
		{
			wrapped = wrap(metrics, left.get(0).text, Math.max(1, room));
		}
		int lines = wrapped == null ? 1 : wrapped.size();
		int height = lines * lineHeight;
		int x = location.x;
		int baseline = location.y + lineHeight;

		if (theme.isGlowText() && theme.getGlow() != null)
		{
			String key = width + "|" + text(left) + "|" + text(right) + "|" + (wrapped == null ? "" : String.join("\n", wrapped));
			if (glow == null || !key.equals(glowKey))
			{
				List<String> lefts = wrapped;
				glow = Glow.paint(width, height, theme.getGlowSize(), g ->
				{
					g.setFont(font);
					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					// a few copies a pixel apart, so the blur has enough to spread
					for (int[] o : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}})
					{
						paintText(g, metrics, lefts, o[0], lineHeight + o[1], width, rightWidth, theme.getGlow());
					}
				});
				glowKey = key;
			}
			int pad = Glow.pad(theme.getGlowSize());
			graphics.drawImage(glow, x - pad, location.y - pad, null);
		}

		Color color = graphics.getColor();
		if (theme.isShadow())
		{
			paintText(graphics, metrics, wrapped, x + 1, baseline + 1, width, rightWidth, Color.BLACK);
		}
		paintText(graphics, metrics, wrapped, x, baseline, width, rightWidth, null);
		graphics.setColor(color);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
		graphics.setFont(previous);

		bounds.setLocation(location);
		bounds.setSize(width, height);
		return new Dimension(width, height);
	}

	/** Both sides, from a baseline, in each part's color or all in one when a color is given. */
	private void paintText(Graphics2D g, FontMetrics metrics, List<String> wrapped, int x, int baseline, int width, int rightWidth,
		Color only)
	{
		if (only != null)
		{
			g.setColor(only);
		}
		if (wrapped != null)
		{
			Color c = left.get(0).color;
			int y = baseline;
			for (String line : wrapped)
			{
				if (only == null)
				{
					g.setColor(c);
				}
				g.drawString(line, x, y);
				y += metrics.getHeight();
			}
		}
		else
		{
			paintParts(g, metrics, left, x, baseline, only);
		}
		paintParts(g, metrics, right, x + width - rightWidth, baseline, only);
	}

	private static void paintParts(Graphics2D g, FontMetrics metrics, List<Part> parts, int x, int baseline, Color only)
	{
		for (Part part : parts)
		{
			if (only == null && part.color != null)
			{
				g.setColor(part.color);
			}
			g.drawString(part.text, x, baseline);
			x += metrics.stringWidth(part.text);
		}
	}

	private static int width(FontMetrics metrics, List<Part> parts)
	{
		int width = 0;
		for (Part part : parts)
		{
			width += metrics.stringWidth(part.text);
		}
		return width;
	}

	private static String text(List<Part> parts)
	{
		StringBuilder text = new StringBuilder();
		for (Part part : parts)
		{
			text.append(part.text).append('\u0000');
		}
		return text.toString();
	}

	/** Words into lines no wider than the width; a word wider than that gets a line of its own. */
	static List<String> wrap(FontMetrics metrics, String text, int width)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String next = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && metrics.stringWidth(next) > width)
			{
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(next);
			}
		}
		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		return lines;
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
