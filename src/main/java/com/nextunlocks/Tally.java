package com.nextunlocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
