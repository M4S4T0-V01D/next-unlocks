package com.nextunlocks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;

final class Skills
{
	/** Skills in skills-tab order, mapped to their skills-tab component. */
	static final Map<Skill, Integer> STATS_TAB;

	static
	{
		Map<Skill, Integer> m = new LinkedHashMap<>();
		m.put(Skill.ATTACK, InterfaceID.Stats.ATTACK);
		m.put(Skill.STRENGTH, InterfaceID.Stats.STRENGTH);
		m.put(Skill.DEFENCE, InterfaceID.Stats.DEFENCE);
		m.put(Skill.RANGED, InterfaceID.Stats.RANGED);
		m.put(Skill.PRAYER, InterfaceID.Stats.PRAYER);
		m.put(Skill.MAGIC, InterfaceID.Stats.MAGIC);
		m.put(Skill.RUNECRAFT, InterfaceID.Stats.RUNECRAFT);
		m.put(Skill.CONSTRUCTION, InterfaceID.Stats.CONSTRUCTION);
		m.put(Skill.HITPOINTS, InterfaceID.Stats.HITPOINTS);
		m.put(Skill.AGILITY, InterfaceID.Stats.AGILITY);
		m.put(Skill.HERBLORE, InterfaceID.Stats.HERBLORE);
		m.put(Skill.THIEVING, InterfaceID.Stats.THIEVING);
		m.put(Skill.CRAFTING, InterfaceID.Stats.CRAFTING);
		m.put(Skill.FLETCHING, InterfaceID.Stats.FLETCHING);
		m.put(Skill.SLAYER, InterfaceID.Stats.SLAYER);
		m.put(Skill.HUNTER, InterfaceID.Stats.HUNTER);
		m.put(Skill.MINING, InterfaceID.Stats.MINING);
		m.put(Skill.SMITHING, InterfaceID.Stats.SMITHING);
		m.put(Skill.FISHING, InterfaceID.Stats.FISHING);
		m.put(Skill.COOKING, InterfaceID.Stats.COOKING);
		m.put(Skill.FIREMAKING, InterfaceID.Stats.FIREMAKING);
		m.put(Skill.WOODCUTTING, InterfaceID.Stats.WOODCUTTING);
		m.put(Skill.FARMING, InterfaceID.Stats.FARMING);
		m.put(Skill.SAILING, InterfaceID.Stats.SAILING);
		STATS_TAB = Collections.unmodifiableMap(m);
	}

	/** The skill of a skills tab widget, or null. */
	static Skill forStatsWidget(int widgetId)
	{
		for (Map.Entry<Skill, Integer> e : STATS_TAB.entrySet())
		{
			if (e.getValue() == widgetId)
			{
				return e.getKey();
			}
		}
		return null;
	}

	private Skills()
	{
	}
}
