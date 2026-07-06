package org.blackum.blackaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;

public class TabListToggleHandler {
    private static boolean tabListToggled = false;
    private static boolean tabKeyWasPressed = false;

    public static boolean shouldShowTabList(KeyMapping keyMapping) {
        if (ConfigManager.data.toggleTabList) {
            return tabListToggled;
        }
        return keyMapping.isDown();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && McCompat.getScreen(mc) == null) {
            boolean isDown = mc.options.keyPlayerList.isDown();
            if (isDown && !tabKeyWasPressed) {
                if (ConfigManager.data.toggleTabList) {
                    tabListToggled = !tabListToggled;
                }
            }
            tabKeyWasPressed = isDown;
        } else {
            tabListToggled = false;
            tabKeyWasPressed = false;
        }
    }
}
