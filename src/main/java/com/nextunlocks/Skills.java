package com.nextunlocks;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
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

	/** A color per skill, after its icon, for goal bars. */
	static final Map<Skill, Color> COLORS;

	static
	{
		Map<Skill, Color> m = new EnumMap<>(Skill.class);
		m.put(Skill.ATTACK, new Color(0xC4352B));
		m.put(Skill.STRENGTH, new Color(0x2E9E5C));
		m.put(Skill.DEFENCE, new Color(0x6A84D4));
		m.put(Skill.RANGED, new Color(0x7FA332));
		m.put(Skill.PRAYER, new Color(0xE6E2B8));
		m.put(Skill.MAGIC, new Color(0x4A6BE0));
		m.put(Skill.RUNECRAFT, new Color(0xC9B48A));
		m.put(Skill.CONSTRUCTION, new Color(0xB0915F));
		m.put(Skill.HITPOINTS, new Color(0xD93B3B));
		m.put(Skill.AGILITY, new Color(0x4A6FB8));
		m.put(Skill.HERBLORE, new Color(0x3FAF3F));
		m.put(Skill.THIEVING, new Color(0x8C5AA6));
		m.put(Skill.CRAFTING, new Color(0xA8794A));
		m.put(Skill.FLETCHING, new Color(0x3E9E92));
		m.put(Skill.SLAYER, new Color(0xA8302A));
		m.put(Skill.HUNTER, new Color(0x9E8A56));
		m.put(Skill.MINING, new Color(0x7A9AA6));
		m.put(Skill.SMITHING, new Color(0x9A9A9A));
		m.put(Skill.FISHING, new Color(0x6EA6D6));
		m.put(Skill.COOKING, new Color(0x9C46B4));
		m.put(Skill.FIREMAKING, new Color(0xE87D1E));
		m.put(Skill.WOODCUTTING, new Color(0x9A7840));
		m.put(Skill.FARMING, new Color(0x5A9E38));
		m.put(Skill.SAILING, new Color(0x2E9AC4));
		COLORS = Collections.unmodifiableMap(m);
	}

	static Color color(Skill skill)
	{
		return COLORS.getOrDefault(skill, Color.WHITE);
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
