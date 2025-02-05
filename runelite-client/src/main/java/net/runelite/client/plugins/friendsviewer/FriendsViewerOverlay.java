/*
 * Copyright (c) 2021, molo-pl <https://github.com/molo-pl>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.friendsviewer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendContainer;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.Text;

public class FriendsViewerOverlay extends OverlayPanel
{
	private final Client client;
	private final FriendsViewerConfig config;
	private final String title;
	private final Supplier<Boolean> enabled;
	private final Supplier<Boolean> friendsExempt;

	@Getter
	@Setter
	private List<FriendsViewerEntry> entries;

	public FriendsViewerOverlay(Client client, FriendsViewerConfig config, String title, Supplier<Boolean> enabled, Supplier<Boolean> exemptFriends)
	{
		this.client = client;
		this.config = config;
		this.title = title;
		this.enabled = enabled;
		this.friendsExempt = exemptFriends;
		setPosition(OverlayPosition.TOP_RIGHT);
		panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH + FriendsViewerIconManager.IMAGE_DIMENSION.width, 0));
	}

	@Override
	public String getName()
	{
		return super.getName() + "-" + title;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (entries == null || !enabled.get())
		{
			return null;
		}

		final Friend[] friends = client.getFriendContainer().getMembers();
		final List<FriendsViewerEntry> entriesToExclude = entries.stream()
			.filter(entry -> Arrays.stream(friends)
				.anyMatch(friend -> Text.toJagexName(friend.getName()).equals(entry.getName())))
			.collect(Collectors.toList());

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(String.format("%s (%d)", title, entries.size()))
			.build());

		entries.stream()
			.filter(entry -> !friendsExempt.get() || !entriesToExclude.contains(entry))
			.limit(config.maxPlayers())
			.map(this::toRenderableEntity)
			.forEach(panelComponent.getChildren()::add);

		if (entries.size() > config.maxPlayers())
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(String.format("... %d more", entries.size() - config.maxPlayers() + entriesToExclude.size()) + ((friendsExempt.get() && entriesToExclude.size() > 0) ? String.format(", including %d friends", entriesToExclude.size()) : ""))
				.build());
		}

		return super.render(graphics);
	}

	private LayoutableRenderableEntity toRenderableEntity(FriendsViewerEntry entry)
	{
		final LineComponent line = LineComponent.builder()
			.leftFont(getFont(config.fontSize()))
			.left(entry.getName())
			.rightFont(getFont(config.fontSize()))
			.right("W" + entry.getWorld())
			.rightColor(entry.getWorld() == client.getWorld() ? config.sameWorldColor() : config.differentWorldColor())
			.build();
		return entry.getIcon() != null ?
			SplitComponent.builder()
				.orientation(ComponentOrientation.HORIZONTAL)
				.first(new ImageComponent(entry.getIcon()))
				.second(line)
				.gap(new Point(1, 0))
				.build() :
			line;
	}

	private Font getFont(FriendsViewerFontSize fontSize)
	{
		switch (fontSize)
		{
			case REGULAR:
			{
				return FontManager.getRunescapeFont();
			}
			case SMALL:
			{
				return FontManager.getRunescapeSmallFont();
			}
			default:
			{
				throw new UnsupportedOperationException("Unknown font size: " + config.fontSize());
			}
		}
	}
}
