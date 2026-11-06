package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class AlmostThereTest
{
	private UnlockRepository repository;

	@Before
	public void setUp() throws Exception
	{
		repository = new UnlockRepository();
		repository.load(new Gson());
	}

	private static Snapshot snapshot(int level, Skill exception, int exceptionLevel, Set<Integer> questsToDo, Set<String> finished)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			levels.put(skill, skill == exception ? exceptionLevel : level);
		}
		return new Snapshot(levels, Collections.emptyMap(), Collections.emptyMap(), u -> true, 5, Collections.emptySet(),
			Collections.emptySet(), Palette.DEFAULT, questsToDo, finished, Collections.emptyList(), true);
	}

	@Test
	public void onlyTheMissingSkillIsListed()
	{
		AlmostThere almost = AlmostThere.of(repository, snapshot(99, Skill.AGILITY, 1, Collections.emptySet(), Collections.emptySet()));
		assertFalse(almost.getOneLevelShort().isEmpty());
		long previous = -1;
		for (AlmostThere.OneLevelShort o : almost.getOneLevelShort())
		{
			assertEquals(Skill.AGILITY, o.getSkill());
			assertTrue(o.getXpToGo() >= previous);
			previous = o.getXpToGo();
		}
	}

	@Test
	public void finishedQuestsAreLeftOut()
	{
		AlmostThere almost = AlmostThere.of(repository, snapshot(99, Skill.AGILITY, 1, Collections.emptySet(), Collections.emptySet()));
		AlmostThere.OneLevelShort quest = almost.getOneLevelShort().stream()
			.filter(o -> o.getType() == Unlock.Type.QUEST).findFirst().orElseThrow(AssertionError::new);
		AlmostThere done = AlmostThere.of(repository, snapshot(99, Skill.AGILITY, 1, Collections.emptySet(), Collections.singleton(quest.getTitle())));
		assertTrue(done.getOneLevelShort().stream().noneMatch(o -> o.getTitle().equals(quest.getTitle())));
	}

	@Test
	public void unlocksWaitingOnAQuest()
	{
		Set<Integer> quests = repository.requiredQuests();
		if (quests.isEmpty())
		{
			// the bundled data has no skill-guide quest requirements
			return;
		}
		int quest = quests.iterator().next();
		AlmostThere almost = AlmostThere.of(repository, snapshot(99, null, 0, Collections.singleton(quest), Collections.emptySet()));
		assertEquals(1, almost.getQuestWaits().size());
		assertEquals(AlmostThere.questName(quest), almost.getQuestWaits().get(0).getQuest());
		assertTrue(AlmostThere.of(repository, snapshot(99, null, 0, Collections.emptySet(), Collections.emptySet())).getQuestWaits().isEmpty());
	}
}
