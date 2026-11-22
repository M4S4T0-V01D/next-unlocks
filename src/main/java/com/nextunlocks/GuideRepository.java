package com.nextunlocks;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

/** The bundled guides, with every step checked against the bundled quests, diaries, items and unlocks. */
@Slf4j
class GuideRepository
{
	private static final String RESOURCE = "guides.json";

	private static class Data
	{
		List<RawGuide> guides;
	}

	private static class RawGuide
	{
		String id;
		String name;
		String category;
		String about;
		String look;
		String account;
		boolean members;
		Map<String, Integer> caps;
		List<RawGroup> groups;
	}

	private static class RawGroup
	{
		String name;
		List<RawStep> steps;
	}

	private static class RawStep
	{
		String quest;
		String diary;
		String skill;
		int level;
		String unlock;
		String item;
		String text;
		String id;
		Integer icon;
		String why;
	}

	private volatile List<Guide> guides = Collections.emptyList();
	/** Steps left out because the data they name wasn't found; empty unless the bundled guides are out of date. */
	private final List<String> problems = new ArrayList<>();

	void load(Gson gson, TaskRepository tasks, UnlockRepository unlocks) throws IOException
	{
		try (InputStream in = GuideRepository.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IOException("missing " + RESOURCE);
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				load(gson.fromJson(reader, Data.class), tasks, unlocks);
			}
		}
	}

	private void load(Data data, TaskRepository tasks, UnlockRepository unlocks)
	{
		problems.clear();
		List<Guide> loaded = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (RawGuide g : data.guides)
		{
			if (g.id == null || !ids.add(g.id))
			{
				problem(g.name + ": missing or repeated id");
				continue;
			}
			Map<Skill, Integer> caps = new EnumMap<>(Skill.class);
			if (g.caps != null)
			{
				g.caps.forEach((name, level) ->
				{
					Skill skill = skill(name);
					if (skill == null)
					{
						problem(g.id + ": unknown cap skill " + name);
					}
					else
					{
						caps.put(skill, level);
					}
				});
			}
			Set<String> keys = new HashSet<>();
			List<Guide.Group> groups = new ArrayList<>();
			for (RawGroup group : g.groups)
			{
				List<Guide.Step> steps = new ArrayList<>();
				for (RawStep raw : group.steps)
				{
					Guide.Step step = step(raw, tasks, unlocks);
					if (step == null)
					{
						problem(g.id + ": no data for step " + describe(raw));
					}
					else if (!keys.add(step.getKey()))
					{
						problem(g.id + ": repeated step " + step.getKey());
					}
					else
					{
						steps.add(step);
					}
				}
				if (!steps.isEmpty())
				{
					groups.add(new Guide.Group(group.name, Collections.unmodifiableList(steps)));
				}
			}
			loaded.add(new Guide(g.id, g.name, g.category == null ? "Other" : g.category, g.about, parse(Guide.Style.class, g.look, Guide.Style.STARTER),
				parse(Guide.Account.class, g.account, Guide.Account.ANY), g.members, Collections.unmodifiableMap(caps),
				Collections.unmodifiableList(groups)));
		}
		guides = Collections.unmodifiableList(loaded);
	}

	private static Guide.Step step(RawStep raw, TaskRepository tasks, UnlockRepository unlocks)
	{
		if (raw.quest != null)
		{
			Task task = tasks.get(raw.quest);
			return task == null || task.isDiary() || task.isItem() ? null
				: new Guide.Step(Guide.Step.Type.QUEST, "quest:" + raw.quest, task.getName(), raw.quest, null, 0, null, raw.icon, raw.why);
		}
		if (raw.diary != null)
		{
			Task task = tasks.get(raw.diary);
			return task == null || !task.isDiary() ? null
				: new Guide.Step(Guide.Step.Type.DIARY, "diary:" + raw.diary, raw.diary, raw.diary, null, 0, null, raw.icon, raw.why);
		}
		if (raw.item != null)
		{
			Task task = tasks.get(raw.item);
			return task == null || !task.isItem() ? null
				: new Guide.Step(Guide.Step.Type.ITEM, "item:" + raw.item, raw.item, raw.item, null, 0, null, task.getItem(), raw.why);
		}
		if (raw.text != null)
		{
			return raw.id == null ? null
				: new Guide.Step(Guide.Step.Type.TEXT, "text:" + raw.id, raw.text, null, null, 0, null, raw.icon, raw.why);
		}
		Skill skill = skill(raw.skill);
		if (skill == null || raw.level < 1 || raw.level > 99)
		{
			return null;
		}
		String title = skill.getName() + " " + raw.level;
		if (raw.unlock == null)
		{
			return new Guide.Step(Guide.Step.Type.LEVEL, "level:" + skill.name() + ":" + raw.level, title, null, skill, raw.level,
				null, raw.icon, raw.why);
		}
		String match = raw.unlock.toLowerCase(Locale.ROOT);
		List<Unlock> found = unlocks.all(skill).stream()
			.filter(u -> u.getLevel() == raw.level && u.getText().toLowerCase(Locale.ROOT).contains(match))
			.collect(Collectors.toList());
		if (found.size() != 1)
		{
			return null;
		}
		Unlock unlock = found.get(0);
		return new Guide.Step(Guide.Step.Type.UNLOCK, TaskRepository.unlockKey(skill, unlock), title, unlock.getWiki(), skill,
			raw.level, unlock, raw.icon != null ? raw.icon : unlock.getItem(), raw.why);
	}

	private static Skill skill(String name)
	{
		try
		{
			return name == null ? null : Skill.valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback)
	{
		try
		{
			return name == null ? fallback : Enum.valueOf(type, name);
		}
		catch (IllegalArgumentException e)
		{
			return fallback;
		}
	}

	private static String describe(RawStep raw)
	{
		return raw.quest != null ? raw.quest : raw.diary != null ? raw.diary : raw.item != null ? raw.item
			: raw.text != null ? raw.text : raw.skill + " " + raw.level + (raw.unlock == null ? "" : " " + raw.unlock);
	}

	private void problem(String message)
	{
		log.debug("Guides: {}", message);
		problems.add(message);
	}

	List<Guide> all()
	{
		return guides;
	}

	Guide get(String id)
	{
		return guides.stream().filter(g -> g.getId().equals(id)).findFirst().orElse(null);
	}

	List<String> problems()
	{
		return Collections.unmodifiableList(problems);
	}

	/** The quest and the quests it needs first, the whole chain the player has to do for it. */
	static Set<String> chain(String wiki, TaskRepository tasks)
	{
		Set<String> chain = new HashSet<>();
		List<String> todo = new ArrayList<>(Collections.singletonList(wiki));
		while (!todo.isEmpty())
		{
			String quest = todo.remove(todo.size() - 1);
			Task task = tasks.get(quest);
			if (task != null && !task.isItem() && chain.add(quest))
			{
				todo.addAll(task.getQuests());
			}
		}
		return chain;
	}
}
