package com.nextunlocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import net.runelite.api.gameval.ItemID;

/**
 * Decorations over a card's frame, whatever its shape: pieces at the four corners and things running along the
 * edges. Each piece is drawn once around the top left corner, or along the top edge, and turned into place.
 */
final class CardTrim
{
	private static final Color GOLD = new Color(222, 184, 74);
	private static final Color PINK = new Color(247, 140, 186);
	private static final Color RUBY = new Color(214, 38, 64);
	private static final Color IVY = new Color(66, 150, 64);
	private static final Color STEM = new Color(74, 104, 42);
	private static final Color BRIAR = new Color(92, 64, 40);
	private static final Color IRON = new Color(128, 132, 140);
	private static final Color STEEL = new Color(196, 204, 214);
	private static final Color BONE = new Color(232, 224, 200);
	private static final Color ICE = new Color(206, 236, 255);
	private static final Color PEARL = new Color(250, 240, 236);
	private static final Color DRAGON = new Color(176, 36, 28);
	private static final Color LASH = new Color(132, 34, 38);
	private static final Color STONE = new Color(112, 99, 80);
	private static final Color HEMP = new Color(186, 150, 98);
	private static final Color CORAL = new Color(230, 112, 52);
	private static final Color FISH = new Color(118, 162, 190);
	private static final Color BARK = new Color(124, 82, 46);
	private static final Color WOOD = new Color(218, 178, 118);
	private static final Color FIRE = new Color(236, 98, 30);
	private static final Color HERB = new Color(104, 176, 62);
	private static final Color RUNE = new Color(150, 146, 140);
	private static final Color FLETCH = new Color(196, 52, 44);

	/** Draws one piece with the corner, or the start of the edge, at 0,0 and the card's inside towards +x +y. */
	private interface Piece
	{
		void draw(Graphics2D g, Colors c, double length);
	}

	/** The trim's own color, or the border's, with a light and a dark shade of it. */
	private static final class Colors
	{
		final Color main;
		final Color light;
		final Color dark;
		/** For a second part, like the leaves on a vine; the main color when tinted. */
		final Color second;
		final boolean tint;

		Colors(Color main, Color second, boolean tint)
		{
			this.main = main;
			this.light = CardTheme.shade(main, Color.WHITE, 0.5);
			this.dark = CardTheme.shade(main, Color.BLACK, 0.5);
			this.second = second;
			this.tint = tint;
		}

		/** A color of the trim's own, or one in the border's color when tinted. */
		Color own(Color natural)
		{
			return tint ? main : natural;
		}
	}

	private CardTrim()
	{
	}

	/** How far a trim's edge pieces reach in from the card's edge at normal size, so the content can keep clear. */
	static int reach(NextUnlocksConfig.Trim trim)
	{
		if (trim == null)
		{
			return 0;
		}
		switch (trim)
		{
			case FILIGREE:
				return 7;
			case THORNS:
			case MINIMAP_DRAGONS:
				return 6;
			case VINES:
			case ROSES:
			case CHAINS:
			case ANCHOR:
			case WHIP:
			case NET:
			case HERBS:
			case ANVILS:
			case BONES:
				return 5;
			case SKULLS:
				return 4;
			case PRINCESS:
				return 4;
			case GEMS:
			case SNOW:
			case FLAMES:
			case ARROWS:
			case DRAGON_WEAPONS:
				return 3;
			case HEARTS:
			case RUNES:
				return 2;
			case STARS:
				return 1;
			default:
				return 0;
		}
	}

	static void paint(Graphics2D graphics, CardTheme theme, Rectangle box)
	{
		NextUnlocksConfig.Trim trim = theme.getTrim();
		if (trim == null || trim == NextUnlocksConfig.Trim.STYLE || trim == NextUnlocksConfig.Trim.AUTO
			|| trim == NextUnlocksConfig.Trim.OFF
			|| box.width < 24 || box.height < 24)
		{
			return;
		}
		double scale = theme.getTrimScale() > 0 ? theme.getTrimScale() : 1;
		Color border = theme.getBorder() != null ? theme.getBorder() : GOLD;
		boolean tint = theme.isTrimTint();
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		long now = System.currentTimeMillis();
		switch (trim)
		{
			case FILIGREE:
			{
				Colors c = colors(GOLD, GOLD, border, tint);
				edges(g, box, scale, 20, c, CardTrim::filigreeEdge);
				corners(g, box, scale, c, CardTrim::filigreeCorner, false);
				middles(g, box, scale, c, CardTrim::filigreeMiddle);
				break;
			}
			case PRINCESS:
			{
				Colors c = colors(GOLD, PINK, border, tint);
				edges(g, box, scale, 22, c, CardTrim::pearls);
				corners(g, box, scale, c, CardTrim::filigreeCorner, false);
				corners(g, box, scale, c, (q, k, l) -> rose(q, k, 6, 6, 4.2, k.second), false);
				middles(g, box, scale, c, CardTrim::princessMiddle);
				break;
			}
			case VINES:
			{
				Colors c = colors(STEM, IVY, border, tint);
				edges(g, box, scale, 14, c, (q, k, l) -> vine(q, k, l, true, false));
				corners(g, box, scale, c, CardTrim::ivyCorner, false);
				break;
			}
			case ROSES:
			{
				Colors c = colors(STEM, IVY, border, tint);
				edges(g, box, scale, 16, c, (q, k, l) -> vine(q, k, l, true, true));
				corners(g, box, scale, c, (q, k, l) ->
				{
					leaf(q, k, 10, 3, Math.toRadians(10), 6);
					leaf(q, k, 3, 10, Math.toRadians(80), 6);
					rose(q, k, 4.5, 4.5, 5.2, k.own(RUBY));
				}, false);
				break;
			}
			case THORNS:
			{
				Colors c = colors(BRIAR, BRIAR, border, tint);
				edges(g, box, scale, 12, c, CardTrim::briar);
				corners(g, box, scale, c, CardTrim::briarCorner, false);
				break;
			}
			case ANVILS:
			{
				Colors c = colors(IRON, IRON, border, tint);
				corners(g, box, scale, c, CardTrim::anvil, true);
				middles(g, box, scale, c, CardTrim::hammer);
				break;
			}
			case SWORDS:
			{
				Colors c = colors(STEEL, GOLD, border, tint);
				corners(g, box, scale, c, CardTrim::swords, false);
				break;
			}
			case GEMS:
			{
				Colors c = colors(GOLD, RUBY, border, tint);
				corners(g, box, scale, c, (q, k, l) -> gem(q, k, 3, 3, 4.5), false);
				middles(g, box, scale, c, (q, k, l) -> gem(q, k, 0, 1, 3.2));
				edges(g, box, scale, 18, c, (q, k, l) -> studs(q, k, l, 14));
				break;
			}
			case STARS:
			{
				Colors c = colors(GOLD, Color.WHITE, border, tint);
				corners(g, box, scale, c, (q, k, l) -> star(q, k, 3.5, 3.5, 5.5, 1), false);
				edges(g, box, scale, 16, c, (q, k, l) -> sparkles(q, k, l, now));
				break;
			}
			case HEARTS:
			{
				Colors c = colors(PINK, RUBY, border, tint);
				corners(g, box, scale, c, (q, k, l) -> heart(q, k, 3.5, 3, 5.5, k.main), true);
				edges(g, box, scale, 16, c, (q, k, l) ->
				{
					for (double x = 12; x < l - 6; x += 18)
					{
						heart(q, k, x, -0.5, 2.4, k.own(PINK));
					}
				});
				break;
			}
			case SKULLS:
			{
				Colors c = colors(BONE, BONE, border, tint);
				corners(g, box, scale, c, CardTrim::skull, true);
				break;
			}
			case SNOW:
			{
				Colors c = colors(ICE, ICE, border, tint);
				corners(g, box, scale, c, (q, k, l) -> snowflake(q, k, 3.5, 3.5, 6), true);
				edges(g, box, scale, 16, c, (q, k, l) ->
				{
					for (double x = 14; x < l - 6; x += 20)
					{
						snowflake(q, k, x, 0, 2.6);
					}
				});
				break;
			}
			case CHAINS:
			{
				Colors c = colors(STEEL, STEEL, border, tint);
				edges(g, box, scale, 6, c, (q, k, l) -> chain(q, k, l, now));
				corners(g, box, scale, c, CardTrim::ring, false);
				break;
			}
			case ANCHOR:
			{
				Colors c = colors(STEEL, IRON, border, tint);
				edges(g, box, scale, 6, c, (q, k, l) -> chain(q, k, l, now));
				corners(g, box, scale, c, CardTrim::ring, false);
				if (box.width >= 60 * scale)
				{
					AffineTransform saved = g.getTransform();
					g.translate(box.x + box.width / 2.0, box.y);
					g.scale(scale * 1.35, scale * 1.35);
					// rocking gently where it rests on the chain
					g.rotate(Math.toRadians(5) * Math.sin(now / 900.0));
					anchor(g, c);
					g.setTransform(saved);
				}
				break;
			}
			case DRAGON_WEAPONS:
			{
				Colors c = colors(DRAGON, GOLD, border, tint);
				BufferedImage claws = CardArt.item(ItemID.DRAGON_CLAWS);
				BufferedImage scimitar = CardArt.item(ItemID.DRAGON_SCIMITAR);
				BufferedImage longsword = CardArt.item(ItemID.DRAGON_LONGSWORD);
				if (CardArt.bounds(claws) == null || CardArt.bounds(scimitar) == null || CardArt.bounds(longsword) == null)
				{
					// no icons yet: red swords of its own
					corners(g, box, scale, c, CardTrim::swords, false);
					break;
				}
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				corners(g, box, scale, c, (q, k, l) -> icon(q, claws, 7, 7, 16, Math.toRadians(-45)), false);
				// on the top edge only: the bottom one would hang over the chatbox
				top(g, box, scale, c, (q, k, l) ->
				{
					icon(q, longsword, 0, 3, 19, Math.toRadians(-90));
					icon(q, scimitar, 0, 3, 19, 0);
				});
				break;
			}
			case NET:
			{
				Colors c = colors(HEMP, CORAL, border, tint);
				edges(g, box, scale, 16, c, (q, k, l) -> net(q, k, l, now));
				corners(g, box, scale, c, CardTrim::fish, false);
				break;
			}
			case LOGS:
			{
				Colors c = colors(BARK, WOOD, border, tint);
				corners(g, box, scale, c, CardTrim::logs, false);
				top(g, box, scale, c, CardTrim::hatchet);
				break;
			}
			case FLAMES:
				flames(g, box, scale, colors(FIRE, GOLD, border, tint), now);
				break;
			case HERBS:
			{
				Colors c = colors(STEM, HERB, border, tint);
				edges(g, box, scale, 16, c, (q, k, l) -> vine(q, k, l, true, false));
				corners(g, box, scale, c, (q, k, l) ->
				{
					leaf(q, k, 4, 2.5, Math.toRadians(8), 8);
					leaf(q, k, 2.5, 4, Math.toRadians(82), 8);
					leaf(q, k, 3, 3, Math.toRadians(45), 9);
				}, false);
				top(g, box, scale, c, CardTrim::vial);
				break;
			}
			case RUNES:
			{
				Colors c = colors(RUNE, new Color(160, 110, 230), border, tint);
				int[] corner = {0};
				corners(g, box, scale, c, (q, k, l) -> rune(q, k, corner[0]++, now), true);
				edges(g, box, scale, 18, c, (q, k, l) -> essence(q, k, l, now));
				break;
			}
			case BONES:
			{
				Colors c = colors(BONE, BONE, border, tint);
				corners(g, box, scale, c, (q, k, l) ->
				{
					bone(q, k, 1.5, 9, 9, 1.5, 0.9);
					bone(q, k, 1.5, 1.5, 9, 9, 0.9);
				}, false);
				edges(g, box, scale, 20, c, (q, k, l) ->
				{
					for (double x = 12; x < l - 8; x += 24)
					{
						bone(q, k, x - 4, 1.5, x + 4, 1.5, 0.7);
					}
				});
				break;
			}
			case ARROWS:
			{
				Colors c = colors(WOOD, FLETCH, border, tint);
				corners(g, box, scale, c, CardTrim::arrows, false);
				top(g, box, scale, c, CardTrim::bow);
				break;
			}
			case WHIP:
				whip(g, box, scale, colors(LASH, LASH, border, tint));
				break;
			case MINIMAP_DRAGONS:
				if (!dragons(g, box, scale, tint ? border : null))
				{
					Colors c = colors(STONE, STONE, border, tint);
					g.setColor(c.dark);
					stroke(g, (float) (6 * scale));
					double in = 3 * scale;
					g.draw(new java.awt.geom.RoundRectangle2D.Double(box.x + in, box.y + in, box.width - 1 - in * 2,
						box.height - 1 - in * 2, 18 * scale, 18 * scale));
					g.setColor(c.main);
					stroke(g, (float) (4 * scale));
					g.draw(new java.awt.geom.RoundRectangle2D.Double(box.x + in, box.y + in, box.width - 1 - in * 2,
						box.height - 1 - in * 2, 18 * scale, 18 * scale));
				}
				break;
			default:
				break;
		}
		g.dispose();
	}

	private static Colors colors(Color main, Color second, Color border, boolean tint)
	{
		return tint ? new Colors(border, border, true) : new Colors(main, second, false);
	}

	/** The four corners, mirrored; upright pieces only turn left to right so they never stand on their heads. */
	private static void corners(Graphics2D g, Rectangle box, double scale, Colors c, Piece piece, boolean upright)
	{
		double right = box.x + box.width - 1;
		double bottom = box.y + box.height - 1;
		double[][] at = {{box.x, box.y, 1, 1}, {right, box.y, -1, 1}, {box.x, bottom, 1, -1}, {right, bottom, -1, -1}};
		for (double[] a : at)
		{
			AffineTransform saved = g.getTransform();
			g.translate(a[0], a[1]);
			g.scale(a[2] * scale, a[3] * scale);
			if (upright && a[3] < 0)
			{
				// stand it in the corner the right way up
				g.scale(1, -1);
				g.translate(0, -8);
			}
			piece.draw(g, c, 0);
			g.setTransform(saved);
		}
	}

	/** The middle of the top edge only, for pieces that stand up out of the card. */
	private static void top(Graphics2D g, Rectangle box, double scale, Colors c, Piece piece)
	{
		if (box.width < 90 * scale)
		{
			return;
		}
		AffineTransform saved = g.getTransform();
		g.translate(box.x + box.width / 2.0, box.y);
		g.scale(scale, scale);
		piece.draw(g, c, 0);
		g.setTransform(saved);
	}

	/** The middle of the top and bottom edges, with the bottom one turned over. */
	private static void middles(Graphics2D g, Rectangle box, double scale, Colors c, Piece piece)
	{
		if (box.width < 90 * scale)
		{
			return;
		}
		top(g, box, scale, c, piece);
		AffineTransform saved = g.getTransform();
		g.translate(box.x + box.width / 2.0, box.y + box.height - 1);
		g.scale(scale, -scale);
		piece.draw(g, c, 0);
		g.setTransform(saved);
	}

	/** Along each edge between the corner pieces, the line at y 0 and the inside towards +y. */
	private static void edges(Graphics2D g, Rectangle box, double scale, double gap, Colors c, Piece piece)
	{
		double x0 = box.x;
		double y0 = box.y;
		double x1 = box.x + box.width - 1;
		double y1 = box.y + box.height - 1;
		double inset = gap * scale;
		double[][] runs = {
			{x0, y0, 0, x1 - x0},
			{x1, y0, Math.PI / 2, y1 - y0},
			{x1, y1, Math.PI, x1 - x0},
			{x0, y1, -Math.PI / 2, y1 - y0},
		};
		for (double[] r : runs)
		{
			double length = (r[3] - inset * 2) / scale;
			if (length <= 4)
			{
				continue;
			}
			AffineTransform saved = g.getTransform();
			g.translate(r[0], r[1]);
			g.rotate(r[2]);
			g.translate(inset, 0);
			g.scale(scale, scale);
			piece.draw(g, c, length);
			g.setTransform(saved);
		}
	}

	private static void stroke(Graphics2D g, float width)
	{
		g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	}

	/** Fill in one color and outline in another, the way the game's sprites are drawn. */
	private static void sprite(Graphics2D g, Shape shape, Color fill, Color line)
	{
		g.setColor(fill);
		g.fill(shape);
		stroke(g, 0.8f);
		g.setColor(line);
		g.draw(shape);
	}

	// ---- filigree ----

	/** Two curling scrolls out from the corner, one along each edge, and a teardrop between them. */
	private static void filigreeCorner(Graphics2D g, Colors c, double length)
	{
		Path2D.Double scroll = new Path2D.Double();
		scroll.moveTo(2, 2);
		scroll.curveTo(7, 8, 13, -1, 18, 3.5);
		scroll.curveTo(20.5, 6, 17.5, 8.5, 15.5, 6.5);
		scroll.curveTo(14.3, 5.3, 15.5, 4, 16.5, 4.8);
		Path2D.Double down = (Path2D.Double) scroll.clone();
		down.transform(new AffineTransform(0, 1, 1, 0, 0, 0));
		g.setColor(c.dark);
		stroke(g, 2.4f);
		g.draw(scroll);
		g.draw(down);
		g.setColor(c.main);
		stroke(g, 1.3f);
		g.draw(scroll);
		g.draw(down);
		Path2D.Double drop = new Path2D.Double();
		drop.moveTo(2.5, 2.5);
		drop.curveTo(4, 9, 9, 4, 2.5, 2.5);
		drop.moveTo(4.5, 4.5);
		drop.curveTo(10.5, 6, 6, 10.5, 4.5, 4.5);
		sprite(g, drop, c.main, c.dark);
		g.setColor(c.light);
		g.fill(new Ellipse2D.Double(0.4, 0.4, 3.2, 3.2));
	}

	/** A fine line inside the edge with little curls every so often. */
	private static void filigreeEdge(Graphics2D g, Colors c, double length)
	{
		g.setColor(c.main);
		stroke(g, 0.9f);
		Path2D.Double line = new Path2D.Double();
		line.moveTo(0, 3);
		line.lineTo(length, 3);
		g.draw(line);
		for (double x = 16; x < length - 10; x += 26)
		{
			Path2D.Double curl = new Path2D.Double();
			curl.moveTo(x - 6, 3);
			curl.curveTo(x - 3, 3, x - 2, 6.5, x, 6.5);
			curl.curveTo(x + 2, 6.5, x + 3, 3, x + 6, 3);
			g.draw(curl);
			g.fill(new Ellipse2D.Double(x - 1, 0.5, 2, 2));
		}
	}

	/** A pair of scrolls meeting at a diamond in the middle of the top edge. */
	private static void filigreeMiddle(Graphics2D g, Colors c, double length)
	{
		for (int side = -1; side <= 1; side += 2)
		{
			Path2D.Double s = new Path2D.Double();
			s.moveTo(side * 3, 3);
			s.curveTo(side * 7, 8, side * 12, -1, side * 15, 3.5);
			s.curveTo(side * 17, 6, side * 14, 7.5, side * 13, 5.5);
			g.setColor(c.dark);
			stroke(g, 2.2f);
			g.draw(s);
			g.setColor(c.main);
			stroke(g, 1.2f);
			g.draw(s);
		}
		Polygon diamond = new Polygon(new int[]{0, 4, 0, -4}, new int[]{-1, 3, 7, 3}, 4);
		sprite(g, diamond, c.light, c.dark);
	}

	// ---- princess ----

	/** Pearls strung along the edge. */
	private static void pearls(Graphics2D g, Colors c, double length)
	{
		int count = Math.max(1, (int) (length / 9));
		double step = length / count;
		for (int i = 0; i <= count; i++)
		{
			double x = i * step;
			double r = i % 2 == 0 ? 1.8 : 1.3;
			g.setColor(c.own(PEARL));
			g.fill(new Ellipse2D.Double(x - r, 2 - r, r * 2, r * 2));
			g.setColor(c.tint ? c.dark : new Color(210, 170, 180));
			stroke(g, 0.6f);
			g.draw(new Ellipse2D.Double(x - r, 2 - r, r * 2, r * 2));
			g.setColor(Color.WHITE);
			g.fill(new Ellipse2D.Double(x - r * 0.5, 2 - r * 0.6, r * 0.7, r * 0.7));
		}
	}

	/** A little crown with a pink jewel over the top edge, a bow below the bottom. */
	private static void princessMiddle(Graphics2D g, Colors c, double length)
	{
		Path2D.Double crown = new Path2D.Double();
		crown.moveTo(-8, 5);
		crown.lineTo(-8, -3);
		crown.lineTo(-4, 1);
		crown.lineTo(0, -5);
		crown.lineTo(4, 1);
		crown.lineTo(8, -3);
		crown.lineTo(8, 5);
		crown.closePath();
		sprite(g, crown, c.main, c.dark);
		g.setColor(c.light);
		g.fill(new Rectangle2D.Double(-7, 3, 14, 1.5));
		for (int x = -8; x <= 8; x += 8)
		{
			g.setColor(PEARL);
			g.fill(new Ellipse2D.Double(x - 1.3, (x == 0 ? -5 : -3) - 1.3, 2.6, 2.6));
		}
		gemShape(g, 0, 2, 1.8, c.second, c.dark);
	}

	// ---- vines and flowers ----

	/** A vine winding along the edge with leaves, and rosebuds now and then. */
	private static void vine(Graphics2D g, Colors c, double length, boolean leaves, boolean buds)
	{
		double wave = 13;
		Path2D.Double stem = new Path2D.Double();
		stem.moveTo(0, 1.5);
		for (double x = 0; x <= length; x += 1)
		{
			stem.lineTo(x, 1.5 + 2.2 * Math.sin(x * Math.PI * 2 / wave));
		}
		g.setColor(c.main);
		stroke(g, 1.3f);
		g.draw(stem);
		if (leaves)
		{
			int i = 0;
			for (double x = wave / 4; x < length - 2; x += wave / 2, i++)
			{
				double y = 1.5 + 2.2 * Math.sin(x * Math.PI * 2 / wave);
				boolean out = i % 2 == 0;
				if (buds && i % 6 == 3)
				{
					bud(g, c, x, y);
				}
				else
				{
					leaf(g, c, x, y, out ? Math.toRadians(-50) : Math.toRadians(50), 4.5);
				}
			}
		}
	}

	/** A leaf with its stalk at x,y pointing at the angle. */
	private static void leaf(Graphics2D g, Colors c, double x, double y, double angle, double size)
	{
		AffineTransform saved = g.getTransform();
		g.translate(x, y);
		g.rotate(angle);
		Path2D.Double leaf = new Path2D.Double();
		leaf.moveTo(0, 0);
		leaf.curveTo(size * 0.3, -size * 0.45, size * 0.8, -size * 0.4, size, 0);
		leaf.curveTo(size * 0.8, size * 0.4, size * 0.3, size * 0.45, 0, 0);
		g.setColor(c.second);
		g.fill(leaf);
		g.setColor(CardTheme.shade(c.second, Color.BLACK, 0.45));
		stroke(g, 0.5f);
		g.draw(leaf);
		g.setColor(CardTheme.shade(c.second, Color.WHITE, 0.35));
		g.draw(new java.awt.geom.Line2D.Double(size * 0.15, 0, size * 0.8, 0));
		g.setTransform(saved);
	}

	/** A closed rosebud on the vine. */
	private static void bud(Graphics2D g, Colors c, double x, double y)
	{
		Color petal = c.own(RUBY);
		g.setColor(c.second);
		g.fill(new Ellipse2D.Double(x - 1.6, y - 0.5, 3.2, 3));
		g.setColor(petal);
		g.fill(new Ellipse2D.Double(x - 1.8, y - 3.2, 3.6, 3.8));
		g.setColor(CardTheme.shade(petal, Color.WHITE, 0.4));
		g.fill(new Ellipse2D.Double(x - 0.8, y - 2.8, 1.4, 1.4));
	}

	/** An open rose seen from above: petals round a spiral heart. */
	private static void rose(Graphics2D g, Colors c, double x, double y, double r, Color petal)
	{
		Color dark = CardTheme.shade(petal, Color.BLACK, 0.4);
		Color light = CardTheme.shade(petal, Color.WHITE, 0.35);
		for (int i = 0; i < 5; i++)
		{
			double a = Math.PI * 2 * i / 5 - Math.PI / 2;
			double px = x + Math.cos(a) * r * 0.45;
			double py = y + Math.sin(a) * r * 0.45;
			Ellipse2D.Double p = new Ellipse2D.Double(px - r * 0.6, py - r * 0.6, r * 1.2, r * 1.2);
			sprite(g, p, petal, dark);
		}
		g.setColor(light);
		g.fill(new Ellipse2D.Double(x - r * 0.5, y - r * 0.5, r, r));
		g.setColor(dark);
		stroke(g, 0.8f);
		g.draw(new Arc2D.Double(x - r * 0.45, y - r * 0.45, r * 0.9, r * 0.9, 90, 270, Arc2D.OPEN));
		g.draw(new Arc2D.Double(x - r * 0.25, y - r * 0.25, r * 0.5, r * 0.5, 270, 270, Arc2D.OPEN));
		g.setColor(Color.WHITE);
		g.fill(new Ellipse2D.Double(x - r * 0.35, y - r * 0.45, r * 0.25, r * 0.25));
	}

	/** A curl of stem in the corner with a fan of ivy leaves. */
	private static void ivyCorner(Graphics2D g, Colors c, double length)
	{
		Path2D.Double curl = new Path2D.Double();
		curl.moveTo(14, 1.5);
		curl.curveTo(6, 1.5, 1.5, 6, 1.5, 14);
		g.setColor(c.main);
		stroke(g, 1.4f);
		g.draw(curl);
		leaf(g, c, 4, 4, Math.toRadians(45), 7);
		leaf(g, c, 4, 4, Math.toRadians(5), 5.5);
		leaf(g, c, 4, 4, Math.toRadians(85), 5.5);
		g.setColor(c.own(new Color(140, 60, 160)));
		for (double[] berry : new double[][]{{1.5, 6}, {6, 1.5}, {2.5, 2.5}})
		{
			g.fill(new Ellipse2D.Double(berry[0] - 1.2, berry[1] - 1.2, 2.4, 2.4));
		}
	}

	// ---- briar ----

	/** A twisting branch with thorns poking out. */
	private static void briar(Graphics2D g, Colors c, double length)
	{
		Path2D.Double stem = new Path2D.Double();
		stem.moveTo(0, 1.5);
		for (double x = 0; x <= length; x += 1)
		{
			stem.lineTo(x, 1.5 + 1.6 * Math.sin(x * Math.PI * 2 / 21) + 0.6 * Math.sin(x * 0.9));
		}
		g.setColor(c.dark);
		stroke(g, 2.2f);
		g.draw(stem);
		g.setColor(c.main);
		stroke(g, 1.3f);
		g.draw(stem);
		int i = 0;
		for (double x = 4; x < length - 2; x += 7, i++)
		{
			double y = 1.5 + 1.6 * Math.sin(x * Math.PI * 2 / 21) + 0.6 * Math.sin(x * 0.9);
			double dir = i % 2 == 0 ? -1 : 1;
			Path2D.Double t = new Path2D.Double();
			t.moveTo(x - 1.4, y);
			t.lineTo(x + 2.5, y + dir * 3.4);
			t.lineTo(x + 1.4, y);
			t.closePath();
			g.setColor(c.light);
			g.fill(t);
		}
	}

	/** Two branches crossing in the corner, thorns and all. */
	private static void briarCorner(Graphics2D g, Colors c, double length)
	{
		for (int i = 0; i < 2; i++)
		{
			Path2D.Double b = new Path2D.Double();
			if (i == 0)
			{
				b.moveTo(-1, 9);
				b.curveTo(3, 2, 8, 0, 14, 2);
			}
			else
			{
				b.moveTo(9, -1);
				b.curveTo(2, 3, 0, 8, 2, 14);
			}
			g.setColor(c.dark);
			stroke(g, 2.6f);
			g.draw(b);
			g.setColor(c.main);
			stroke(g, 1.5f);
			g.draw(b);
		}
		g.setColor(c.light);
		for (double[] t : new double[][]{{5, 3.2, -1, -3}, {3.2, 5, -3, -1}, {9, 1.2, 1, -3}, {1.2, 9, -3, 1}})
		{
			Path2D.Double thorn = new Path2D.Double();
			thorn.moveTo(t[0] - 1, t[1]);
			thorn.lineTo(t[0] + t[2], t[1] + t[3]);
			thorn.lineTo(t[0] + 1, t[1] + 0.5);
			thorn.closePath();
			g.fill(thorn);
		}
	}

	// ---- smithing ----

	/** An anvil standing in the corner, its horn towards the middle of the edge. */
	private static void anvil(Graphics2D g, Colors c, double length)
	{
		Path2D.Double a = new Path2D.Double();
		a.moveTo(1, 0);
		a.lineTo(13, 0);
		a.curveTo(16, 0, 18.5, 0.5, 20, 1.6);
		a.curveTo(17.5, 2.6, 15, 3, 13, 3.2);
		a.lineTo(11.5, 4.2);
		a.lineTo(10, 5.5);
		a.lineTo(12.5, 8);
		a.lineTo(1, 8);
		a.lineTo(3.5, 5.5);
		a.lineTo(2.5, 4.2);
		a.lineTo(1, 3.2);
		a.closePath();
		AffineTransform saved = g.getTransform();
		g.translate(1, 0.5);
		sprite(g, a, c.main, c.dark);
		g.setColor(c.light);
		g.fill(new Rectangle2D.Double(1.5, 0.4, 11, 1.1));
		g.setColor(c.dark);
		g.fill(new Rectangle2D.Double(4, 5.5, 5.5, 1));
		g.setTransform(saved);
	}

	/** A smithing hammer lying over the middle of the edge. */
	private static void hammer(Graphics2D g, Colors c, double length)
	{
		AffineTransform saved = g.getTransform();
		g.rotate(Math.toRadians(-18));
		Rectangle2D.Double handle = new Rectangle2D.Double(-9, 1.2, 14, 1.8);
		sprite(g, handle, c.own(new Color(150, 98, 52)), c.tint ? c.dark : new Color(80, 50, 24));
		Rectangle2D.Double head = new Rectangle2D.Double(4, -1.5, 4, 7);
		sprite(g, head, c.main, c.dark);
		g.setColor(c.light);
		g.fill(new Rectangle2D.Double(4.5, -1, 1, 6));
		g.setTransform(saved);
	}

	// ---- weapons ----

	/** Two swords crossed over the corner, hilts out. */
	private static void swords(Graphics2D g, Colors c, double length)
	{
		for (int i = 0; i < 2; i++)
		{
			AffineTransform saved = g.getTransform();
			if (i == 1)
			{
				// the second, mirrored over the diagonal
				g.transform(new AffineTransform(0, 1, 1, 0, 0, 0));
			}
			g.translate(1.5, 9);
			g.rotate(Math.toRadians(-40));
			Path2D.Double blade = new Path2D.Double();
			blade.moveTo(2.5, -1);
			blade.lineTo(15, -1);
			blade.lineTo(17.5, 0);
			blade.lineTo(15, 1);
			blade.lineTo(2.5, 1);
			blade.closePath();
			sprite(g, blade, c.main, c.dark);
			g.setColor(c.light);
			g.draw(new java.awt.geom.Line2D.Double(3, -0.2, 15, -0.2));
			Rectangle2D.Double guard = new Rectangle2D.Double(1.5, -3, 1.4, 6);
			sprite(g, guard, c.own(GOLD), c.dark);
			Rectangle2D.Double grip = new Rectangle2D.Double(-2.5, -0.7, 4, 1.4);
			sprite(g, grip, c.own(new Color(110, 60, 40)), c.dark);
			g.setColor(c.own(GOLD));
			g.fill(new Ellipse2D.Double(-4, -1.2, 2.4, 2.4));
			g.setTransform(saved);
		}
	}

	// ---- gems and stars ----

	private static void gem(Graphics2D g, Colors c, double x, double y, double r)
	{
		// set in a gold bezel
		Ellipse2D.Double bezel = new Ellipse2D.Double(x - r - 1.2, y - r - 1.2, (r + 1.2) * 2, (r + 1.2) * 2);
		sprite(g, bezel, c.main, c.dark);
		gemShape(g, x, y, r, c.second, CardTheme.shade(c.second, Color.BLACK, 0.5));
	}

	/** A cut stone: a diamond with a lighter table and a sparkle. */
	private static void gemShape(Graphics2D g, double x, double y, double r, Color stone, Color line)
	{
		Path2D.Double d = new Path2D.Double();
		d.moveTo(x, y - r);
		d.lineTo(x + r, y);
		d.lineTo(x, y + r);
		d.lineTo(x - r, y);
		d.closePath();
		sprite(g, d, stone, line);
		Path2D.Double table = new Path2D.Double();
		table.moveTo(x, y - r * 0.5);
		table.lineTo(x + r * 0.5, y);
		table.lineTo(x, y + r * 0.5);
		table.lineTo(x - r * 0.5, y);
		table.closePath();
		g.setColor(CardTheme.shade(stone, Color.WHITE, 0.35));
		g.fill(table);
		g.setColor(Color.WHITE);
		g.fill(new Ellipse2D.Double(x - r * 0.45, y - r * 0.55, r * 0.35, r * 0.35));
	}

	/** Little gold studs along the edge. */
	private static void studs(Graphics2D g, Colors c, double length, double step)
	{
		for (double x = step / 2; x < length; x += step)
		{
			Ellipse2D.Double s = new Ellipse2D.Double(x - 1.3, 0.2, 2.6, 2.6);
			sprite(g, s, c.light, c.dark);
		}
	}

	private static void star(Graphics2D g, Colors c, double x, double y, double r, double alpha)
	{
		Path2D.Double s = new Path2D.Double();
		for (int i = 0; i < 10; i++)
		{
			double a = Math.PI * i / 5 - Math.PI / 2;
			double d = i % 2 == 0 ? r : r * 0.45;
			if (i == 0)
			{
				s.moveTo(x + Math.cos(a) * d, y + Math.sin(a) * d);
			}
			else
			{
				s.lineTo(x + Math.cos(a) * d, y + Math.sin(a) * d);
			}
		}
		s.closePath();
		sprite(g, s, fade(c.main, alpha), fade(c.dark, alpha));
		g.setColor(fade(c.light, alpha));
		g.fill(new Ellipse2D.Double(x - r * 0.25, y - r * 0.3, r * 0.4, r * 0.4));
	}

	/** Four-pointed sparkles along the edge that twinkle one after another. */
	private static void sparkles(Graphics2D g, Colors c, double length, long now)
	{
		int i = 0;
		for (double x = 10; x < length - 4; x += 15, i++)
		{
			double phase = (now / 900.0 + i * 0.37) % 1.0;
			double glint = 0.35 + 0.65 * Math.max(0, Math.sin(phase * Math.PI * 2));
			double r = 1.6 + 1.6 * glint;
			Path2D.Double s = new Path2D.Double();
			s.moveTo(x, -r);
			s.quadTo(x, 0, x + r * 0.7, 0);
			s.quadTo(x, 0, x, r);
			s.quadTo(x, 0, x - r * 0.7, 0);
			s.quadTo(x, 0, x, -r);
			s.closePath();
			g.setColor(fade(i % 3 == 0 ? c.second : c.light, glint));
			g.fill(s);
		}
	}

	// ---- hearts, skulls, snow ----

	private static void heart(Graphics2D g, Colors c, double x, double y, double r, Color fill)
	{
		Path2D.Double h = new Path2D.Double();
		h.moveTo(x, y + r * 0.95);
		h.curveTo(x - r * 1.4, y, x - r * 0.9, y - r * 1.1, x, y - r * 0.35);
		h.curveTo(x + r * 0.9, y - r * 1.1, x + r * 1.4, y, x, y + r * 0.95);
		h.closePath();
		sprite(g, h, fill, CardTheme.shade(fill, Color.BLACK, 0.45));
		g.setColor(new Color(255, 255, 255, 190));
		g.fill(new Ellipse2D.Double(x - r * 0.65, y - r * 0.45, r * 0.4, r * 0.3));
	}

	/** A skull and crossbones standing in the corner. */
	private static void skull(Graphics2D g, Colors c, double length)
	{
		AffineTransform saved = g.getTransform();
		g.translate(5.5, 4.5);
		stroke(g, 1.6f);
		for (int side = -1; side <= 1; side += 2)
		{
			g.setColor(c.dark);
			stroke(g, 2.4f);
			g.draw(new java.awt.geom.Line2D.Double(-6, side * 5, 6, -side * 5));
			g.setColor(c.main);
			stroke(g, 1.4f);
			g.draw(new java.awt.geom.Line2D.Double(-6, side * 5, 6, -side * 5));
			g.fill(new Ellipse2D.Double(-7.5, side * 5 - 1.5, 3, 3));
			g.fill(new Ellipse2D.Double(4.5, -side * 5 - 1.5, 3, 3));
		}
		Path2D.Double head = new Path2D.Double();
		head.append(new Ellipse2D.Double(-4.5, -5.5, 9, 8.5), false);
		head.append(new Rectangle2D.Double(-2.8, 1.5, 5.6, 3), false);
		g.setColor(c.main);
		g.fill(head);
		g.setColor(c.dark);
		stroke(g, 0.8f);
		g.draw(new Ellipse2D.Double(-4.5, -5.5, 9, 8.5));
		g.fill(new Ellipse2D.Double(-3.3, -2.5, 2.6, 2.8));
		g.fill(new Ellipse2D.Double(0.7, -2.5, 2.6, 2.8));
		g.fill(new Rectangle2D.Double(-0.4, 0.8, 0.8, 1.2));
		for (double x = -1.8; x <= 1.9; x += 1.2)
		{
			g.fill(new Rectangle2D.Double(x, 3, 0.5, 1.5));
		}
		g.setTransform(saved);
	}

	private static void snowflake(Graphics2D g, Colors c, double x, double y, double r)
	{
		g.setColor(c.dark);
		stroke(g, r > 4 ? 2.2f : 1.6f);
		arms(g, x, y, r);
		g.setColor(c.main);
		stroke(g, r > 4 ? 1.1f : 0.8f);
		arms(g, x, y, r);
		g.setColor(Color.WHITE);
		g.fill(new Ellipse2D.Double(x - 0.8, y - 0.8, 1.6, 1.6));
	}

	private static void arms(Graphics2D g, double x, double y, double r)
	{
		Path2D.Double p = new Path2D.Double();
		for (int i = 0; i < 6; i++)
		{
			double a = Math.PI * i / 3 + Math.PI / 6;
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			p.moveTo(x, y);
			p.lineTo(x + cos * r, y + sin * r);
			if (r > 3)
			{
				// a pair of branches part way out each arm
				double bx = x + cos * r * 0.55;
				double by = y + sin * r * 0.55;
				for (int s = -1; s <= 1; s += 2)
				{
					double b = a + s * Math.PI / 4;
					p.moveTo(bx, by);
					p.lineTo(bx + Math.cos(b) * r * 0.35, by + Math.sin(b) * r * 0.35);
				}
			}
		}
		g.draw(p);
	}

	// ---- chains ----

	/** Links along the edge, flat and edge-on by turns, with a glint running along them now and then. */
	private static void chain(Graphics2D g, Colors c, double length, long now)
	{
		double glint = (now % 4200) / 2600.0 * (length + 20) - 10;
		double link = 7;
		int count = Math.max(1, (int) Math.round(length / (link - 1.5)));
		double step = length / count;
		Stroke thick = new BasicStroke(2.4f);
		Stroke thin = new BasicStroke(1.2f);
		for (int i = 0; i < count; i++)
		{
			double x = i * step;
			Shape s = i % 2 == 0
				? new java.awt.geom.RoundRectangle2D.Double(x, -0.5, link, 4, 4, 4)
				: new java.awt.geom.RoundRectangle2D.Double(x, 1, link, 1, 1, 1);
			g.setColor(c.dark);
			g.setStroke(thick);
			g.draw(s);
			g.setColor(c.main);
			g.setStroke(thin);
			g.draw(s);
			double near = 1 - Math.abs(x + link / 2 - glint) / 9;
			if (near > 0)
			{
				g.setColor(fade(Color.WHITE, near * 0.8));
				g.draw(s);
			}
		}
		g.setColor(c.light);
		g.setStroke(new BasicStroke(0.6f));
		for (int i = 0; i < count; i += 2)
		{
			double x = i * step;
			g.draw(new java.awt.geom.Line2D.Double(x + 1.8, -0.4, x + link - 1.8, -0.4));
		}
	}

	/** An anchor standing on the top edge, ring up and its arms resting on the chain; up is -y. */
	private static void anchor(Graphics2D g, Colors c)
	{
		Color iron = c.own(IRON);
		Color dark = CardTheme.shade(iron, Color.BLACK, 0.6);
		Color shine = CardTheme.shade(iron, Color.WHITE, 0.45);
		Path2D.Double arms = new Path2D.Double();
		arms.moveTo(-7.5, -4.5);
		arms.quadTo(-6.5, 1.2, 0, 1.2);
		arms.quadTo(6.5, 1.2, 7.5, -4.5);
		Shape[] lines = {arms, new java.awt.geom.Line2D.Double(0, -13, 0, 1)};
		for (int pass = 0; pass < 2; pass++)
		{
			g.setColor(pass == 0 ? dark : iron);
			stroke(g, pass == 0 ? 3.6f : 2f);
			for (Shape line : lines)
			{
				g.draw(line);
			}
			g.draw(new Ellipse2D.Double(-2.6, -18.6, 5.2, 5.2));
		}
		// the flukes, the crossbar with its knobs, and a little light down the shank
		for (int side = -1; side <= 1; side += 2)
		{
			Path2D.Double fluke = new Path2D.Double();
			fluke.moveTo(side * 7.5, -7.6);
			fluke.lineTo(side * 9.8, -2.8);
			fluke.lineTo(side * 5.2, -3.6);
			fluke.closePath();
			sprite(g, fluke, iron, dark);
			sprite(g, new Ellipse2D.Double(side * 6.2 - 1.3, -12.6, 2.6, 2.6), iron, dark);
		}
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(-6, -12.1, 12, 1.8, 1.2, 1.2), iron, dark);
		g.setColor(shine);
		stroke(g, 0.6f);
		g.draw(new java.awt.geom.Line2D.Double(-0.5, -9.5, -0.5, -1));
		g.draw(new Arc2D.Double(-2, -18, 4, 4, 100, 80, Arc2D.OPEN));
	}

	/** A big ring at the corner where the chains meet. */
	private static void ring(Graphics2D g, Colors c, double length)
	{
		Ellipse2D.Double r = new Ellipse2D.Double(-2, -2, 9, 9);
		g.setColor(c.dark);
		g.setStroke(new BasicStroke(3.4f));
		g.draw(r);
		g.setColor(c.main);
		g.setStroke(new BasicStroke(1.8f));
		g.draw(r);
		g.setColor(c.light);
		g.setStroke(new BasicStroke(0.7f));
		g.draw(new Arc2D.Double(-1.5, -1.5, 8, 8, 100, 80, Arc2D.OPEN));
	}

	// ---- skills ----

	/** A rope along the edge with a net hanging from it, rippling a little, and a cork float now and then. */
	private static void net(Graphics2D g, Colors c, double length, long now)
	{
		double mesh = 5;
		int count = Math.max(1, (int) Math.round(length / mesh));
		double step = length / count;
		Path2D.Double net = new Path2D.Double();
		for (int i = 0; i < count; i++)
		{
			double x = i * step;
			double low = 5 + 0.7 * Math.sin(now / 650.0 + x / 9);
			double next = 5 + 0.7 * Math.sin(now / 650.0 + (x + step) / 9);
			net.moveTo(x, 0);
			net.lineTo(x + step, next);
			net.moveTo(x + step, 0);
			net.lineTo(x, low);
		}
		g.setColor(fade(c.dark, 0.8));
		stroke(g, 1.3f);
		g.draw(net);
		g.setColor(c.main);
		stroke(g, 0.6f);
		g.draw(net);
		java.awt.geom.Line2D.Double rope = new java.awt.geom.Line2D.Double(0, 0, length, 0);
		g.setColor(c.dark);
		stroke(g, 2.4f);
		g.draw(rope);
		g.setColor(c.main);
		stroke(g, 1.3f);
		g.draw(rope);
		for (double x = 18; x < length - 8; x += 34)
		{
			sprite(g, new Ellipse2D.Double(x - 2.8, -1.7, 5.6, 3.4), c.second, CardTheme.shade(c.second, Color.BLACK, 0.5));
			g.setColor(CardTheme.shade(c.second, Color.WHITE, 0.45));
			g.fill(new Ellipse2D.Double(x - 1.6, -1.1, 2, 1));
		}
	}

	/** A fish lying along the edge by the corner, nose towards the middle. */
	private static void fish(Graphics2D g, Colors c, double length)
	{
		Color body = c.own(FISH);
		Color dark = CardTheme.shade(body, Color.BLACK, 0.5);
		Path2D.Double tail = new Path2D.Double();
		tail.moveTo(5, 4);
		tail.lineTo(1, 0.8);
		tail.lineTo(2, 4);
		tail.lineTo(1, 7.2);
		tail.closePath();
		sprite(g, tail, CardTheme.shade(body, Color.BLACK, 0.2), dark);
		Path2D.Double fish = new Path2D.Double();
		fish.moveTo(4, 4);
		fish.quadTo(10, -0.6, 17, 4);
		fish.quadTo(10, 8.6, 4, 4);
		fish.closePath();
		sprite(g, fish, body, dark);
		g.setColor(CardTheme.shade(body, Color.WHITE, 0.45));
		stroke(g, 0.6f);
		g.draw(new java.awt.geom.Line2D.Double(7, 5.4, 13.5, 5.4));
		g.setColor(dark);
		g.draw(new Arc2D.Double(11.5, 2, 3, 4, 60, -120, Arc2D.OPEN));
		g.fill(new Ellipse2D.Double(14.2, 3.2, 1.3, 1.3));
	}

	/** Two logs meeting in the corner like the walls of a cabin, their cut ends showing. */
	private static void logs(Graphics2D g, Colors c, double length)
	{
		Color end = c.own(WOOD);
		for (int i = 0; i < 2; i++)
		{
			AffineTransform saved = g.getTransform();
			if (i == 0)
			{
				// the one down the side, under the other
				g.transform(new AffineTransform(0, 1, 1, 0, 0, 0));
			}
			sprite(g, new java.awt.geom.RoundRectangle2D.Double(1, 0, 19, 4.6, 3, 3), c.main, c.dark);
			g.setColor(c.dark);
			stroke(g, 0.6f);
			g.draw(new java.awt.geom.Line2D.Double(6, 1.6, 11, 1.6));
			g.draw(new java.awt.geom.Line2D.Double(13, 3.1, 18, 3.1));
			g.setColor(c.light);
			g.draw(new java.awt.geom.Line2D.Double(8, 0.8, 15, 0.8));
			sprite(g, new Ellipse2D.Double(0, 0, 3.4, 4.6), end, c.dark);
			g.setColor(CardTheme.shade(end, Color.BLACK, 0.35));
			g.draw(new Ellipse2D.Double(0.9, 1.1, 1.6, 2.4));
			g.setTransform(saved);
		}
	}

	/** A hatchet lying over the middle of the top edge. */
	private static void hatchet(Graphics2D g, Colors c, double length)
	{
		AffineTransform saved = g.getTransform();
		g.rotate(Math.toRadians(-14));
		g.scale(1.65, 1.65);
		Color iron = c.own(IRON);
		Color dark = CardTheme.shade(iron, Color.BLACK, 0.55);
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(-11, 0.2, 17, 2, 1.5, 1.5), c.own(WOOD), c.dark);
		Path2D.Double head = new Path2D.Double();
		head.moveTo(3.5, -1);
		head.lineTo(7, -2.6);
		head.quadTo(10.5, 1.2, 7.5, 5.8);
		head.lineTo(3.5, 3.4);
		head.closePath();
		sprite(g, head, iron, dark);
		g.setColor(CardTheme.shade(iron, Color.WHITE, 0.6));
		stroke(g, 0.7f);
		g.draw(new java.awt.geom.QuadCurve2D.Double(7.4, -1.6, 9.6, 1.4, 7.7, 4.6));
		g.setTransform(saved);
	}

	/**
	 * Fire: tongues of flame licking up over the top edge and standing in each corner, flickering, with a bed of
	 * glowing coals along the bottom.
	 */
	private static void flames(Graphics2D g, Rectangle box, double scale, Colors c, long now)
	{
		Color outer = c.main;
		Color middle = c.tint ? c.light : GOLD;
		Color core = c.tint ? Color.WHITE : new Color(255, 238, 160);
		AffineTransform saved = g.getTransform();
		double inset = 16 * scale;
		double across = (box.width - 1 - inset * 2) / scale;
		if (across > 8)
		{
			g.translate(box.x + inset, box.y);
			g.scale(scale, scale);
			int i = 0;
			for (double x = 4; x < across - 2; x += 9, i++)
			{
				flame(g, x, 1.5, 4.6, 5 + 2.4 * flicker(now, i), Math.sin(now / 260.0 + i) * 0.9, outer, middle, core);
			}
			g.setTransform(saved);
			// the coals, glowing up and down
			g.translate(box.x + inset, box.y + box.height - 1);
			g.scale(scale, scale);
			i = 0;
			for (double x = 3; x < across - 2; x += 5.5, i++)
			{
				double glow = flicker(now / 3, i * 3);
				Color coal = CardTheme.shade(CardTheme.shade(outer, Color.BLACK, 0.55), middle, glow * 0.8);
				sprite(g, new Ellipse2D.Double(x - 2.4, -3.2 + (i % 2) * 0.5, 4.8, 2.8), coal, CardTheme.shade(outer, Color.BLACK, 0.7));
			}
			g.setTransform(saved);
		}
		int[] corner = {0};
		corners(g, box, scale, c, (q, k, l) ->
		{
			int at = corner[0]++;
			flame(q, 6, 8, 8, 9 + 3 * flicker(now, at * 5 + 2), Math.sin(now / 300.0 + at) * 1.2, outer, middle, core);
		}, true);
	}

	/** Between 0 and 1, wobbling irregularly, different for each i. */
	private static double flicker(long now, int i)
	{
		return 0.5 + 0.3 * Math.sin(now / 140.0 + i * 1.7) + 0.2 * Math.sin(now / 83.0 + i * 2.9);
	}

	/** A tongue of flame with its foot at x,y, rising height above it, its tip leaning by lean. */
	private static void flame(Graphics2D g, double x, double y, double width, double height, double lean,
		Color outer, Color middle, Color core)
	{
		double[][] layers = {{1, 1}, {0.62, 0.7}, {0.3, 0.42}};
		Color[] colors = {outer, middle, core};
		for (int i = 0; i < layers.length; i++)
		{
			double w = width * layers[i][0] / 2;
			double h = height * layers[i][1];
			Path2D.Double f = new Path2D.Double();
			f.moveTo(x - w, y);
			f.curveTo(x - w * 1.1, y - h * 0.45, x + lean - w * 0.2, y - h * 0.7, x + lean, y - h);
			f.curveTo(x + lean + w * 0.4, y - h * 0.6, x + w * 1.1, y - h * 0.4, x + w, y);
			f.quadTo(x, y + w * 0.5, x - w, y);
			f.closePath();
			g.setColor(i == 0 ? fade(colors[i], 0.92) : colors[i]);
			g.fill(f);
		}
	}

	/** A little potion flask standing up out of the middle of the top edge. */
	private static void vial(Graphics2D g, Colors c, double length)
	{
		Color glass = new Color(220, 236, 240, 150);
		Color line = c.own(new Color(60, 70, 74));
		AffineTransform saved = g.getTransform();
		g.scale(1.45, 1.45);
		g.translate(0, -1);
		Ellipse2D.Double bulb = new Ellipse2D.Double(-4.2, -6.5, 8.4, 8.4);
		Rectangle2D.Double neck = new Rectangle2D.Double(-1.5, -10, 3, 4.5);
		g.setColor(glass);
		g.fill(bulb);
		g.fill(neck);
		Shape clip = g.getClip();
		g.clip(new Rectangle2D.Double(-5, -3.6, 10, 6));
		g.setColor(c.second);
		g.fill(bulb);
		g.setClip(clip);
		g.setColor(line);
		stroke(g, 0.8f);
		g.draw(bulb);
		g.draw(neck);
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(-1.9, -12, 3.8, 2.4, 1, 1), c.own(BARK), line);
		g.setColor(new Color(255, 255, 255, 200));
		g.fill(new Ellipse2D.Double(-2.6, -4.8, 1.4, 2));
		g.setTransform(saved);
	}

	/** A rune stone standing in the corner with an element's glyph glowing on it: air, water, earth, fire. */
	private static void rune(Graphics2D g, Colors c, int which, long now)
	{
		Color[] elements = {new Color(236, 240, 255), new Color(80, 150, 255), new Color(110, 200, 80), new Color(255, 110, 50)};
		Color glow = c.own(elements[which % 4]);
		double pulse = 0.55 + 0.45 * Math.sin(now / 500.0 + which * 1.6);
		// a halo the colour of its element, breathing with the pulse
		g.setColor(fade(glow, 0.12 + 0.18 * pulse));
		g.fill(new Ellipse2D.Double(-1.5, -2.2, 13.4, 12.4));
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(0.8, 0, 8.8, 8, 3.6, 3.6), c.main, c.dark);
		g.setColor(c.light);
		stroke(g, 0.5f);
		g.draw(new Arc2D.Double(1.6, 0.6, 7, 6, 90, 70, Arc2D.OPEN));
		g.setColor(fade(glow, 0.25 * pulse));
		g.fill(new Ellipse2D.Double(1.2, 0, 8, 8));
		g.setColor(fade(glow, 0.6 + 0.4 * pulse));
		stroke(g, 1f);
		double x = 5.2;
		double y = 4;
		switch (which % 4)
		{
			case 0:
				// air: a ring round a dot
				g.draw(new Ellipse2D.Double(x - 2.2, y - 2.2, 4.4, 4.4));
				g.fill(new Ellipse2D.Double(x - 0.7, y - 0.7, 1.4, 1.4));
				break;
			case 1:
			{
				// water: a drop
				Path2D.Double drop = new Path2D.Double();
				drop.moveTo(x, y - 2.6);
				drop.quadTo(x + 2.4, y + 0.4, x, y + 2.3);
				drop.quadTo(x - 2.4, y + 0.4, x, y - 2.6);
				g.fill(drop);
				break;
			}
			case 2:
			{
				// earth: a square on its point
				Path2D.Double square = new Path2D.Double();
				square.moveTo(x - 2.2, y);
				square.lineTo(x, y - 2.2);
				square.lineTo(x + 2.2, y);
				square.lineTo(x, y + 2.2);
				square.closePath();
				g.draw(square);
				break;
			}
			default:
			{
				// fire: a point up
				Path2D.Double fire = new Path2D.Double();
				fire.moveTo(x, y - 2.6);
				fire.lineTo(x + 2.2, y + 1.9);
				fire.lineTo(x - 2.2, y + 1.9);
				fire.closePath();
				g.fill(fire);
				break;
			}
		}
	}

	/** Specks of rune essence along the edge, glowing one after another. */
	private static void essence(Graphics2D g, Colors c, double length, long now)
	{
		// a faint seam of light the specks sit on
		g.setColor(fade(c.second, 0.22));
		stroke(g, 0.6f);
		g.draw(new java.awt.geom.Line2D.Double(9, 1, length - 9, 1));
		int i = 0;
		for (double x = 9; x < length - 4; x += 14, i++)
		{
			double glow = 0.3 + 0.7 * Math.max(0, Math.sin((now / 1100.0 - i * 0.21) * Math.PI * 2));
			double r = 1.1 + 0.6 * glow;
			Path2D.Double d = new Path2D.Double();
			d.moveTo(x, 1 - r);
			d.lineTo(x + r, 1);
			d.lineTo(x, 1 + r);
			d.lineTo(x - r, 1);
			d.closePath();
			g.setColor(fade(c.second, 0.25 * glow));
			g.fill(new Ellipse2D.Double(x - r * 2.2, 1 - r * 2.2, r * 4.4, r * 4.4));
			g.setColor(fade(c.second, glow));
			g.fill(d);
		}
	}

	/** A bone from one point to another, its knobbly ends a little past them; size 1 for the corner ones. */
	private static void bone(Graphics2D g, Colors c, double x1, double y1, double x2, double y2, double size)
	{
		double length = Math.hypot(x2 - x1, y2 - y1);
		double nx = -(y2 - y1) / length * 1.1 * size;
		double ny = (x2 - x1) / length * 1.1 * size;
		java.awt.geom.Line2D.Double shaft = new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
		double r = 1.5 * size;
		for (int coat = 0; coat < 2; coat++)
		{
			// the outline first, then the bone over it
			g.setColor(coat == 0 ? c.dark : c.main);
			stroke(g, (float) ((coat == 0 ? 2.8 : 1.8) * size));
			g.draw(shaft);
			double grow = coat == 0 ? 0.55 * size : 0;
			for (double[] end : new double[][]{{x1, y1}, {x2, y2}})
			{
				for (int side = -1; side <= 1; side += 2)
				{
					g.fill(new Ellipse2D.Double(end[0] + side * nx - r - grow, end[1] + side * ny - r - grow, (r + grow) * 2,
						(r + grow) * 2));
				}
			}
		}
		g.setColor(c.light);
		stroke(g, (float) (0.6 * size));
		g.draw(new java.awt.geom.Line2D.Double(x1 + (x2 - x1) * 0.25 + nx * 0.3, y1 + (y2 - y1) * 0.25 + ny * 0.3,
			x1 + (x2 - x1) * 0.7 + nx * 0.3, y1 + (y2 - y1) * 0.7 + ny * 0.3));
	}

	/** Two arrows crossed in the corner, one shot in from the corner and one across it, all inside the card. */
	private static void arrows(Graphics2D g, Colors c, double length)
	{
		arrow(g, c, 1.5, 12, 12, 1.5);
		arrow(g, c, 1, 1, 12.5, 12.5);
	}

	/** An arrow with its flights at x1,y1 and its head at x2,y2. */
	private static void arrow(Graphics2D g, Colors c, double x1, double y1, double x2, double y2)
	{
		Color steel = c.own(STEEL);
		double length = Math.hypot(x2 - x1, y2 - y1);
		AffineTransform saved = g.getTransform();
		g.translate(x1, y1);
		g.rotate(Math.atan2(y2 - y1, x2 - x1));
		java.awt.geom.Line2D.Double shaft = new java.awt.geom.Line2D.Double(0.5, 0, length - 3.5, 0);
		g.setColor(c.dark);
		stroke(g, 1.9f);
		g.draw(shaft);
		g.setColor(c.main);
		stroke(g, 0.9f);
		g.draw(shaft);
		Path2D.Double head = new Path2D.Double();
		head.moveTo(length, 0);
		head.lineTo(length - 4, -2);
		head.lineTo(length - 3.2, 0);
		head.lineTo(length - 4, 2);
		head.closePath();
		sprite(g, head, steel, CardTheme.shade(steel, Color.BLACK, 0.55));
		for (int side = -1; side <= 1; side += 2)
		{
			Path2D.Double feather = new Path2D.Double();
			feather.moveTo(0, 0);
			feather.lineTo(-0.6, side * 2.1);
			feather.lineTo(3.6, side * 2.1);
			feather.lineTo(5.2, 0);
			feather.closePath();
			sprite(g, feather, c.second, CardTheme.shade(c.second, Color.BLACK, 0.5));
		}
		g.setTransform(saved);
	}

	/** A shortbow standing over the middle of the top edge, an arrow nocked on its string pointing up. */
	private static void bow(Graphics2D g, Colors c, double length)
	{
		AffineTransform saved = g.getTransform();
		g.scale(1.5, 1.5);
		Path2D.Double bow = new Path2D.Double();
		bow.moveTo(-12, 1);
		bow.curveTo(-8.5, -1.5, -5, -7, 0, -7);
		bow.curveTo(5, -7, 8.5, -1.5, 12, 1);
		Color wood = c.own(WOOD);
		Color dark = CardTheme.shade(c.own(BARK), Color.BLACK, 0.45);
		// the string drawn back a little to the nock
		g.setColor(c.own(new Color(240, 236, 220)));
		stroke(g, 0.6f);
		Path2D.Double string = new Path2D.Double();
		string.moveTo(-11.7, 0.8);
		string.lineTo(0, 1.6);
		string.lineTo(11.7, 0.8);
		g.draw(string);
		g.setColor(dark);
		stroke(g, 2.6f);
		g.draw(bow);
		g.setColor(wood);
		stroke(g, 1.5f);
		g.draw(bow);
		g.setColor(CardTheme.shade(wood, Color.WHITE, 0.45));
		stroke(g, 0.5f);
		g.draw(new java.awt.geom.QuadCurve2D.Double(-6, -5.6, 0, -7.6, 6, -5.6));
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(-1.6, -8.4, 3.2, 3, 1, 1), c.own(BARK), dark);
		arrow(g, c, 0, 1.6, 0, -13);
		g.setTransform(saved);
	}

	// ---- the game's own art ----

	/** An item icon centred on x,y with its longer side the given size, turned by an angle. */
	private static void icon(Graphics2D g, BufferedImage image, double x, double y, double size, double angle)
	{
		Rectangle part = CardArt.bounds(image);
		if (part == null)
		{
			return;
		}
		double fit = size / Math.max(part.width, part.height);
		AffineTransform saved = g.getTransform();
		g.translate(x, y);
		g.rotate(angle);
		g.scale(fit, fit);
		g.translate(-part.width / 2.0, -part.height / 2.0);
		g.drawImage(image, 0, 0, part.width, part.height, part.x, part.y, part.x + part.width, part.y + part.height, null);
		g.setTransform(saved);
	}

	/**
	 * An abyssal whip laid round the card: its grip along the top edge from the top left corner, the knobbly lash
	 * running all the way round, thinning as it goes, to the barbed tip coming back up the left edge.
	 */
	private static void whip(Graphics2D g, Rectangle box, double scale, Colors c)
	{
		double in = 1.5 * scale;
		double x0 = box.x + in;
		double y0 = box.y + in;
		double x1 = box.x + box.width - 1 - in;
		double y1 = box.y + box.height - 1 - in;
		double r = Math.min(7 * scale, Math.min(x1 - x0, y1 - y0) / 2);
		double grip = 21.5 * scale;
		Path2D.Double path = new Path2D.Double();
		path.moveTo(x0 + grip, y0);
		path.lineTo(x1 - r, y0);
		path.quadTo(x1, y0, x1, y0 + r);
		path.lineTo(x1, y1 - r);
		path.quadTo(x1, y1, x1 - r, y1);
		path.lineTo(x0 + r, y1);
		path.quadTo(x0, y1, x0, y1 - r);
		path.lineTo(x0, y0 + 14 * scale);

		// even steps along the path, swaying a little to either side
		java.util.List<double[]> points = new java.util.ArrayList<>();
		double[] seg = new double[6];
		double px = 0;
		double py = 0;
		double carried = 0;
		double step = 2.6 * scale;
		for (java.awt.geom.PathIterator it = path.getPathIterator(null, 0.2); !it.isDone(); it.next())
		{
			int type = it.currentSegment(seg);
			if (type == java.awt.geom.PathIterator.SEG_MOVETO)
			{
				points.add(new double[]{seg[0], seg[1], 1, 0});
			}
			else
			{
				double dx = seg[0] - px;
				double dy = seg[1] - py;
				double length = Math.hypot(dx, dy);
				double at = step - carried;
				while (at <= length)
				{
					points.add(new double[]{px + dx * at / length, py + dy * at / length, dx / length, dy / length});
					at += step;
				}
				carried = length - (at - step);
			}
			px = seg[0];
			py = seg[1];
		}
		int n = points.size();
		if (n < 8)
		{
			return;
		}
		Color outline = c.own(new Color(24, 12, 14));
		Color deep = CardTheme.shade(c.main, Color.BLACK, 0.3);
		// a ripple cracks down the lash from the grip every few seconds
		double crack = (System.currentTimeMillis() % 3600) / 1500.0 * n;
		AffineTransform saved = g.getTransform();

		// the lash: a row of overlapping knobs, outlines first so they join up, then the knobs and their shine
		for (int pass = 0; pass < 3; pass++)
		{
			for (int i = 0; i < n; i++)
			{
				double[] p = points.get(i);
				double sway = Math.sin(i / 4.0 * Math.PI) * 0.5 * scale * (1 - (double) i / n)
					+ Math.exp(-Math.pow((i - crack) / 3.5, 2)) * 1.4 * scale * (0.4 + 0.6 * i / n);
				double half = (3.0 - 1.5 * i / n) * scale;
				g.translate(p[0] - p[3] * sway, p[1] + p[2] * sway);
				g.rotate(Math.atan2(p[3], p[2]));
				if (pass == 0)
				{
					g.setColor(outline);
					double o = 0.6 * scale;
					g.fill(new Ellipse2D.Double(-half * 0.9 - o, -half - o, half * 1.8 + o * 2, half * 2 + o * 2));
				}
				else if (pass == 1)
				{
					g.setColor(i % 2 == 0 ? c.main : deep);
					g.fill(new Ellipse2D.Double(-half * 0.9, -half, half * 1.8, half * 2));
				}
				else if (i % 2 == 0)
				{
					g.setColor(c.light);
					g.fill(new Ellipse2D.Double(-half * 0.4, -half * 0.75, half * 0.8, half * 0.6));
				}
				g.setTransform(saved);
			}
		}

		// the barbed tip, pointing on up the left edge
		double[] end = points.get(n - 1);
		g.translate(end[0], end[1]);
		g.rotate(Math.atan2(end[3], end[2]));
		g.scale(scale, scale);
		Path2D.Double barb = new Path2D.Double();
		barb.moveTo(6, 0);
		barb.lineTo(-1.5, -3.4);
		barb.lineTo(0.6, 0);
		barb.lineTo(-1.5, 3.4);
		barb.closePath();
		sprite(g, barb, c.main, outline);
		g.setTransform(saved);

		// the grip along the top edge, its pommel in the corner and a red collar where the lash comes out
		g.translate(x0, y0);
		g.scale(scale, scale);
		Color dark = c.own(new Color(48, 46, 54));
		Color band = c.own(new Color(104, 100, 112));
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(3, -3, 17, 6, 3, 3), dark, outline);
		g.setColor(band);
		stroke(g, 0.9f);
		for (double x = 6.5; x < 19; x += 2.6)
		{
			g.draw(new java.awt.geom.Line2D.Double(x, -2.2, x - 1, 2.2));
		}
		sprite(g, new java.awt.geom.RoundRectangle2D.Double(18.5, -3.6, 3.2, 7.2, 1.5, 1.5), deep, outline);
		sprite(g, new Ellipse2D.Double(-1.4, -3.5, 7, 7), dark, outline);
		g.setColor(band);
		g.fill(new Ellipse2D.Double(0.4, -2.3, 2.2, 2));
		g.setTransform(saved);
	}

	/**
	 * The stone ring and its two dragons from around the fixed mode minimap, drawn round the card: the corners keep
	 * their shape, with the dragons' heads meeting in the middle of the bottom, and only the plain stone between is
	 * stretched to the card's size. False when the client hasn't handed over the art.
	 */
	private static boolean dragons(Graphics2D g, Rectangle box, double scale, Color tint)
	{
		BufferedImage art = CardArt.dragons();
		if (art == null)
		{
			return false;
		}
		// the parts that keep their shape, across and down; the rest is stretched
		double[] across = {0, 0.3, 0.38, 0.62, 0.7, 1};
		double[] down = {0, 0.3, 0.36, 1};
		double fixedW = art.getWidth() * (1 - 0.08 - 0.08);
		double fixedH = art.getHeight() * (1 - 0.06);
		double size = Math.min(0.42 * scale, Math.min(box.width / fixedW, box.height / fixedH));
		// the stone sits mostly outside the card so it doesn't cover the text
		int out = (int) Math.round(16 * size);
		// and further down, where the heads reach in
		int low = (int) Math.round(34 * size);
		Rectangle area = new Rectangle(box.x - out, box.y - out, box.width + out * 2, box.height + out + low);
		int w = (int) Math.round(art.getWidth() * size);
		int h = (int) Math.round(art.getHeight() * size);
		BufferedImage sized = CardArt.dragons(w, h, tint);
		if (sized == null || w < 24 || h < 20)
		{
			return true;
		}
		int[] sx = cuts(across, w);
		int[] sy = cuts(down, h);
		int[] dx = place(sx, new boolean[]{false, true, false, true, false}, area.x, area.width);
		int[] dy = place(sy, new boolean[]{false, true, false}, area.y, area.height);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		for (int j = 0; j < sy.length - 1; j++)
		{
			for (int i = 0; i < sx.length - 1; i++)
			{
				if (sx[i + 1] > sx[i] && sy[j + 1] > sy[j])
				{
					g.drawImage(sized, dx[i], dy[j], dx[i + 1], dy[j + 1], sx[i], sy[j], sx[i + 1], sy[j + 1], null);
				}
			}
		}
		return true;
	}

	private static int[] cuts(double[] at, int size)
	{
		int[] out = new int[at.length];
		for (int i = 0; i < at.length; i++)
		{
			out[i] = (int) Math.round(at[i] * size);
		}
		return out;
	}

	/** Where each part lands: the fixed ones keep their size and the stretched ones share what is left. */
	private static int[] place(int[] cuts, boolean[] stretched, int start, int length)
	{
		int fixed = 0;
		int count = 0;
		for (int i = 0; i < stretched.length; i++)
		{
			if (stretched[i])
			{
				count++;
			}
			else
			{
				fixed += cuts[i + 1] - cuts[i];
			}
		}
		double share = Math.max(0, length - fixed) / (double) Math.max(1, count);
		int[] out = new int[cuts.length];
		double at = start;
		out[0] = start;
		for (int i = 0; i < stretched.length; i++)
		{
			at += stretched[i] ? share : cuts[i + 1] - cuts[i];
			out[i + 1] = (int) Math.round(at);
		}
		out[cuts.length - 1] = start + length;
		return out;
	}

	private static Color fade(Color color, double alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(color.getAlpha() * Math.max(0, Math.min(1, alpha))));
	}
}
