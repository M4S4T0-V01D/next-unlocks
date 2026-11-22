package com.nextunlocks;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;
import net.runelite.api.Skill;

/** An amount goal, like catching 500 lobsters: items counted as a skill makes or gathers them. */
@Value
class Tally
{
	/** Prefix of an amount goal's pin key, followed by its skill and item ID. */
	static final String KEY = "tally:";
	private static final String FIELD = "|";
	/** Most an amount goal can ask for. */
	static final int MAX_TARGET = 10_000_000;
	/** Slots in the inventory, for how many trips an item that doesn't stack has left. */
	static final int INVENTORY = 28;

	Skill skill;
	/** The item counted, unnoted. */
	int item;
	String name;
	int target;
	int count;

	String key()
	{
		return key(skill, item);
	}

	static String key(Skill skill, int item)
	{
		return KEY + skill.name() + ":" + item;
	}

	boolean isDone()
	{
		return count >= target;
	}

	Tally withCount(int count)
	{
		return new Tally(skill, item, name, target, Math.max(0, Math.min(MAX_TARGET, count)));
	}

	Tally withTarget(int target)
	{
		return new Tally(skill, item, name, target, count);
	}

	/** Whether the skill uses the item up, like logs burnt or bones buried, rather than making or gathering it. */
	static boolean usesUp(Skill skill)
	{
		return skill == Skill.FIREMAKING || skill == Skill.PRAYER || skill == Skill.CONSTRUCTION;
	}

	/** What the skill does to the item, for the card: "Catch", "Chop", "Burn". */
	static String verb(Skill skill)
	{
		switch (skill)
		{
			case FISHING:
			case HUNTER:
				return "Catch";
			case WOODCUTTING:
				return "Chop";
			case MINING:
				return "Mine";
			case COOKING:
				return "Cook";
			case SMITHING:
				return "Smith";
			case FLETCHING:
				return "Fletch";
			case HERBLORE:
				return "Mix";
			case FIREMAKING:
				return "Burn";
			case PRAYER:
				return "Bury";
			case FARMING:
				return "Harvest";
			case THIEVING:
				return "Steal";
			case CONSTRUCTION:
				return "Use";
			case CRAFTING:
			case RUNECRAFT:
				return "Craft";
			default:
				return "Make";
		}
	}

	/** The card's line, like "Catch 500 Lobster". */
	String goalText()
	{
		return verb(skill) + " " + String.format(Locale.ROOT, "%,d", target) + " " + name;
	}

	/** Parses an amount typed in the chatbox, like 500, 1,000, 2.5k or 1m; 0 for anything else. */
	static int parseAmount(String text)
	{
		String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace(",", "");
		double multiplier = 1;
		if (t.endsWith("k") || t.endsWith("m"))
		{
			multiplier = t.endsWith("k") ? 1_000 : 1_000_000;
			t = t.substring(0, t.length() - 1);
		}
		try
		{
			double amount = Double.parseDouble(t) * multiplier;
			return amount >= 1 && amount <= MAX_TARGET ? (int) Math.round(amount) : 0;
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	/** Amount goals as kept in the config: SKILL|item|target|count|name, one per line; bad lines are skipped. */
	static List<Tally> parse(String text)
	{
		List<Tally> out = new ArrayList<>();
		for (String line : text == null ? new String[0] : text.split("\n"))
		{
			String[] parts = line.trim().split("\\|", 5);
			if (parts.length < 5 || parts[4].isEmpty())
			{
				continue;
			}
			try
			{
				int target = Integer.parseInt(parts[2]);
				if (target > 0)
				{
					out.add(new Tally(Skill.valueOf(parts[0]), Integer.parseInt(parts[1]), parts[4], target, 0)
						.withCount(Integer.parseInt(parts[3])));
				}
			}
			catch (IllegalArgumentException e)
			{
				// a skill that no longer exists, or a broken number
			}
		}
		return out;
	}

	static String format(Collection<Tally> tallies)
	{
		return tallies.stream()
			.map(t -> t.skill.name() + FIELD + t.item + FIELD + t.target + FIELD + t.count + FIELD + t.name.replace("\n", " "))
			.collect(Collectors.joining("\n"));
	}

	/** How fast an amount goal is going this session; a break of {@link #BREAK_MILLIS} starts it over. */
	static final class Pace
	{
		static final long BREAK_MILLIS = 10 * 60_000L;
		/** Shortest stretch a rate is shown for, so the first catches don't claim thousands an hour. */
		static final long MIN_MILLIS = 60_000L;

		private long start;
		private long last;
		private int counted;

		void add(int amount, long now)
		{
			if (counted == 0 || now - last > BREAK_MILLIS)
			{
				start = now;
				counted = 0;
			}
			counted += amount;
			last = now;
		}

		/** Items an hour, or 0 when there's too little to go on or the last one was a break ago. */
		int perHour(long now)
		{
			long span = now - start;
			if (counted < 2 || span < MIN_MILLIS || now - last > BREAK_MILLIS)
			{
				return 0;
			}
			return (int) Math.min(MAX_TARGET, counted * 3_600_000L / span);
		}
	}

	/** The pace line, like "420/h, 1h 5m left"; empty without a rate. */
	static String pace(int perHour, int left)
	{
		if (perHour <= 0)
		{
			return "";
		}
		String rate = String.format(Locale.ROOT, "%,d/h", perHour);
		if (left <= 0)
		{
			return rate;
		}
		long minutes = Math.max(1, (left * 60L + perHour - 1) / perHour);
		return rate + ", " + (minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m") + " left";
	}

	/**
	 * The card's second line for a goal still going, like "Done about 9:40 PM, ~14 invs, 312 banked": the clock time it
	 * should be done by at this pace, the inventories left of an item that doesn't stack, and how many are in the bank;
	 * each only when known.
	 */
	static String outlook(int perHour, int left, boolean stackable, int banked, long now, ZoneId zone, Locale locale)
	{
		List<String> parts = new ArrayList<>();
		if (perHour > 0 && left > 0)
		{
			long minutes = Math.max(1, (left * 60L + perHour - 1) / perHour);
			parts.add("Done about " + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
				.format(Instant.ofEpochMilli(now + minutes * 60_000L).atZone(zone)));
		}
		if (!stackable && left > 0)
		{
			int inventories = (left + INVENTORY - 1) / INVENTORY;
			parts.add("~" + inventories + (inventories == 1 ? " inv" : " invs"));
		}
		if (banked > 0)
		{
			parts.add(String.format(Locale.ROOT, "%,d banked", banked));
		}
		return String.join(", ", parts);
	}

	/** Banked amounts as kept in the config: item:amount, comma separated; bad pairs are skipped. */
	static Map<Integer, Integer> parseBanked(String text)
	{
		Map<Integer, Integer> out = new LinkedHashMap<>();
		for (String pair : text == null ? new String[0] : text.split(","))
		{
			String[] parts = pair.trim().split(":");
			try
			{
				if (parts.length == 2 && Integer.parseInt(parts[1]) > 0)
				{
					out.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
				}
			}
			catch (NumberFormatException e)
			{
				// a broken number
			}
		}
		return out;
	}

	static String formatBanked(Map<Integer, Integer> banked)
	{
		return banked.entrySet().stream()
			.filter(e -> e.getValue() > 0)
			.map(e -> e.getKey() + ":" + e.getValue())
			.collect(Collectors.joining(","));
	}

	/**
	 * How much a tick's change in the item's inventory amount adds to the count: what arrived, or for a skill that uses
	 * items up, what went, and only while the skill is gaining xp.
	 */
	static int counted(Skill skill, int change, boolean gainingXp)
	{
		if (!gainingXp)
		{
			return 0;
		}
		return Math.max(0, usesUp(skill) ? -change : change);
	}
}
