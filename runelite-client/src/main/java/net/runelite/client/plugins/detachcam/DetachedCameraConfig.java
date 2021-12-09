package net.runelite.client.plugins.detachcam;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ConfigGroup("detachcam")
public interface DetachedCameraConfig extends Config {


    @ConfigItem(
            keyName = "hotkey",
            name = "Toggle Hotkey",
            description = "When you press this key, the camera's attached state will be toggled."
    )
    default Keybind hotkey()
    {
        return new Keybind(KeyEvent.VK_QUOTE, 0);
    }

}
