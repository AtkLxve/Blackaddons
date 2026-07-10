package org.blackum.blackaddons.feature.dungeon.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.List;
import java.util.regex.Pattern;

public class DungeonUtils {
    private static final double[] CATA_XP = {
            50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385,
            6275, 8940, 12700, 17960, 25340, 35640, 50040, 70040, 97640, 135640,
            188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1684640, 2284640, 3084640,
            4149640, 5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640, 51559640,
            66559640, 85559640, 109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640
    };

    public static double getCataLevel(double xp) {
        for (int i = 0; i < CATA_XP.length; i++) {
            if (xp < CATA_XP[i]) {
                double prev = i == 0 ? 0 : CATA_XP[i - 1];
                double next = CATA_XP[i];
                return i + (xp - prev) / (next - prev);
            }
        }
        return 50 + (xp - CATA_XP[49]) / 200000000.0;
    }

    public static class DungeonStats {
        public int completedRooms = 0;
        public int totalRooms = 1;
        public int secretsFound = 0;
        public int secretPercent = 0;
        public int deaths = 0;
        public int crypts = 0;
        public int expectedPuzzles = 0;
        public boolean mimicKilled = false;
        public boolean princeKilled = false;
        public List<String> completedPuzzles = new ArrayList<>();

        public int calculateScore() {
            double roomCompletion = (double) completedRooms / Math.max(1, totalRooms);
            double exploration = (60.0 * roomCompletion) + (40.0 * secretPercent / 100.0);
            
            int puzzlePenalty = Math.max(0, (expectedPuzzles - completedPuzzles.size()) * 10);
            int deathPenalty = Math.max(0, (deaths > 0) ? (deaths * 2 - 1) : 0);
            
            double skill = Math.max(20, 20.0 + 80.0 * roomCompletion - puzzlePenalty - deathPenalty);
            int bonus = Math.min(5, crypts) + (mimicKilled ? 2 : 0) + (princeKilled ? 1 : 0);
            
            return 100 + (int)Math.floor(exploration) + (int)Math.floor(skill) + bonus;
        }
    }

    private static final Pattern COMPLETED_ROOMS_PATTERN = Pattern.compile("(?i)Completed Rooms:\\s*(\\d+)(?:/|\\s*out\\s*of\\s*)(\\d+)");
    private static final Pattern DEATHS_PATTERN = Pattern.compile("(?i)Deaths:\\s*.*?(\\d+)");
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("(?i)Crypts:\\s*(\\d+)");
    private static final Pattern PUZZLES_HEADER_PATTERN = Pattern.compile("(?i)Puzzles:\\s*\\((\\d+)\\)");

    public static DungeonStats parseDungeonStats(List<String> tabListLines) {
        DungeonStats stats = new DungeonStats();

        for (String line : tabListLines) {
            String cleanLine = line.trim();
            
            Matcher roomsMatcher = COMPLETED_ROOMS_PATTERN.matcher(cleanLine);
            if (roomsMatcher.find()) {
                stats.completedRooms = Integer.parseInt(roomsMatcher.group(1));
                stats.totalRooms = Integer.parseInt(roomsMatcher.group(2));
            }

            if (cleanLine.toLowerCase().contains("secrets found")) {
                String secretsPart = cleanLine.substring(cleanLine.toLowerCase().indexOf("found") + 5).trim();
                Pattern numPattern = Pattern.compile("(\\d+)");
                Matcher m = numPattern.matcher(secretsPart);
                List<Integer> nums = new ArrayList<>();
                while (m.find()) nums.add(Integer.parseInt(m.group(1)));

                if (!nums.isEmpty()) {
                    if (secretsPart.contains("%")) {
                        Matcher pm = Pattern.compile("(\\d+(?:\\.\\d+)?)%").matcher(secretsPart);
                        if (pm.find()) {
                            stats.secretPercent = (int) Double.parseDouble(pm.group(pm.groupCount() > 0 ? 1 : 0));
                        } else {
                            stats.secretPercent = nums.get(nums.size() - 1);
                        }
                    } else if (secretsPart.contains("/") || nums.size() == 1) {
                        stats.secretsFound = nums.get(0);
                    }
                }
            }

            Matcher deathsMatcher = DEATHS_PATTERN.matcher(cleanLine);
            if (deathsMatcher.find()) {
                stats.deaths = Integer.parseInt(deathsMatcher.group(1));
            }

            Matcher cryptsMatcher = CRYPTS_PATTERN.matcher(cleanLine);
            if (cryptsMatcher.find()) {
                stats.crypts = Integer.parseInt(cryptsMatcher.group(1));
            }

            Matcher puzzleHeaderMatcher = PUZZLES_HEADER_PATTERN.matcher(cleanLine);
            if (puzzleHeaderMatcher.find()) {
                stats.expectedPuzzles = Integer.parseInt(puzzleHeaderMatcher.group(1));
            }

            if (cleanLine.contains("Mimic:") && (cleanLine.contains("✔") || cleanLine.toLowerCase().contains("killed"))) {
                stats.mimicKilled = true;
            }
            if (cleanLine.contains("Prince:") && (cleanLine.contains("✔") || cleanLine.toLowerCase().contains("killed"))) {
                stats.princeKilled = true;
            }

            if (cleanLine.contains(":") && (cleanLine.contains("[✔]") || cleanLine.contains("[✖]") || cleanLine.contains("[✦]"))) {
                String name = cleanLine.split(":")[0].trim();
                if (!name.equalsIgnoreCase("Mimic") && !name.equalsIgnoreCase("Prince") && 
                    !name.equalsIgnoreCase("Secrets") && !name.equalsIgnoreCase("Deaths") && 
                    !name.equalsIgnoreCase("Crypts")) {
                    if (cleanLine.contains("[✔]") || (name.equalsIgnoreCase("Quiz") && cleanLine.contains("[✦]"))) {
                        stats.completedPuzzles.add(name);
                    }
                }
            }
        }
        
        return stats;
    }
}
