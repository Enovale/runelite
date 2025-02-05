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
package net.runelite.client.plugins.lastseen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.RemovedFriend;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@PluginDescriptor(
	name = "Last Seen Online",
	description = "Track when you've last seen your friends play",
	tags = {"last", "seen", "online", "friends", "activity", "watch"}
)
public class LastSeenPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LastSeenOverlay overlay;
	@Inject
	private LastSeenDao dao;

	private final Set<String> currentlyOnline = new HashSet<>();
	// in-memory buffer of 'last seen online', persisted periodically
	private final Map<String, Long> lastSeenBuffer = new HashMap<>();

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		dao.clearCache();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		persistLastSeen();
		dao.clearCache();
	}

	@Subscribe
	public void onNameableNameChanged(NameableNameChanged event)
	{
		final Nameable nameable = event.getNameable();
		if (nameable instanceof Friend && StringUtils.isNotBlank(nameable.getPrevName()))
		{
			dao.migrateLastSeen(
				Text.toJagexName(nameable.getPrevName()),
				Text.toJagexName(nameable.getName())
			);
		}
	}

	@Subscribe
	public void onRemovedFriend(RemovedFriend event)
	{
		dao.deleteLastSeen(Text.toJagexName(event.getNameable().getName()));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());

		overlay.setTooltip(null);
		if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() && event.getOption().equals("Message"))
		{
			final String displayName = Text.toJagexName(Text.removeTags(event.getTarget()));
			if (StringUtils.isNotBlank(displayName) && !currentlyOnline.contains(displayName))
			{
				final Long lastSeen = lastSeenBuffer.getOrDefault(displayName, dao.getLastSeen(displayName));
				overlay.setTooltip("Last online: " + LastSeenFormatter.format(lastSeen));
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// persist in-memory state every minute
		if (client.getTickCount() % 100 == 0)
		{
			persistLastSeen();
		}

		final FriendContainer friendContainer = client.getFriendContainer();
		if (friendContainer == null)
		{
			return;
		}

		currentlyOnline.clear();
		Arrays.stream(friendContainer.getMembers())
			.filter(friend -> friend.getWorld() > 0)
			.forEach(friend -> currentlyOnline.add(Text.toJagexName(friend.getName())));

		final long currentTimeMillis = System.currentTimeMillis();
		currentlyOnline.forEach(displayName -> lastSeenBuffer.put(displayName, currentTimeMillis));
	}

	private void persistLastSeen()
	{
		lastSeenBuffer.forEach(dao::setLastSeen);
		lastSeenBuffer.clear();
	}
}
