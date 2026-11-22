package com.nextunlocks;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.Getter;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** How far the player is through a guide, from what the game says and the steps ticked off by hand. */
class GuideProgress
{
	/** What the game knows about the player, and what they ticked off. */
	interface Account
	{
		int level(Skill skill);

		int xp(Skill skill);

		boolean questDone(String wiki);

		boolean diaryDone(String title);

		boolean checked(String key);
	}

	@Getter
	private final Guide guide;
	private final Set<String> done = new HashSet<>();
	/** Capped skills past their cap, to the level they're at. */
	@Getter
	private final Map<Skill, Integer> broken = new EnumMap<>(Skill.class);
	/** Quest steps whose rewards could break a cap, by step key. */
	private final Map<String, String> warnings = new HashMap<>();

	private GuideProgress(Guide guide)
	{
		this.guide = guide;
	}

	static GuideProgress of(Guide guide, Account account, TaskRepository tasks)
	{
		GuideProgress progress = new GuideProgress(guide);
		for (Guide.Step step : guide.steps())
		{
			if (isDone(step, account))
			{
				progress.done.add(step.getKey());
			}
		}
		guide.getCaps().forEach((skill, cap) ->
		{
			int level = account.level(skill);
			if (level > cap)
			{
				progress.broken.put(skill, level);
			}
		});
		if (guide.isPure())
		{
			for (Guide.Step step : guide.steps())
			{
				if (step.getType() == Guide.Step.Type.QUEST && !progress.done.contains(step.getKey()))
				{
					String warning = warning(guide, step, account, tasks);
					if (warning != null)
					{
						progress.warnings.put(step.getKey(), warning);
					}
				}
			}
		}
		return progress;
	}

	private static boolean isDone(Guide.Step step, Account account)
	{
		switch (step.getType())
		{
			case QUEST:
				return account.questDone(step.getWiki());
			case DIARY:
				return account.diaryDone(step.getWiki());
			case LEVEL:
			case UNLOCK:
				return account.level(step.getSkill()) >= step.getLevel();
			default:
				return account.checked(step.getKey());
		}
	}

	/** What doing this quest, and the unfinished quests it needs, could do to the guide's caps; null if nothing. */
	private static String warning(Guide guide, Guide.Step step, Account account, TaskRepository tasks)
	{
		Set<String> left = new TreeSet<>();
		for (String quest : GuideRepository.chain(step.getWiki(), tasks))
		{
			if (!account.questDone(quest))
			{
				left.add(quest);
			}
		}
		List<String> lines = new ArrayList<>();
		for (Map.Entry<Skill, Integer> cap : guide.getCaps().entrySet())
		{
			Skill skill = cap.getKey();
			int fixed = 0;
			List<String> choices = new ArrayList<>();
			for (String quest : left)
			{
				TaskRepository.Reward reward = tasks.reward(quest);
				fixed += reward.getXp().getOrDefault(skill, 0);
				if (reward.getMaybe().contains(skill) || reward.isLamp())
				{
					choices.add(tasks.name(quest));
				}
			}
			if (fixed > 0 && account.xp(skill) + fixed >= limit(cap.getValue()))
			{
				lines.add("Gives " + NumberFormat.getIntegerInstance(Locale.US).format(fixed) + " " + skill.getName()
					+ " XP, past your " + skill.getName() + " " + cap.getValue() + " cap");
			}
			else if (!choices.isEmpty())
			{
				lines.add("Don't pick " + skill.getName() + " for the reward of " + String.join(", ", choices));
			}
		}
		return lines.isEmpty() ? null : String.join(". ", lines) + ".";
	}

	/** The XP that takes a skill past this cap. */
	static int limit(int cap)
	{
		return cap >= Experience.MAX_REAL_LEVEL ? Integer.MAX_VALUE : Experience.getXpForLevel(cap + 1);
	}

	/**
	 * A chat line for XP gained in one of the guide's capped skills, or null if the skill has no cap there. Gaining any
	 * XP there usually means the wrong attack style or spell.
	 */
	static String capWarning(Guide guide, Skill skill, int xp, int level)
	{
		Integer cap = guide.getCaps().get(skill);
		if (cap == null)
		{
			return null;
		}
		if (level > cap)
		{
			return skill.getName() + " is " + level + ": past the " + guide.getName() + " cap of " + cap + ".";
		}
		int left = limit(cap) - xp;
		return "You gained " + skill.getName() + " XP. " + NumberFormat.getIntegerInstance(Locale.US).format(left)
			+ " more and the " + guide.getName() + " cap of " + cap + " is broken.";
	}

	/** Whether the other progress, for the same guide, shows the same: the same steps done, caps broken and warnings. */
	boolean sameAs(GuideProgress other)
	{
		return other != null && done.equals(other.done) && broken.equals(other.broken) && warnings.equals(other.warnings);
	}

	boolean isDone(Guide.Step step)
	{
		return done.contains(step.getKey());
	}

	String warning(Guide.Step step)
	{
		return warnings.get(step.getKey());
	}

	int done()
	{
		return done.size();
	}

	int total()
	{
		return guide.steps().size();
	}

	boolean isComplete()
	{
		return done() == total();
	}

	/** The first steps not done yet, in guide order. */
	List<Guide.Step> upcoming(int count)
	{
		List<Guide.Step> next = new ArrayList<>();
		for (Guide.Step step : guide.steps())
		{
			if (next.size() >= count)
			{
				break;
			}
			if (!isDone(step))
			{
				next.add(step);
			}
		}
		return Collections.unmodifiableList(next);
	}
}
