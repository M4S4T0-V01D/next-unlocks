package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class PinBarTest
{
	@Test
	public void stillUpToHalfwayThenHeatsUp()
	{
		assertEquals(0, PinBar.heat(0.3), 0);
		assertEquals(0, PinBar.heat(0.5), 0);
		assertEquals(0.25, PinBar.heat(0.75), 1e-9);
		assertEquals(1, PinBar.heat(1), 0);
		assertTrue(PinBar.heat(0.95) > PinBar.heat(0.9));
	}

	@Test
	public void progressIsClamped()
	{
		assertEquals(0.5, bar(150, null).progress(), 1e-9);
		assertEquals(1, bar(500, null).progress(), 0);
		assertEquals(0, bar(50, null).progress(), 0);
	}

	@Test
	public void rendersAtFullAndThroughTheBurst()
	{
		long now = System.currentTimeMillis();
		for (PinBar.LevelUp up : new PinBar.LevelUp[]{
			null,
			new PinBar.LevelUp(now, true),
			new PinBar.LevelUp(now - PinBar.FULL_MS - 50, true),
			new PinBar.LevelUp(now - 50, false)})
		{
			for (NextUnlocksConfig.BarStyle style : NextUnlocksConfig.BarStyle.values())
			{
				for (Color[] pixels : new Color[][]{null, {Color.BLACK, Color.GRAY}})
				{
					PinBar bar = new PinBar(style, 100, 200, 195, Color.GREEN, Color.DARK_GRAY, Color.WHITE, pixels, () -> up);
					BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = image.createGraphics();
					bar.setPreferredLocation(new Point(20, 60));
					bar.setPreferredSize(new Dimension(150, 0));
					assertEquals(new Dimension(150, PinBar.HEIGHT), bar.render(g));
					bar.renderBurst(g);
					g.dispose();
				}
			}
		}
	}

	@Test
	public void noLevelUpWhenTheEffectIsOff()
	{
		PinBar bar = new PinBar(NextUnlocksConfig.BarStyle.ARCADE, 100, 200, 150, Color.GREEN, Color.DARK_GRAY, Color.WHITE, null, null);
		BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		bar.setPreferredSize(new Dimension(150, 0));
		bar.render(g);
		bar.renderBurst(g);
		g.dispose();
	}

	private static PinBar bar(double value, PinBar.LevelUp up)
	{
		return new PinBar(NextUnlocksConfig.BarStyle.ARCADE, 100, 200, value, Color.GREEN, Color.DARK_GRAY, Color.WHITE, null, () -> up);
	}
}
