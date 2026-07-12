package org.blackum.blackaddons.feature;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.common.util.mc.KeybindManager;
import org.lwjgl.glfw.GLFW;

@AutoModule(order = 109)
public final class ScoreboardToggleHandler {
    private static boolean keybindPressedLastTick = false;

    private ScoreboardToggleHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
    }

    private static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || McCompat.getScreen(mc) != null) {
            keybindPressedLastTick = false;
            return;
        }

        int keyCode = ConfigManager.data.toggleScoreboardKeyCode;
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode < 0) {
            keybindPressedLastTick = false;
            return;
        }

        boolean pressed = KeybindManager.isKeyPressed(keyCode);
        if (pressed && !keybindPressedLastTick) {
            ConfigManager.data.scoreboardEnabled = !ConfigManager.data.scoreboardEnabled;
            ConfigManager.save();
        }
        keybindPressedLastTick = pressed;
    }
}
