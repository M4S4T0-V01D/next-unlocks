package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class RequirementTest
{
	private final TaskRepository tasks = new TaskRepository();

	@Before
	public void load() throws Exception
	{
		UnlockRepository repository = new UnlockRepository();
		repository.load(new Gson());
		tasks.load(new Gson(), repository.diaries());
	}

	@Test
	public void unmetComeFirst()
	{
		Map<Skill, Integer> skills = new LinkedHashMap<>();
		skills.put(Skill.MAGIC, 75);
		skills.put(Skill.MINING, 40);
		Task task = Task.of("Q", "Q", false, skills, 50, 70, Arrays.asList("Done quest", "Open quest"));
		List<Requirement> r = Requirement.check(task, s -> s == Skill.MAGIC ? 61 : 45, 60, 65,
			"Done quest"::equals, Function.identity());

		assertEquals(Arrays.asList(
			new Requirement("Magic", "61/75", false, null, 0, Skill.MAGIC, null),
			new Requirement("Combat level", "65/70", false),
			new Requirement("Open quest", "", false, null, 0, null, "Open quest"),
			new Requirement("Mining", "45/40", true, null, 0, Skill.MINING, null),
			new Requirement("Quest points", "60/50", true),
			new Requirement("Done quest", "", true, null, 0, null, "Done quest")), r);
	}

	@Test
	public void noRequirements()
	{
		Task task = Task.of("Q", "Q", false, Collections.emptyMap(), 0, 0, Collections.emptyList());
		assertTrue(Requirement.check(task, s -> 1, 0, 3, q -> false, Function.identity()).isEmpty());
	}

	@Test
	public void bundledQuestsLoad()
	{
		Task dt2 = tasks.byName("Desert Treasure II - The Fallen Empire");
		assertNotNull(dt2);
		assertFalse(dt2.isDiary());
		assertEquals(Integer.valueOf(75), dt2.getSkills().get(Skill.MAGIC));
		assertTrue(dt2.getQuests().contains("Desert Treasure I"));
		assertNull(tasks.get("Not a quest"));
	}

	@Test
	public void itemsCountTheirMakingLevels()
	{
		Map<Skill, Integer> wield = new LinkedHashMap<>();
		wield.put(Skill.RANGED, 55);
		wield.put(Skill.FLETCHING, 80);
		Map<Skill, Integer> make = new LinkedHashMap<>();
		make.put(Skill.WOODCUTTING, 80);
		make.put(Skill.FLETCHING, 72);
		Map<Skill, String> madeFor = new LinkedHashMap<>();
		madeFor.put(Skill.WOODCUTTING, "Ironwood logs");
		madeFor.put(Skill.FLETCHING, "Bow");
		Task task = new Task("Bow", "Bow", false, wield, 0, 0, Collections.emptyList(), 1, make, madeFor,
			Collections.singletonMap(Skill.WOODCUTTING, 32907), 0, false);

		assertEquals(Arrays.asList(
			new Requirement("Ranged", "50/55", false, null, 0, Skill.RANGED, null),
			new Requirement("Fletching", "50/80", false, null, 0, Skill.FLETCHING, null),
			new Requirement("Woodcutting", "50/80", false, "Ironwood logs", 32907, Skill.WOODCUTTING, null)),
			Requirement.check(task, s -> 50, 0, 3, q -> false, Function.identity(), true));
		assertEquals(2, Requirement.check(task, s -> 50, 0, 3, q -> false, Function.identity(), false).size());
	}

	@Test
	public void bundledItemsLoad()
	{
		Task blowpipe = tasks.get("Ironwood blowpipe");
		assertNotNull(blowpipe);
		assertTrue(blowpipe.isItem());
		assertEquals(Integer.valueOf(55), blowpipe.getSkills().get(Skill.RANGED));
		assertEquals(Integer.valueOf(80), blowpipe.getMake().get(Skill.WOODCUTTING));
		assertEquals("Ironwood logs", blowpipe.getMadeFor().get(Skill.WOODCUTTING));
		assertEquals(Integer.valueOf(32907), blowpipe.getMaterials().get(Skill.WOODCUTTING));
		assertEquals(blowpipe, tasks.byItem(blowpipe.getItem()));
		assertNull(tasks.byName("Ironwood blowpipe"));

		Task scimitar = tasks.get("Dragon scimitar");
		assertNotNull(scimitar);
		assertTrue(scimitar.getQuests().contains("Monkey Madness I"));
		assertFalse(tasks.get("Desert Treasure II - The Fallen Empire").isItem());
	}

	@Test
	public void skillGuideItemsBecomeGoals()
	{
		Task blowpipe = tasks.get("Ironwood blowpipe");
		Map<Integer, GuideItem> guide = new HashMap<>();
		GuideItem higher = new GuideItem("Ironwood blowpipe");
		higher.add(Skill.FLETCHING, 90, "Darts");
		higher.add(Skill.RANGED, 40, null);
		guide.put(blowpipe.getItem(), higher);
		GuideItem only = new GuideItem("Mystery thing");
		only.add(Skill.SMITHING, 61, "Bars");
		only.add(Skill.DEFENCE, 42, null);
		only.add(Skill.CRAFTING, 1, null);
		guide.put(999_999, only);
		tasks.addGuide(guide);

		Task merged = tasks.get("Ironwood blowpipe");
		assertEquals(Integer.valueOf(55), merged.getSkills().get(Skill.RANGED));
		assertEquals(Integer.valueOf(90), merged.getMake().get(Skill.FLETCHING));
		assertEquals("Ironwood blowpipe", merged.getMadeFor().get(Skill.FLETCHING));
		assertEquals("Ironwood logs", merged.getMadeFor().get(Skill.WOODCUTTING));
		assertEquals(merged, tasks.byItem(blowpipe.getItem()));

		Task added = tasks.get(TaskRepository.GUIDE_ITEM + 999_999);
		assertNotNull(added);
		assertEquals(added, tasks.byItem(999_999));
		assertEquals("Mystery thing", added.getName());
		assertEquals(Integer.valueOf(42), added.getSkills().get(Skill.DEFENCE));
		assertEquals(Integer.valueOf(61), added.getMake().get(Skill.SMITHING));
		assertFalse(added.getMake().containsKey(Skill.CRAFTING));
		assertTrue(tasks.items().contains(added));

		// reading the guide again starts from the bundled data, not the last merge
		tasks.addGuide(new HashMap<>());
		assertNull(tasks.get(TaskRepository.GUIDE_ITEM + 999_999));
		assertEquals(blowpipe, tasks.get("Ironwood blowpipe"));
	}

	@Test
	public void checkedOffCountAsMet()
	{
		List<Requirement> r = Arrays.asList(new Requirement("Magic", "61/75", false), new Requirement("Open quest", "", false),
			new Requirement("Mining", "45/40", true));
		assertEquals(Arrays.asList(new Requirement("Magic", "61/75", false), new Requirement("Open quest", Requirement.DONE, true),
			new Requirement("Mining", "45/40", true)), Requirement.checkOff(r, Collections.singleton("Open quest")));
		assertEquals(r, Requirement.checkOff(r, Collections.emptySet()));
	}

	@Test
	public void unlocksPinByKey()
	{
		Unlock spell = new Unlock(66, true, Unlock.Type.UNLOCK, "<col=ff0000>Vengeance: other</col>", "Vengeance", 0, 564, null,
			null, 1);
		String key = TaskRepository.unlockKey(Skill.MAGIC, spell);
		assertEquals(Skill.MAGIC, TaskRepository.unlockSkill(key));
		assertEquals(66, TaskRepository.unlockLevel(key));
		assertEquals(spell.getText(), TaskRepository.unlockText(key));
		assertNull(TaskRepository.unlockSkill("Dragon Slayer I"));

		Task task = TaskRepository.unlockTask(Skill.MAGIC, spell, "Lunar Diplomacy");
		assertEquals("Vengeance: other", task.getName());
		assertTrue(task.isUnlock());
		assertFalse(task.isItem());
		assertEquals(564, task.getSprite());
		assertEquals(Integer.valueOf(66), task.getSkills().get(Skill.MAGIC));
		assertEquals(Collections.singletonList("Lunar Diplomacy"), task.getQuests());

		int[] calls = {0};
		tasks.setUnlocks(k -> {
			calls[0]++;
			return k.equals(key) ? task : null;
		});
		assertEquals(task, tasks.get(key));
		assertEquals(task, tasks.get(key));
		assertEquals(1, calls[0]);
		assertNull(tasks.get(TaskRepository.UNLOCK + "MAGIC:1:Nothing"));
		assertNull(tasks.byName("Vengeance: other"));
	}

	@Test
	public void diariesKeepTheirHighestSkillLevel()
	{
		Task diary = tasks.get("Elite Ardougne Diary");
		assertNotNull(diary);
		assertTrue(diary.isDiary());
		assertFalse(diary.getSkills().isEmpty());
		int previous = Integer.MAX_VALUE;
		for (int level : diary.getSkills().values())
		{
			assertTrue(level <= previous);
			previous = level;
		}
	}
}
