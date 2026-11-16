package com.nextunlocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** The combat level from the seven combat skills, and how many levels of each one alone reach a higher combat level. */
final class CombatLevel
{
	/** The combat level card's pin, apart from skills and wiki titles. */
	static final Object PIN = new Object();
	/** The combat level card's own look, kept with the skills' looks under this name. */
	static final String LOOK = "COMBAT";
	/** In the order the card lists them. */
	static final List<Skill> SKILLS = Collections.unmodifiableList(Arrays.asList(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
		Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER));

	private CombatLevel()
	{
	}

	static boolean isCombat(Skill skill)
	{
		return SKILLS.contains(skill);
	}

	/** The levels a missing skill counts as: 10 Hitpoints, 1 for the rest, like a new account. */
	private static int get(Map<Skill, Integer> levels, Skill skill)
	{
		Integer level = levels.get(skill);
		return level != null ? level : skill == Skill.HITPOINTS ? 10 : 1;
	}

	static int level(Map<Skill, Integer> levels)
	{
		return Experience.getCombatLevel(get(levels, Skill.ATTACK), get(levels, Skill.STRENGTH), get(levels, Skill.DEFENCE),
			get(levels, Skill.HITPOINTS), get(levels, Skill.MAGIC), get(levels, Skill.RANGED), get(levels, Skill.PRAYER));
	}

	static double precise(Map<Skill, Integer> levels)
	{
		return Experience.getCombatLevelPrecise(get(levels, Skill.ATTACK), get(levels, Skill.STRENGTH), get(levels, Skill.DEFENCE),
			get(levels, Skill.HITPOINTS), get(levels, Skill.MAGIC), get(levels, Skill.RANGED), get(levels, Skill.PRAYER));
	}

	/** Levels of the skill alone that take the combat level to the target, or null when 99 isn't enough. */
	static Integer needed(Map<Skill, Integer> levels, Skill skill, int target)
	{
		Map<Skill, Integer> trial = new EnumMap<>(Skill.class);
		for (Skill s : SKILLS)
		{
			trial.put(s, get(levels, s));
		}
		int start = trial.get(skill);
		for (int l = start; l <= Experience.MAX_REAL_LEVEL; l++)
		{
			trial.put(skill, l);
			if (level(trial) >= target)
			{
				return l - start;
			}
		}
		return null;
	}

	/** {@link #needed} for every combat skill, in the card's order. */
	static Map<Skill, Integer> needed(Map<Skill, Integer> levels, int target)
	{
		Map<Skill, Integer> needed = new EnumMap<>(Skill.class);
		for (Skill skill : SKILLS)
		{
			needed.put(skill, needed(levels, skill, target));
		}
		return needed;
	}
}
