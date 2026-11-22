package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

public class HistoryTest
{
	private static final ZoneId UTC = ZoneOffset.UTC;
	private static final long NOW = ZonedDateTime.of(2026, 11, 16, 15, 0, 0, 0, UTC).toInstant().toEpochMilli();
	private static final long MINUTE = 60_000;

	@Test
	public void roundTrips()
	{
		List<History.Entry> entries = History.add(Collections.emptyList(), new History.Entry(Skill.SMITHING, 49, 50, NOW));
		entries = History.add(entries, new History.Entry(Skill.MAGIC, 1, 3, NOW + 1));
		assertEquals(entries, History.parse(History.format(entries)));
	}

	@Test
	public void skipsBrokenEntries()
	{
		List<History.Entry> entries = History.parse("SMITHING|49|50|1\nNOT_A_SKILL|1|2|3\nMAGIC|x|2|3\nMAGIC|5|5|3\nMINING|1|2\n\nFISHING|9|10|7");
		assertEquals(2, entries.size());
		assertEquals(Skill.FISHING, entries.get(1).getSkill());
		assertTrue(History.parse(null).isEmpty());
		assertTrue(History.parse("").isEmpty());
	}

	@Test
	public void dropsOldestPastTheCap()
	{
		List<History.Entry> entries = Collections.emptyList();
		for (int i = 0; i < History.MAX_ENTRIES + 3; i++)
		{
			entries = History.add(entries, new History.Entry(Skill.AGILITY, 1, 2, i));
		}
		assertEquals(History.MAX_ENTRIES, entries.size());
		assertEquals(3, entries.get(0).getTime());
	}

	@Test
	public void saysHowLongAgo()
	{
		assertEquals("just now", History.ago(NOW - 30_000, NOW, UTC));
		assertEquals("just now", History.ago(NOW + MINUTE, NOW, UTC));
		assertEquals("5 min ago", History.ago(NOW - 5 * MINUTE, NOW, UTC));
		assertEquals("3 h ago", History.ago(NOW - 180 * MINUTE, NOW, UTC));
		// late last night is yesterday, not "16 h ago"
		assertEquals("yesterday", History.ago(NOW - 16 * 60 * MINUTE, NOW, UTC));
		assertEquals("4 days ago", History.ago(NOW - 4 * 24 * 60 * MINUTE, NOW, UTC));
		assertEquals("2 Oct 2026", History.ago(NOW - 45L * 24 * 60 * MINUTE, NOW, UTC));
	}
}
