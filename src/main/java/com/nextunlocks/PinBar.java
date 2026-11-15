package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.function.Supplier;
import lombok.Setter;
import lombok.Value;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/**
 * The progress bar on cards, in one of a few styles. The arcade one sits still while the unlock is far off, starts to
 * twitch past halfway, and splits into shifting color layers that shake harder as it closes in. With the level-up
 * effect on, a reached unlock holds the bar at a flashing 100%, then it bursts into colored pixels; other level-ups
 * just burst.
 */
class PinBar implements LayoutableRenderableEntity
{
	static final int HEIGHT = 16;
	/** How long a reached unlock shows the full rainbow bar before it bursts. */
	static final long FULL_MS = 1400;
	static final long BURST_MS = 1100;
	/** The jitter moves in steps rather than smoothly, like an old arcade cabinet. */
	private static final long STEP_MS = 45;
	private static final int SEGMENT = 6;
	/** Width of a segmented bar's blocks. */
	private static final int BLOCK = 5;
	/** The classic bar's label, as RuneLite's own progress bar writes it. */
	private static final DecimalFormat PERCENT = new DecimalFormat("0.0");
	private static final float GRAVITY = 300;
	private static final Color[] PIXELS = {
		new Color(255, 64, 64), new Color(255, 160, 32), new Color(255, 236, 64), new Color(96, 255, 96),
		new Color(64, 232, 255), new Color(96, 128, 255), new Color(232, 96, 255), Color.WHITE,
	};

	/** A level-up to show: when it happened, and whether it reached the unlock the bar was filling toward. */
	@Value
	static class LevelUp
	{
		long at;
		boolean unlock;
	}

	private final NextUnlocksConfig.BarStyle style;
	private final double minimum;
	private final double maximum;
	@Setter
	private double value;
	private final Color color;
	private final Color background;
	private final Color textColor;
	private final Color[] pixels;
	/** The skill's latest level-up, or null; null when the effect is off. */
	private final Supplier<LevelUp> levelUp;

	private Point location = new Point();
	private Dimension preferred = new Dimension();
	/** Where the bar was last laid out, unshaken; bursts fly out from here. */
	private Rectangle bounds = new Rectangle();

	/** A bar in a theme's style and colors; the level-up goes unused when the theme has the effect off. */
	PinBar(CardTheme theme, double minimum, double maximum, double value, Color color, Supplier<LevelUp> levelUp)
	{
		this(theme.getBar(), minimum, maximum, value, color, theme.getBarBackground(), theme.getBarText(), theme.getBurst(),
			theme.isLevelUp() ? levelUp : null);
	}

	PinBar(NextUnlocksConfig.BarStyle style, double minimum, double maximum, double value, Color color, Color background,
		Color textColor, Color[] pixels, Supplier<LevelUp> levelUp)
	{
		this.style = style;
		this.textColor = textColor;
		this.pixels = pixels == null ? PIXELS : pixels;
		this.minimum = minimum;
		this.maximum = maximum;
		this.value = value;
		this.color = color;
		this.background = background;
		this.levelUp = levelUp;
	}

	/** 0 to 1. */
	double progress()
	{
		return maximum <= minimum ? 1 : Math.max(0, Math.min(1, (value - minimum) / (maximum - minimum)));
	}

	/** How wild the bar gets: nothing up to halfway, rising faster and faster to 1 at 100%. */
	static double heat(double progress)
	{
		if (progress <= 0.5)
		{
			return 0;
		}
		double t = Math.min(1, (progress - 0.5) / 0.5);
		return t * t;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int width = preferred.width;
		bounds = new Rectangle(location.x, location.y, width, HEIGHT);
		if (width <= 2)
		{
			return new Dimension(width, HEIGHT);
		}

		long now = System.currentTimeMillis();
		LevelUp up = levelUp == null ? null : levelUp.get();
		boolean full = up != null && up.isUnlock() && now - up.getAt() < FULL_MS;
		double progress = full ? 1 : progress();

		Object antialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Object textAntialias = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		Color oldColor = graphics.getColor();
		Paint oldPaint = graphics.getPaint();
		switch (style)
		{
			case ARCADE:
				renderArcade(graphics, width, now, full, progress);
				break;
			case SEGMENTED:
				renderSegmented(graphics, width, now, full, progress);
				break;
			case MODERN:
				renderModern(graphics, width, now, full, progress);
				break;
			default:
				renderClassic(graphics, width, now, full, progress);
				break;
		}
		graphics.setPaint(oldPaint);
		graphics.setColor(oldColor);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialias);
		return new Dimension(width, HEIGHT);
	}

	/** Like RuneLite's own bar; a reached unlock flashes it. */
	private void renderClassic(Graphics2D graphics, int width, long now, boolean full, double progress)
	{
		int x = location.x;
		int y = location.y;
		graphics.setColor(background);
		graphics.fillRect(x, y, width, HEIGHT);
		graphics.setColor(full ? mix(color, Color.WHITE, pulse(now, 300) * 0.6) : color);
		graphics.fillRect(x, y, (int) (width * progress), HEIGHT);
		String text = PERCENT.format(progress * 100) + "%";
		FontMetrics metrics = graphics.getFontMetrics();
		label(graphics, text, x + (width - metrics.stringWidth(text)) / 2,
			y + (HEIGHT - metrics.getHeight()) / 2 + metrics.getHeight(), textColor);
	}

	/** Blocks that light up one by one; the last one blinks near the end, and they chase in color at 100%. */
	private void renderSegmented(Graphics2D graphics, int width, long now, boolean full, double progress)
	{
		int x = location.x;
		int y = location.y;
		long step = now / STEP_MS;
		graphics.setColor(Color.BLACK);
		graphics.fillRect(x, y, width, HEIGHT);
		graphics.setColor(background);
		graphics.fillRect(x + 1, y + 1, width - 2, HEIGHT - 2);
		int inner = width - 4;
		int blocks = Math.max(1, (inner + 1) / (BLOCK + 1));
		int start = x + 2 + (inner - (blocks * (BLOCK + 1) - 1)) / 2;
		int lit = (int) Math.round(progress * blocks);
		for (int i = 0; i < lit; i++)
		{
			Color c = color;
			if (full)
			{
				c = pixels == PIXELS
					? Color.getHSBColor((float) (i / (double) blocks - (now % 900) / 900.0), 0.8f, 1f)
					: pixels[(int) ((i + step / 2) % pixels.length)];
			}
			else if (i == lit - 1 && progress > 0.75 && (step / 4) % 2 == 0)
			{
				c = mix(color, Color.WHITE, 0.6);
			}
			int bx = start + i * (BLOCK + 1);
			graphics.setColor(c);
			graphics.fillRect(bx, y + 2, BLOCK, HEIGHT - 4);
			graphics.setColor(mix(c, Color.WHITE, 0.35));
			graphics.fillRect(bx, y + 2, BLOCK, 1);
		}
		centered(graphics, width, progress, full ? pixels[(int) (step % pixels.length)] : textColor);
	}

	/** A smooth rounded bar with a gradient; a reached unlock makes it pulse. */
	private void renderModern(Graphics2D graphics, int width, long now, boolean full, double progress)
	{
		int x = location.x;
		int y = location.y;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int height = HEIGHT - 2;
		Shape track = new RoundRectangle2D.Double(x, y + 1, width, height, height, height);
		graphics.setColor(background);
		graphics.fill(track);
		int fill = (int) Math.round(width * progress);
		if (fill > 0)
		{
			Color c = full ? mix(color, Color.WHITE, pulse(now, 420) * 0.5) : color;
			graphics.setPaint(new GradientPaint(x, 0, mix(c, Color.BLACK, 0.15), x + Math.max(1, fill), 0, mix(c, Color.WHITE, 0.3)));
			graphics.fill(new RoundRectangle2D.Double(x, y + 1, Math.max(fill, height), height, height, height));
			graphics.setPaint(new Color(255, 255, 255, 46));
			graphics.fill(new RoundRectangle2D.Double(x + 2, y + 2, Math.max(fill, height) - 4, height / 2.0 - 1, height / 2.0, height / 2.0));
		}
		if (full)
		{
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (90 + 140 * pulse(now, 420))));
			graphics.draw(new RoundRectangle2D.Double(x - 1, y, width + 1, height + 1, height + 2, height + 2));
		}
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		centered(graphics, width, progress, textColor);
	}

	/** The arcade cabinet bar: still to halfway, then shaking and splitting into colors more and more. */
	private void renderArcade(Graphics2D graphics, int width, long now, boolean full, double progress)
	{
		double heat = full ? 1 : heat(progress);
		long step = now / STEP_MS;

		// the shake: whole pixels, a new spot each step
		double amplitude = full ? 3.2 : heat > 0.02 ? 0.6 + heat * 1.8 : 0;
		int x = location.x + (int) Math.round(amplitude * noise(step, 1));
		int y = location.y + (int) Math.round(amplitude * 0.7 * noise(step, 2));
		int fill = (int) Math.round((width - 2) * progress);

		if (heat > 0 && fill > 0)
		{
			// color split: magenta and cyan copies pulled apart from the bar, wider as it heats up
			int alpha = (int) (60 + 140 * heat);
			int split = Math.max(1, (int) Math.round((full ? 2.5 : heat * 2) + noise(step, 3)));
			graphics.setColor(new Color(255, 0, 170, alpha));
			graphics.fillRect(x - split, y - (int) Math.round(heat * noise(step, 4)), fill + 2, HEIGHT);
			graphics.setColor(new Color(0, 230, 255, alpha));
			graphics.fillRect(x + split, y + (int) Math.round(heat * noise(step, 5)), fill + 2, HEIGHT);
		}

		// frame and empty part
		graphics.setColor(Color.BLACK);
		graphics.fillRect(x, y, width, HEIGHT);
		graphics.setColor(background);
		graphics.fillRect(x + 1, y + 1, width - 2, HEIGHT - 2);

		// filled part, one column at a time so it can run rainbow
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		double wobble = heat * 0.12 * Math.sin(now / (420.0 - 260 * heat));
		int inner = HEIGHT - 2;
		int band = Math.max(1, inner / 3);
		for (int col = 0; col < fill; col += 2)
		{
			int w = Math.min(2, fill - col);
			Color c;
			if (full)
			{
				c = Color.getHSBColor((float) (col / (double) width - (now % 1040) / 1040.0), 0.85f, 1f);
			}
			else if (heat > 0)
			{
				// the hue runs along the bar and drifts, more as it heats up
				float hue = (float) (hsb[0] + wobble + heat * 0.18 * Math.sin(col / 9.0 - now / 160.0));
				float bright = (float) Math.min(1, hsb[2] + heat * 0.25 * (0.5 + 0.5 * Math.sin(now / 90.0 + col / 5.0)));
				c = Color.getHSBColor(hue, Math.max(hsb[1], 0.55f + 0.3f * (float) heat), bright);
			}
			else
			{
				c = color;
			}
			int cx = x + 1 + col;
			graphics.setColor(c);
			graphics.fillRect(cx, y + 1, w, inner);
			// a lit top and a shaded bottom, for the raised look of an arcade bar
			graphics.setColor(mix(c, Color.WHITE, 0.4));
			graphics.fillRect(cx, y + 1, w, band);
			graphics.setColor(mix(c, Color.BLACK, 0.4));
			graphics.fillRect(cx, y + HEIGHT - 3, w, 2);
		}

		if (fill > 0)
		{
			// segment notches
			graphics.setColor(new Color(0, 0, 0, 70));
			for (int col = SEGMENT; col < fill; col += SEGMENT)
			{
				graphics.fillRect(x + 1 + col, y + 1, 1, inner);
			}
			// a shine that sweeps across, faster as it heats up
			double period = 2200 - 1500 * heat;
			int shine = (int) ((now % (long) period) / period * (fill + 24)) - 12;
			graphics.setColor(new Color(255, 255, 255, (int) (50 + 110 * heat)));
			for (int row = 0; row < inner; row++)
			{
				int sx = shine + row / 2;
				int from = Math.max(0, sx);
				int to = Math.min(fill, sx + 4);
				if (to > from)
				{
					graphics.fillRect(x + 1 + from, y + 1 + row, to - from, 1);
				}
			}
		}

		// soft scanlines over everything
		graphics.setColor(new Color(0, 0, 0, 34));
		for (int row = 2; row < HEIGHT - 1; row += 2)
		{
			graphics.fillRect(x + 1, y + row, width - 2, 1);
		}

		if (heat > 0.15)
		{
			// a glowing outline that cycles through colors
			long cycle = full ? 960 : 2800;
			float hue = (float) ((now % cycle) / (double) cycle);
			Color glow = Color.getHSBColor(hue, 0.9f, 1f);
			graphics.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), (int) (220 * heat)));
			graphics.drawRect(x - 1, y - 1, width + 1, HEIGHT + 1);
		}

		// the percentage, flickering as it heats up
		String text = (int) Math.floor(progress * 100) + "%";
		FontMetrics metrics = graphics.getFontMetrics();
		int tx = x + (width - metrics.stringWidth(text)) / 2;
		int ty = y + (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
		if (full)
		{
			tx += (int) Math.round(noise(step, 6));
			ty += (int) Math.round(noise(step, 7));
		}
		Color c = textColor;
		if (full)
		{
			c = pixels[(int) (step % pixels.length)];
		}
		else if (heat > 0.5 && noise(step, 8) > 1.4 - heat)
		{
			c = new Color(255, 236, 64);
		}
		label(graphics, text, tx, ty, c);
	}

	/** The whole percentage in the middle of the bar. */
	private void centered(Graphics2D graphics, int width, double progress, Color c)
	{
		String text = (int) Math.floor(progress * 100) + "%";
		FontMetrics metrics = graphics.getFontMetrics();
		label(graphics, text, location.x + (width - metrics.stringWidth(text)) / 2,
			location.y + (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent(), c);
	}

	/** Text with a dark shadow, so it reads on any fill. */
	private static void label(Graphics2D graphics, String text, int x, int y, Color c)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(c);
		graphics.drawString(text, x, y);
	}

	/** 0 to 1 and back, over a period. */
	private static double pulse(long now, long period)
	{
		return 0.5 + 0.5 * Math.sin((now % period) / (double) period * Math.PI * 2);
	}

	/**
	 * The burst of pixels after a level-up, if one is going. Drawn after every card so the pixels fly over the
	 * cards next to this one.
	 */
	void renderBurst(Graphics2D graphics)
	{
		LevelUp up = levelUp == null ? null : levelUp.get();
		if (up == null || bounds.width <= 0)
		{
			return;
		}
		long now = System.currentTimeMillis();
		long since = now - up.getAt() - (up.isUnlock() ? FULL_MS : 0);
		if (since < 0 || since >= BURST_MS)
		{
			return;
		}
		double t = since / 1000.0;
		double life = since / (double) BURST_MS;
		long step = now / STEP_MS;

		Object antialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		Color oldColor = graphics.getColor();

		if (since < 120)
		{
			// a white flash over the bar as it pops
			int grow = (int) (since / 30);
			graphics.setColor(new Color(255, 255, 255, (int) (230 * (1 - since / 120.0))));
			graphics.fillRect(bounds.x - grow, bounds.y - grow, bounds.width + grow * 2, bounds.height + grow * 2);
		}

		int count = up.isUnlock() ? 64 : 32;
		double speed = up.isUnlock() ? 1 : 0.7;
		for (int i = 0; i < count; i++)
		{
			// every frame works each pixel's path out again from the same seed, so nothing has to be kept
			Random random = new Random(mix(up.getAt() * 31 + i));
			double sx = bounds.x + random.nextDouble() * bounds.width;
			double sy = bounds.y + random.nextDouble() * bounds.height;
			double vx = (random.nextDouble() * 2 - 1) * 110 * speed;
			double vy = -(50 + random.nextDouble() * 170) * speed;
			int size = random.nextInt(4) == 0 ? 1 : 2 + random.nextInt(2);
			double fade = 0.55 + random.nextDouble() * 0.45;
			if (life > fade)
			{
				continue;
			}
			// near the end of their life the pixels blink out
			if (life > fade * 0.7 && ((step + i) & 1) == 0)
			{
				continue;
			}
			int px = (int) Math.round(sx + vx * t);
			int py = (int) Math.round(sy + vy * t + 0.5 * GRAVITY * t * t);
			Color c = pixels[(int) ((i + step) % pixels.length)];
			int alpha = (int) (255 * Math.min(1, (fade - life) / 0.2));
			graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
			graphics.fillRect(px, py, size, size);
		}

		graphics.setColor(oldColor);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
	}

	private static Color mix(Color a, Color b, double amount)
	{
		return new Color(
			(int) (a.getRed() + (b.getRed() - a.getRed()) * amount),
			(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
			(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
	}

	/** A steady random number from -1 to 1 for a time step, a different one per salt. */
	private static double noise(long step, int salt)
	{
		return (mix(step * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL) & 0xFFFFFF) / (double) 0x800000 - 1;
	}

	/** Scrambles a number, as seeds next to each other give Random alike first values. */
	private static long mix(long h)
	{
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		h *= 0x94D049BB133111EBL;
		return h ^ h >>> 32;
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
