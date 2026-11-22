package com.nextunlocks;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.Text;

/**
 * Pinned skill tooltips, quest or diary cards and followed guides' cards, side by side on top of the chatbox. When the chatbox is collapsed they sit
 * on top of its buttons instead.
 */
class PinnedOverlay extends Overlay
{
	/** Between cards: wide enough that trims sticking out past a frame, like chain rings, don't meet. */
	private static final int GAP = 4;
	/** Above the chatbox or the private messages. */
	private static final int LIFT = 2;
	/** Where the game opens windows like the bank, in the fixed, resizable and other layouts. */
	private static final int[] WINDOW_CONTAINERS = {
		InterfaceID.Toplevel.MAINMODAL, InterfaceID.Toplevel.SIDEMODAL,
		InterfaceID.ToplevelOsrsStretch.MAINMODAL, InterfaceID.ToplevelOsrsStretch.SIDEMODAL,
		InterfaceID.ToplevelPreEoc.MAINMODAL, InterfaceID.ToplevelPreEoc.SIDEMODAL,
		InterfaceID.ToplevelOsm.MAINMODAL, InterfaceID.ToplevelOsm.SIDEMODAL,
		InterfaceID.ToplevelDisplay.MAINMODAL, InterfaceID.ToplevelDisplay.SIDEMODAL,
	};

	private final Client client;
	private final NextUnlocksPlugin plugin;
	private final TooltipManager tooltipManager;

	/** Where each pinned card was drawn last frame, for the right-click menu: by Skill, or by wiki title. Client thread. */
	private final Map<Object, Rectangle> drawn = new HashMap<>();
	/** Measured size of the card last drawn for a pin; cards are drawn upward from the chatbox. */
	private final Map<Object, LayoutableRenderableEntity> measured = new HashMap<>();
	private final Map<Object, Dimension> sizes = new HashMap<>();
	/** Where each guide card was drawn last frame, for scrolling them from the mouse wheel off the client thread. */
	private volatile Map<Guide, Rectangle> guideCards = Collections.emptyMap();

	@Inject
	PinnedOverlay(Client client, NextUnlocksPlugin plugin, TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		drawn.clear();
		List<Guide> guides = plugin.guideCardGuides();
		if (plugin.getPinned().isEmpty() && plugin.getPinnedTasks().isEmpty() && !plugin.isCombatPinned() && guides.isEmpty())
		{
			guideCards = Collections.emptyMap();
			return null;
		}
		Rectangle chat = chatTop();
		if (chat == null)
		{
			guideCards = Collections.emptyMap();
			return null;
		}

		// combat level first, then skill cards, then quests and diaries, then followed guides
		Map<Object, LayoutableRenderableEntity> cards = new LinkedHashMap<>();
		if (plugin.isCombatPinned())
		{
			LayoutableRenderableEntity card = plugin.combatCard();
			if (card != null)
			{
				cards.put(CombatLevel.PIN, card);
			}
		}
		for (Skill skill : plugin.getPinned())
		{
			LayoutableRenderableEntity card = plugin.pinnedTooltip(skill);
			if (card != null)
			{
				cards.put(skill, card);
			}
		}
		for (String task : plugin.getPinnedTasks())
		{
			LayoutableRenderableEntity card = plugin.taskCard(task);
			if (card != null)
			{
				cards.put(task, card);
			}
		}
		for (Guide guide : guides)
		{
			LayoutableRenderableEntity card = plugin.guideCard(guide);
			if (card != null)
			{
				cards.put(guide, card);
			}
		}

		// every skill card as tall as the tallest, so text-only ones line up with ones holding icons
		int skillHeight = 0;
		for (Map.Entry<Object, LayoutableRenderableEntity> e : cards.entrySet())
		{
			if (e.getKey() instanceof Skill)
			{
				skillHeight = Math.max(skillHeight, size(graphics, e.getKey(), e.getValue()).height);
			}
		}

		Map<Object, Integer> heights = new HashMap<>();
		int width = 0;
		int tallest = 0;
		for (Map.Entry<Object, LayoutableRenderableEntity> e : cards.entrySet())
		{
			Dimension size = size(graphics, e.getKey(), e.getValue());
			int height = e.getKey() instanceof Skill ? skillHeight : size.height;
			heights.put(e.getKey(), height);
			width += size.width + GAP;
			tallest = Math.max(tallest, height);
		}
		// split private chat shows just above the chatbox; go above its messages instead of over them
		int bottom = chat.y - LIFT;
		Integer pmTop = privateChatTop(new Rectangle(chat.x, bottom - tallest, width, tallest));
		if (pmTop != null)
		{
			bottom = Math.min(bottom, pmTop - LIFT);
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		List<Rectangle> windows = openWindows();
		Map<Object, LayoutableRenderableEntity> shown = new LinkedHashMap<>();
		int x = chat.x;
		for (Map.Entry<Object, LayoutableRenderableEntity> e : cards.entrySet())
		{
			LayoutableRenderableEntity card = e.getValue();
			int height = heights.get(e.getKey());
			Point location = new Point(x, bottom - height);
			// a window like the bank goes in front of the cards: leave out any card it would overlap
			Rectangle area = new Rectangle(location, new Dimension(size(graphics, e.getKey(), card).width, height));
			if (windows.stream().anyMatch(area::intersects))
			{
				x += area.width + GAP;
				continue;
			}
			shown.put(e.getKey(), card);
			card.setPreferredLocation(location);
			if (card instanceof TooltipBox)
			{
				((TooltipBox) card).setMinHeight(height);
			}
			Dimension rendered = card.render(graphics);
			if (card instanceof TooltipBox)
			{
				TooltipBox box = (TooltipBox) card;
				box.setMinHeight(0);
				// keep up with late changes, like item icons finishing loading
				sizes.put(e.getKey(), box.getLast());
				hover(box, mouse);
			}
			drawn.put(e.getKey(), new Rectangle(location, rendered));
			x += rendered.width + GAP;
		}
		Map<Guide, Rectangle> guideRects = new HashMap<>();
		drawn.forEach((pin, bounds) ->
		{
			if (pin instanceof Guide)
			{
				guideRects.put((Guide) pin, bounds);
			}
		});
		guideCards = guideRects;
		// level-up bursts and xp effects last, so they fly over the cards beside them
		for (Map.Entry<Object, LayoutableRenderableEntity> e : shown.entrySet())
		{
			if (!(e.getValue() instanceof TooltipBox))
			{
				continue;
			}
			TooltipBox box = (TooltipBox) e.getValue();
			box.bars().forEach(bar -> bar.renderBurst(graphics));
			if (e.getKey() instanceof Skill && !box.bars().isEmpty())
			{
				XpEffects.render(graphics, box.getTheme(), box.bars().get(0), plugin.xpGain((Skill) e.getKey()));
			}
		}
		return null;
	}

	/** Name the icon under the mouse, so icon-only cards still say what each one is. */
	private void hover(TooltipBox box, net.runelite.api.Point mouse)
	{
		if (mouse == null || !box.getBounds().contains(mouse.getX(), mouse.getY()))
		{
			return;
		}
		for (UnlockTileComponent tile : box.tiles())
		{
			if (tile.getName() != null && tile.getBounds().contains(mouse.getX(), mouse.getY()))
			{
				tooltipManager.add(new Tooltip(tile.getName()));
				return;
			}
		}
	}

	/** The pinned skill drawn under a canvas point, or null. Client thread. */
	Skill skillAt(int x, int y)
	{
		Object pin = pinAt(x, y);
		return pin instanceof Skill ? (Skill) pin : null;
	}

	/** Whether the combat level card is under the mouse. */
	boolean combatAt(int x, int y)
	{
		return pinAt(x, y) == CombatLevel.PIN;
	}

	/** The followed guide whose card is drawn under a canvas point, or null. Client thread. */
	Guide guideAt(int x, int y)
	{
		Object pin = pinAt(x, y);
		return pin instanceof Guide ? (Guide) pin : null;
	}

	/** The followed guide whose card was drawn under a canvas point last frame, or null. Any thread. */
	Guide guideUnder(int x, int y)
	{
		for (Map.Entry<Guide, Rectangle> e : guideCards.entrySet())
		{
			if (e.getValue().contains(x, y))
			{
				return e.getKey();
			}
		}
		return null;
	}

	/** The wiki title of the pinned quest or diary drawn under a canvas point, or null. Client thread. */
	String taskAt(int x, int y)
	{
		Object pin = pinAt(x, y);
		return pin instanceof String ? (String) pin : null;
	}

	private Object pinAt(int x, int y)
	{
		for (Map.Entry<Object, Rectangle> e : drawn.entrySet())
		{
			if (e.getValue().contains(x, y))
			{
				return e.getKey();
			}
		}
		return null;
	}

	/** Where the open windows are, like the bank, a shop or the Grand Exchange, in every screen layout. */
	private List<Rectangle> openWindows()
	{
		List<Rectangle> windows = new ArrayList<>();
		for (int id : WINDOW_CONTAINERS)
		{
			Widget container = client.getWidget(id);
			if (container == null || container.isHidden() || container.getNestedChildren() == null)
			{
				continue;
			}
			for (Widget window : container.getNestedChildren())
			{
				if (window != null && !window.isHidden())
				{
					windows.add(window.getBounds());
				}
			}
		}
		return windows;
	}

	/** The top of the split private messages that overlap the area, or null when none are showing there. */
	private Integer privateChatTop(Rectangle area)
	{
		Widget container = client.getWidget(InterfaceID.PmChat.CONTAINER);
		if (container == null || container.isHidden())
		{
			return null;
		}
		return top(container, area, null);
	}

	/** The lowest y of the message lines under the widget that overlap the area, or top when there are none. */
	private static Integer top(Widget widget, Rectangle area, Integer top)
	{
		for (Widget[] children : new Widget[][]{widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				if (child == null || child.isHidden())
				{
					continue;
				}
				String text = child.getText();
				Rectangle bounds = child.getBounds();
				if (text != null && !Text.removeTags(text).trim().isEmpty() && bounds.intersects(area))
				{
					top = top == null ? bounds.y : Math.min(top, bounds.y);
				}
				top = top(child, area, top);
			}
		}
		return top;
	}

	/** Bounds of the visible chat messages, or of the chat buttons when the chatbox is collapsed. */
	private Rectangle chatTop()
	{
		Widget area = client.getWidget(InterfaceID.Chatbox.CHATAREA);
		if (area != null && !area.isHidden())
		{
			return area.getBounds();
		}
		Widget controls = client.getWidget(InterfaceID.Chatbox.CONTROLS);
		if (controls != null && !controls.isHidden())
		{
			return controls.getBounds();
		}
		return null;
	}

	/** The tooltip's size, measured off-screen when it changed, so it can be placed above the chat before drawing. */
	private Dimension size(Graphics2D graphics, Object pin, LayoutableRenderableEntity tooltip)
	{
		Dimension size = sizes.get(pin);
		if (size == null || measured.get(pin) != tooltip)
		{
			BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = scratch.createGraphics();
			g.setFont(graphics.getFont());
			g.setRenderingHints(graphics.getRenderingHints());
			tooltip.setPreferredLocation(new Point());
			// a new panel lays out from its previous frame, so the first pass is off; the second has settled
			tooltip.render(g);
			size = tooltip.render(g);
			g.dispose();
			measured.put(pin, tooltip);
			sizes.put(pin, size);
		}
		return size;
	}
}
