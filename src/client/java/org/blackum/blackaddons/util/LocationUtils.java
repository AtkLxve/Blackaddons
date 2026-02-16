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
        String loc = getLocation();
        for (int i = 1; i <= 7; i++) {
            if (loc.contains("(F" + i + ")") || loc.contains("(M" + i + ")")) {
                return true;
            }
        }
        return loc.contains("(E)") || loc.contains("Catacombs");
    }
}
