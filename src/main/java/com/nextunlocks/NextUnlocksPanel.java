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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
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
import net.runelite.client.util.LinkBrowser;

class NextUnlocksPanel extends PluginPanel
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final int TEXT_WIDTH = PANEL_WIDTH - 40;
	private static final Color QUEST_COLOR = new Color(255, 152, 31);
	private static final Color DIARY_COLOR = new Color(110, 200, 110);
	private static final Color LEVEL_COLOR = new Color(255, 255, 0);
	private static final int GRID_COLUMNS = 5;
	private static final int PREVIEW_TILES = 4;
	private static final int SEARCH_RESULTS = 60;
	private static final Color LOCKED_COLOR = new Color(0xe05a5a);
	private static final int ALMOST_THERE_ROWS = 5;

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
	private final IconTextField search = new IconTextField();
	private final JComboBox<Skill> skillBox = new JComboBox<>();
	private final JPanel content = new JPanel();

	private Snapshot snapshot = Snapshot.EMPTY;
	private boolean stale = true;
	private boolean sectionsOpen;
	private boolean almostThereOpen = true;

	NextUnlocksPanel(UnlockRepository repository, SkillIconManager iconManager, ItemManager itemManager, SpriteManager spriteManager,
		BiConsumer<String, Boolean> sectionToggle, Consumer<Goal> goalToggle)
	{
		this.repository = repository;
		this.iconManager = iconManager;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.sectionToggle = sectionToggle;
		this.goalToggle = goalToggle;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		skillBox.addItem(null);
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			skillBox.addItem(skill);
		}
		skillBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
			{
				super.getListCellRendererComponent(list, value, index, selected, focus);
				Skill skill = (Skill) value;
				setText(skill == null ? "All skills" : skill.getName());
				setIcon(skill == null ? null : new ImageIcon(iconManager.getSkillImage(skill, true)));
				return this;
			}
		});
		skillBox.addActionListener(e -> rebuild());

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

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.add(search, BorderLayout.NORTH);
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

		Skill selected = (Skill) skillBox.getSelectedItem();
		String query = search.getText().trim();
		if (!query.isEmpty())
		{
			buildSearch(query);
		}
		else if (selected == null)
		{
			buildOverview();
		}
		else
		{
			buildSkill(selected);
		}

		content.add(Box.createVerticalStrut(10));
		String wiki = "OSRS Wiki (CC BY-NC-SA 3.0)" + (repository.getFetched().isEmpty() ? "" : ", " + repository.getFetched());
		content.add(note(repository.isSkillGuide() ? "Unlocks from the in-game skill guide; quests and diaries from the " + wiki : "Data from the " + wiki));
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
		if (!snapshot.getGoals().isEmpty() || almostThereShown())
		{
			content.add(heading("Next unlocks"));
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

	/** A card per goal, with the xp left to it. */
	private void buildGoals()
	{
		List<Goal> goals = snapshot.getGoals();
		if (goals.isEmpty())
		{
			return;
		}
		content.add(heading("Goals"));
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
		content.add(Box.createVerticalStrut(6));
	}

	private boolean almostThereShown()
	{
		AlmostThere almost = AlmostThere.of(repository, snapshot);
		return snapshot.isAlmostThere() && (!almost.getOneLevelShort().isEmpty() || !almost.getQuestWaits().isEmpty());
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
		if (shorts.isEmpty() && waits.isEmpty())
		{
			return;
		}

		JLabel toggle = heading((almostThereOpen ? "\u25BE " : "\u25B8 ") + "Almost there (" + (shorts.size() + waits.size()) + ")");
		clickable(toggle, () ->
		{
			almostThereOpen = !almostThereOpen;
			rebuild();
		});
		content.add(toggle);
		if (!almostThereOpen)
		{
			content.add(Box.createVerticalStrut(4));
			return;
		}

		for (AlmostThere.OneLevelShort o : shorts.subList(0, Math.min(shorts.size(), ALMOST_THERE_ROWS)))
		{
			Color color = o.getType() == Unlock.Type.QUEST ? QUEST_COLOR : DIARY_COLOR;
			String xp = snapshot.isLoggedIn() ? " &middot; " + shortXp(o.getXpToGo()) + " xp" : "";
			JLabel row = html(colored(escape(o.getTitle()), color) + "<br>"
				+ colored("Needs " + o.getSkill().getName() + " " + o.getLevel() + xp, ColorScheme.LIGHT_GRAY_COLOR), TEXT_WIDTH - 20);
			row.setIcon(new ImageIcon(iconManager.getSkillImage(o.getSkill(), true)));
			row.setIconTextGap(6);
			row.setToolTipText("<html>Every other skill level for this " + (o.getType() == Unlock.Type.QUEST ? "quest" : "diary") + " is met"
				+ (o.getType() == Unlock.Type.DIARY ? "<br>Diary completion isn't checked" : "")
				+ "<br>Open " + escape(o.getTitle()) + " on the OSRS Wiki</html>");
			clickable(row, () -> openWiki(o.getTitle()));
			content.add(row);
			content.add(Box.createVerticalStrut(4));
		}
		if (shorts.size() > ALMOST_THERE_ROWS)
		{
			content.add(note("+" + (shorts.size() - ALMOST_THERE_ROWS) + " more one level short"));
			content.add(Box.createVerticalStrut(4));
		}

		for (AlmostThere.QuestWait w : waits.subList(0, Math.min(waits.size(), ALMOST_THERE_ROWS)))
		{
			int n = w.getUnlocks().size();
			JLabel row = html(colored(escape(w.getQuest()), QUEST_COLOR) + "<br>"
				+ colored("Unlocks " + n + (n == 1 ? " thing" : " things") + " you have the level for", ColorScheme.LIGHT_GRAY_COLOR), TEXT_WIDTH - 20);
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
			tip.append("Open ").append(escape(w.getQuest())).append(" on the OSRS Wiki</html>");
			row.setToolTipText(tip.toString());
			clickable(row, () -> openWiki(w.getQuest()));
			content.add(row);
			content.add(Box.createVerticalStrut(4));
		}
		if (waits.size() > ALMOST_THERE_ROWS)
		{
			content.add(note("+" + (waits.size() - ALMOST_THERE_ROWS) + " more quests holding unlocks back"));
			content.add(Box.createVerticalStrut(4));
		}
		content.add(Box.createVerticalStrut(6));
	}

	private static JLabel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(LEFT_ALIGNMENT);
		label.setBorder(new EmptyBorder(0, 0, 4, 0));
		return label;
	}

	private void buildSkill(Skill skill)
	{
		int level = snapshot.level(skill);
		JLabel header = new JLabel(skill.getName() + " - level " + level, new ImageIcon(iconManager.getSkillImage(skill, false)), JLabel.LEFT);
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setForeground(Color.WHITE);
		header.setAlignmentX(LEFT_ALIGNMENT);
		content.add(header);
		content.add(Box.createVerticalStrut(6));
		buildSections(skill);

		NavigableMap<Integer, List<Unlock>> upcoming = repository.upcoming(skill, level, snapshot.upcoming(skill), snapshot.getLevelsAhead());
		if (snapshot.isLoggedIn() && !upcoming.isEmpty())
		{
			JComponent progress = progress(skill, upcoming.firstKey());
			progress.setAlignmentX(LEFT_ALIGNMENT);
			content.add(progress);
			content.add(Box.createVerticalStrut(6));
		}
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

		if (upcoming.isEmpty())
		{
			content.add(note("Nothing left to unlock with these filters."));
			return;
		}
		addUnlocks(skill, upcoming.values().stream().flatMap(List::stream).collect(Collectors.toList()));
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

	/** Unlocks of every skill whose text or note contains the query, grouped by skill. */
	private void buildSearch(String query)
	{
		String needle = query.toLowerCase(Locale.ROOT);
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
		if (found == 0)
		{
			content.add(note("No unlocks match \"" + query + "\" with these filters."));
		}
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
			int id = u.getSprite();
			BufferedImage cached = sprites.get(id);
			if (cached != null)
			{
				tile.setImage(cached);
			}
			else
			{
				spriteManager.getSpriteAsync(id, 0, image -> SwingUtilities.invokeLater(() ->
				{
					sprites.put(id, image);
					tile.setImage(image);
				}));
			}
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

	/** Right-click menu of an unlock: set or drop it as a goal, open its wiki page. */
	private JPopupMenu unlockMenu(Skill skill, Unlock u)
	{
		JPopupMenu menu = new JPopupMenu();
		boolean goal = snapshot.isGoal(skill, u);
		boolean reached = snapshot.isLoggedIn() && u.getLevel() <= snapshot.level(skill);
		if (goal || !reached)
		{
			menu.add(menuItem(goal ? "Remove goal" : "Set as goal", () -> goalToggle.accept(Goal.of(skill, u))));
		}
		if (u.getWiki() != null)
		{
			menu.add(menuItem("Open on the OSRS Wiki", () -> openWiki(u)));
		}
		return menu.getComponentCount() == 0 ? null : menu;
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
