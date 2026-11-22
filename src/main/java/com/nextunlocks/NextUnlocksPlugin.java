package com.nextunlocks;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.ZoneId;
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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
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
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.events.InfoBoxMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;
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
	/** The slot behind pinned icons when the card has no bar background. */
	private static final Color SLOT_FILL = new Color(0, 0, 0, 90);
	/** PanelComponent's default left plus right border. */
	private static final int PANEL_BORDER = 10;
	/** PanelComponent's default border on each side, which the classic card keeps. */
	static final int PANEL_PADDING = 4;
	private static final String PIN = "Pin";
	private static final String UNPIN = "Unpin";
	private static final String EXAMINE = "Examine";
	private static final String REMOVE_GOAL = "Remove goal";
	private static final String SET_GOAL = "Set goal";
	private static final String COMBAT_LEVEL = "Combat level";
	private static final String HIDE_TOP = "Hide from top";
	/** Upcoming unlock levels offered in the Set goal menu. */
	private static final int GOAL_CHOICES = 3;
	private static final int MESSAGE_ENTRIES = 3;
	/** Panel refresh interval for xp-only changes, in game ticks. */
	private static final int PANEL_REFRESH_TICKS = 5;
	/** How often quest states are checked again, in game ticks. */
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final String SECTION_SEPARATOR = ";";
	private static final String TASK_SEPARATOR = "\n";
	/** Between the pin keys of amount goals sharing a card, in its cache key. */
	private static final String TALLY_GROUP = ",";
	/** Requirement lines on a quest or diary card before the rest are counted as "+N more". */
	private static final int TASK_LINES = 10;
	/** The material line under an item goal's making level: its text size against the card's, and the smallest. */
	static final float MATERIAL_SIZE = 0.75f;
	static final int MATERIAL_MIN_SIZE = 10;
	/** Points the name on a quest or diary card is set bigger than the card's text. */
	static final int TASK_TITLE_GROWTH = 4;
	/** Unmet requirements of a quest an unlock or item needs, on tiny lines under it, before "+N more". */
	private static final int QUEST_SUB_LINES = 3;
	/** Between a pinned card's key and a requirement's label in the checked-off list. */
	private static final String CHECK_SEPARATOR = "\t";
	private static final String SET_AMOUNT = "Set amount goal";
	/** Guides one account can follow at once, each with its card above the chatbox. */
	static final int MAX_GUIDES = 3;
	/** RSProfile keys: the followed guides' IDs, comma separated, and the hand-ticked steps' keys, a line each. */
	private static final String GUIDES_KEY = "followedGuides";
	private static final String GUIDE_CHECKS_KEY = "guideChecks";
	/** Between chat warnings about XP in one capped skill; a level-up past the cap always warns. */
	private static final long CAP_WARNING_GAP_MILLIS = 60_000;
	private static final Color CAP_WARNING_COLOR = new Color(255, 48, 48);
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
	private CombatTopOverlay combatTopOverlay;

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
	private volatile TooltipBox combatCache;
	private volatile TooltipBox plateCache;
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
	/** This account's level-ups, from its RSProfile; client thread writes. */
	private volatile List<History.Entry> history = Collections.emptyList();
	/** The XP each goal was set at, for its bar. */
	private volatile Map<Goal, Integer> goalStarts = Collections.emptyMap();
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
	/** This session's pace per amount goal, by pin key. */
	private final Map<String, Tally.Pace> paces = new ConcurrentHashMap<>();
	/** When each amount goal was reached this session, by pin key, for its bar's celebration. */
	private final Map<String, Long> tallyReached = new ConcurrentHashMap<>();
	/** How many of each amount goal's item were in the bank when it was last open, by unnoted item ID. */
	private volatile Map<Integer, Integer> banked = Collections.emptyMap();
	/** The inventory's amounts at the last tick, by unnoted item ID, or null before the first; client thread. */
	private Map<Integer, Integer> lastInventory;
	/** The xp of each skill with an amount goal at the last tick, and the skills that gained some then; client thread. */
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private Set<Skill> gainedLastTick = EnumSet.noneOf(Skill.class);
	private final GuideRepository guides = new GuideRepository();
	/** Wiki titles of every quest the guides name, with the quests those need; set on start-up. */
	private Set<String> guideQuestTitles = Collections.emptySet();
	/** The guides this account follows, by ID in follow order, from its RSProfile; replaced whole. */
	private volatile List<String> followedGuides = Collections.emptyList();
	/** Guide steps this account ticked off by hand, by step key, from its RSProfile; replaced whole. */
	private volatile Set<String> guideChecks = Collections.emptySet();
	/** States of the quests in {@link #guideQuestTitles}; client thread. */
	private Map<String, QuestState> guideQuests = Collections.emptyMap();
	/** Each guide's progress for this account, by ID; empty while logged out. Replaced whole. */
	@Getter
	private volatile Map<String, GuideProgress> guideProgress = Collections.emptyMap();
	/** When each capped skill last warned in the chat; client thread. */
	private final Map<Skill, Long> capWarned = new EnumMap<>(Skill.class);
	/** The followed guides' in-game cards, by guide id. */
	private final Map<String, TooltipBox> guideCards = new ConcurrentHashMap<>();
	/** How many steps left each guide card is scrolled past, by guide id. */
	private final Map<String, Integer> guideScroll = new ConcurrentHashMap<>();
	private final MouseWheelListener guideWheel = this::guideWheelMoved;
	private boolean ironman;

	@Provides
	NextUnlocksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NextUnlocksConfig.class);
	}

	/**
	 * Each skill's own card became the default style in 1.5.0; a style saved before then is cleared once so it shows,
	 * after which whatever style is picked stays.
	 */
	private void useSkillLooksOnce()
	{
		if (configManager.getConfiguration(NextUnlocksConfig.GROUP, "skillLooksDefault") == null)
		{
			configManager.unsetConfiguration(NextUnlocksConfig.GROUP, "pinStyle");
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooksDefault", "1");
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		repository.load(gson);
		tasks.load(gson, repository.diaries());
		tasks.setUnlocks(this::unlockTask);
		guides.load(gson, tasks, repository);
		Set<String> guideQuestsNeeded = new HashSet<>();
		for (Guide guide : guides.all())
		{
			for (Guide.Step step : guide.steps())
			{
				if (step.getType() == Guide.Step.Type.QUEST)
				{
					guideQuestsNeeded.addAll(GuideRepository.chain(step.getWiki(), tasks));
				}
			}
		}
		guideQuestTitles = Collections.unmodifiableSet(guideQuestsNeeded);

		useSkillLooksOnce();
		loadHiddenSections();
		goals = Goal.parse(config.goals());
		goalStarts = Goal.parseStarts(config.goalStarts());
		panel = new NextUnlocksPanel(repository, skillIconManager, itemManager, spriteManager, this::setSectionHidden, this::toggleGoal,
			title -> tasks.get(title) != null, this::toggleTaskLater, tasks::items,
			(skill, item) -> clientThread.invokeLater(() -> askTally(skill, item)),
			id -> clientThread.invokeLater(() -> toggleGuide(id)), key -> clientThread.invokeLater(() -> toggleGuideCheck(key)));
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
		overlayManager.add(combatTopOverlay);
		overlayManager.add(menu);
		mouseManager.registerMouseListener(menu);
		mouseManager.registerMouseWheelListener(menu);
		mouseManager.registerMouseWheelListener(guideWheel);
		keyManager.registerKeyListener(menu);
		skillGuide = new SkillGuide(client, clientThread, config, repository, this::snapshot);
		CardArt.items = itemManager::getImage;
		spriteManager.getSpriteAsync(SpriteID.MINI_LEFT, 0, left ->
			spriteManager.getSpriteAsync(SpriteID.FIXED_MINIMAP_COVER, 0, cover ->
				spriteManager.getSpriteAsync(SpriteID.MINI_RIGHT, 0, right -> CardArt.minimap(left, cover, right))));

		clientThread.invokeLater(() ->
		{
			loadPins();
			loadSkillGuide();
			loadHistory();
			loadGuides();
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
		overlayManager.remove(combatTopOverlay);
		keyManager.unregisterKeyListener(menu);
		mouseManager.unregisterMouseWheelListener(menu);
		mouseManager.unregisterMouseWheelListener(guideWheel);
		mouseManager.unregisterMouseListener(menu);
		overlayManager.remove(menu);
		menu.close();
		goalBoxes.forEach(infoBoxManager::removeInfoBox);
		goalBoxes.clear();
		skillGuide.shutDown();
		skillGuide = null;
		CardArt.clear();
		pinned.clear();
		pinnedTasks.clear();
		tallies.clear();
		paces.clear();
		lastInventory = null;
		lastXp.clear();
		questStates = Collections.emptyMap();
		clientToolbar.removeNavigation(navButton);
		levels.clear();
		boosts.clear();
		levelUps.clear();
		history = Collections.emptyList();
		xpSeen.clear();
		xpGains.clear();
		clearTooltips();
		unfinishedQuests = Collections.emptySet();
		questsToDo = Collections.emptySet();
		finishedQuests = Collections.emptySet();
		followedGuides = Collections.emptyList();
		guideChecks = Collections.emptySet();
		guideQuests = Collections.emptyMap();
		guideProgress = Collections.emptyMap();
		capWarned.clear();
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
				guideQuests = Collections.emptyMap();
				guideProgress = Collections.emptyMap();
				capWarned.clear();
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
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadHistory();
		loadGuides();
		refreshGuides();
		pushToPanel();
	}

	/** This account's followed guides and hand-ticked steps. */
	private void loadGuides()
	{
		String followed = configManager.getRSProfileConfiguration(NextUnlocksConfig.GROUP, GUIDES_KEY);
		List<String> ids = new ArrayList<>();
		for (String id : followed == null ? new String[0] : followed.split(","))
		{
			if (guides.get(id.trim()) != null && !ids.contains(id.trim()) && ids.size() < MAX_GUIDES)
			{
				ids.add(id.trim());
			}
		}
		followedGuides = Collections.unmodifiableList(ids);
		String checks = configManager.getRSProfileConfiguration(NextUnlocksConfig.GROUP, GUIDE_CHECKS_KEY);
		Set<String> keys = new TreeSet<>();
		for (String key : checks == null ? new String[0] : checks.split("\n"))
		{
			if (!key.trim().isEmpty())
			{
				keys.add(key.trim());
			}
		}
		guideChecks = Collections.unmodifiableSet(keys);
	}

	/** The guides this account follows, in follow order. Any thread. */
	List<Guide> followedGuides()
	{
		List<Guide> list = new ArrayList<>();
		for (String id : followedGuides)
		{
			Guide guide = guides.get(id);
			if (guide != null)
			{
				list.add(guide);
			}
		}
		return list;
	}

	List<Guide> allGuides()
	{
		return guides.all();
	}

	/** Follows a guide, or stops following it. Client thread. */
	void toggleGuide(String id)
	{
		Guide guide = guides.get(id);
		if (guide == null)
		{
			return;
		}
		List<String> ids = new ArrayList<>(followedGuides);
		guideScroll.remove(id);
		if (!ids.remove(id))
		{
			if (ids.size() >= MAX_GUIDES)
			{
				sendChat("", "Already following " + MAX_GUIDES + " guides:", "stop one to follow " + guide.getName() + ".");
				return;
			}
			ids.add(id);
		}
		followedGuides = Collections.unmodifiableList(ids);
		configManager.setRSProfileConfiguration(NextUnlocksConfig.GROUP, GUIDES_KEY, String.join(",", ids));
		guideCards.clear();
		refreshGuides();
		pushToPanel();
	}

	/** Ticks a hand-ticked guide step off, or back on. Client thread. */
	void toggleGuideCheck(String key)
	{
		Set<String> keys = new TreeSet<>(guideChecks);
		if (!keys.remove(key))
		{
			keys.add(key);
		}
		guideChecks = Collections.unmodifiableSet(keys);
		configManager.setRSProfileConfiguration(NextUnlocksConfig.GROUP, GUIDE_CHECKS_KEY, String.join("\n", keys));
		refreshGuides();
		pushToPanel();
	}

	/** Works out each guide's progress, and says in the chat when a followed guide's step is done. Client thread. */
	private void refreshGuides()
	{
		if (levels.isEmpty() || client.getGameState() != GameState.LOGGED_IN)
		{
			guideProgress = Collections.emptyMap();
			guideCards.clear();
			return;
		}
		ironman = client.getVarbitValue(VarbitID.IRONMAN) > 0;
		GuideProgress.Account account = new GuideProgress.Account()
		{
			@Override
			public int level(Skill skill)
			{
				return levels.getOrDefault(skill, 1);
			}

			@Override
			public int xp(Skill skill)
			{
				return client.getSkillExperience(skill);
			}

			@Override
			public boolean questDone(String wiki)
			{
				return guideQuests.get(wiki) == QuestState.FINISHED;
			}

			@Override
			public boolean diaryDone(String title)
			{
				Integer varbit = Diaries.completeVarbit(title);
				return varbit != null && client.getVarbitValue(varbit) > 0;
			}

			@Override
			public boolean checked(String key)
			{
				return guideChecks.contains(key);
			}
		};
		Map<String, GuideProgress> before = guideProgress;
		Map<String, GuideProgress> after = new HashMap<>();
		for (Guide guide : guides.all())
		{
			after.put(guide.getId(), GuideProgress.of(guide, account, tasks));
		}
		guideProgress = Collections.unmodifiableMap(after);
		// keep the cards that didn't change, so their animations don't start over every refresh
		after.forEach((id, now) ->
		{
			if (!now.sameAs(before.get(id)))
			{
				guideCards.remove(id);
			}
		});
		if (before.isEmpty() || !config.guideMessages())
		{
			// the first look after logging in isn't news
			return;
		}
		for (Guide guide : followedGuides())
		{
			GuideProgress was = before.get(guide.getId());
			GuideProgress now = after.get(guide.getId());
			if (was == null || now.done() <= was.done())
			{
				continue;
			}
			for (Guide.Step step : guide.steps())
			{
				// a step ticked by hand was just clicked; no need to say so
				if (!step.isManual() && now.isDone(step) && !was.isDone(step))
				{
					List<Guide.Step> next = now.upcoming(1);
					sendChat(guide.getName() + ": ", step.getTitle() + " done.",
						now.isComplete() ? "Guide complete!" : next.isEmpty() ? "" : "Next: " + next.get(0).getTitle());
				}
			}
		}
	}

	/** Warns in red when a followed pure guide's capped skill gains XP. Client thread. */
	private void warnCaps(Skill skill, int xp, int level, Integer previous)
	{
		long now = System.currentTimeMillis();
		Long last = capWarned.get(skill);
		Integer cap = null;
		for (Guide guide : followedGuides())
		{
			String line = GuideProgress.capWarning(guide, skill, xp, level);
			if (line == null)
			{
				continue;
			}
			cap = guide.getCaps().get(skill);
			boolean broke = previous != null && level > previous && level > cap;
			if (broke || last == null || now - last >= CAP_WARNING_GAP_MILLIS)
			{
				capWarned.put(skill, now);
				sendWarning(line);
			}
			return;
		}
	}

	private void sendWarning(String text)
	{
		String line = new ChatMessageBuilder().append(CAP_WARNING_COLOR, text).build();
		if (config.chatShadow())
		{
			line = "<shad=000000>" + line + "</shad>";
		}
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(line)
			.build());
	}

	private void loadHistory()
	{
		history = Collections.unmodifiableList(History.parse(configManager.getRSProfileConfiguration(NextUnlocksConfig.GROUP, History.KEY)));
	}

	private void recordLevelUp(Skill skill, int from, int to)
	{
		history = History.add(history, new History.Entry(skill, from, to, System.currentTimeMillis()));
		configManager.setRSProfileConfiguration(NextUnlocksConfig.GROUP, History.KEY, History.format(history));
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
		if (seen == null)
		{
			startGoals();
		}
		if (seen != null && event.getXp() > seen)
		{
			long now = System.currentTimeMillis();
			XpEffects.Gain gain = xpGains.get(skill);
			int gained = event.getXp() - seen;
			xpGains.put(skill, gain == null ? new XpEffects.Gain(now, gained) : gain.plus(gained, now));
		}

		int level = event.getLevel();
		Integer previous = levels.put(skill, level);
		if (seen != null && event.getXp() > seen && config.guideCapWarnings())
		{
			warnCaps(skill, event.getXp(), level, previous);
		}
		Integer previousBoost = boosts.put(skill, event.getBoostedLevel());
		if (!Objects.equals(previous, level) || !Objects.equals(previousBoost, event.getBoostedLevel()))
		{
			// levels and combat level are on the quest and diary cards
			taskCache.clear();
			combatCache = null;
			plateCache = null;
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
			recordLevelUp(skill, previous, level);
			if (config.levelUpMessage())
			{
				announce(skill, previous, level);
			}
			// the cards show levels against their targets
			guideCards.clear();
			refreshGuides();
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
		if (before == null || open(InterfaceID.Bankmain.UNIVERSE) || open(InterfaceID.BankDepositbox.UNIVERSE))
		{
			// items moved in and out of the bank while the skill trains weren't made or used up
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
				paces.computeIfAbsent(e.getKey(), k -> new Tally.Pace()).add(add, System.currentTimeMillis());
				forgetTally(e.getKey());
				changed = true;
				if (now.isDone())
				{
					tallyReached.put(e.getKey(), System.currentTimeMillis());
					sendChat("Amount goal reached! ", tally.getSkill().getName() + ":", now.goalText());
					notifier.notify(config.goalNotification(), "Amount goal reached! " + now.goalText());
				}
			}
		}
		if (changed)
		{
			saveTallies();
			panelDirty = true;
		}
	}

	/** Whether an interface is open and showing. Client thread. */
	private boolean open(int component)
	{
		Widget widget = client.getWidget(component);
		return widget != null && !widget.isHidden();
	}

	/** Drops the cached card an amount goal is on, alone or with the skill's others. */
	private void forgetTally(String key)
	{
		taskCache.keySet().removeIf(k -> Arrays.asList(k.split(TALLY_GROUP)).contains(key));
	}

	/** The pinned amount goals on the same card as this one: all of its skill's, in the order pinned. Client thread. */
	private List<String> tallyGroup(String key)
	{
		Tally tally = tallies.get(key);
		List<String> group = new ArrayList<>();
		for (String pinned : pinnedTasks)
		{
			Tally other = tallies.get(pinned);
			if (other != null && tally != null && other.getSkill() == tally.getSkill())
			{
				group.add(pinned);
			}
		}
		if (group.isEmpty())
		{
			group.add(key);
		}
		return group;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK || tallies.isEmpty())
		{
			return;
		}
		// only the items amount goals count, so the config keeps a handful of numbers rather than the whole bank
		Set<Integer> wanted = tallies.values().stream().map(Tally::getItem).collect(Collectors.toSet());
		Map<Integer, Integer> found = new TreeMap<>();
		for (Item item : event.getItemContainer().getItems())
		{
			int id = item.getId() > 0 ? itemManager.canonicalize(item.getId()) : -1;
			if (wanted.contains(id))
			{
				found.merge(id, item.getQuantity(), Integer::sum);
			}
		}
		if (!found.equals(banked))
		{
			banked = Collections.unmodifiableMap(found);
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "tallyBanked", Tally.formatBanked(found));
			tallies.keySet().forEach(this::forgetTally);
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
		Tally current = tallies.get(key);
		chatboxPanelManager.openTextInput("How many " + name + " to " + Tally.verb(skill).toLowerCase(Locale.ROOT) + "? (like 500 or 2k)")
			.value(current != null ? String.valueOf(current.getTarget()) : "")
			.onDone((String text) -> clientThread.invoke(() ->
			{
				int amount = Tally.parseAmount(text);
				if (amount <= 0)
				{
					if (text != null && !text.trim().isEmpty())
					{
						sendChat("", "Amount goal not set:", "type a number like 500, 2.5k or 1m.");
					}
					return;
				}
				Tally old = tallies.get(key);
				Tally now = old != null ? old.withTarget(amount) : new Tally(skill, item, name, amount, 0);
				tallies.put(key, now);
				sendChat("Amount goal set: ", skill.getName() + ":", now.goalText()
					+ (now.getCount() > 0 ? String.format(Locale.ROOT, " (%,d so far)", now.getCount()) : ""));
				pinnedTasks.add(key);
				forgetTally(key);
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
			paces.remove(key);
			tallyReached.remove(key);
			forgetTally(key);
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
			if ("tallies".equals(event.getKey()) || "tallyBanked".equals(event.getKey()) || "checkedOff".equals(event.getKey()) || "lookPresets".equals(event.getKey())
				|| History.KEY.equals(event.getKey()))
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
			if ("goalStarts".equals(event.getKey()))
			{
				goalStarts = Goal.parseStarts(config.goalStarts());
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
		guideCards.clear();
		combatCache = null;
		plateCache = null;
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
					.left(pinnedCard ? "Next level:" : "Next level at:", theme.getNextLevelLabel())
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
		int iconsTop = 0;
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
				// pinned cards sit icons in slots; the level is only on them when no line says it
				UnlockTileComponent tile = new UnlockTileComponent(image, u.getText(), pinnedCard && !iconsOnly ? null : nextLevel,
					palette.getLevel(), snapshot.locked(u), scale);
				icons.getChildren().add(pinnedCard ? slot(tile, theme) : tile);
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
				iconsTop = ICON_TOP_PADDING;
				icons.setBorder(new Rectangle(0, iconsTop, 0, 0));
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
			// from the XP the goal was set at; no level-up effect, the bar above has it
			Integer start = goalStarts.get(goal);
			PinBar goalBar = goalBar(theme, start != null && start < goalXp ? start : 0, goalXp, xp, skillColor);
			lines.add(goalBar);
			updates.add(goalBar::setValue);
		}

		int columns = iconsOnly ? Math.min(TOOLTIP_ICON_COLUMNS, icons.getChildren().size()) : TOOLTIP_ICON_COLUMNS;
		int tileWidth = UnlockTileComponent.width(scale, pinnedCard);
		int width = columns * (tileWidth + ICON_GAP) - ICON_GAP + PANEL_BORDER;
		if (!iconsOnly || config.tooltipXp())
		{
			// room for the text, which grows with its size and with fonts wider than the game's
			width = (int) (Math.max(width, ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize()) * theme.widthScale(textSize));
		}
		// the same room inside, whatever the theme's padding
		width += 2 * (theme.getPadding() - PANEL_PADDING);
		// wide enough that no line's two sides run into each other, like a long skill name and its level up top
		for (LayoutableRenderableEntity line : lines)
		{
			if (line instanceof CardLine)
			{
				width = Math.max(width, ((CardLine) line).minWidth() + 2 * theme.getPadding());
			}
		}
		tooltip.setPreferredSize(new Dimension(width, 0));
		if (pinnedCard && !icons.getChildren().isEmpty())
		{
			// center the row of icons, the first row when they wrap
			int inside = width - 2 * theme.getPadding();
			int perRow = Math.max(1, (inside + ICON_GAP) / (tileWidth + ICON_GAP));
			int shown = Math.min(perRow, icons.getChildren().size());
			icons.setBorder(new Rectangle(Math.max(0, (inside - shown * (tileWidth + ICON_GAP) + ICON_GAP) / 2), iconsTop, 0, 0));
		}

		TooltipBox box = new TooltipBox(tooltip, theme, updates);
		if (complete)
		{
			cache.put(skill, box);
		}
		return box;
	}

	/** An icon in a slot in the card's colors: the bar's background darkened, edged with the card's border. */
	static UnlockTileComponent slot(UnlockTileComponent tile, CardTheme theme)
	{
		Color bar = theme.getBarBackground();
		// darker than the bar, so the slot looks sunk into the card whatever its colors
		Color fill = bar == null ? SLOT_FILL : new Color(bar.getRed() * 11 / 20, bar.getGreen() * 11 / 20, bar.getBlue() * 11 / 20,
			Math.min(bar.getAlpha(), 170));
		Color border = theme.getBorder();
		Color edge = border == null ? null : new Color(border.getRed(), border.getGreen(), border.getBlue(), Math.min(border.getAlpha(), 110));
		return tile.slot(fill, edge, Math.min(theme.getCorner(), 4));
	}

	/** A goal bar: the card's goal colors, or the skill's color when it has none. */
	private static PinBar goalBar(CardTheme theme, double min, double max, double value, Color skillColor)
	{
		return goalBar(theme, min, max, value, skillColor, null);
	}

	/** A goal bar with its own celebration when the goal is reached. */
	private static PinBar goalBar(CardTheme theme, double min, double max, double value, Color skillColor,
		Supplier<PinBar.LevelUp> reached)
	{
		return new PinBar(theme, min, max, value, theme.getGoalBar() == null ? skillColor : theme.getGoalBar(), theme.getGoalBar2(),
			theme.getGoalBarBackground() == null ? theme.getBarBackground() : theme.getGoalBarBackground(), reached);
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
		return pinnedCard ? CardTheme.pinned(look(skill == null ? null : skill.name()), palette, classic, skill) : classic;
	}

	/**
	 * The pinned card look some cards have: the config with their own changes, or just the config for null. Cards are named
	 * by skill, or {@link CombatLevel#LOOK} for the combat level card.
	 */
	Look look(String cards)
	{
		return new Look(config, cards == null ? null : Look.readList(config.skillLooks()).get(cards));
	}

	/** Whether the cards look different from the rest. */
	boolean hasOwnLook(String cards)
	{
		Map<String, String> own = Look.readList(config.skillLooks()).get(cards);
		return own != null && !own.isEmpty();
	}

	/** Change a look setting for every card when the cards are null, or only for those cards. */
	synchronized void setLook(String cards, String key, String value)
	{
		if (cards == null)
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, key, value);
			return;
		}
		Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
		list.computeIfAbsent(cards, k -> new LinkedHashMap<>()).put(key, Look.clean(value));
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooks", Look.writeList(list));
	}

	/** Change several look settings at once, for every card when the cards are null, or only for those cards. */
	synchronized void setLooks(String cards, Map<String, String> values)
	{
		if (cards == null)
		{
			values.forEach((key, value) -> configManager.setConfiguration(NextUnlocksConfig.GROUP, key, value));
			return;
		}
		Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
		Map<String, String> own = list.computeIfAbsent(cards, k -> new LinkedHashMap<>());
		values.forEach((key, value) -> own.put(key, Look.clean(value)));
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooks", Look.writeList(list));
	}

	/**
	 * The colors the cards show with their style's own colors, under the own-colors settings' keys, so own colors can start
	 * from what the card looks like now.
	 */
	Map<String, String> styleColors(String cards)
	{
		Map<String, String> own = cards == null ? null : Look.readList(config.skillLooks()).get(cards);
		Map<String, String> overrides = own == null ? new LinkedHashMap<>() : new LinkedHashMap<>(own);
		overrides.put("pinCustomColors", "false");
		Look look = new Look(config, overrides);
		Palette palette = snapshot().getPalette();
		boolean combat = CombatLevel.LOOK.equals(cards);
		Skill skill = null;
		if (cards != null && !combat)
		{
			try
			{
				skill = Skill.valueOf(cards);
			}
			catch (IllegalArgumentException e)
			{
				skill = null;
			}
		}
		CardTheme theme = CardTheme.pinned(look, palette, theme(palette, false, null), skill, combat);
		Map<String, String> colors = new LinkedHashMap<>();
		CardTheme.ownColors(theme).forEach((key, color) -> colors.put(key, Look.format(color)));
		return colors;
	}

	/** Put the cards back to the look the rest have. */
	synchronized void resetLook(String cards)
	{
		Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
		if (list.remove(cards) != null)
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "skillLooks", Look.writeList(list));
		}
	}

	/** The saved looks' names, in the order they were saved. */
	List<String> presets()
	{
		return new ArrayList<>(Look.readList(config.lookPresets()).keySet());
	}

	/** Save the look of every card, or of some cards, under a name; one of the same name is replaced. */
	synchronized void savePreset(String name, String cards)
	{
		String clean = Look.clean(name);
		if (clean.isEmpty())
		{
			return;
		}
		Map<String, String> values = look(cards).values();
		if (cards != null && !CombatLevel.LOOK.equals(cards) && NextUnlocksConfig.CardStyle.SKILL.name().equals(values.get("pinStyle"))
			&& values.get("pinBase").isEmpty())
		{
			// keep the skill's built-in look when this is loaded onto another skill
			values.put("pinBase", cards);
		}
		Map<String, Map<String, String>> list = Look.readList(config.lookPresets());
		list.put(clean, values);
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "lookPresets", Look.writeList(list));
	}

	/** Give every card, or some cards, a saved look. */
	synchronized void loadPreset(String name, String cards)
	{
		Map<String, String> values = Look.readList(config.lookPresets()).get(name);
		if (values == null)
		{
			return;
		}
		if (cards != null)
		{
			Map<String, Map<String, String>> list = Look.readList(config.skillLooks());
			list.put(cards, values);
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
	void askSavePreset(String cards, String name)
	{
		chatboxPanelManager.openTextInput("Save " + (cards == null ? "the cards'" : name + "'s") + " look as:")
			.onDone((String text) -> savePreset(text, cards))
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
			// an item in the inventory, bank, equipment or a shop: an amount goal for it, and its item goal to pin
			int item = itemManager.canonicalize(entry.getItemId());
			addAmountEntry(item, repository.makers(item));
			Task task = tasks.byItem(item);
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
		if (CombatLevel.isCombat(skill))
		{
			addCombatEntries();
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
		if (pinnedOverlay.combatAt(mouse.getX(), mouse.getY()) && !hasPinEntry())
		{
			addCombatEntries();
		}
		if (combatTopOverlay.contains(mouse.getX(), mouse.getY()) && !hasPinEntry())
		{
			client.getMenu().createMenuEntry(1)
				.setOption(HIDE_TOP)
				.setTarget(ColorUtil.wrapWithColorTag(COMBAT_LEVEL, config.colorNext()))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> configManager.setConfiguration(NextUnlocksConfig.GROUP, "combatTop", false));
			addCombatEntries();
		}
		String task = pinnedOverlay.taskAt(mouse.getX(), mouse.getY());
		if (task != null && !hasPinEntry())
		{
			if (tallies.containsKey(task))
			{
				// the card holds all of the skill's amount goals
				tallyGroup(task).forEach(this::addTallyEntries);
			}
			else
			{
				addCheckOffEntry(task);
				addTaskPinEntry(tasks.get(task));
			}
		}
		Guide followed = pinnedOverlay.guideAt(mouse.getX(), mouse.getY());
		if (followed != null)
		{
			addGuideEntries(followed);
		}
		Skill guideSkill = skillGuide.skill();
		if (guideSkill == null || hasPinEntry())
		{
			return;
		}
		int item = skillGuide.itemAt(mouse.getX(), mouse.getY());
		Task goal = item <= 0 ? null : tasks.byItem(item) != null ? tasks.byItem(item) : tasks.byItem(itemManager.canonicalize(item));
		Unlock unlock = goal == null ? skillGuide.unlockAt(mouse.getX(), mouse.getY()) : null;
		if (item > 0 && !repository.isTool(itemManager.canonicalize(item)))
		{
			addAmountEntry(itemManager.canonicalize(item), Collections.singletonList(guideSkill));
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

	/**
	 * Set amount goal for an item, just above Cancel: straight away for one skill, or a submenu naming what each skill
	 * does with it, like Chop or Burn for Logs. Nothing when no skill makes or uses it, or the menu already has one.
	 */
	private void addAmountEntry(int item, List<Skill> skills)
	{
		if (skills.isEmpty())
		{
			return;
		}
		for (MenuEntry entry : client.getMenu().getMenuEntries())
		{
			if (entry.getType() == MenuAction.RUNELITE && SET_AMOUNT.equals(entry.getOption()))
			{
				return;
			}
		}
		String name = itemManager.getItemComposition(item).getName();
		MenuEntry parent = client.getMenu().createMenuEntry(1)
			.setOption(SET_AMOUNT)
			.setTarget(ColorUtil.wrapWithColorTag(name, config.colorNext()))
			.setType(MenuAction.RUNELITE);
		if (skills.size() == 1)
		{
			parent.onClick(e -> askTally(skills.get(0), item));
			return;
		}
		Menu sub = parent.createSubMenu();
		// each entry goes on top, so the list is built from the bottom up
		for (int i = skills.size() - 1; i >= 0; i--)
		{
			Skill skill = skills.get(i);
			Tally set = tallies.get(Tally.key(skill, item));
			sub.createMenuEntry(-1)
				.setOption(Tally.verb(skill))
				.setTarget(ColorUtil.wrapWithColorTag(name, config.colorNext()) + ColorUtil.wrapWithColorTag(" (" + skill.getName()
					+ (set != null ? String.format(Locale.ROOT, ", %,d/%,d", set.getCount(), set.getTarget()) : "") + ")", Color.WHITE))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> askTally(skill, item));
		}
	}

	/** Stop following and Open guide for a guide's card, just above Cancel. */
	private void addGuideEntries(Guide guide)
	{
		String target = ColorUtil.wrapWithColorTag(guide.getName(), GuideLooks.accent(guide.getStyle()));
		client.getMenu().createMenuEntry(1)
			.setOption("Stop following")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> toggleGuide(guide.getId()));
		client.getMenu().createMenuEntry(1)
			.setOption("Open guide")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> SwingUtilities.invokeLater(() ->
			{
				if (panel != null && navButton != null)
				{
					clientToolbar.openPanel(navButton);
					panel.showGuide(guide.getId());
				}
			}));
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

	/** Set goal and Pin or Unpin for the combat level card, just above Cancel. Client thread. */
	private void addCombatEntries()
	{
		String target = ColorUtil.wrapWithColorTag(COMBAT_LEVEL, config.colorNext());
		addCombatGoalEntry(target);
		client.getMenu().createMenuEntry(1)
			.setOption(config.pinnedCombat() ? UNPIN : PIN)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> setCombatPinned(!config.pinnedCombat()));
	}

	/** Set goal for the combat level, with the next level and 126 to pick from, and the goal to remove. */
	private void addCombatGoalEntry(String target)
	{
		if (levels.isEmpty())
		{
			return;
		}
		int level = CombatLevel.level(levels);
		int goal = config.combatGoal();
		if (level >= Experience.MAX_COMBAT_LEVEL && goal == 0)
		{
			return;
		}
		Menu sub = client.getMenu().createMenuEntry(1)
			.setOption(SET_GOAL)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.createSubMenu();
		// each entry goes on top, so the list is built from the bottom up
		if (goal > 0)
		{
			sub.createMenuEntry(-1)
				.setOption(REMOVE_GOAL)
				.setTarget(ColorUtil.wrapWithColorTag("Level " + goal, config.colorNext()))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> setCombatGoal(0));
		}
		if (level >= Experience.MAX_COMBAT_LEVEL)
		{
			return;
		}
		sub.createMenuEntry(-1)
			.setOption("Choose level...")
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> askCombatGoal(level));
		for (int choice : level + 1 < Experience.MAX_COMBAT_LEVEL ? new int[]{Experience.MAX_COMBAT_LEVEL, level + 1}
			: new int[]{Experience.MAX_COMBAT_LEVEL})
		{
			if (choice != goal)
			{
				sub.createMenuEntry(-1)
					.setOption("Level " + choice)
					.setTarget("")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> setCombatGoal(choice));
			}
		}
	}

	/** Asks in the chatbox for a combat level to aim for, above the current one. Client thread. */
	private void askCombatGoal(int level)
	{
		chatboxPanelManager.openTextInput("Combat level goal (" + (level + 1) + "-" + Experience.MAX_COMBAT_LEVEL + "):")
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
				if (target > level && target <= Experience.MAX_COMBAT_LEVEL)
				{
					setCombatGoal(target);
				}
			})
			.build();
	}

	/** Aims the combat level card at a level, 0 for none; a goal pins the card, where it shows. */
	void setCombatGoal(int level)
	{
		if (level > 0)
		{
			if (level != config.combatGoal())
			{
				configManager.unsetConfiguration(NextUnlocksConfig.GROUP, "combatGoalStart");
				configManager.setConfiguration(NextUnlocksConfig.GROUP, "combatGoal", level);
				startGoals();
			}
			setCombatPinned(true);
		}
		else
		{
			configManager.unsetConfiguration(NextUnlocksConfig.GROUP, "combatGoal");
			configManager.unsetConfiguration(NextUnlocksConfig.GROUP, "combatGoalStart");
		}
	}

	void setCombatPinned(boolean pin)
	{
		configManager.setConfiguration(NextUnlocksConfig.GROUP, "pinnedCombat", pin);
	}

	boolean isCombatPinned()
	{
		return config.pinnedCombat();
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
			paces.remove(wiki);
			tallyReached.remove(wiki);
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
		banked = Collections.unmodifiableMap(Tally.parseBanked(config.tallyBanked()));
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
			// a goal for a tool like the hammer, pinned before tools stopped being goals
			if (pinnedTasks.removeIf(key -> key.startsWith(TaskRepository.GUIDE_ITEM) && tasks.get(key) == null))
			{
				saveTaskPins();
			}
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
		Map<String, QuestState> guideStates = new HashMap<>();
		guideQuestTitles.forEach(q -> questState(q, guideStates));
		guideQuests = guideStates;
		// diaries finish without a quest changing, so this runs every time
		refreshGuides();
		Set<Integer> unfinished =config.greyOutLocked() ? toDo : Collections.emptySet();
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
	static Color materialShade(Color color, CardTheme theme)
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
		// a skill's amount goals share one card, on the first one pinned
		List<String> group = tally == null ? null : tallyGroup(wiki);
		if (group != null && !group.get(0).equals(wiki))
		{
			return null;
		}
		TooltipBox cached = taskCache.get(group == null ? wiki : String.join(TALLY_GROUP, group));
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
			return tallyCard(group, theme, font, small, textSize, regular);
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

	/** The guides shown as cards in game: the followed ones, unless guide cards are off. */
	List<Guide> guideCardGuides()
	{
		return config.guideCards() ? followedGuides() : Collections.emptyList();
	}

	/**
	 * Card for a followed guide, in its style's look: how far along it is, the next steps, and any broken pure caps. Client
	 * thread.
	 */
	LayoutableRenderableEntity guideCard(Guide guide)
	{
		GuideProgress progress = guideProgress.get(guide.getId());
		if (progress == null)
		{
			return null;
		}
		TooltipBox cached = guideCards.get(guide.getId());
		if (cached != null)
		{
			return cached;
		}
		BufferedImage icon = guideIcon(guide.getStyle());
		TooltipBox box = GuideLooks.card(progress, GuideLooks.theme(guide.getStyle(), snapshot().getPalette()), icon,
			config.tooltipTextSize(), skill -> levels.getOrDefault(skill, 1), guideScroll.getOrDefault(guide.getId(), 0));
		if (icon != null)
		{
			guideCards.put(guide.getId(), box);
		}
		return box;
	}

	/** Scrolls a guide card's steps with the mouse wheel over it, instead of zooming the camera. */
	private MouseWheelEvent guideWheelMoved(MouseWheelEvent e)
	{
		if (e.isConsumed())
		{
			return e;
		}
		Guide guide = pinnedOverlay.guideUnder(e.getX(), e.getY());
		if (guide != null)
		{
			e.consume();
			int steps = e.getWheelRotation();
			clientThread.invokeLater(() -> scrollGuide(guide.getId(), steps));
		}
		return e;
	}

	/** Moves a guide card down its steps left, or back up, and redraws it. Client thread. */
	private void scrollGuide(String id, int steps)
	{
		GuideProgress progress = guideProgress.get(id);
		if (progress == null)
		{
			return;
		}
		int before = GuideLooks.clampScroll(guideScroll.getOrDefault(id, 0), progress.total() - progress.done());
		int after = GuideLooks.clampScroll(before + steps, progress.total() - progress.done());
		if (after != before)
		{
			guideScroll.put(id, after);
			guideCards.remove(id);
		}
	}

	/** The header icon for a guide style: a bronze sword to start, the iron helm, a PK skull for pures. */
	private BufferedImage guideIcon(Guide.Style style)
	{
		switch (style)
		{
			case STARTER:
				return itemManager.getImage(ItemID.BRONZE_SWORD);
			case IRON:
				return itemManager.getImage(ItemID.IRONMAN_HELM);
			case PURE:
				return spriteManager.getSprite(SpriteID.HEADICONS_PK, 0);
			case QUESTS:
				return spriteManager.getSprite(SpriteID.SideIcons.QUEST, 0);
			default:
				return spriteManager.getSprite(SpriteID.SideIcons.STATS, 0);
		}
	}

	/**
	 * Card for the combat level: the exact level, the way to the next one, and the levels each combat skill needs on its own
	 * to reach it and the goal. Client thread.
	 */
	LayoutableRenderableEntity combatCard()
	{
		if (levels.isEmpty())
		{
			return null;
		}
		TooltipBox cached = combatCache;
		if (cached != null)
		{
			return cached;
		}
		Palette palette = snapshot().getPalette();
		CardTheme theme = CardTheme.pinned(look(CombatLevel.LOOK), palette, theme(palette, false, null), null, true);
		int textSize = config.tooltipTextSize();
		Font regular = FontManager.getRunescapeFont();
		Font font = theme.font(textSize);
		int level = CombatLevel.level(levels);
		double precise = CombatLevel.precise(levels);
		int goal = config.combatGoal();
		boolean aiming = goal > level;

		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		// the combat tab icon can be missing before the cache is ready; don't keep a card without it
		BufferedImage icon = theme.isSkillIcon() ? spriteManager.getSprite(SpriteID.SideIcons.COMBAT, 0) : null;
		lines.add(new CardLine(theme, font, true)
			.icon(icon)
			.left(COMBAT_LEVEL, theme.getSkillName())
			.right("Level ", theme.getLevelLabel())
			.right(String.valueOf(level), theme.getLevelValue()));
		lines.add(new CardLine(theme, font)
			.left("Exact:", theme.getXpLabel())
			.right(String.format(Locale.ROOT, "%.2f", precise), theme.getXpValue()));
		if (level >= Experience.MAX_COMBAT_LEVEL)
		{
			lines.add(new CardLine(theme, font).left("Max combat level!", theme.getMet()));
		}
		else
		{
			lines.add(new CardLine(theme, font)
				.left("Next level: ", theme.getNextLevelLabel())
				.left(String.valueOf(level + 1), theme.getNextLevelValue())
				.right(String.format(Locale.ROOT, "%.2f to go", level + 1 - precise), theme.getRemainingValue()));
			lines.add(new PinBar(theme, level, level + 1, precise, theme.getBarFill(), theme.getBarFill2(), theme.getBarBackground(), null));
			Map<Skill, Integer> next = CombatLevel.needed(levels, level + 1);
			Map<Skill, Integer> toGoal = aiming ? CombatLevel.needed(levels, goal) : Collections.emptyMap();
			CardLine heading = new CardLine(theme, font).left("Levels needed", theme.getMore())
				.right(aiming ? "next / goal" : "next", theme.getMore());
			lines.add(heading);
			Color goalColor = theme.getGoalLevel() == null ? theme.getBarFill() : theme.getGoalLevel();
			for (Skill skill : CombatLevel.SKILLS)
			{
				CardLine line = new CardLine(theme, font)
					.icon(theme.isSkillIcon() ? skillIconManager.getSkillImage(skill, true) : null)
					.left(skill.getName(), theme.getUnlock())
					.right(neededText(next.get(skill)), next.get(skill) == null ? theme.getMore() : theme.getNextUnlockLevel());
				if (aiming)
				{
					line.right(" / ", theme.getMore())
						.right(neededText(toGoal.get(skill)), toGoal.get(skill) == null ? theme.getMore() : goalColor);
				}
				lines.add(line);
			}
		}
		if (goal > 0)
		{
			Color goalLabel = theme.getGoalLabel() == null ? theme.getBarFill() : theme.getGoalLabel();
			CardLine line = new CardLine(theme, font)
				.left("Goal: ", goalLabel)
				.left(String.valueOf(goal), theme.getGoalLevel() == null ? theme.getBarFill() : theme.getGoalLevel());
			lines.add(aiming ? line.right(String.format(Locale.ROOT, "%.2f to go", goal - precise), theme.getGoalXp())
				: line.right("Reached!", theme.getMet()));
			// from the exact combat level the goal was set at
			double start = combatGoalStart();
			lines.add(goalBar(theme, start < goal ? start : 3, goal, Math.min(precise, goal), theme.getBarFill()));
		}

		int width = (int) (ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize() * theme.widthScale(textSize))
			+ 2 * (theme.getPadding() - PANEL_PADDING);
		for (LayoutableRenderableEntity line : lines)
		{
			if (line instanceof CardLine)
			{
				width = Math.max(width, ((CardLine) line).minWidth() + 2 * theme.getPadding());
			}
		}
		card.setPreferredSize(new Dimension(width, 0));
		TooltipBox box = new TooltipBox(card, theme, Collections.emptyList());
		if (icon != null || !theme.isSkillIcon())
		{
			combatCache = box;
		}
		return box;
	}

	/** The combat level plate for the top of the screen: icon, level and a thin bar to the next level. Client thread. */
	LayoutableRenderableEntity combatPlate()
	{
		if (levels.isEmpty())
		{
			return null;
		}
		TooltipBox cached = plateCache;
		if (cached != null)
		{
			return cached;
		}
		Palette palette = snapshot().getPalette();
		CardTheme theme = CardTheme.pinned(look(CombatLevel.LOOK), palette, theme(palette, false, null), null, true);
		Font font = theme.font(config.tooltipTextSize());
		int level = CombatLevel.level(levels);
		double precise = CombatLevel.precise(levels);

		PanelComponent card = new PanelComponent();
		BufferedImage icon = theme.isSkillIcon() ? spriteManager.getSprite(SpriteID.SideIcons.COMBAT, 0) : null;
		CardLine line = new CardLine(theme, font, true)
			.icon(icon)
			.left(COMBAT_LEVEL, theme.getSkillName())
			.right(String.valueOf(level), theme.getLevelValue());
		card.getChildren().add(line);
		if (level < Experience.MAX_COMBAT_LEVEL)
		{
			card.getChildren().add(new PinBar(theme, level, level + 1, precise, theme.getBarFill(), theme.getBarFill2(),
				theme.getBarBackground(), null));
		}
		card.setPreferredSize(new Dimension(line.minWidth() + 2 * theme.getPadding(), 0));
		TooltipBox box = new TooltipBox(card, theme, Collections.emptyList());
		if (icon != null || !theme.isSkillIcon())
		{
			plateCache = box;
		}
		return box;
	}

	/** "+3" for levels still to get, "-" when 99 isn't enough on its own. */
	static String neededText(Integer levels)
	{
		return levels == null ? "-" : "+" + levels;
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

	/**
	 * Card for a skill's amount goals: for one, the item, what's done of the target, a bar in the skill's color and the
	 * skill; for several, the skill with a line and a bar for each item. A goal still going adds when it should be done,
	 * the inventories left and the bank's amount; a reached one turns the bar the done color after it bursts.
	 */
	private LayoutableRenderableEntity tallyCard(List<String> keys, CardTheme theme, Font font, Font small, int textSize,
		Font regular)
	{
		PanelComponent card = new PanelComponent();
		List<LayoutableRenderableEntity> lines = card.getChildren();
		Tally first = tallies.get(keys.get(0));
		Skill skill = first.getSkill();
		boolean single = keys.size() == 1;
		BufferedImage icon = single ? itemManager.getImage(first.getItem()) : skillIconManager.getSkillImage(skill, false);
		lines.add(new CardHeader(icon, single ? first.getName() : skill.getName() + " goals", theme.getTaskTitle(),
			theme.titleFont(textSize, TASK_TITLE_GROWTH), (double) textSize / regular.getSize(), theme));
		long now = System.currentTimeMillis();
		boolean showBank = config.tallyBank();
		for (String key : keys)
		{
			Tally tally = tallies.get(key);
			int left = tally.getTarget() - tally.getCount();
			Color color = tally.isDone() ? theme.getMet() : theme.getXpValue();
			String what = tally.isDone() ? (single ? "Done!" : tally.getName() + " - done!")
				: single ? Tally.verb(skill) + String.format(Locale.ROOT, " %,d more", left) : tally.getName();
			lines.add(new CardLine(theme, font)
				.left(what, color)
				.right(String.format(Locale.ROOT, "%,d/%,d", tally.getCount(), tally.getTarget()), color));
			Supplier<PinBar.LevelUp> reached = () ->
			{
				Long at = tallyReached.get(key);
				return at == null ? null : new PinBar.LevelUp(at, true);
			};
			Color barColor = Skills.color(skill);
			lines.add(tally.isDone()
				? new PinBar(theme, 0, tally.getTarget(), tally.getTarget(), theme.getMet(), null,
					theme.getGoalBarBackground() == null ? theme.getBarBackground() : theme.getGoalBarBackground(), reached)
				: goalBar(theme, 0, tally.getTarget(), tally.getCount(), barColor, reached));
			Tally.Pace pace = paces.get(key);
			int perHour = pace == null || tally.isDone() ? 0 : pace.perHour(now);
			String speed = Tally.pace(perHour, left);
			Color shade = materialShade(theme.getXpValue(), theme);
			if (single)
			{
				lines.add(new CardMaterial(theme, small, skillIconManager.getSkillImage(skill, true),
					skill.getName() + (tally.isDone() ? " - goal reached" : speed.isEmpty() ? "" : " - " + speed), shade));
			}
			else
			{
				lines.add(new CardMaterial(theme, small, itemManager.getImage(tally.getItem()), tally.isDone() ? "Goal reached"
					: speed.isEmpty() ? Tally.verb(skill) + String.format(Locale.ROOT, " %,d more", left) : speed, shade));
			}
			if (!tally.isDone())
			{
				String outlook = Tally.outlook(perHour, left, itemManager.getItemComposition(tally.getItem()).isStackable(),
					showBank ? banked.getOrDefault(tally.getItem(), 0) : 0, now, ZoneId.systemDefault(), Locale.getDefault());
				if (!outlook.isEmpty())
				{
					lines.add(new CardMaterial(theme, small, null, outlook, shade));
				}
			}
		}
		int width = (int) (ComponentConstants.STANDARD_WIDTH * textSize / regular.getSize() * theme.widthScale(textSize))
			+ 2 * (theme.getPadding() - PANEL_PADDING);
		for (LayoutableRenderableEntity line : lines)
		{
			if (line instanceof CardLine)
			{
				width = Math.max(width, ((CardLine) line).minWidth() + 2 * theme.getPadding());
			}
		}
		card.setPreferredSize(new Dimension(width, 0));
		TooltipBox box = new TooltipBox(card, theme, Collections.emptyList());
		if (icon != null)
		{
			taskCache.put(String.join(TALLY_GROUP, keys), box);
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
		startGoals();
	}

	/**
	 * Keeps the XP each goal was set at, so its bar runs from there to the goal: new goals start at the XP the player has
	 * now, or when the XP is first seen if they were set logged out, and dropped goals are forgotten. Any thread.
	 */
	private synchronized void startGoals()
	{
		Map<Goal, Integer> starts = new LinkedHashMap<>();
		for (Goal goal : goals)
		{
			Integer start = goalStarts.containsKey(goal) ? goalStarts.get(goal) : xpSeen.get(goal.getSkill());
			if (start != null)
			{
				starts.put(goal, start);
			}
		}
		if (!starts.equals(goalStarts))
		{
			goalStarts = Collections.unmodifiableMap(starts);
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "goalStarts", Goal.formatStarts(starts));
		}
		if (config.combatGoal() > 0 && config.combatGoalStart().isEmpty() && levels.keySet().containsAll(CombatLevel.SKILLS))
		{
			configManager.setConfiguration(NextUnlocksConfig.GROUP, "combatGoalStart",
				String.format(Locale.ROOT, "%.4f", CombatLevel.precise(levels)));
		}
	}

	/** Where the combat goal's bar starts: the exact combat level the goal was set at, or 3 when that isn't known. */
	private double combatGoalStart()
	{
		try
		{
			return Double.parseDouble(config.combatGoalStart());
		}
		catch (NumberFormatException e)
		{
			return 3;
		}
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
			questsToDo, finishedQuests, goals, config.almostThere(), pinnedTaskTitles,
			history, config.recentUnlocks(), pinnedTasks.stream().map(tallies::get).filter(Objects::nonNull).collect(Collectors.toList()));
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
		GuidesView view = new GuidesView(snapshot.isLoggedIn(), ironman, guides.all(), followedGuides, guideProgress);
		SwingUtilities.invokeLater(() ->
		{
			target.updateGuides(view);
			target.update(snapshot);
		});
	}
}
