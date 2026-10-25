package com.nextunlocks;

import lombok.Value;

@Value
public class Unlock
{
	public enum Type
	{
		UNLOCK,
		QUEST,
		DIARY
	}

	int level;
	boolean members;
	Type type;
	String text;
	/** Wiki page title or search, or null when the entry links nowhere. */
	String wiki;
	/** Item whose image pictures the entry, or null. */
	Integer item;
	/** Spell or prayer sprite picturing the entry, or null. */
	Integer sprite;
	/** More about the entry, such as its requirements, one line per newline; or null. */
	String note;
	/** The skill guide section listing the entry, such as "Armour", or null. */
	String section;
	/** Quest (its id, as in {@link net.runelite.api.Quest#getId()}) the entry requires, or null. */
	Integer quest;

	boolean hasIcon()
	{
		return item != null || sprite != null;
	}
}
