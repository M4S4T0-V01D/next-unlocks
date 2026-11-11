package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

public class GoalTest
{
	@Test
	public void formatAndParseRoundTrip()
	{
		List<Goal> goals = Arrays.asList(new Goal(Skill.SMITHING, 88, "Rune platebody"), new Goal(Skill.MAGIC, 55, "High Level Alchemy | spell"));
		assertEquals(goals, Goal.parse(Goal.format(goals)));
	}

	@Test
	public void badEntriesAreSkipped()
	{
		assertTrue(Goal.parse("").isEmpty());
		assertEquals(Arrays.asList(new Goal(Skill.MINING, 85, "Runite ore")),
			Goal.parse("NOT_A_SKILL|5|x\nMINING|abc|y\nMINING\nMINING|85|Runite ore"));
	}

	@Test
	public void levelGoalsRoundTrip()
	{
		Goal goal = Goal.level(Skill.FISHING, 99);
		assertEquals("Level 99", goal.getText());
		assertEquals(Arrays.asList(goal), Goal.parse(Goal.format(Arrays.asList(goal))));
	}

	@Test
	public void everySkillHasAColor()
	{
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			assertTrue(skill.getName(), Skills.COLORS.containsKey(skill));
		}
	}

	@Test
	public void matchesItsUnlock()
	{
		Unlock ore = UnlockRepository.guideUnlock(13, 85, 1, "Rocks", "Runite ore", null, true, 451, null, null);
		Goal goal = Goal.of(Skill.MINING, ore);
		assertTrue(goal.is(Skill.MINING, ore));
		assertTrue(!goal.is(Skill.SMITHING, ore));
	}
}
