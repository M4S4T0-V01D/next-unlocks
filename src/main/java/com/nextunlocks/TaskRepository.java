package com.nextunlocks;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.util.Text;

/** Quests and items from the bundled wiki data and diaries from the bundled unlocks, by wiki title. */
@Slf4j
class TaskRepository
{
	private static final String RESOURCE = "quests.json";
	private static final String ITEMS = "items.json";

	private static class Data
	{
		List<RawQuest> quests;
	}

	private static class ItemData
	{
		List<RawItem> items;
	}

	private static class RawItem
	{
		String name;
		List<Integer> ids;
		/** Skill to level, highest first. */
		LinkedHashMap<String, Integer> wield;
		List<String> quests;
		/** [skill, level, what it makes, its item ID or 0], highest first. */
		List<List<Object>> make;
	}

	private static class RawQuest
	{
		String name;
		String wiki;
		Map<String, Integer> skills;
		int questPoints;
		int combat;
		List<String> quests;
	}

	/** Prefix of the key of an item goal only the skill guide knows, followed by its item ID. */
	static final String GUIDE_ITEM = "item:";
	/** Prefix of a pinned skill guide unlock's key, followed by its skill, level and text, split by colons. */
	static final String UNLOCK = "unlock:";

	/** Turns an {@link #UNLOCK} key into its card, or null when the unlock isn't known. */
	private volatile Function<String, Task> unlocks = key -> null;
	private final Map<String, Task> unlockCache = new ConcurrentHashMap<>();

	private volatile Map<String, Task> byWiki = Collections.emptyMap();
	private volatile Map<Integer, Task> byItem = Collections.emptyMap();
	private volatile List<Task> items = Collections.emptyList();
	/** The bundled data alone, which {@link #addGuide} adds the skill guide's items to. */
	private Map<String, Task> bundledByWiki = Collections.emptyMap();
	private Map<Integer, Task> bundledByItem = Collections.emptyMap();
	private List<Task> bundledItems = Collections.emptyList();

	void load(Gson gson, Map<String, Map<Skill, Integer>> diaries) throws IOException
	{
		load(read(gson, RESOURCE, Data.class), read(gson, ITEMS, ItemData.class), diaries);
	}

	private static <T> T read(Gson gson, String resource, Class<T> type) throws IOException
	{
		try (InputStream in = TaskRepository.class.getResourceAsStream(resource))
		{
			if (in == null)
			{
				throw new IOException("missing " + resource);
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return gson.fromJson(reader, type);
			}
		}
	}

	private void load(Data data, ItemData itemData, Map<String, Map<Skill, Integer>> diaries)
	{
		Map<String, Task> tasks = new LinkedHashMap<>();
		for (RawQuest q : data.quests)
		{
			// a linked map keeps the data's order, highest level first
			Map<Skill, Integer> skills = new LinkedHashMap<>();
			if (q.skills != null)
			{
				q.skills.forEach((name, level) -> put(skills, name, level));
			}
			List<String> quests = q.quests == null ? Collections.emptyList() : q.quests;
			tasks.put(q.wiki, Task.of(q.name, q.wiki, false, skills, q.questPoints, q.combat, quests));
		}
		diaries.forEach((title, skills) -> tasks.put(title, Task.of(title, title, true, skills, 0, 0, Collections.emptyList())));

		Map<Integer, Task> ids = new HashMap<>();
		List<Task> itemTasks = new ArrayList<>();
		for (RawItem i : itemData.items)
		{
			if (i.ids == null || i.ids.isEmpty() || tasks.containsKey(i.name))
			{
				continue;
			}
			Map<Skill, Integer> wield = new LinkedHashMap<>();
			if (i.wield != null)
			{
				i.wield.forEach((name, level) -> put(wield, name, level));
			}
			Map<Skill, Integer> make = new LinkedHashMap<>();
			Map<Skill, String> madeFor = new HashMap<>();
			Map<Skill, Integer> materials = new HashMap<>();
			for (List<Object> m : i.make == null ? Collections.<List<Object>>emptyList() : i.make)
			{
				String name = (String) m.get(0);
				if (put(make, name, ((Number) m.get(1)).intValue()))
				{
					madeFor.put(Skill.valueOf(name), (String) m.get(2));
					int material = m.size() > 3 ? ((Number) m.get(3)).intValue() : 0;
					if (material > 0)
					{
						materials.put(Skill.valueOf(name), material);
					}
				}
			}
			Task task = new Task(i.name, i.name, false, wield, 0, 0, i.quests == null ? Collections.emptyList() : i.quests,
				i.ids.get(0), make, madeFor, materials, 0, false);
			tasks.put(i.name, task);
			itemTasks.add(task);
			i.ids.forEach(id -> ids.putIfAbsent(id, task));
		}
		bundledByWiki = byWiki = tasks;
		bundledByItem = byItem = ids;
		bundledItems = items = Collections.unmodifiableList(itemTasks);
	}

	/**
	 * Adds the items the game's skill guide lists: each one's guide levels go into the bundled item goal for it, or make
	 * a goal of their own, keyed {@link #GUIDE_ITEM} and the item ID, when the wiki data has none.
	 */
	void addGuide(Map<Integer, GuideItem> guide)
	{
		// every guide entry for one goal, since an item can be listed under several IDs and skills
		Map<Task, GuideItem> merged = new IdentityHashMap<>();
		Map<String, Task> tasks = new LinkedHashMap<>(bundledByWiki);
		Map<Integer, Task> ids = new HashMap<>(bundledByItem);
		List<Task> itemTasks = new ArrayList<>(bundledItems);
		for (Map.Entry<Integer, GuideItem> e : guide.entrySet())
		{
			Task task = ids.get(e.getKey());
			if (task == null)
			{
				task = new Task(e.getValue().getName(), GUIDE_ITEM + e.getKey(), false, Collections.emptyMap(), 0, 0,
					Collections.emptyList(), e.getKey(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), 0, false);
				ids.put(e.getKey(), task);
				itemTasks.add(task);
			}
			GuideItem all = merged.computeIfAbsent(task, t -> new GuideItem(t.getName()));
			e.getValue().getWield().forEach((skill, level) -> all.getWield().merge(skill, level, Math::max));
			e.getValue().getMake().forEach((skill, level) -> all.getMake().merge(skill, level, Math::max));
		}
		Map<Task, Task> replaced = new IdentityHashMap<>();
		merged.forEach((task, levels) -> replaced.put(task, withGuide(task, levels)));
		ids.replaceAll((id, task) -> replaced.getOrDefault(task, task));
		itemTasks.replaceAll(task -> replaced.getOrDefault(task, task));
		itemTasks.sort(Comparator.comparing(Task::getName, String.CASE_INSENSITIVE_ORDER));
		replaced.values().forEach(task -> tasks.put(task.getWiki(), task));
		byWiki = tasks;
		byItem = ids;
		items = Collections.unmodifiableList(itemTasks);
	}

	/** The goal with the guide's levels added, each skill keeping the higher level. */
	private static Task withGuide(Task task, GuideItem guide)
	{
		Map<Skill, Integer> wield = new LinkedHashMap<>(task.getSkills());
		guide.getWield().forEach((skill, level) -> wield.merge(skill, level, Math::max));
		Map<Skill, Integer> make = new LinkedHashMap<>(task.getMake());
		Map<Skill, String> madeFor = new HashMap<>(task.getMadeFor());
		Map<Skill, Integer> materials = new HashMap<>(task.getMaterials());
		guide.getMake().forEach((skill, level) ->
		{
			if (level > make.getOrDefault(skill, 0))
			{
				make.put(skill, level);
				madeFor.put(skill, task.getName());
				materials.put(skill, task.getItem());
			}
		});
		return new Task(task.getName(), task.getWiki(), false, highestFirst(wield), 0, 0, task.getQuests(), task.getItem(),
			highestFirst(make), madeFor, materials, 0, false);
	}

	private static Map<Skill, Integer> highestFirst(Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> sorted = new LinkedHashMap<>();
		levels.entrySet().stream()
			.sorted(Map.Entry.<Skill, Integer>comparingByValue().reversed())
			.forEach(e -> sorted.put(e.getKey(), e.getValue()));
		return sorted;
	}

	/** Puts a skill's level in by the skill's enum name; false for a skill this RuneLite doesn't have. */
	private static boolean put(Map<Skill, Integer> skills, String name, int level)
	{
		try
		{
			skills.put(Skill.valueOf(name), level);
			return true;
		}
		catch (IllegalArgumentException e)
		{
			log.debug("Skipping unknown skill {}", name);
			return false;
		}
	}

	/**
	 * The quest, diary, item goal or unlock with this key (its wiki title, {@link #GUIDE_ITEM} and an ID, or an
	 * {@link #UNLOCK} key), or null.
	 */
	Task get(String wiki)
	{
		if (wiki == null || !wiki.startsWith(UNLOCK))
		{
			return wiki == null ? null : byWiki.get(wiki);
		}
		Task task = unlockCache.get(wiki);
		if (task == null)
		{
			task = unlocks.apply(wiki);
			if (task != null)
			{
				unlockCache.put(wiki, task);
			}
		}
		return task;
	}

	/** Sets what turns {@link #UNLOCK} keys into cards, and forgets the ones made so far, as after the unlocks reload. */
	void setUnlocks(Function<String, Task> resolver)
	{
		unlocks = resolver;
		unlockCache.clear();
	}

	/** The key a skill guide unlock is pinned by. */
	static String unlockKey(Skill skill, Unlock unlock)
	{
		return UNLOCK + skill.name() + ":" + unlock.getLevel() + ":" + unlock.getText();
	}

	/** The skill in an {@link #UNLOCK} key, or null for any other key. */
	static Skill unlockSkill(String key)
	{
		String[] parts = key == null || !key.startsWith(UNLOCK) ? null : key.substring(UNLOCK.length()).split(":", 3);
		if (parts == null || parts.length < 3)
		{
			return null;
		}
		try
		{
			return Skill.valueOf(parts[0]);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/** The level in an {@link #UNLOCK} key, or 0. */
	static int unlockLevel(String key)
	{
		try
		{
			return unlockSkill(key) == null ? 0 : Integer.parseInt(key.substring(UNLOCK.length()).split(":", 3)[1]);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	/** The text in an {@link #UNLOCK} key, or null. */
	static String unlockText(String key)
	{
		return unlockSkill(key) == null ? null : key.substring(UNLOCK.length()).split(":", 3)[2];
	}

	/**
	 * The card for a skill guide unlock: its level in its skill and the quest it needs, by the quest's wiki title or
	 * name, or null for none.
	 */
	static Task unlockTask(Skill skill, Unlock unlock, String quest)
	{
		Map<Skill, Integer> skills = new LinkedHashMap<>();
		skills.put(skill, unlock.getLevel());
		return new Task(Text.removeTags(unlock.getText()), unlockKey(skill, unlock), false, skills, 0, 0,
			quest == null ? Collections.emptyList() : Collections.singletonList(quest), unlock.getItem() == null ? 0 : unlock.getItem(),
			Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), unlock.getSprite() == null ? 0 : unlock.getSprite(),
			true);
	}

	/** The quest's name for a wiki title, or the title when it isn't known. */
	String name(String wiki)
	{
		Task task = get(wiki);
		return task == null ? wiki : task.getName();
	}

	/** The item goal for an item ID (any of the item's versions, unnoted), or null. */
	Task byItem(int id)
	{
		return byItem.get(id);
	}

	/** Every item goal, in name order. */
	List<Task> items()
	{
		return items;
	}

	/**
	 * The goal a player most likely means by what they typed: the one of that name, or else the shortest name with it in,
	 * quests and diaries before items. Null for nothing, or blank text.
	 */
	Task find(String text)
	{
		String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty())
		{
			return null;
		}
		Task best = null;
		for (Task task : byWiki.values())
		{
			String name = task.getName().toLowerCase(Locale.ROOT);
			if (name.equals(query))
			{
				return task;
			}
			if (name.contains(query) && closer(task, best))
			{
				best = task;
			}
		}
		return best;
	}

	/** Whether a match beats the best so far: quests and diaries before items, then the shorter name. */
	private static boolean closer(Task task, Task best)
	{
		if (best == null)
		{
			return true;
		}
		if (task.isItem() != best.isItem())
		{
			return !task.isItem();
		}
		return task.getName().length() < best.getName().length();
	}

	/** The quest or diary with this name, or null. */
	Task byName(String name)
	{
		for (Task task : byWiki.values())
		{
			if (!task.isItem() && !task.isUnlock() && task.getName().equalsIgnoreCase(name))
			{
				return task;
			}
		}
		return null;
	}
}
