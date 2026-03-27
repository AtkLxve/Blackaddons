package org.blackum.blackaddons.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.ProfileStateManager;
import org.blackum.blackaddons.core.model.DungeonFloor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonScore {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlackAddons-DungeonScore");

    // Patterns from Skyblocker & Tablist
    private static final Pattern SECRETS_PATTERN = Pattern
            .compile("(?i)Secrets (?:Found|):?\\s*(\\d+(?:\\.\\d+)?)(?:%|)");
    private static final Pattern PUZZLES_PATTERN = Pattern.compile("(?i).+?:\\s*\\[(.)\\]");
    private static final Pattern PUZZLE_COUNT_PATTERN = Pattern.compile("(?i)Puzzles:\\s*\\((\\d+)\\)");
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("(?i)Crypts:\\s*(\\d+)");
    private static final Pattern COMPLETED_ROOMS_PATTERN = Pattern.compile("(?i)Completed Rooms:\\s*(\\d+)");

    private static FloorRequirement floorRequirement = FloorRequirement.NONE;
    private static String currentFloor = "";
    private static boolean isCurrentFloorEntrance;
    private static boolean floorHasMimics;

    private static boolean sent270;
    private static boolean sent300;
    private static boolean mimicKilled;
    private static boolean princeKilled;
    private static boolean dungeonStarted;
    private static boolean bloodRoomCompleted;
    private static long startingTime;
    private static int puzzleCount;
    private static int score;

    public static void update() {
        if (!LocationUtils.inDungeons()) {
            reset();
            return;
        }

        if (!dungeonStarted) {
            onDungeonStart();
        }

        if (dungeonStarted && puzzleCount == 0) {
            puzzleCount = getPuzzleCountFromTab();
        }

        score = calculateScore();
        checkThresholds();
    }

    private static void checkThresholds() {
        if (!ConfigManager.data.enableDungeonScoreAnnouncements)
            return;

        if (!sent270 && score >= 270 && score < 300) {
            sendMessage("/pc " + ConfigManager.data.dungeonScore270Message);
            sent270 = true;
        }

        if (!sent300 && score >= 300) {
            sendMessage("/pc " + ConfigManager.data.dungeonScore300Message);
            sent300 = true;
        }
    }

    private static void sendMessage(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendChat(msg);
        }
    }

    public static void reset() {
        floorRequirement = FloorRequirement.NONE;
        currentFloor = "";
        isCurrentFloorEntrance = false;
        floorHasMimics = false;
        sent270 = false;
        sent300 = false;
        mimicKilled = false;
        princeKilled = false;
        dungeonStarted = false;
        bloodRoomCompleted = false;
        startingTime = 0L;
        puzzleCount = 0;
        score = 0;
    }

    private static void onDungeonStart() {
        DungeonFloor floor = LocationUtils.getCurrentFloor();
        if (floor == null)
            return;

        currentFloor = floor.getDisplayName();
        dungeonStarted = true;
        startingTime = System.currentTimeMillis();

        try {
            floorRequirement = FloorRequirement.valueOf(currentFloor.replace("Entrance", "E"));
        } catch (IllegalArgumentException e) {
            floorRequirement = FloorRequirement.NONE;
        }

        floorHasMimics = currentFloor.matches("[FM][67]");
        isCurrentFloorEntrance = currentFloor.equals("Entrance");

        // Initial puzzle count from tab
        puzzleCount = getPuzzleCountFromTab();
    }

    public static int calculateScore() {
        int timeScore = calculateTimeScore();
        int exploreScore = calculateExploreScore();
        int skillScore = calculateSkillScore();
        int bonusScore = calculateBonusScore();

        if (isCurrentFloorEntrance) {
            return Math.round(timeScore * 0.7f) + Math.round(exploreScore * 0.7f) + Math.round(skillScore * 0.7f)
                    + Math.round(bonusScore * 0.7f);
        }
        return timeScore + exploreScore + skillScore + bonusScore;
    }

    private static int calculateSkillScore() {
        int totalRooms = getTotalRooms();
        int completedRooms = getCompletedRooms();
        int extraRooms = getExtraCompletedRooms();

        int completedRoomScore = totalRooms != 0 ? (int) (80.0 * (completedRooms + extraRooms) / totalRooms) : 0;
        completedRoomScore = Math.min(80, Math.max(0, completedRoomScore));

        return 20 + Math.min(80, Math.max(0, completedRoomScore - getPuzzlePenalty()));
    }

    private static int calculateExploreScore() {
        int totalRooms = getTotalRooms();
        int completedRooms = getCompletedRooms();
        int extraRooms = getExtraCompletedRooms();

        int completedRoomScore = totalRooms != 0 ? (int) (60.0 * (completedRooms + extraRooms) / totalRooms) : 0;
        completedRoomScore = Math.min(60, Math.max(0, completedRoomScore));

        double secretsPercent = getSecretsPercentage();
        int secretsScore = (int) (40 * Math.min(floorRequirement.percentage, secretsPercent)
                / Math.max(1, floorRequirement.percentage));
        secretsScore = Math.min(40, Math.max(0, secretsScore));

        return completedRoomScore + secretsScore;
    }

    private static int calculateTimeScore() {
        if (floorRequirement == FloorRequirement.NONE || startingTime == 0)
            return 100;

        int timeSpent = (int) (System.currentTimeMillis() - startingTime) / 1000;
        if (timeSpent < floorRequirement.timeLimit)
            return 100;

        double timePastReq = ((double) (timeSpent - floorRequirement.timeLimit) / floorRequirement.timeLimit) * 100;

        int score = 100;
        if (timePastReq < 20)
            score -= (int) (timePastReq / 2);
        else if (timePastReq < 40)
            score -= (int) (10 + (timePastReq - 20) / 4);
        else if (timePastReq < 50)
            score -= (int) (15 + (timePastReq - 40) / 5);
        else if (timePastReq < 60)
            score -= (int) (17 + (timePastReq - 50) / 6);
        else
            score -= (int) (18.67 + (timePastReq - 60) / 7);

        return Math.max(0, score);
    }

    private static int calculateBonusScore() {
        int bonus = 0;
        bonus += Math.min(5, getCrypts());
        if (mimicKilled || (getSecretsPercentage() >= 100 && floorHasMimics))
            bonus += 2;
        if (princeKilled)
            bonus += 1;
        return bonus;
    }

    // Data Extraction Helpers
    private static int getTotalRooms() {
        int completed = getCompletedRooms();
        double clear = getClearPercentage();
        if (clear <= 0)
            return 1;
        return (int) Math.round(completed / clear);
    }

    private static int getCompletedRooms() {
        List<String> tab = TabListUtils.getTabListLines();
        for (String line : tab) {
            Matcher m = COMPLETED_ROOMS_PATTERN.matcher(line);
            if (m.find())
                return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static int getExtraCompletedRooms() {
        if (!bloodRoomCompleted)
            return isCurrentFloorEntrance ? 1 : 2;
        if (!LocationUtils.inBoss() && !isCurrentFloorEntrance)
            return 1;
        return 0;
    }

    private static double getClearPercentage() {
        List<String> sidebar = ScoreboardUtils.getCleanSidebarLines();
        for (String line : sidebar) {
            if (line.contains("Cleared: ")) {
                try {
                    String percentStr = line.split("Cleared: ")[1].split("%")[0];
                    return Double.parseDouble(percentStr) / 100.0;
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private static int getCrypts() {
        List<String> tab = TabListUtils.getTabListLines();
        for (String line : tab) {
            Matcher m = CRYPTS_PATTERN.matcher(line);
            if (m.find())
                return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static double getSecretsPercentage() {
        List<String> tab = TabListUtils.getTabListLines();
        for (String line : tab) {
            String cleanLine = line.trim().replaceAll("(?i)§[0-9a-fk-or]", "");
            Matcher m = SECRETS_PATTERN.matcher(cleanLine);
            if (m.find()) {
                String val = m.group(1);
                try {
                    double d = Double.parseDouble(val);
                    return d;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private static int getPuzzleCountFromTab() {
        List<String> tab = TabListUtils.getTabListLines();
        for (String line : tab) {
            Matcher m = PUZZLE_COUNT_PATTERN.matcher(line);
            if (m.find())
                return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static int getPuzzlePenalty() {
        int completed = 0;
        List<String> tab = TabListUtils.getTabListLines();
        
        for (String line : tab) {
            String cleanLine = line.trim();
            if (cleanLine.isEmpty()) continue;
            
            // Exclude known non-puzzle headers
            if (cleanLine.contains("Mimic") || cleanLine.contains("Prince") || 
                cleanLine.contains("Secrets") || cleanLine.contains("Deaths") || 
                cleanLine.contains("Crypts") || cleanLine.contains("Completed Rooms") ||
                cleanLine.contains("Puzzles: (")) continue;
            
            // Check for any of the checkmark symbols
            if (cleanLine.contains("\u2714") || cleanLine.contains("\u2713") || 
                cleanLine.contains("\u2705") || cleanLine.contains("✔")) {
                completed++;
            }
        }
        
        // Penalty is 10 points for each puzzle that is NOT completed
        return Math.max(0, (puzzleCount - completed)) * 10;
    }

    public static void onMimicKill() {
        if (mimicKilled)
            return;
        mimicKilled = true;
        if (ConfigManager.data.enableMimicKilledMessage) {
            sendMessage("/pc " + ConfigManager.data.mimicKilledMessage);
        }
    }

    public static void onPrinceKill() {
        if (princeKilled)
            return;
        princeKilled = true;
        if (ConfigManager.data.enablePrinceKilledMessage) {
            sendMessage("/pc " + ConfigManager.data.princeKilledMessage);
        }
    }

    public static void onBloodRoomPassed() {
        bloodRoomCompleted = true;
    }

    public static List<String> getScoreBreakdown() {
        List<String> lines = new ArrayList<>();
        if (!dungeonStarted) {
            lines.add("§cDungeon hasn't started yet!");
            return lines;
        }

        int timeScore = calculateTimeScore();
        int exploreScore = calculateExploreScore();
        int skillScore = calculateSkillScore();
        int bonusScore = calculateBonusScore();
        int total = calculateScore();

        lines.add("§6§lDungeon Score Breakdown:");
        lines.add("§7Floor: §e" + currentFloor);
        lines.add("§7Time Score: §a" + timeScore + " §8/ 100");
        lines.add("§7Skill Score: §a" + skillScore + " §8/ 100");
        lines.add("§7Explore Score: §a" + exploreScore + " §8/ 100");
        lines.add("§7Bonus Score: §a" + bonusScore + " §8/ 5+");
        lines.add("§7--------------------");
        lines.add("§6§lTotal Score: §e" + total);

        lines.add("");
        lines.add("§e§lDetails:");
        int penalty = getPuzzlePenalty();
        int completed = puzzleCount - (penalty / 10);
        lines.add("§7- Puzzles: §a" + completed + " §8/ " + puzzleCount);
        lines.add("§7- Secrets: §a" + String.format("%.1f", getSecretsPercentage()) + "% §8(Req: "
                + floorRequirement.percentage + "%)");
        lines.add("§7- Mimic: " + (mimicKilled ? "§aKilled" : "§cNot Killed"));
        lines.add("§7- Prince: " + (princeKilled ? "§aKilled" : "§cNot Killed"));
        lines.add("§7- Crypts: §a" + getCrypts() + " §8/ 5");

        return lines;
    }

    public static int getScore() {
        return score;
    }

    public static boolean isMimicKilled() {
        return mimicKilled;
    }

    public static boolean isPrinceKilled() {
        return princeKilled;
    }

    public static boolean isDungeonStarted() {
        return dungeonStarted;
    }

    enum FloorRequirement {
        E(30, 1200),
        F1(30, 600),
        F2(40, 600),
        F3(50, 600),
        F4(60, 720),
        F5(70, 600),
        F6(85, 720),
        F7(100, 840),
        M1(100, 480),
        M2(100, 480),
        M3(100, 480),
        M4(100, 480),
        M5(100, 480),
        M6(100, 600),
        M7(100, 840),
        NONE(0, 0);

        private final int percentage;
        private final int timeLimit;

        FloorRequirement(int percentage, int timeLimit) {
            this.percentage = percentage;
            this.timeLimit = timeLimit;
        }
    }
}
