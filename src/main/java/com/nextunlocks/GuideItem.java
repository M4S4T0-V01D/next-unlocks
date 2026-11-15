package com.nextunlocks;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.Getter;
import net.runelite.api.Skill;

/**
 * An item the game's skill guide lists, with every level its entries need: the guide keeps all of an entry's skills,
 * even the ones it only shows under another skill, so this is the game's own list of what the item needs.
 */
@Getter
class GuideItem
{
	/** Skills whose guide levels are for wielding or wearing, not making. */
	private static final Set<Skill> COMBAT = EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED,
		Skill.MAGIC, Skill.PRAYER, Skill.HITPOINTS);
	/** Guide sections of things to equip or use, like Woodcutting's axes. */
	private static final Pattern EQUIPMENT = Pattern.compile("(?i)weapon|armou?r|equipment|tool|axe|harpoon|outfit|cape|staff|staves|ammunition|shield|helm|boots|gloves");

	private final String name;
	private final Map<Skill, Integer> wield = new LinkedHashMap<>();
	private final Map<Skill, Integer> make = new LinkedHashMap<>();

	GuideItem(String name)
	{
		this.name = name;
	}

	/** Adds one level an entry needs; the section is the guide section listing it there, or null when hidden. */
	void add(Skill skill, int level, String section)
	{
		if (level <= 1)
		{
			return;
		}
		boolean equip = COMBAT.contains(skill) || (section != null && EQUIPMENT.matcher(section).find());
		(equip ? wield : make).merge(skill, level, Math::max);
	}

	boolean isEmpty()
	{
		return wield.isEmpty() && make.isEmpty();
	}
}
