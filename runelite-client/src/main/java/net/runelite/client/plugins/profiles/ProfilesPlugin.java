/*
 * Copyright (c) 2019, Spedwards <https://github.com/Spedwards>
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
package net.runelite.client.plugins.profiles;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "Profiles"
)
public class ProfilesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ProfilesConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WorldService worldService;

	private ProfilesPanel panel;
	private NavigationButton navButton;
	private boolean autoFillRequired;

	@Provides
	ProfilesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ProfilesConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = new ProfilesPanel(client, config, worldService);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "profiles_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Profiles")
				.priority(8)
				.icon(icon)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		autoFillRequired = true;
		autoFill();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("profiles"))
		{
			panel.redrawProfiles();
		}
	}

	@Subscribe
	public void onSessionOpen(SessionOpen event)
	{
		autoFillRequired = true;
		autoFill();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		autoFill();
	}

	private void autoFill()
	{
		try {
			if ((!config.fillOnLogout() && !autoFillRequired) || client.getGameState() != GameState.LOGIN_SCREEN)
			{
				return;
			}

			autoFillRequired = false;

			if(!config.getAutofillProfile().isEmpty()) {
				for (Component component : panel.profilesPanel.getComponents()) {
					ProfilePanel panel = (ProfilePanel)component;
					if(Objects.equals(panel.Label, config.getAutofillProfile())) {
						panel.Login(client);
						break;
					}
				}
			}
		} catch (Exception e) {
			log.error("Error autofilling profile: " + e.getMessage());
		}
	}

	public static Integer CorrectWorld(Integer id) {

		int correctedWorld = id < 300 ? id + 300 : id;

		// Old School RuneScape worlds start on 301 so don't even bother trying to find lower id ones
		// and also do not try to set world if we are already on it
		return correctedWorld <= 300 ? 0 : correctedWorld;
	}
}
