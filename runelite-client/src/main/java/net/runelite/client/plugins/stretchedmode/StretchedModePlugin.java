/*
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.stretchedmode;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.ResizeableChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@PluginDescriptor(
	name = "Stretched Mode",
	description = "Stretches the game in fixed and resizable modes.",
	tags = {"resize", "ui", "interface", "stretch", "scaling", "fixed"},
	enabledByDefault = false
)
public class StretchedModePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private StretchedModeConfig config;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private TranslateMouseListener mouseListener;

	@Inject
	private TranslateMouseWheelListener mouseWheelListener;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			toggleStretch();
		}
	};

	@Provides
	StretchedModeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StretchedModeConfig.class);
	}

	@Override
	protected void startUp()
	{
		mouseManager.registerMouseListener(0, mouseListener);
		mouseManager.registerMouseWheelListener(0, mouseWheelListener);

		hotkeyListener.setEnabledOnLoginScreen(true);
		keyManager.registerKeyListener(hotkeyListener);

		final BufferedImage iconImage = ImageUtil.loadImageResource(getClass(), "open.png");

		NavigationButton titleBarButton = NavigationButton.builder()
				.tab(false)
				.tooltip("Toggle Stretched Mode")
				.icon(iconImage)
				.onClick(this::toggleStretch)
				.build();

		clientToolbar.addNavigation(titleBarButton);

		client.setStretchedEnabled(config.enableOnStart());
		updateConfig();
	}

	@Override
	protected void shutDown() throws Exception
	{
		client.setStretchedEnabled(false);
		client.invalidateStretching(true);

		mouseManager.unregisterMouseListener(mouseListener);
		mouseManager.unregisterMouseWheelListener(mouseWheelListener);
	}

	@Subscribe
	public void onResizeableChanged(ResizeableChanged event)
	{
		client.invalidateStretching(true);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("stretchedmode"))
		{
			return;
		}

		client.setStretchedEnabled(config.enableOnStart());
		updateConfig();
	}

	private void toggleStretch() {
		client.setStretchedEnabled(!client.isStretchedEnabled());
		updateConfig();
	}

	private void updateConfig()
	{
		client.setStretchedIntegerScaling(config.integerScaling());
		client.setStretchedKeepAspectRatio(config.keepAspectRatio());
		client.setStretchedFast(config.increasedPerformance());
		client.setScalingFactor(config.scalingFactor());

		client.invalidateStretching(true);
	}
}
