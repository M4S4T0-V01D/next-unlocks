package com.nextunlocks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import org.junit.Test;

public class SkillGuideTest
{
	private static final ToIntFunction<String> MONOSPACE = s -> s.length() * 6;

	@Test
	public void fitMatchesTheGameWhenTabsFit()
	{
		assertArrayEquals(new int[]{26, 2}, SkillGuide.fit(400, 8));
		assertArrayEquals(new int[]{20, 0}, SkillGuide.fit(200, 5));
	}

	@Test
	public void fitSqueezesAnExtraTabIn()
	{
		for (int count = 1; count <= 16; count++)
		{
			int[] fit = SkillGuide.fit(250, count);
			assertTrue(fit[0] >= SkillGuide.MIN_TAB_HEIGHT);
			if (count * (SkillGuide.MIN_TAB_HEIGHT - 1) <= 250)
			{
				assertTrue("count " + count, count * (fit[0] + fit[1]) <= 250);
			}
		}
	}

	@Test
	public void linesWrapAndBreak()
	{
		assertEquals(1, SkillGuide.lines("Use rune pickaxes", 200, MONOSPACE));
		assertEquals(2, SkillGuide.lines("Use rune pickaxes<br><col=9f9f9f>Note</col>", 200, MONOSPACE));
		// 18 characters fit in 108 pixels; tags take no room
		assertEquals(2, SkillGuide.lines("<col=ff0000>aaaa bbbb cccc dddd eeee</col>", 108, MONOSPACE));
	}

	@Test
	public void rowsMatchTheirUnlock()
	{
		Unlock pickaxe = UnlockRepository.guideUnlock(13, 41, 2, "Pickaxes", "Use rune pickaxes", null, false, 1275, null, null);
		Unlock ore = UnlockRepository.guideUnlock(13, 85, 1, "Rocks", "Runite ore", null, true, 451, null, null);
		assertEquals(pickaxe, SkillGuide.match(Arrays.asList(ore, pickaxe), "Use rune pickaxes"));
		assertEquals(ore, SkillGuide.match(Arrays.asList(ore, pickaxe), "Members: Runite ore<br>Requires: <col=ff0000>Quest</col>"));
		assertNull(SkillGuide.match(Arrays.asList(ore, pickaxe), "Something else"));
		assertNull(SkillGuide.match(null, "Use rune pickaxes"));
	}
}
