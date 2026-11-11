package com.nextunlocks;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

/**
 * Pinned skill tooltips and quest or diary cards, side by side on top of the chatbox. When the chatbox is collapsed they sit
 * on top of its buttons instead.
 */
class PinnedOverlay extends Overlay
{
	private static final int GAP = 2;

	private final Client client;
	private final NextUnlocksPlugin plugin;
	private final TooltipManager tooltipManager;

	/** Where each pinned card was drawn last frame, for the right-click menu: by Skill, or by wiki title. Client thread. */
	private final Map<Object, Rectangle> drawn = new HashMap<>();
	/** Measured size of the card last drawn for a pin; cards are drawn upward from the chatbox. */
	private final Map<Object, LayoutableRenderableEntity> measured = new HashMap<>();
	private final Map<Object, Dimension> sizes = new HashMap<>();

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
		if (plugin.getPinned().isEmpty() && plugin.getPinnedTasks().isEmpty())
		{
			return null;
		}
		Rectangle chat = chatTop();
		if (chat == null)
		{
			return null;
		}

		// skill cards first, then quests and diaries
		Map<Object, LayoutableRenderableEntity> cards = new LinkedHashMap<>();
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

		// every skill card as tall as the tallest, so text-only ones line up with ones holding icons
		int skillHeight = 0;
		for (Map.Entry<Object, LayoutableRenderableEntity> e : cards.entrySet())
		{
			if (e.getKey() instanceof Skill)
			{
				skillHeight = Math.max(skillHeight, size(graphics, e.getKey(), e.getValue()).height);
			}
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int x = chat.x;
		int bottom = chat.y - GAP;
		for (Map.Entry<Object, LayoutableRenderableEntity> e : cards.entrySet())
		{
			LayoutableRenderableEntity card = e.getValue();
			int height = e.getKey() instanceof Skill ? skillHeight : size(graphics, e.getKey(), card).height;
			Point location = new Point(x, bottom - height);
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
