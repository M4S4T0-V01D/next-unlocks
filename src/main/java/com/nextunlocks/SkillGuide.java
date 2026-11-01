package com.nextunlocks;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.FontID;
import net.runelite.api.FontTypeFace;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.SpritePixels;
import net.runelite.api.StructComposition;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;

/**
 * Additions to the game's skill guide: marks on its rows, a progress strip above the list and a Next Unlocks tab.
 * Everything is applied after the guide's rebuild script, which deletes and recreates the tabs and rows each time.
 * Client thread only.
 */
class SkillGuide
{
	/** skill_guide_v2_init(skill, tab, a, b). */
	private static final int INIT_SCRIPT = 1902;
	/** skill_guide_v2_rebuild(a, b), the guide's onresize; rebuilds the tabs and rows. */
	private static final int REBUILD_SCRIPT = 1903;
	/** skill_guide_v2_tab_select(tab). */
	private static final int TAB_SELECT_SCRIPT = 1906;
	/** Sets a tab's border sprites from a struct: (component, first child, struct). */
	private static final int TAB_SPRITES_SCRIPT = 5292;
	/** The tab id of the guide's overview, which hides the list. */
	private static final int OVERVIEW_TAB = 0;
	/** Our tab's id; the game's subsection ids run from 0, and 31 is its level-up view. */
	static final int CUSTOM_TAB = 99;

	/** Tab border sprite structs and their params, as the game's tab builder uses them. */
	private static final int STRUCT_SELECTED = 3712;
	private static final int STRUCT_HOVER = 3713;
	private static final int STRUCT_UNSELECTED = 3714;
	private static final int PARAM_FILL = 1391;
	private static final int PARAM_TOP = 1388;
	private static final int PARAM_BOTTOM = 1394;
	private static final int PARAM_LEFT = 1390;
	private static final int PARAM_TOP_LEFT = 1387;
	private static final int PARAM_BOTTOM_LEFT = 1393;
	private static final int TAB_CHILDREN = 9;
	private static final int TAB_COLOR = 0x726451;
	private static final int TAB_BORDER = 4;
	/** Smallest tab height we squeeze tabs to; the game stops at 20, which can't fit an extra tab in a small guide. */
	static final int MIN_TAB_HEIGHT = 14;

	private static final int ROW_CHILDREN = 4;
	private static final int ROW_PADDING = 6;
	private static final int ROW_MIN_HEIGHT = 36;
	private static final int LINE_HEIGHT = 16;
	private static final int TEXT_COLOR = 0xff981f;
	private static final String NOTE_TAG = "<col=9f9f9f>";
	private static final int FADED = 130;
	private static final int LOCKED_ICON = 150;
	private static final int HIDDEN_TAB_TEXT = 140;

	/** Room the progress strip takes above the list. */
	private static final int STRIP = 22;
	/** The list components' unshifted y and height (minus mode), from the interface. */
	private static final int BORDER_Y = 0;
	private static final int LIST_Y = 2;
	private static final int LIST_HEIGHT_MINUS = 4;
	private static final int SCROLLER_WIDTH = 16;

	private final Client client;
	private final ClientThread clientThread;
	private final NextUnlocksConfig config;
	private final UnlockRepository repository;
	private final Supplier<Snapshot> snapshots;

	/** The rebuild script's arguments, to rebuild with when settings change. */
	private int rebuildA;
	private int rebuildB;
	/** Tabs already scrolled to their next unlock since the guide opened, as skill * 1000 + tab. */
	private final Set<Integer> scrolled = new HashSet<>();

	SkillGuide(Client client, ClientThread clientThread, NextUnlocksConfig config, UnlockRepository repository, Supplier<Snapshot> snapshots)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.repository = repository;
		this.snapshots = snapshots;
	}

	void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == INIT_SCRIPT)
		{
			scrolled.clear();
		}
		else if (event.getScriptId() == REBUILD_SCRIPT)
		{
			ScriptEvent script = event.getScriptEvent();
			Object[] args = script == null ? null : script.getArguments();
			if (args != null && args.length >= 3 && args[1] instanceof Integer && args[2] instanceof Integer)
			{
				rebuildA = (Integer) args[1];
				rebuildB = (Integer) args[2];
			}
			// before the rows are built, so the scrollbar is built for the list's final height
			shiftList(config.guideProgress() ? STRIP : 0);
		}
	}

	void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == REBUILD_SCRIPT)
		{
			apply();
		}
	}

	/** Rebuilds an open guide, after a setting or level change. */
	void refresh()
	{
		clientThread.invokeLater(() ->
		{
			if (!isOpen())
			{
				return;
			}
			if (!config.guideTab() && currentTab() == CUSTOM_TAB)
			{
				client.setVarcIntValue(VarClientID.SKILL_GUIDE_V2_CURRENT_TAB, OVERVIEW_TAB);
			}
			client.runScript(REBUILD_SCRIPT, rebuildA, rebuildB);
		});
	}

	/** Puts the guide back as the game built it. */
	void shutDown()
	{
		clientThread.invokeLater(() ->
		{
			if (!isOpen())
			{
				return;
			}
			if (currentTab() == CUSTOM_TAB)
			{
				client.setVarcIntValue(VarClientID.SKILL_GUIDE_V2_CURRENT_TAB, OVERVIEW_TAB);
			}
			shiftList(0);
			Widget data = client.getWidget(InterfaceID.SkillGuideV2.GUIDE_DATA);
			if (data != null)
			{
				data.deleteAllChildren();
			}
			client.runScript(REBUILD_SCRIPT, rebuildA, rebuildB);
		});
	}

	private boolean isOpen()
	{
		Widget list = client.getWidget(InterfaceID.SkillGuideV2.LIST);
		return list != null && !list.isHidden();
	}

	private int currentTab()
	{
		return client.getVarcIntValue(VarClientID.SKILL_GUIDE_V2_CURRENT_TAB);
	}

	private void apply()
	{
		Skill skill = UnlockRepository.guideSkill(client.getVarcIntValue(VarClientID.SKILL_GUIDE_V2_CURRENT_SKILL));
		Snapshot snapshot = snapshots.get();
		if (skill == null || !snapshot.isLoggedIn())
		{
			return;
		}
		int tab = currentTab();
		if (config.guideTab() || config.guideDimSections())
		{
			tabs(skill, snapshot, tab);
		}
		Widget list = client.getWidget(InterfaceID.SkillGuideV2.LIST);
		if (list == null || tab == OVERVIEW_TAB)
		{
			return;
		}
		strip(skill, snapshot);
		if (tab == CUSTOM_TAB && config.guideTab())
		{
			title(skill);
			customRows(list, skill, snapshot);
		}
		else
		{
			markRows(list, skill, snapshot, tab);
		}
	}

	// tabs

	/**
	 * The tab height and gap for a tab list: the game's own fitting, but squeezing tabs down to
	 * {@link #MIN_TAB_HEIGHT} instead of 20 when they don't fit.
	 */
	static int[] fit(int height, int count)
	{
		int h = 20;
		int gap = 0;
		if (height >= 392)
		{
			h = 26;
			gap = 2;
		}
		else if (height >= 350)
		{
			h = 24;
			gap = 1;
		}
		else if (height >= 308)
		{
			h = 21;
			gap = 1;
		}
		else if (height >= 294)
		{
			h = 21;
		}
		int per = height / Math.max(count, 1);
		while (h + gap > per && (h > MIN_TAB_HEIGHT || gap > -1))
		{
			gap = Math.max(gap - 1, -1);
			if (h + gap > per)
			{
				h = Math.max(h - 1, MIN_TAB_HEIGHT);
			}
		}
		return new int[]{h, gap};
	}

	private static int textOffset(int h)
	{
		return h >= 26 ? 4 : h >= 24 ? 3 : h >= 21 ? 2 : 1;
	}

	private void tabs(Skill skill, Snapshot snapshot, int current)
	{
		Widget tabs = client.getWidget(InterfaceID.SkillGuideV2.TABS);
		Widget[] children = tabs == null ? null : tabs.getDynamicChildren();
		if (children == null || children.length == 0 || children.length % TAB_CHILDREN != 0)
		{
			return;
		}
		int count = children.length / TAB_CHILDREN;
		if (config.guideDimSections())
		{
			for (int i = 0; i < count; i++)
			{
				Widget text = children[i * TAB_CHILDREN + 8];
				if (snapshot.getHiddenSections().contains(Snapshot.sectionKey(skill, Text.removeTags(text.getText()))))
				{
					text.setOpacity(HIDDEN_TAB_TEXT);
				}
			}
		}
		if (!config.guideTab())
		{
			return;
		}

		int height = tabs.getHeight();
		int[] fit = fit(height, count + 1);
		int h = fit[0];
		int gap = fit[1];
		for (int i = 0; i < count; i++)
		{
			layoutTab(children, i * TAB_CHILDREN, i * (h + gap), h, height, i == current);
		}

		int base = count * TAB_CHILDREN;
		boolean selected = current == CUSTOM_TAB;
		StructComposition sprites = client.getStructComposition(selected ? STRUCT_SELECTED : STRUCT_UNSELECTED);
		Widget[] ours = new Widget[TAB_CHILDREN];
		ours[0] = tabs.createChild(base, WidgetType.RECTANGLE);
		ours[0].setFilled(true);
		ours[0].setTextColor(TAB_COLOR);
		int[] params = {PARAM_FILL, PARAM_TOP, PARAM_BOTTOM, PARAM_LEFT, PARAM_TOP_LEFT, PARAM_BOTTOM_LEFT};
		for (int i = 0; i < params.length; i++)
		{
			ours[i + 1] = tabs.createChild(base + i + 1, WidgetType.GRAPHIC);
			ours[i + 1].setSpriteId(sprites.getIntValue(params[i]));
			ours[i + 1].setSpriteTiling(true);
		}
		ours[7] = tabs.createChild(base + 7, WidgetType.GRAPHIC);
		ours[7].setSpriteId(-1);
		ours[8] = tabs.createChild(base + 8, WidgetType.TEXT);
		ours[8].setText("Next Unlocks");
		ours[8].setFontId(FontID.PLAIN_12);
		ours[8].setTextColor(TEXT_COLOR);
		ours[8].setTextShadowed(true);
		ours[8].setXTextAlignment(WidgetTextAlignment.LEFT);
		ours[8].setYTextAlignment(WidgetTextAlignment.CENTER);
		if (!selected)
		{
			ours[0].setAction(0, "View");
			ours[0].setName("<col=ff9040>Next Unlocks</col>");
			ours[0].setOnMouseOverListener(TAB_SPRITES_SCRIPT, InterfaceID.SkillGuideV2.TABS, base, STRUCT_HOVER);
			ours[0].setOnMouseLeaveListener(TAB_SPRITES_SCRIPT, InterfaceID.SkillGuideV2.TABS, base, STRUCT_UNSELECTED);
		}
		ours[0].setOnOpListener(TAB_SELECT_SCRIPT, CUSTOM_TAB);
		ours[0].setHasListener(true);
		layoutTab(ours, 0, count * (h + gap), h, height, selected);
	}

	/** Positions one tab's nine children like the game's tab builder does. */
	private static void layoutTab(Widget[] c, int i, int top, int h, int height, boolean selected)
	{
		place(c[i], 1, top + 1, 3, h - 2, true);
		place(c[i + 1], TAB_BORDER, top + 2, selected ? TAB_BORDER : 2 * TAB_BORDER - 1, h - 4, true);
		place(c[i + 2], TAB_BORDER, top, top == 0 && selected ? TAB_BORDER : 2 * TAB_BORDER - 1, TAB_BORDER, true);
		place(c[i + 3], TAB_BORDER, top + h - TAB_BORDER, top + h == height && selected ? TAB_BORDER : 2 * TAB_BORDER - 1, TAB_BORDER, true);
		place(c[i + 4], 0, top + TAB_BORDER, TAB_BORDER, h - 2 * TAB_BORDER, false);
		place(c[i + 5], 0, top, TAB_BORDER, TAB_BORDER, false);
		place(c[i + 6], 0, top + h - TAB_BORDER, TAB_BORDER, TAB_BORDER, false);
		place(c[i + 7], 3, top + h / 2, 0, 0, false);
		int x = 8 + textOffset(h);
		place(c[i + 8], x, top, x + 6, h, true);
	}

	/** Places a widget at the top left of its parent; minusWidth makes the width "parent width minus". */
	private static void place(Widget w, int x, int y, int width, int height, boolean minusWidth)
	{
		w.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		w.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		w.setWidthMode(minusWidth ? WidgetSizeMode.MINUS : WidgetSizeMode.ABSOLUTE);
		w.setHeightMode(WidgetSizeMode.ABSOLUTE);
		w.setOriginalX(x);
		w.setOriginalY(y);
		w.setOriginalWidth(width);
		w.setOriginalHeight(height);
		w.revalidate();
	}

	private void title(Skill skill)
	{
		Widget frame = client.getWidget(InterfaceID.SkillGuideV2.FRAME);
		Widget[] children = frame == null ? null : frame.getDynamicChildren();
		if (children == null)
		{
			return;
		}
		Widget title = null;
		for (Widget w : children)
		{
			if (w.getType() == WidgetType.TEXT && w.getText() != null && w.getText().startsWith(skill.getName() + " - "))
			{
				title = w;
				break;
			}
		}
		if (title == null)
		{
			return;
		}
		String text = skill.getName() + " - Next Unlocks ";
		title.setText(text);
		FontTypeFace font = title.getFont();
		if (font == null)
		{
			return;
		}
		for (Widget w : children)
		{
			// the skill icon beside the title, centred on the text's width
			if (w.getType() == WidgetType.GRAPHIC && w.getOriginalWidth() == 13 && w.getOriginalHeight() == 13)
			{
				w.setOriginalX(-(font.getTextWidth(text) + 13) / 2 - 5);
				w.revalidate();
			}
		}
	}

	// progress strip

	private void shiftList(int shift)
	{
		move(client.getWidget(InterfaceID.SkillGuideV2.LIST_BORDER), BORDER_Y + shift, shift);
		move(client.getWidget(InterfaceID.SkillGuideV2.LIST), LIST_Y + shift, LIST_HEIGHT_MINUS + shift);
		move(client.getWidget(InterfaceID.SkillGuideV2.LIST_SCROLLER), shift, shift);
		if (shift == 0)
		{
			Widget data = client.getWidget(InterfaceID.SkillGuideV2.GUIDE_DATA);
			if (data != null)
			{
				data.deleteAllChildren();
			}
		}
	}

	private static void move(Widget w, int y, int heightMinus)
	{
		if (w != null && (w.getOriginalY() != y || w.getOriginalHeight() != heightMinus))
		{
			w.setOriginalY(y);
			w.setOriginalHeight(heightMinus);
			w.revalidate();
		}
	}

	private void strip(Skill skill, Snapshot snapshot)
	{
		Widget data = client.getWidget(InterfaceID.SkillGuideV2.GUIDE_DATA);
		if (data == null)
		{
			return;
		}
		data.deleteAllChildren();
		if (!config.guideProgress())
		{
			return;
		}
		Palette palette = snapshot.getPalette();
		int level = snapshot.level(skill);
		int xp = snapshot.xp(skill);
		NavigableMap<Integer, List<Unlock>> next = repository.upcoming(skill, level, snapshot.upcoming(skill), 1);
		double done = 1;
		String left = "All unlocks reached";
		String right = "";
		if (!next.isEmpty())
		{
			int from = Experience.getXpForLevel(repository.previous(skill, level, snapshot.shown(skill)));
			int to = Experience.getXpForLevel(next.firstKey());
			done = to > from ? Math.max(0, Math.min(1, (xp - from) / (double) (to - from))) : 1;
			left = "Next unlock: " + next.firstKey();
			right = NumberFormat.getIntegerInstance().format(Math.max(0, to - xp)) + " xp";
		}
		int width = data.getWidth() - SCROLLER_WIDTH;
		text(data, left, palette.getNext().getRGB() & 0xffffff, WidgetTextAlignment.LEFT, width);
		text(data, right, TEXT_COLOR, WidgetTextAlignment.RIGHT, width);

		Widget background = data.createChild(-1, WidgetType.RECTANGLE);
		background.setFilled(true);
		background.setTextColor(Palette.rgb(palette.getProgressBackground()));
		background.setOpacity(Palette.opacity(palette.getProgressBackground()));
		place(background, 0, 14, width, 5, false);
		Widget fill = data.createChild(-1, WidgetType.RECTANGLE);
		fill.setFilled(true);
		fill.setTextColor(Palette.rgb(palette.getProgress()));
		place(fill, 0, 14, (int) Math.round(width * done), 5, false);
	}

	private static void text(Widget parent, String text, int color, int align, int width)
	{
		Widget w = parent.createChild(-1, WidgetType.TEXT);
		w.setText(text);
		w.setFontId(FontID.PLAIN_11);
		w.setTextColor(color);
		w.setTextShadowed(true);
		w.setXTextAlignment(align);
		w.setYTextAlignment(WidgetTextAlignment.CENTER);
		place(w, 0, 0, width, 13, false);
	}

	// the game's rows

	/** A row of the guide's list: its four children, as the game's row builder makes them. */
	private static final class Row
	{
		Widget background;
		Widget level;
		Widget icon;
		Widget text;
		int lvl;
		Unlock unlock;
	}

	private void markRows(Widget list, Skill skill, Snapshot snapshot, int tab)
	{
		Widget[] children = list.getDynamicChildren();
		if (children == null || children.length % ROW_CHILDREN != 1)
		{
			return;
		}
		Map<Integer, List<Unlock>> byLevel = new HashMap<>();
		for (Unlock u : repository.all(skill))
		{
			byLevel.computeIfAbsent(u.getLevel(), k -> new ArrayList<>()).add(u);
		}
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i + ROW_CHILDREN <= children.length - 1; i += ROW_CHILDREN)
		{
			Row row = new Row();
			row.background = children[i];
			row.level = children[i + 1];
			row.icon = children[i + 2];
			row.text = children[i + 3];
			try
			{
				row.lvl = row.level.isSelfHidden() ? 0 : Integer.parseInt(row.level.getText().trim());
			}
			catch (NumberFormatException e)
			{
				row.lvl = 0;
			}
			row.unlock = match(byLevel.get(row.lvl), row.text.getText());
			rows.add(row);
		}

		int level = snapshot.level(skill);
		int next = Integer.MAX_VALUE;
		for (Row row : rows)
		{
			boolean boosted = row.unlock != null && snapshot.reachedByBoost(skill, row.unlock);
			boolean shown = row.unlock == null || snapshot.getFilter().test(row.unlock);
			if (row.lvl > level && !boosted && shown)
			{
				next = Math.min(next, row.lvl);
			}
		}

		Palette palette = snapshot.getPalette();
		int top = -1;
		int bottom = -1;
		for (Row row : rows)
		{
			if (row.lvl <= 0)
			{
				continue;
			}
			if (config.guideMarkings() && row.unlock != null && snapshot.locked(row.unlock))
			{
				row.level.setTextColor(Palette.rgb(palette.getLocked()));
				row.text.setTextColor(Palette.rgb(palette.getLocked()));
				row.icon.setOpacity(LOCKED_ICON);
			}
			else if (config.guideMarkings() && row.unlock != null && snapshot.reachedByBoost(skill, row.unlock))
			{
				row.level.setTextColor(Palette.rgb(palette.getBoost()));
			}
			else if (row.lvl == next && config.guideHighlight())
			{
				row.level.setTextColor(Palette.rgb(palette.getNext()));
			}
			if (row.lvl == next)
			{
				int y = row.background.getOriginalY();
				top = top < 0 ? y : Math.min(top, y);
				bottom = Math.max(bottom, y + row.background.getOriginalHeight());
			}
			else if (row.lvl > next && config.guideFadeAhead())
			{
				row.level.setOpacity(FADED);
				row.icon.setOpacity(FADED);
				row.text.setOpacity(FADED);
			}
		}
		if (top >= 0)
		{
			if (config.guideHighlight())
			{
				outline(list, top, bottom - top, palette);
			}
			scrollTo(list, skill, tab, top);
		}
	}

	/** The unlock a row shows, by its first line without the guide's "Members: " prefix. */
	static Unlock match(List<Unlock> candidates, String rowText)
	{
		if (candidates == null || rowText == null)
		{
			return null;
		}
		String text = Text.removeTags(rowText.split("<br>")[0]).trim();
		if (text.startsWith("Members: "))
		{
			text = text.substring("Members: ".length()).trim();
		}
		for (Unlock u : candidates)
		{
			if (u.getType() == Unlock.Type.UNLOCK && text.equals(u.getText()))
			{
				return u;
			}
		}
		for (Unlock u : candidates)
		{
			if (u.getType() == Unlock.Type.UNLOCK && (text.startsWith(u.getText()) || u.getText().startsWith(text)))
			{
				return u;
			}
		}
		return null;
	}

	private static void outline(Widget list, int y, int height, Palette palette)
	{
		Widget w = list.createChild(-1, WidgetType.RECTANGLE);
		w.setFilled(false);
		w.setTextColor(Palette.rgb(palette.getOutline()));
		w.setOpacity(Palette.opacity(palette.getOutline()));
		place(w, 0, y, 0, height, true);
	}

	/** Scrolls to a row once per tab while the guide is open; later rebuilds, such as resizes, keep the player's scroll. */
	private void scrollTo(Widget list, Skill skill, int tab, int y)
	{
		if (!config.guideScroll() || !scrolled.add(skill.ordinal() * 1000 + tab))
		{
			return;
		}
		// after the tab select script, which resets the scroll once the rebuild is done
		clientThread.invokeLater(() -> setScroll(list, y));
	}

	private void setScroll(Widget list, int y)
	{
		Widget current = client.getWidget(InterfaceID.SkillGuideV2.LIST);
		if (current != list || list.isHidden())
		{
			return;
		}
		int max = Math.max(0, list.getScrollHeight() - list.getHeight());
		client.runScript(ScriptID.UPDATE_SCROLLBAR, InterfaceID.SkillGuideV2.LIST_SCROLLER, InterfaceID.SkillGuideV2.LIST,
			Math.max(0, Math.min(max, y)));
	}

	// our tab

	private void customRows(Widget list, Skill skill, Snapshot snapshot)
	{
		list.deleteAllChildren();
		Palette palette = snapshot.getPalette();
		int level = snapshot.level(skill);
		int listWidth = list.getWidth();
		int textWidth = listWidth - 66 - 2 * ROW_PADDING;

		List<Unlock> boosted = new ArrayList<>();
		for (Unlock u : repository.all(skill))
		{
			if (snapshot.reachedByBoost(skill, u) && snapshot.shown(skill).test(u))
			{
				boosted.add(u);
			}
		}
		NavigableMap<Integer, List<Unlock>> upcoming = repository.upcoming(skill, level, snapshot.upcoming(skill), snapshot.getLevelsAhead());
		Integer next = upcoming.isEmpty() ? null : upcoming.firstKey();

		int y = 0;
		int index = 0;
		int top = -1;
		int bottom = -1;
		for (Unlock u : boosted)
		{
			y += customRow(list, skill, u, snapshot, index++, y, textWidth, Palette.rgb(palette.getBoost()), false);
		}
		for (List<Unlock> unlocks : upcoming.values())
		{
			for (Unlock u : unlocks)
			{
				boolean isNext = next != null && u.getLevel() == next;
				int color = isNext && config.guideHighlight() ? Palette.rgb(palette.getNext()) : TEXT_COLOR;
				int height = customRow(list, skill, u, snapshot, index++, y, textWidth, color, !isNext && config.guideFadeAhead());
				if (isNext)
				{
					top = top < 0 ? y : top;
					bottom = y + height;
				}
				y += height;
			}
		}
		if (index == 0)
		{
			Widget empty = list.createChild(-1, WidgetType.TEXT);
			empty.setText("Nothing left to unlock with these settings.");
			empty.setFontId(FontID.PLAIN_12);
			empty.setTextColor(TEXT_COLOR);
			empty.setTextShadowed(true);
			empty.setXTextAlignment(WidgetTextAlignment.CENTER);
			empty.setYTextAlignment(WidgetTextAlignment.CENTER);
			place(empty, 0, 0, 0, ROW_MIN_HEIGHT, true);
			y = ROW_MIN_HEIGHT;
		}

		Widget filler = list.createChild(-1, WidgetType.RECTANGLE);
		filler.setFilled(true);
		filler.setTextColor(0);
		filler.setOpacity(index % 2 == 0 ? 200 : 220);
		place(filler, 0, y, 0, list.getHeight(), true);
		if (top >= 0 && config.guideHighlight())
		{
			outline(list, top, bottom - top, palette);
		}
		list.setScrollHeight(y);
		list.revalidateScroll();
		clientThread.invokeLater(() -> setScroll(list, list.getScrollY()));
	}

	/** Adds one row in the guide's own style and returns its height. */
	private int customRow(Widget list, Skill skill, Unlock u, Snapshot snapshot, int index, int y, int textWidth, int levelColor, boolean faded)
	{
		Palette palette = snapshot.getPalette();
		boolean locked = config.guideMarkings() && snapshot.locked(u);
		StringBuilder text = new StringBuilder();
		if (u.getType() == Unlock.Type.QUEST)
		{
			text.append("Quest: ");
		}
		else if (u.getType() == Unlock.Type.DIARY)
		{
			text.append("Diary: ");
		}
		text.append(Text.escapeJagex(u.getText()));
		if (snapshot.reachedByBoost(skill, u))
		{
			text.append("<br><col=").append(Integer.toHexString(Palette.rgb(palette.getBoost()))).append(">Reached with your boost</col>");
		}
		if (u.getNote() != null)
		{
			for (String note : u.getNote().split("\n"))
			{
				text.append("<br>").append(NOTE_TAG).append(Text.escapeJagex(note)).append("</col>");
			}
		}
		if (u.getSection() != null)
		{
			text.append("<br>").append(NOTE_TAG).append(Text.escapeJagex(u.getSection())).append("</col>");
		}

		Widget description = list.createChild(-1, WidgetType.TEXT);
		description.setFontId(FontID.PLAIN_12);
		FontTypeFace font = description.getFont();
		int lines = lines(text.toString(), textWidth, font != null ? font::getTextWidth : s -> s.length() * 6);
		int height = Math.max(lines * LINE_HEIGHT + 2 * ROW_PADDING, ROW_MIN_HEIGHT);

		Widget background = list.createChild(-1, WidgetType.RECTANGLE);
		background.setFilled(true);
		background.setTextColor(0);
		background.setOpacity(index % 2 == 0 ? 200 : 220);
		place(background, 0, y, 0, height, true);
		if (u.getWiki() != null)
		{
			background.setAction(0, "Open wiki");
			background.setName("<col=ff9040>" + Text.escapeJagex(u.getText()) + "</col>");
			background.setOnOpListener((JavaScriptCallback) e -> NextUnlocksPanel.openWiki(u));
			background.setHasListener(true);
		}

		Widget levelText = list.createChild(-1, WidgetType.TEXT);
		levelText.setText(String.valueOf(u.getLevel()));
		levelText.setFontId(FontID.PLAIN_12);
		levelText.setTextColor(locked ? Palette.rgb(palette.getLocked()) : levelColor);
		levelText.setTextShadowed(true);
		levelText.setXTextAlignment(WidgetTextAlignment.LEFT);
		levelText.setYTextAlignment(WidgetTextAlignment.CENTER);
		place(levelText, ROW_PADDING, y, 16, ROW_MIN_HEIGHT, false);

		Integer spriteId = guideSprite(u);
		if (u.getItem() != null || spriteId != null)
		{
			Widget icon = list.createChild(-1, WidgetType.GRAPHIC);
			if (u.getItem() != null)
			{
				icon.setItemId(u.getItem());
				icon.setItemQuantity(-1);
				icon.setItemQuantityMode(ItemQuantityMode.NEVER);
				icon.setBorderType(1);
				place(icon, ROW_PADDING + 25, y + 3, 36, 32, false);
			}
			else
			{
				SpritePixels[] sprite = client.getSprites(client.getIndexSprites(), spriteId, 0);
				int w = sprite != null && sprite.length > 0 ? sprite[0].getMaxWidth() : 25;
				int h = sprite != null && sprite.length > 0 ? sprite[0].getMaxHeight() : 25;
				icon.setSpriteId(spriteId);
				place(icon, ROW_PADDING + 25 + Math.max(0, (36 - w) / 2 - 3), y + 3 + Math.max(0, (32 - h) / 2 - 1), w, h, false);
			}
			icon.setOpacity(locked ? LOCKED_ICON : faded ? FADED : 0);
		}

		description.setText(text.toString());
		description.setTextColor(locked ? Palette.rgb(palette.getLocked()) : TEXT_COLOR);
		description.setTextShadowed(true);
		description.setLineHeight(LINE_HEIGHT);
		description.setXTextAlignment(WidgetTextAlignment.LEFT);
		description.setYTextAlignment(WidgetTextAlignment.CENTER);
		description.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		description.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		description.setWidthMode(WidgetSizeMode.ABSOLUTE);
		description.setHeightMode(WidgetSizeMode.ABSOLUTE);
		description.setOriginalX(ROW_PADDING);
		description.setOriginalY(y + ROW_PADDING);
		description.setOriginalWidth(textWidth);
		description.setOriginalHeight(height - 2 * ROW_PADDING);
		description.revalidate();
		if (faded)
		{
			levelText.setOpacity(FADED);
			description.setOpacity(FADED);
		}
		return height;
	}

	/** The sprite picturing a guide row: the unlock's own, else the quest or diary tab icon for those; or null. */
	static Integer guideSprite(Unlock u)
	{
		if (u.getSprite() != null)
		{
			return u.getSprite();
		}
		switch (u.getType())
		{
			case QUEST:
				return SpriteID.SideIcons.QUEST;
			case DIARY:
				return SpriteID.SideIcons.ACHIEVEMENT_DIARIES;
			default:
				return null;
		}
	}

	/** How many lines the game wraps a text to, at a width; {@code <br>} breaks lines and tags take no room. */
	static int lines(String text, int width, ToIntFunction<String> measure)
	{
		int lines = 0;
		for (String paragraph : text.split("<br>", -1))
		{
			lines++;
			String line = "";
			for (String word : Text.removeTags(paragraph).split(" "))
			{
				String candidate = line.isEmpty() ? word : line + " " + word;
				if (!line.isEmpty() && measure.applyAsInt(candidate) > width)
				{
					lines++;
					line = word;
				}
				else
				{
					line = candidate;
				}
			}
		}
		return lines;
	}
}
