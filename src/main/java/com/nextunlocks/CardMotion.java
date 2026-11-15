package com.nextunlocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * What a card does all the time: things drifting about inside it and a border that moves. Like the xp effects, every
 * frame works it all out again from the clock, so nothing is kept between frames.
 */
final class CardMotion
{
	private static final Color[] EMBERS = {
		new Color(255, 200, 60), new Color(255, 130, 30), new Color(230, 70, 20), new Color(255, 230, 140),
	};
	private static final Color[] BUBBLES = {
		new Color(200, 240, 255), new Color(150, 210, 255), Color.WHITE,
	};
	private static final Color[] LEAVES = {
		new Color(90, 170, 60), new Color(140, 200, 70), new Color(60, 130, 40), new Color(200, 180, 60),
	};
	private static final Color FIREFLY = new Color(220, 255, 120);
	private static final Color DUST = new Color(255, 240, 210);

	private CardMotion()
	{
	}

	/** One particle's place in its loop, from 0 to 1, out of step with the rest. */
	private static double phase(long now, double period, double offset)
	{
		double t = (now / period + offset) % 1;
		return t < 0 ? t + 1 : t;
	}

	private static Color alpha(Color c, double amount)
	{
		int a = (int) Math.max(0, Math.min(255, c.getAlpha() * amount));
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}

	/** Draw the theme's ambient particles inside the outline, behind the text. */
	static void ambient(Graphics2D graphics, CardTheme theme, Shape outline, Rectangle box)
	{
		NextUnlocksConfig.Ambient kind = theme.getAmbient();
		if (kind == null || kind == NextUnlocksConfig.Ambient.OFF || kind == NextUnlocksConfig.Ambient.STYLE
			|| kind == NextUnlocksConfig.Ambient.AUTO || box.width <= 0 || box.height <= 0)
		{
			return;
		}
		long now = System.currentTimeMillis();
		Shape clip = graphics.getClip();
		Color oldColor = graphics.getColor();
		Stroke oldStroke = graphics.getStroke();
		Object antialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.clip(outline);
		boolean smooth = kind == NextUnlocksConfig.Ambient.BUBBLES || kind == NextUnlocksConfig.Ambient.LEAVES;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			smooth ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
		graphics.setStroke(new BasicStroke(1));

		int count = Math.max(6, Math.min(36, box.width * box.height / 1500));
		if (kind == NextUnlocksConfig.Ambient.FIREFLIES || kind == NextUnlocksConfig.Ambient.RUNES)
		{
			count = Math.max(4, count / 2);
		}
		for (int i = 0; i < count; i++)
		{
			Random random = new Random(i * 7919L + kind.ordinal() * 104729L);
			double fx = random.nextDouble();
			double fy = random.nextDouble();
			double speed = 0.7 + random.nextDouble() * 0.6;
			double offset = random.nextDouble();
			double x0 = box.x + fx * box.width;
			double y0 = box.y + fy * box.height;
			switch (kind)
			{
				case EMBERS:
				{
					double t = phase(now, 4200 * speed, offset);
					double x = x0 + Math.sin(t * 7 + i) * 4;
					double y = box.y + box.height - t * box.height * 0.95;
					// flares up from the bottom and flickers out near the top
					double fade = Math.min(1, t * 6) * (1 - t);
					if (t > 0.6 && ((now / 70 + i) & 3) == 0)
					{
						break;
					}
					graphics.setColor(alpha(EMBERS[random.nextInt(EMBERS.length)], 0.85 * fade));
					int size = random.nextInt(3) == 0 ? 2 : 1;
					graphics.fillRect((int) x, (int) y, size, size);
					break;
				}
				case BUBBLES:
				{
					double t = phase(now, 6000 * speed, offset);
					double x = x0 + Math.sin(t * 14 + i * 1.7) * 2.5;
					double y = box.y + box.height + 3 - t * (box.height + 6);
					double size = 2 + random.nextInt(3);
					graphics.setColor(alpha(BUBBLES[random.nextInt(BUBBLES.length)], 0.55 * Math.min(1, (1 - t) * 4)));
					graphics.draw(new Ellipse2D.Double(x, y, size, size));
					break;
				}
				case SNOW:
				{
					double t = phase(now, 9000 * speed, offset);
					double x = x0 + Math.sin(t * Math.PI * 4 + i) * 4;
					double y = box.y - 2 + t * (box.height + 4);
					graphics.setColor(new Color(255, 255, 255, 150 + random.nextInt(60)));
					int size = random.nextInt(3) == 0 ? 2 : 1;
					graphics.fillRect((int) x, (int) y, size, size);
					break;
				}
				case FIREFLIES:
				{
					double t = phase(now, 3200 * speed, offset);
					double x = x0 + Math.sin(now / (1700.0 * speed) + i) * 14;
					double y = y0 + Math.cos(now / (2300.0 * speed) + i * 2) * 8;
					// glows up and dims away, now and then
					double glow = Math.max(0, Math.sin(t * Math.PI * 2));
					glow *= glow;
					if (glow < 0.02)
					{
						break;
					}
					graphics.setColor(alpha(FIREFLY, 0.3 * glow));
					graphics.fillRect((int) x - 1, (int) y - 1, 3, 3);
					graphics.setColor(alpha(FIREFLY, 0.95 * glow));
					graphics.fillRect((int) x, (int) y, 1, 1);
					break;
				}
				case SPARKLES:
				{
					double t = phase(now, 3800 * speed, offset);
					// a twinkle in a quarter of the loop, then dark
					if (t > 0.25)
					{
						break;
					}
					double grow = Math.sin(Math.PI * t / 0.25);
					int arm = (int) Math.round(grow * (1 + random.nextInt(2)));
					Color c = random.nextBoolean() ? Color.WHITE : theme.getSkillName();
					graphics.setColor(alpha(c, 0.9 * grow));
					graphics.fillRect((int) x0, (int) y0, 1, 1);
					if (arm > 0)
					{
						graphics.drawLine((int) x0 - arm, (int) y0, (int) x0 + arm, (int) y0);
						graphics.drawLine((int) x0, (int) y0 - arm, (int) x0, (int) y0 + arm);
					}
					break;
				}
				case LEAVES:
				{
					double t = phase(now, 8000 * speed, offset);
					double x = x0 + Math.sin(t * Math.PI * 3 + i) * 8;
					double y = box.y - 3 + t * (box.height + 6);
					Graphics2D g = (Graphics2D) graphics.create();
					g.setColor(alpha(LEAVES[random.nextInt(LEAVES.length)], 0.7));
					g.translate(x, y);
					g.rotate(Math.sin(t * Math.PI * 4 + i) * 1.2);
					g.fill(new Ellipse2D.Double(-2.5, -1, 5, 2));
					g.dispose();
					break;
				}
				case RUNES:
				{
					double t = phase(now, 7000 * speed, offset);
					double x = x0 + Math.sin(t * 5 + i) * 3;
					double y = box.y + box.height - t * box.height;
					double fade = Math.sin(Math.PI * t);
					graphics.setColor(alpha(random.nextBoolean() ? theme.getSkillName() : theme.getBarFill(), 0.5 * fade));
					graphics.drawRect((int) x - 2, (int) y - 2, 4, 4);
					graphics.fillRect((int) x, (int) y - 1, 1, 2);
					break;
				}
				case DUST:
				default:
				{
					double t = phase(now, 10000 * speed, offset);
					double x = x0 + (t - 0.5) * 24;
					double y = y0 + Math.sin(t * Math.PI * 2 + i) * 4;
					graphics.setColor(alpha(DUST, 0.45 * Math.sin(Math.PI * t)));
					graphics.fillRect((int) x, (int) y, 1, 1);
					break;
				}
			}
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
		graphics.setStroke(oldStroke);
		graphics.setColor(oldColor);
		graphics.setClip(clip);
	}

	/** Move the border: path runs along the border's pixels. */
	static void border(Graphics2D graphics, CardTheme theme, Shape path, Rectangle box)
	{
		NextUnlocksConfig.BorderMotion motion = theme.getBorderMotion();
		if (motion == null || motion == NextUnlocksConfig.BorderMotion.OFF || motion == NextUnlocksConfig.BorderMotion.STYLE)
		{
			return;
		}
		long now = System.currentTimeMillis();
		Color base = theme.getBorder() != null ? theme.getBorder() : theme.getSkillName();
		int width = Math.max(1, theme.getBorderWidth());
		Color oldColor = graphics.getColor();
		Stroke oldStroke = graphics.getStroke();
		Object antialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		// cards further right run a little behind, like the shimmer
		long at = now - box.x * 3L;
		switch (motion)
		{
			case PULSE:
			{
				double beat = 0.5 - 0.5 * Math.cos(Math.PI * 2 * phase(at, 2000, 0));
				graphics.setColor(alpha(CardTheme.shade(base, Color.WHITE, 0.55), 0.85 * beat));
				graphics.setStroke(new BasicStroke(width));
				graphics.draw(path);
				break;
			}
			case FLICKER:
			{
				double f = 0.55 + 0.2 * Math.sin(at / 90.0) + 0.15 * Math.sin(at / 37.0 + 1) + 0.1 * Math.sin(at / 211.0 + 2);
				Color warm = PinBar.mix(base, new Color(255, 200, 90), 0.45);
				graphics.setColor(alpha(CardTheme.shade(warm, Color.WHITE, 0.25), Math.max(0, Math.min(1, f))));
				graphics.setStroke(new BasicStroke(width));
				graphics.draw(path);
				break;
			}
			case MARCH:
			{
				float dash = 4;
				float shift = (float) (phase(at, 800, 0) * dash * 2);
				graphics.setColor(CardTheme.shade(base, Color.WHITE, 0.5));
				graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
					new float[]{dash, dash}, shift));
				graphics.draw(path);
				break;
			}
			case RAINBOW:
			case RUNNER:
			default:
			{
				Track track = new Track(path);
				if (track.length <= 0)
				{
					break;
				}
				int half = width / 2;
				if (motion == NextUnlocksConfig.BorderMotion.RAINBOW)
				{
					double shift = phase(at, 4000, 0);
					for (double s = 0; s < track.length; s += 1)
					{
						double[] p = track.at(s);
						graphics.setColor(Color.getHSBColor((float) (s / track.length + shift), 0.7f, 1f));
						graphics.fillRect((int) Math.round(p[0]) - half, (int) Math.round(p[1]) - half, width, width);
					}
					break;
				}
				// two lights chasing each other round, each with a fading tail
				Color head = CardTheme.shade(base, Color.WHITE, 0.75);
				double trail = Math.min(48, track.length / 4);
				double lead = phase(at, 3200, 0) * track.length;
				for (int n = 0; n < 2; n++)
				{
					double from = lead + n * track.length / 2;
					for (double s = 0; s < trail; s += 1)
					{
						double[] p = track.at(from - s);
						double left = 1 - s / trail;
						graphics.setColor(alpha(head, left * left));
						graphics.fillRect((int) Math.round(p[0]) - half, (int) Math.round(p[1]) - half, width, width);
					}
				}
				break;
			}
		}
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
		graphics.setStroke(oldStroke);
		graphics.setColor(oldColor);
	}

	/** A shape's outline as straight runs, to find the point any distance along it. */
	static final class Track
	{
		private final List<double[]> points = new ArrayList<>();
		private final List<Double> lengths = new ArrayList<>();
		final double length;

		Track(Shape shape)
		{
			PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), 0.5);
			double[] c = new double[6];
			double[] start = null;
			double total = 0;
			while (!it.isDone())
			{
				int type = it.currentSegment(c);
				double[] p = type == PathIterator.SEG_CLOSE ? start : new double[]{c[0], c[1]};
				if (type == PathIterator.SEG_MOVETO)
				{
					if (start != null)
					{
						// one loop is enough
						break;
					}
					start = p;
				}
				else if (p != null && !points.isEmpty())
				{
					double[] last = points.get(points.size() - 1);
					total += Math.hypot(p[0] - last[0], p[1] - last[1]);
				}
				if (p != null)
				{
					points.add(p);
					lengths.add(total);
				}
				it.next();
			}
			length = total;
		}

		/** The point a distance along, wrapping round. */
		double[] at(double distance)
		{
			double d = distance % length;
			if (d < 0)
			{
				d += length;
			}
			int lo = 0;
			int hi = lengths.size() - 1;
			while (lo < hi - 1)
			{
				int mid = (lo + hi) >>> 1;
				if (lengths.get(mid) <= d)
				{
					lo = mid;
				}
				else
				{
					hi = mid;
				}
			}
			double[] a = points.get(lo);
			double[] b = points.get(Math.min(hi, points.size() - 1));
			double span = lengths.get(Math.min(hi, lengths.size() - 1)) - lengths.get(lo);
			double f = span <= 0 ? 0 : (d - lengths.get(lo)) / span;
			return new double[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f};
		}
	}
}
