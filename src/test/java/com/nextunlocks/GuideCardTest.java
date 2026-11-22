package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class GuideCardTest
{
	private final TaskRepository tasks = new TaskRepository();
	private final GuideRepository guides = new GuideRepository();

	/** A level 3 account with a few quests done and Defence past the 1 Defence cap. */
	private static final GuideProgress.Account ACCOUNT = new GuideProgress.Account()
	{
		@Override
		public int level(Skill skill)
		{
			return skill == Skill.HITPOINTS ? 10 : skill == Skill.DEFENCE ? 2 : skill == Skill.ATTACK ? 12 : 1;
		}

		@Override
		public int xp(Skill skill)
		{
			int level = level(skill);
			return level == 1 ? 0 : Experience.getXpForLevel(level);
		}

		@Override
		public boolean questDone(String wiki)
		{
			return "Cook's Assistant".equals(wiki) || "Sheep Shearer".equals(wiki);
		}

		@Override
		public boolean diaryDone(String title)
		{
			return false;
		}

		@Override
		public boolean checked(String key)
		{
			return false;
		}
	};

	@Before
	public void load() throws Exception
	{
		UnlockRepository unlocks = new UnlockRepository();
		unlocks.load(new Gson());
		tasks.load(new Gson(), unlocks.diaries());
		guides.load(new Gson(), tasks, unlocks);
	}

	private static Dimension draw(TooltipBox box, Graphics2D g, int x, int y)
	{
		box.setPreferredLocation(new Point(x, y));
		// the first frame measures
		box.render(g);
		return box.render(g);
	}

	@Test
	public void everyStyleDraws() throws Exception
	{
		String[] ids = {"f2p-start", "iron-start", "important-quests", "pure-1def", "vale-totems"};
		String icons = System.getenv("GUIDE_ICONS");
		String[] files = {"Bronze_longsword.png", "Ironman_helm.png", "Quest_point_icon.png", "Skull_status_icon.png", "Stats_icon.png"};
		List<TooltipBox> cards = new ArrayList<>();
		for (int i = 0; i < ids.length; i++)
		{
			Guide guide = guides.get(ids[i]);
			BufferedImage icon = icons == null ? null : ImageIO.read(new File(icons, files[i]));
			cards.add(GuideLooks.card(GuideProgress.of(guide, ACCOUNT, tasks), GuideLooks.theme(guide.getStyle(), Palette.DEFAULT),
				icon, 16, ACCOUNT::level, 0));
		}
		// measure first, then lay the cards out three to a row
		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D measure = scratch.createGraphics();
		int[] widths = new int[3];
		int[] heights = new int[2];
		for (int i = 0; i < cards.size(); i++)
		{
			Dimension size = draw(cards.get(i), measure, 0, 0);
			assertTrue(ids[i], size.width > 100 && size.height > 60);
			widths[i % 3] = Math.max(widths[i % 3], size.width);
			heights[i / 3] = Math.max(heights[i / 3], size.height);
		}
		measure.dispose();
		int gap = 12;
		BufferedImage image = new BufferedImage(widths[0] + widths[1] + widths[2] + 4 * gap, heights[0] + heights[1] + 3 * gap,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		for (int i = 0; i < cards.size(); i++)
		{
			int x = gap;
			for (int c = 0; c < i % 3; c++)
			{
				x += widths[c] + gap;
			}
			draw(cards.get(i), g, x, gap + (i / 3) * (heights[0] + gap));
		}
		g.dispose();
		String out = System.getenv("GUIDE_PREVIEW");
		if (out != null)
		{
			ImageIO.write(image, "png", new File(out));
		}
	}

	@Test
	public void brokenCapsShowOnTheCard()
	{
		Guide guide = guides.get("pure-1def");
		GuideProgress progress = GuideProgress.of(guide, ACCOUNT, tasks);
		assertEquals(Integer.valueOf(2), progress.getBroken().get(Skill.DEFENCE));
		TooltipBox card = GuideLooks.card(progress, GuideLooks.theme(guide.getStyle(), Palette.DEFAULT), null, 16, ACCOUNT::level, 0);
		BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Dimension broken = draw(card, g, 0, 0);
		// without the broken cap the card is a line shorter
		GuideProgress.Account capped = new GuideProgress.Account()
		{
			@Override
			public int level(Skill skill)
			{
				return skill == Skill.DEFENCE ? 1 : ACCOUNT.level(skill);
			}

			@Override
			public int xp(Skill skill)
			{
				return skill == Skill.DEFENCE ? 0 : ACCOUNT.xp(skill);
			}

			@Override
			public boolean questDone(String wiki)
			{
				return ACCOUNT.questDone(wiki);
			}

			@Override
			public boolean diaryDone(String title)
			{
				return false;
			}

			@Override
			public boolean checked(String key)
			{
				return false;
			}
		};
		Dimension clean = draw(GuideLooks.card(GuideProgress.of(guide, capped, tasks), GuideLooks.theme(guide.getStyle(), Palette.DEFAULT),
			null, 16, capped::level, 0), g, 0, 0);
		g.dispose();
		assertTrue(broken.height > clean.height);
	}

	@Test
	public void scrollStaysOnAFullPage()
	{
		assertEquals(0, GuideLooks.clampScroll(-2, 20));
		assertEquals(5, GuideLooks.clampScroll(5, 20));
		assertEquals(17, GuideLooks.clampScroll(40, 20));
		assertEquals(0, GuideLooks.clampScroll(3, 2));
		Guide guide = guides.get("vale-totems");
		GuideProgress progress = GuideProgress.of(guide, ACCOUNT, tasks);
		CardTheme theme = GuideLooks.theme(guide.getStyle(), Palette.DEFAULT);
		BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Dimension top = draw(GuideLooks.card(progress, theme, null, 16, ACCOUNT::level, 0), g, 0, 0);
		Dimension end = draw(GuideLooks.card(progress, theme, null, 16, ACCOUNT::level, 1000), g, 0, 0);
		g.dispose();
		assertEquals(top.height, end.height);
	}

	@Test
	public void stepStatus()
	{
		Guide guide = guides.get("vale-totems");
		for (Guide.Step step : guide.steps())
		{
			String status = GuideLooks.stepStatus(step, ACCOUNT::level);
			switch (step.getType())
			{
				case LEVEL:
				case UNLOCK:
					assertEquals(ACCOUNT.level(step.getSkill()) + "/" + step.getLevel(), status);
					break;
				case QUEST:
					assertEquals("Quest", status);
					break;
				default:
					break;
			}
		}
	}
}
