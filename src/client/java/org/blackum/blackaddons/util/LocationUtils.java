package org.blackum.blackaddons.util;

import java.util.List;

public class LocationUtils {

    public static String getLocation() {
        List<String> lines = ScoreboardUtils.getCleanSidebarLines();

        for (String line : lines) {
            if (line.contains("⏣")) {
                return line.replace("⏣", "").trim();
            }
        }

        return "Unknown";
    }

    public static boolean inSkyblock() {
        List<String> lines = ScoreboardUtils.getCleanSidebarLines();
        if (!lines.isEmpty()) {
            return !getLocation().equals("Unknown");
        }
        return false;
    }

    public static boolean inDungeons() {
        String raw = getLocation();
        String normalized = raw.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        // Temporary excessive debug
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
             net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§d[LocUtils] Raw: '" + raw + "' -> Norm: '" + normalized + "' | Contains 'catacombs': " + normalized.contains("catacombs")), 
                false
            );
        }
        
        return normalized.contains("catacombs");
    }
}
