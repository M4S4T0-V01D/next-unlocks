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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Text;

/**
 * Level unlocks per skill. Unlocks come from the game's skill guide, read from the cache once the client has it;
 * quest and diary requirements, and the unlocks until then, come from the bundled OSRS Wiki "Level up table" pages.
 */
@Slf4j
public class UnlockRepository
{
	private static final String RESOURCE = "unlocks.json";

	private static class Data
	{
		String fetched;
		Map<String, List<Unlock>> skills;
	}

	/** Skills in the order of the skill guide's skill ids, which start at 1. */
	private static final Skill[] GUIDE_SKILLS = {
		null, Skill.ATTACK, Skill.STRENGTH, Skill.RANGED, Skill.MAGIC, Skill.DEFENCE, Skill.HITPOINTS, Skill.PRAYER,
		Skill.AGILITY, Skill.HERBLORE, Skill.THIEVING, Skill.CRAFTING, Skill.RUNECRAFT, Skill.MINING, Skill.SMITHING,
		Skill.FISHING, Skill.COOKING, Skill.FIREMAKING, Skill.WOODCUTTING, Skill.FLETCHING, Skill.SLAYER, Skill.FARMING,
		Skill.CONSTRUCTION, Skill.HUNTER, Skill.SAILING,
	};
	/** The skill for a skill guide skill id, or null. */
	static Skill guideSkill(int id)
	{
		return id > 0 && id < GUIDE_SKILLS.length ? GUIDE_SKILLS[id] : null;
	}


	private Map<Skill, List<Unlock>> wiki = Collections.emptyMap();
	/** Replaced whole, so the panel can read it while the client thread loads the skill guide. */
	private volatile Map<Skill, List<Unlock>> bySkill = Collections.emptyMap();
	@Getter
	private String fetched = "";
	@Getter
	private volatile boolean skillGuide;
	/** Skill guide section headers per skill, in guide order. */
	private volatile Map<Skill, List<String>> sections = Collections.emptyMap();

	public void load(Gson gson) throws IOException
	{
		try (InputStream in = UnlockRepository.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IOException("missing " + RESOURCE);
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				load(gson.fromJson(reader, Data.class));
			}
		}
	}

	private void load(Data data)
	{
		Map<Skill, List<Unlock>> bySkill = new EnumMap<>(Skill.class);
		fetched = data.fetched == null ? "" : data.fetched;
		for (Map.Entry<String, List<Unlock>> e : data.skills.entrySet())
		{
			Skill skill;
			try
			{
				skill = Skill.valueOf(e.getKey());
			}
			catch (IllegalArgumentException ex)
			{
				log.debug("Skipping unknown skill {}", e.getKey());
				continue;
			}
			bySkill.put(skill, sorted(e.getValue()));
		}
		wiki = bySkill;
		this.bySkill = bySkill;
		skillGuide = false;
	}

	private static List<Unlock> sorted(List<Unlock> unlocks)
	{
		List<Unlock> list = new ArrayList<>(unlocks);
		list.sort(Comparator.comparingInt(Unlock::getLevel));
		return Collections.unmodifiableList(list);
	}

	/** Reads the skill guide's features from the cache. Client thread, once the game state is past LOADING. */
	public void loadSkillGuide(Client client)
	{
		Map<Skill, List<Unlock>> guide = new EnumMap<>(Skill.class);
		Map<Integer, String> questNames = new HashMap<>();
		Map<Skill, TreeMap<Integer, String>> ordered = new EnumMap<>(Skill.class);
		Map<Long, String> headers = new HashMap<>();
		for (int row : client.getDBTableRows(DBTableID.SkillGuideSubsections.ID))
		{
			Integer skillId = integer(client, row, DBTableID.SkillGuideSubsections.COL_SKILL);
			Integer id = integer(client, row, DBTableID.SkillGuideSubsections.COL_ID);
			String header = string(client, row, DBTableID.SkillGuideSubsections.COL_HEADER);
			if (skillId == null || id == null || skillId <= 0 || skillId >= GUIDE_SKILLS.length || header.isEmpty())
			{
				continue;
			}
			headers.put(sectionKey(skillId, id), header);
			Integer order = integer(client, row, DBTableID.SkillGuideSubsections.COL_CUSTOM_ORDER);
			ordered.computeIfAbsent(GUIDE_SKILLS[skillId], k -> new TreeMap<>()).put(order != null ? order : id, header);
		}
		for (int row : client.getDBTableRows(DBTableID.SkillFeatures.ID))
		{
			String[] lines = Text.removeTags(string(client, row, DBTableID.SkillFeatures.COL_TEXT).replace("<br>", "\n")).split("\n");
			String text = lines[0].trim();
			if (text.isEmpty())
			{
				continue;
			}
			List<String> notes = new ArrayList<>();
			for (int i = 1; i < lines.length; i++)
			{
				if (!lines[i].trim().isEmpty())
				{
					notes.add(lines[i].trim());
				}
			}
			Integer quest = integer(client, row, DBTableID.SkillFeatures.COL_QUEST);
			if (quest != null)
			{
				notes.add("Requires " + questNames.computeIfAbsent(quest, q -> string(client, q, DBTableID.Quest.COL_DISPLAYNAME)));
			}
			String other = string(client, row, DBTableID.SkillFeatures.COL_OTHERREQ);
			if (!other.isEmpty())
			{
				notes.add("Requires " + other);
			}
			Integer sprite = integer(client, row, DBTableID.SkillFeatures.COL_SPRITE);
			Integer item = sprite != null ? null : integer(client, row, DBTableID.SkillFeatures.COL_ICON);
			if (item != null && (item <= 0 || item == ItemID.INVIS_ROD))
			{
				item = null;
			}
			boolean members = bool(field(client, row, DBTableID.SkillFeatures.COL_MEMBERSONLY, 0));

			// the skill column lists (skill, level, subsection) tuples; each read returns one position across all of them
			Object[] skillIds = field(client, row, DBTableID.SkillFeatures.COL_SKILL, 0);
			Object[] levels = field(client, row, DBTableID.SkillFeatures.COL_SKILL, 1);
			Object[] subsections = field(client, row, DBTableID.SkillFeatures.COL_SKILL, 2);
			int count = Math.min(skillIds.length, Math.min(levels.length, subsections.length));
			for (int i = 0; i < count; i++)
			{
				if (!(skillIds[i] instanceof Number) || !(levels[i] instanceof Number) || !(subsections[i] instanceof Number))
				{
					continue;
				}
				int skillId = ((Number) skillIds[i]).intValue();
				int subsection = ((Number) subsections[i]).intValue();
				Unlock unlock = guideUnlock(skillId, ((Number) levels[i]).intValue(), subsection, headers.get(sectionKey(skillId, subsection)),
					text, notes.isEmpty() ? null : String.join("\n", notes), members, item, sprite, quest);
				if (unlock != null)
				{
					guide.computeIfAbsent(GUIDE_SKILLS[skillId], k -> new ArrayList<>()).add(unlock);
				}
			}
		}
		Map<Skill, List<String>> sections = new EnumMap<>(Skill.class);
		ordered.forEach((skill, byOrder) -> sections.put(skill, Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(byOrder.values())))));
		this.sections = sections;
		setSkillGuide(guide);
	}

	private static long sectionKey(int skillId, int subsection)
	{
		return ((long) skillId << 32) | (subsection & 0xffffffffL);
	}

	/**
	 * The unlock one skill-guide tuple stands for, or null when the guide doesn't list it at a level: overview
	 * entries have no level, and a subsection of -1 marks another skill's requirement that the guide hides.
	 */
	static Unlock guideUnlock(int skillId, int level, int subsection, String section, String text, String note, boolean members,
		Integer item, Integer sprite, Integer quest)
	{
		if (skillId <= 0 || skillId >= GUIDE_SKILLS.length || level < 1 || level > Experience.MAX_REAL_LEVEL || subsection < 0)
		{
			return null;
		}
		return new Unlock(level, members, Unlock.Type.UNLOCK, text, text, item, sprite, note, section, quest);
	}

	/** Uses the skill guide's unlocks, keeping the wiki's quest and diary requirements; skills it lacks keep the wiki's. */
	void setSkillGuide(Map<Skill, List<Unlock>> guide)
	{
		Map<Skill, List<Unlock>> merged = new EnumMap<>(wiki);
		for (Map.Entry<Skill, List<Unlock>> e : guide.entrySet())
		{
			List<Unlock> list = new ArrayList<>(e.getValue());
			for (Unlock u : wiki.getOrDefault(e.getKey(), Collections.emptyList()))
			{
				if (u.getType() != Unlock.Type.UNLOCK)
				{
					list.add(u);
				}
			}
			merged.put(e.getKey(), sorted(list));
		}
		bySkill = merged;
		skillGuide = !guide.isEmpty();
		log.debug("Loaded skill guide unlocks for {} skills", guide.size());
	}

	private static Object[] field(Client client, int row, int column, int tuple)
	{
		Object[] values = client.getDBTableField(row, column, tuple);
		return values == null ? new Object[0] : values;
	}

	private static Integer integer(Client client, int row, int column)
	{
		Object[] values = field(client, row, column, 0);
		return values.length > 0 && values[0] instanceof Number && ((Number) values[0]).intValue() >= 0 ? ((Number) values[0]).intValue() : null;
	}

	private static String string(Client client, int row, int column)
	{
		Object[] values = field(client, row, column, 0);
		return values.length > 0 && values[0] instanceof String ? (String) values[0] : "";
	}

	private static boolean bool(Object[] values)
	{
		if (values.length == 0)
		{
			return false;
		}
		return values[0] instanceof Boolean ? (Boolean) values[0] : values[0] instanceof Number && ((Number) values[0]).intValue() != 0;
	}

	/** The skill guide's section headers for a skill, in guide order; empty until the guide is loaded. */
	public List<String> sections(Skill skill)
	{
		return sections.getOrDefault(skill, Collections.emptyList());
	}

	/** Quests that unlocks require. */
	public Set<Integer> requiredQuests()
	{
		Set<Integer> quests = new HashSet<>();
		for (List<Unlock> unlocks : bySkill.values())
		{
			for (Unlock u : unlocks)
			{
				if (u.getQuest() != null)
				{
					quests.add(u.getQuest());
				}
			}
		}
		return quests;
	}

	/**
	 * Whether a boosted level is enough for an unlock. Equipment needs real levels, and so do the combat skills'
	 * guide entries, which are mostly equipment and prayers; quests, diaries and skilling methods take boosts.
	 */
	static boolean boostable(Skill skill, Unlock u)
	{
		if (u.getType() != Unlock.Type.UNLOCK)
		{
			return true;
		}
		String section = u.getSection() == null ? "" : u.getSection();
		switch (skill)
		{
			case ATTACK:
			case DEFENCE:
			case HITPOINTS:
			case PRAYER:
				return false;
			case STRENGTH:
				return section.equals("Shortcuts") || section.equals("Barbarian");
			case RANGED:
				return section.equals("Shortcuts") || section.equals("Miscellaneous") || section.equals("Boat cannons");
			case MAGIC:
				return !section.equals("Armour") && !section.equals("Weapons") && !section.equals("Equipment");
			case SLAYER:
				return !section.equals("Equipment") && !section.equals("Slayer Masters");
			default:
				return true;
		}
	}

	public List<Unlock> all(Skill skill)
	{
		return bySkill.getOrDefault(skill, Collections.emptyList());
	}

	/** Unlocks at exactly {@code level}. */
	public List<Unlock> at(Skill skill, int level, Predicate<Unlock> filter)
	{
		List<Unlock> out = new ArrayList<>();
		for (Unlock u : all(skill))
		{
			if (u.getLevel() == level && filter.test(u))
			{
				out.add(u);
			}
		}
		return out;
	}

	/** The highest level at or below {@code level} that unlocks something, or 1. */
	public int previous(Skill skill, int level, Predicate<Unlock> filter)
	{
		int previous = 1;
		for (Unlock u : all(skill))
		{
			if (u.getLevel() > level)
			{
				break;
			}
			if (filter.test(u))
			{
				previous = u.getLevel();
			}
		}
		return previous;
	}

	/** Unlocks of the first {@code maxLevels} levels above {@code level} that unlock something, grouped by level. */
	public NavigableMap<Integer, List<Unlock>> upcoming(Skill skill, int level, Predicate<Unlock> filter, int maxLevels)
	{
		NavigableMap<Integer, List<Unlock>> out = new TreeMap<>();
		for (Unlock u : all(skill))
		{
			if (u.getLevel() <= level || !filter.test(u))
			{
				continue;
			}
			if (!out.containsKey(u.getLevel()) && out.size() >= maxLevels)
			{
				break;
			}
			out.computeIfAbsent(u.getLevel(), l -> new ArrayList<>()).add(u);
		}
		return out;
	}
}
