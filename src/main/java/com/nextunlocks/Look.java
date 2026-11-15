package com.nextunlocks;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The pinned card settings as one card sees them: the config, with a skill's own changes or a preset's values on top.
 * Values are kept as the strings RuneLite stores them as, so they can go into presets and the skill looks list as is.
 */
final class Look
{
	/** Every setting a look is made of, in menu order, read from the config. */
	static final Map<String, Function<NextUnlocksConfig, Object>> KEYS;

	static
	{
		Map<String, Function<NextUnlocksConfig, Object>> k = new LinkedHashMap<>();
		k.put("pinStyle", NextUnlocksConfig::pinStyle);
		k.put("pinBase", NextUnlocksConfig::pinBase);
		k.put("pinFont", NextUnlocksConfig::pinFont);
		k.put("pinBar", NextUnlocksConfig::pinBar);
		k.put("pinLevelUp", NextUnlocksConfig::pinLevelUp);
		k.put("pinGlow", NextUnlocksConfig::pinGlow);
		k.put("pinGlowColor", NextUnlocksConfig::pinGlowColor);
		k.put("pinGlowSize", NextUnlocksConfig::pinGlowSize);
		k.put("pinTextShadow", NextUnlocksConfig::pinTextShadow);
		k.put("pinScanlines", NextUnlocksConfig::pinScanlines);
		k.put("pinSkillIcon", NextUnlocksConfig::pinSkillIcon);
		k.put("pinXpEffect", NextUnlocksConfig::pinXpEffect);
		k.put("pinXpPopup", NextUnlocksConfig::pinXpPopup);
		k.put("pinXpPulse", NextUnlocksConfig::pinXpPulse);
		k.put("pinBackgroundGradient", NextUnlocksConfig::pinBackgroundGradient);
		k.put("pinBarGradient", NextUnlocksConfig::pinBarGradient);
		k.put("pinShimmer", NextUnlocksConfig::pinShimmer);
		k.put("pinHeaderBand", NextUnlocksConfig::pinHeaderBand);
		k.put("pinBorderWidth", NextUnlocksConfig::pinBorderWidth);
		k.put("pinCorner", NextUnlocksConfig::pinCorner);
		k.put("pinPadding", NextUnlocksConfig::pinPadding);
		k.put("tooltipOpacity", NextUnlocksConfig::tooltipOpacity);
		k.put("pinCustomColors", NextUnlocksConfig::pinCustomColors);
		k.put("pinColorBackground", NextUnlocksConfig::pinColorBackground);
		k.put("pinColorBackground2", NextUnlocksConfig::pinColorBackground2);
		k.put("pinColorBorder", NextUnlocksConfig::pinColorBorder);
		k.put("pinColorHeaderBand", NextUnlocksConfig::pinColorHeaderBand);
		k.put("pinColorSkillName", NextUnlocksConfig::pinColorSkillName);
		k.put("pinColorLevelLabel", NextUnlocksConfig::pinColorLevelLabel);
		k.put("pinColorLevelValue", NextUnlocksConfig::pinColorLevelValue);
		k.put("pinColorXpLabel", NextUnlocksConfig::pinColorXpLabel);
		k.put("pinColorXpValue", NextUnlocksConfig::pinColorXpValue);
		k.put("pinColorNextLevelLabel", NextUnlocksConfig::pinColorNextLevelLabel);
		k.put("pinColorNextLevelValue", NextUnlocksConfig::pinColorNextLevelValue);
		k.put("pinColorRemainingLabel", NextUnlocksConfig::pinColorRemainingLabel);
		k.put("pinColorRemainingValue", NextUnlocksConfig::pinColorRemainingValue);
		k.put("pinColorNextUnlockLabel", NextUnlocksConfig::pinColorNextUnlockLabel);
		k.put("pinColorNextUnlockLevel", NextUnlocksConfig::pinColorNextUnlockLevel);
		k.put("pinColorNextUnlockXp", NextUnlocksConfig::pinColorNextUnlockXp);
		k.put("pinColorUnlock", NextUnlocksConfig::pinColorUnlock);
		k.put("pinColorMore", NextUnlocksConfig::pinColorMore);
		k.put("pinColorGoalLabel", NextUnlocksConfig::pinColorGoalLabel);
		k.put("pinColorGoalLevel", NextUnlocksConfig::pinColorGoalLevel);
		k.put("pinColorGoalXp", NextUnlocksConfig::pinColorGoalXp);
		k.put("pinColorGoalBar", NextUnlocksConfig::pinColorGoalBar);
		k.put("pinColorGoalBar2", NextUnlocksConfig::pinColorGoalBar2);
		k.put("pinColorTaskTitle", NextUnlocksConfig::pinColorTaskTitle);
		k.put("pinColorMet", NextUnlocksConfig::pinColorMet);
		k.put("pinColorUnmet", NextUnlocksConfig::pinColorUnmet);
		k.put("pinColorBarFill", NextUnlocksConfig::pinColorBarFill);
		k.put("pinColorBarFill2", NextUnlocksConfig::pinColorBarFill2);
		k.put("pinColorBarBackground", NextUnlocksConfig::pinColorBarBackground);
		k.put("pinColorBarText", NextUnlocksConfig::pinColorBarText);
		KEYS = Collections.unmodifiableMap(k);
	}

	private final NextUnlocksConfig config;
	private final Map<String, String> overrides;

	Look(NextUnlocksConfig config, Map<String, String> overrides)
	{
		this.config = config;
		this.overrides = overrides == null ? Collections.emptyMap() : overrides;
	}

	/** A setting's value: the override when there is a good one, else the config's. */
	@SuppressWarnings("unchecked")
	<T> T get(String key)
	{
		Object fallback = KEYS.get(key).apply(config);
		String value = overrides.get(key);
		return (T) (value == null ? fallback : parse(value, fallback));
	}

	/** Whether this look changes the setting from the config's. */
	boolean changes(String key)
	{
		return overrides.containsKey(key);
	}

	/** Every setting as a string, for saving as a preset. */
	Map<String, String> values()
	{
		Map<String, String> values = new LinkedHashMap<>();
		for (String key : KEYS.keySet())
		{
			values.put(key, format(get(key)));
		}
		return values;
	}

	/** A value as RuneLite stores it: enums by name, colors as their ARGB number. */
	static String format(Object value)
	{
		if (value instanceof Enum)
		{
			return ((Enum<?>) value).name();
		}
		if (value instanceof Color)
		{
			return String.valueOf(((Color) value).getRGB());
		}
		return String.valueOf(value);
	}

	/** A stored value read as the type of the one it stands in for; that one when it can't be read. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static Object parse(String value, Object like)
	{
		try
		{
			if (like instanceof Enum)
			{
				return Enum.valueOf((Class) ((Enum<?>) like).getDeclaringClass(), value);
			}
			if (like instanceof Color)
			{
				return new Color(Integer.parseInt(value), true);
			}
			if (like instanceof Integer)
			{
				return Integer.parseInt(value);
			}
			if (like instanceof Boolean)
			{
				return Boolean.parseBoolean(value);
			}
			return value;
		}
		catch (IllegalArgumentException e)
		{
			return like;
		}
	}

	/** key=value;key=value, for one line of a list; ; and = are left out of values, which never need them. */
	static String encode(Map<String, String> values)
	{
		StringBuilder line = new StringBuilder();
		for (Map.Entry<String, String> e : values.entrySet())
		{
			if (line.length() > 0)
			{
				line.append(';');
			}
			line.append(e.getKey()).append('=').append(clean(e.getValue()));
		}
		return line.toString();
	}

	/** Read key=value;key=value back, keeping only settings a look has. */
	static Map<String, String> decode(String line)
	{
		Map<String, String> values = new LinkedHashMap<>();
		for (String pair : line.split(";"))
		{
			int eq = pair.indexOf('=');
			if (eq > 0 && KEYS.containsKey(pair.substring(0, eq).trim()))
			{
				values.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
			}
		}
		return values;
	}

	/** A name|key=value;... list, one per line, like the skill looks and the presets, in order. */
	static Map<String, Map<String, String>> readList(String text)
	{
		Map<String, Map<String, String>> list = new LinkedHashMap<>();
		if (text == null)
		{
			return list;
		}
		for (String line : text.split("\n"))
		{
			int bar = line.indexOf('|');
			if (bar > 0)
			{
				list.put(line.substring(0, bar).trim(), decode(line.substring(bar + 1)));
			}
		}
		return list;
	}

	static String writeList(Map<String, Map<String, String>> list)
	{
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, Map<String, String>> e : list.entrySet())
		{
			if (text.length() > 0)
			{
				text.append('\n');
			}
			text.append(clean(e.getKey())).append('|').append(encode(e.getValue()));
		}
		return text.toString();
	}

	/** A name or value fit for the lists: no separators or line breaks. */
	static String clean(String text)
	{
		return text == null ? "" : text.replaceAll("[|;=\\r\\n]", "").trim();
	}
}
