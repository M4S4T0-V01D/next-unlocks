package com.nextunlocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ImageUtil;

/**
 * The in-game Next Unlocks menu, drawn like the game's settings window, and the button on the minimap's rim that opens
 * it: the pinned cards' looks, goals, pins and the other settings, so none of it needs the side panel.
 */
class NextUnlocksMenu extends Overlay implements MouseListener, MouseWheelListener, KeyListener
{
	private static final int[] MINIMAPS = {
		InterfaceID.Toplevel.MINIMAP,
		InterfaceID.ToplevelOsrsStretch.MINIMAP,
		InterfaceID.ToplevelPreEoc.MINIMAP,
	};

	/** the same size as VisionHD's menu button, so the two sit on the rim as a pair */
	private static final int RIM_BUTTON = 21;

	/** where the menu was last dragged to, its top left on the canvas; unset centres it */
	static final String MENU_X = "menuX";
	static final String MENU_Y = "menuY";

	private static final int WIDTH = 316;
	private static final int PAD = 8;
	private static final int SKILL_COLUMNS = 8;
	private static final int SKILL_CELL = 34;
	private static final int MAX_ROWS = 8;
	private static final int LABEL = 92;

	private static final Color ORANGE = new Color(0xff981f);
	private static final Color WHITE = new Color(0xffffff);
	private static final Color GREY = new Color(0x9f9f9f);
	private static final Color FILL = new Color(0x3b3226);
	private static final Color INNER = new Color(0x2b261d);
	private static final Color EDGE = new Color(0x0e0e0c);
	private static final Color LIGHT = new Color(0x726451);
	private static final Color DARK = new Color(0x1f1a13);
	private static final Color BUTTON = new Color(0x4a3f31);
	private static final Color HOVER = new Color(0x5c4e3c);
	private static final Color ON = new Color(0x7a5a22);

	/** glow colours to pick from, all a little see-through like the default */
	static final Color[] GLOWS = {
		new Color(64, 224, 255, 220),
		new Color(255, 64, 200, 220),
		new Color(80, 255, 120, 220),
		new Color(255, 200, 60, 220),
		new Color(255, 140, 40, 220),
		new Color(255, 70, 70, 220),
		new Color(170, 110, 255, 220),
		new Color(255, 255, 255, 220),
	};

	/** the pinned cards' look, which the Looks tab's reset puts back, with every skill's own */
	static final String[] LOOK_KEYS;

	static
	{
		List<String> keys = new ArrayList<>(Look.KEYS.keySet());
		keys.add("tooltipIconSize");
		keys.add("tooltipTextSize");
		keys.add("tooltipOutline");
		keys.add("skillLooks");
		LOOK_KEYS = keys.toArray(new String[0]);
	}

	enum Tab
	{
		LOOKS("Looks"),
		GOALS("Goals"),
		PINS("Pins"),
		OPTIONS("Options");

		final String label;

		Tab(String label)
		{
			this.label = label;
		}
	}

	/** the parts of the Looks tab */
	enum LookTab
	{
		STYLE("Style"),
		FRAME("Frame"),
		EFFECTS("Effects"),
		PRESETS("Presets");

		final String label;

		LookTab(String label)
		{
			this.label = label;
		}
	}

	/** something on screen that takes a click: a button, or a slider when {@code slider} is set */
	private static final class Hit
	{
		final Rectangle bounds;
		final Runnable press;
		final Slider slider;

		Hit(Rectangle bounds, Runnable press, Slider slider)
		{
			this.bounds = bounds;
			this.press = press;
			this.slider = slider;
		}
	}

	private static final class Slider
	{
		final String name;
		final String key;
		final int min;
		final int max;
		final IntSupplier value;
		Rectangle track;

		Slider(String name, String key, int min, int max, IntSupplier value)
		{
			this.name = name;
			this.key = key;
			this.min = min;
			this.max = max;
			this.value = value;
		}
	}

	private static final class Toggle
	{
		final String name;
		final String key;
		final BooleanSupplier on;

		Toggle(String name, String key, BooleanSupplier on)
		{
			this.name = name;
			this.key = key;
			this.on = on;
		}
	}

	/** a setting with a few named values, stepped through with arrows */
	private static final class Choice
	{
		final String name;
		final String key;
		final Enum<?>[] values;
		final Supplier<Enum<?>> value;

		Choice(String name, String key, Enum<?>[] values, Supplier<Enum<?>> value)
		{
			this.name = name;
			this.key = key;
			this.values = values;
			this.value = value;
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final NextUnlocksConfig config;
	private final NextUnlocksPlugin plugin;
	private final SkillIconManager skillIcons;
	private final TooltipManager tooltipManager;
	private final BufferedImage icon;

	private final List<Choice> looks = new ArrayList<>();
	private final List<Slider> lookSliders = new ArrayList<>();
	private final List<Slider> sizeSliders = new ArrayList<>();
	private final List<Toggle> lookToggles = new ArrayList<>();
	private final List<Choice> frames = new ArrayList<>();
	private final Slider opacity;
	private final List<Choice> effects = new ArrayList<>();
	private final List<Toggle> goalToggles = new ArrayList<>();
	private final List<Toggle> pinToggles = new ArrayList<>();
	private final List<Toggle> combatToggles = new ArrayList<>();
	private final List<Toggle> options = new ArrayList<>();
	private final List<Toggle> guide = new ArrayList<>();
	private final Choice members;
	private final Slider levelsAhead;
	private final Slider buttonAngle;

	private volatile boolean open;
	private volatile Tab tab = Tab.LOOKS;
	/** the skill picked on the Goals tab to add a goal for, or null */
	private volatile Skill picking;
	/** the skill whose cards the Looks tab changes, or null for every card */
	/** The cards the Looks tab changes: null for all, a skill's name, or {@link CombatLevel#LOOK}. */
	private volatile String target;
	private volatile boolean choosingTarget;
	private volatile LookTab lookTab = LookTab.STYLE;
	/** each tab's height as last drawn, the window's size before it is drawn again */
	private final int[] heights = new int[Tab.values().length];
	// the reset asks for a second click within a few seconds
	private long resetArmedUntil;
	/** what the last frame drew that takes clicks, read by the mouse handlers on the AWT thread */
	private volatile List<Hit> hits = Collections.emptyList();
	private volatile Rectangle panel;
	private volatile Slider dragging;
	private volatile int dragged = Integer.MIN_VALUE;
	private volatile boolean swallow;
	/** the menu's position while it is dragged, or saved; {@link Integer#MIN_VALUE} when it is centred */
	private volatile int menuX = Integer.MIN_VALUE;
	private volatile int menuY = Integer.MIN_VALUE;
	private volatile boolean moving;
	private volatile int grabX;
	private volatile int grabY;

	@Inject
	NextUnlocksMenu(Client client, ClientThread clientThread, ConfigManager configManager, NextUnlocksConfig config,
		NextUnlocksPlugin plugin, SkillIconManager skillIcons, TooltipManager tooltipManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.plugin = plugin;
		this.skillIcons = skillIcons;
		this.tooltipManager = tooltipManager;
		Integer sx = configManager.getConfiguration(NextUnlocksConfig.GROUP, MENU_X, Integer.class);
		Integer sy = configManager.getConfiguration(NextUnlocksConfig.GROUP, MENU_Y, Integer.class);
		if (sx != null && sy != null)
		{
			menuX = sx;
			menuY = sy;
		}
		icon = ImageUtil.loadImageResource(NextUnlocksMenu.class, "icon.png");
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);

		NextUnlocksConfig.StyleToggle[] toggle = NextUnlocksConfig.StyleToggle.values();
		looks.add(lookChoice("Style", "pinStyle", NextUnlocksConfig.CardStyle.values()));
		looks.add(lookChoice("Font", "pinFont", NextUnlocksConfig.CardFont.values()));
		looks.add(lookChoice("Progress bar", "pinBar", NextUnlocksConfig.BarStyle.values()));
		looks.add(lookChoice("Glow", "pinGlow", NextUnlocksConfig.GlowMode.values()));
		looks.add(lookChoice("Text shadow", "pinTextShadow", toggle));
		looks.add(lookChoice("Scanlines", "pinScanlines", toggle));
		lookSliders.add(new Slider("Glow size", "pinGlowSize", 1, 8, () -> plugin.look(target).<Integer>get("pinGlowSize")));
		sizeSliders.add(new Slider("Text size", "tooltipTextSize", 10, 32, config::tooltipTextSize));
		sizeSliders.add(new Slider("Icon size", "tooltipIconSize", 50, 200, config::tooltipIconSize));
		lookToggles.add(new Toggle("Own colours", "pinCustomColors", () -> plugin.look(target).<Boolean>get("pinCustomColors")));
		lookToggles.add(new Toggle("Skill icon", "pinSkillIcon", () -> plugin.look(target).<Boolean>get("pinSkillIcon")));
		lookToggles.add(new Toggle("Outline", "tooltipOutline", config::tooltipOutline));

		frames.add(lookChoice("Background fade", "pinBackgroundGradient", toggle));
		frames.add(lookChoice("Bar fade", "pinBarGradient", toggle));
		frames.add(lookChoice("Title band", "pinHeaderBand", toggle));
		frames.add(lookChoice("Shimmer", "pinShimmer", toggle));
		frames.add(lookChoice("Frame", "pinFrame", NextUnlocksConfig.FrameStyle.values()));
		frames.add(lookChoice("Border", "pinBorderWidth", NextUnlocksConfig.BorderWidth.values()));
		frames.add(lookChoice("Border motion", "pinBorderMotion", NextUnlocksConfig.BorderMotion.values()));
		frames.add(lookChoice("Corners", "pinCorner", NextUnlocksConfig.Corner.values()));
		frames.add(lookChoice("Padding", "pinPadding", NextUnlocksConfig.Padding.values()));
		opacity = new Slider("Opacity", "tooltipOpacity", 0, 100, () -> plugin.look(target).<Integer>get("tooltipOpacity"));

		effects.add(lookChoice("XP effect", "pinXpEffect", NextUnlocksConfig.XpEffect.values()));
		effects.add(lookChoice("Ambient", "pinAmbient", NextUnlocksConfig.Ambient.values()));
		effects.add(lookChoice("XP popup", "pinXpPopup", toggle));
		effects.add(lookChoice("Bar pulse", "pinXpPulse", toggle));
		effects.add(lookChoice("Level-up flash", "pinLevelUp", toggle));

		goalToggles.add(new Toggle("Infoboxes", "goalInfoboxes", config::goalInfoboxes));
		goalToggles.add(new Toggle("Almost there", "almostThere", config::almostThere));
		goalToggles.add(new Toggle("Item making levels", "itemMakingLevels", config::itemMakingLevels));

		pinToggles.add(new Toggle("Skill and level", "pinnedLevel", config::pinnedLevel));
		pinToggles.add(new Toggle("Goal", "pinnedGoal", config::pinnedGoal));
		combatToggles.add(new Toggle("Pinned card", "pinnedCombat", config::pinnedCombat));
		combatToggles.add(new Toggle("Top of screen", "combatTop", config::combatTop));

		options.add(new Toggle("Quests", "showQuests", config::showQuests));
		options.add(new Toggle("Diaries", "showDiaries", config::showDiaries));
		options.add(new Toggle("Level-up message", "levelUpMessage", config::levelUpMessage));
		options.add(new Toggle("Count boosts", "countBoosts", config::countBoosts));
		options.add(new Toggle("Grey out locked", "greyOutLocked", config::greyOutLocked));
		options.add(new Toggle("Message shadow", "chatShadow", config::chatShadow));
		options.add(new Toggle("Skill tooltip", "skillTooltip", config::skillTooltip));
		options.add(new Toggle("Tooltip icons only", "tooltipIconsOnly", config::tooltipIconsOnly));
		options.add(new Toggle("Tooltip XP", "tooltipXp", config::tooltipXp));
		options.add(new Toggle("Tooltip progress", "tooltipProgress", config::tooltipProgress));

		guide.add(new Toggle("Next Unlocks tab", "guideTab", config::guideTab));
		guide.add(new Toggle("Highlight next", "guideHighlight", config::guideHighlight));
		guide.add(new Toggle("Fade later", "guideFadeAhead", config::guideFadeAhead));
		guide.add(new Toggle("Boosted and locked", "guideMarkings", config::guideMarkings));
		guide.add(new Toggle("Progress bar", "guideProgress", config::guideProgress));
		guide.add(new Toggle("Scroll to next", "guideScroll", config::guideScroll));
		guide.add(new Toggle("Dim hidden sections", "guideDimSections", config::guideDimSections));

		members = new Choice("Members", "membersContent", NextUnlocksConfig.MembersContent.values(), config::membersContent);
		levelsAhead = new Slider("Levels to show", "levelsAhead", 1, 20, config::levelsAhead);
		buttonAngle = new Slider("Button position", "minimapButtonPosition", 0, 359, config::minimapButtonAngle);
	}

	/** a pinned card look setting, read from the cards being changed */
	private Choice lookChoice(String name, String key, Enum<?>[] values)
	{
		return new Choice(name, key, values, () -> plugin.look(target).<Enum<?>>get(key));
	}

	/** look settings go to the picked skill's own look when there is one, the rest to the config */
	private void set(String key, Object value)
	{
		String cards = target;
		if (cards != null && Look.KEYS.containsKey(key))
		{
			plugin.setLook(cards, key, Look.format(value));
			return;
		}
		configManager.setConfiguration(NextUnlocksConfig.GROUP, key, value);
	}

	/** the value {@code step} along from the current one, wrapping round at either end */
	static Enum<?> step(Enum<?>[] values, Enum<?> current, int step)
	{
		int i = current == null ? 0 : current.ordinal();
		return values[Math.floorMod(i + step, values.length)];
	}

	/** where on a minimap {@code r} across the rim button sits, its top left, at {@code degrees} clockwise from the right */
	static Rectangle rimButton(double cx, double cy, double r, int size, double degrees)
	{
		double rad = Math.toRadians(degrees);
		// centred just past the rim's edge, the same distance out as VisionHD's menu button
		double along = r + 3;
		return new Rectangle((int) Math.round(cx + Math.cos(rad) * along - size / 2.0),
			(int) Math.round(cy + Math.sin(rad) * along - size / 2.0), size, size);
	}

	/** a saved position along one side of the canvas, pulled back so a menu {@code size} long stays on it */
	static int onCanvas(int saved, int size, int span)
	{
		return Math.max(0, Math.min(saved, span - size));
	}

	/** the value a slider takes with the mouse at {@code x} along a track from {@code left}, {@code width} wide */
	static int sliderValue(int x, int left, int width, int min, int max)
	{
		double t = Math.max(0, Math.min(1, (x - left) / (double) Math.max(1, width - 1)));
		return min + (int) Math.round(t * (max - min));
	}

	/** the XP left to a level, written short: 950, 12.3k, 1.25m */
	static String xpLeft(int xp)
	{
		if (xp < 10_000)
		{
			return xp + " xp";
		}
		if (xp < 1_000_000)
		{
			return String.format("%.1fk xp", xp / 1000.0);
		}
		return String.format("%.2fm xp", xp / 1_000_000.0);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<Hit> out = new ArrayList<>();
		Point mouse = client.getMouseCanvasPosition();
		if (config.minimapButton())
		{
			Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawRimButton(g, mouse, out);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
		}
		if (open)
		{
			drawMenu(g, mouse, out);
		}
		else
		{
			panel = null;
		}
		hits = out;
		return null;
	}

	private Widget minimap()
	{
		for (int id : MINIMAPS)
		{
			Widget w = client.getWidget(id);
			if (w != null && !w.isHidden())
			{
				return w;
			}
		}
		return null;
	}

	private void drawRimButton(Graphics2D g, Point mouse, List<Hit> out)
	{
		Widget map = minimap();
		if (map == null)
		{
			return;
		}
		Rectangle b = map.getBounds();
		Rectangle r = rimButton(b.getCenterX(), b.getCenterY(), Math.min(b.width, b.height) / 2.0, RIM_BUTTON,
			config.minimapButtonAngle());
		boolean hover = contains(r, mouse);
		g.setColor(new Color(0, 0, 0, 120));
		g.fillOval(r.x, r.y + 1, r.width, r.height);
		g.setColor(hover ? HOVER : FILL);
		g.fillOval(r.x, r.y, r.width, r.height);
		g.setColor(EDGE);
		g.drawOval(r.x, r.y, r.width - 1, r.height - 1);
		// VisionHD's look: an orange ring inside the edge, white while hovered or open, and the icon at its own size
		g.setColor(open || hover ? WHITE : ORANGE);
		g.drawOval(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
		g.drawImage(icon, r.x + (RIM_BUTTON - icon.getWidth()) / 2, r.y + (RIM_BUTTON - icon.getHeight()) / 2, null);
		if (hover && !open)
		{
			tooltipManager.add(new Tooltip("Next Unlocks"));
		}
		out.add(new Hit(r, () -> open = !open, null));
	}

	private void drawMenu(Graphics2D g, Point mouse, List<Hit> out)
	{
		Tab shown = tab;
		int height = heights[shown.ordinal()] > 0 ? heights[shown.ordinal()] : 300;
		int x;
		int y;
		if (menuX != Integer.MIN_VALUE)
		{
			// where it was left, kept on the canvas if the window has shrunk since
			x = onCanvas(menuX, WIDTH, client.getCanvasWidth());
			y = onCanvas(menuY, height, client.getCanvasHeight());
		}
		else
		{
			x = client.getViewportXOffset() + (client.getViewportWidth() - WIDTH) / 2;
			y = client.getViewportYOffset() + Math.max(4, (client.getViewportHeight() - height) / 2);
			// fixed mode's viewport is shorter than the menu, so keep it on the canvas instead
			y = Math.max(4, Math.min(y, client.getCanvasHeight() - height - 4));
		}
		Rectangle box = new Rectangle(x, y, WIDTH, height);
		panel = box;

		bevel(g, box, FILL, LIGHT, DARK);
		// the title bar moves the menu; a double click puts it back in the middle
		Rectangle title = new Rectangle(x, y, WIDTH - 24, 24);
		if (moving || contains(title, mouse))
		{
			g.setColor(HOVER);
			g.fillRect(x + 2, y + 2, WIDTH - 26, 21);
		}
		out.add(new Hit(title, null, null));
		g.setFont(FontManager.getRunescapeBoldFont());
		text(g, "Next Unlocks", x + WIDTH / 2 - g.getFontMetrics().stringWidth("Next Unlocks") / 2, y + 17, ORANGE);
		button(g, mouse, out, new Rectangle(x + WIDTH - 22, y + 4, 17, 17), "X", false, () -> open = false);
		g.setColor(DARK);
		g.drawLine(x + 4, y + 24, x + WIDTH - 5, y + 24);

		int left = x + PAD;
		int inner = WIDTH - 2 * PAD;
		int cy = y + 30;
		g.setFont(FontManager.getRunescapeFont());
		Tab[] tabs = Tab.values();
		int tw = (inner - (tabs.length - 1) * 4) / tabs.length;
		for (int i = 0; i < tabs.length; i++)
		{
			Tab t = tabs[i];
			button(g, mouse, out, new Rectangle(left + i * (tw + 4), cy, tw, 18), t.label, t == shown, () ->
			{
				tab = t;
				picking = null;
			});
		}
		cy += 24;

		switch (shown)
		{
			case LOOKS:
				cy = drawLooks(g, mouse, out, left, cy, inner);
				break;
			case GOALS:
				cy = drawGoals(g, mouse, out, left, cy, inner);
				break;
			case PINS:
				cy = drawPins(g, mouse, out, left, cy, inner);
				break;
			default:
				cy = drawOptions(g, mouse, out, left, cy, inner);
				break;
		}

		g.setFont(FontManager.getRunescapeSmallFont());
		String foot = "Every setting is also in the RuneLite config";
		text(g, foot, x + WIDTH / 2 - g.getFontMetrics().stringWidth(foot) / 2, cy + 10, GREY);
		// the window fits what was drawn from the next frame on
		heights[shown.ordinal()] = cy + 18 - y;
	}

	private int drawLooks(Graphics2D g, Point mouse, List<Hit> out, int left, int cy, int inner)
	{
		String skill = target;
		String name = cardsName(skill);
		cy = heading(g, "Cards to change", left, cy);
		String who = skill == null ? name : name + (plugin.hasOwnLook(skill) ? " (own look)" : "");
		button(g, mouse, out, new Rectangle(left, cy, inner, 18), choosingTarget ? "Pick below" : who, choosingTarget,
			() -> choosingTarget = !choosingTarget);
		cy += 22;
		if (choosingTarget)
		{
			button(g, mouse, out, new Rectangle(left, cy, inner, 18), "All skills", skill == null, () ->
			{
				target = null;
				choosingTarget = false;
			});
			cy += 22;
			button(g, mouse, out, new Rectangle(left, cy, inner, 18), "Combat level", CombatLevel.LOOK.equals(skill), () ->
			{
				target = CombatLevel.LOOK;
				choosingTarget = false;
			});
			cy += 22;
			cy = skillGrid(g, mouse, out, left, cy, s -> s.name().equals(target), s ->
			{
				target = s.name();
				choosingTarget = false;
			});
		}

		LookTab[] tabs = LookTab.values();
		LookTab part = lookTab;
		int tw = (inner - (tabs.length - 1) * 4) / tabs.length;
		for (int i = 0; i < tabs.length; i++)
		{
			LookTab t = tabs[i];
			button(g, mouse, out, new Rectangle(left + i * (tw + 4), cy, tw, 18), t.label, t == part, () -> lookTab = t);
		}
		cy += 24;

		switch (part)
		{
			case STYLE:
				cy = choices(g, mouse, out, looks, left, cy, inner);
				cy = glowColours(g, mouse, out, left, cy, inner);
				for (Slider s : lookSliders)
				{
					slider(g, mouse, out, s, left, cy, inner);
					cy += 20;
				}
				if (skill == null)
				{
					// sizes and the outline are for every card
					for (Slider s : sizeSliders)
					{
						slider(g, mouse, out, s, left, cy, inner);
						cy += 20;
					}
					cy = toggles(g, mouse, out, lookToggles, left, cy, inner, 2);
				}
				else
				{
					cy = toggles(g, mouse, out, lookToggles.subList(0, 2), left, cy, inner, 2);
				}
				break;
			case FRAME:
				cy = choices(g, mouse, out, frames, left, cy, inner);
				slider(g, mouse, out, opacity, left, cy, inner);
				cy += 20;
				break;
			case EFFECTS:
				cy = choices(g, mouse, out, effects, left, cy, inner);
				g.setFont(FontManager.getRunescapeSmallFont());
				text(g, "Skill's own picks sparks, leaves, bubbles and more to suit", left, cy + 10, GREY);
				g.setFont(FontManager.getRunescapeFont());
				cy += 16;
				break;
			default:
				return drawPresets(g, mouse, out, left, cy, inner, skill);
		}

		cy += 2;
		if (skill != null)
		{
			button(g, mouse, out, new Rectangle(left, cy, inner, 18), "Give " + name + " the same look as the rest",
				false, () -> plugin.resetLook(skill));
			return cy + 22;
		}
		boolean armed = System.currentTimeMillis() < resetArmedUntil;
		button(g, mouse, out, new Rectangle(left, cy, inner, 18), armed ? "Click again to reset every look" : "Reset looks",
			armed, () ->
			{
				if (System.currentTimeMillis() < resetArmedUntil)
				{
					resetArmedUntil = 0;
					for (String key : LOOK_KEYS)
					{
						configManager.unsetConfiguration(NextUnlocksConfig.GROUP, key);
					}
				}
				else
				{
					resetArmedUntil = System.currentTimeMillis() + 3000;
				}
			});
		return cy + 22;
	}

	private int choices(Graphics2D g, Point mouse, List<Hit> out, List<Choice> list, int left, int cy, int inner)
	{
		for (Choice c : list)
		{
			choice(g, mouse, out, c, left, cy, inner);
			cy += 20;
		}
		return cy;
	}

	private int glowColours(Graphics2D g, Point mouse, List<Hit> out, int left, int cy, int inner)
	{
		text(g, "Glow colour", left, cy + 13, ORANGE);
		int sw = (inner - LABEL - (GLOWS.length - 1) * 3) / GLOWS.length;
		Color current = plugin.look(target).get("pinGlowColor");
		for (int i = 0; i < GLOWS.length; i++)
		{
			Color c = GLOWS[i];
			Rectangle r = new Rectangle(left + LABEL + i * (sw + 3), cy + 2, sw, 14);
			boolean picked = current != null && current.getRGB() == c.getRGB();
			bevel(g, r, new Color(c.getRGB() & 0xffffff), picked ? WHITE : LIGHT, picked ? WHITE : DARK);
			if (picked || contains(r, mouse))
			{
				g.setColor(WHITE);
				g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
			}
			out.add(new Hit(r, () -> set("pinGlowColor", c), null));
		}
		return cy + 20;
	}

	/** saved looks: load one onto the cards being changed, rename or delete it, or save the current one */
	/** "All skills", the skill's name, or "Combat level" for the Looks tab's cards. */
	private static String cardsName(String cards)
	{
		if (cards == null)
		{
			return "All skills";
		}
		if (CombatLevel.LOOK.equals(cards))
		{
			return "Combat level";
		}
		try
		{
			return Skill.valueOf(cards).getName();
		}
		catch (IllegalArgumentException e)
		{
			return cards;
		}
	}

	private int drawPresets(Graphics2D g, Point mouse, List<Hit> out, int left, int cy, int inner, String skill)
	{
		List<String> presets = plugin.presets();
		if (presets.isEmpty())
		{
			text(g, "None yet. Save a look to use it again.", left, cy + 13, GREY);
			cy += 18;
		}
		for (int i = 0; i < presets.size() && i < MAX_ROWS; i++)
		{
			String name = presets.get(i);
			int load = 40;
			int rename = 54;
			int end = left + inner - 17 - rename - load - 9;
			text(g, fit(g, name, end - left), left, cy + 13, ORANGE);
			button(g, mouse, out, new Rectangle(end + 3, cy + 1, load, 16), "Load", false, () -> plugin.loadPreset(name, skill));
			button(g, mouse, out, new Rectangle(end + 6 + load, cy + 1, rename, 16), "Rename", false,
				() -> plugin.askRenamePreset(name));
			button(g, mouse, out, new Rectangle(left + inner - 17, cy + 1, 17, 16), "X", false, () -> plugin.deletePreset(name));
			cy += 20;
		}
		if (presets.size() > MAX_ROWS)
		{
			text(g, "and " + (presets.size() - MAX_ROWS) + " more, in the RuneLite config", left, cy + 13, GREY);
			cy += 18;
		}
		String save = skill == null ? "Save the cards' look..." : "Save " + cardsName(skill) + "'s look...";
		button(g, mouse, out, new Rectangle(left, cy + 2, inner, 18), save, false, () -> plugin.askSavePreset(skill, cardsName(skill)));
		cy += 24;
		g.setFont(FontManager.getRunescapeSmallFont());
		text(g, skill == null ? "Load sets every card's look" : "Load sets " + cardsName(skill) + "'s own look", left, cy + 10, GREY);
		g.setFont(FontManager.getRunescapeFont());
		return cy + 16;
	}

	private int drawGoals(Graphics2D g, Point mouse, List<Hit> out, int left, int cy, int inner)
	{
		cy = heading(g, "Goals", left, cy);
		List<Goal> goals = plugin.goals();
		if (goals.isEmpty())
		{
			text(g, "None yet. Pick a skill below to set one.", left, cy + 13, GREY);
			cy += 18;
		}
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		for (int i = 0; i < goals.size() && i < MAX_ROWS; i++)
		{
			Goal goal = goals.get(i);
			String right = null;
			if (loggedIn)
			{
				int xp = Experience.getXpForLevel(Math.max(1, Math.min(goal.getLevel(), Experience.MAX_VIRT_LEVEL)))
					- client.getSkillExperience(goal.getSkill());
				right = xp > 0 ? xpLeft(xp) : "Done";
			}
			boolean levelOnly = goal.equals(Goal.level(goal.getSkill(), goal.getLevel()));
			String label = goal.getSkill().getName() + " " + goal.getLevel() + (levelOnly ? "" : ": " + goal.getText());
			row(g, mouse, out, plugin.goalImage(goal), label, right, left, cy, inner, () -> plugin.toggleGoal(goal));
			cy += 20;
		}
		if (goals.size() > MAX_ROWS)
		{
			text(g, "and " + (goals.size() - MAX_ROWS) + " more", left, cy + 13, GREY);
			cy += 18;
		}

		cy = heading(g, "Set a goal", left, cy + 2);
		Skill skill = picking;
		cy = skillGrid(g, mouse, out, left, cy, s -> s == picking, s -> picking = picking == s ? null : s);
		if (skill != null)
		{
			Integer level = plugin.level(skill);
			if (level == null)
			{
				text(g, "Log in to set a goal.", left, cy + 13, GREY);
				cy += 18;
			}
			else
			{
				for (Goal goal : plugin.goalChoices(skill))
				{
					boolean levelOnly = goal.equals(Goal.level(skill, goal.getLevel()));
					String label = "Level " + goal.getLevel() + (levelOnly ? "" : ": " + goal.getText());
					button(g, mouse, out, new Rectangle(left, cy, inner, 18), fit(g, label, inner - 8), false,
						() -> plugin.toggleGoal(goal));
					cy += 22;
				}
				if (level < Experience.MAX_REAL_LEVEL)
				{
					button(g, mouse, out, new Rectangle(left, cy, inner, 18), "Choose a level...", false,
						() -> plugin.askGoalLevel(skill, level));
					cy += 22;
				}
			}
		}
		return toggles(g, mouse, out, goalToggles, left, cy + 2, inner, 2);
	}

	private int drawPins(Graphics2D g, Point mouse, List<Hit> out, int left, int cy, int inner)
	{
		cy = heading(g, "Pinned skills", left, cy);
		cy = skillGrid(g, mouse, out, left, cy, s -> plugin.getPinned().contains(s), plugin::togglePin);

		cy = heading(g, "Pinned quests, diaries and items", left, cy + 2);
		List<String> tasks = new ArrayList<>(plugin.getPinnedTasks());
		if (tasks.isEmpty())
		{
			text(g, "None yet.", left, cy + 13, GREY);
			cy += 18;
		}
		for (int i = 0; i < tasks.size() && i < MAX_ROWS; i++)
		{
			String wiki = tasks.get(i);
			row(g, mouse, out, null, plugin.taskName(wiki), null, left, cy, inner, () -> plugin.toggleTaskLater(wiki));
			cy += 20;
		}
		if (tasks.size() > MAX_ROWS)
		{
			text(g, "and " + (tasks.size() - MAX_ROWS) + " more", left, cy + 13, GREY);
			cy += 18;
		}
		button(g, mouse, out, new Rectangle(left, cy + 2, inner, 18), "Pin a quest, diary or item...", false,
			plugin::askPinTask);
		cy += 26;
		cy = heading(g, "Combat level", left, cy);
		cy = toggles(g, mouse, out, combatToggles, left, cy, inner, 2);
		cy = heading(g, "Skill cards show", left, cy + 2);
		return toggles(g, mouse, out, pinToggles, left, cy, inner, 2);
	}

	private int drawOptions(Graphics2D g, Point mouse, List<Hit> out, int left, int cy, int inner)
	{
		cy = heading(g, "Unlocks", left, cy);
		choice(g, mouse, out, members, left, cy, inner);
		cy += 20;
		slider(g, mouse, out, levelsAhead, left, cy, inner);
		cy += 20;
		cy = toggles(g, mouse, out, options, left, cy, inner, 2);
		cy = heading(g, "Skill guide", left, cy + 2);
		cy = toggles(g, mouse, out, guide, left, cy, inner, 2);
		cy = heading(g, "Minimap button", left, cy + 2);
		slider(g, mouse, out, buttonAngle, left, cy, inner);
		cy += 20;
		Toggle button = new Toggle("Show the button (the RuneLite config brings it back)", "minimapButton",
			config::minimapButton);
		return toggles(g, mouse, out, Collections.singletonList(button), left, cy, inner, 1);
	}

	/** checkboxes, {@code columns} across; the y under them */
	private int toggles(Graphics2D g, Point mouse, List<Hit> out, List<Toggle> list, int left, int cy, int inner, int columns)
	{
		int w = inner / columns;
		for (int i = 0; i < list.size(); i++)
		{
			checkbox(g, mouse, out, left + (i % columns) * w, cy + (i / columns) * 18, w - 4, list.get(i));
		}
		return cy + ((list.size() + columns - 1) / columns) * 18;
	}

	/** every skill's icon, lit when {@code lit}, {@code press} run with the skill clicked; the y under it */
	private int skillGrid(Graphics2D g, Point mouse, List<Hit> out, int left, int cy,
		java.util.function.Predicate<Skill> lit, java.util.function.Consumer<Skill> press)
	{
		int gap = (WIDTH - 2 * PAD - SKILL_COLUMNS * SKILL_CELL) / Math.max(1, SKILL_COLUMNS - 1);
		int i = 0;
		for (Skill skill : Skills.STATS_TAB.keySet())
		{
			Rectangle r = new Rectangle(left + (i % SKILL_COLUMNS) * (SKILL_CELL + gap), cy + (i / SKILL_COLUMNS) * 26,
				SKILL_CELL, 24);
			boolean on = lit.test(skill);
			boolean hover = contains(r, mouse);
			bevel(g, r, on ? ON : hover ? HOVER : BUTTON, on ? DARK : LIGHT, on ? LIGHT : DARK);
			BufferedImage image = skillIcons.getSkillImage(skill, true);
			if (image != null)
			{
				g.drawImage(image, r.x + (r.width - image.getWidth()) / 2, r.y + (r.height - image.getHeight()) / 2, null);
			}
			if (hover)
			{
				tooltipManager.add(new Tooltip(skill.getName()));
			}
			out.add(new Hit(r, () -> press.accept(skill), null));
			i++;
		}
		return cy + ((i + SKILL_COLUMNS - 1) / SKILL_COLUMNS) * 26 + 2;
	}

	/** a line of a list: a picture, its text cut to fit, grey text on the right, and an X that runs {@code remove} */
	private void row(Graphics2D g, Point mouse, List<Hit> out, BufferedImage image, String label, String right, int left,
		int cy, int inner, Runnable remove)
	{
		int x = left;
		if (image != null && image.getHeight() > 0)
		{
			int w = Math.max(1, image.getWidth() * 16 / image.getHeight());
			Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(image, x, cy + 1, Math.min(w, 20), 16, null);
			if (interpolation != null)
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			}
			x += 22;
		}
		int end = left + inner - 22;
		if (right != null)
		{
			Font f = g.getFont();
			g.setFont(FontManager.getRunescapeSmallFont());
			int rw = g.getFontMetrics().stringWidth(right);
			text(g, right, end - rw, cy + 13, GREY);
			g.setFont(f);
			end -= rw + 6;
		}
		text(g, fit(g, label, end - x), x, cy + 13, ORANGE);
		button(g, mouse, out, new Rectangle(left + inner - 17, cy + 1, 17, 16), "X", false, remove);
	}

	private void choice(Graphics2D g, Point mouse, List<Hit> out, Choice c, int left, int cy, int inner)
	{
		text(g, c.name, left, cy + 13, ORANGE);
		Enum<?> value = c.value.get();
		Rectangle back = new Rectangle(left + LABEL, cy + 1, 17, 16);
		Rectangle next = new Rectangle(left + inner - 17, cy + 1, 17, 16);
		button(g, mouse, out, back, "<", false, () -> set(c.key, step(c.values, c.value.get(), -1)));
		button(g, mouse, out, next, ">", false, () -> set(c.key, step(c.values, c.value.get(), 1)));
		Rectangle field = new Rectangle(back.x + back.width + 3, cy + 1, next.x - back.x - back.width - 6, 16);
		bevel(g, field, INNER, DARK, LIGHT);
		String shown = fit(g, String.valueOf(value), field.width - 6);
		FontMetrics fm = g.getFontMetrics();
		text(g, shown, field.x + (field.width - fm.stringWidth(shown)) / 2, cy + 13, WHITE);
		// a click on the value steps forward too
		out.add(new Hit(field, () -> set(c.key, step(c.values, c.value.get(), 1)), null));
	}

	private static String fit(Graphics2D g, String s, int width)
	{
		return CardMaterial.fit(g.getFontMetrics(), s, Math.max(1, width));
	}

	private int heading(Graphics2D g, String name, int x, int y)
	{
		Font f = g.getFont();
		g.setFont(FontManager.getRunescapeBoldFont());
		text(g, name, x, y + 12, ORANGE);
		g.setFont(f);
		return y + 18;
	}

	private void button(Graphics2D g, Point mouse, List<Hit> out, Rectangle r, String label, boolean on, Runnable press)
	{
		boolean hover = contains(r, mouse);
		bevel(g, r, on ? ON : hover ? HOVER : BUTTON, on ? DARK : LIGHT, on ? LIGHT : DARK);
		FontMetrics fm = g.getFontMetrics();
		text(g, label, r.x + (r.width - fm.stringWidth(label)) / 2, r.y + (r.height + fm.getAscent()) / 2 - 2,
			hover || on ? WHITE : ORANGE);
		out.add(new Hit(r, press, null));
	}

	private void checkbox(Graphics2D g, Point mouse, List<Hit> out, int x, int y, int width, Toggle t)
	{
		Rectangle r = new Rectangle(x, y, width, 16);
		boolean hover = contains(r, mouse);
		Rectangle box = new Rectangle(x, y + 2, 12, 12);
		bevel(g, box, INNER, DARK, LIGHT);
		if (t.on.getAsBoolean())
		{
			Stroke s = g.getStroke();
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(ORANGE);
			g.drawLine(box.x + 3, box.y + 6, box.x + 5, box.y + 9);
			g.drawLine(box.x + 5, box.y + 9, box.x + 9, box.y + 3);
			g.setStroke(s);
		}
		text(g, fit(g, t.name, width - 17), x + 17, y + 13, hover ? WHITE : ORANGE);
		out.add(new Hit(r, () -> set(t.key, !t.on.getAsBoolean()), null));
	}

	private void slider(Graphics2D g, Point mouse, List<Hit> out, Slider s, int x, int y, int width)
	{
		int number = 30;
		int value = s.value.getAsInt();
		text(g, s.name, x, y + 13, ORANGE);
		Rectangle track = new Rectangle(x + LABEL, y + 6, width - LABEL - number, 5);
		s.track = track;
		bevel(g, track, INNER, DARK, LIGHT);
		double t = (value - s.min) / (double) Math.max(1, s.max - s.min);
		t = Math.max(0, Math.min(1, t));
		g.setColor(ON);
		g.fillRect(track.x + 1, track.y + 1, (int) Math.round(t * (track.width - 2)), track.height - 2);
		Rectangle knob = new Rectangle(track.x + (int) Math.round(t * (track.width - 1)) - 4, y + 2, 9, 13);
		boolean hover = dragging == s || contains(new Rectangle(track.x - 4, y, track.width + 8, 18), mouse);
		bevel(g, knob, hover ? HOVER : BUTTON, LIGHT, DARK);
		String v = String.valueOf(value);
		text(g, v, x + width - g.getFontMetrics().stringWidth(v), y + 13, WHITE);
		out.add(new Hit(new Rectangle(track.x - 4, y, track.width + 8, 18), null, s));
	}

	private static void bevel(Graphics2D g, Rectangle r, Color fill, Color light, Color dark)
	{
		g.setColor(fill);
		g.fillRect(r.x, r.y, r.width, r.height);
		g.setColor(EDGE);
		g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
		if (r.width > 4 && r.height > 4)
		{
			g.setColor(light);
			g.drawLine(r.x + 1, r.y + 1, r.x + r.width - 2, r.y + 1);
			g.drawLine(r.x + 1, r.y + 1, r.x + 1, r.y + r.height - 2);
			g.setColor(dark);
			g.drawLine(r.x + 1, r.y + r.height - 2, r.x + r.width - 2, r.y + r.height - 2);
			g.drawLine(r.x + r.width - 2, r.y + 1, r.x + r.width - 2, r.y + r.height - 2);
		}
	}

	private static void text(Graphics2D g, String s, int x, int y, Color c)
	{
		g.setColor(Color.BLACK);
		g.drawString(s, x + 1, y + 1);
		g.setColor(c);
		g.drawString(s, x, y);
	}

	private static boolean contains(Rectangle r, Point p)
	{
		return p != null && r.contains(p.getX(), p.getY());
	}

	private void drag(Slider s, int x)
	{
		Rectangle t = s.track;
		if (t == null)
		{
			return;
		}
		int v = sliderValue(x, t.x, t.width, s.min, s.max);
		if (v != dragged)
		{
			dragged = v;
			set(s.key, v);
		}
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		swallow = false;
		List<Hit> hs = hits;
		// last drawn is on top, so the menu wins over the rim button it covers
		for (int i = hs.size() - 1; i >= 0; i--)
		{
			Hit h = hs.get(i);
			if (h.bounds.contains(e.getPoint()))
			{
				swallow = true;
				e.consume();
				if (e.getButton() != MouseEvent.BUTTON1)
				{
					return e;
				}
				if (h.slider == null && h.press == null)
				{
					if (e.getClickCount() >= 2)
					{
						menuX = Integer.MIN_VALUE;
						menuY = Integer.MIN_VALUE;
						configManager.unsetConfiguration(NextUnlocksConfig.GROUP, MENU_X);
						configManager.unsetConfiguration(NextUnlocksConfig.GROUP, MENU_Y);
					}
					else
					{
						Rectangle p = panel;
						moving = p != null;
						if (moving)
						{
							grabX = e.getX() - p.x;
							grabY = e.getY() - p.y;
							menuX = p.x;
							menuY = p.y;
						}
					}
				}
				else if (h.slider != null)
				{
					dragging = h.slider;
					dragged = h.slider.value.getAsInt();
					drag(h.slider, e.getX());
				}
				else
				{
					// the plugin's goals and pins live on the client thread
					clientThread.invokeLater(h.press);
				}
				return e;
			}
		}
		Rectangle p = panel;
		if (open && p != null && p.contains(e.getPoint()))
		{
			swallow = true;
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		if (moving)
		{
			menuX = e.getX() - grabX;
			menuY = e.getY() - grabY;
			e.consume();
			return e;
		}
		Slider s = dragging;
		if (s != null)
		{
			drag(s, e.getX());
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		dragging = null;
		if (moving)
		{
			moving = false;
			// save where it ended up on the canvas, not where the mouse pulled it past the edge
			Rectangle p = panel;
			int x = p != null ? onCanvas(menuX, p.width, client.getCanvasWidth()) : menuX;
			int y = p != null ? onCanvas(menuY, p.height, client.getCanvasHeight()) : menuY;
			menuX = x;
			menuY = y;
			configManager.setConfiguration(NextUnlocksConfig.GROUP, MENU_X, x);
			configManager.setConfiguration(NextUnlocksConfig.GROUP, MENU_Y, y);
		}
		if (swallow)
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (swallow)
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e)
	{
		Rectangle p = panel;
		if (open && p != null && p.contains(e.getPoint()))
		{
			e.consume();
		}
		return e;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (open && e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			open = false;
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	/** closes the menu, as when the plugin stops */
	void close()
	{
		open = false;
		panel = null;
		hits = Collections.emptyList();
	}
}
