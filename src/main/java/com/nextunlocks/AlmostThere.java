package com.nextunlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * What's one requirement away: quests and diaries short of a single skill level, and unlocks the player has the
 * level for that only wait on a quest.
 */
@Value
class AlmostThere
{
	static final AlmostThere NONE = new AlmostThere(Collections.emptyList(), Collections.emptyList());
	private static final Map<Integer, Quest> QUESTS = new HashMap<>();

	static
	{
		for (Quest quest : Quest.values())
		{
			QUESTS.put(quest.getId(), quest);
		}
	}

	/** A quest or diary needing one more skill level. */
	@Value
	static class OneLevelShort
	{
		Unlock.Type type;
		/** The quest or diary, as its wiki title. */
		String title;
		Skill skill;
		int level;
		long xpToGo;
	}

	/** Unlocks the player has the level for, waiting on one quest. */
	@Value
	static class QuestWait
	{
		String quest;
		/** Skill of each unlock, in the same order. */
		List<Skill> skills;
		List<Unlock> unlocks;
	}

	/** Closest first. */
	List<OneLevelShort> oneLevelShort;
	/** Most unlocks first. */
	List<QuestWait> questWaits;

	static AlmostThere of(UnlockRepository repository, Snapshot snapshot)
	{
		if (!snapshot.isLoggedIn())
		{
			return NONE;
		}
		return new AlmostThere(oneLevelShort(repository, snapshot), questWaits(repository, snapshot));
	}

	private static List<OneLevelShort> oneLevelShort(UnlockRepository repository, Snapshot snapshot)
	{
		// every skill's quest and diary entries, gathered per quest or diary
		Map<String, Map<Skill, Integer>> requirements = new LinkedHashMap<>();
		Map<String, Unlock.Type> types = new HashMap<>();
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			for (Unlock u : repository.all(skill))
			{
				if (u.getType() == Unlock.Type.UNLOCK || !snapshot.getFilter().test(u))
				{
					continue;
				}
				String title = u.getWiki() != null ? u.getWiki() : u.getText();
				requirements.computeIfAbsent(title, t -> new EnumMap<>(Skill.class)).merge(skill, u.getLevel(), Math::max);
				types.put(title, u.getType());
			}
		}

		List<OneLevelShort> result = new ArrayList<>();
		for (Map.Entry<String, Map<Skill, Integer>> e : requirements.entrySet())
		{
			String title = e.getKey();
			if (types.get(title) == Unlock.Type.QUEST && snapshot.getFinishedQuests().contains(title))
			{
				continue;
			}
			Skill missing = null;
			int count = 0;
			for (Map.Entry<Skill, Integer> req : e.getValue().entrySet())
			{
				if (snapshot.effectiveLevel(req.getKey()) < req.getValue())
				{
					missing = req.getKey();
					count++;
				}
			}
			// a quest or diary with every level met is done or only waiting on things this plugin can't see
			if (count == 1)
			{
				int level = e.getValue().get(missing);
				long xp = Math.max(0, Experience.getXpForLevel(level) - snapshot.xp(missing));
				result.add(new OneLevelShort(types.get(title), title, missing, level, xp));
			}
		}
		result.sort(Comparator.comparingLong(OneLevelShort::getXpToGo));
		return result;
	}

	private static List<QuestWait> questWaits(UnlockRepository repository, Snapshot snapshot)
	{
		Map<Integer, QuestWait> waits = new LinkedHashMap<>();
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			for (Unlock u : repository.all(skill))
			{
				Integer quest = u.getQuest();
				if (quest == null || !snapshot.getQuestsToDo().contains(quest) || !snapshot.shown(skill).test(u))
				{
					continue;
				}
				int level = UnlockRepository.boostable(skill, u) ? snapshot.effectiveLevel(skill) : snapshot.level(skill);
				if (u.getLevel() > level)
				{
					continue;
				}
				QuestWait wait = waits.computeIfAbsent(quest, id -> new QuestWait(questName(id), new ArrayList<>(), new ArrayList<>()));
				wait.skills.add(skill);
				wait.unlocks.add(u);
			}
		}
		List<QuestWait> result = new ArrayList<>(waits.values());
		result.sort(Comparator.comparingInt((QuestWait w) -> -w.unlocks.size()).thenComparing(QuestWait::getQuest));
		return result;
	}

	static String questName(int id)
	{
		Quest quest = QUESTS.get(id);
		return quest == null ? "Quest " + id : quest.getName();
	}
}
