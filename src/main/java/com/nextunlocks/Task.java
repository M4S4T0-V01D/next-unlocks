package com.nextunlocks;

import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Skill;

/** A quest or achievement diary that can be pinned, with its hard requirements. */
@Value
class Task
{
	/** The quest's name, as in {@link net.runelite.api.Quest#getName()}, or the diary's title. */
	String name;
	/** The wiki page, which pins are kept by. */
	String wiki;
	boolean diary;
	Map<Skill, Integer> skills;
	int questPoints;
	int combat;
	/** Wiki titles of the quests needed first, the whole chain. */
	List<String> quests;
}
