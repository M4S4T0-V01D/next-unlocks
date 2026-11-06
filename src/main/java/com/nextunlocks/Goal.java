package com.nextunlocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import net.runelite.api.Skill;

/** An unlock the player has set as a goal, kept by skill, level and text so it survives a data reload. */
@Value
class Goal
{
	private static final String FIELD = "|";
	private static final String SEPARATOR = "\n";

	Skill skill;
	int level;
	String text;

	static Goal of(Skill skill, Unlock u)
	{
		return new Goal(skill, u.getLevel(), u.getText());
	}

	boolean is(Skill skill, Unlock u)
	{
		return this.skill == skill && level == u.getLevel() && text.equals(u.getText());
	}

	/** Goals from the config string; entries that can't be read are skipped. */
	static List<Goal> parse(String config)
	{
		List<Goal> goals = new ArrayList<>();
		for (String line : config.split(SEPARATOR))
		{
			String[] parts = line.split("\\" + FIELD, 3);
			if (parts.length < 3)
			{
				continue;
			}
			try
			{
				goals.add(new Goal(Skill.valueOf(parts[0].trim()), Integer.parseInt(parts[1].trim()), parts[2]));
			}
			catch (IllegalArgumentException ignored)
			{
				// a skill that no longer exists, or a mangled level
			}
		}
		return goals;
	}

	static String format(Collection<Goal> goals)
	{
		return goals.stream()
			.map(g -> g.skill.name() + FIELD + g.level + FIELD + g.text)
			.collect(Collectors.joining(SEPARATOR));
	}
}
