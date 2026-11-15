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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
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
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
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
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.events.InfoBoxMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
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
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.AsyncBufferedImage;
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
	/** PanelComponent's default border on each side, which the classic card keeps. */
	private static final int PANEL_PADDING = 4;
	private static final String PIN = "Pin";
	private static final String UNPIN = "Unpin";
	private static final String EXAMINE = "Examine";
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
	/** The material line under an item goal's making level: its text size against the card's, and the smallest. */
	private static final float MATERIAL_SIZE = 0.75f;
	private static final int MATERIAL_MIN_SIZE = 10;
	/** Points the name on a quest or diary card is set bigger than the card's text. */
	private static final int TASK_TITLE_GROWTH = 4;
	/** Unmet requirements of a quest an unlock or item needs, on tiny lines under it, before "+N more". */
	private static final int QUEST_SUB_LINES = 3;
	/** Between a pinned card's key and a requirement's label in the checked-off list. */
	private static final String CHECK_SEPARATOR = "\t";
	private static final String SET_AMOUNT = "Set amount goal";
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

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private NextUnlocksMenu menu;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	private final UnlockRepository repository = new UnlockRepository();
	/** Real levels as last seen; client thread only. Empty while logged out. */
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> boosts = new EnumMap<>(Skill.class);
	private final Map<Skill, TooltipBox> tooltipCache = new ConcurrentHashMap<>();
	/** Pinned cards, built apart from the hover tooltips since they also show the skill and level. */
	private final Map<Skill, TooltipBox> pinnedCache = new ConcurrentHashMap<>();
	/** Each skill's latest level-up, for the arcade bar's show; client thread. */
	private final Map<Skill, PinBar.LevelUp> levelUps = new EnumMap<>(Skill.class);
	/** Each skill's xp as last seen, and what it just gained, for the pinned cards' xp effects; client thread. */
	private final Map<Skill, Integer> xpSeen = new EnumMap<>(Skill.class);
	private final Map<Skill, XpEffects.Gain> xpGains = new EnumMap<>(Skill.class);

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
	/** Requirements checked off by hand, as the card's key, a tab and the requirement's label; replaced whole. */
	private volatile Set<String> checkedOff = Collections.emptySet();
	/** Amount goals by pin key, in the order set; client thread. */
	private final Map<String, Tally> tallies = new ConcurrentHashMap<>();
	/** The inventory's amounts at the last tick, by unnoted item ID, or null before the first; client thread. */
	private Map<Integer, Integer> lastInventory;
	/** The xp of each skill with an amount goal at the last tick, and the skills that gained some then; client thread. */
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private Set<Skill> gainedLastTick = EnumSet.noneOf(Skill.class);

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
		tasks.setUnlocks(this::unlockTask);

		loadHiddenSections();
		goals = Goal.parse(config.goals());
		panel = new NextUnlocksPanel(repository, skillIconManager, itemManager, spriteManager, this::setSectionHidden, this::toggleGoal,
			title -> tasks.get(title) != null, this::toggleTaskLater, tasks::items,
			(skill, item) -> clientThread.invokeLater(() -> askTally(skill, item)));
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
		overlayManager.add(menu);
		mouseManager.registerMouseListener(menu);
		mouseManager.registerMouseWheelListener(menu);
		keyManager.registerKeyListener(menu);
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
		keyManager.unregisterKeyListener(menu);
		mouseManager.unregisterMouseWheelListener(menu);
		mouseManager.unregisterMouseListener(menu);
		overlayManager.remove(menu);
		menu.close();
		goalBoxes.forEach(infoBoxManager::removeInfoBox);
		goalBoxes.clear();
		skillGuide.shutDown();
		skillGuide = null;
		pinned.clear();
		pinnedTasks.clear();
		tallies.clear();
		lastInventory = null;
		lastXp.clear();
		questStates = Collections.emptyMap();
		clientToolbar.removeNavigation(navButton);
		levels.clear();
		boosts.clear();
		levelUps.clear();
		xpSeen.clear();
		xpGains.clear();
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
				lastInventory = null;
				lastXp.clear();
				boosts.clear();
				levelUps.clear();
				xpSeen.clear();
				xpGains.clear();
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

		Integer seen = xpSeen.put(skill, event.getXp());
		if (seen != null && event.getXp() > seen)
		{
			long now = System.currentTimeMillis();
			XpEffects.Gain gain = xpGains.get(skill);
			int gained = event.getXp() - seen;
			xpGains.put(skill, gain == null ? new XpEffects.Gain(now, gained) : gain.plus(gained, now));
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
			// reached the unlock its bar was filling toward, or just another level on the way
			NavigableMap<Integer, List<Unlock>> wasNext = repository.upcoming(skill, previous, snapshot().upcoming(skill), 1);
			levelUps.put(skill, new PinBar.LevelUp(System.currentTimeMillis(), !wasNext.isEmpty() && wasNext.firstKey() <= level));
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
		countTallies();
	}

	/**
	 * Counts what the amount goals' skills made or gathered this tick: the change in each item's inventory amount, on a
	 * tick the skill gained xp or the one after, since an item can land a tick after its xp.
	 */
	private void countTallies()
	{
		if (tallies.isEmpty() || client.getGameState() != GameState.LOGGED_IN)
		{
			lastInventory = null;
			return;
		}
		Set<Skill> gained = EnumSet.noneOf(Skill.class);
		for (Tally tally : tallies.values())
		{
			int xp = client.getSkillExperience(tally.getSkill());
			Integer was = lastXp.put(tally.getSkill(), xp);
			if (was != null && xp > was)
			{
				gained.add(tally.getSkill());
			}
		}
		Map<Integer, Integer> inventory = new HashMap<>();
		ItemContainer container = client.getItemContainer(InventoryID.INV);
		for (Item item : container == null ? new Item[0] : container.getItems())
		{
			if (item.getId() > 0)
			{
				inventory.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
			}
		}
		Map<Integer, Integer> before = lastInventory;
		Set<Skill> gainedBefore = gainedLastTick;
		lastInventory = inventory;
		gainedLastTick = gained;
		if (before == null)
		{
			return;
		}
		boolean changed = false;
		for (Map.Entry<String, Tally> e : tallies.entrySet())
		{
			Tally tally = e.getValue();
			int change = inventory.getOrDefault(tally.getItem(), 0) - before.getOrDefault(tally.getItem(), 0);
			int add = tally.isDone() ? 0 : Tally.counted(tally.getSkill(), change,
				gained.contains(tally.getSkill()) || gainedBefore.contains(tally.getSkill()));
			if (add > 0)
			{
				Tally now = tally.withCount(tally.getCount() + add);
				e.setValue(now);
				taskCache.remove(e.getKey());
				changed = true;
				if (now.isDone())
				{
					sendChat("Amount goal reached! ", tally.getSkill().getName() + ":", now.goalText());
					notifier.notify(config.goalNotification(), "Amount goal reached! " + now.goalText());
				}
			}
		}
		if (changed)
		{
			saveTallies();
		}
	}

	private void saveTallies()
	{
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "tallies", Tally.format(tallies.values()));
	}

	/** Asks in the chatbox how many of a skill's item to aim for, and pins or updates that amount goal. Client thread. */
	void askTally(Skill skill, int itemId)
	{
		int item = itemManager.canonicalize(itemId);
		String name = itemManager.getItemComposition(item).getName();
		String key = Tally.key(skill, item);
		chatboxPanelManager.openTextInput("How many " + name + " to " + Tally.verb(skill).toLowerCase() + "? (like 500 or 2k)")
			.onDone((String text) -> clientThread.invoke(() ->
			{
				int amount = Tally.parseAmount(text);
				if (amount <= 0)
				{
					return;
				}
				Tally old = tallies.get(key);
				tallies.put(key, old != null ? old.withTarget(amount) : new Tally(skill, item, name, amount, 0));
				pinnedTasks.add(key);
				taskCache.remove(key);
				saveTallies();
				saveTaskPins();
				pushToPanel();
			}))
			.build();
	}

	/** Starts an amount goal's count again from 0. Client thread. */
	private void resetTally(String key)
	{
		Tally tally = tallies.get(key);
		if (tally != null)
		{
			tallies.put(key, tally.withCount(0));
			taskCache.remove(key);
			saveTallies();
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
			if ("tallies".equals(event.getKey()) || "checkedOff".equals(event.getKey()) || "lookPresets".equals(event.getKey()))
			{
				// saved by the plugin itself, which already updated its cards
				return;
			}
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

		CardTheme theme = theme(palette, pinnedCard, skill);
		int textSize = config.tooltipTextSize();
		Font regular = FontManager.getRunescapeFont();
		Font font = theme.font(textSize);
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
			lines.add(new CardLine(theme, font, true)
				.icon(theme.isSkillIcon() ? skillIconManager.getSkillImage(skill, true) : null)
				.left(skill.getName(), theme.getSkillName())
				.right("Level ", theme.getLevelLabel())
				.right(boosted == level ? String.valueOf(level) : boosted + "/" + level,
					boosted > level ? Color.GREEN : boosted < level ? Color.RED : theme.getLevelValue()));
		}
		if (config.tooltipXp())
		{
			// the lines of the game's own tooltip, which onBeforeRender hides
			// the header already names the skill
			CardLine current = new CardLine(theme, font)
				.left(header ? "XP:" : skill.getName() + " XP:", theme.getXpLabel())
				.right(number.format(xp), theme.getXpValue());
			lines.add(current);
			updates.add(x -> current.setRight(0, number.format(x)));
			if (level < Experience.MAX_REAL_LEVEL)
			{
				int nextXp = Experience.getXpForLevel(level + 1);
				lines.add(new CardLine(theme, font)
					.left("Next level at:", theme.getNextLevelLabel())
					.right(number.format(nextXp), theme.getNextLevelValue()));
				CardLine remaining = new CardLine(theme, font)
					.left("Remaining XP:", theme.getRemainingLabel())
					.right(number.format(Math.max(0, nextXp - xp)), theme.getRemainingValue());
				lines.add(remaining);
				updates.add(x -> remaining.setRight(0, number.format(Math.max(0, nextXp - x))));
			}
		}
		if (!iconsOnly)
		{
			CardLine nextUnlock = new CardLine(theme, font)
				.left("Next unlock: ", theme.getNextUnlockLabel())
				.left(String.valueOf(nextLevel), theme.getNextUnlockLevel())
				.right(number.format(xpToGo) + " xp", theme.getNextUnlockXp());
			lines.add(nextUnlock);
			int nextUnlockXp = Experience.getXpForLevel(nextLevel);
			updates.add(x -> nextUnlock.setRight(0, number.format(Math.max(0, nextUnlockXp - x)) + " xp"));
		}
		if (config.tooltipProgress())
		{
			PinBar progress = new PinBar(theme, Experience.getXpForLevel(repository.previous(skill, level, snapshot.shown(skill))),
				Experience.getXpForLevel(nextLevel), xp, theme.getBarFill(), theme.getBarFill2(), theme.getBarBackground(),
				() -> levelUps.get(skill));
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
				lines.add(new CardLine(theme, font).left(u.getText(), snapshot.locked(u) ? theme.getLocked() : theme.getUnlock()));
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
			lines.add(new CardLine(theme, font).left("+" + hidden + " more", theme.getMore()));
		}
		if (goal != null)
		{
			int goalXp = Experience.getXpForLevel(goal.getLevel());
			Color skillColor = Skills.color(skill);
			CardLine goalLine = new CardLine(theme, font)
				.left("Goal: ", theme.getGoalLabel() == null ? skillColor : theme.getGoalLabel())
				.left(String.valueOf(goal.getLevel()), theme.getGoalLevel() == null ? skillColor : theme.getGoalLevel())
				.right(number.format(Math.max(0, goalXp - xp)) + " xp", theme.getGoalXp());
			lines.add(goalLine);
			updates.add(x -> goalLine.setRight(0, number.format(Math.max(0, goalXp - x)) + " xp"));
			// the whole way from 0 xp, as the start of a goal isn't kept; no level-up effect, the bar above has it
			PinBar goalBar = goalBar(theme, 0, goalXp, xp, skillColor);
			lines.add(goalBar);
			updates.add(goalBar::setValue);
		}

		int columns = iconsOnly ? Math.min(TOOLTIP_ICON_COLUMNS, icons.getChildren().size()) : TOOLTIP_ICON_COLUMNS;
		int width = columns * (UnlockTileComponent.size(UnlockTile.WIDTH, scale) + ICON_GAP) - ICON_GAP + PANEL_BORDER;
		if (!iconsOnly || config.tooltipXp())
		{
			// room for the text, which grows with its size and with fonts wider than the game's
			width = (int) (Math.max(width, ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize()) * theme.widthScale(textSize));
		}
		// the same room inside, whatever the theme's padding
		width += 2 * (theme.getPadding() - PANEL_PADDING);
		tooltip.setPreferredSize(new Dimension(width, 0));

		TooltipBox box = new TooltipBox(tooltip, theme, updates);
		if (complete)
		{
			cache.put(skill, box);
		}
		return box;
	}

	/** A goal bar: the card's goal colors, or the skill's color when it has none. */
	private static PinBar goalBar(CardTheme theme, double min, double max, double value, Color skillColor)
	{
		return new PinBar(theme, min, max, value, theme.getGoalBar() == null ? skillColor : theme.getGoalBar(), theme.getGoalBar2(),
			theme.getGoalBarBackground() == null ? theme.getBarBackground() : theme.getGoalBarBackground(), null);
	}

	/**
	 * The theme for a card: the look the plugin always had for hover tooltips, the player's pick for pinned cards, with
	 * the skill's own changes. The skill is null for quests, diaries and items.
	 */
	private CardTheme theme(Palette palette, boolean pinnedCard, Skill skill)
	{
		Color base = runeLiteConfig.overlayBackgroundColor();
		Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255 * config.tooltipOpacity() / 100);
		CardTheme classic = CardTheme.classic(palette, background, config.tooltipOutline() ? palette.getOutline() : null);
		return pinnedCard ? CardTheme.pinned(look(skill), palette, classic, skill) : classic;
	}

	/** The pinned card look a skill's cards have: the config with the skill's own changes, or just the config for null. */
	Look look(Skill skill)
	{
		return new Look(config, skill == null ? null : Look.readList(config.skillLooks()).get(skill.name()));
	}

	/** Whether the skill's cards look different from the rest. */
	boolean hasOwnLook(Skill skill)
	{
		Map<String, String> own = Look.readList(config.skillLooks()).get(skill.name());
		return own != null && !own.isEmpty();
	}

	/** Change a look setting for every card when the skill is null, or only for the skill's cards. */
	synchronized void setLook(Skill skill, String key, String value)
	{
		if (skill == null)
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, key, value);
			return;
		}
		Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
		list.computeIfAbsent(skill.name(), k -> new LinkedHashMap<>()).put(key, Look.clean(value));
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooks", Look.writeList(list));
	}

	/** Put the skill's cards back to the look the rest have. */
	synchronized void resetLook(Skill skill)
	{
		Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
		if (list.remove(skill.name()) != null)
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooks", Look.writeList(list));
		}
	}

	/** The saved looks' names, in the order they were saved. */
	List<String> presets()
	{
		return new ArrayList<>(Look.readList(config.lookPresets()).keySet());
	}

	/** Save the look of every card, or of the skill's cards, under a name; one of the same name is replaced. */
	synchronized void savePreset(String name, Skill skill)
	{
		String clean = Look.clean(name);
		if (clean.isEmpty())
		{
			return;
		}
		Map<String, String> values = look(skill).values();
		if (skill != null && NextUnlocksConfig.CardStyle.SKILL.name().equals(values.get("pinStyle")) && values.get("pinBase").isEmpty())
		{
			// keep the skill's built-in look when this is loaded onto another skill
			values.put("pinBase", skill.name());
		}
		Map<String, Map<String, String>> list = Look.readList(config.lookPresets());
		list.put(clean, values);
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "lookPresets", Look.writeList(list));
	}

	/** Give every card, or the skill's cards, a saved look. */
	synchronized void loadPreset(String name, Skill skill)
	{
		Map<String, String> values = Look.readList(config.lookPresets()).get(name);
		if (values == null)
		{
			return;
		}
		if (skill != null)
		{
			Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
			list.put(skill.name(), values);
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooks", Look.writeList(list));
			return;
		}
		for (Map.Entry<String, String> e : values.entrySet())
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, e.getKey(), e.getValue());
		}
	}

	synchronized void deletePreset(String name)
	{
		Map<String, Map<String, String>> list = Look.readList(config.lookPresets());
		if (list.remove(name) != null)
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "lookPresets", Look.writeList(list));
		}
	}

	synchronized void renamePreset(String name, String to)
	{
		String clean = Look.clean(to);
		Map<String, Map<String, String>> list = Look.readList(config.lookPresets());
		if (clean.isEmpty() || !list.containsKey(name) || clean.equals(name))
		{
			return;
		}
		// in the same place in the list
		Map<String, Map<String, String>> renamed = new LinkedHashMap<>();
		list.forEach((k, v) ->
		{
			if (!k.equals(clean))
			{
				renamed.put(k.equals(name) ? clean : k, v);
			}
		});
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "lookPresets", Look.writeList(renamed));
	}

	/** Asks in the chatbox for a name to save the look under. Client thread. */
	void askSavePreset(Skill skill)
	{
		chatboxPanelManager.openTextInput("Save " + (skill == null ? "the cards'" : skill.getName() + "'s") + " look as:")
			.onDone((String text) -> savePreset(text, skill))
			.build();
	}

	/** Asks in the chatbox for a saved look's new name. Client thread. */
	void askRenamePreset(String name)
	{
		chatboxPanelManager.openTextInput("Rename " + name + " to:")
			.value(name)
			.onDone((String text) -> renamePreset(name, text))
			.build();
	}

	/** What the skill just gained, while its show is going; client thread. */
	XpEffects.Gain xpGain(Skill skill)
	{
		XpEffects.Gain gain = xpGains.get(skill);
		return gain == null || System.currentTimeMillis() - gain.getAt() >= XpEffects.MS ? null : gain;
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
		if (EXAMINE.equals(entry.getOption()) && entry.getItemId() > 0)
		{
			// an item in the inventory, bank, equipment or a shop with a goal to pin
			Task task = tasks.byItem(itemManager.canonicalize(entry.getItemId()));
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
			if (tallies.containsKey(task))
			{
				addTallyEntries(task);
			}
			else
			{
				addCheckOffEntry(task);
				addTaskPinEntry(tasks.get(task));
			}
		}
		Skill guideSkill = skillGuide.skill();
		if (guideSkill == null || hasPinEntry())
		{
			return;
		}
		int item = skillGuide.itemAt(mouse.getX(), mouse.getY());
		Task goal = item <= 0 ? null : tasks.byItem(item) != null ? tasks.byItem(item) : tasks.byItem(itemManager.canonicalize(item));
		Unlock unlock = goal == null ? skillGuide.unlockAt(mouse.getX(), mouse.getY()) : null;
		if (item > 0)
		{
			int counted = itemManager.canonicalize(item);
			client.getMenu().createMenuEntry(1)
				.setOption(SET_AMOUNT)
				.setTarget(ColorUtil.wrapWithColorTag(itemManager.getItemComposition(counted).getName(), config.colorNext()))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> askTally(guideSkill, counted));
		}
		if (goal != null)
		{
			addTaskPinEntry(goal);
		}
		else if (unlock != null)
		{
			// a quest or diary row pins the quest or diary itself
			addTaskPinEntry(unlock.getType() == Unlock.Type.UNLOCK ? tasks.get(TaskRepository.unlockKey(guideSkill, unlock))
				: tasks.get(unlock.getWiki()));
		}
	}

	/** Unpin, Set amount goal and Reset count for an amount goal's card, just above Cancel. */
	private void addTallyEntries(String key)
	{
		Tally tally = tallies.get(key);
		String target = ColorUtil.wrapWithColorTag(tally.getName(), config.colorNext());
		client.getMenu().createMenuEntry(1)
			.setOption("Reset count")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> resetTally(key));
		client.getMenu().createMenuEntry(1)
			.setOption(SET_AMOUNT)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> askTally(tally.getSkill(), tally.getItem()));
		client.getMenu().createMenuEntry(1)
			.setOption(UNPIN)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> toggleTask(key));
	}

	/**
	 * Check off, with each requirement the game doesn't count as met to tick by hand, like a quest done before this
	 * plugin could see it, and the ones ticked to untick.
	 */
	private void addCheckOffEntry(String key)
	{
		Task task = tasks.get(key);
		if (task == null)
		{
			return;
		}
		Set<String> checked = checkedFor(key);
		List<Requirement> open = requirements(task, Collections.emptySet()).stream()
			.filter(r -> !r.isMet())
			.collect(Collectors.toList());
		if (open.isEmpty())
		{
			return;
		}
		MenuEntry parent = client.getMenu().createMenuEntry(1)
			.setOption("Check off")
			.setTarget(ColorUtil.wrapWithColorTag(task.getName(), config.colorNext()))
			.setType(MenuAction.RUNELITE);
		Menu sub = parent.createSubMenu();
		// each entry goes on top, so the list is built from the bottom up
		for (int i = open.size() - 1; i >= 0; i--)
		{
			Requirement r = open.get(i);
			boolean done = checked.contains(r.getLabel());
			sub.createMenuEntry(-1)
				.setOption(done ? "Uncheck" : "Check off")
				.setTarget(ColorUtil.wrapWithColorTag(r.getLabel() + (r.getProgress().isEmpty() ? "" : " " + r.getProgress()),
					done ? Color.GREEN : Color.RED))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> toggleCheckedOff(key, r.getLabel()));
		}
	}

	/** The labels of a card's requirements checked off by hand. Any thread. */
	private Set<String> checkedFor(String key)
	{
		Set<String> labels = new HashSet<>();
		String prefix = key + CHECK_SEPARATOR;
		for (String entry : checkedOff)
		{
			if (entry.startsWith(prefix))
			{
				labels.add(entry.substring(prefix.length()));
			}
		}
		return labels;
	}

	/** Ticks a card's requirement off by hand, or unticks it. Client thread. */
	private void toggleCheckedOff(String key, String label)
	{
		Set<String> entries = new TreeSet<>(checkedOff);
		String entry = key + CHECK_SEPARATOR + label;
		if (!entries.remove(entry))
		{
			entries.add(entry);
		}
		setCheckedOff(entries);
	}

	private void setCheckedOff(Set<String> entries)
	{
		checkedOff = Collections.unmodifiableSet(entries);
		taskCache.clear();
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "checkedOff", String.join("\n", entries));
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

	/** Pin or Unpin for a quest, diary or item card, just above Cancel. */
	private void addTaskPinEntry(Task task)
	{
		if (task == null)
		{
			return;
		}
		client.getMenu().createMenuEntry(1)
			.setOption(pinnedTasks.contains(task.getWiki()) ? UNPIN : PIN)
			.setTarget(ColorUtil.wrapWithColorTag(task.getName() + (task.isItem() ? " goal" : task.isUnlock() ? " unlock" : " requirements"),
				config.colorNext()))
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
		else
		{
			// an unpinned card forgets its amount goal and what was checked off on it
			if (tallies.remove(wiki) != null)
			{
				saveTallies();
			}
			Set<String> entries = new TreeSet<>(checkedOff);
			if (entries.removeIf(e -> e.startsWith(wiki + CHECK_SEPARATOR)))
			{
				setCheckedOff(entries);
			}
		}
		saveTaskPins();
		pushToPanel();
	}

	/** {@link #toggleTask} from the panel's thread. */
	void toggleTaskLater(String wiki)
	{
		clientThread.invokeLater(() -> toggleTask(wiki));
	}

	/** The goals set, in the order they were. Any thread. */
	List<Goal> goals()
	{
		return goals;
	}

	/** The name of a pinned quest, diary or item goal. */
	String taskName(String wiki)
	{
		Tally tally = tallies.get(wiki);
		return tally != null ? tally.goalText() : tasks.name(wiki);
	}

	/**
	 * The card for an {@link TaskRepository#UNLOCK} key, from the unlocks as loaded now: its level, and the quest it needs
	 * by wiki title when the bundled quests know it, else by name. Null when no unlock matches. Any thread.
	 */
	private Task unlockTask(String key)
	{
		Skill skill = TaskRepository.unlockSkill(key);
		int level = TaskRepository.unlockLevel(key);
		String text = TaskRepository.unlockText(key);
		if (skill == null)
		{
			return null;
		}
		for (Unlock u : repository.all(skill))
		{
			if (u.getLevel() == level && u.getType() == Unlock.Type.UNLOCK && u.getText().equals(text))
			{
				Quest quest = u.getQuest() == null ? null : QUESTS.get(u.getQuest());
				Task questTask = quest == null ? null : tasks.byName(quest.getName());
				return TaskRepository.unlockTask(skill, u, questTask != null ? questTask.getWiki() : quest == null ? null : quest.getName());
			}
		}
		return null;
	}

	/** Asks in the chatbox for a quest, diary or item to pin, and pins the best match. Client thread. */
	void askPinTask()
	{
		chatboxPanelManager.openTextInput("Pin a quest, diary or item:")
			.onDone((String text) ->
			{
				Task task = tasks.find(text);
				if (text.trim().isEmpty())
				{
					return;
				}
				if (task == null)
				{
					sendChat("", "Nothing found for", text.trim());
				}
				else if (!pinnedTasks.contains(task.getWiki()))
				{
					toggleTask(task.getWiki());
				}
			})
			.build();
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
		List<Goal> choices = goalChoices(skill);
		if (choices.isEmpty() && current.isEmpty() && level >= Experience.MAX_REAL_LEVEL)
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
		if (level < Experience.MAX_REAL_LEVEL)
		{
			sub.createMenuEntry(-1)
				.setOption("Choose level...")
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> askGoalLevel(skill, level));
		}
		for (int i = choices.size() - 1; i >= 0; i--)
		{
			Goal goal = choices.get(i);
			boolean levelOnly = goal.equals(Goal.level(skill, goal.getLevel()));
			goalChoice(sub, "Level " + goal.getLevel(), levelOnly ? "" : goal.getText(), goal);
		}
	}

	/**
	 * Goals to pick from for a skill, those already set left out: the next few unlocks, the next level and 99. Empty when
	 * logged out. Client thread.
	 */
	List<Goal> goalChoices(Skill skill)
	{
		Integer level = levels.get(skill);
		if (level == null)
		{
			return Collections.emptyList();
		}
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
		choices.removeAll(goals);
		return choices;
	}

	/** The skill's real level, or null when logged out. Client thread. */
	Integer level(Skill skill)
	{
		return levels.get(skill);
	}

	/** Asks in the chatbox for a level to aim for, above the current one. Client thread. */
	void askGoalLevel(Skill skill, int level)
	{
		chatboxPanelManager.openTextInput(skill.getName() + " goal level (" + (level + 1) + "-" + Experience.MAX_REAL_LEVEL + "):")
			.charValidator(Character::isDigit)
			.onDone((String text) ->
			{
				int target;
				try
				{
					target = Integer.parseInt(text.trim());
				}
				catch (NumberFormatException e)
				{
					return;
				}
				Goal goal = Goal.level(skill, target);
				if (target > level && target <= Experience.MAX_REAL_LEVEL && !goals.contains(goal))
				{
					toggleGoal(goal);
				}
			})
			.build();
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

	/** Pins or unpins a skill's card. Client thread. */
	void togglePin(Skill skill)
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
		Set<String> checked = new TreeSet<>();
		for (String line : config.checkedOff().split("\n"))
		{
			if (!line.trim().isEmpty())
			{
				checked.add(line.trim());
			}
		}
		checkedOff = Collections.unmodifiableSet(checked);
		tallies.clear();
		for (Tally tally : Tally.parse(config.tallies()))
		{
			tallies.put(tally.key(), tally);
		}
		pinnedTasks.clear();
		for (String title : config.pinnedTasks().split(TASK_SEPARATOR))
		{
			// a skill guide item's goal only exists once the cache is read, after the pins load, and an unlock's may
			// only match once the guide's unlocks replace the wiki's
			String key = title.trim();
			if (tasks.get(key) != null || key.startsWith(TaskRepository.GUIDE_ITEM) || key.startsWith(TaskRepository.UNLOCK)
				|| tallies.containsKey(key))
			{
				pinnedTasks.add(key);
			}
		}
		// an amount goal is always pinned
		pinnedTasks.addAll(tallies.keySet());
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
			tasks.addGuide(repository.getGuideItems());
			// unlock pins match the guide's unlocks now
			tasks.setUnlocks(this::unlockTask);
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
			if (task == null)
			{
				continue;
			}
			questState(task.getWiki(), states);
			for (String quest : task.getQuests())
			{
				questState(quest, states);
				// and the quests that quest needs, for the lines under it
				Task needed = tasks.get(quest);
				if (needed != null)
				{
					needed.getQuests().forEach(q -> questState(q, states));
				}
			}
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

	/** A requirement's color, faded halfway to the card's "+N more" gray, for the material line under it. */
	private static Color materialShade(Color color, CardTheme theme)
	{
		Color gray = theme.getMore();
		return new Color((color.getRed() + gray.getRed()) / 2, (color.getGreen() + gray.getGreen()) / 2,
			(color.getBlue() + gray.getBlue()) / 2, color.getAlpha());
	}

	/** Card for a pinned quest, diary or item: its hard requirements, unmet first. Client thread. */
	LayoutableRenderableEntity taskCard(String wiki)
	{
		if (levels.isEmpty())
		{
			return null;
		}
		Tally tally = tallies.get(wiki);
		Task task = tally == null ? tasks.get(wiki) : null;
		if (task == null && tally == null)
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
		CardTheme theme = theme(palette, true, tally == null ? null : tally.getSkill());
		int textSize = config.tooltipTextSize();
		Font regular = FontManager.getRunescapeFont();
		Font font = theme.font(textSize);
		Font small = theme.font(Math.max(MATERIAL_MIN_SIZE, Math.round(textSize * MATERIAL_SIZE)));

		if (tally != null)
		{
			return tallyCard(wiki, tally, theme, font, small, textSize, regular);
		}
		List<Requirement> requirements = requirements(task, checkedFor(wiki));
		long met = requirements.stream().filter(Requirement::isMet).count();

		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		QuestState state = questStates.get(wiki);
		boolean goal = task.isItem() || task.isUnlock();
		String status = goal ? (met == requirements.size() ? "Ready" : task.isItem() ? "Item goal" : "Unlock") : task.isDiary() ? "Diary" : state == QuestState.FINISHED ? "Completed"
			: state == QuestState.IN_PROGRESS ? "In progress" : state == QuestState.NOT_STARTED ? "Not started" : "";
		Color statusColor = goal ? (met == requirements.size() ? theme.getMet() : theme.getXpValue())
			: state == QuestState.FINISHED ? theme.getMet() : state == QuestState.IN_PROGRESS ? Color.YELLOW
			: state == QuestState.NOT_STARTED ? theme.getUnmet() : theme.getXpValue();
		// the item's picture, drawn as it loads, or the quest or diary tab icon, which can be missing before the cache is
		// ready; don't keep a card without it
		BufferedImage icon = task.getItem() > 0 ? itemManager.getImage(task.getItem())
			: task.isUnlock() ? (task.getSprite() > 0 ? spriteManager.getSprite(task.getSprite(), 0)
				: skillIconManager.getSkillImage(task.getSkills().keySet().iterator().next(), true))
			: spriteManager.getSprite(task.isDiary() ? SpriteID.SideIcons.ACHIEVEMENT_DIARIES : SpriteID.SideIcons.QUEST, 0);
		lines.add(new CardHeader(icon, task.getName(), theme.getTaskTitle(), theme.titleFont(textSize, TASK_TITLE_GROWTH),
			(double) textSize / regular.getSize(), theme));
		lines.add(new CardLine(theme, font)
			.left(status, statusColor)
			.right(requirements.isEmpty() ? "No requirements" : met + "/" + requirements.size() + " met",
				met == requirements.size() ? theme.getMet() : theme.getXpValue()));
		for (Requirement r : requirements.subList(0, Math.min(TASK_LINES, requirements.size())))
		{
			Color color = r.isMet() ? theme.getMet() : theme.getUnmet();
			lines.add(new CardLine(theme, font).left(r.getLabel(), color).right(r.getProgress(), color));
			if (r.getMaterial() != null)
			{
				lines.add(new CardMaterial(theme, small, r.getMaterialItem() > 0 ? itemManager.getImage(r.getMaterialItem()) : null,
					r.getMaterial(), materialShade(color, theme)));
			}
			if (!r.isMet() && r.getQuest() != null)
			{
				addQuestSubLines(lines, r, theme, small);
			}
		}
		if (requirements.size() > TASK_LINES)
		{
			lines.add(new CardLine(theme, font).left("+" + (requirements.size() - TASK_LINES) + " more", theme.getMore()));
		}
		card.setPreferredSize(new Dimension((int) (ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize() * theme.widthScale(textSize))
			+ 2 * (theme.getPadding() - PANEL_PADDING), 0));

		TooltipBox box = new TooltipBox(card, theme, Collections.emptyList());
		if (icon != null)
		{
			taskCache.put(wiki, box);
		}
		return box;
	}

	/** The task's requirements as the player stands, with the ones checked off by hand counted as met. Client thread. */
	private List<Requirement> requirements(Task task, Set<String> checked)
	{
		Player player = client.getLocalPlayer();
		int combat = player == null ? 0 : player.getCombatLevel();
		return Requirement.checkOff(Requirement.check(task, snapshot()::effectiveLevel,
			client.getVarpValue(VarPlayerID.QP), combat, q -> questStates.get(q) == QuestState.FINISHED, tasks::name,
			config.itemMakingLevels() || !task.isItem()), checked);
	}

	/**
	 * Under an unfinished quest the card needs, what that quest still needs in tiny text: its skill levels with the
	 * skill's icon and its quests with the quest icon, at most {@link #QUEST_SUB_LINES} then "+N more".
	 */
	private void addQuestSubLines(List<LayoutableRenderableEntity> lines, Requirement quest, CardTheme theme, Font small)
	{
		Task task = tasks.get(quest.getQuest());
		if (task == null)
		{
			task = tasks.byName(quest.getLabel());
		}
		if (task == null)
		{
			return;
		}
		List<Requirement> open = requirements(task, checkedFor(task.getWiki())).stream()
			.filter(r -> !r.isMet())
			.collect(Collectors.toList());
		Color shade = materialShade(theme.getUnmet(), theme);
		for (Requirement r : open.subList(0, Math.min(QUEST_SUB_LINES, open.size())))
		{
			BufferedImage icon = r.getSkill() != null ? skillIconManager.getSkillImage(r.getSkill(), true)
				: r.getQuest() != null ? spriteManager.getSprite(SpriteID.SideIcons.QUEST, 0) : null;
			lines.add(new CardMaterial(theme, small, icon, r.getLabel() + (r.getProgress().isEmpty() ? "" : " " + r.getProgress()), shade));
		}
		if (open.size() > QUEST_SUB_LINES)
		{
			lines.add(new CardMaterial(theme, small, null, "+" + (open.size() - QUEST_SUB_LINES) + " more", theme.getMore()));
		}
	}

	/** Card for an amount goal: the item, what's done of the target, a bar in the skill's color and the skill. */
	private LayoutableRenderableEntity tallyCard(String key, Tally tally, CardTheme theme, Font font, Font small, int textSize,
		Font regular)
	{
		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		BufferedImage icon = itemManager.getImage(tally.getItem());
		lines.add(new CardHeader(icon, tally.getName(), theme.getTaskTitle(), theme.titleFont(textSize, TASK_TITLE_GROWTH),
			(double) textSize / regular.getSize(), theme));
		Color color = tally.isDone() ? theme.getMet() : theme.getXpValue();
		lines.add(new CardLine(theme, font)
			.left(tally.isDone() ? "Done!" : Tally.verb(tally.getSkill()) + " " + String.format(Locale.ROOT, "%,d", tally.getTarget()), color)
			.right(String.format(Locale.ROOT, "%,d/%,d", tally.getCount(), tally.getTarget()), color));
		lines.add(goalBar(theme, 0, tally.getTarget(), Math.min(tally.getCount(), tally.getTarget()), Skills.color(tally.getSkill())));
		lines.add(new CardMaterial(theme, small, skillIconManager.getSkillImage(tally.getSkill(), true), tally.getSkill().getName(),
			materialShade(theme.getXpValue(), theme)));
		card.setPreferredSize(new Dimension((int) (ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize() * theme.widthScale(textSize))
			+ 2 * (theme.getPadding() - PANEL_PADDING), 0));
		TooltipBox box = new TooltipBox(card, theme, Collections.emptyList());
		if (icon != null)
		{
			taskCache.put(key, box);
		}
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
	synchronized void toggleGoal(Goal goal)
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
			BufferedImage icon = goalImage(goal);
			GoalInfoBox box = new GoalInfoBox(GoalInfoBox.withTextRoom(icon), this, goal, client::getSkillExperience, config.colorNext());
			if (icon instanceof AsyncBufferedImage)
			{
				// item pictures load later; redraw the box's picture once this one has
				((AsyncBufferedImage) icon).onLoaded(() ->
				{
					box.setImage(GoalInfoBox.withTextRoom(icon));
					infoBoxManager.updateInfoBoxImage(box);
				});
			}
			box.getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_INFOBOX, REMOVE_GOAL,
				ColorUtil.wrapWithColorTag(goal.getText(), config.colorNext())));
			goalBoxes.add(box);
			infoBoxManager.addInfoBox(box);
		}
	}

	/** The goal's item or spell picture, or its skill's icon. Client thread. */
	BufferedImage goalImage(Goal goal)
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
	public void onInfoBoxMenuClicked(InfoBoxMenuClicked event)
	{
		if (!REMOVE_GOAL.equals(event.getEntry().getOption()) || !(event.getInfoBox() instanceof GoalInfoBox)
			|| !goalBoxes.contains(event.getInfoBox()))
		{
			return;
		}
		synchronized (this)
		{
			List<Goal> remaining = new ArrayList<>(goals);
			remaining.remove(((GoalInfoBox) event.getInfoBox()).getGoal());
			saveGoals(remaining);
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
