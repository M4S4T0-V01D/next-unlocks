package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

public class CombatLevelTest
{
	private static final NextUnlocksConfig CONFIG = new NextUnlocksConfig()
	{
	};

	private static Map<Skill, Integer> all(int level)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill skill : CombatLevel.SKILLS)
		{
			levels.put(skill, level);
		}
		return levels;
	}

	@Test
	public void maxedIsMaxCombat()
	{
		assertEquals(Experience.MAX_COMBAT_LEVEL, CombatLevel.level(all(99)));
	}

	@Test
	public void missingLevelsAreANewAccount()
	{
		assertEquals(3, CombatLevel.level(Collections.emptyMap()));
	}

	@Test
	public void neededIsTheFewestLevels()
	{
		Map<Skill, Integer> levels = all(40);
		int next = CombatLevel.level(levels) + 1;
		for (Skill skill : CombatLevel.SKILLS)
		{
			Integer needed = CombatLevel.needed(levels, skill, next);
			assertNotNull(skill.getName(), needed);
			assertTrue(needed > 0);
			Map<Skill, Integer> trial = new EnumMap<>(levels);
			trial.put(skill, 40 + needed);
			assertEquals(next, CombatLevel.level(trial));
			trial.put(skill, 40 + needed - 1);
			assertEquals(next - 1, CombatLevel.level(trial));
		}
	}

	@Test
	public void oneSkillCantReachEverything()
	{
		assertNull(CombatLevel.needed(Collections.emptyMap(), Skill.PRAYER, Experience.MAX_COMBAT_LEVEL));
		assertEquals(CombatLevel.SKILLS.size(), CombatLevel.needed(all(40), CombatLevel.level(all(40)) + 1).size());
	}

	@Test
	public void combatCardHasItsOwnLook()
	{
		CardTheme classic = CardTheme.classic(Palette.DEFAULT, new Color(70, 61, 50, 200), null);
		CardTheme combat = CardTheme.pinned(new Look(CONFIG, null), Palette.DEFAULT, classic, null, true);
		CardTheme plain = CardTheme.pinned(new Look(CONFIG, null), Palette.DEFAULT, classic, null, false);
		assertNotNull(combat);
		assertTrue(!combat.equals(plain));
	}

	@Test
	public void neededText()
	{
		assertEquals("+3", NextUnlocksPlugin.neededText(3));
		assertEquals("-", NextUnlocksPlugin.neededText(null));
	}
}
