package com.nextunlocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.PanelComponent;
import org.junit.Test;

/** A sheet of pinned cards laid out like the overlay does, for looking at; PIN_PREVIEW=out.png kit test writes it. */
public class PinPreviewTest
{
	private static final NextUnlocksConfig CONFIG = new NextUnlocksConfig()
	{
	};
	private static final Skill[] SKILLS = {Skill.FISHING, Skill.ATTACK, Skill.COOKING, Skill.MAGIC, Skill.SLAYER, Skill.MINING,
		Skill.SMITHING, Skill.PRAYER};

	@Test
	public void sheet() throws java.io.IOException
	{
		String file = System.getenv("PIN_PREVIEW");
		if (file == null)
		{
			return;
		}
		int rows = (SKILLS.length + 3) / 4;
		BufferedImage sheet = new BufferedImage(5 * 180 + 160, rows * 190 + 20, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = sheet.createGraphics();
		g.setColor(new Color(74, 98, 52));
		g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
		for (int row = 0; row < rows; row++)
		{
			List<TooltipBox> cards = new ArrayList<>();
			for (int i = row * 4; i < Math.min(SKILLS.length, row * 4 + 4); i++)
			{
				cards.add(skillCard(SKILLS[i]));
			}
			if (row == 1)
			{
				cards.add(skillCard(Skill.FISHING, theme(Skill.FISHING, CHAINED), 13));
			}
			int skillCards = cards.size();
			if (row == 0)
			{
				cards.add(itemCard());
			}
			layOut(g, cards, skillCards, 10, row * 190 + 180);
		}
		g.dispose();
		ImageIO.write(sheet, "png", new File(file));
	}

	/** Every trim on a plain card, PIN_TRIMS=out.png; PIN_TRIM_SIZE picks a size other than normal. */
	@Test
	public void trims() throws java.io.IOException
	{
		String file = System.getenv("PIN_TRIMS");
		if (file == null)
		{
			return;
		}
		trims(file, System.getenv("PIN_TRIM_SIZE"), System.getenv("PIN_ART"));
	}

	/**
	 * The README's labeled galleries, PIN_GALLERY=folder: every skill's own card, every style, every frame and every trim
	 * at normal and large size; PIN_ART is the folder of game icons the item trims use.
	 */
	@Test
	public void galleries() throws java.io.IOException
	{
		String folder = System.getenv("PIN_GALLERY");
		if (folder == null)
		{
			return;
		}
		Skill[] skills = java.util.Arrays.stream(Skill.values()).filter(s -> !s.name().equals("OVERALL")).toArray(Skill[]::new);
		gallery(new File(folder, "skill-cards.png"), 6, java.util.Arrays.stream(skills).map(Skill::getName).toArray(String[]::new),
			i -> skillCard(skills[i]));

		NextUnlocksConfig.CardStyle[] styles = NextUnlocksConfig.CardStyle.values();
		gallery(new File(folder, "card-styles.png"), 5, java.util.Arrays.stream(styles).map(Object::toString).toArray(String[]::new),
			i ->
			{
				Skill skill = SKILLS[i % SKILLS.length];
				return skillCard(skill, theme(skill, java.util.Collections.singletonMap("pinStyle", styles[i].name())), CONFIG.tooltipTextSize());
			});

		NextUnlocksConfig.FrameStyle[] frames = java.util.Arrays.stream(NextUnlocksConfig.FrameStyle.values())
			.filter(f -> f != NextUnlocksConfig.FrameStyle.STYLE).toArray(NextUnlocksConfig.FrameStyle[]::new);
		gallery(new File(folder, "card-frames.png"), 5, java.util.Arrays.stream(frames).map(Object::toString).toArray(String[]::new),
			i ->
			{
				java.util.Map<String, String> look = new java.util.HashMap<>();
				look.put("pinFrame", frames[i].name());
				look.put("pinTrim", "OFF");
				look.put("pinBorderWidth", "THICK");
				Skill skill = SKILLS[i % SKILLS.length];
				return skillCard(skill, theme(skill, look), CONFIG.tooltipTextSize());
			});

		String art = System.getenv("PIN_ART");
		trims(new File(folder, "card-trims.png").getPath(), null, art);
		trims(new File(folder, "card-trims-large.png").getPath(), "LARGE", art);
	}

	/** Cards in rows of the given width, each with its label above it, on the game's grass green. */
	private static void gallery(File file, int columns, String[] labels, java.util.function.IntFunction<TooltipBox> card)
		throws java.io.IOException
	{
		BufferedImage canvas = new BufferedImage(columns * 260 + 20, ((labels.length + columns - 1) / columns) * 320 + 20,
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas.createGraphics();
		g.setColor(new Color(74, 98, 52));
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int top = 10;
		int right = 0;
		for (int start = 0; start < labels.length; start += columns)
		{
			List<TooltipBox> cards = new ArrayList<>();
			for (int i = start; i < Math.min(labels.length, start + columns); i++)
			{
				cards.add(card.apply(i));
			}
			// room for the label and for trim pieces that stick up above a card
			int bottom = top + 34 + measure(cards, cards.size());
			layOut(g, cards, cards.size(), 10, bottom);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			for (int i = 0; i < cards.size(); i++)
			{
				Rectangle bounds = cards.get(i).getBounds();
				g.setColor(new Color(0, 0, 0, 140));
				g.drawString(labels[start + i], bounds.x + 1, top + 13);
				g.setColor(Color.WHITE);
				g.drawString(labels[start + i], bounds.x, top + 12);
				right = Math.max(right, bounds.x + bounds.width);
			}
			top = bottom + 12;
		}
		g.dispose();
		ImageIO.write(canvas.getSubimage(0, 0, Math.min(canvas.getWidth(), right + 10), Math.min(canvas.getHeight(), top)), "png", file);
	}

	/** Every trim but the defaults, labeled; size is a TrimSize name or null for normal, art the folder of game icons. */
	private static void trims(String file, String size, String art) throws java.io.IOException
	{
		NextUnlocksConfig.Trim[] trims = java.util.Arrays.stream(NextUnlocksConfig.Trim.values())
			.filter(t -> t != NextUnlocksConfig.Trim.STYLE && t != NextUnlocksConfig.Trim.AUTO && t != NextUnlocksConfig.Trim.OFF).toArray(NextUnlocksConfig.Trim[]::new);
		if (art != null)
		{
			// the game's icons for the trims that use them, saved by name in that folder
			java.util.Map<Integer, String> names = new java.util.HashMap<>();
			names.put(net.runelite.api.gameval.ItemID.ABYSSAL_WHIP, "Abyssal_whip");
			names.put(net.runelite.api.gameval.ItemID.DRAGON_CLAWS, "Dragon_claws");
			names.put(net.runelite.api.gameval.ItemID.DRAGON_SCIMITAR, "Dragon_scimitar");
			names.put(net.runelite.api.gameval.ItemID.DRAGON_LONGSWORD, "Dragon_longsword");
			CardArt.items = id ->
			{
				try
				{
					BufferedImage read = ImageIO.read(new File(art, names.get(id) + ".png"));
					BufferedImage argb = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
					argb.createGraphics().drawImage(read, 0, 0, null);
					return argb;
				}
				catch (Exception e)
				{
					return null;
				}
			};
		}
		try
		{
			gallery(new File(file), 5, java.util.Arrays.stream(trims).map(Object::toString).toArray(String[]::new), i ->
			{
				java.util.Map<String, String> look = new java.util.HashMap<>();
				look.put("pinTrim", trims[i].name());
				if (size != null)
				{
					look.put("pinTrimSize", size);
				}
				return skillCard(SKILLS[i % SKILLS.length], theme(SKILLS[i % SKILLS.length], look), CONFIG.tooltipTextSize());
			});
		}
		finally
		{
			CardArt.items = null;
		}
	}

	/** Like PinnedOverlay: side by side from the left, bottoms lined up, skill cards as tall as the tallest. */
	private static void layOut(Graphics2D g, List<TooltipBox> cards, int skillCards, int x, int bottom)
	{
		int tallest = measure(cards, skillCards);
		for (int i = 0; i < cards.size(); i++)
		{
			TooltipBox card = cards.get(i);
			int height = i < skillCards ? tallest : card.getLast().height;
			card.setPreferredLocation(new Point(x, bottom - height));
			card.setMinHeight(height);
			Dimension size = card.render(g);
			x += size.width + 4;
		}
	}

	/** Renders the cards off screen twice, the first only measuring, and gives the tallest of the first skillCards. */
	private static int measure(List<TooltipBox> cards, int skillCards)
	{
		Graphics2D scratch = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB).createGraphics();
		int tallest = 0;
		for (int i = 0; i < cards.size(); i++)
		{
			TooltipBox card = cards.get(i);
			card.setPreferredLocation(new Point(0, 0));
			card.render(scratch);
			card.render(scratch);
			if (i < skillCards)
			{
				tallest = Math.max(tallest, card.getLast().height);
			}
		}
		scratch.dispose();
		return tallest;
	}

	/** A player's own Fishing look: chains round a riveted frame. */
	private static final java.util.Map<String, String> CHAINED = new java.util.HashMap<>();

	static
	{
		String[] pairs = {"pinBorderWidth", "THICK", "pinFrame", "RIVETED", "pinTrim", "CHAINS", "pinBorderMotion", "MARCH",
			"pinCorner", "ROUNDER", "pinShimmer", "OFF", "pinHeaderBand", "ON", "pinBarGradient", "ON", "pinBackgroundGradient", "ON"};
		for (int i = 0; i < pairs.length; i += 2)
		{
			CHAINED.put(pairs[i], pairs[i + 1]);
		}
	}

	private static CardTheme theme(Skill skill)
	{
		return theme(skill, Collections.emptyMap());
	}

	private static CardTheme theme(Skill skill, java.util.Map<String, String> overrides)
	{
		CardTheme classic = CardTheme.classic(Palette.DEFAULT, new Color(70, 61, 50, 200), null);
		return CardTheme.pinned(new Look(CONFIG, overrides), Palette.DEFAULT, classic, skill);
	}

	private static BufferedImage blob(Color color)
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(color);
		g.fillOval(6, 8, 20, 16);
		g.dispose();
		return image;
	}

	/** The plugin's pinned skill card with icons only, level header, xp lines, bar, two unlocks and a goal. */
	private static TooltipBox skillCard(Skill skill)
	{
		return skillCard(skill, theme(skill), CONFIG.tooltipTextSize());
	}

	private static TooltipBox skillCard(Skill skill, CardTheme theme, int textSize)
	{
		Font font = theme.font(textSize);
		List<LayoutableRenderableEntity> lines = new ArrayList<>();
		BufferedImage skillIcon = net.runelite.client.util.ImageUtil.loadImageResource(net.runelite.client.game.SkillIconManager.class,
			"/skill_icons_small/" + skill.getName().toLowerCase() + ".png");
		lines.add(new CardLine(theme, font, true).icon(theme.isSkillIcon() ? skillIcon : null).left(skill.getName(), theme.getSkillName())
			.right("Level ", theme.getLevelLabel()).right("42", theme.getLevelValue()));
		lines.add(new CardLine(theme, font).left("XP:", theme.getXpLabel()).right("45,580", theme.getXpValue()));
		lines.add(new CardLine(theme, font).left("Next level:", theme.getNextLevelLabel()).right("50,339", theme.getNextLevelValue()));
		lines.add(new CardLine(theme, font).left("Remaining XP:", theme.getRemainingLabel()).right("4,759", theme.getRemainingValue()));
		lines.add(new PinBar(theme, 41_171, 50_339, 45_580, theme.getBarFill(), theme.getBarFill2(), theme.getBarBackground(), null));

		PanelComponent icons = new PanelComponent();
		icons.setOrientation(ComponentOrientation.HORIZONTAL);
		icons.setWrap(true);
		icons.setBackgroundColor(null);
		icons.setGap(new Point(2, 2));
		for (Color c : new Color[]{new Color(70, 90, 100), new Color(110, 70, 40)})
		{
			icons.getChildren().add(NextUnlocksPlugin.slot(new UnlockTileComponent(blob(c), "Unlock", 43, Palette.DEFAULT.getLevel(), false, 1), theme));
		}
		lines.add(icons);

		Color skillColor = Skills.color(skill);
		lines.add(new CardLine(theme, font).left("Goal: ", theme.getGoalLabel() == null ? skillColor : theme.getGoalLabel())
			.left("52", theme.getGoalLevel() == null ? skillColor : theme.getGoalLevel()).right("78,080 xp", theme.getGoalXp()));
		lines.add(new PinBar(theme, 0, 123_660, 45_580, theme.getGoalBar() == null ? skillColor : theme.getGoalBar(), theme.getGoalBar2(),
			theme.getGoalBarBackground() == null ? theme.getBarBackground() : theme.getGoalBarBackground(), null));

		PanelComponent card = new PanelComponent();
		card.getChildren().addAll(lines);
		int width = (int) (ComponentConstants.STANDARD_WIDTH * theme.widthScale(textSize)) + 2 * (theme.getPadding() - NextUnlocksPlugin.PANEL_PADDING);
		for (LayoutableRenderableEntity line : lines)
		{
			if (line instanceof CardLine)
			{
				width = Math.max(width, ((CardLine) line).minWidth() + 2 * theme.getPadding());
			}
		}
		card.setPreferredSize(new Dimension(width, 0));
		int inside = width - 2 * theme.getPadding();
		int tile = UnlockTileComponent.width(1, true);
		icons.setBorder(new Rectangle(Math.max(0, (inside - 2 * (tile + 2) + 2) / 2), 3, 0, 0));
		return new TooltipBox(card, theme, new ArrayList<>());
	}

	/** The plugin's item goal card. */
	private static TooltipBox itemCard()
	{
		CardTheme theme = theme(null);
		int textSize = CONFIG.tooltipTextSize();
		Font font = theme.font(textSize);
		Font small = theme.font(Math.max(NextUnlocksPlugin.MATERIAL_MIN_SIZE, Math.round(textSize * NextUnlocksPlugin.MATERIAL_SIZE)));
		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		lines.add(new CardHeader(blob(new Color(120, 90, 50)), "Camphor blowpipe", theme.getTaskTitle(),
			theme.titleFont(textSize, NextUnlocksPlugin.TASK_TITLE_GROWTH), (double) textSize / FontManager.getRunescapeFont().getSize(), theme));
		lines.add(new CardLine(theme, font).left("Item goal", theme.getXpValue()).right("3/4 met", theme.getXpValue()));
		lines.add(new CardLine(theme, font).left("Fishing", theme.getUnmet()).right("42/52", theme.getUnmet()));
		lines.add(new CardMaterial(theme, small, blob(new Color(90, 90, 110)), "Raw swordtip squid", theme.getUnmet().darker()));
		lines.add(new CardLine(theme, font).left("Ranged", theme.getMet()).right("45/45", theme.getMet()));
		lines.add(new CardLine(theme, font).left("Woodcutting", theme.getMet()).right("66/66", theme.getMet()));
		lines.add(new CardMaterial(theme, small, blob(new Color(120, 80, 40)), "Camphor logs", theme.getMet().darker()));
		lines.add(new CardLine(theme, font).left("Fletching", theme.getMet()).right("58/58", theme.getMet()));
		card.setPreferredSize(new Dimension((int) (ComponentConstants.STANDARD_WIDTH * theme.widthScale(textSize))
			+ 2 * (theme.getPadding() - NextUnlocksPlugin.PANEL_PADDING), 0));
		return new TooltipBox(card, theme, Collections.emptyList());
	}
}
