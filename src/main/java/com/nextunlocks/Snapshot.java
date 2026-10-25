package com.nextunlocks;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Value;
import net.runelite.api.Skill;

/** Player stats and settings handed from the client thread to the panel and tooltips. */
@Value
class Snapshot
{
	static final Snapshot EMPTY = new Snapshot(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
		u -> true, 5, Collections.emptySet(), Collections.emptySet(), Palette.DEFAULT);

	/** Real levels. */
	Map<Skill, Integer> levels;
	Map<Skill, Integer> xp;
	/** Boosted levels, for skills whose boost counts; empty when boosts don't count. */
	Map<Skill, Integer> boosted;
	/** The members, quest and diary settings. */
	Predicate<Unlock> filter;
	int levelsAhead;
	/** Required quests the player hasn't finished; empty when unknown or not shown. */
	Set<Integer> unfinishedQuests;
	/** Hidden skill guide sections, as {@link #sectionKey} strings. */
	Set<String> hiddenSections;
	Palette palette;

	static String sectionKey(Skill skill, String section)
	{
		return skill.name() + ":" + section;
	}

	boolean isLoggedIn()
	{
		return !levels.isEmpty();
	}

	int level(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	int xp(Skill skill)
	{
		return xp.getOrDefault(skill, 0);
	}

	/** Whether the unlock is above the real level but within the current boost, and takes boosts. */
	boolean reachedByBoost(Skill skill, Unlock u)
	{
		return u.getLevel() > level(skill) && u.getLevel() <= boosted.getOrDefault(skill, 0) && UnlockRepository.boostable(skill, u);
	}

	/** Whether the unlock needs a quest the player hasn't finished. */
	boolean locked(Unlock u)
	{
		return u.getQuest() != null && unfinishedQuests.contains(u.getQuest());
	}

	boolean hidden(Skill skill, Unlock u)
	{
		return u.getSection() != null && hiddenSections.contains(sectionKey(skill, u.getSection()));
	}

	/** The settings' filter without hidden sections, for the unlocks still to come: boosts count as reached. */
	Predicate<Unlock> upcoming(Skill skill)
	{
		return u -> filter.test(u) && !hidden(skill, u) && !reachedByBoost(skill, u);
	}

	/** The settings' filter without hidden sections. */
	Predicate<Unlock> shown(Skill skill)
	{
		return u -> filter.test(u) && !hidden(skill, u);
	}
}
