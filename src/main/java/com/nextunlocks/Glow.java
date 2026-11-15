package com.nextunlocks;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;
import java.util.function.Consumer;

/** Soft glows: something painted once, blurred, then drawn behind the real thing. Callers keep the image until it changes. */
final class Glow
{
	private Glow()
	{
	}

	/** Space around the painted area the glow can spread into. */
	static int pad(int radius)
	{
		return radius * 2;
	}

	/**
	 * Paint into an image the size of the area plus the pad on every side, then blur it. The painter's origin is the
	 * area's corner, so the image goes at the area's corner minus the pad.
	 */
	static BufferedImage paint(int width, int height, int radius, Consumer<Graphics2D> painter)
	{
		int pad = pad(radius);
		// premultiplied, so blurring into the transparent edge doesn't darken the color
		BufferedImage image = new BufferedImage(width + pad * 2, height + pad * 2, BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g = image.createGraphics();
		g.translate(pad, pad);
		painter.accept(g);
		g.dispose();
		// a box blur twice in each direction is close to a gaussian
		for (int pass = 0; pass < 2; pass++)
		{
			image = blur(image, radius, true);
			image = blur(image, radius, false);
		}
		return image;
	}

	private static BufferedImage blur(BufferedImage image, int radius, boolean horizontal)
	{
		int size = radius * 2 + 1;
		float[] weights = new float[size];
		Arrays.fill(weights, 1f / size);
		Kernel kernel = horizontal ? new Kernel(size, 1, weights) : new Kernel(1, size, weights);
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null).filter(image, out);
	}
}
