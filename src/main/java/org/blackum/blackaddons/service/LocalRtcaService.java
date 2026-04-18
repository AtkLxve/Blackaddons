package org.blackum.blackaddons.service;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LocalRtcaService {

    private static final double[] DUNGEON_XP = {
            0, 50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
            1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600,
            38000, 52500, 71500, 97000, 132000, 180000, 243000, 328000,
            445000, 600000, 800000, 1065000, 1410000, 1900000, 2500000,
            3300000, 4300000, 5600000, 7200000, 9200000, 12000000, 15000000,
            19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000,
            93000000, 116250000, 200000000
    };

    private static final Map<String, Integer> FLOOR_XP_MAP = new HashMap<>();

    static {
        FLOOR_XP_MAP.put("M7", 300000);
        FLOOR_XP_MAP.put("M6", 100000);
        FLOOR_XP_MAP.put("M5", 70000);
        FLOOR_XP_MAP.put("M4", 55000);
        FLOOR_XP_MAP.put("M3", 35000);
        FLOOR_XP_MAP.put("M2", 20000);
        FLOOR_XP_MAP.put("M1", 15000);
        FLOOR_XP_MAP.put("F7", 28000);
        FLOOR_XP_MAP.put("F6", 4880);
        FLOOR_XP_MAP.put("F5", 2400);
        FLOOR_XP_MAP.put("F4", 1420);
        FLOOR_XP_MAP.put("F3", 560);
        FLOOR_XP_MAP.put("F2", 220);
        FLOOR_XP_MAP.put("F1", 110);
        FLOOR_XP_MAP.put("ENTRANCE", 55);
    }

    public static CompletableFuture<JsonObject> simulate(String floor, Map<String, Double> currentXp,
            Map<String, Double> bonuses) {
        return CompletableFuture.supplyAsync(() -> runSimulation(floor, currentXp, bonuses));
    }

    private static JsonObject runSimulation(String floor, Map<String, Double> currentXp, Map<String, Double> bonuses) {
        double floorXp = FLOOR_XP_MAP.getOrDefault(floor, 300000).doubleValue();
        int targetLevel = 50;

        double hecatomb = bonuses.getOrDefault("hecatomb", 0.02);
        double scarfAccessory = bonuses.getOrDefault("scarf_accessory", 0.06);
        double scarfAttribute = bonuses.getOrDefault("scarf_attribute", 0.2);
        double globalMult = bonuses.getOrDefault("global", 1.0);
        double mayorMult = bonuses.getOrDefault("mayor", 1.0);

        Map<String, Double> perClassBase = new HashMap<>();
        for (String cls : currentXp.keySet()) {
            double boost = 0.0;
            double base = floorXp * (1.0 + (hecatomb * 2) + boost + scarfAccessory + scarfAttribute) * globalMult
                    * mayorMult;
            perClassBase.put(cls, base);
        }

        double targetXp = getTotalXpForLevel(targetLevel);

        Map<String, Double> classXpLeft = new HashMap<>();
        for (String cls : currentXp.keySet()) {
            classXpLeft.put(cls, Math.max(targetXp - currentXp.get(cls), 0));
        }

        Map<String, Integer> runsDone = new HashMap<>();
        for (String cls : currentXp.keySet()) {
            runsDone.put(cls, 0);
        }

        int runs = 0;
        int maxRuns = 200000;

        while (runs < maxRuns) {
            boolean allNegative = true;
            for (double rem : classXpLeft.values()) {
                if (rem > 0) {
                    allNegative = false;
                    break;
                }
            }
            if (allNegative)
                break;

            runs++;

            double maxVal = -1;
            String maxIndex = null;
            for (Map.Entry<String, Double> entry : classXpLeft.entrySet()) {
                if (entry.getValue() > maxVal) {
                    maxVal = entry.getValue();
                    maxIndex = entry.getKey();
                }
            }

            if (maxIndex == null)
                break;

            for (String cls : currentXp.keySet()) {
                double xpGain = perClassBase.getOrDefault(cls, 0.0);
                if (cls.equals(maxIndex)) {
                    classXpLeft.put(cls, classXpLeft.get(cls) - xpGain);
                    runsDone.put(cls, runsDone.get(cls) + 1);
                } else {
                    classXpLeft.put(cls, classXpLeft.get(cls) - (xpGain * 0.25));
                }
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty("status", "success");
        result.addProperty("total_runs", runs);

        JsonObject resultsObj = new JsonObject();
        for (String cls : currentXp.keySet()) {
            JsonObject clsObj = new JsonObject();
            double xp = targetXp - classXpLeft.get(cls);
            clsObj.addProperty("current_level", getDungeonLevel(xp));

            double initialRemaining = Math.max(0, targetXp - currentXp.get(cls));
            clsObj.addProperty("remaining_xp", initialRemaining);
            clsObj.addProperty("runs_done", runsDone.get(cls));

            resultsObj.add(cls, clsObj);
        }
        result.add("results", resultsObj);

        return result;
    }

    private static double getTotalXpForLevel(int level) {
        double total = 0.0;
        for (int i = 1; i <= level && i < DUNGEON_XP.length; i++) {
            total += DUNGEON_XP[i];
        }
        return total;
    }

    private static double getDungeonLevel(double xp) {
        double total = 0.0;
        for (int i = 1; i < DUNGEON_XP.length; i++) {
            total += DUNGEON_XP[i];
            if (xp < total) {
                double prev = total - DUNGEON_XP[i];
                double progress = (xp - prev) / DUNGEON_XP[i];
                return Math.round((i - 1 + progress) * 100.0) / 100.0;
            }
        }
        double lastLevelXp = DUNGEON_XP[DUNGEON_XP.length - 1];
        double extra = xp - total;
        double extraLevels = extra / lastLevelXp;
        return Math.round(((DUNGEON_XP.length - 1) + extraLevels) * 100.0) / 100.0;
    }
}
