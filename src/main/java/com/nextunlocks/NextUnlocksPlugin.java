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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
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
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Quest;
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
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

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
	private static final int MESSAGE_ENTRIES = 3;
	/** Panel refresh interval for xp-only changes, in game ticks. */
	private static final int PANEL_REFRESH_TICKS = 5;
	/** How often quest states are checked again, in game ticks. */
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final String SECTION_SEPARATOR = ";";
	private static final Map<Integer, Quest> QUESTS = new HashMap<>();

	static
	{
		for (Quest quest : Quest.values())
		{
			QUESTS.put(quest.getId(), quest);
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

	private final UnlockRepository repository = new UnlockRepository();
	/** Real levels as last seen; client thread only. Empty while logged out. */
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<Skill, TooltipBox> tooltipCache = new ConcurrentHashMap<>();

	/** Skills whose tooltip is pinned above the chatbox, in pin order. Client thread. */
	@Getter
	private final Set<Skill> pinned = new LinkedHashSet<>();

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

	@Provides
	NextUnlocksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NextUnlocksConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		repository.load(gson);

		loadHiddenSections();
		panel = new NextUnlocksPanel(repository, skillIconManager, itemManager, spriteManager, this::setSectionHidden);
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
			pushToPanel();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(tooltipOverlay);
		overlayManager.remove(pinnedOverlay);
		skillGuide.shutDown();
		skillGuide = null;
		pinned.clear();
		clientToolbar.removeNavigation(navButton);
		levels.clear();
		tooltipCache.clear();
		unfinishedQuests = Collections.emptySet();
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
				tooltipCache.clear();
				unfinishedQuests = Collections.emptySet();
				pushToPanel();
				break;
			case LOGGED_IN:
				loadSkillGuide();
				membersWorld = client.getWorldType().contains(WorldType.MEMBERS);
				tooltipCache.clear();
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
		tooltipCache.remove(skill);
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
			tooltipCache.clear();
			clientThread.invokeLater(() ->
			{
				if ("greyOutLocked".equals(event.getKey()))
				{
					questsDirty = true;
				}
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
		Integer level = levels.get(skill);
		if (level == null)
		{
			return null;
		}
		TooltipBox cached = tooltipCache.get(skill);
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
		if (config.tooltipXp())
		{
			// the lines of the game's own tooltip, which onBeforeRender hides
			lines.add(line(font, skill.getName() + " XP:", number.format(xp)));
			if (level < Experience.MAX_REAL_LEVEL)
			{
				int nextXp = Experience.getXpForLevel(level + 1);
				lines.add(line(font, "Next level at:", number.format(nextXp)));
				lines.add(line(font, "Remaining XP:", number.format(Math.max(0, nextXp - xp))));
			}
		}
		if (!iconsOnly)
		{
			lines.add(LineComponent.builder()
				.left("Next unlock: " + nextLevel)
				.leftColor(palette.getNext())
				.leftFont(font)
				.right(number.format(xpToGo) + " xp")
				.rightFont(font)
				.build());
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
				icons.getChildren().add(new UnlockTileComponent(image, nextLevel, palette.getLevel(), snapshot.locked(u), scale));
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
		TooltipBox box = new TooltipBox(tooltip, background, config.tooltipOutline() ? palette.getOutline() : null);
		if (complete)
		{
			tooltipCache.put(skill, box);
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
		Skill skill = Skills.forStatsWidget(event.getMenuEntry().getParam1());
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

	/** Adds Pin or Unpin just above Cancel, so it never becomes the left-click action. */
	private void addPinEntry(Skill skill)
	{
		client.getMenu().createMenuEntry(1)
			.setOption(pinned.contains(skill) ? UNPIN : PIN)
			.setTarget(ColorUtil.wrapWithColorTag(skill.getName() + " unlocks", config.colorNext()))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> togglePin(skill));
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
			String message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("New at " + skill.getName() + " " + level + ": ")
				.append(ChatColorType.NORMAL)
				.append(list)
				.build();
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(message)
				.build());
		}
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
		tooltipCache.clear();
		panelDirty = true;
	}

	/** Checks which required quests are unfinished. Client thread, logged in. */
	private void refreshQuests()
	{
		questsDirty = false;
		ticksSinceQuestRefresh = 0;
		Set<Integer> unfinished = new HashSet<>();
		if (config.greyOutLocked())
		{
			for (int id : repository.requiredQuests())
			{
				Quest quest = QUESTS.get(id);
				// quests newer than this RuneLite's list can't be checked; they're left as usable
				if (quest != null && quest.getState(client) != QuestState.FINISHED)
				{
					unfinished.add(id);
				}
			}
		}
		if (!unfinished.equals(unfinishedQuests))
		{
			unfinishedQuests = unfinished;
			tooltipCache.clear();
			panelDirty = true;
		}
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
		return new Snapshot(levelCopy, xpCopy, boosted, filter(), config.levelsAhead(), unfinishedQuests, hiddenSections, Palette.of(config));
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
