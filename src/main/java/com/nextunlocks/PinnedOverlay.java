package com.nextunlocks;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
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

/**
 * Pinned skill tooltips, side by side on top of the chatbox. When the chatbox is collapsed they sit
 * on top of its buttons instead.
 */
class PinnedOverlay extends Overlay
{
	private static final int GAP = 2;

	private final Client client;
	private final NextUnlocksPlugin plugin;

	/** Where each pinned tooltip was drawn last frame, for the right-click menu. Client thread. */
	private final Map<Skill, Rectangle> drawn = new EnumMap<>(Skill.class);
	/** Measured size of the tooltip last drawn for a skill; tooltips are drawn upward from the chatbox. */
	private final Map<Skill, LayoutableRenderableEntity> measured = new EnumMap<>(Skill.class);
	private final Map<Skill, Dimension> sizes = new EnumMap<>(Skill.class);

	@Inject
	PinnedOverlay(Client client, NextUnlocksPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		drawn.clear();
		if (plugin.getPinned().isEmpty())
		{
			return null;
		}
		Rectangle chat = chatTop();
		if (chat == null)
		{
			return null;
		}

		int x = chat.x;
		int bottom = chat.y - GAP;
		for (Skill skill : plugin.getPinned())
		{
			LayoutableRenderableEntity tooltip = plugin.tooltip(skill);
			if (tooltip == null)
			{
				continue;
			}
			Dimension size = size(graphics, skill, tooltip);
			Point location = new Point(x, bottom - size.height);
			tooltip.setPreferredLocation(location);
			Dimension rendered = tooltip.render(graphics);
			sizes.put(skill, rendered);
			drawn.put(skill, new Rectangle(location, rendered));
			x += rendered.width + GAP;
		}
		return null;
	}

	/** The pinned skill drawn under a canvas point, or null. Client thread. */
	Skill skillAt(int x, int y)
	{
		for (Map.Entry<Skill, Rectangle> e : drawn.entrySet())
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
	private Dimension size(Graphics2D graphics, Skill skill, LayoutableRenderableEntity tooltip)
	{
		Dimension size = sizes.get(skill);
		if (size == null || measured.get(skill) != tooltip)
		{
			BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = scratch.createGraphics();
			g.setFont(graphics.getFont());
			g.setRenderingHints(graphics.getRenderingHints());
			tooltip.setPreferredLocation(new Point());
			size = tooltip.render(g);
			g.dispose();
			measured.put(skill, tooltip);
			sizes.put(skill, size);
		}
		return size;
	}
}
