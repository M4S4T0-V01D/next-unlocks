package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import org.junit.Test;

public class LooksTest
{
	private static final NextUnlocksConfig CONFIG = new NextUnlocksConfig()
	{
	};

	private static CardTheme pinned(Skill skill, String... pairs)
	{
		Map<String, String> overrides = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			overrides.put(pairs[i], pairs[i + 1]);
		}
		CardTheme classic = CardTheme.classic(Palette.DEFAULT, new Color(70, 61, 50, 200), null);
		return CardTheme.pinned(new Look(CONFIG, overrides), Palette.DEFAULT, classic, skill);
	}

	@Test
	public void everyLookKeyIsAConfigKey()
	{
		for (String key : Look.KEYS.keySet())
		{
			assertNotNull(key, Look.KEYS.get(key).apply(CONFIG));
		}
	}

	@Test
	public void valuesGoBackAndForth()
	{
		Map<String, String> values = new Look(CONFIG, null).values();
		Look again = new Look(CONFIG, values);
		for (String key : Look.KEYS.keySet())
		{
			assertEquals(key, Look.format(Look.KEYS.get(key).apply(CONFIG)), Look.format(again.get(key)));
		}
		assertEquals(values, Look.decode(Look.encode(values)));
	}

	@Test
	public void overridesWinAndBadOnesAreIgnored()
	{
		Look look = new Look(CONFIG, Collections.singletonMap("pinStyle", "NEON"));
		assertEquals(NextUnlocksConfig.CardStyle.NEON, look.get("pinStyle"));
		assertTrue(look.changes("pinStyle"));
		assertFalse(look.changes("pinFont"));
		Look bad = new Look(CONFIG, Collections.singletonMap("pinStyle", "NOT_A_STYLE"));
		assertEquals(CONFIG.pinStyle(), bad.get("pinStyle"));
		Color red = new Look(CONFIG, Collections.singletonMap("pinGlowColor", String.valueOf(new Color(255, 0, 0, 128).getRGB())))
			.get("pinGlowColor");
		assertEquals(128, red.getAlpha());
	}

	@Test
	public void listsKeepOrderAndDropSeparators()
	{
		Map<String, Map<String, String>> list = new LinkedHashMap<>();
		list.put("Zeta", Collections.singletonMap("pinStyle", "NEON"));
		list.put("Al|pha;=", Collections.singletonMap("pinFont", "SANS"));
		list.put("Unknown keys", Collections.singletonMap("notALookKey", "1"));
		Map<String, Map<String, String>> read = Look.readList(Look.writeList(list));
		assertEquals(java.util.Arrays.asList("Zeta", "Alpha", "Unknown keys"), new java.util.ArrayList<>(read.keySet()));
		assertEquals("SANS", read.get("Alpha").get("pinFont"));
		assertTrue(read.get("Unknown keys").isEmpty());
		assertTrue(Look.readList(null).isEmpty());
		assertTrue(Look.readList("").isEmpty());
	}

	@Test
	public void everySkillHasItsOwnLook()
	{
		CardTheme classic = CardTheme.classic(Palette.DEFAULT, Color.DARK_GRAY, null);
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			CardTheme theme = SkillLooks.theme(skill, Palette.DEFAULT, classic);
			assertNotNull(skill.getName(), theme);
			assertNotNull(skill.getName(), theme.getBarFill());
			NextUnlocksConfig.XpEffect effect = SkillLooks.effect(skill);
			assertNotNull(effect);
			assertTrue(skill.getName(), effect.ordinal() > NextUnlocksConfig.XpEffect.OFF.ordinal());
		}
	}

	@Test
	public void magicIsTheRoseWindow()
	{
		CardTheme magic = pinned(Skill.MAGIC);
		assertEquals(CardTheme.Frame.BEVEL, magic.getFrame());
		assertEquals(new Color(0xb88c9e).getRGB(), magic.getBackground().getRGB() | 0xff000000);
		assertEquals(NextUnlocksConfig.XpEffect.STARS, magic.getXpEffect());
		assertTrue(magic.isXpPopup());
		assertTrue(magic.isXpPulse());
	}

	@Test
	public void switchesChangeTheCard()
	{
		assertNull(pinned(Skill.ATTACK, "pinBorderWidth", "NONE").getBorder());
		assertEquals(3, pinned(Skill.ATTACK, "pinBorderWidth", "HEAVY").getBorderWidth());
		assertEquals(CardTheme.Frame.ROUNDED, pinned(Skill.ATTACK, "pinStyle", "CLASSIC", "pinCorner", "ROUND").getFrame());
		assertEquals(6, pinned(Skill.ATTACK, "pinCorner", "ROUND").getCorner());
		assertEquals(8, pinned(Skill.ATTACK, "pinPadding", "ROOMY").getPadding());
		assertNull(pinned(Skill.ATTACK, "pinXpEffect", "OFF").getXpEffect());
		assertEquals(NextUnlocksConfig.XpEffect.RUNES, pinned(Skill.ATTACK, "pinXpEffect", "RUNES").getXpEffect());
		// a preset's base skill sets the style's look, but the skill's own effect is the card's
		assertEquals(NextUnlocksConfig.XpEffect.BUBBLES, pinned(Skill.ATTACK, "pinBase", "FISHING").getXpEffect());
		assertEquals(NextUnlocksConfig.XpEffect.SPARKS,
			pinned(Skill.ATTACK, "pinBase", "FISHING", "pinXpEffect", "AUTO").getXpEffect());
		assertEquals(NextUnlocksConfig.XpEffect.LEAVES,
			pinned(Skill.WOODCUTTING, "pinStyle", "CLASSIC", "pinXpEffect", "AUTO").getXpEffect());
		assertNull(pinned(null, "pinStyle", "CLASSIC", "pinXpEffect", "AUTO").getXpEffect());
		assertNotNull(pinned(Skill.ATTACK, "pinStyle", "CLASSIC", "pinBackgroundGradient", "ON").getBackground2());
		assertNull(pinned(Skill.ATTACK, "pinStyle", "GILDED", "pinBarGradient", "OFF").getBarFill2());
		assertTrue(pinned(Skill.ATTACK, "pinStyle", "CLASSIC", "pinShimmer", "ON").isShimmer());
		assertNotNull(pinned(Skill.ATTACK, "pinStyle", "CLASSIC", "pinHeaderBand", "ON").getHeaderBand());
		assertNull(pinned(Skill.MAGIC, "pinHeaderBand", "OFF").getHeaderBand());
	}

	@Test
	public void everyStyleBuilds()
	{
		for (NextUnlocksConfig.CardStyle style : NextUnlocksConfig.CardStyle.values())
		{
			for (Skill skill : new Skill[]{null, Skill.MAGIC, Skill.SAILING})
			{
				assertNotNull(style.name(), pinned(skill, "pinStyle", style.name()));
			}
		}
	}

	@Test
	public void framesMatchTheMenu()
	{
		for (NextUnlocksConfig.FrameStyle frame : NextUnlocksConfig.FrameStyle.values())
		{
			if (frame != NextUnlocksConfig.FrameStyle.STYLE)
			{
				assertEquals(frame.name(), CardTheme.Frame.valueOf(frame.name()),
					pinned(Skill.ATTACK, "pinStyle", "CLASSIC", "pinFrame", frame.name()).getFrame());
			}
		}
		assertEquals(CardTheme.Frame.NOTCHED, pinned(Skill.ATTACK).getFrame());
		assertEquals(NextUnlocksConfig.Ambient.LEAVES, pinned(Skill.WOODCUTTING, "pinStyle", "CLASSIC", "pinAmbient", "AUTO").getAmbient());
		assertNull(pinned(Skill.WOODCUTTING, "pinAmbient", "OFF").getAmbient());
		assertNull(pinned(null, "pinStyle", "CLASSIC", "pinAmbient", "AUTO").getAmbient());
		assertEquals(NextUnlocksConfig.BorderMotion.RAINBOW, pinned(Skill.ATTACK, "pinBorderMotion", "RAINBOW").getBorderMotion());
		assertNull(pinned(Skill.ATTACK, "pinBorderMotion", "OFF").getBorderMotion());
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			assertNotNull(skill.getName(), SkillLooks.ambient(skill));
		}
	}

	@Test
	public void everyFrameMotionAndAmbientDraws()
	{
		NextUnlocksConfig.FrameStyle[] frames = NextUnlocksConfig.FrameStyle.values();
		NextUnlocksConfig.BorderMotion[] motions = NextUnlocksConfig.BorderMotion.values();
		NextUnlocksConfig.Ambient[] ambients = NextUnlocksConfig.Ambient.values();
		int runs = Math.max(frames.length, Math.max(motions.length, ambients.length));
		for (int i = 0; i < runs; i++)
		{
			for (String width : new String[]{"NONE", "HEAVY"})
			{
				CardTheme theme = pinned(Skill.ATTACK, "pinFrame", frames[i % frames.length].name(),
					"pinBorderMotion", motions[i % motions.length].name(), "pinAmbient", ambients[i % ambients.length].name(),
					"pinBorderWidth", width, "pinScanlines", "ON");
				PanelComponent panel = new PanelComponent();
				panel.getChildren().add(LineComponent.builder().left("Attack").right("50").build());
				TooltipBox box = new TooltipBox(panel, theme, new java.util.ArrayList<>());
				BufferedImage image = new BufferedImage(260, 120, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = image.createGraphics();
				box.setPreferredLocation(new Point(10, 10));
				box.render(g);
				box.render(g);
				g.dispose();
			}
		}
	}

	@Test
	public void everyTrimDraws() throws java.io.IOException
	{
		assertNull(pinned(Skill.ATTACK, "pinTrim", "OFF").getTrim());
		assertEquals(NextUnlocksConfig.Trim.ROSES, pinned(Skill.ATTACK, "pinTrim", "ROSES").getTrim());
		assertTrue(pinned(Skill.ATTACK, "pinTrim", "ROSES", "pinTrimTint", "ON").isTrimTint());
		assertEquals(1.35, pinned(Skill.ATTACK, "pinTrimSize", "LARGE").getTrimScale(), 0.001);

		// the game's art for the preview, exported from a cache: sprites as id-0.png, icons by item id
		String sprites = System.getenv("TRIM_SPRITES");
		String icons = System.getenv("TRIM_ICONS");
		if (sprites != null)
		{
			CardArt.minimap(javax.imageio.ImageIO.read(new java.io.File(sprites, "1037-0.png")),
				javax.imageio.ImageIO.read(new java.io.File(sprites, "1182-0.png")),
				javax.imageio.ImageIO.read(new java.io.File(sprites, "1038-0.png")));
		}
		if (icons != null)
		{
			CardArt.items = id ->
			{
				try
				{
					return javax.imageio.ImageIO.read(new java.io.File(icons, id + ".png"));
				}
				catch (java.io.IOException e)
				{
					return null;
				}
			};
		}
		try
		{
			trimSheet(System.getenv("TRIM_PREVIEW"));
		}
		finally
		{
			CardArt.clear();
		}
	}

	@Test
	public void trimsUseTheGamesArt() throws java.io.IOException
	{
		// a stand-in minimap: a stone square with a round map hole, on a panel that should be cut away
		BufferedImage cover = new BufferedImage(120, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = cover.createGraphics();
		g.setColor(new Color(110, 100, 80));
		g.fillRect(0, 0, 120, 100);
		g.setComposite(java.awt.AlphaComposite.Clear);
		g.fillOval(30, 20, 60, 60);
		g.dispose();
		BufferedImage side = new BufferedImage(20, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D s = side.createGraphics();
		s.setColor(new Color(60, 56, 37));
		s.fillRect(0, 0, 20, 100);
		s.dispose();
		BufferedImage icon = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D i = icon.createGraphics();
		i.setColor(Color.RED);
		i.fillRect(10, 8, 12, 14);
		i.dispose();
		try
		{
			assertNull(CardArt.bounds(new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB)));
			assertEquals(new java.awt.Rectangle(10, 8, 12, 14), CardArt.bounds(icon));
			CardArt.items = id -> icon;
			CardArt.minimap(side, cover, side);
			BufferedImage dragons = CardArt.dragons();
			assertNotNull(dragons);
			// only the ring around the hole is kept, not the panel out at the sides
			assertTrue(dragons.getWidth() < 140 && dragons.getWidth() > 100);
			assertNotNull(CardArt.dragons(50, 40, Color.CYAN));
			trimSheet(null);
		}
		finally
		{
			CardArt.clear();
		}
		assertNull(CardArt.dragons());
	}

	private void trimSheet(String file) throws java.io.IOException
	{
		NextUnlocksConfig.Trim[] trims = NextUnlocksConfig.Trim.values();
		String[] frames = {"ORNATE", "TICKET", "SCALLOPED", "ROUNDED", "DOUBLE"};
		int cols = 3;
		BufferedImage sheet = new BufferedImage(cols * 250, (trims.length + cols - 1) / cols * 110, BufferedImage.TYPE_INT_RGB);
		Graphics2D out = sheet.createGraphics();
		out.setColor(new Color(40, 40, 40));
		out.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
		for (int i = 0; i < trims.length; i++)
		{
			for (String size : new String[]{"SMALL", "LARGE", "NORMAL"})
			{
				for (String tint : new String[]{"ON", "OFF"})
				{
					CardTheme theme = pinned(Skill.ATTACK, "pinFrame", frames[i % frames.length],
						"pinTrim", trims[i].name(), "pinTrimSize", size, "pinTrimTint", tint, "pinBorderWidth", "THICK");
					PanelComponent panel = new PanelComponent();
					panel.getChildren().add(LineComponent.builder().left(trims[i].toString()).right(frames[i % frames.length]).build());
					panel.getChildren().add(LineComponent.builder().left("Next unlock").right("Level 50").build());
					panel.getChildren().add(LineComponent.builder().left("Rune scimitar").right("12 levels").build());
					TooltipBox box = new TooltipBox(panel, theme, new java.util.ArrayList<>());
					box.setPreferredSize(new Dimension(220, 0));
					box.setPreferredLocation(new Point(i % cols * 250 + 15, i / cols * 110 + 15));
					// the last pass, normal size in the trim's own colors, is the one left on the sheet
					// the first render lays the card out, off the sheet
					Graphics2D scratch = new BufferedImage(sheet.getWidth(), sheet.getHeight(), BufferedImage.TYPE_INT_ARGB).createGraphics();
					box.render(scratch);
					scratch.dispose();
					Graphics2D g = (Graphics2D) out.create();
					box.render(g);
					g.dispose();
				}
			}
		}
		out.dispose();
		if (file != null)
		{
			javax.imageio.ImageIO.write(sheet, "png", new java.io.File(file));
		}
	}

	@Test
	public void titleLineHasRoomForBothSides()
	{
		CardTheme theme = pinned(Skill.HITPOINTS);
		java.awt.Font font = theme.font(16);
		BufferedImage icon = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		CardLine longName = new CardLine(theme, font, true).icon(icon).left("Hitpoints", Color.RED)
			.right("Level ", Color.WHITE).right("45/48", Color.RED);
		CardLine shortName = new CardLine(theme, font, true).left("Magic", Color.RED).right("Level ", Color.WHITE).right("45", Color.RED);
		assertTrue(longName.minWidth() > shortName.minWidth());
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setFont(font);
		java.awt.FontMetrics metrics = g.getFontMetrics();
		assertTrue(longName.minWidth() > metrics.stringWidth("HitpointsLevel 45/48") + 4);
		g.dispose();
		assertEquals(0, new CardLine(theme, font).left("A long line that can wrap", Color.WHITE).minWidth());
	}

	@Test
	public void closeGainsAddUp()
	{
		XpEffects.Gain gain = new XpEffects.Gain(1000, 50);
		XpEffects.Gain more = gain.plus(25, 1000 + XpEffects.MERGE_MS - 1);
		assertEquals(75, more.getAmount());
		assertEquals(1000 + XpEffects.MERGE_MS - 1, more.getAt());
		assertEquals(10, more.plus(10, more.getAt() + XpEffects.MERGE_MS).getAmount());
		assertTrue(XpEffects.count(NextUnlocksConfig.XpEffect.SPARKS, 1_000_000) > XpEffects.count(NextUnlocksConfig.XpEffect.SPARKS, 1));
	}

	@Test
	public void everyEffectDraws()
	{
		for (NextUnlocksConfig.XpEffect effect : NextUnlocksConfig.XpEffect.values())
		{
			CardTheme theme = pinned(Skill.ATTACK, "pinXpEffect", effect.name(), "pinXpPopup", "ON", "pinXpPulse", "ON");
			for (long since : new long[]{0, 100, 400, 900, XpEffects.MS - 1, XpEffects.MS + 10})
			{
				BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = image.createGraphics();
				PinBar bar = new PinBar(theme, 0, 100, 60, Color.GREEN, null);
				bar.setPreferredLocation(new Point(20, 60));
				bar.setPreferredSize(new Dimension(150, 0));
				bar.render(g);
				XpEffects.render(g, theme, bar, new XpEffects.Gain(System.currentTimeMillis() - since, 120));
				XpEffects.render(g, theme, bar, null);
				g.dispose();
			}
		}
	}

	@Test
	public void styleColorsComeBackAsOwnColors()
	{
		for (String style : new String[]{"SKILL", "NEON", "PARCHMENT", "CLASSIC"})
		{
			CardTheme styled = pinned(Skill.MAGIC, "pinStyle", style);
			java.util.List<String> pairs = new java.util.ArrayList<>(java.util.Arrays.asList("pinStyle", style, "pinCustomColors", "true"));
			CardTheme.ownColors(styled).forEach((key, color) ->
			{
				assertTrue(key, Look.KEYS.containsKey(key));
				pairs.add(key);
				pairs.add(Look.format(color));
			});
			CardTheme own = pinned(Skill.MAGIC, pairs.toArray(new String[0]));
			assertEquals(style, styled.getBackground(), own.getBackground());
			assertEquals(style, styled.getBorder(), own.getBorder());
			assertEquals(style, styled.getSkillName(), own.getSkillName());
			assertEquals(style, styled.getLevelValue(), own.getLevelValue());
			assertEquals(style, styled.getUnlock(), own.getUnlock());
			assertEquals(style, styled.getBarFill(), own.getBarFill());
			assertEquals(style, styled.getBarText(), own.getBarText());
		}
	}

	@Test
	public void menuColourPartsAreEveryColorSetting()
	{
		java.util.Set<String> parts = new java.util.HashSet<>();
		for (String[] part : NextUnlocksMenu.COLOUR_PARTS)
		{
			assertTrue(part[0], Look.KEYS.get(part[0]).apply(CONFIG) instanceof Color);
			parts.add(part[0]);
		}
		for (String key : Look.KEYS.keySet())
		{
			assertEquals(key, key.startsWith("pinColor"), parts.contains(key));
		}
	}

	@Test
	public void aCardsOwnColorsGoIntoItsPreset()
	{
		Color mine = new Color(12, 34, 56, 78);
		Map<String, String> own = new HashMap<>();
		own.put("pinCustomColors", "true");
		own.put("pinColorSkillName", Look.format(mine));
		Map<String, String> preset = Look.decode(Look.encode(new Look(CONFIG, own).values()));
		assertEquals("true", preset.get("pinCustomColors"));
		assertEquals(mine, new Look(CONFIG, preset).get("pinColorSkillName"));
		assertEquals(mine, pinned(Skill.MAGIC, "pinCustomColors", "true", "pinColorSkillName", Look.format(mine)).getSkillName());
	}

	@Test
	public void everySkillHasItsOwnTrim()
	{
		for (Skill skill : Skill.values())
		{
			NextUnlocksConfig.Trim trim = SkillLooks.trim(skill);
			assertNotNull(skill.name(), trim);
			assertTrue(skill.name(), trim != NextUnlocksConfig.Trim.AUTO && trim != NextUnlocksConfig.Trim.STYLE
				&& trim != NextUnlocksConfig.Trim.OFF);
		}
		assertEquals(NextUnlocksConfig.Trim.NET, SkillLooks.trim(Skill.FISHING));
		assertEquals(NextUnlocksConfig.Trim.RUNES, SkillLooks.trim(Skill.RUNECRAFT));
	}
}
