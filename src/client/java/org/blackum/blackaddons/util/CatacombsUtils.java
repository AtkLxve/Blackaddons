package org.blackum.blackaddons.util;

public class CatacombsUtils {

    public static double calculateDungeonXpPerRun(String floor, double ring, double hecatomb, double globalMult,
            double mayorMult) {
        int baseFloor = Constants.FLOOR_XP_MAP.getOrDefault(floor, 0);
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
        for (int i = 1; i < Constants.DUNGEON_XP.size(); i++) {
            total += Constants.DUNGEON_XP.get(i);
            if (xp < total) {
                double prev = total - Constants.DUNGEON_XP.get(i);
                double progress = (xp - prev) / Constants.DUNGEON_XP.get(i);
                return Math.round((i - 1 + progress) * 100.0) / 100.0;
            }
        }
        double extra = xp - total;
        double extraLevels = extra / Constants.DUNGEON_XP.get(Constants.DUNGEON_XP.size() - 1);
        return Math.round(((Constants.DUNGEON_XP.size() - 1) + extraLevels) * 100.0) / 100.0;
    }

    public static double getTotalXpForLevel(double level) {
        double total = 0.0;
        int levelInt = (int) Math.floor(level);

        for (int i = 1; i < Math.min(levelInt + 1, Constants.DUNGEON_XP.size()); i++) {
            total += Constants.DUNGEON_XP.get(i);
        }

        if (levelInt + 1 < Constants.DUNGEON_XP.size()) {
            double frac = level - levelInt;
            if (frac > 0) {
                total += Constants.DUNGEON_XP.get(levelInt + 1) * frac;
            }
            return total;
        }

        int baseLevels = Constants.DUNGEON_XP.size() - 1;
        total = 0;
        for (int i = 1; i <= baseLevels; i++)
            total += Constants.DUNGEON_XP.get(i);

        if (level > baseLevels) {
            double extraLevels = level - baseLevels;
            double extraWhole = Math.floor(extraLevels);
            total += extraWhole * Constants.DUNGEON_XP.get(Constants.DUNGEON_XP.size() - 1);

            double frac = extraLevels - extraWhole;
            if (frac > 0) {
                total += Constants.DUNGEON_XP.get(Constants.DUNGEON_XP.size() - 1) * frac;
            }
        }
        return total;
    }
}
