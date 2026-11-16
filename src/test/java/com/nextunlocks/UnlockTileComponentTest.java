package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class UnlockTileComponentTest
{
	private static Dimension render(UnlockTileComponent tile, BufferedImage canvas)
	{
		Graphics2D g = canvas.createGraphics();
		try
		{
			return tile.render(g);
		}
		finally
		{
			g.dispose();
		}
	}

	@Test
	public void slotsAreBiggerByTheirPadding()
	{
		assertEquals(UnlockTileComponent.width(1, false) + 2 * UnlockTileComponent.SLOT_PADDING, UnlockTileComponent.width(1, true));
		assertEquals(UnlockTileComponent.height(1, false) + 2 * UnlockTileComponent.SLOT_PADDING, UnlockTileComponent.height(1, true));
		assertTrue(UnlockTileComponent.width(2, true) > UnlockTileComponent.width(1, true));
	}

	@Test
	public void slottedTileDrawsItsSlotWithoutALevel()
	{
		BufferedImage icon = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		UnlockTileComponent tile = new UnlockTileComponent(icon, "Rune platebody", null, Color.WHITE, false, 1)
			.slot(new Color(10, 20, 30, 170), Color.ORANGE, 4);
		Dimension size = render(tile, canvas);
		assertEquals(new Dimension(UnlockTileComponent.width(1, true), UnlockTileComponent.height(1, true)), size);
		// the slot's fill shows in its middle, where the transparent icon is
		assertTrue(new Color(canvas.getRGB(size.width / 2, size.height / 2), true).getAlpha() > 0);
	}

	@Test
	public void bareTileKeepsItsLevel()
	{
		BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Dimension size = render(new UnlockTileComponent(null, "Runite ore", 85, Color.YELLOW, false, 1), canvas);
		assertEquals(new Dimension(UnlockTileComponent.width(1, false), UnlockTileComponent.height(1, false)), size);
		boolean drawn = false;
		for (int x = 0; x < size.width && !drawn; x++)
		{
			for (int y = 0; y < size.height && !drawn; y++)
			{
				drawn = new Color(canvas.getRGB(x, y), true).getAlpha() > 0;
			}
		}
		assertTrue(drawn);
	}
}
