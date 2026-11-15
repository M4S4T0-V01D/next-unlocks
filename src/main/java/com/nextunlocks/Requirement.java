package com.nextunlocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.Skill;

/** One hard requirement of a pinned quest, diary or item, and whether the player meets it. */
@Value
@AllArgsConstructor
class Requirement
{
	/** The skill's name, "Quest points", "Combat level" or the quest's name. */
	String label;
	/** What the player has against what is needed, like 61/75; empty for quests. */
	String progress;
	boolean met;
	/** For an item's making level, the material it's for, like Ironwood logs; null when it's for the item itself. */
	String material;
	/** The material's item ID, for its icon; 0 when unknown. */
	int materialItem;
	/** The skill, for a skill level; null otherwise. */
	Skill skill;
	/** The quest's wiki title, for a quest; null otherwise. */
	String quest;

	Requirement(String label, String progress, boolean met)
	{
		this(label, progress, met, null, 0, null, null);
	}

	/** Progress shown for a requirement checked off by hand. */
	static final String DONE = "Done";

	/** The requirements with the ones whose label is in {@code checked} counted as met, unmet ones still first. */
	static List<Requirement> checkOff(List<Requirement> requirements, Set<String> checked)
	{
		if (checked.isEmpty())
		{
			return requirements;
		}
		List<Requirement> out = new ArrayList<>();
		for (Requirement r : requirements)
		{
			out.add(r.met || !checked.contains(r.label) ? r
				: new Requirement(r.label, DONE, true, r.material, r.materialItem, r.skill, r.quest));
		}
		out.sort(Comparator.comparing(Requirement::isMet));
		return out;
	}

	/**
	 * The task's requirements, unmet ones first. {@code level} gives the level that counts for each skill,
	 * {@code questDone} takes a quest's wiki title and {@code questName} turns one into the quest's name.
	 */
	static List<Requirement> check(Task task, ToIntFunction<Skill> level, int questPoints, int combat,
		Predicate<String> questDone, Function<String, String> questName)
	{
		return check(task, level, questPoints, combat, questDone, questName, true);
	}

	/**
	 * As above; for an item goal, {@code make} also counts the levels that make it from scratch, each skill needing the
	 * higher of its wield and making level.
	 */
	static List<Requirement> check(Task task, ToIntFunction<Skill> level, int questPoints, int combat,
		Predicate<String> questDone, Function<String, String> questName, boolean make)
	{
		Map<Skill, Integer> need = new LinkedHashMap<>(task.getSkills());
		Map<Skill, String> material = new HashMap<>();
		if (make)
		{
			for (Map.Entry<Skill, Integer> e : task.getMake().entrySet())
			{
				if (e.getValue() > need.getOrDefault(e.getKey(), 0))
				{
					need.put(e.getKey(), e.getValue());
					String what = task.getMadeFor().get(e.getKey());
					if (what != null && !what.equals(task.getName()))
					{
						material.put(e.getKey(), what);
					}
				}
			}
		}
		List<Requirement> out = new ArrayList<>();
		for (Map.Entry<Skill, Integer> e : need.entrySet())
		{
			int have = level.applyAsInt(e.getKey());
			String what = material.get(e.getKey());
			out.add(new Requirement(e.getKey().getName(), have + "/" + e.getValue(), have >= e.getValue(), what,
				what == null ? 0 : task.getMaterials().getOrDefault(e.getKey(), 0), e.getKey(), null));
		}
		if (task.getQuestPoints() > 0)
		{
			out.add(new Requirement("Quest points", questPoints + "/" + task.getQuestPoints(), questPoints >= task.getQuestPoints()));
		}
		if (task.getCombat() > 0)
		{
			out.add(new Requirement("Combat level", combat + "/" + task.getCombat(), combat >= task.getCombat()));
		}
		for (String quest : task.getQuests())
		{
			out.add(new Requirement(questName.apply(quest), "", questDone.test(quest), null, 0, null, quest));
		}
		// stable, so each group keeps the data's order
		out.sort(Comparator.comparing(Requirement::isMet));
		return out;
	}
}
