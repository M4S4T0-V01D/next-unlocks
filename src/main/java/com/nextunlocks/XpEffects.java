package com.nextunlocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.Locale;
import java.util.Random;
import lombok.Value;
import net.runelite.client.ui.FontManager;

/**
 * What a pinned card does as its skill gains xp: particles off the end of the bar, the xp floating up, the bar lighting
 * up. Like the level-up burst, every frame works each particle's path out again from the gain, so nothing is kept.
 */
final class XpEffects
{
	/** How long a gain shows for; the plugin forgets gains older than this. */
	static final long MS = 1500;
	/** Gains closer together than this add up, so fast xp drops make one steady show. */
	static final long MERGE_MS = 600;
	private static final long PULSE_MS = 450;
	private static final long POPUP_MS = 1300;

	private static final Color[] SPARKS = {
		new Color(255, 250, 200), new Color(255, 220, 90), new Color(255, 170, 40), Color.WHITE,
	};
	private static final Color[] EMBERS = {
		new Color(255, 200, 60), new Color(255, 130, 30), new Color(230, 70, 20), new Color(255, 230, 140),
	};
	private static final Color[] BUBBLES = {
		new Color(200, 240, 255), new Color(150, 210, 255), Color.WHITE,
	};
	private static final Color[] LEAVES = {
		new Color(90, 170, 60), new Color(140, 200, 70), new Color(60, 130, 40), new Color(200, 180, 60),
	};
	private static final Color[] HEARTS = {
		new Color(255, 60, 80), new Color(220, 20, 50), new Color(255, 120, 140),
	};
	private static final Color[] COINS = {
		new Color(255, 215, 60), new Color(230, 180, 30), new Color(255, 240, 140),
	};
	private static final Color[] SMOKE = {
		new Color(190, 190, 190, 150), new Color(150, 150, 150, 150), new Color(220, 220, 220, 130),
	};
	private static final Color[] BOLTS = {
		new Color(200, 235, 255), new Color(140, 200, 255), Color.WHITE, new Color(255, 250, 170),
	};
	/** A little heart, row by row. */
	private static final String[] HEART = {
		".X.X.",
		"XXXXX",
		".XXX.",
		"..X..",
	};

	/** The xp a skill just gained, added up over gains that came close together. */
	@Value
	static class Gain
	{
		/** When the latest of the added-up gains came in. */
		long at;
		int amount;

		/** This gain with another one added, or a fresh one when the last was too long ago. */
		Gain plus(int xp, long now)
		{
			return now - at < MERGE_MS ? new Gain(now, amount + xp) : new Gain(now, xp);
		}
	}

	private XpEffects()
	{
	}

	/** Draw a gain's show around the bar; after every card, like the level-up bursts. */
	static void render(Graphics2D graphics, CardTheme theme, PinBar bar, Gain gain)
	{
		Rectangle bounds = bar.getBounds();
		if (gain == null || bounds.width <= 0)
		{
			return;
		}
		long now = System.currentTimeMillis();
		long since = now - gain.getAt();
		if (since < 0 || since >= MS)
		{
			return;
		}
		Object antialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Color oldColor = graphics.getColor();
		Stroke oldStroke = graphics.getStroke();
		Font oldFont = graphics.getFont();

		if (theme.isXpPulse() && since < PULSE_MS)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			double t = since / (double) PULSE_MS;
			graphics.setColor(new Color(255, 255, 255, (int) (110 * (1 - t) * (1 - t))));
			graphics.fillRect(bounds.x, bounds.y, (int) Math.round(bounds.width * Math.max(bar.progress(), 0.02)), bounds.height);
		}
		if (theme.getXpEffect() != null && theme.getXpEffect() != NextUnlocksConfig.XpEffect.OFF)
		{
			particles(graphics, theme, bounds, bar.progress(), gain, since);
		}
		if (theme.isXpPopup() && since < POPUP_MS)
		{
			popup(graphics, theme, bounds, gain, since);
		}

		graphics.setFont(oldFont);
		graphics.setStroke(oldStroke);
		graphics.setColor(oldColor);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
	}

	private static void particles(Graphics2D graphics, CardTheme theme, Rectangle bounds, double progress, Gain gain, long since)
	{
		NextUnlocksConfig.XpEffect effect = theme.getXpEffect();
		boolean smooth = effect == NextUnlocksConfig.XpEffect.BUBBLES || effect == NextUnlocksConfig.XpEffect.STARS
			|| effect == NextUnlocksConfig.XpEffect.LEAVES || effect == NextUnlocksConfig.XpEffect.SMOKE;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			smooth ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
		Color[] colors = colors(theme, effect);
		// out of the end of the fill, where the xp just went
		double edge = bounds.x + bounds.width * Math.max(0, Math.min(1, progress));
		double t = since / 1000.0;
		int count = count(effect, gain.getAmount());
		for (int i = 0; i < count; i++)
		{
			Random random = new Random(seed(gain.getAt() * 131 + i * 7919L));
			double delay = random.nextDouble() * 0.25;
			double life = 0.6 + random.nextDouble() * 0.6;
			double age = t - delay;
			if (age < 0 || age > life)
			{
				continue;
			}
			double left = 1 - age / life;
			double sx = edge + (random.nextDouble() * 2 - 1) * Math.min(14, bounds.width / 6.0);
			double sy = bounds.y + random.nextDouble() * bounds.height;
			Color c = colors[random.nextInt(colors.length)];
			int alpha = (int) (255 * Math.min(1, left / 0.35));
			Color color = new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() * alpha / 255);
			graphics.setColor(color);
			switch (effect)
			{
				case SPARKS:
				{
					double vx = (random.nextDouble() * 2 - 1) * 90;
					double vy = -(60 + random.nextDouble() * 110);
					double x = sx + vx * age;
					double y = sy + vy * age + 0.5 * 420 * age * age;
					// a short streak along its path
					double dx = vx * 0.025;
					double dy = (vy + 420 * age) * 0.025;
					graphics.setStroke(new BasicStroke(1));
					graphics.drawLine((int) x, (int) y, (int) (x - dx), (int) (y - dy));
					break;
				}
				case EMBERS:
				{
					double x = sx + Math.sin(age * 6 + i) * 4 + (random.nextDouble() * 2 - 1) * 12 * age;
					double y = sy - (20 + random.nextDouble() * 30) * age;
					// flickers as it cools
					if (left < 0.5 && ((System.currentTimeMillis() / 60 + i) & 1) == 0)
					{
						break;
					}
					int size = left > 0.6 && random.nextBoolean() ? 2 : 1;
					graphics.fillRect((int) x, (int) y, size, size);
					break;
				}
				case BUBBLES:
				{
					double x = sx + Math.sin(age * 9 + i * 1.7) * 2.5;
					double y = sy - (18 + random.nextDouble() * 26) * age;
					int size = 2 + random.nextInt(3);
					graphics.setStroke(new BasicStroke(1));
					graphics.drawOval((int) x, (int) y, size, size);
					if (left < 0.15)
					{
						// pops
						graphics.drawLine((int) x - 1, (int) y + size / 2, (int) x + size + 1, (int) y + size / 2);
					}
					break;
				}
				case LEAVES:
				{
					double vx = (random.nextDouble() * 2 - 1) * 40;
					double vy = -(40 + random.nextDouble() * 40);
					double x = sx + vx * age + Math.sin(age * 7 + i) * 4;
					double y = sy + vy * age + 0.5 * 160 * age * age;
					double turn = age * (3 + random.nextDouble() * 4) + i;
					Graphics2D g = (Graphics2D) graphics.create();
					g.translate(x, y);
					g.rotate(turn);
					g.fillOval(-2, -1, 4, 2);
					g.dispose();
					break;
				}
				case STARS:
				{
					double x = sx + (random.nextDouble() * 2 - 1) * 14;
					double y = sy - 4 - random.nextDouble() * 14 - 6 * age;
					// grows in, then shrinks out
					double grow = Math.sin(Math.PI * age / life);
					int arm = (int) Math.round(1 + grow * (1 + random.nextInt(3)));
					graphics.setStroke(new BasicStroke(1));
					graphics.drawLine((int) x - arm, (int) y, (int) x + arm, (int) y);
					graphics.drawLine((int) x, (int) y - arm, (int) x, (int) y + arm);
					break;
				}
				case RUNES:
				{
					double x = sx + (random.nextDouble() * 2 - 1) * 10 * age;
					double y = sy - (16 + random.nextDouble() * 22) * age;
					// a little rune stone: a square with a mark on it
					graphics.setStroke(new BasicStroke(1));
					graphics.drawRect((int) x - 2, (int) y - 2, 4, 4);
					graphics.fillRect((int) x, (int) y - 1, 1, 2);
					break;
				}
				case HEARTS:
				{
					double x = sx + Math.sin(age * 5 + i) * 3;
					double y = sy - 4 - (18 + random.nextDouble() * 24) * age;
					heart(graphics, (int) x - 2, (int) y - 2);
					break;
				}
				case COINS:
				{
					double vx = (random.nextDouble() * 2 - 1) * 50;
					double vy = -(70 + random.nextDouble() * 70);
					double x = sx + vx * age;
					double y = sy + vy * age + 0.5 * 380 * age * age;
					// turning over as it flies: narrow, wide, narrow
					int w = Math.max(1, (int) Math.round(3 * Math.abs(Math.cos(age * 12 + i))));
					graphics.fillRect((int) x - w / 2, (int) y - 1, w, 3);
					if (w > 1)
					{
						graphics.setColor(new Color(255, 250, 210, color.getAlpha()));
						graphics.fillRect((int) x - w / 2, (int) y - 1, 1, 1);
					}
					break;
				}
				case SMOKE:
				{
					double x = sx + Math.sin(age * 3 + i) * 3 + 8 * age;
					double y = sy - (14 + random.nextDouble() * 16) * age;
					// spreads out and thins as it rises
					double size = 2 + 6 * age / life;
					graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * left * 0.8)));
					graphics.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
					break;
				}
				case BOLTS:
				{
					// a quick crooked flash off the bar, gone before it moves
					if (age > life * 0.35)
					{
						break;
					}
					graphics.setStroke(new BasicStroke(1));
					double angle = -Math.PI / 2 + (random.nextDouble() * 2 - 1) * 1.1;
					double x = sx;
					double y = sy;
					int steps = 3 + random.nextInt(3);
					for (int s = 0; s < steps; s++)
					{
						double reach = 3 + random.nextDouble() * 4;
						double turn = angle + (random.nextDouble() * 2 - 1) * 0.9;
						double nx = x + Math.cos(turn) * reach;
						double ny = y + Math.sin(turn) * reach;
						graphics.drawLine((int) x, (int) y, (int) nx, (int) ny);
						x = nx;
						y = ny;
					}
					break;
				}
				case PIXELS:
				default:
				{
					double vx = (random.nextDouble() * 2 - 1) * 60;
					double vy = -(40 + random.nextDouble() * 90);
					int x = (int) Math.round(sx + vx * age);
					int y = (int) Math.round(sy + vy * age + 0.5 * 300 * age * age);
					int size = random.nextInt(3) == 0 ? 1 : 2;
					graphics.fillRect(x, y, size, size);
					break;
				}
			}
		}
	}

	/** Bigger gains make more of a show, up to a point. */
	static int count(NextUnlocksConfig.XpEffect effect, int amount)
	{
		int base = effect == NextUnlocksConfig.XpEffect.BUBBLES || effect == NextUnlocksConfig.XpEffect.RUNES
			|| effect == NextUnlocksConfig.XpEffect.LEAVES || effect == NextUnlocksConfig.XpEffect.HEARTS
			|| effect == NextUnlocksConfig.XpEffect.SMOKE || effect == NextUnlocksConfig.XpEffect.BOLTS ? 5 : 9;
		int extra = (int) Math.round(Math.log10(Math.max(1, amount)) * 3);
		return Math.min(base * 2 + 6, base + extra);
	}

	/** The fixed colors of fire, water, leaves and the like; stars, runes and pixels take the card's own. */
	static Color[] colors(CardTheme theme, NextUnlocksConfig.XpEffect effect)
	{
		switch (effect)
		{
			case SPARKS:
				return SPARKS;
			case EMBERS:
				return EMBERS;
			case BUBBLES:
				return BUBBLES;
			case LEAVES:
				return LEAVES;
			case HEARTS:
				return HEARTS;
			case COINS:
				return COINS;
			case SMOKE:
				return SMOKE;
			case BOLTS:
				return BOLTS;
			case PIXELS:
				if (theme.getBurst() != null)
				{
					return theme.getBurst();
				}
				return new Color[]{theme.getBarFill(), theme.getSkillName(), Color.WHITE};
			case STARS:
			case RUNES:
			default:
				return new Color[]{theme.getSkillName(), theme.getBarFill(), Color.WHITE};
		}
	}

	private static void popup(Graphics2D graphics, CardTheme theme, Rectangle bounds, Gain gain, long since)
	{
		double t = since / (double) POPUP_MS;
		String text = String.format(Locale.ROOT, "+%,d xp", gain.getAmount());
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int x = bounds.x + bounds.width - metrics.stringWidth(text);
		// eases up off the bar
		int y = bounds.y - 1 - (int) Math.round(16 * (1 - (1 - t) * (1 - t)));
		int alpha = (int) (255 * Math.min(1, (1 - t) / 0.4));
		Color c = theme.getXpValue();
		graphics.setColor(new Color(0, 0, 0, alpha));
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
		graphics.drawString(text, x, y);
	}

	private static void heart(Graphics2D graphics, int x, int y)
	{
		for (int row = 0; row < HEART.length; row++)
		{
			for (int col = 0; col < HEART[row].length(); col++)
			{
				if (HEART[row].charAt(col) == 'X')
				{
					graphics.fillRect(x + col, y + row, 1, 1);
				}
			}
		}
	}

	private static long seed(long h)
	{
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		return h;
	}
}
