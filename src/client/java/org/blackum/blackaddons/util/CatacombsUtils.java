package org.blackum.blackaddons.util;

import java.util.List;
import java.util.Map;

public class CatacombsUtils {

    public static final double CATA_50_XP = 569809640.0;

    public static final List<Double> DUNGEON_XP = List.of(
            0.0, 50.0, 75.0, 110.0, 160.0, 230.0, 330.0, 470.0, 670.0, 950.0, 1340.0,
            1890.0, 2665.0, 3760.0, 5260.0, 7380.0, 10300.0, 14400.0, 20000.0, 27600.0,
            38000.0, 52500.0, 71500.0, 97000.0, 132000.0, 180000.0, 243000.0, 328000.0,
            445000.0, 600000.0, 800000.0, 1065000.0, 1410000.0, 1900000.0, 2500000.0,
            3300000.0, 4300000.0, 5600000.0, 7200000.0, 9200000.0, 12000000.0, 15000000.0,
            19000000.0, 24000000.0, 30000000.0, 38000000.0, 48000000.0, 60000000.0, 75000000.0,
            93000000.0, 116250000.0, 200000000.0);

    private static final Map<String, Integer> FLOOR_XP_MAP = Map.ofEntries(
            Map.entry("M7", 300000), Map.entry("M6", 100000), Map.entry("M5", 70000),
            Map.entry("M4", 55000), Map.entry("M3", 35000), Map.entry("M2", 20000),
            Map.entry("M1", 15000),
            Map.entry("F7", 28000), Map.entry("F6", 4880), Map.entry("F5", 2400),
            Map.entry("F4", 1420), Map.entry("F3", 560), Map.entry("F2", 220),
            Map.entry("F1", 110), Map.entry("Entrance", 55));

    public static double calculateDungeonXpPerRun(String floor, double ring, double hecatomb, double globalMult,
            double mayorMult) {
        int baseFloor = FLOOR_XP_MAP.getOrDefault(floor, 0);
        if (baseFloor == 0)
            return 0;

        int maxcomps;
        if (baseFloor >= 15000) {
            maxcomps = 26;
        } else if (baseFloor == 4880) {
            maxcomps = 51;
        } else {
            maxcomps = 76;
        }

        double cataperrun;
        if (ring > 0 && mayorMult > 1) {
            cataperrun = baseFloor * (0.95 + ((mayorMult - 1) + (maxcomps - 1) / 100.0) + ring + hecatomb
                    + (maxcomps - 1) * (0.024 + hecatomb / 50.0));
        } else if (ring > 0) {
            cataperrun = baseFloor
                    * (0.95 + ring + hecatomb + (maxcomps - 1) * (0.024 + hecatomb / 50.0));
        } else {
            cataperrun = baseFloor
                    * (0.95 + hecatomb + (maxcomps - 1) * (0.022 + hecatomb / 50.0));
        }

        cataperrun *= globalMult;
        return Math.ceil(cataperrun);
    }

    public static double getDungeonLevel(double xp) {
        double total = 0.0;
        for (int i = 1; i < DUNGEON_XP.size(); i++) {
            total += DUNGEON_XP.get(i);
            if (xp < total) {
                double prev = total - DUNGEON_XP.get(i);
                double progress = (xp - prev) / DUNGEON_XP.get(i);
                return Math.round((i - 1 + progress) * 100.0) / 100.0;
            }
        }
        double extra = xp - total;
        double extraLevels = extra / DUNGEON_XP.get(DUNGEON_XP.size() - 1);
        return Math.round(((DUNGEON_XP.size() - 1) + extraLevels) * 100.0) / 100.0;
    }

    public static double getTotalXpForLevel(double level) {
        double total = 0.0;
        int levelInt = (int) Math.floor(level);

        for (int i = 1; i < Math.min(levelInt + 1, DUNGEON_XP.size()); i++) {
            total += DUNGEON_XP.get(i);
        }

        if (levelInt + 1 < DUNGEON_XP.size()) {
            double frac = level - levelInt;
            if (frac > 0) {
                total += DUNGEON_XP.get(levelInt + 1) * frac;
            }
            return total;
        }

        int baseLevels = DUNGEON_XP.size() - 1;
        total = 0;
        for (int i = 1; i <= baseLevels; i++)
            total += DUNGEON_XP.get(i);

        if (level > baseLevels) {
            double extraLevels = level - baseLevels;
            double extraWhole = Math.floor(extraLevels);
            total += extraWhole * DUNGEON_XP.get(DUNGEON_XP.size() - 1);

            double frac = extraLevels - extraWhole;
            if (frac > 0) {
                total += DUNGEON_XP.get(DUNGEON_XP.size() - 1) * frac;
            }
        }
        return total;
    }
}
