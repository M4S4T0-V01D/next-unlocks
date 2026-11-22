package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

	@Test
	public void paceWaitsForAMinuteAndStartsOverAfterABreak()
	{
		Tally.Pace pace = new Tally.Pace();
		pace.add(1, 0);
		pace.add(1, 30_000);
		assertEquals(0, pace.perHour(30_000));
		pace.add(1, 60_000);
		assertEquals(180, pace.perHour(60_000));
		long away = 60_000 + Tally.Pace.BREAK_MILLIS + 1;
		assertEquals(0, pace.perHour(away));
		pace.add(5, away);
		assertEquals(0, pace.perHour(away + 30_000));
		assertEquals(300, pace.perHour(away + 60_000));
	}

	@Test
	public void paceText()
	{
		assertEquals("420/h, 1h 5m left", Tally.pace(420, 455));
		assertEquals("1,000/h, 1m left", Tally.pace(1000, 1));
		assertEquals("100/h", Tally.pace(100, 0));
		assertEquals("", Tally.pace(0, 50));
	}

	@Test
	public void outlookText()
	{
		// 90 left at 60/h from midnight UTC: done at 01:30, four inventories of a non-stacking item
		assertEquals("Done about 01:30, ~4 invs, 1,250 banked",
			Tally.outlook(60, 90, false, 1250, 0, ZoneOffset.UTC, Locale.UK));
		assertEquals("~1 inv", Tally.outlook(0, 5, false, 0, 0, ZoneOffset.UTC, Locale.UK));
		assertEquals("Done about 00:01", Tally.outlook(6000, 1, true, 0, 0, ZoneOffset.UTC, Locale.UK));
		assertEquals("", Tally.outlook(60, 0, false, 0, 0, ZoneOffset.UTC, Locale.UK));
	}

	@Test
	public void bankedRoundTrip()
	{
		Map<Integer, Integer> banked = new LinkedHashMap<>();
		banked.put(377, 312);
		banked.put(1511, 0);
		banked.put(995, 1_000_000);
		assertEquals("377:312,995:1000000", Tally.formatBanked(banked));
		Map<Integer, Integer> back = Tally.parseBanked("377:312,995:1000000");
		assertEquals(Integer.valueOf(312), back.get(377));
		assertEquals(Integer.valueOf(1_000_000), back.get(995));
		assertEquals(1, Tally.parseBanked(" 377:5, junk, 1:x, 2:-3, 3:").size());
		assertTrue(Tally.parseBanked(null).isEmpty());
		assertTrue(Tally.parseBanked("").isEmpty());
	}
}
