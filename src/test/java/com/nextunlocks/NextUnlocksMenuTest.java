package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.awt.Rectangle;
import org.junit.Test;

public class NextUnlocksMenuTest
{
	@Test
	public void stepWrapsRound()
	{
		NextUnlocksConfig.GlowMode[] values = NextUnlocksConfig.GlowMode.values();
		assertEquals(values[1], NextUnlocksMenu.step(values, values[0], 1));
		assertEquals(values[values.length - 1], NextUnlocksMenu.step(values, values[0], -1));
		assertEquals(values[0], NextUnlocksMenu.step(values, values[values.length - 1], 1));
		assertEquals(values[1], NextUnlocksMenu.step(values, null, 1));
	}

	@Test
	public void rimButtonSitsOnTheRim()
	{
		// straight right of a 150 wide minimap centred on 100,100
		Rectangle right = NextUnlocksMenu.rimButton(100, 100, 75, 18, 0);
		assertEquals(new Rectangle(169, 91, 18, 18), right);
		// straight down: positive angles go clockwise
		Rectangle down = NextUnlocksMenu.rimButton(100, 100, 75, 18, 90);
		assertEquals(91, down.x);
		assertEquals(169, down.y);
	}

	@Test
	public void slidersAndPositionsStayInRange()
	{
		assertEquals(1, NextUnlocksMenu.sliderValue(-50, 10, 101, 1, 8));
		assertEquals(8, NextUnlocksMenu.sliderValue(500, 10, 101, 1, 8));
		assertEquals(50, NextUnlocksMenu.sliderValue(60, 10, 101, 0, 100));
		assertEquals(0, NextUnlocksMenu.onCanvas(-20, 300, 800));
		assertEquals(500, NextUnlocksMenu.onCanvas(700, 300, 800));
		assertEquals(120, NextUnlocksMenu.onCanvas(120, 300, 800));
	}

	@Test
	public void xpLeftIsShort()
	{
		assertEquals("950 xp", NextUnlocksMenu.xpLeft(950));
		assertEquals("12.3k xp", NextUnlocksMenu.xpLeft(12_345));
		assertEquals("1.25m xp", NextUnlocksMenu.xpLeft(1_250_000));
	}

	@Test
	public void lookKeysAreConfigKeys()
	{
		for (String key : NextUnlocksMenu.LOOK_KEYS)
		{
			boolean found = false;
			for (java.lang.reflect.Method m : NextUnlocksConfig.class.getMethods())
			{
				net.runelite.client.config.ConfigItem item = m.getAnnotation(net.runelite.client.config.ConfigItem.class);
				found |= item != null && item.keyName().equals(key);
			}
			assertTrue(key, found);
		}
	}

	@Test
	public void findsWhatWasTyped() throws Exception
	{
		UnlockRepository repository = new UnlockRepository();
		repository.load(new Gson());
		TaskRepository tasks = new TaskRepository();
		tasks.load(new Gson(), repository.diaries());

		assertNull(tasks.find("  "));
		assertNull(tasks.find("no such thing at all"));
		assertEquals("Dragon Slayer I", tasks.find("dragon slayer i").getName());
		Task partial = tasks.find("monkey madness");
		assertNotNull(partial);
		assertFalse(partial.isItem());
		assertTrue(tasks.find("ironwood blowpipe").isItem());
	}
}
