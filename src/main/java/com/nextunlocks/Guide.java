package com.nextunlocks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.Skill;

/** A bundled guide: ordered groups of steps, and for pure builds the levels some skills must stay at. */
@Value
// guides key the in-game cards every frame, and the id is what tells them apart
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class Guide
{
	/** How the guide's cards look. */
	enum Style
	{
		/** Warm parchment with gold trim. */
		STARTER,
		/** Grey and riveted. */
		IRON,
		/** Quest blue with a scroll header. */
		QUESTS,
		/** Dark with blood red and a skull. */
		PURE,
		/** Forest green with bark-brown trim, for skilling minigames and activities. */
		SKILLING
	}

	/** Which accounts the guide is shown first to. */
	enum Account
	{
		ANY,
		MAIN,
		IRON
	}

	@EqualsAndHashCode.Include
	String id;
	String name;
	/** The heading the guide is listed under, such as "Account builds". */
	String category;
	String about;
	Style style;
	Account account;
	boolean members;
	/** Skill to the highest level it may reach; empty for guides that aren't pure builds. */
	Map<Skill, Integer> caps;
	List<Group> groups;

	@Value
	static class Group
	{
		String name;
		List<Step> steps;
	}

	@Value
	static class Step
	{
		enum Type
		{
			QUEST,
			DIARY,
			LEVEL,
			/** A skill guide unlock: a level with something to show for it. */
			UNLOCK,
			/** An item to get, ticked off by hand. */
			ITEM,
			/** Anything else, ticked off by hand. */
			TEXT
		}

		Type type;
		/** Unique within the guide; also the key a hand-ticked step is saved under. */
		String key;
		/** What the step says, such as "Waterfall Quest" or "Attack 60". */
		String title;
		/** Wiki title of the quest, diary or item, or null. */
		String wiki;
		/** The skill of a level or unlock step, or null. */
		Skill skill;
		int level;
		/** The skill guide unlock of an unlock step, or null. */
		Unlock unlock;
		/** Item whose image pictures the step, or null. */
		Integer icon;
		/** Why the step is worth doing. */
		String why;

		boolean isManual()
		{
			return type == Type.ITEM || type == Type.TEXT;
		}
	}

	boolean isPure()
	{
		return !caps.isEmpty();
	}

	/** Every step in order. */
	List<Step> steps()
	{
		return groups.stream().flatMap(g -> g.getSteps().stream()).collect(Collectors.toList());
	}
}
