package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class GuideRepositoryTest
{
	private final TaskRepository tasks = new TaskRepository();
	private final GuideRepository guides = new GuideRepository();

	@Before
	public void load() throws Exception
	{
		UnlockRepository unlocks = new UnlockRepository();
		unlocks.load(new Gson());
		tasks.load(new Gson(), unlocks.diaries());
		guides.load(new Gson(), tasks, unlocks);
	}

	@Test
	public void everyStepFindsItsData()
	{
		assertEquals(Collections.emptyList(), guides.problems());
		assertEquals(12, guides.all().size());
		for (Guide guide : guides.all())
		{
			assertFalse(guide.getId(), guide.steps().isEmpty());
			for (Guide.Step step : guide.steps())
			{
				assertNotNull(guide.getId() + " " + step.getKey(), step.getWhy());
			}
		}
	}

	/** Quest steps tick themselves off through RuneLite's quest list, which goes by the quest's name. */
	@Test
	public void runeLiteKnowsEveryQuestStep()
	{
		Set<String> known = new HashSet<>();
		for (Quest quest : Quest.values())
		{
			known.add(quest.getName());
		}
		for (Guide guide : guides.all())
		{
			for (Guide.Step step : guide.steps())
			{
				if (step.getType() == Guide.Step.Type.QUEST)
				{
					assertTrue(guide.getId() + ": " + step.getWiki(), known.contains(tasks.name(step.getWiki())));
				}
			}
		}
	}

	@Test
	public void readsQuestRewards()
	{
		assertEquals(13750, (int) tasks.reward("Waterfall Quest").getXp().get(Skill.ATTACK));
		assertTrue(tasks.reward("Monkey Madness I").getMaybe().contains(Skill.DEFENCE));
		assertTrue(tasks.reward("Recipe for Disaster").isLamp());
		assertFalse(tasks.reward("Bone Voyage").mayGive(Skill.DEFENCE));
	}

	/** Doing every quest in a pure guide, with all the quests they need first, keeps each capped skill under its cap. */
	@Test
	public void pureQuestsKeepTheirCaps()
	{
		for (Guide guide : guides.all())
		{
			if (!guide.isPure())
			{
				continue;
			}
			Set<String> quests = new HashSet<>();
			for (Guide.Step step : guide.steps())
			{
				if (step.getType() == Guide.Step.Type.QUEST)
				{
					quests.addAll(GuideRepository.chain(step.getWiki(), tasks));
				}
			}
			for (Map.Entry<Skill, Integer> cap : guide.getCaps().entrySet())
			{
				int xp = quests.stream().mapToInt(q -> tasks.reward(q).getXp().getOrDefault(cap.getKey(), 0)).sum();
				int limit = cap.getValue() == 1 ? 1 : Experience.getXpForLevel(cap.getValue() + 1);
				assertTrue(guide.getId() + " gives " + xp + " " + cap.getKey() + " XP", xp < limit);
			}
		}
	}

	@Test
	public void chainsIncludeEarlierQuests()
	{
		Set<String> chain = GuideRepository.chain("Recipe for Disaster", tasks);
		assertTrue(chain.contains("Nature Spirit"));
		assertTrue(chain.contains("Recipe for Disaster"));
	}
}
