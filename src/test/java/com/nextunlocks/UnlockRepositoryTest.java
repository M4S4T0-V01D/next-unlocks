package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Predicate;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class UnlockRepositoryTest
{
	private static final Predicate<Unlock> ALL = u -> true;

	private UnlockRepository repository;

	@Before
	public void setUp() throws Exception
	{
		repository = new UnlockRepository();
		repository.load(new Gson());
	}

	@Test
	public void everySkillHasData()
	{
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			List<Unlock> unlocks = repository.all(skill);
			assertFalse(skill + " has no unlocks", unlocks.isEmpty());
			for (Unlock u : unlocks)
			{
				assertTrue(u.getLevel() >= 1 && u.getLevel() <= 99);
				assertFalse(u.getText().isEmpty());
				assertFalse(skill + ": leftover markup in " + u.getText(), u.getText().matches(".*[{}\\[\\]|].*"));
			}
		}
	}

	@Test
	public void runePickaxeAt41Mining()
	{
		assertTrue(repository.at(Skill.MINING, 41, ALL).stream()
			.anyMatch(u -> u.getText().equals("Use rune pickaxes") && !u.isMembers()));
	}

	@Test
	public void itemsAndSpellsHavePictures()
	{
		assertTrue(repository.at(Skill.MINING, 41, ALL).stream()
			.anyMatch(u -> u.getText().equals("Use rune pickaxes") && u.getItem() != null));
		assertTrue(repository.at(Skill.MAGIC, 1, ALL).stream()
			.anyMatch(u -> u.getText().startsWith("Cast Wind Strike") && u.getSprite() != null));
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			for (Unlock u : repository.all(skill))
			{
				assertFalse(skill + ": quest or diary with a picture: " + u.getText(), u.getType() != Unlock.Type.UNLOCK && u.hasIcon());
			}
		}
	}

	@Test
	public void upcomingStopsAfterMaxLevelsAndSkipsCurrent()
	{
		NavigableMap<Integer, List<Unlock>> next = repository.upcoming(Skill.MINING, 40, ALL, 3);
		assertEquals(3, next.size());
		assertTrue(next.firstKey() > 40);
		assertEquals(41, (int) next.firstKey());
	}

	@Test
	public void filterHidesMembersAndQuests()
	{
		Predicate<Unlock> f2pNoQuests = u -> !u.isMembers() && u.getType() != Unlock.Type.QUEST;
		repository.upcoming(Skill.MINING, 1, f2pNoQuests, 99).values().stream()
			.flatMap(List::stream)
			.forEach(u ->
			{
				assertFalse(u.isMembers());
				assertTrue(u.getType() != Unlock.Type.QUEST);
			});
	}

	@Test
	public void nothingAbove99()
	{
		assertTrue(repository.upcoming(Skill.ATTACK, 99, ALL, 5).isEmpty());
	}

	@Test
	public void skillGuideTuplesOutsideTheGuideAreSkipped()
	{
		// overview (no level), a hidden requirement of another skill (subsection -1), and an unknown skill id
		assertNull(UnlockRepository.guideUnlock(10, -1, 0, null, "Overview", null, true, null, null, null));
		assertNull(UnlockRepository.guideUnlock(1, 40, -1, null, "Rune halberd", null, true, null, null, null));
		assertNull(UnlockRepository.guideUnlock(99, 10, 1, null, "Unknown", null, true, null, null, null));
		assertNull(UnlockRepository.guideUnlock(16, 100, 1, null, "Over 99", null, true, null, null, null));
		Unlock chest = UnlockRepository.guideUnlock(10, 43, 3, "Chests", "Chest upstairs in Ardougne", null, true, 1003, null, null);
		assertEquals(43, chest.getLevel());
		assertEquals(Unlock.Type.UNLOCK, chest.getType());
		assertEquals("Chest upstairs in Ardougne", chest.getWiki());
	}

	@Test
	public void skillGuideReplacesWikiUnlocksButKeepsQuestsAndDiaries()
	{
		long wikiRequirements = repository.all(Skill.THIEVING).stream().filter(u -> u.getType() != Unlock.Type.UNLOCK).count();
		assertTrue(wikiRequirements > 0);
		Unlock chest = UnlockRepository.guideUnlock(10, 43, 3, "Chests", "Chest upstairs in Ardougne", null, true, 1003, null, null);
		repository.setSkillGuide(Collections.singletonMap(Skill.THIEVING, Collections.singletonList(chest)));

		assertTrue(repository.isSkillGuide());
		List<Unlock> thieving = repository.all(Skill.THIEVING);
		assertEquals(1, thieving.stream().filter(u -> u.getType() == Unlock.Type.UNLOCK).count());
		assertEquals(wikiRequirements, thieving.stream().filter(u -> u.getType() != Unlock.Type.UNLOCK).count());
		assertTrue(repository.at(Skill.THIEVING, 43, ALL).contains(chest));
		// skills the guide didn't cover keep the wiki's unlocks
		assertTrue(repository.at(Skill.MINING, 41, ALL).stream().anyMatch(u -> u.getText().equals("Use rune pickaxes")));
	}

	@Test
	public void boostsCountOnlyWhereTheGameAllowsThem()
	{
		Unlock armour = UnlockRepository.guideUnlock(1, 40, 2, "Armour", "Rune armour", null, false, 1127, null, null);
		Unlock shortcut = UnlockRepository.guideUnlock(2, 30, 3, "Shortcuts", "Shortcut", null, true, null, null, null);
		Unlock spell = UnlockRepository.guideUnlock(4, 55, 1, "Standard Spellbook", "High Level Alchemy", null, false, null, 41, null);
		Unlock staff = UnlockRepository.guideUnlock(4, 60, 7, "Weapons", "Staff", null, true, 1, null, null);
		Unlock ore = UnlockRepository.guideUnlock(13, 85, 1, "Rocks", "Runite ore", null, true, 451, null, null);
		assertFalse(UnlockRepository.boostable(Skill.ATTACK, armour));
		assertTrue(UnlockRepository.boostable(Skill.STRENGTH, shortcut));
		assertTrue(UnlockRepository.boostable(Skill.MAGIC, spell));
		assertFalse(UnlockRepository.boostable(Skill.MAGIC, staff));
		assertTrue(UnlockRepository.boostable(Skill.MINING, ore));

		Map<Skill, Integer> levels = Collections.singletonMap(Skill.MINING, 83);
		Snapshot boosted = new Snapshot(levels, Collections.emptyMap(), Collections.singletonMap(Skill.MINING, 86), ALL, 5,
			Collections.emptySet(), Collections.emptySet(), Palette.DEFAULT);
		assertTrue(boosted.reachedByBoost(Skill.MINING, ore));
		assertFalse(boosted.upcoming(Skill.MINING).test(ore));
	}

	@Test
	public void questLockedAndHiddenSections()
	{
		Unlock chest = UnlockRepository.guideUnlock(10, 43, 3, "Chests", "Chest", null, true, null, null, 7);
		Snapshot snapshot = new Snapshot(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), ALL, 5,
			Collections.singleton(7), Collections.singleton(Snapshot.sectionKey(Skill.THIEVING, "Chests")), Palette.DEFAULT);
		assertTrue(snapshot.locked(chest));
		assertFalse(snapshot.shown(Skill.THIEVING).test(chest));
		assertTrue(snapshot.shown(Skill.CRAFTING).test(chest));
	}

	@Test
	public void previousIsTheLastUnlockLevelReached()
	{
		assertEquals(41, repository.previous(Skill.MINING, 41, ALL));
		assertTrue(repository.previous(Skill.MINING, 40, ALL) < 41);
		assertEquals(1, repository.previous(Skill.MINING, 1, u -> false));
	}
}
