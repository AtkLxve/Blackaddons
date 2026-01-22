package org.blackum.blackaddons.modhider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

public class ToastUtils {
    private static final Set<String> serversAttemptedReadingMods = new HashSet<>();

    public static void showServerAttemptedReadingModsToast() {
        ServerData serverData = Minecraft.getInstance().getCurrentServer();
        if (serverData != null) {
            if (serversAttemptedReadingMods.contains(serverData.ip)) {
                return;
            }
            serversAttemptedReadingMods.add(serverData.ip);
        }

        SystemToast.SystemToastId id = new SystemToast.SystemToastId(10001L);
        Component title = Component.literal("BlackAddons");
        Component message = Component.literal("Server attempted to read your mod list.");
        Minecraft.getInstance().getToastManager().addToast(
                SystemToast.multiline(Minecraft.getInstance(), id, title, message));
    }
}

