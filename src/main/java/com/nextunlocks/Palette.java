package com.nextunlocks;

import java.awt.Color;
import lombok.Value;

/** The configurable colors, shared by the tooltip, the panel and the skill guide. */
@Value
class Palette
{
	static final Palette DEFAULT = new Palette(new Color(0xff9040), new Color(0xff9040), new Color(0xc86a28),
		new Color(0x4d4d4d), Color.YELLOW, new Color(0x4cd94c), new Color(0x8a8a8a));

	/** The next unlock: the tooltip's level line, the skill guide's next level. */
	Color next;
	/** The skill guide's next-unlock rows, and the tooltip when outlined. */
	Color outline;
	Color progress;
	Color progressBackground;
	/** Level numbers on unlock icons. */
	Color level;
	/** Unlocks reached with a boost. */
	Color boost;
	/** Quest-locked unlocks. */
	Color locked;

	static Palette of(NextUnlocksConfig config)
	{
		return new Palette(config.colorNext(), config.colorOutline(), config.colorProgress(), config.colorProgressBackground(),
			config.colorLevel(), config.colorBoost(), config.colorLocked());
	}

	/** The color as a widget's RGB, ignoring alpha. */
	static int rgb(Color color)
	{
		return color.getRGB() & 0xffffff;
	}

	/** The color's alpha as a widget's opacity, where 0 is opaque. */
	static int opacity(Color color)
	{
		return 255 - color.getAlpha();
	}
}
