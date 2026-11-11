package com.nextunlocks;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

/** Quests from the bundled wiki data and diaries from the bundled unlocks, by wiki title. */
@Slf4j
class TaskRepository
{
	private static final String RESOURCE = "quests.json";

	private static class Data
	{
		List<RawQuest> quests;
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

	private Map<String, Task> byWiki = Collections.emptyMap();

	void load(Gson gson, Map<String, Map<Skill, Integer>> diaries) throws IOException
	{
		try (InputStream in = TaskRepository.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IOException("missing " + RESOURCE);
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				load(gson.fromJson(reader, Data.class), diaries);
			}
		}
	}

	private void load(Data data, Map<String, Map<Skill, Integer>> diaries)
	{
		Map<String, Task> tasks = new LinkedHashMap<>();
		for (RawQuest q : data.quests)
		{
			// a linked map keeps the data's order, highest level first
			Map<Skill, Integer> skills = new LinkedHashMap<>();
			if (q.skills != null)
			{
				q.skills.forEach((name, level) ->
				{
					try
					{
						skills.put(Skill.valueOf(name), level);
					}
					catch (IllegalArgumentException e)
					{
						log.debug("Skipping unknown skill {}", name);
					}
				});
			}
			List<String> quests = q.quests == null ? Collections.emptyList() : q.quests;
			tasks.put(q.wiki, new Task(q.name, q.wiki, false, skills, q.questPoints, q.combat, quests));
		}
		diaries.forEach((title, skills) ->
			tasks.put(title, new Task(title, title, true, skills, 0, 0, Collections.emptyList())));
		byWiki = tasks;
	}

	/** The quest or diary with this wiki title, or null. */
	Task get(String wiki)
	{
		return wiki == null ? null : byWiki.get(wiki);
	}

	/** The quest's name for a wiki title, or the title when it isn't known. */
	String name(String wiki)
	{
		Task task = get(wiki);
		return task == null ? wiki : task.getName();
	}

	/** The quest or diary with this name, or null. */
	Task byName(String name)
	{
		for (Task task : byWiki.values())
		{
			if (task.getName().equalsIgnoreCase(name))
			{
				return task;
			}
		}
		return null;
	}
}
