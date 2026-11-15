package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class CardMaterialTest
{
	@Test
	public void longNamesAreCutShortNotWrapped()
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		FontMetrics metrics = g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		assertEquals("Ironwood logs", CardMaterial.fit(metrics, "Ironwood logs", 500));

		String cut = CardMaterial.fit(metrics, "Raw swordtip squid", metrics.stringWidth("Raw sword"));
		assertTrue(cut, cut.endsWith("...") && !cut.contains("squid"));
		assertTrue(metrics.stringWidth(cut) <= metrics.stringWidth("Raw sword"));
		g.dispose();
	}
}
