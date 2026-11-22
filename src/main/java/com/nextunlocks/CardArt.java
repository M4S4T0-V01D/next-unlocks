package com.nextunlocks;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

/**
 * The game's own art for trims: item icons and the stone dragons around the fixed mode minimap, handed over by the
 * plugin. Until they are there (in tests, or before the client has loaded) the trims draw something of their own.
 */
final class CardArt
{
	/** How far out from the map hole the dragons' ring reaches; past it is the side panel's background. */
	private static final int RING = 30;

	/** Item icons by id; null when there is no client. */
	static volatile IntFunction<BufferedImage> items;
	private static volatile BufferedImage dragons;
	private static final Map<String, BufferedImage> SIZED = new ConcurrentHashMap<>();
	private static final Map<BufferedImage, Rectangle> BOUNDS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<BufferedImage, BufferedImage> BRIGHT = Collections.synchronizedMap(new WeakHashMap<>());

	private CardArt()
	{
	}

	static BufferedImage item(int id)
	{
		IntFunction<BufferedImage> source = items;
		return source == null ? null : source.apply(id);
	}

	/** The part of an icon that isn't see-through, or null while it is still loading and blank. */
	static Rectangle bounds(BufferedImage image)
	{
		if (image == null)
		{
			return null;
		}
		Rectangle known = BOUNDS.get(image);
		if (known != null)
		{
			return known;
		}
		int left = image.getWidth();
		int top = image.getHeight();
		int right = -1;
		int bottom = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					left = Math.min(left, x);
					top = Math.min(top, y);
					right = Math.max(right, x);
					bottom = Math.max(bottom, y);
				}
			}
		}
		if (right < 0)
		{
			// item icons arrive empty and get drawn in later, so don't remember this
			return null;
		}
		Rectangle found = new Rectangle(left, top, right - left + 1, bottom - top + 1);
		BOUNDS.put(image, found);
		return found;
	}

	/** A loaded icon made lighter, for dark items that would vanish on a dark card. */
	static BufferedImage bright(BufferedImage image)
	{
		BufferedImage known = BRIGHT.get(image);
		if (known != null)
		{
			return known;
		}
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int p = image.getRGB(x, y);
				int r = Math.min(255, ((p >> 16) & 255) * 2 + 14);
				int g = Math.min(255, ((p >> 8) & 255) * 2 + 10);
				int b = Math.min(255, (p & 255) * 2 + 10);
				out.setRGB(x, y, (p & 0xff000000) | r << 16 | g << 8 | b);
			}
		}
		BRIGHT.put(image, out);
		return out;
	}

	static BufferedImage dragons()
	{
		return dragons;
	}

	static void clear()
	{
		items = null;
		dragons = null;
		SIZED.clear();
		BOUNDS.clear();
		BRIGHT.clear();
	}

	/**
	 * Cuts the two dragons and their stone ring out of the fixed mode minimap: its left edge, the cover over the map
	 * and its right edge, side by side. Everything further from the map hole than the ring is the panel behind it.
	 */
	static void minimap(BufferedImage left, BufferedImage cover, BufferedImage right)
	{
		if (left == null || cover == null || right == null)
		{
			return;
		}
		int w = left.getWidth() + cover.getWidth() + right.getWidth();
		int h = Math.max(cover.getHeight(), Math.max(left.getHeight(), right.getHeight()));
		BufferedImage all = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = all.createGraphics();
		g.drawImage(left, 0, 0, null);
		g.drawImage(cover, left.getWidth(), 0, null);
		g.drawImage(right, left.getWidth() + cover.getWidth(), 0, null);
		g.dispose();

		// the map hole is the see-through middle of the cover; the compass hole beside it doesn't count
		boolean[] hole = new boolean[w * h];
		int seedX = left.getWidth() + (int) (cover.getWidth() * 0.56);
		int seedY = cover.getHeight() / 2;
		if (clear(all, seedX, seedY))
		{
			ArrayDeque<int[]> queue = new ArrayDeque<>();
			hole[seedY * w + seedX] = true;
			queue.add(new int[]{seedX, seedY});
			while (!queue.isEmpty())
			{
				int[] at = queue.poll();
				int[][] next = {{at[0] + 1, at[1]}, {at[0] - 1, at[1]}, {at[0], at[1] + 1}, {at[0], at[1] - 1}};
				for (int[] n : next)
				{
					if (n[0] >= 0 && n[1] >= 0 && n[0] < w && n[1] < h && !hole[n[1] * w + n[0]] && clear(all, n[0], n[1]))
					{
						hole[n[1] * w + n[0]] = true;
						queue.add(n);
					}
				}
			}
		}
		else
		{
			for (int y = 0; y < h; y++)
			{
				for (int x = 0; x < w; x++)
				{
					hole[y * w + x] = clear(all, x, y);
				}
			}
		}

		// distance from the hole, in thirds of a pixel: 3 straight, 4 across
		int[] far = new int[w * h];
		for (int i = 0; i < far.length; i++)
		{
			far[i] = hole[i] ? 0 : 1 << 20;
		}
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int i = y * w + x;
				if (x > 0)
				{
					far[i] = Math.min(far[i], far[i - 1] + 3);
				}
				if (y > 0)
				{
					far[i] = Math.min(far[i], far[i - w] + 3);
					if (x > 0)
					{
						far[i] = Math.min(far[i], far[i - w - 1] + 4);
					}
					if (x < w - 1)
					{
						far[i] = Math.min(far[i], far[i - w + 1] + 4);
					}
				}
			}
		}
		for (int y = h - 1; y >= 0; y--)
		{
			for (int x = w - 1; x >= 0; x--)
			{
				int i = y * w + x;
				if (x < w - 1)
				{
					far[i] = Math.min(far[i], far[i + 1] + 3);
				}
				if (y < h - 1)
				{
					far[i] = Math.min(far[i], far[i + w] + 3);
					if (x < w - 1)
					{
						far[i] = Math.min(far[i], far[i + w + 1] + 4);
					}
					if (x > 0)
					{
						far[i] = Math.min(far[i], far[i + w - 1] + 4);
					}
				}
			}
		}

		boolean[] keep = new boolean[w * h];
		for (int i = 0; i < keep.length; i++)
		{
			keep[i] = !hole[i] && far[i] <= RING * 3 && (all.getRGB(i % w, i / w) >>> 24) != 0;
		}
		BufferedImage cut = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int left0 = w;
		int top0 = h;
		int right0 = -1;
		int bottom0 = -1;
		int outline = new Color(24, 20, 16).getRGB();
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int i = y * w + x;
				int pixel;
				if (keep[i])
				{
					pixel = all.getRGB(x, y);
				}
				else if (!hole[i] && (keep(keep, w, h, x - 1, y) || keep(keep, w, h, x + 1, y)
					|| keep(keep, w, h, x, y - 1) || keep(keep, w, h, x, y + 1)))
				{
					// a dark edge where the ring was cut from the panel behind it
					pixel = outline;
				}
				else
				{
					continue;
				}
				cut.setRGB(x, y, pixel);
				left0 = Math.min(left0, x);
				top0 = Math.min(top0, y);
				right0 = Math.max(right0, x);
				bottom0 = Math.max(bottom0, y);
			}
		}
		if (right0 < 0)
		{
			return;
		}
		SIZED.clear();
		dragons = cut.getSubimage(left0, top0, right0 - left0 + 1, bottom0 - top0 + 1);
	}

	/** The dragons at a size, in the trim's color when tinted, made once and kept. */
	static BufferedImage dragons(int width, int height, Color tint)
	{
		BufferedImage art = dragons;
		if (art == null || width < 1 || height < 1)
		{
			return null;
		}
		String key = width + "x" + height + (tint == null ? "" : ":" + tint.getRGB());
		BufferedImage known = SIZED.get(key);
		if (known != null)
		{
			return known;
		}
		if (SIZED.size() > 32)
		{
			SIZED.clear();
		}
		BufferedImage sized = resize(tint == null ? art : tint(art, tint), width, height);
		SIZED.put(key, sized);
		return sized;
	}

	/** Shrinks in halves first so the stone keeps its texture instead of going to noise. */
	private static BufferedImage resize(BufferedImage image, int width, int height)
	{
		BufferedImage at = image;
		while (at.getWidth() / 2 >= width && at.getHeight() / 2 >= height)
		{
			at = draw(at, at.getWidth() / 2, at.getHeight() / 2);
		}
		return draw(at, width, height);
	}

	private static BufferedImage draw(BufferedImage image, int width, int height)
	{
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return out;
	}

	/** The stone recolored by its lightness, so the carving still shows. */
	private static BufferedImage tint(BufferedImage image, Color tint)
	{
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int p = image.getRGB(x, y);
				double light = (0.3 * ((p >> 16) & 255) + 0.59 * ((p >> 8) & 255) + 0.11 * (p & 255)) / 255 / 0.4;
				int r = Math.min(255, (int) (tint.getRed() * light));
				int gr = Math.min(255, (int) (tint.getGreen() * light));
				int b = Math.min(255, (int) (tint.getBlue() * light));
				out.setRGB(x, y, (p & 0xff000000) | r << 16 | gr << 8 | b);
			}
		}
		return out;
	}

	private static boolean clear(BufferedImage image, int x, int y)
	{
		return (image.getRGB(x, y) >>> 24) == 0;
	}

	private static boolean keep(boolean[] keep, int w, int h, int x, int y)
	{
		return x >= 0 && y >= 0 && x < w && y < h && keep[y * w + x];
	}
}
