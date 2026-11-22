package com.nextunlocks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

class NextUnlocksPanel extends PluginPanel
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final int TEXT_WIDTH = PANEL_WIDTH - 40;
	/** Text beside a card's icon: the panel less its padding, the card's and the icon's. */
	private static final int CARD_TEXT_WIDTH = TEXT_WIDTH - 50;
	private static final int ICON_WIDTH = 25;
	private static final Color QUEST_COLOR = new Color(255, 152, 31);
	private static final Color DIARY_COLOR = new Color(110, 200, 110);
	private static final Color LEVEL_COLOR = new Color(255, 255, 0);
	private static final int GRID_COLUMNS = 5;
	private static final int PREVIEW_TILES = 4;
	private static final int SEARCH_RESULTS = 60;
	private static final int ITEM_RESULTS = 25;
	private static final int ITEM_ICON = 18;
	private static final Color LOCKED_COLOR = new Color(0xe05a5a);
	private static final int ALMOST_THERE_ROWS = 5;
	private static final int RECENT_ROWS = 5;
	/** "Show more" stops here; the skill pages have the rest. */
	private static final int RECENT_MAX = 50;
	/** The skill box's entry for the Guides page. */
	private static final String GUIDES = "Guides";
	private static final Color DONE_COLOR = new Color(0x4cbb4c);

	private final UnlockRepository repository;
	private final SkillIconManager iconManager;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	/** EDT only. */
	private final Map<Integer, BufferedImage> sprites = new HashMap<>();
	/** Called with a section key and whether to hide it. */
	private final BiConsumer<String, Boolean> sectionToggle;
	/** Called with a goal to set, or to drop when it is one already. */
	private final Consumer<Goal> goalToggle;
	/** Whether a wiki title is a quest or diary that can be pinned. */
	private final Predicate<String> pinnable;
	/** Items with a goal to pin: the levels and quests to wield, wear or make them. */
	/** Every item goal; the skill guide adds to them once the cache is read. */
	private final Supplier<List<Task>> items;
	/** Called with a quest or diary's wiki title to pin, or to unpin when it is pinned. */
	private final Consumer<String> taskToggle;
	/** Asks for an amount goal of a skill's item, like 500 lobsters. */
	private final BiConsumer<Skill, Integer> amountGoal;
	/** Called with a guide's id to follow it, or to stop following it. */
	private final Consumer<String> guideToggle;
	/** Called with a hand-ticked step's key to tick it off, or back on. */
	private final Consumer<String> guideCheck;
	private final IconTextField search = new IconTextField();
	/** The Guides page, "All skills" (null) or a skill. */
	private final JComboBox<Object> skillBox = new JComboBox<>();
	private final JPanel content = new JPanel();

	private Snapshot snapshot = Snapshot.EMPTY;
	private GuidesView guides = GuidesView.EMPTY;
	/** The guide whose steps the Guides page shows, or null for the list. */
	private String openGuide;
	private boolean stale = true;
	private boolean sectionsOpen;
	/** Keys of the collapsed panel sections. */
	private final Set<String> closed = new HashSet<>();
	private boolean almostThereAll;
	private boolean recentAll;

	NextUnlocksPanel(UnlockRepository repository, SkillIconManager iconManager, ItemManager itemManager, SpriteManager spriteManager,
		BiConsumer<String, Boolean> sectionToggle, Consumer<Goal> goalToggle, Predicate<String> pinnable, Consumer<String> taskToggle,
		Supplier<List<Task>> items, BiConsumer<Skill, Integer> amountGoal, Consumer<String> guideToggle, Consumer<String> guideCheck)
	{
		this.guideToggle = guideToggle;
		this.guideCheck = guideCheck;
		this.items = items;
		this.amountGoal = amountGoal;
		this.repository = repository;
		this.iconManager = iconManager;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.sectionToggle = sectionToggle;
		this.goalToggle = goalToggle;
		this.pinnable = pinnable;
		this.taskToggle = taskToggle;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		skillBox.addItem(GUIDES);
		skillBox.addItem(null);
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			skillBox.addItem(skill);
		}
		skillBox.setSelectedIndex(1);
		skillBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
			{
				super.getListCellRendererComponent(list, value, index, selected, focus);
				if (value == GUIDES)
				{
					setText(GUIDES);
					setIcon(sprites.containsKey(SpriteID.SideIcons.QUEST) ? new ImageIcon(sprites.get(SpriteID.SideIcons.QUEST)) : null);
					return this;
				}
				Skill skill = (Skill) value;
				setText(skill == null ? "All skills" : skill.getName());
				setIcon(skill == null ? null : new ImageIcon(iconManager.getSkillImage(skill, true)));
				return this;
			}
		});
		sprite(SpriteID.SideIcons.QUEST, image -> skillBox.repaint());
		skillBox.addActionListener(e ->
		{
			openGuide = null;
			rebuild();
		});

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild();
			}
		});

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel logo = new JLabel(new ImageIcon(ImageUtil.loadImageResource(NextUnlocksPanel.class, "panel_logo.png")));
		logo.setHorizontalAlignment(SwingConstants.CENTER);
		logo.setBorder(new EmptyBorder(0, 0, 4, 0));

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.add(logo, BorderLayout.NORTH);
		top.add(search, BorderLayout.CENTER);
		top.add(skillBox, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	/** EDT. */
	void update(Snapshot snapshot)
	{
		this.snapshot = snapshot;
		if (isShowing())
		{
			rebuild();
		}
		else
		{
			stale = true;
		}
	}

	/** Opens a guide's page, as from its in-game card. EDT. */
	void showGuide(String id)
	{
		skillBox.setSelectedItem(GUIDES);
		openGuide = id;
		rebuild();
	}

	/** EDT. */
	void updateGuides(GuidesView guides)
	{
		this.guides = guides;
		if (isShowing())
		{
			if (skillBox.getSelectedItem() == GUIDES)
			{
				rebuild();
			}
		}
		else
		{
			stale = true;
		}
	}

	@Override
	public void onActivate()
	{
		if (stale)
		{
			rebuild();
		}
	}

	private void rebuild()
	{
		stale = false;
		content.removeAll();
		if (!snapshot.isLoggedIn())
		{
			content.add(note("Log in to see unlocks for your levels. Showing everything from level 1."));
			content.add(Box.createVerticalStrut(8));
		}

		Object item = skillBox.getSelectedItem();
		Skill selected = item instanceof Skill ? (Skill) item : null;
		String query = search.getText().trim();
		if (!query.isEmpty())
		{
			buildSearch(query);
		}
		else if (item == GUIDES)
		{
			buildGuides();
		}
		else if (selected == null)
		{
			buildOverview();
		}
		else
		{
			buildSkill(selected);
		}

		content.add(Box.createVerticalStrut(6));
		String wiki = "OSRS Wiki (CC BY-NC-SA 3.0)" + (repository.getFetched().isEmpty() ? "" : ", " + repository.getFetched());
		JLabel footer = note(repository.isSkillGuide() ? "Unlocks from the in-game skill guide; quests and diaries from the " + wiki : "Data from the " + wiki);
		footer.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(6, 0, 0, 0)));
		content.add(footer);
		content.revalidate();
		content.repaint();
	}

	private void buildOverview()
	{
		class Row
		{
			final Skill skill;
			final int nextLevel;
			final List<Unlock> unlocks;
			final long xpToGo;

			Row(Skill skill, Map.Entry<Integer, List<Unlock>> next)
			{
				this.skill = skill;
				this.nextLevel = next == null ? -1 : next.getKey();
				this.unlocks = next == null ? null : next.getValue();
				this.xpToGo = next == null ? Long.MAX_VALUE : xpToGo(skill, nextLevel);
			}
		}

		List<Row> rows = new ArrayList<>();
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			NavigableMap<Integer, List<Unlock>> next = repository.upcoming(skill, snapshot.level(skill), snapshot.upcoming(skill), 1);
			rows.add(new Row(skill, next.firstEntry()));
		}
		if (snapshot.isLoggedIn())
		{
			// closest unlock first
			rows.sort(Comparator.comparingLong(r -> r.xpToGo));
		}

		buildGoals();
		buildAlmostThere(null);
		buildRecent();
		if (!section("next", "Next unlocks", rows.size()))
		{
			return;
		}
		for (Row row : rows)
		{
			JPanel card = card();
			Runnable open = () -> skillBox.setSelectedItem(row.skill);
			card.add(new JLabel(new ImageIcon(iconManager.getSkillImage(row.skill, true))), BorderLayout.WEST);

			JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			top.add(html("<b>" + row.skill.getName() + "</b> " + snapshot.level(row.skill), TEXT_WIDTH - 110), BorderLayout.WEST);
			if (row.unlocks != null && snapshot.isLoggedIn())
			{
				JLabel xp = new JLabel(shortXp(row.xpToGo) + " xp");
				xp.setFont(FontManager.getRunescapeSmallFont());
				xp.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				top.add(xp, BorderLayout.EAST);
			}

			JPanel body = new JPanel(new BorderLayout(0, 3));
			body.setOpaque(false);
			body.add(top, BorderLayout.NORTH);
			body.add(row.unlocks == null ? note("Nothing left to unlock") : preview(row.skill, row.unlocks, open), BorderLayout.CENTER);
			if (row.unlocks != null && snapshot.isLoggedIn())
			{
				body.add(progress(row.skill, row.nextLevel), BorderLayout.SOUTH);
			}
			card.add(body, BorderLayout.CENTER);
			clickable(card, open);
			addCard(card);
		}
	}

	/** The unlocks at a skill's next level: their pictures, or the first one's text when none has a picture. */
	private JComponent preview(Skill skill, List<Unlock> unlocks, Runnable open)
	{
		List<Unlock> pictured = unlocks.stream().filter(Unlock::hasIcon).collect(Collectors.toList());
		if (pictured.isEmpty())
		{
			Unlock first = unlocks.get(0);
			String more = unlocks.size() > 1 ? " (+" + (unlocks.size() - 1) + ")" : "";
			return html(levelText(skill, first) + colored(escape(first.getText() + more), textColor(first)), TEXT_WIDTH - 70);
		}

		// one slot goes to the "+N" count when not everything fits
		int shown = unlocks.size() > PREVIEW_TILES ? Math.min(pictured.size(), PREVIEW_TILES - 1) : pictured.size();
		JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		tiles.setOpaque(false);
		for (Unlock u : pictured.subList(0, shown))
		{
			tiles.add(tile(skill, u, open));
		}
		if (unlocks.size() > shown)
		{
			JLabel more = new JLabel("+" + (unlocks.size() - shown));
			more.setFont(FontManager.getRunescapeSmallFont());
			more.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			more.setToolTipText(unlocks.stream().filter(u -> !pictured.subList(0, shown).contains(u))
				.map(u -> escape(u.getText())).collect(Collectors.joining("<br>", "<html>", "</html>")));
			tiles.add(more);
		}
		return tiles;
	}

	/** A card per goal, with the xp left to it, then one per amount goal. */
	private void buildGoals()
	{
		List<Goal> goals = snapshot.getGoals();
		List<Tally> tallies = snapshot.getTallies();
		if (goals.isEmpty() && tallies.isEmpty() || !section("goals", "Goals", goals.size() + tallies.size()))
		{
			return;
		}
		for (Goal goal : goals)
		{
			Skill skill = goal.getSkill();
			Unlock u = repository.all(skill).stream().filter(x -> goal.is(skill, x)).findFirst().orElse(null);
			Runnable open = () -> skillBox.setSelectedItem(skill);
			JPanel card = card();
			card.add(u != null && u.hasIcon() ? tile(skill, u, open) : new JLabel(new ImageIcon(iconManager.getSkillImage(skill, true))), BorderLayout.WEST);

			JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			top.add(html("<b>" + skill.getName() + " " + goal.getLevel() + "</b>", TEXT_WIDTH - 110), BorderLayout.WEST);
			long xp = xpToGo(skill, goal.getLevel());
			if (snapshot.isLoggedIn())
			{
				JLabel left = new JLabel(shortXp(xp) + " xp");
				left.setFont(FontManager.getRunescapeSmallFont());
				left.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				top.add(left, BorderLayout.EAST);
			}

			JPanel body = new JPanel(new BorderLayout(0, 3));
			body.setOpaque(false);
			body.add(top, BorderLayout.NORTH);
			body.add(html(colored(escape(goal.getText()), u == null ? ColorScheme.LIGHT_GRAY_COLOR : textColor(u)), TEXT_WIDTH - 70), BorderLayout.CENTER);
			if (snapshot.isLoggedIn())
			{
				body.add(progress(skill, repository.previous(skill, snapshot.level(skill), snapshot.shown(skill)), goal.getLevel()), BorderLayout.SOUTH);
			}
			card.add(body, BorderLayout.CENTER);
			card.setToolTipText("Right-click to remove the goal");
			clickable(card, open);
			JPopupMenu menu = new JPopupMenu();
			menu.add(menuItem("Remove goal", () -> goalToggle.accept(goal)));
			if (u != null && u.getWiki() != null)
			{
				menu.add(menuItem("Open on the OSRS Wiki", () -> openWiki(u.getWiki())));
			}
			card.setComponentPopupMenu(menu);
			addCard(card);
		}
		for (Tally tally : tallies)
		{
			addCard(tallyCard(tally));
		}
		content.add(Box.createVerticalStrut(6));
	}

	/** An amount goal's card: the item, how many so far, and a bar in the skill's colour. */
	private JPanel tallyCard(Tally tally)
	{
		Skill skill = tally.getSkill();
		JPanel card = card();
		JLabel icon = new JLabel();
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.TOP);
		icon.setPreferredSize(new Dimension(ICON_WIDTH, ICON_WIDTH));
		AsyncBufferedImage image = itemManager.getImage(tally.getItem());
		Runnable set = () -> icon.setIcon(new ImageIcon(ImageUtil.resizeImage(image, ITEM_ICON + 4, ITEM_ICON + 4, true)));
		set.run();
		image.onLoaded(() -> SwingUtilities.invokeLater(set));
		card.add(icon, BorderLayout.WEST);

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(html("<b>" + escape(tally.goalText()) + "</b>", TEXT_WIDTH - 110), BorderLayout.WEST);
		JLabel count = new JLabel(tally.isDone() ? "Done!" : String.format(Locale.ROOT, "%,d/%,d", tally.getCount(), tally.getTarget()));
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(tally.isDone() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		top.add(count, BorderLayout.EAST);

		ProgressBar bar = new ProgressBar();
		bar.setMaximumValue(tally.getTarget());
		bar.setValue(Math.min(tally.getCount(), tally.getTarget()));
		bar.setForeground(Skills.color(skill));
		bar.setBackground(new Color(Palette.rgb(snapshot.getPalette().getProgressBackground())));
		bar.setPreferredSize(new Dimension(0, 4));
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));

		JPanel body = new JPanel(new BorderLayout(0, 3));
		body.setOpaque(false);
		body.add(top, BorderLayout.NORTH);
		JLabel how = new JLabel(skill.getName() + (Tally.usesUp(skill) ? ", counts items used up" : ", counts items gained"));
		how.setFont(FontManager.getRunescapeSmallFont());
		how.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		body.add(how, BorderLayout.CENTER);
		body.add(bar, BorderLayout.SOUTH);
		card.add(body, BorderLayout.CENTER);

		Runnable open = () -> skillBox.setSelectedItem(skill);
		card.setToolTipText(bar.getPercentage() + "% there. Right-click to change the amount or remove the goal");
		clickable(card, open);
		JPopupMenu menu = new JPopupMenu();
		menu.add(menuItem("Change amount", () -> amountGoal.accept(skill, tally.getItem())));
		menu.add(menuItem("Remove goal", () -> taskToggle.accept(tally.key())));
		card.setComponentPopupMenu(menu);
		return card;
	}

	/** Quests and diaries one skill level away, and unlocks only waiting on a quest; of one skill, or of all when null. */
	private void buildAlmostThere(Skill only)
	{
		if (!snapshot.isAlmostThere())
		{
			return;
		}
		AlmostThere almost = AlmostThere.of(repository, snapshot);
		List<AlmostThere.OneLevelShort> shorts = almost.getOneLevelShort().stream()
			.filter(o -> only == null || o.getSkill() == only)
			.collect(Collectors.toList());
		List<AlmostThere.QuestWait> waits = only != null ? new ArrayList<>() : almost.getQuestWaits();
		if (shorts.isEmpty() && waits.isEmpty() || !section("almost", "Almost there", shorts.size() + waits.size()))
		{
			return;
		}

		int rows = almostThereAll ? Integer.MAX_VALUE : ALMOST_THERE_ROWS;
		for (AlmostThere.OneLevelShort o : shorts.subList(0, Math.min(shorts.size(), rows)))
		{
			boolean quest = o.getType() == Unlock.Type.QUEST;
			JPanel card = card();
			card.add(spriteIcon(quest ? SpriteID.SideIcons.QUEST : SpriteID.SideIcons.ACHIEVEMENT_DIARIES), BorderLayout.WEST);

			JLabel needs = new JLabel("Needs " + o.getSkill().getName() + " " + o.getLevel()
				+ (snapshot.isLoggedIn() ? " \u00B7 " + shortXp(o.getXpToGo()) + " xp" : ""));
			needs.setIcon(new ImageIcon(iconManager.getSkillImage(o.getSkill(), true)));
			needs.setIconTextGap(4);
			needs.setFont(FontManager.getRunescapeSmallFont());
			needs.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

			JPanel body = new JPanel(new BorderLayout(0, 3));
			body.setOpaque(false);
			body.add(html(colored(escape(displayTitle(o.getTitle())), quest ? QUEST_COLOR : DIARY_COLOR), CARD_TEXT_WIDTH), BorderLayout.NORTH);
			body.add(needs, BorderLayout.CENTER);
			if (snapshot.isLoggedIn())
			{
				body.add(progress(o.getSkill(), o.getLevel()), BorderLayout.SOUTH);
			}
			card.add(body, BorderLayout.CENTER);
			card.setToolTipText("<html>" + escape(o.getTitle()) + "<br>Every other skill level for this " + (quest ? "quest" : "diary") + " is met"
				+ (quest ? "" : "<br>Diary completion isn't checked")
				+ "<br>Click to open it on the OSRS Wiki</html>");
			clickable(card, () -> openWiki(o.getTitle()));
			card.setComponentPopupMenu(taskMenu(o.getTitle()));
			addCard(card);
		}

		for (AlmostThere.QuestWait w : waits.subList(0, Math.min(waits.size(), rows)))
		{
			int n = w.getUnlocks().size();
			JPanel card = card();
			card.add(spriteIcon(SpriteID.SideIcons.QUEST), BorderLayout.WEST);

			JPanel body = new JPanel(new BorderLayout(0, 3));
			body.setOpaque(false);
			body.add(html(colored(escape(displayTitle(w.getQuest())), QUEST_COLOR), CARD_TEXT_WIDTH), BorderLayout.NORTH);
			JLabel holds = html(colored("Holds back " + n + (n == 1 ? " unlock" : " unlocks") + " you have the level for", ColorScheme.LIGHT_GRAY_COLOR), CARD_TEXT_WIDTH);
			holds.setFont(FontManager.getRunescapeSmallFont());
			body.add(holds, BorderLayout.CENTER);
			card.add(body, BorderLayout.CENTER);

			StringBuilder tip = new StringBuilder("<html>");
			for (int i = 0; i < n && i < 15; i++)
			{
				Unlock u = w.getUnlocks().get(i);
				tip.append(w.getSkills().get(i).getName()).append(' ').append(u.getLevel()).append(": ").append(escape(u.getText())).append("<br>");
			}
			if (n > 15)
			{
				tip.append("+").append(n - 15).append(" more<br>");
			}
			tip.append("Click to open ").append(escape(w.getQuest())).append(" on the OSRS Wiki</html>");
			card.setToolTipText(tip.toString());
			clickable(card, () -> openWiki(w.getQuest()));
			card.setComponentPopupMenu(taskMenu(w.getQuest()));
			addCard(card);
		}

		int hidden = Math.max(0, shorts.size() - ALMOST_THERE_ROWS) + Math.max(0, waits.size() - ALMOST_THERE_ROWS);
		if (hidden > 0)
		{
			JLabel more = new JLabel(almostThereAll ? "Show fewer" : "Show " + hidden + " more");
			more.setFont(FontManager.getRunescapeSmallFont());
			more.setForeground(LEVEL_COLOR);
			more.setAlignmentX(LEFT_ALIGNMENT);
			clickable(more, () ->
			{
				almostThereAll = !almostThereAll;
				rebuild();
			});
			content.add(more);
		}
		content.add(Box.createVerticalStrut(8));
	}

	/**
	 * A collapsible section header: an arrow, the title, a count on the right and a rule under them.
	 * Returns whether the section is open.
	 */
	private boolean section(String key, String title, int count)
	{
		boolean open = !closed.contains(key);
		JLabel name = new JLabel((open ? "\u25BE " : "\u25B8 ") + title);
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		JLabel number = new JLabel(String.valueOf(count));
		number.setFont(FontManager.getRunescapeSmallFont());
		number.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(0, 0, 3, 0)));
		header.add(name, BorderLayout.WEST);
		header.add(number, BorderLayout.EAST);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
		header.setToolTipText(open ? "Click to collapse" : "Click to expand");
		clickable(header, () ->
		{
			if (!closed.remove(key))
			{
				closed.add(key);
			}
			rebuild();
		});
		content.add(header);
		content.add(Box.createVerticalStrut(open ? 6 : 8));
		return open;
	}

	/** The Guides page: the followed guides, then every guide by category, or one guide's steps. */
	private void buildGuides()
	{
		Guide open = openGuide == null ? null : guides.getGuides().stream()
			.filter(g -> g.getId().equals(openGuide)).findFirst().orElse(null);
		if (open != null)
		{
			buildGuide(open);
			return;
		}
		content.add(note("Follow up to " + NextUnlocksPlugin.MAX_GUIDES + " guides. Steps tick off as you play, and the guides"
			+ " you follow get a card in game. Click a guide to see its steps."));
		content.add(Box.createVerticalStrut(8));

		List<Guide> followed = guides.getGuides().stream().filter(guides::isFollowed)
			.sorted(Comparator.comparingInt(g -> guides.getFollowed().indexOf(g.getId())))
			.collect(Collectors.toList());
		if (!followed.isEmpty() && section("guides:following", "Following", followed.size()))
		{
			followed.forEach(g -> addCard(guideCard(g, true)));
			content.add(Box.createVerticalStrut(8));
		}

		Map<String, List<Guide>> byCategory = new LinkedHashMap<>();
		for (Guide guide : guides.getGuides())
		{
			byCategory.computeIfAbsent(guide.getCategory(), k -> new ArrayList<>()).add(guide);
		}
		for (Map.Entry<String, List<Guide>> category : byCategory.entrySet())
		{
			List<Guide> list = new ArrayList<>(category.getValue());
			if (guides.isLoggedIn())
			{
				// guides for this kind of account first
				Guide.Account other = guides.isIron() ? Guide.Account.MAIN : Guide.Account.IRON;
				list.sort(Comparator.comparing(g -> g.getAccount() == other));
			}
			if (section("guides:" + category.getKey(), category.getKey(), list.size()))
			{
				list.forEach(g -> addCard(guideCard(g, false)));
				content.add(Box.createVerticalStrut(8));
			}
		}
	}

	/** A guide in the list: its accent, name, what it's for and how far along it is. */
	private JPanel guideCard(Guide guide, boolean following)
	{
		GuideProgress progress = guides.getProgress().get(guide.getId());
		Color accent = GuideLooks.accent(guide.getStyle());
		JPanel card = card();
		card.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, accent), new EmptyBorder(6, 6, 6, 6)));

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		String tags = (guide.isMembers() ? "" : " " + colored("F2P", ColorScheme.LIGHT_GRAY_COLOR))
			+ (guide.getAccount() == Guide.Account.IRON ? " " + colored("Iron", GuideLooks.accent(Guide.Style.IRON)) : "")
			+ (!following && guides.isFollowed(guide) ? " " + colored("✓ Following", DONE_COLOR) : "");
		top.add(html("<b>" + colored(escape(guide.getName()), accent) + "</b>" + tags, CARD_TEXT_WIDTH), BorderLayout.WEST);
		if (progress != null)
		{
			JLabel count = new JLabel(progress.done() + "/" + progress.total());
			count.setFont(FontManager.getRunescapeSmallFont());
			count.setForeground(progress.isComplete() ? DONE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
			top.add(count, BorderLayout.EAST);
		}

		JPanel body = new JPanel(new BorderLayout(0, 3));
		body.setOpaque(false);
		body.add(top, BorderLayout.NORTH);
		String line = guide.getAbout();
		if (following && progress != null)
		{
			List<Guide.Step> next = progress.upcoming(1);
			line = progress.isComplete() ? "Guide complete!" : next.isEmpty() ? "" : "Next: " + next.get(0).getTitle();
		}
		JLabel about = html(colored(escape(line), ColorScheme.LIGHT_GRAY_COLOR), TEXT_WIDTH - 10);
		about.setFont(FontManager.getRunescapeSmallFont());
		body.add(about, BorderLayout.CENTER);
		if (progress != null && progress.done() > 0)
		{
			body.add(stepBar(progress, accent), BorderLayout.SOUTH);
		}
		card.add(body, BorderLayout.CENTER);
		card.setToolTipText("Click to see the steps");
		clickable(card, () ->
		{
			openGuide = guide.getId();
			rebuild();
		});
		JPopupMenu menu = new JPopupMenu();
		menu.add(followItem(guide));
		card.setComponentPopupMenu(menu);
		return card;
	}

	private JMenuItem followItem(Guide guide)
	{
		return menuItem(guides.isFollowed(guide) ? "Stop following" : "Follow", () -> guideToggle.accept(guide.getId()));
	}

	private static JComponent stepBar(GuideProgress progress, Color color)
	{
		ProgressBar bar = new ProgressBar();
		bar.setMaximumValue(Math.max(1, progress.total()));
		bar.setValue(progress.done());
		bar.setForeground(color);
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setPreferredSize(new Dimension(0, 4));
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
		bar.setToolTipText(progress.done() + " of " + progress.total() + " steps done");
		return bar;
	}

	/** One guide: a header to follow it, its caps, then its steps by group. */
	private void buildGuide(Guide guide)
	{
		GuideProgress progress = guides.getProgress().get(guide.getId());
		Color accent = GuideLooks.accent(guide.getStyle());

		JLabel back = new JLabel("‹ All guides");
		back.setFont(FontManager.getRunescapeSmallFont());
		back.setForeground(LEVEL_COLOR);
		back.setAlignmentX(LEFT_ALIGNMENT);
		clickable(back, () ->
		{
			openGuide = null;
			rebuild();
		});
		content.add(back);
		content.add(Box.createVerticalStrut(6));

		JPanel head = new JPanel(new BorderLayout(0, 4));
		head.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		head.setAlignmentX(LEFT_ALIGNMENT);
		head.setBorder(new CompoundBorder(new MatteBorder(3, 0, 0, 0, accent), new EmptyBorder(8, 8, 8, 8)));
		StringBuilder text = new StringBuilder("<b>" + colored(escape(guide.getName()), accent) + "</b><br>"
			+ colored(escape(guide.getAbout()), ColorScheme.LIGHT_GRAY_COLOR));
		if (guide.isPure())
		{
			text.append("<br><br>Keep ");
			List<String> caps = new ArrayList<>();
			guide.getCaps().forEach((skill, cap) ->
			{
				Integer at = progress == null ? null : progress.getBroken().get(skill);
				caps.add(at == null ? skill.getName() + " at " + cap
					: colored(skill.getName() + " at " + cap + " (it's " + at + ")", LOCKED_COLOR));
			});
			text.append(String.join(", ", caps)).append('.');
		}
		head.add(html(text.toString(), TEXT_WIDTH - 10), BorderLayout.NORTH);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		boolean following = guides.isFollowed(guide);
		JLabel follow = new JLabel(following ? "✓ Following" : "Follow", SwingConstants.CENTER);
		follow.setOpaque(true);
		follow.setBackground(following ? ColorScheme.DARK_GRAY_COLOR : accent.darker());
		follow.setForeground(following ? DONE_COLOR : Color.WHITE);
		follow.setBorder(new EmptyBorder(4, 10, 4, 10));
		follow.setToolTipText(following ? "Click to stop following; its card leaves the game screen"
			: guides.getFollowed().size() >= NextUnlocksPlugin.MAX_GUIDES ? "You follow " + NextUnlocksPlugin.MAX_GUIDES + " guides; stop one first"
			: "Click to follow: its next steps get a card in game");
		clickable(follow, () -> guideToggle.accept(guide.getId()));
		row.add(follow, BorderLayout.WEST);
		if (progress != null)
		{
			JLabel count = new JLabel(progress.isComplete() ? "Complete!" : progress.done() + " of " + progress.total() + " done");
			count.setFont(FontManager.getRunescapeSmallFont());
			count.setForeground(progress.isComplete() ? DONE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
			row.add(count, BorderLayout.EAST);
		}
		head.add(row, BorderLayout.CENTER);
		if (progress != null)
		{
			head.add(stepBar(progress, accent), BorderLayout.SOUTH);
		}
		head.setMaximumSize(new Dimension(Integer.MAX_VALUE, head.getPreferredSize().height));
		content.add(head);
		content.add(Box.createVerticalStrut(8));
		if (!guides.isLoggedIn())
		{
			content.add(note("Log in to tick off the steps you've done."));
			content.add(Box.createVerticalStrut(8));
		}

		for (Guide.Group group : guide.getGroups())
		{
			long done = progress == null ? 0 : group.getSteps().stream().filter(progress::isDone).count();
			if (section("guide:" + guide.getId() + ":" + group.getName(), group.getName(), group.getSteps().size() - (int) done))
			{
				group.getSteps().forEach(step -> addCard(stepCard(step, progress)));
				content.add(Box.createVerticalStrut(8));
			}
		}
	}

	/** A guide step: its icon, what to do and why, ticked once done. */
	private JPanel stepCard(Guide.Step step, GuideProgress progress)
	{
		boolean done = progress != null && progress.isDone(step);
		JPanel card = card();
		card.add(stepIcon(step), BorderLayout.WEST);

		Color color;
		switch (step.getType())
		{
			case QUEST:
				color = QUEST_COLOR;
				break;
			case DIARY:
				color = DIARY_COLOR;
				break;
			case LEVEL:
			case UNLOCK:
				color = LEVEL_COLOR;
				break;
			default:
				color = Color.WHITE;
		}
		String title = done ? colored("✓ ", DONE_COLOR) + colored(escape(step.getTitle()), ColorScheme.LIGHT_GRAY_COLOR)
			: colored(escape(step.getTitle()), color);
		StringBuilder text = new StringBuilder(title);
		if (!done && step.getWhy() != null)
		{
			text.append("<br>").append(colored(escape(step.getWhy()), ColorScheme.LIGHT_GRAY_COLOR));
		}
		String warning = progress == null || done ? null : progress.warning(step);
		if (warning != null)
		{
			text.append("<br>").append(colored(escape(warning), LOCKED_COLOR));
		}
		JLabel body = html(text.toString(), CARD_TEXT_WIDTH);
		if (done)
		{
			body.setFont(FontManager.getRunescapeSmallFont());
		}
		card.add(body, BorderLayout.CENTER);

		JPopupMenu menu = new JPopupMenu();
		Runnable click;
		String tip;
		if (step.isManual())
		{
			click = () -> guideCheck.accept(step.getKey());
			tip = done ? "Click to mark it not done" : "Click to tick it off";
			menu.add(menuItem(done ? "Mark not done" : "Mark done", click));
		}
		else if (step.getSkill() != null)
		{
			Skill skill = step.getSkill();
			click = () -> skillBox.setSelectedItem(skill);
			tip = "Ticks off at " + skill.getName() + " " + step.getLevel() + "; click to see " + skill.getName() + " unlocks";
		}
		else
		{
			click = () -> openWiki(step.getWiki());
			tip = "Ticks off once it's done in game; click to open it on the OSRS Wiki";
			if (pinnable.test(step.getWiki()))
			{
				menu.add(pinItem(step.getWiki()));
			}
		}
		if (step.getWiki() != null)
		{
			menu.add(menuItem("Open on the OSRS Wiki", () -> openWiki(step.getWiki())));
		}
		card.setToolTipText(tip);
		clickable(card, click);
		card.setComponentPopupMenu(menu.getComponentCount() == 0 ? null : menu);
		return card;
	}

	private JLabel stepIcon(Guide.Step step)
	{
		Integer item = step.getIcon() != null ? step.getIcon() : step.getUnlock() != null ? step.getUnlock().getItem() : null;
		if (item != null)
		{
			JLabel label = new JLabel();
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setVerticalAlignment(SwingConstants.TOP);
			label.setPreferredSize(new Dimension(ICON_WIDTH, ICON_WIDTH));
			AsyncBufferedImage image = itemManager.getImage(item);
			Runnable set = () -> label.setIcon(new ImageIcon(ImageUtil.resizeImage(image, ITEM_ICON + 4, ITEM_ICON + 4, true)));
			set.run();
			image.onLoaded(() -> SwingUtilities.invokeLater(set));
			return label;
		}
		switch (step.getType())
		{
			case QUEST:
				return spriteIcon(SpriteID.SideIcons.QUEST);
			case DIARY:
				return spriteIcon(SpriteID.SideIcons.ACHIEVEMENT_DIARIES);
			case LEVEL:
			case UNLOCK:
				JLabel label = new JLabel(new ImageIcon(iconManager.getSkillImage(step.getSkill(), true)));
				label.setVerticalAlignment(SwingConstants.TOP);
				label.setPreferredSize(new Dimension(ICON_WIDTH, ICON_WIDTH));
				return label;
			default:
				return spriteIcon(SpriteID.SideIcons.OPTIONS);
		}
	}

	/** A wiki title as players know it: a subquest's or a page section's own name. */
	static String displayTitle(String title)
	{
		int hash = title.indexOf('#');
		if (hash >= 0 && hash < title.length() - 1)
		{
			return title.substring(hash + 1);
		}
		int slash = title.lastIndexOf('/');
		return slash >= 0 && slash < title.length() - 1 ? title.substring(slash + 1) : title;
	}

	/** A game sprite in an icon-wide column, filled in once it loads. */
	private JLabel spriteIcon(int id)
	{
		JLabel label = new JLabel();
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setVerticalAlignment(SwingConstants.TOP);
		label.setPreferredSize(new Dimension(ICON_WIDTH, ICON_WIDTH));
		sprite(id, image -> label.setIcon(new ImageIcon(image)));
		return label;
	}

	/** Hands the sprite to the consumer on the EDT, from the cache or once it loads. */
	private void sprite(int id, Consumer<BufferedImage> consumer)
	{
		BufferedImage cached = sprites.get(id);
		if (cached != null)
		{
			consumer.accept(cached);
			return;
		}
		spriteManager.getSpriteAsync(id, 0, image -> SwingUtilities.invokeLater(() ->
		{
			sprites.put(id, image);
			consumer.accept(image);
		}));
	}

	private void buildSkill(Skill skill)
	{
		int level = snapshot.level(skill);
		NavigableMap<Integer, List<Unlock>> upcoming = repository.upcoming(skill, level, snapshot.upcoming(skill), snapshot.getLevelsAhead());

		// the skill, its level and the way to its next unlock, on a card of their own
		JPanel card = card();
		JLabel icon = new JLabel(new ImageIcon(iconManager.getSkillImage(skill, false)));
		icon.setVerticalAlignment(SwingConstants.TOP);
		card.add(icon, BorderLayout.WEST);
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(html("<b>" + skill.getName() + "</b>", TEXT_WIDTH - 110), BorderLayout.WEST);
		JLabel levelLabel = new JLabel("Level " + level);
		levelLabel.setFont(FontManager.getRunescapeBoldFont());
		levelLabel.setForeground(snapshot.getPalette().getLevel());
		top.add(levelLabel, BorderLayout.EAST);
		JPanel body = new JPanel(new BorderLayout(0, 3));
		body.setOpaque(false);
		body.add(top, BorderLayout.NORTH);
		if (!upcoming.isEmpty() && snapshot.isLoggedIn())
		{
			int next = upcoming.firstKey();
			JLabel toGo = new JLabel(shortXp(xpToGo(skill, next)) + " xp to level " + next);
			toGo.setFont(FontManager.getRunescapeSmallFont());
			toGo.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			body.add(toGo, BorderLayout.CENTER);
			body.add(progress(skill, next), BorderLayout.SOUTH);
		}
		card.add(body, BorderLayout.CENTER);
		addCard(card);
		content.add(Box.createVerticalStrut(4));
		buildSections(skill);
		buildAlmostThere(skill);

		List<Unlock> boosted = repository.all(skill).stream()
			.filter(u -> snapshot.reachedByBoost(skill, u) && snapshot.shown(skill).test(u))
			.collect(Collectors.toList());
		if (!boosted.isEmpty())
		{
			JLabel label = new JLabel("Reached with your boost");
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(snapshot.getPalette().getBoost());
			label.setAlignmentX(LEFT_ALIGNMENT);
			content.add(label);
			content.add(Box.createVerticalStrut(3));
			addUnlocks(skill, boosted);
		}

		List<Unlock> next = upcoming.values().stream().flatMap(List::stream).collect(Collectors.toList());
		if (section("upcoming", "Upcoming unlocks", next.size()))
		{
			if (next.isEmpty())
			{
				content.add(note("Nothing left to unlock with these filters."));
			}
			else
			{
				addUnlocks(skill, next);
			}
		}
		content.add(Box.createVerticalStrut(4));
		buildTimeline(skill);
	}

	/** A level-up from the history and what it unlocked with the current filters. */
	private static final class Reached
	{
		final History.Entry entry;
		final List<Unlock> unlocks;

		Reached(History.Entry entry, List<Unlock> unlocks)
		{
			this.entry = entry;
			this.unlocks = unlocks;
		}
	}

	/** Level-ups that unlocked something, newest first, of one skill or all when null. */
	private List<Reached> reached(Skill only)
	{
		List<History.Entry> history = snapshot.getHistory();
		List<Reached> out = new ArrayList<>();
		for (int i = history.size() - 1; i >= 0; i--)
		{
			History.Entry e = history.get(i);
			if (only != null && e.getSkill() != only)
			{
				continue;
			}
			List<Unlock> unlocks = new ArrayList<>();
			for (int level = e.getFrom() + 1; level <= e.getTo(); level++)
			{
				unlocks.addAll(repository.at(e.getSkill(), level, snapshot.shown(e.getSkill())));
			}
			if (!unlocks.isEmpty())
			{
				out.add(new Reached(e, unlocks));
			}
		}
		return out;
	}

	private static String levels(History.Entry e)
	{
		return e.getTo() - e.getFrom() > 1 ? (e.getFrom() + 1) + "-" + e.getTo() : String.valueOf(e.getTo());
	}

	private JLabel when(History.Entry e, long now)
	{
		JLabel label = new JLabel(History.ago(e.getTime(), now, ZoneId.systemDefault()));
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setToolTipText(History.date(e.getTime(), ZoneId.systemDefault()));
		return label;
	}

	/** The latest level-ups that unlocked something, as cards like the Next unlocks rows. */
	private void buildRecent()
	{
		if (!snapshot.isRecentUnlocks() || !snapshot.isLoggedIn())
		{
			return;
		}
		List<Reached> recent = reached(null);
		if (recent.isEmpty() || !section("recent", "Recently unlocked", recent.size()))
		{
			return;
		}
		long now = System.currentTimeMillis();
		for (Reached r : recent.subList(0, Math.min(recent.size(), recentAll ? RECENT_MAX : RECENT_ROWS)))
		{
			Skill skill = r.entry.getSkill();
			JPanel card = card();
			Runnable open = () -> skillBox.setSelectedItem(skill);
			card.add(new JLabel(new ImageIcon(iconManager.getSkillImage(skill, true))), BorderLayout.WEST);

			JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			top.add(html("<b>" + skill.getName() + "</b> " + levels(r.entry), TEXT_WIDTH - 110), BorderLayout.WEST);
			top.add(when(r.entry, now), BorderLayout.EAST);

			JPanel body = new JPanel(new BorderLayout(0, 3));
			body.setOpaque(false);
			body.add(top, BorderLayout.NORTH);
			body.add(preview(skill, r.unlocks, open), BorderLayout.CENTER);
			card.add(body, BorderLayout.CENTER);
			clickable(card, open);
			addCard(card);
		}

		if (recent.size() > RECENT_ROWS)
		{
			int more = Math.min(recent.size(), RECENT_MAX) - RECENT_ROWS;
			JLabel toggle = new JLabel(recentAll ? "Show fewer" : "Show " + more + " more");
			toggle.setFont(FontManager.getRunescapeSmallFont());
			toggle.setForeground(LEVEL_COLOR);
			toggle.setAlignmentX(LEFT_ALIGNMENT);
			clickable(toggle, () ->
			{
				recentAll = !recentAll;
				rebuild();
			});
			content.add(toggle);
		}
		content.add(Box.createVerticalStrut(8));
	}

	/** Every recorded level-up of the skill that unlocked something, newest first. */
	private void buildTimeline(Skill skill)
	{
		if (!snapshot.isLoggedIn())
		{
			return;
		}
		List<Reached> timeline = reached(skill);
		if (!section("timeline", "Timeline", timeline.size()))
		{
			return;
		}
		if (timeline.isEmpty())
		{
			content.add(note("What your " + skill.getName() + " level-ups unlock shows here, with the date."));
			return;
		}
		long now = System.currentTimeMillis();
		for (Reached r : timeline)
		{
			JPanel row = new JPanel(new BorderLayout());
			row.setOpaque(false);
			row.setAlignmentX(LEFT_ALIGNMENT);
			JLabel level = new JLabel("Level " + levels(r.entry));
			level.setFont(FontManager.getRunescapeSmallFont());
			level.setForeground(snapshot.getPalette().getLevel());
			row.add(level, BorderLayout.WEST);
			row.add(when(r.entry, now), BorderLayout.EAST);
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
			content.add(row);
			content.add(Box.createVerticalStrut(3));
			addUnlocks(skill, r.unlocks);
		}
	}

	/** Checkboxes for the skill guide's sections, behind a toggle. */
	private void buildSections(Skill skill)
	{
		List<String> sections = repository.sections(skill);
		if (sections.isEmpty())
		{
			return;
		}
		long hidden = sections.stream().filter(sec -> snapshot.getHiddenSections().contains(Snapshot.sectionKey(skill, sec))).count();
		JLabel toggle = new JLabel((sectionsOpen ? "\u25BE " : "\u25B8 ") + "Sections" + (hidden > 0 ? " (" + hidden + " hidden)" : ""));
		toggle.setFont(FontManager.getRunescapeSmallFont());
		toggle.setForeground(hidden > 0 ? LEVEL_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		toggle.setAlignmentX(LEFT_ALIGNMENT);
		clickable(toggle, () ->
		{
			sectionsOpen = !sectionsOpen;
			rebuild();
		});
		content.add(toggle);
		content.add(Box.createVerticalStrut(4));
		if (!sectionsOpen)
		{
			return;
		}

		JPanel boxes = new JPanel(new GridLayout(0, 2));
		boxes.setOpaque(false);
		boxes.setAlignmentX(LEFT_ALIGNMENT);
		for (String section : sections)
		{
			String key = Snapshot.sectionKey(skill, section);
			JCheckBox box = new JCheckBox(section, !snapshot.getHiddenSections().contains(key));
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			box.setOpaque(false);
			box.setFocusPainted(false);
			box.addActionListener(e -> sectionToggle.accept(key, !box.isSelected()));
			boxes.add(box);
		}
		content.add(boxes);
		content.add(Box.createVerticalStrut(6));
	}

	/** Pictures first, in level order, then the entries only words can describe. */
	private void addUnlocks(Skill skill, List<Unlock> unlocks)
	{
		JPanel grid = new JPanel(new GridLayout(0, GRID_COLUMNS, 2, 2));
		grid.setOpaque(false);
		grid.setAlignmentX(LEFT_ALIGNMENT);
		List<Unlock> worded = new ArrayList<>();
		for (Unlock u : unlocks)
		{
			if (u.hasIcon())
			{
				grid.add(tile(skill, u, u.getWiki() == null ? null : () -> openWiki(u)));
			}
			else
			{
				worded.add(u);
			}
		}
		if (grid.getComponentCount() > 0)
		{
			// keep a short last row from stretching the tiles across the panel
			while (grid.getComponentCount() < GRID_COLUMNS)
			{
				grid.add(Box.createGlue());
			}
			content.add(grid);
			content.add(Box.createVerticalStrut(6));
		}
		for (Unlock u : worded)
		{
			content.add(entry(skill, u));
			content.add(Box.createVerticalStrut(3));
		}
	}

	/** Items to pin as goals, then unlocks of every skill whose text or note contains the query, grouped by skill. */
	private void buildSearch(String query)
	{
		String needle = query.toLowerCase(Locale.ROOT);
		int items = buildItemSearch(needle);
		int found = 0;
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			List<Unlock> matches = repository.all(skill).stream()
				.filter(u -> snapshot.getFilter().test(u) && (contains(u.getText(), needle) || contains(u.getNote(), needle)))
				.collect(Collectors.toList());
			if (matches.isEmpty())
			{
				continue;
			}
			JLabel header = new JLabel(skill.getName(), new ImageIcon(iconManager.getSkillImage(skill, true)), JLabel.LEFT);
			header.setFont(FontManager.getRunescapeBoldFont());
			header.setForeground(Color.WHITE);
			header.setAlignmentX(LEFT_ALIGNMENT);
			content.add(header);
			content.add(Box.createVerticalStrut(3));
			for (Unlock u : matches)
			{
				if (found++ == SEARCH_RESULTS)
				{
					content.add(note("Showing the first " + SEARCH_RESULTS + " matches; type more to narrow them down."));
					return;
				}
				content.add(entry(skill, u));
				content.add(Box.createVerticalStrut(3));
			}
			content.add(Box.createVerticalStrut(6));
		}
		if (found + items == 0)
		{
			content.add(note("No unlocks or items match \"" + query + "\" with these filters."));
		}
	}

	/** Items whose name contains the query, to pin as goals; how many were shown. */
	private int buildItemSearch(String needle)
	{
		List<Task> matches = items.get().stream()
			.filter(t -> contains(t.getName(), needle))
			.sorted(Comparator.comparing((Task t) -> !t.getName().toLowerCase(Locale.ROOT).startsWith(needle)).thenComparing(Task::getName))
			.limit(ITEM_RESULTS)
			.collect(Collectors.toList());
		if (matches.isEmpty())
		{
			return 0;
		}
		JLabel header = new JLabel("Item goals");
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setForeground(Color.WHITE);
		header.setAlignmentX(LEFT_ALIGNMENT);
		content.add(header);
		content.add(Box.createVerticalStrut(3));
		for (Task item : matches)
		{
			content.add(itemEntry(item));
			content.add(Box.createVerticalStrut(3));
		}
		content.add(Box.createVerticalStrut(6));
		return matches.size();
	}

	/** An item to pin: its picture and name, with what it needs in the tooltip. */
	private JComponent itemEntry(Task item)
	{
		boolean pinned = snapshot.getPinnedTasks().contains(item.getWiki());
		JLabel label = html(colored(escape(item.getName()), ColorScheme.LIGHT_GRAY_COLOR)
			+ (pinned ? colored(" (pinned)", snapshot.getPalette().getLevel()) : ""), TEXT_WIDTH - ITEM_ICON);
		AsyncBufferedImage image = itemManager.getImage(item.getItem());
		image.onLoaded(() -> SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(ImageUtil.resizeImage(image, ITEM_ICON, ITEM_ICON, true)))));
		label.setIcon(new ImageIcon(ImageUtil.resizeImage(image, ITEM_ICON, ITEM_ICON, true)));
		StringBuilder tip = new StringBuilder("<html>");
		item.getSkills().forEach((skill, level) -> tip.append(skill.getName()).append(' ').append(level).append(" to wield or wear<br>"));
		item.getMake().forEach((skill, level) -> tip.append(skill.getName()).append(' ').append(level).append(" to make ")
			.append(escape(item.getMadeFor().getOrDefault(skill, item.getName()))).append("<br>"));
		item.getQuests().forEach(q -> tip.append(escape(q)).append("<br>"));
		tip.append(pinned ? "Click to unpin" : "Click to pin its requirements above the chatbox").append("</html>");
		label.setToolTipText(tip.toString());
		clickable(label, () -> taskToggle.accept(item.getWiki()));
		label.setComponentPopupMenu(taskMenu(item.getWiki()));
		return label;
	}

	private static boolean contains(String text, String needle)
	{
		return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
	}

	/** XP progress from the last unlock level to the next. */
	private JComponent progress(Skill skill, int nextLevel)
	{
		return progress(skill, repository.previous(skill, snapshot.level(skill), snapshot.shown(skill)), nextLevel);
	}

	private JComponent progress(Skill skill, int fromLevel, int nextLevel)
	{
		int from = Experience.getXpForLevel(fromLevel);
		int to = Experience.getXpForLevel(nextLevel);
		ProgressBar bar = new ProgressBar();
		bar.setMaximumValue(Math.max(1, to - from));
		bar.setValue(Math.max(0, Math.min(to, snapshot.xp(skill)) - from));
		bar.setForeground(snapshot.getPalette().getProgress());
		// Swing paints translucent backgrounds badly
		bar.setBackground(new Color(Palette.rgb(snapshot.getPalette().getProgressBackground())));
		bar.setPreferredSize(new Dimension(0, 4));
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
		bar.setToolTipText(bar.getPercentage() + "% of the way to level " + nextLevel);
		return bar;
	}

	private UnlockTile tile(Skill skill, Unlock u, Runnable action)
	{
		UnlockTile tile = new UnlockTile(u.getLevel());
		tile.setOpaque(true);
		tile.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tile.setDimmed(snapshot.locked(u));
		tile.setLevelColor(levelColor(skill, u));
		tile.setToolTipText("<html>" + escape(u.getText()) + noteHtml(u) + "<br>" + levelDetail(skill, u) + "</html>");
		if (u.getItem() != null)
		{
			AsyncBufferedImage image = itemManager.getImage(u.getItem());
			tile.setImage(image);
			image.onLoaded(() -> SwingUtilities.invokeLater(() -> tile.setImage(image)));
		}
		else
		{
			sprite(u.getSprite(), tile::setImage);
		}
		if (action != null)
		{
			clickable(tile, action);
		}
		if (snapshot.isGoal(skill, u))
		{
			tile.setBorder(BorderFactory.createLineBorder(snapshot.getPalette().getLevel()));
			tile.setToolTipText(tile.getToolTipText().replace("</html>", "<br>" + colored("Goal", snapshot.getPalette().getLevel()) + "</html>"));
		}
		tile.setComponentPopupMenu(unlockMenu(skill, u));
		return tile;
	}

	private JComponent entry(Skill skill, Unlock u)
	{
		boolean reached = snapshot.isLoggedIn() && u.getLevel() <= snapshot.level(skill);
		JLabel label = html(levelText(skill, u) + colored(escape(u.getText()), textColor(u))
			+ (reached ? colored(" (unlocked)", ColorScheme.MEDIUM_GRAY_COLOR) : "")
			+ (snapshot.isGoal(skill, u) ? colored(" (goal)", snapshot.getPalette().getLevel()) : ""), TEXT_WIDTH);
		if (u.getWiki() != null)
		{
			label.setToolTipText("<html>" + levelDetail(skill, u) + noteHtml(u) + "<br>Open " + escape(u.getWiki()) + " on the OSRS Wiki</html>");
			clickable(label, () -> openWiki(u));
		}
		label.setComponentPopupMenu(unlockMenu(skill, u));
		return label;
	}

	private String levelDetail(Skill skill, Unlock u)
	{
		String text = "Level " + u.getLevel();
		if (snapshot.isLoggedIn())
		{
			long xp = xpToGo(skill, u.getLevel());
			text += xp == 0 ? " &middot; unlocked" : " &middot; " + NumberFormat.getIntegerInstance().format(xp) + " xp to go";
		}
		if (snapshot.reachedByBoost(skill, u))
		{
			text += "<br>" + colored("Reached with your boost", snapshot.getPalette().getBoost());
		}
		if (snapshot.locked(u))
		{
			text += "<br>" + colored("Quest not completed", LOCKED_COLOR);
		}
		return text;
	}

	private Color levelColor(Skill skill, Unlock u)
	{
		return snapshot.reachedByBoost(skill, u) ? snapshot.getPalette().getBoost() : snapshot.getPalette().getLevel();
	}

	private String levelText(Skill skill, Unlock u)
	{
		Color color = snapshot.locked(u) ? snapshot.getPalette().getLocked() : levelColor(skill, u);
		return "<b>" + colored(String.valueOf(u.getLevel()), color) + "</b>&nbsp; ";
	}

	private Color textColor(Unlock u)
	{
		return snapshot.locked(u) ? snapshot.getPalette().getLocked() : typeColor(u);
	}

	private static Color typeColor(Unlock u)
	{
		return u.getType() == Unlock.Type.QUEST ? QUEST_COLOR
			: u.getType() == Unlock.Type.DIARY ? DIARY_COLOR
			: ColorScheme.LIGHT_GRAY_COLOR;
	}

	private static String noteHtml(Unlock u)
	{
		return u.getNote() == null ? "" : "<br>" + colored(escape(u.getNote()).replace("\n", "<br>"), ColorScheme.LIGHT_GRAY_COLOR);
	}

	/** Right-click menu of an unlock: set or drop it as a goal, pin it, set an amount goal, open its wiki page. */
	private JPopupMenu unlockMenu(Skill skill, Unlock u)
	{
		JPopupMenu menu = new JPopupMenu();
		boolean goal = snapshot.isGoal(skill, u);
		boolean reached = snapshot.isLoggedIn() && u.getLevel() <= snapshot.level(skill);
		if (goal || !reached)
		{
			menu.add(menuItem(goal ? "Remove goal" : "Set as goal", () -> goalToggle.accept(Goal.of(skill, u))));
		}
		if (u.getType() != Unlock.Type.UNLOCK && pinnable.test(u.getWiki()))
		{
			menu.add(pinItem(u.getWiki()));
		}
		else if (u.getType() == Unlock.Type.UNLOCK && pinnable.test(TaskRepository.unlockKey(skill, u)))
		{
			menu.add(pinItem(TaskRepository.unlockKey(skill, u)));
		}
		if (u.getType() == Unlock.Type.UNLOCK && u.getItem() != null)
		{
			menu.add(menuItem("Set amount goal...", () -> amountGoal.accept(skill, u.getItem())));
		}
		if (u.getWiki() != null)
		{
			menu.add(menuItem("Open on the OSRS Wiki", () -> openWiki(u)));
		}
		return menu.getComponentCount() == 0 ? null : menu;
	}

	/** Right-click menu of an Almost there row: pin its requirements, open its wiki page. */
	private JPopupMenu taskMenu(String title)
	{
		JPopupMenu menu = new JPopupMenu();
		if (pinnable.test(title))
		{
			menu.add(pinItem(title));
		}
		menu.add(menuItem("Open on the OSRS Wiki", () -> openWiki(title)));
		return menu;
	}

	private JMenuItem pinItem(String title)
	{
		return menuItem(snapshot.getPinnedTasks().contains(title) ? "Unpin requirements" : "Pin requirements",
			() -> taskToggle.accept(title));
	}

	private static JMenuItem menuItem(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(e -> action.run());
		return item;
	}

	static void openWiki(Unlock u)
	{
		openWiki(u.getWiki());
	}

	/** Goes straight to the page when the title exists, else to search results. */
	static void openWiki(String title)
	{
		LinkBrowser.browse(WIKI_URL + "Special:Search?go=Go&search=" + URLEncoder.encode(title, StandardCharsets.UTF_8));
	}

	private long xpToGo(Skill skill, int level)
	{
		return Math.max(0, Experience.getXpForLevel(level) - snapshot.xp(skill));
	}

	static String shortXp(long xp)
	{
		if (xp >= 1_000_000)
		{
			return String.format("%.1fm", xp / 1_000_000.0);
		}
		if (xp >= 10_000)
		{
			return (xp / 1000) + "k";
		}
		return NumberFormat.getIntegerInstance().format(xp);
	}

	private static JPanel card()
	{
		JPanel card = new JPanel(new BorderLayout(6, 0));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		card.setAlignmentX(LEFT_ALIGNMENT);
		return card;
	}

	private void addCard(JPanel card)
	{
		content.add(card);
		content.add(Box.createVerticalStrut(4));
	}

	private static JLabel note(String text)
	{
		JLabel label = html(escape(text), TEXT_WIDTH);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JLabel html(String body, int width)
	{
		JLabel label = new JLabel("<html><body style='width:" + width + "px'>" + body + "</body></html>");
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static String colored(String text, Color color)
	{
		return "<font color='#" + ColorUtil.colorToHexCode(color) + "'>" + text + "</font>";
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static void clickable(JComponent component, Runnable action)
	{
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		Color normal = component.getBackground();
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// right-click is for the goal menu
				if (SwingUtilities.isLeftMouseButton(e))
				{
					action.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				component.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				component.setBackground(normal);
			}
		});
	}
}
