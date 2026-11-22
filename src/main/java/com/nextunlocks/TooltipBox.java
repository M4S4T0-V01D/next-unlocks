package com.nextunlocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Wraps the tooltip panel so it keeps its own background: RuneLite's tooltip overlay replaces the
 * background of any PanelComponent it is given with the default overlay color. Draws the theme's frame, header band,
 * scanlines and glow around the panel, and can be stretched taller than its content so boxes side by side line up.
 */
class TooltipBox implements LayoutableRenderableEntity
{
	private final PanelComponent panel;
	@Getter
	private final CardTheme theme;
	/** The blurred border behind the frame, kept until the size changes. */
	private BufferedImage glow;
	private Dimension glowSize;
	private Point location = new Point();
	/** Panel size from the last render, before stretching; the panel lays out from its previous frame too. */
	@Getter
	private Dimension last;
	/** Height to stretch to when the content is shorter; 0 for the content's own height. */
	@Setter
	private int minHeight;

	/** Rewrite the xp numbers and progress for new xp. */
	private final List<IntConsumer> xpUpdates;

	TooltipBox(PanelComponent panel, CardTheme theme, List<IntConsumer> xpUpdates)
	{
		this.panel = panel;
		this.theme = theme;
		this.xpUpdates = xpUpdates;
		int padding = theme.getPadding();
		panel.setBorder(new Rectangle(padding, padding, padding, padding));
	}

	/** Update the xp shown without rebuilding, so the box keeps its size and place. */
	void updateXp(int xp)
	{
		xpUpdates.forEach(u -> u.accept(xp));
	}

	/** The icon tiles inside, where they were drawn last. */
	List<UnlockTileComponent> tiles()
	{
		List<UnlockTileComponent> tiles = new ArrayList<>();
		collect(panel, tiles);
		return tiles;
	}

	/** The progress bars inside, if any. */
	List<PinBar> bars()
	{
		List<PinBar> bars = new ArrayList<>();
		for (LayoutableRenderableEntity child : panel.getChildren())
		{
			if (child instanceof PinBar)
			{
				bars.add((PinBar) child);
			}
		}
		return bars;
	}

	private static void collect(PanelComponent panel, List<UnlockTileComponent> tiles)
	{
		for (LayoutableRenderableEntity child : panel.getChildren())
		{
			if (child instanceof UnlockTileComponent)
			{
				tiles.add((UnlockTileComponent) child);
			}
			else if (child instanceof PanelComponent)
			{
				collect((PanelComponent) child, tiles);
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// the frame goes on first, sized to fit the stretched height, then the panel's content over it
		if (last != null && last.width > 0)
		{
			paintFrame(graphics, new Rectangle(location, new Dimension(last.width, Math.max(last.height, minHeight))));
		}
		panel.setBackgroundColor(null);
		Dimension size = panel.render(graphics);
		if (last == null && size.width > 0)
		{
			// first frame: nothing to size the frame by yet, so it goes over the content's height
			last = size;
			return render(graphics);
		}
		last = size;
		Dimension stretched = new Dimension(size.width, Math.max(size.height, minHeight));
		if (stretched.width > 0 && stretched.height > 0)
		{
			paintOver(graphics, new Rectangle(location, stretched));
		}
		return stretched;
	}

	/** Glow, background and header band, behind the content. */
	private void paintFrame(Graphics2D graphics, Rectangle box)
	{
		Color color = graphics.getColor();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Color border = theme.getBorder();
		if (theme.isGlowBorder() && theme.getGlow() != null && border != null)
		{
			if (glow == null || !box.getSize().equals(glowSize))
			{
				glow = Glow.paint(box.width, box.height, theme.getGlowSize(), g ->
				{
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(theme.getGlow());
					g.setStroke(new BasicStroke(2));
					g.draw(outline(new Rectangle(0, 0, box.width, box.height)));
				});
				glowSize = box.getSize();
			}
			int pad = Glow.pad(theme.getGlowSize());
			graphics.drawImage(glow, box.x - pad, box.y - pad, null);
		}

		Paint fill = theme.getBackground2() == null ? theme.getBackground()
			: new GradientPaint(0, box.y, theme.getBackground(), 0, box.y + box.height, theme.getBackground2());
		Paint paint = graphics.getPaint();
		switch (theme.getFrame())
		{
			case CLASSIC:
				if (theme.getBackground2() != null)
				{
					// RuneLite's box can't take a gradient
					graphics.setPaint(fill);
					graphics.fillRect(box.x, box.y, box.width, box.height);
					break;
				}
				BackgroundComponent back = new BackgroundComponent();
				back.setRectangle(box);
				back.setBackgroundColor(theme.getBackground());
				back.render(graphics);
				break;
			case ROUNDED:
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setPaint(fill);
				graphics.fill(new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, corner(), corner()));
				break;
			case NOTCHED:
			case PIXEL:
				graphics.setPaint(fill);
				graphics.fill(outline(box));
				break;
			case SCALLOPED:
			case TICKET:
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setPaint(fill);
				graphics.fill(outline(box));
				break;
			default:
				graphics.setPaint(fill);
				graphics.fillRect(box.x, box.y, box.width, box.height);
				break;
		}
		graphics.setPaint(paint);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);

		Rectangle header = header();
		if (theme.getHeaderBand() != null && header != null)
		{
			// inside the border, however thick
			int inset = theme.getFrame() == CardTheme.Frame.BEVEL ? 2 : Math.max(2, theme.getBorderWidth() + 1);
			int top = box.y + inset;
			int bottom = header.y + header.height + 1;
			if (bottom > top)
			{
				Shape clip = graphics.getClip();
				graphics.clip(outline(box));
				graphics.setColor(theme.getHeaderBand());
				graphics.fillRect(box.x + inset, top, box.width - inset * 2, bottom - top);
				graphics.setClip(clip);
			}
		}
		CardMotion.ambient(graphics, theme, outline(box), box);
		graphics.setColor(color);
	}

	/** Scanlines and borders, over the content. */
	private void paintOver(Graphics2D graphics, Rectangle box)
	{
		Color color = graphics.getColor();
		Object aa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Stroke stroke = graphics.getStroke();
		if (theme.isScanlines())
		{
			Shape clip = graphics.getClip();
			graphics.clip(outline(box));
			graphics.setColor(new Color(0, 0, 0, 40));
			for (int y = box.y + 1; y < box.y + box.height; y += 2)
			{
				graphics.drawLine(box.x, y, box.x + box.width - 1, y);
			}
			graphics.setClip(clip);
		}
		if (theme.isShimmer())
		{
			shimmer(graphics, box);
		}

		Color border = theme.getBorder();
		int right = box.x + box.width - 1;
		int bottom = box.y + box.height - 1;
		switch (theme.getFrame())
		{
			case BEVEL:
				// raised like a 95 window: light top and left, dark bottom and right, twice over
				graphics.setColor(Color.WHITE);
				graphics.drawLine(box.x, box.y, right - 1, box.y);
				graphics.drawLine(box.x, box.y, box.x, bottom - 1);
				graphics.setColor(Color.BLACK);
				graphics.drawLine(box.x, bottom, right, bottom);
				graphics.drawLine(right, box.y, right, bottom);
				graphics.setColor(new Color(223, 223, 223));
				graphics.drawLine(box.x + 1, box.y + 1, right - 2, box.y + 1);
				graphics.drawLine(box.x + 1, box.y + 1, box.x + 1, bottom - 2);
				graphics.setColor(new Color(128, 128, 128));
				graphics.drawLine(box.x + 1, bottom - 1, right - 1, bottom - 1);
				graphics.drawLine(right - 1, box.y + 1, right - 1, bottom - 1);
				break;
			case ROUNDED:
				if (border != null)
				{
					int w = theme.getBorderWidth();
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.setColor(border);
					graphics.setStroke(new BasicStroke(w));
					graphics.draw(new RoundRectangle2D.Double(box.x + w / 2.0, box.y + w / 2.0, box.width - w, box.height - w, corner(), corner()));
				}
				break;
			case NOTCHED:
			case PIXEL:
			{
				boolean notched = theme.getFrame() == CardTheme.Frame.NOTCHED;
				int w = theme.getBorderWidth();
				if (border != null)
				{
					graphics.setColor(border);
					for (int i = 0; i < w; i++)
					{
						graphics.drawPolygon(notched ? notched(box.x + i, box.y + i, right - i, bottom - i, notch(i))
							: pixel(box.x + i, box.y + i, right - i, bottom - i));
					}
				}
				if (theme.getInnerBorder() != null)
				{
					int in = Math.max(1, w) + 1;
					graphics.setColor(theme.getInnerBorder());
					graphics.drawPolygon(notched ? notched(box.x + in, box.y + in, right - in, bottom - in, notch(in))
						: pixel(box.x + in, box.y + in, right - in, bottom - in));
				}
				break;
			}
			case SCALLOPED:
			case TICKET:
			{
				int w = Math.max(1, theme.getBorderWidth());
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				if (border != null)
				{
					graphics.setColor(border);
					graphics.setStroke(new BasicStroke(w));
					graphics.draw(borderPath(box));
				}
				if (theme.getInnerBorder() != null)
				{
					double in = w + 1.5;
					graphics.setColor(theme.getInnerBorder());
					graphics.setStroke(new BasicStroke(1));
					graphics.draw(theme.getFrame() == CardTheme.Frame.SCALLOPED
						? scalloped(box.x + in, box.y + in, right - in, bottom - in)
						: ticket(box.x + in, box.y + in, right - in, bottom - in));
				}
				break;
			}
			case BRACKETS:
			{
				if (border == null)
				{
					break;
				}
				int t = Math.max(2, theme.getBorderWidth());
				int arm = Math.max(6, Math.min(12, Math.min(box.width, box.height) / 5));
				graphics.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), border.getAlpha() / 3));
				graphics.drawRect(box.x, box.y, box.width - 1, box.height - 1);
				graphics.setColor(border);
				graphics.fillRect(box.x, box.y, arm, t);
				graphics.fillRect(box.x, box.y, t, arm);
				graphics.fillRect(right + 1 - arm, box.y, arm, t);
				graphics.fillRect(right + 1 - t, box.y, t, arm);
				graphics.fillRect(box.x, bottom + 1 - t, arm, t);
				graphics.fillRect(box.x, bottom + 1 - arm, t, arm);
				graphics.fillRect(right + 1 - arm, bottom + 1 - t, arm, t);
				graphics.fillRect(right + 1 - t, bottom + 1 - arm, t, arm);
				break;
			}
			default:
			{
				int w = theme.getBorderWidth();
				CardTheme.Frame frame = theme.getFrame();
				if (border != null)
				{
					graphics.setColor(border);
					if (frame == CardTheme.Frame.DASHED)
					{
						graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 3}, 0));
					}
					for (int i = 0; i < w; i++)
					{
						graphics.drawRect(box.x + i, box.y + i, box.width - 1 - i * 2, box.height - 1 - i * 2);
					}
					graphics.setStroke(stroke);
				}
				// riveted and ornate frames always have an inner line; one the style lacks is the border, darker
				Color inner = theme.getInnerBorder();
				if (inner == null && border != null && (frame == CardTheme.Frame.RIVETED || frame == CardTheme.Frame.ORNATE))
				{
					inner = CardTheme.shade(border, Color.BLACK, 0.45);
				}
				if (inner != null && frame != CardTheme.Frame.CLASSIC && frame != CardTheme.Frame.FLAT)
				{
					int in = Math.max(1, w) + 1;
					graphics.setColor(inner);
					graphics.drawRect(box.x + in, box.y + in, box.width - 1 - in * 2, box.height - 1 - in * 2);
				}
				if (border != null && frame == CardTheme.Frame.RIVETED)
				{
					rivets(graphics, box, border);
				}
				if (border != null && frame == CardTheme.Frame.ORNATE)
				{
					ornaments(graphics, box, border, inner);
				}
				break;
			}
		}
		CardTrim.paint(graphics, theme, box);
		CardMotion.border(graphics, theme, borderPath(box), box);
		graphics.setStroke(stroke);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
		graphics.setColor(color);
	}

	/** A slanted band of light sweeping across now and then; cards further right catch it a little later. */
	private void shimmer(Graphics2D graphics, Rectangle box)
	{
		long period = 4200;
		double t = ((System.currentTimeMillis() - box.x * 2L) % period + period) % period / 1300.0;
		if (t >= 1)
		{
			return;
		}
		int band = 22;
		float x = (float) (box.x - band - box.height + t * (box.width + band * 2 + box.height));
		Path2D.Float shape = new Path2D.Float();
		shape.moveTo(x + box.height, box.y);
		shape.lineTo(x + box.height + band, box.y);
		shape.lineTo(x + band, box.y + box.height);
		shape.lineTo(x, box.y + box.height);
		shape.closePath();
		Shape clip = graphics.getClip();
		Paint paint = graphics.getPaint();
		graphics.clip(outline(box));
		Color clear = new Color(255, 255, 255, 0);
		float mid = x + box.height / 2f;
		graphics.setPaint(new LinearGradientPaint(mid, 0, mid + band, 0, new float[]{0, 0.5f, 1},
			new Color[]{clear, new Color(255, 255, 255, 70), clear}));
		graphics.fill(shape);
		graphics.setPaint(paint);
		graphics.setClip(clip);
	}

	/** Bolts at the corners and every so often along the edges. */
	private static void rivets(Graphics2D graphics, Rectangle box, Color border)
	{
		Color dark = CardTheme.shade(border, Color.BLACK, 0.55);
		Color light = CardTheme.shade(border, Color.WHITE, 0.45);
		int size = 4;
		int right = box.x + box.width - size;
		int bottom = box.y + box.height - size;
		int across = Math.max(1, (box.width - size) / 56);
		int down = Math.max(1, (box.height - size) / 56);
		for (int i = 0; i <= across; i++)
		{
			int x = box.x + (right - box.x) * i / across;
			rivet(graphics, x, box.y, dark, light);
			rivet(graphics, x, bottom, dark, light);
		}
		for (int i = 1; i < down; i++)
		{
			int y = box.y + (bottom - box.y) * i / down;
			rivet(graphics, box.x, y, dark, light);
			rivet(graphics, right, y, dark, light);
		}
	}

	private static void rivet(Graphics2D graphics, int x, int y, Color dark, Color light)
	{
		graphics.setColor(dark);
		graphics.fillRect(x + 1, y, 2, 4);
		graphics.fillRect(x, y + 1, 4, 2);
		graphics.setColor(light);
		graphics.fillRect(x + 1, y + 1, 2, 2);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(x + 1, y + 1, 1, 1);
	}

	/** Diamonds where the two lines meet at the corners, and smaller ones halfway along the top and bottom. */
	private static void ornaments(Graphics2D graphics, Rectangle box, Color border, Color inner)
	{
		Color fill = CardTheme.shade(border, Color.WHITE, 0.35);
		Color middle = inner != null ? inner : CardTheme.shade(border, Color.BLACK, 0.45);
		int right = box.x + box.width - 1;
		int bottom = box.y + box.height - 1;
		diamond(graphics, box.x + 3, box.y + 3, 3, fill, middle);
		diamond(graphics, right - 3, box.y + 3, 3, fill, middle);
		diamond(graphics, box.x + 3, bottom - 3, 3, fill, middle);
		diamond(graphics, right - 3, bottom - 3, 3, fill, middle);
		int mid = box.x + box.width / 2;
		diamond(graphics, mid, box.y + 2, 2, fill, middle);
		diamond(graphics, mid, bottom - 2, 2, fill, middle);
	}

	private static void diamond(Graphics2D graphics, int x, int y, int r, Color fill, Color middle)
	{
		graphics.setColor(fill);
		for (int d = -r; d <= r; d++)
		{
			int half = r - Math.abs(d);
			graphics.fillRect(x - half, y + d, half * 2 + 1, 1);
		}
		graphics.setColor(middle);
		graphics.fillRect(x, y, 1, 1);
	}

	private int corner()
	{
		return theme.getCorner() * 2;
	}

	/** How far the cut corners go in, at an inset; the corner setting makes them bigger. */
	private int notch(int inset)
	{
		int cut = Math.max(4, theme.getCorner() + 2);
		return Math.max(1, cut - (int) Math.round(inset * 0.414));
	}

	/** An eight-sided box with its corners cut by n. */
	private static Polygon notched(int x, int y, int right, int bottom, int n)
	{
		return new Polygon(
			new int[]{x + n, right - n, right, right, right - n, x + n, x, x},
			new int[]{y, y, y + n, bottom - n, bottom, bottom, bottom - n, y + n}, 8);
	}

	/** A box with corners stepped two pixels at a time. */
	private static Polygon pixel(int x, int y, int right, int bottom)
	{
		int s = 2;
		return new Polygon(
			new int[]{x + 2 * s, right - 2 * s, right - 2 * s, right - s, right - s, right, right, right - s, right - s,
				right - 2 * s, right - 2 * s, x + 2 * s, x + 2 * s, x + s, x + s, x, x, x + s, x + s, x + 2 * s},
			new int[]{y, y, y + s, y + s, y + 2 * s, y + 2 * s, bottom - 2 * s, bottom - 2 * s, bottom - s, bottom - s,
				bottom, bottom, bottom - s, bottom - s, bottom - 2 * s, bottom - 2 * s, y + 2 * s, y + 2 * s, y + s, y + s}, 20);
	}

	/** A box whose edges are rows of half circles bulging out, their tops on the box; the corner setting makes them bigger. */
	private Shape scalloped(double x, double y, double right, double bottom)
	{
		double r = Math.max(4, Math.min(9, 4.5 + theme.getCorner() / 2.0));
		Area area = new Area(new Rectangle2D.Double(x + r, y + r, Math.max(0, right - x - r * 2), Math.max(0, bottom - y - r * 2)));
		bumps(area, x + r, right - r, y + r, r, true);
		bumps(area, x + r, right - r, bottom - r, r, true);
		bumps(area, y + r, bottom - r, x + r, r, false);
		bumps(area, y + r, bottom - r, right - r, r, false);
		return area;
	}

	/** Circles from one end of an edge to the other, spaced to fit it exactly. */
	private static void bumps(Area area, double from, double to, double at, double r, boolean across)
	{
		int n = Math.max(1, (int) Math.ceil((to - from) / (r * 2)));
		double step = (to - from) / n;
		for (int i = 0; i <= n; i++)
		{
			double c = from + i * step;
			area.add(new Area(across ? new Ellipse2D.Double(c - r, at - r, r * 2, r * 2)
				: new Ellipse2D.Double(at - r, c - r, r * 2, r * 2)));
		}
	}

	/** A box with a quarter circle bitten out of each corner, like a ticket stub. */
	private Shape ticket(double x, double y, double right, double bottom)
	{
		double r = Math.max(4, Math.min(Math.min(right - x, bottom - y) / 3, theme.getCorner() + 7));
		Area area = new Area(new Rectangle2D.Double(x, y, right - x, bottom - y));
		for (double[] c : new double[][]{{x, y}, {right, y}, {x, bottom}, {right, bottom}})
		{
			area.subtract(new Area(new Ellipse2D.Double(c[0] - r, c[1] - r, r * 2, r * 2)));
		}
		return area;
	}

	/** The frame's outline shape, for the glow and for clipping. */
	private Shape outline(Rectangle box)
	{
		switch (theme.getFrame())
		{
			case ROUNDED:
				return new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, corner(), corner());
			case NOTCHED:
				return notched(box.x, box.y, box.x + box.width, box.y + box.height, notch(0));
			case PIXEL:
				return pixel(box.x, box.y, box.x + box.width, box.y + box.height);
			case SCALLOPED:
				return scalloped(box.x, box.y, box.x + box.width, box.y + box.height);
			case TICKET:
				return ticket(box.x, box.y, box.x + box.width, box.y + box.height);
			default:
				return box;
		}
	}

	/** The line the border's pixels run along, for moving borders. */
	private Shape borderPath(Rectangle box)
	{
		int w = Math.max(1, theme.getBorderWidth());
		int h = (w - 1) / 2;
		int right = box.x + box.width - 1 - h;
		int bottom = box.y + box.height - 1 - h;
		switch (theme.getFrame())
		{
			case ROUNDED:
				return new RoundRectangle2D.Double(box.x + w / 2.0, box.y + w / 2.0, box.width - w, box.height - w, corner(), corner());
			case NOTCHED:
				return notched(box.x + h, box.y + h, right, bottom, notch(h));
			case PIXEL:
				return pixel(box.x + h, box.y + h, right, bottom);
			case SCALLOPED:
				return scalloped(box.x + w / 2.0, box.y + w / 2.0, box.x + box.width - w / 2.0, box.y + box.height - w / 2.0);
			case TICKET:
				return ticket(box.x + w / 2.0, box.y + w / 2.0, box.x + box.width - w / 2.0, box.y + box.height - w / 2.0);
			default:
				return new Rectangle(box.x + h, box.y + h, right - box.x - h, bottom - box.y - h);
		}
	}

	/** Where the title line was last drawn, if the card starts with one. */
	private Rectangle header()
	{
		List<LayoutableRenderableEntity> children = panel.getChildren();
		if (children.isEmpty())
		{
			return null;
		}
		LayoutableRenderableEntity first = children.get(0);
		if (first instanceof CardHeader || first instanceof CardLine && ((CardLine) first).isHeader())
		{
			Rectangle bounds = first.getBounds();
			return bounds == null || bounds.height == 0 ? null : bounds;
		}
		return null;
	}

	@Override
	public Rectangle getBounds()
	{
		Rectangle bounds = panel.getBounds();
		return new Rectangle(bounds.x, bounds.y, bounds.width, Math.max(bounds.height, minHeight));
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		location = position;
		panel.setPreferredLocation(position);
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		panel.setPreferredSize(dimension);
	}
}
