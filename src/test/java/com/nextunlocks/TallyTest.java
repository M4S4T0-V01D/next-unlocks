package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

public class TallyTest
{
	@Test
	public void roundTrips()
	{
		List<Tally> tallies = Arrays.asList(new Tally(Skill.FISHING, 377, "Raw lobster", 500, 12),
			new Tally(Skill.FIREMAKING, 1515, "Yew logs | big", 1_000, 1_000));
		assertEquals(tallies, Tally.parse(Tally.format(tallies)));
		assertEquals("tally:FISHING:377", tallies.get(0).key());
		assertTrue(tallies.get(1).isDone());
		assertFalse(tallies.get(0).isDone());
	}

	@Test
	public void skipsBadLines()
	{
		assertTrue(Tally.parse("NOT_A_SKILL|1|5|0|X\nFISHING|x|5|0|X\nFISHING|1|0|0|X\nFISHING|1|5|0\n").isEmpty());
		assertTrue(Tally.parse("").isEmpty());
		assertEquals(Tally.MAX_TARGET, Tally.parse("MINING|1|5|99999999|Ore").get(0).getCount());
	}

	@Test
	public void parsesAmounts()
	{
		assertEquals(500, Tally.parseAmount("500"));
		assertEquals(1_000, Tally.parseAmount(" 1,000 "));
		assertEquals(2_500, Tally.parseAmount("2.5K"));
		assertEquals(1_000_000, Tally.parseAmount("1m"));
		assertEquals(0, Tally.parseAmount("0"));
		assertEquals(0, Tally.parseAmount("lots"));
		assertEquals(0, Tally.parseAmount("11m"));
		assertEquals(0, Tally.parseAmount(null));
	}

	@Test
	public void wordsTheGoal()
	{
		assertEquals("Catch 500 Raw lobster", new Tally(Skill.FISHING, 377, "Raw lobster", 500, 0).goalText());
		assertEquals("Burn 1,000 Yew logs", new Tally(Skill.FIREMAKING, 1515, "Yew logs", 1_000, 0).goalText());
		assertEquals("Make", Tally.verb(Skill.ATTACK));
	}

	@Test
	public void countsGainsOrUses()
	{
		assertEquals(1, Tally.counted(Skill.FISHING, 1, true));
		assertEquals(0, Tally.counted(Skill.FISHING, 1, false));
		assertEquals(0, Tally.counted(Skill.FISHING, -1, true));
		assertEquals(2, Tally.counted(Skill.PRAYER, -2, true));
		assertEquals(0, Tally.counted(Skill.FIREMAKING, 1, true));
	}
}
