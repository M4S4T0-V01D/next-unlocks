package com.nextunlocks;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * The level-ups an account has made while the plugin ran, newest last. Only levels and times are kept;
 * what they unlocked comes from the data, so it follows the filters and data updates.
 */
final class History
{
	/** Per-account (RSProfile) config key. */
	static final String KEY = "history";
	/** Oldest entries go first past this. */
	static final int MAX_ENTRIES = 400;

	private static final String FIELD = "|";
	private static final String SEPARATOR = "\n";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	@Value
	static class Entry
	{
		Skill skill;
		/** The level before; the entry covers the levels above it up to {@link #to}. */
		int from;
		int to;
		/** Epoch millis. */
		long time;
	}

	private History()
	{
	}

	/** Entries from the config string; ones that can't be read are skipped. */
	static List<Entry> parse(String value)
	{
		List<Entry> entries = new ArrayList<>();
		if (value == null || value.isEmpty())
		{
			return entries;
		}
		for (String line : value.split(SEPARATOR))
		{
			String[] parts = line.split("\\" + FIELD);
			if (parts.length != 4)
			{
				continue;
			}
			try
			{
				Skill skill = Skill.valueOf(parts[0]);
				int from = Integer.parseInt(parts[1]);
				int to = Integer.parseInt(parts[2]);
				long time = Long.parseLong(parts[3]);
				if (to > from)
				{
					entries.add(new Entry(skill, from, to, time));
				}
			}
			catch (IllegalArgumentException e)
			{
				// skill renamed or a broken number: drop the entry
			}
		}
		return entries;
	}

	static String format(List<Entry> entries)
	{
		StringBuilder out = new StringBuilder();
		for (Entry e : entries)
		{
			if (out.length() > 0)
			{
				out.append(SEPARATOR);
			}
			out.append(e.skill.name()).append(FIELD).append(e.from).append(FIELD).append(e.to).append(FIELD).append(e.time);
		}
		return out.toString();
	}

	/** The entries with this one added at the end, dropping the oldest past {@link #MAX_ENTRIES}. */
	static List<Entry> add(List<Entry> entries, Entry entry)
	{
		List<Entry> out = new ArrayList<>(entries);
		out.add(entry);
		if (out.size() > MAX_ENTRIES)
		{
			out = new ArrayList<>(out.subList(out.size() - MAX_ENTRIES, out.size()));
		}
		return Collections.unmodifiableList(out);
	}

	/** "just now", "5 min ago", "3 h ago", "yesterday", "4 days ago", then the date after a month. */
	static String ago(long time, long now, ZoneId zone)
	{
		long minutes = Math.max(0, (now - time) / 60_000);
		if (minutes < 1)
		{
			return "just now";
		}
		if (minutes < 60)
		{
			return minutes + " min ago";
		}
		if (minutes < 24 * 60 && sameDay(time, now, zone))
		{
			return minutes / 60 + " h ago";
		}
		long days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(time).atZone(zone).toLocalDate(), Instant.ofEpochMilli(now).atZone(zone).toLocalDate());
		if (days <= 1)
		{
			return "yesterday";
		}
		if (days < 31)
		{
			return days + " days ago";
		}
		return date(time, zone);
	}

	static String date(long time, ZoneId zone)
	{
		return DATE.format(Instant.ofEpochMilli(time).atZone(zone));
	}

	private static boolean sameDay(long a, long b, ZoneId zone)
	{
		return Instant.ofEpochMilli(a).atZone(zone).toLocalDate().equals(Instant.ofEpochMilli(b).atZone(zone).toLocalDate());
	}
}
