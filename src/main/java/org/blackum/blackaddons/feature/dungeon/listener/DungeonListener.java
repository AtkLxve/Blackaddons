package org.blackum.blackaddons.feature.dungeon.listener;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.regex.Pattern;

public class DungeonListener {
    public static long currentTime = 0L;

    public static int keyTimerTicks = -1;
    public static String keyTimerType = ""; // "Wither" or "Blood"
    public static int lastDetectedKeyEntityId = -1;

    private static final Pattern KEY_PICKUP_PATTERN = Pattern.compile(
            "^RIGHT CLICK on .+ to open it\\. This key can only be used to open \\d+ door!$");
    private static final Pattern WITHER_DOOR_OPEN_PATTERN = Pattern.compile(
            "^(?:\\[.+?] )?\\w+ opened a WITHER door!$");

    public static void tick() {
        if (keyTimerTicks > 0) {
            keyTimerTicks--;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && lastDetectedKeyEntityId != -1) {
                Entity entity = mc.level.getEntity(lastDetectedKeyEntityId);
                if (entity == null || !entity.isAlive()) {
                    // key was picked up so you can off marshy </3
                    // jk <3
                    resetKeyTimer();
                }
            }

            if (keyTimerTicks == 0) {
                resetKeyTimer();
            }
        }
    }

    public static void onChatMessage(Component message) {
        if (message == null)
            return;
        String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null || text.isEmpty())
            return;

        if (KEY_PICKUP_PATTERN.matcher(text).find() ||
                WITHER_DOOR_OPEN_PATTERN.matcher(text).find() ||
                text.contains("The BLOOD DOOR has been opened!")) {
            resetKeyTimer();
        }
    }

    public static void resetKeyTimer() {
        keyTimerTicks = -1;
        keyTimerType = "";
        lastDetectedKeyEntityId = -1;
    }
}
