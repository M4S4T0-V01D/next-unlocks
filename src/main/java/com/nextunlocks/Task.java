package com.nextunlocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Skill;

/** A quest, achievement diary or item that can be pinned, with its hard requirements. */
@Value
class Task
{
	/** The quest's name, as in {@link net.runelite.api.Quest#getName()}, or the diary's or item's title. */
	String name;
	/** The wiki page, which pins are kept by. */
	String wiki;
	boolean diary;
	Map<Skill, Integer> skills;
	int questPoints;
	int combat;
	/** Wiki titles of the quests needed first, the whole chain; for an item, the ones needed to wield or get it. */
	List<String> quests;
	/** For an item goal, the item ID its icon shows; 0 for quests and diaries. */
	int item;
	/** For an item goal, the levels that make it from scratch; {@link #skills} are the ones to wield or wear it. */
	Map<Skill, Integer> make;
	/** For each making level, what it makes: the item itself or a material, like Ironwood logs. */
	Map<Skill, String> madeFor;
	/** The item ID of each making level's material, for its icon; missing when unknown. */
	Map<Skill, Integer> materials;
	/** For a pinned skill guide unlock, like a spell or prayer, keyed {@link TaskRepository#UNLOCK}: its sprite, or 0. */
	int sprite;
	/** Whether this is a skill guide unlock, whose {@link #item} is only its icon. */
	boolean unlock;

	/** A quest or diary. */
	static Task of(String name, String wiki, boolean diary, Map<Skill, Integer> skills, int questPoints, int combat,
		List<String> quests)
	{
		return new Task(name, wiki, diary, skills, questPoints, combat, quests, 0, Collections.emptyMap(), Collections.emptyMap(),
			Collections.emptyMap(), 0, false);
	}

	/** An item goal; an unlock with an item icon isn't one. */
	boolean isItem()
	{
		return item > 0 && !unlock;
	}
}
