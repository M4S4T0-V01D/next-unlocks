package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class GuideProgressTest
{
	private final TaskRepository tasks = new TaskRepository();
	private final GuideRepository guides = new GuideRepository();

	/** A made-up account: every skill at 1 unless set, nothing done. */
	private static class Fake implements GuideProgress.Account
	{
		final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		final Set<String> quests = new HashSet<>();
		final Set<String> diaries = new HashSet<>();
		final Set<String> checked = new HashSet<>();

		@Override
		public int level(Skill skill)
		{
			return levels.getOrDefault(skill, skill == Skill.HITPOINTS ? 10 : 1);
		}

		@Override
		public int xp(Skill skill)
		{
			int level = level(skill);
			return level == 1 ? 0 : Experience.getXpForLevel(level);
		}

		@Override
		public boolean questDone(String wiki)
		{
			return quests.contains(wiki);
		}

		@Override
		public boolean diaryDone(String title)
		{
			return diaries.contains(title);
		}

		@Override
		public boolean checked(String key)
		{
			return checked.contains(key);
		}
	}

	@Before
	public void load() throws Exception
	{
		UnlockRepository unlocks = new UnlockRepository();
		unlocks.load(new Gson());
		tasks.load(new Gson(), unlocks.diaries());
		guides.load(new Gson(), tasks, unlocks);
	}

	@Test
	public void valeTotemsStartsWithChildrenOfTheSun()
	{
		Guide guide = guides.get("vale-totems");
		Fake account = new Fake();
		GuideProgress progress = GuideProgress.of(guide, account, tasks);
		assertEquals(0, progress.done());
		assertEquals("quest:Children of the Sun", progress.upcoming(1).get(0).getKey());

		account.quests.add("Children of the Sun");
		account.levels.put(Skill.FLETCHING, 20);
		progress = GuideProgress.of(guide, account, tasks);
		assertEquals(2, progress.done());
		assertEquals(Skill.WOODCUTTING, progress.upcoming(1).get(0).getSkill());
	}

	@Test
	public void handTickedStepsCount()
	{
		Guide guide = guides.get("vale-totems");
		Fake account = new Fake();
		account.checked.add("text:log-basket");
		GuideProgress progress = GuideProgress.of(guide, account, tasks);
		Guide.Step basket = guide.steps().stream().filter(s -> s.getKey().equals("text:log-basket")).findFirst().get();
		assertTrue(progress.isDone(basket));
		assertEquals(1, progress.done());
		assertFalse(progress.isComplete());
	}

	@Test
	public void sameProgressKeepsTheCard()
	{
		Guide guide = guides.get("pure-1def");
		Fake account = new Fake();
		GuideProgress before = GuideProgress.of(guide, account, tasks);
		assertTrue(before.sameAs(GuideProgress.of(guide, account, tasks)));
		assertFalse(before.sameAs(null));
		account.levels.put(Skill.DEFENCE, 2);
		assertFalse(before.sameAs(GuideProgress.of(guide, account, tasks)));
		assertEquals(guide, guides.get("pure-1def"));
	}

	@Test
	public void brokenCaps()
	{
		Guide guide = guides.get("pure-1def");
		Fake account = new Fake();
		assertTrue(GuideProgress.of(guide, account, tasks).getBroken().isEmpty());
		account.levels.put(Skill.DEFENCE, 3);
		assertEquals(Integer.valueOf(3), GuideProgress.of(guide, account, tasks).getBroken().get(Skill.DEFENCE));
	}

	@Test
	public void warnsAboutRewardChoices()
	{
		Guide guide = guides.get("pure-1def");
		Guide.Step mm1 = guide.steps().stream().filter(s -> "Monkey Madness I".equals(s.getWiki())).findFirst().orElse(null);
		assertNotNull(mm1);
		String warning = GuideProgress.of(guide, new Fake(), tasks).warning(mm1);
		assertNotNull(warning);
		assertTrue(warning, warning.startsWith("Don't pick Defence"));
	}

	@Test
	public void warnsWhenQuestXpWouldPassTheCap()
	{
		Guide guide = guides.get("pure-zerker");
		Fake account = new Fake();
		// a level 45 zerker has little Defence XP to spare for the quests left
		account.levels.put(Skill.DEFENCE, 45);
		GuideProgress progress = GuideProgress.of(guide, account, tasks);
		boolean any = guide.steps().stream().map(progress::warning).anyMatch(w -> w != null && w.startsWith("Gives"));
		assertTrue(any);
	}

	@Test
	public void capWarnings()
	{
		Guide guide = guides.get("pure-1def");
		assertNull(GuideProgress.capWarning(guide, Skill.ATTACK, 1000, 10));
		String line = GuideProgress.capWarning(guide, Skill.DEFENCE, 40, 1);
		assertTrue(line, line.contains("43 more"));
		assertTrue(GuideProgress.capWarning(guide, Skill.DEFENCE, 200, 2).contains("past"));
	}

	@Test
	public void everyDiaryStepHasAVarbit()
	{
		for (Guide guide : guides.all())
		{
			for (Guide.Step step : guide.steps())
			{
				if (step.getType() == Guide.Step.Type.DIARY)
				{
					assertNotNull(step.getWiki(), Diaries.completeVarbit(step.getWiki()));
				}
			}
		}
		assertNotNull(Diaries.completeVarbit("Elite Kourend & Kebos Diary"));
		assertNotNull(Diaries.completeVarbit("Medium Karamja Diary"));
	}
}
