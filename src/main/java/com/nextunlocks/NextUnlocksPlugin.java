package com.nextunlocks;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Quest;
import net.runelite.api.Player;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Next Unlocks",
	description = "Shows what your next levels unlock in every skill",
	tags = {"skill", "level", "unlock", "requirement", "goal", "skill guide", "progress", "boost"}
)
public class NextUnlocksPlugin extends Plugin
{
	private static final int TOOLTIP_ENTRIES = 3;
	private static final int TOOLTIP_ICONS = 10;
	private static final int TOOLTIP_ICON_COLUMNS = 5;
	private static final int ICON_GAP = 2;
	private static final int ICON_TOP_PADDING = 3;
	/** PanelComponent's default left plus right border. */
	private static final int PANEL_BORDER = 10;
	private static final String PIN = "Pin";
	private static final String UNPIN = "Unpin";
	private static final String REMOVE_GOAL = "Remove goal";
	private static final String SET_GOAL = "Set goal";
	/** Upcoming unlock levels offered in the Set goal menu. */
	private static final int GOAL_CHOICES = 3;
	private static final int MESSAGE_ENTRIES = 3;
	/** Panel refresh interval for xp-only changes, in game ticks. */
	private static final int PANEL_REFRESH_TICKS = 5;
	/** How often quest states are checked again, in game ticks. */
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final String SECTION_SEPARATOR = ";";
	private static final String TASK_SEPARATOR = "\n";
	/** Requirement lines on a quest or diary card before the rest are counted as "+N more". */
	private static final int TASK_LINES = 10;
	private static final Map<Integer, Quest> QUESTS = new HashMap<>();
	private static final Map<String, Quest> QUESTS_BY_NAME = new HashMap<>();

	static
	{
		for (Quest quest : Quest.values())
		{
			QUESTS.put(quest.getId(), quest);
			QUESTS_BY_NAME.put(quest.getName(), quest);
		}
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private NextUnlocksConfig config;

	@Inject
	private Gson gson;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private SkillTooltipOverlay tooltipOverlay;

	@Inject
	private RuneLiteConfig runeLiteConfig;

	@Inject
	private PinnedOverlay pinnedOverlay;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Notifier notifier;

	@Inject
	private InfoBoxManager infoBoxManager;

	private final UnlockRepository repository = new UnlockRepository();
	/** Real levels as last seen; client thread only. Empty while logged out. */
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> boosts = new EnumMap<>(Skill.class);
	private final Map<Skill, TooltipBox> tooltipCache = new ConcurrentHashMap<>();
	/** Pinned cards, built apart from the hover tooltips since they also show the skill and level. */
	private final Map<Skill, TooltipBox> pinnedCache = new ConcurrentHashMap<>();

	/** Skills whose tooltip is pinned above the chatbox, in pin order. Client thread. */
	@Getter
	private final Set<Skill> pinned = new LinkedHashSet<>();
	/** Wiki titles of the quests and diaries pinned above the chatbox, in pin order. Client thread. */
	@Getter
	private final Set<String> pinnedTasks = new LinkedHashSet<>();
	private final TaskRepository tasks = new TaskRepository();
	private final Map<String, TooltipBox> taskCache = new ConcurrentHashMap<>();
	/** States of the pinned quests and the quests they need, by wiki title; client thread. */
	private Map<String, QuestState> questStates = Collections.emptyMap();
	/** Replaced whole, for the panel's menus. */
	private volatile Set<String> pinnedTaskTitles = Collections.emptySet();

	private NextUnlocksPanel panel;
	private SkillGuide skillGuide;
	private NavigationButton navButton;
	private boolean membersWorld;
	private boolean panelDirty;
	private int ticksSincePanelRefresh;
	/** Required quests not finished yet; client thread. */
	private Set<Integer> unfinishedQuests = Collections.emptySet();
	private boolean questsDirty;
	private int ticksSinceQuestRefresh;
	/** Replaced whole; written from the panel's thread too. */
	private volatile Set<String> hiddenSections = Collections.emptySet();
	/** Replaced whole; written from the panel's thread too. */
	private volatile List<Goal> goals = Collections.emptyList();
	/** Required quests not finished yet, whatever the grey-out setting; client thread. */
	private Set<Integer> questsToDo = Collections.emptySet();
	/** Names of finished quests that quest entries name; client thread. */
	private Set<String> finishedQuests = Collections.emptySet();
	/** Client thread. */
	private final List<GoalInfoBox> goalBoxes = new ArrayList<>();

	@Provides
	NextUnlocksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NextUnlocksConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		repository.load(gson);
		tasks.load(gson, repository.diaries());

		loadHiddenSections();
		goals = Goal.parse(config.goals());
		panel = new NextUnlocksPanel(repository, skillIconManager, itemManager, spriteManager, this::setSectionHidden, this::toggleGoal,
			title -> tasks.get(title) != null, this::toggleTaskLater);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Next Unlocks")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(tooltipOverlay);
		overlayManager.add(pinnedOverlay);
		skillGuide = new SkillGuide(client, clientThread, config, repository, this::snapshot);

		clientThread.invokeLater(() ->
		{
			loadPins();
			loadSkillGuide();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				membersWorld = client.getWorldType().contains(WorldType.MEMBERS);
				for (Skill skill : Skills.STATS_TAB.keySet())
				{
					levels.put(skill, client.getRealSkillLevel(skill));
				}
				questsDirty = true;
			}
			syncGoalBoxes();
			pushToPanel();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(tooltipOverlay);
		overlayManager.remove(pinnedOverlay);
		goalBoxes.forEach(infoBoxManager::removeInfoBox);
		goalBoxes.clear();
		skillGuide.shutDown();
		skillGuide = null;
		pinned.clear();
		pinnedTasks.clear();
		questStates = Collections.emptyMap();
		clientToolbar.removeNavigation(navButton);
		levels.clear();
		boosts.clear();
		clearTooltips();
		unfinishedQuests = Collections.emptySet();
		questsToDo = Collections.emptySet();
		finishedQuests = Collections.emptySet();
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				loadSkillGuide();
				// stats arrive again after login; an empty map keeps those from looking like level-ups
				levels.clear();
				boosts.clear();
				clearTooltips();
				unfinishedQuests = Collections.emptySet();
				questsToDo = Collections.emptySet();
				finishedQuests = Collections.emptySet();
				questStates = Collections.emptyMap();
				pushToPanel();
				break;
			case LOGGED_IN:
				loadSkillGuide();
				membersWorld = client.getWorldType().contains(WorldType.MEMBERS);
				clearTooltips();
				panelDirty = true;
				questsDirty = true;
				break;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		if (!Skills.STATS_TAB.containsKey(skill))
		{
			return;
		}

		int level = event.getLevel();
		Integer previous = levels.put(skill, level);
		Integer previousBoost = boosts.put(skill, event.getBoostedLevel());
		if (!Objects.equals(previous, level) || !Objects.equals(previousBoost, event.getBoostedLevel()))
		{
			// levels and combat level are on the quest and diary cards
			taskCache.clear();
		}
		for (Map<Skill, TooltipBox> cache : Arrays.asList(tooltipCache, pinnedCache))
		{
			TooltipBox cached = cache.get(skill);
			if (cached != null && Objects.equals(previous, level) && Objects.equals(previousBoost, event.getBoostedLevel()))
			{
				// only xp changed: update the numbers in place, rebuilding would make a pinned card jump
				cached.updateXp(event.getXp());
			}
			else
			{
				cache.remove(skill);
			}
		}
		if (previous == null || level > previous)
		{
			// also on login, for goals reached elsewhere
			reachGoals(skill, level);
		}
		if (previous != null && level > previous)
		{
			if (config.levelUpMessage())
			{
				announce(skill, previous, level);
			}
			pushToPanel();
			skillGuide.refresh();
		}
		else
		{
			panelDirty = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() == GameState.LOGGED_IN && (questsDirty || ++ticksSinceQuestRefresh >= QUEST_REFRESH_TICKS))
		{
			refreshQuests();
		}
		if (panelDirty && ++ticksSincePanelRefresh >= PANEL_REFRESH_TICKS)
		{
			pushToPanel();
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		skillGuide.onScriptPreFired(event);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		skillGuide.onScriptPostFired(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (NextUnlocksConfig.GROUP.equals(event.getGroup()))
		{
			if ("hiddenSections".equals(event.getKey()))
			{
				loadHiddenSections();
			}
			if ("goals".equals(event.getKey()))
			{
				goals = Goal.parse(config.goals());
			}
			clearTooltips();
			clientThread.invokeLater(() ->
			{
				if ("greyOutLocked".equals(event.getKey()) || "almostThere".equals(event.getKey()))
				{
					questsDirty = true;
				}
				syncGoalBoxes();
				pushToPanel();
				skillGuide.refresh();
			});
		}
	}

	Predicate<Unlock> filter()
	{
		NextUnlocksConfig.MembersContent membersContent = config.membersContent();
		boolean members = membersContent == NextUnlocksConfig.MembersContent.ALWAYS
			|| (membersContent == NextUnlocksConfig.MembersContent.MATCH_WORLD && membersWorld);
		boolean quests = config.showQuests();
		boolean diaries = config.showDiaries();
		return u -> (members || !u.isMembers())
			&& (quests || u.getType() != Unlock.Type.QUEST)
			&& (diaries || u.getType() != Unlock.Type.DIARY);
	}

	/** Skills-tab tooltip for a skill, or null when logged out or nothing is left. Client thread. */
	LayoutableRenderableEntity tooltip(Skill skill)
	{
		return tooltip(skill, false);
	}

	/** Pinned card for a skill: the tooltip, with the skill, level and goal as turned on. Client thread. */
	LayoutableRenderableEntity pinnedTooltip(Skill skill)
	{
		return tooltip(skill, true);
	}

	private void clearTooltips()
	{
		tooltipCache.clear();
		pinnedCache.clear();
		taskCache.clear();
	}

	private LayoutableRenderableEntity tooltip(Skill skill, boolean pinnedCard)
	{
		Integer level = levels.get(skill);
		if (level == null)
		{
			return null;
		}
		boolean header = pinnedCard && config.pinnedLevel();
		Goal goal = pinnedCard && config.pinnedGoal() ? nearestGoal(skill) : null;
		Map<Skill, TooltipBox> cache = pinnedCard ? pinnedCache : tooltipCache;
		TooltipBox cached = cache.get(skill);
		if (cached != null)
		{
			return cached;
		}
		Snapshot snapshot = snapshot();
		Palette palette = snapshot.getPalette();
		NavigableMap<Integer, List<Unlock>> next = repository.upcoming(skill, level, snapshot.upcoming(skill), 1);
		if (next.isEmpty())
		{
			return null;
		}
		int nextLevel = next.firstKey();
		List<Unlock> unlocks = next.firstEntry().getValue();
		int xp = client.getSkillExperience(skill);
		int xpToGo = Math.max(0, Experience.getXpForLevel(nextLevel) - xp);

		int textSize = config.tooltipTextSize();
		Font regular = FontManager.getRunescapeFont();
		Font font = textSize == regular.getSize() ? regular : regular.deriveFont((float) textSize);
		double scale = config.tooltipIconSize() / 100.0;
		boolean iconsOnly = config.tooltipIconsOnly() && unlocks.stream().anyMatch(Unlock::hasIcon);
		NumberFormat number = NumberFormat.getIntegerInstance();

		PanelComponent tooltip = new PanelComponent();
		List<LayoutableRenderableEntity> lines = tooltip.getChildren();
		List<IntConsumer> updates = new ArrayList<>();
		if (header)
		{
			// boosted or drained levels read like the skills tab, 47/45
			int boosted = client.getBoostedSkillLevel(skill);
			lines.add(LineComponent.builder()
				.left(skill.getName())
				.leftColor(palette.getNext())
				.leftFont(font)
				.right(boosted == level ? "Level " + level : "Level " + boosted + "/" + level)
				.rightColor(boosted > level ? Color.GREEN : boosted < level ? Color.RED : Color.WHITE)
				.rightFont(font)
				.build());
		}
		if (config.tooltipXp())
		{
			// the lines of the game's own tooltip, which onBeforeRender hides
			// the header already names the skill
			LineComponent current = line(font, header ? "XP:" : skill.getName() + " XP:", number.format(xp));
			lines.add(current);
			updates.add(x -> current.setRight(number.format(x)));
			if (level < Experience.MAX_REAL_LEVEL)
			{
				int nextXp = Experience.getXpForLevel(level + 1);
				lines.add(line(font, "Next level at:", number.format(nextXp)));
				LineComponent remaining = line(font, "Remaining XP:", number.format(Math.max(0, nextXp - xp)));
				lines.add(remaining);
				updates.add(x -> remaining.setRight(number.format(Math.max(0, nextXp - x))));
			}
		}
		if (!iconsOnly)
		{
			LineComponent nextUnlock = LineComponent.builder()
				.left("Next unlock: " + nextLevel)
				.leftColor(palette.getNext())
				.leftFont(font)
				.right(number.format(xpToGo) + " xp")
				.rightFont(font)
				.build();
			lines.add(nextUnlock);
			int nextUnlockXp = Experience.getXpForLevel(nextLevel);
			updates.add(x -> nextUnlock.setRight(number.format(Math.max(0, nextUnlockXp - x)) + " xp"));
		}
		if (config.tooltipProgress())
		{
			ProgressBarComponent progress = new ProgressBarComponent();
			progress.setMinimum(Experience.getXpForLevel(repository.previous(skill, level, snapshot.shown(skill))));
			progress.setMaximum(Experience.getXpForLevel(nextLevel));
			progress.setValue(xp);
			progress.setForegroundColor(palette.getProgress());
			progress.setBackgroundColor(palette.getProgressBackground());
			lines.add(progress);
			updates.add(progress::setValue);
		}
		int iconsAt = lines.size();

		PanelComponent icons = new PanelComponent();
		icons.setOrientation(ComponentOrientation.HORIZONTAL);
		icons.setWrap(true);
		icons.setBackgroundColor(null);
		icons.setBorder(new Rectangle());
		icons.setGap(new Point(ICON_GAP, ICON_GAP));
		boolean complete = true;
		int texts = 0;
		int hidden = 0;
		for (Unlock u : unlocks)
		{
			if (u.hasIcon() && icons.getChildren().size() < TOOLTIP_ICONS)
			{
				BufferedImage image = u.getItem() != null ? itemManager.getImage(u.getItem()) : spriteManager.getSprite(u.getSprite(), 0);
				// sprites can be missing before the cache is ready; don't keep a tooltip without them
				complete &= image != null;
				icons.getChildren().add(new UnlockTileComponent(image, u.getText(), nextLevel, palette.getLevel(), snapshot.locked(u), scale));
			}
			else if (!iconsOnly && !u.hasIcon() && texts < TOOLTIP_ENTRIES)
			{
				texts++;
				lines.add(LineComponent.builder().left(u.getText()).leftColor(snapshot.locked(u) ? palette.getLocked() : Color.WHITE).leftFont(font).build());
			}
			else
			{
				hidden++;
			}
		}
		if (!icons.getChildren().isEmpty())
		{
			if (iconsAt > 0)
			{
				// a little space under the text above (border is left, top, right, bottom)
				icons.setBorder(new Rectangle(0, ICON_TOP_PADDING, 0, 0));
			}
			lines.add(iconsAt, icons);
		}
		if (hidden > 0 && !iconsOnly)
		{
			lines.add(LineComponent.builder().left("+" + hidden + " more").leftColor(Color.LIGHT_GRAY).leftFont(font).build());
		}
		if (goal != null)
		{
			int goalXp = Experience.getXpForLevel(goal.getLevel());
			LineComponent goalLine = LineComponent.builder()
				.left("Goal: " + goal.getLevel())
				.leftColor(Skills.color(skill))
				.leftFont(font)
				.right(number.format(Math.max(0, goalXp - xp)) + " xp")
				.rightFont(font)
				.build();
			lines.add(goalLine);
			updates.add(x -> goalLine.setRight(number.format(Math.max(0, goalXp - x)) + " xp"));
			// the whole way from 0 xp, as the start of a goal isn't kept
			ProgressBarComponent goalBar = new ProgressBarComponent();
			goalBar.setMinimum(0);
			goalBar.setMaximum(goalXp);
			goalBar.setValue(xp);
			goalBar.setForegroundColor(Skills.color(skill));
			goalBar.setBackgroundColor(palette.getProgressBackground());
			lines.add(goalBar);
			updates.add(goalBar::setValue);
		}

		int columns = iconsOnly ? Math.min(TOOLTIP_ICON_COLUMNS, icons.getChildren().size()) : TOOLTIP_ICON_COLUMNS;
		int width = columns * (UnlockTileComponent.size(UnlockTile.WIDTH, scale) + ICON_GAP) - ICON_GAP + PANEL_BORDER;
		if (!iconsOnly || config.tooltipXp())
		{
			// room for the text, which grows with its size
			width = Math.max(width, ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize());
		}
		tooltip.setPreferredSize(new Dimension(width, 0));

		Color base = runeLiteConfig.overlayBackgroundColor();
		Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255 * config.tooltipOpacity() / 100);
		TooltipBox box = new TooltipBox(tooltip, background, config.tooltipOutline() ? palette.getOutline() : null, updates);
		if (complete)
		{
			cache.put(skill, box);
		}
		return box;
	}

	private static LineComponent line(Font font, String left, String right)
	{
		return LineComponent.builder().left(left).leftFont(font).right(right).rightFont(font).build();
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (!config.skillTooltip() || !config.tooltipXp())
		{
			return;
		}
		Skill skill = tooltipOverlay.hoveredSkill();
		if (skill != null && tooltip(skill) != null)
		{
			Widget gameTooltip = client.getWidget(InterfaceID.Stats.TOOLTIP);
			if (gameTooltip != null)
			{
				gameTooltip.setHidden(true);
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry entry = event.getMenuEntry();
		if (entry.getParam1() == InterfaceID.Questlist.LIST)
		{
			Task task = tasks.byName(questListName(entry));
			if (task != null && !hasPinEntry())
			{
				addTaskPinEntry(task);
			}
			return;
		}
		Skill skill = Skills.forStatsWidget(entry.getParam1());
		if (skill == null || hasPinEntry())
		{
			return;
		}
		addPinEntry(skill);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		Skill skill = pinnedOverlay.skillAt(mouse.getX(), mouse.getY());
		if (skill != null && !hasPinEntry())
		{
			addPinEntry(skill);
		}
		String task = pinnedOverlay.taskAt(mouse.getX(), mouse.getY());
		if (task != null && !hasPinEntry())
		{
			addTaskPinEntry(tasks.get(task));
		}
	}

	private boolean hasPinEntry()
	{
		for (MenuEntry entry : client.getMenu().getMenuEntries())
		{
			if (entry.getType() == MenuAction.RUNELITE && (PIN.equals(entry.getOption()) || UNPIN.equals(entry.getOption())))
			{
				return true;
			}
		}
		return false;
	}

	/** Adds Pin or Unpin and Set goal just above Cancel, so they never become the left-click action. */
	private void addPinEntry(Skill skill)
	{
		addGoalEntry(skill);
		client.getMenu().createMenuEntry(1)
			.setOption(pinned.contains(skill) ? UNPIN : PIN)
			.setTarget(ColorUtil.wrapWithColorTag(skill.getName() + " unlocks", config.colorNext()))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> togglePin(skill));
	}

	/** The quest a quest list entry is for, from the list's text since the entry's target can be empty. */
	private String questListName(MenuEntry entry)
	{
		Widget list = client.getWidget(entry.getParam1());
		Widget child = list == null ? null : list.getChild(entry.getParam0());
		String text = child != null && child.getText() != null && !child.getText().isEmpty() ? child.getText() : entry.getTarget();
		return Text.removeTags(text).trim();
	}

	/** Pin or Unpin for a quest or diary card, just above Cancel. */
	private void addTaskPinEntry(Task task)
	{
		if (task == null)
		{
			return;
		}
		client.getMenu().createMenuEntry(1)
			.setOption(pinnedTasks.contains(task.getWiki()) ? UNPIN : PIN)
			.setTarget(ColorUtil.wrapWithColorTag(task.getName() + " requirements", config.colorNext()))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> toggleTask(task.getWiki()));
	}

	/** Pins or unpins a quest or diary card. Client thread. */
	private void toggleTask(String wiki)
	{
		if (!pinnedTasks.remove(wiki))
		{
			pinnedTasks.add(wiki);
			questsDirty = true;
		}
		saveTaskPins();
		pushToPanel();
	}

	/** {@link #toggleTask} from the panel's thread. */
	private void toggleTaskLater(String wiki)
	{
		clientThread.invokeLater(() -> toggleTask(wiki));
	}

	private void saveTaskPins()
	{
		pinnedTaskTitles = Collections.unmodifiableSet(new HashSet<>(pinnedTasks));
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "pinnedTasks", String.join(TASK_SEPARATOR, pinnedTasks));
	}

	/** Set goal, with the next few unlocks, the next level and 99 to pick from, and the skill's goals to remove. */
	private void addGoalEntry(Skill skill)
	{
		Integer level = levels.get(skill);
		if (level == null)
		{
			return;
		}
		List<Goal> current = goals.stream().filter(g -> g.getSkill() == skill).collect(Collectors.toList());
		List<Goal> choices = new ArrayList<>();
		repository.upcoming(skill, level, snapshot().upcoming(skill), GOAL_CHOICES).forEach((lvl, unlocks) ->
			choices.add(Goal.of(skill, unlocks.get(0))));
		if (level < Experience.MAX_REAL_LEVEL)
		{
			choices.add(Goal.level(skill, level + 1));
		}
		if (level + 1 < Experience.MAX_REAL_LEVEL)
		{
			choices.add(Goal.level(skill, Experience.MAX_REAL_LEVEL));
		}
		choices.removeAll(current);
		if (choices.isEmpty() && current.isEmpty())
		{
			return;
		}

		MenuEntry parent = client.getMenu().createMenuEntry(1)
			.setOption(SET_GOAL)
			.setTarget(ColorUtil.wrapWithColorTag(skill.getName(), Skills.color(skill)))
			.setType(MenuAction.RUNELITE);
		Menu sub = parent.createSubMenu();
		// each entry goes on top, so the list is built from the bottom up
		for (Goal goal : current)
		{
			boolean levelOnly = goal.equals(Goal.level(skill, goal.getLevel()));
			goalChoice(sub, REMOVE_GOAL, levelOnly ? goal.getText() : goal.getLevel() + " " + goal.getText(), goal);
		}
		for (int i = choices.size() - 1; i >= 0; i--)
		{
			Goal goal = choices.get(i);
			boolean levelOnly = goal.equals(Goal.level(skill, goal.getLevel()));
			goalChoice(sub, "Level " + goal.getLevel(), levelOnly ? "" : goal.getText(), goal);
		}
	}

	private void goalChoice(Menu menu, String option, String text, Goal goal)
	{
		menu.createMenuEntry(-1)
			.setOption(option)
			.setTarget(ColorUtil.wrapWithColorTag(text, config.colorNext()))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> toggleGoal(goal));
	}

	/** The skill's goal with the lowest level, or null. */
	private Goal nearestGoal(Skill skill)
	{
		return goals.stream()
			.filter(g -> g.getSkill() == skill)
			.min(Comparator.comparingInt(Goal::getLevel))
			.orElse(null);
	}

	private void togglePin(Skill skill)
	{
		if (!pinned.remove(skill))
		{
			pinned.add(skill);
		}
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "pinnedSkills",
			pinned.stream().map(Skill::name).collect(Collectors.joining(",")));
	}

	private void loadPins()
	{
		pinned.clear();
		for (String name : config.pinnedSkills().split(","))
		{
			try
			{
				pinned.add(Skill.valueOf(name.trim()));
			}
			catch (IllegalArgumentException ignored)
			{
				// empty or a skill that no longer exists
			}
		}
		pinnedTasks.clear();
		for (String title : config.pinnedTasks().split(TASK_SEPARATOR))
		{
			if (tasks.get(title.trim()) != null)
			{
				pinnedTasks.add(title.trim());
			}
		}
		pinnedTaskTitles = Collections.unmodifiableSet(new HashSet<>(pinnedTasks));
	}

	private void announce(Skill skill, int from, int to)
	{
		Predicate<Unlock> filter = snapshot().shown(skill);
		for (int level = from + 1; level <= to; level++)
		{
			List<Unlock> unlocks = repository.at(skill, level, filter);
			if (unlocks.isEmpty())
			{
				continue;
			}
			String list = unlocks.stream()
				.limit(MESSAGE_ENTRIES)
				.map(Unlock::getText)
				.collect(Collectors.joining("; "));
			if (unlocks.size() > MESSAGE_ENTRIES)
			{
				list += " (+" + (unlocks.size() - MESSAGE_ENTRIES) + " more)";
			}
			sendChat("New at ", skill.getName() + " " + level + ":", list);
		}
	}

	/** A game message: a plain prefix, then the skill and level and the unlocks in their colors. */
	private void sendChat(String prefix, String label, String text)
	{
		String colored = new ChatMessageBuilder()
			.append(config.colorChatLevel(), label)
			.append(" ")
			.append(config.colorChatUnlocks(), text)
			.build();
		// chat fonts have no bold; a black shadow makes the text heavier and keeps yellow readable on parchment.
		// The builder escapes tags in appended text, so the shadow goes around what it built.
		if (config.chatShadow())
		{
			colored = "<shad=000000>" + colored + "</shad>";
		}
		String message = new ChatMessageBuilder()
			.append(ChatColorType.NORMAL)
			.append(prefix)
			.build() + colored;
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	/** Switches to the game's skill guide unlocks once the cache is loaded. Client thread. */
	private void loadSkillGuide()
	{
		if (repository.isSkillGuide() || client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
		{
			return;
		}
		try
		{
			repository.loadSkillGuide(client);
		}
		catch (RuntimeException e)
		{
			log.warn("Couldn't read the skill guide; using the bundled wiki unlocks", e);
		}
		clearTooltips();
		panelDirty = true;
	}

	/** Checks which required quests are unfinished. Client thread, logged in. */
	private void refreshQuests()
	{
		questsDirty = false;
		ticksSinceQuestRefresh = 0;
		Set<Integer> toDo = new HashSet<>();
		for (int id : repository.requiredQuests())
		{
			Quest quest = QUESTS.get(id);
			// quests newer than this RuneLite's list can't be checked; they're left as usable
			if (quest != null && quest.getState(client) != QuestState.FINISHED)
			{
				toDo.add(id);
			}
		}
		Set<String> finished = new HashSet<>();
		if (config.almostThere())
		{
			Set<String> titles = repository.questTitles();
			for (Quest quest : Quest.values())
			{
				if (titles.contains(quest.getName()) && quest.getState(client) == QuestState.FINISHED)
				{
					finished.add(quest.getName());
				}
			}
		}
		refreshTaskQuests();
		Set<Integer> unfinished = config.greyOutLocked() ? toDo : Collections.emptySet();
		if (!unfinished.equals(unfinishedQuests))
		{
			unfinishedQuests = unfinished;
			clearTooltips();
			panelDirty = true;
		}
		if (!toDo.equals(questsToDo) || !finished.equals(finishedQuests))
		{
			questsToDo = toDo;
			finishedQuests = finished;
			panelDirty = true;
		}
	}

	/** Checks the pinned quests and the quests they need, and unpins quests once they're done. Client thread. */
	private void refreshTaskQuests()
	{
		Map<String, QuestState> states = new HashMap<>();
		for (String title : pinnedTasks)
		{
			Task task = tasks.get(title);
			questState(task.getWiki(), states);
			task.getQuests().forEach(q -> questState(q, states));
		}
		if (states.equals(questStates))
		{
			return;
		}
		Map<String, QuestState> before = questStates;
		questStates = states;
		taskCache.clear();
		boolean unpinned = false;
		for (Iterator<String> it = pinnedTasks.iterator(); it.hasNext(); )
		{
			String title = it.next();
			QuestState was = before.get(title);
			// only as it happens, so a finished quest can still be pinned
			if (was != null && was != QuestState.FINISHED && states.get(title) == QuestState.FINISHED)
			{
				it.remove();
				unpinned = true;
				sendChat("Quest complete! ", tasks.name(title), "is unpinned.");
			}
		}
		if (unpinned)
		{
			saveTaskPins();
			pushToPanel();
		}
	}

	/** Adds the state of the quest with this wiki title, when this RuneLite knows the quest. */
	private void questState(String wiki, Map<String, QuestState> states)
	{
		Quest quest = QUESTS_BY_NAME.get(tasks.name(wiki));
		if (quest != null)
		{
			states.put(wiki, quest.getState(client));
		}
	}

	/** Card for a pinned quest or diary: its hard requirements, unmet first. Client thread. */
	LayoutableRenderableEntity taskCard(String wiki)
	{
		Task task = tasks.get(wiki);
		if (task == null || levels.isEmpty())
		{
			return null;
		}
		TooltipBox cached = taskCache.get(wiki);
		if (cached != null)
		{
			return cached;
		}
		Snapshot snapshot = snapshot();
		Palette palette = snapshot.getPalette();
		int textSize = config.tooltipTextSize();
		Font regular = FontManager.getRunescapeFont();
		Font font = textSize == regular.getSize() ? regular : regular.deriveFont((float) textSize);

		Player player = client.getLocalPlayer();
		int combat = player == null ? 0 : player.getCombatLevel();
		List<Requirement> requirements = Requirement.check(task, snapshot::effectiveLevel,
			client.getVarpValue(VarPlayerID.QP), combat, q -> questStates.get(q) == QuestState.FINISHED, tasks::name);
		long met = requirements.stream().filter(Requirement::isMet).count();

		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		QuestState state = questStates.get(wiki);
		String status = task.isDiary() ? "Diary" : state == QuestState.FINISHED ? "Completed"
			: state == QuestState.IN_PROGRESS ? "In progress" : state == QuestState.NOT_STARTED ? "Not started" : "";
		Color statusColor = state == QuestState.FINISHED ? Color.GREEN : state == QuestState.IN_PROGRESS ? Color.YELLOW
			: state == QuestState.NOT_STARTED ? Color.RED : Color.WHITE;
		lines.add(LineComponent.builder().left(task.getName()).leftColor(palette.getNext()).leftFont(font).build());
		lines.add(LineComponent.builder()
			.left(status)
			.leftColor(statusColor)
			.leftFont(font)
			.right(requirements.isEmpty() ? "No requirements" : met + "/" + requirements.size() + " met")
			.rightColor(met == requirements.size() ? Color.GREEN : Color.WHITE)
			.rightFont(font)
			.build());
		for (Requirement r : requirements.subList(0, Math.min(TASK_LINES, requirements.size())))
		{
			Color color = r.isMet() ? Color.GREEN : Color.RED;
			lines.add(LineComponent.builder()
				.left(r.getLabel())
				.leftColor(color)
				.leftFont(font)
				.right(r.getProgress())
				.rightColor(color)
				.rightFont(font)
				.build());
		}
		if (requirements.size() > TASK_LINES)
		{
			lines.add(LineComponent.builder().left("+" + (requirements.size() - TASK_LINES) + " more").leftColor(Color.LIGHT_GRAY).leftFont(font).build());
		}
		card.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize(), 0));

		Color base = runeLiteConfig.overlayBackgroundColor();
		Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255 * config.tooltipOpacity() / 100);
		TooltipBox box = new TooltipBox(card, background, config.tooltipOutline() ? palette.getOutline() : null, Collections.emptyList());
		taskCache.put(wiki, box);
		return box;
	}

	private void loadHiddenSections()
	{
		Set<String> sections = new HashSet<>();
		for (String key : config.hiddenSections().split(SECTION_SEPARATOR))
		{
			if (!key.trim().isEmpty())
			{
				sections.add(key.trim());
			}
		}
		hiddenSections = Collections.unmodifiableSet(sections);
	}

	/** Hides or shows a skill guide section; the config change refreshes the panel. Any thread. */
	private void setSectionHidden(String key, boolean hide)
	{
		Set<String> sections = new TreeSet<>(hiddenSections);
		if (hide ? sections.add(key) : sections.remove(key))
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "hiddenSections", String.join(SECTION_SEPARATOR, sections));
		}
	}

	/** Sets a goal, or drops it when it is one. Any thread. */
	private synchronized void toggleGoal(Goal goal)
	{
		List<Goal> updated = new ArrayList<>(goals);
		if (!updated.remove(goal))
		{
			updated.add(goal);
		}
		saveGoals(updated);
	}

	/** The config change puts up the infoboxes and refreshes the panel. */
	private synchronized void saveGoals(List<Goal> updated)
	{
		goals = Collections.unmodifiableList(updated);
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "goals", Goal.format(updated));
	}

	/** Announces and drops the skill's goals the level reaches. Client thread. */
	private void reachGoals(Skill skill, int level)
	{
		List<Goal> reached = goals.stream()
			.filter(g -> g.getSkill() == skill && g.getLevel() <= level)
			.collect(Collectors.toList());
		if (reached.isEmpty())
		{
			return;
		}
		for (Goal goal : reached)
		{
			String label = goal.getSkill().getName() + " " + goal.getLevel() + ":";
			sendChat("Goal reached! ", label, goal.getText());
			notifier.notify(config.goalNotification(), "Goal reached! " + label + " " + goal.getText());
		}
		synchronized (this)
		{
			List<Goal> remaining = new ArrayList<>(goals);
			remaining.removeAll(reached);
			saveGoals(remaining);
		}
	}

	/** Puts up an infobox per goal, or none when they're off. Client thread. */
	private void syncGoalBoxes()
	{
		goalBoxes.forEach(infoBoxManager::removeInfoBox);
		goalBoxes.clear();
		if (!config.goalInfoboxes())
		{
			return;
		}
		for (Goal goal : goals)
		{
			GoalInfoBox box = new GoalInfoBox(goalImage(goal), this, goal, client::getSkillExperience, config.colorNext());
			box.getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_INFOBOX, REMOVE_GOAL, "Goal"));
			goalBoxes.add(box);
			infoBoxManager.addInfoBox(box);
		}
	}

	/** The goal's item or spell picture, or its skill's icon. Client thread. */
	private BufferedImage goalImage(Goal goal)
	{
		Unlock u = repository.all(goal.getSkill()).stream()
			.filter(x -> goal.is(goal.getSkill(), x))
			.findFirst()
			.orElse(null);
		BufferedImage image = null;
		if (u != null && u.getItem() != null)
		{
			image = itemManager.getImage(u.getItem());
		}
		else if (u != null && u.getSprite() != null)
		{
			image = spriteManager.getSprite(u.getSprite(), 0);
		}
		return image != null ? image : skillIconManager.getSkillImage(goal.getSkill(), false);
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (!REMOVE_GOAL.equals(event.getEntry().getOption()))
		{
			return;
		}
		for (GoalInfoBox box : goalBoxes)
		{
			if (box.getMenuEntries().contains(event.getEntry()))
			{
				synchronized (this)
				{
					List<Goal> remaining = new ArrayList<>(goals);
					remaining.remove(box.getGoal());
					saveGoals(remaining);
				}
				return;
			}
		}
	}

	/** Current stats and settings. Client thread. */
	private Snapshot snapshot()
	{
		Map<Skill, Integer> levelCopy = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xpCopy = new EnumMap<>(Skill.class);
		Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			levelCopy.putAll(levels);
			for (Skill skill : levels.keySet())
			{
				xpCopy.put(skill, client.getSkillExperience(skill));
				int boost = client.getBoostedSkillLevel(skill);
				if (config.countBoosts() && boost > levels.get(skill))
				{
					boosted.put(skill, boost);
				}
			}
		}
		return new Snapshot(levelCopy, xpCopy, boosted, filter(), config.levelsAhead(), unfinishedQuests, hiddenSections, Palette.of(config),
			questsToDo, finishedQuests, goals, config.almostThere(), pinnedTaskTitles);
	}

	/** Copies current stats for the panel. Client thread. */
	private void pushToPanel()
	{
		panelDirty = false;
		ticksSincePanelRefresh = 0;
		NextUnlocksPanel target = panel;
		if (target == null)
		{
			return;
		}
		Snapshot snapshot = snapshot();
		SwingUtilities.invokeLater(() -> target.update(snapshot));
	}
}
