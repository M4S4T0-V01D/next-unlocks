package com.nextunlocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import lombok.Value;
import net.runelite.api.Skill;

/** One hard requirement of a pinned quest or diary, and whether the player meets it. */
@Value
class Requirement
{
	/** The skill's name, "Quest points", "Combat level" or the quest's name. */
	String label;
	/** What the player has against what is needed, like 61/75; empty for quests. */
	String progress;
	boolean met;

	/**
	 * The task's requirements, unmet ones first. {@code level} gives the level that counts for each skill,
	 * {@code questDone} takes a quest's wiki title and {@code questName} turns one into the quest's name.
	 */
	static List<Requirement> check(Task task, ToIntFunction<Skill> level, int questPoints, int combat,
		Predicate<String> questDone, Function<String, String> questName)
	{
		List<Requirement> out = new ArrayList<>();
		for (Map.Entry<Skill, Integer> e : task.getSkills().entrySet())
		{
			int have = level.applyAsInt(e.getKey());
			out.add(new Requirement(e.getKey().getName(), have + "/" + e.getValue(), have >= e.getValue()));
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
			out.add(new Requirement(questName.apply(quest), "", questDone.test(quest)));
		}
		// stable, so each group keeps the data's order
		out.sort(Comparator.comparing(Requirement::isMet));
		return out;
	}
}
