package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class RequirementTest
{
	private final TaskRepository tasks = new TaskRepository();

	@Before
	public void load() throws Exception
	{
		UnlockRepository repository = new UnlockRepository();
		repository.load(new Gson());
		tasks.load(new Gson(), repository.diaries());
	}

	@Test
	public void unmetComeFirst()
	{
		Map<Skill, Integer> skills = new LinkedHashMap<>();
		skills.put(Skill.MAGIC, 75);
		skills.put(Skill.MINING, 40);
		Task task = new Task("Q", "Q", false, skills, 50, 70, Arrays.asList("Done quest", "Open quest"));
		List<Requirement> r = Requirement.check(task, s -> s == Skill.MAGIC ? 61 : 45, 60, 65,
			"Done quest"::equals, Function.identity());

		assertEquals(Arrays.asList(
			new Requirement("Magic", "61/75", false),
			new Requirement("Combat level", "65/70", false),
			new Requirement("Open quest", "", false),
			new Requirement("Mining", "45/40", true),
			new Requirement("Quest points", "60/50", true),
			new Requirement("Done quest", "", true)), r);
	}

	@Test
	public void noRequirements()
	{
		Task task = new Task("Q", "Q", false, Collections.emptyMap(), 0, 0, Collections.emptyList());
		assertTrue(Requirement.check(task, s -> 1, 0, 3, q -> false, Function.identity()).isEmpty());
	}

	@Test
	public void bundledQuestsLoad()
	{
		Task dt2 = tasks.byName("Desert Treasure II - The Fallen Empire");
		assertNotNull(dt2);
		assertFalse(dt2.isDiary());
		assertEquals(Integer.valueOf(75), dt2.getSkills().get(Skill.MAGIC));
		assertTrue(dt2.getQuests().contains("Desert Treasure I"));
		assertNull(tasks.get("Not a quest"));
	}

	@Test
	public void diariesKeepTheirHighestSkillLevel()
	{
		Task diary = tasks.get("Elite Ardougne Diary");
		assertNotNull(diary);
		assertTrue(diary.isDiary());
		assertFalse(diary.getSkills().isEmpty());
		int previous = Integer.MAX_VALUE;
		for (int level : diary.getSkills().values())
		{
			assertTrue(level <= previous);
			previous = level;
		}
	}
}
