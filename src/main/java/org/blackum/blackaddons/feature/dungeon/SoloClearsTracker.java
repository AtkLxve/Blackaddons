package org.blackum.blackaddons.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.model.DungeonFloor;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.core.util.ScoreboardUtils;
import org.blackum.blackaddons.core.util.TabListUtils;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

import org.blackum.blackaddons.feature.chat.ChatUtils;
import net.minecraft.ChatFormatting;
import org.blackum.blackaddons.core.util.DungeonScore;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoloClearsTracker {
    private static boolean runRecorded = false;
    private static boolean princeKilledThisRun = false;
    private static int ticks = 0;
    private static String lastLocation = "";


    private static final Pattern SCORE_PATTERN = Pattern.compile("(?i)(?:Score:\\s*(\\d+))|(?:Cleared:.*?\\((\\d+)\\))");
    private static final Pattern TIME_PATTERN = Pattern.compile("(?i)(?:Elapsed|Time|Cleared:.*?\\(\\d+\\))\\s*:?\\s*[^0-9\\s]*\\s*([0-9][0-9:m\\s]*s?)");
    

    private static final Pattern COMPLETED_ROOMS_PATTERN = Pattern.compile("(?i)Completed Rooms:\\s*(\\d+)(?:/|\\s*out\\s*of\\s*)(\\d+)");
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("(?i)Crypts:\\s*(\\d+)");
    private static final Pattern PUZZLES_HEADER_PATTERN = Pattern.compile("(?i)Puzzles:\\s*\\((\\d+)\\)");

    public static void tick() {
        ticks++;
        if (ticks % 10 != 0) return;

        String currentLocation = LocationUtils.getLocation();
        if (!LocationUtils.inDungeons()) {
            runRecorded = false;
            lastLocation = currentLocation;
            princeKilledThisRun = false;
            DungeonScore.reset();
            return;
        }

        if (!lastLocation.equals(currentLocation)) {
            runRecorded = false;
            lastLocation = currentLocation;
            princeKilledThisRun = false;
            DungeonScore.reset();
        }

        DungeonScore.update();
        if (runRecorded) return;

        DungeonFloor floor = LocationUtils.getCurrentFloor();
        if (floor == null) return;
        
        String floorName = floor.getDisplayName();
        if (!floorName.equals("F7") && !floorName.equals("M7")) return;

        List<String> scoreboardLines = ScoreboardUtils.getCleanSidebarLines();
        List<String> tabListLines = TabListUtils.getTabListLines();

        boolean isSolo = false;
        String time = "Unknown";
        
        int sidebarScore = 0;
        for (String line : scoreboardLines) {
            String cleanLine = line.trim();
            if (cleanLine.contains("Solo")) isSolo = true;
            
            Matcher timeMatcher = TIME_PATTERN.matcher(cleanLine);
            if (timeMatcher.find()) {
                time = timeMatcher.group(1).trim();
            }
            
            Matcher scoreMatcher = SCORE_PATTERN.matcher(cleanLine);
            if (scoreMatcher.find()) {
                try {
                    String strScore = scoreMatcher.group(1) != null ? scoreMatcher.group(1) : scoreMatcher.group(2);
                    if (strScore != null) sidebarScore = Integer.parseInt(strScore);
                } catch (NumberFormatException ignored) {}
            }
        }

        for (String line : tabListLines) {
            if (line.trim().contains("Solo")) isSolo = true;
        }

        org.blackum.blackaddons.core.util.DungeonUtils.DungeonStats stats = org.blackum.blackaddons.core.util.DungeonUtils.parseDungeonStats(tabListLines);
        
        int finalScore = DungeonScore.getScore();

        boolean mimicKilled = DungeonScore.isMimicKilled() || stats.mimicKilled;
        boolean princeDefeated = DungeonScore.isPrinceKilled() || stats.princeKilled || princeKilledThisRun;

        // TRIGGER: Manual Score >= 300
        if (finalScore >= 300 && isSolo && !time.equals("Unknown") && !time.equals("00m 00s") && !time.equals("00:00")) {
            ConfigManager.SoloClearInfo info = new ConfigManager.SoloClearInfo(floorName, time, stats.secretsFound, stats.completedPuzzles, princeDefeated, mimicKilled);
            if (floorName.equals("M7")) {
                ConfigManager.data.m7SoloClears.add(info);
            } else {
                ConfigManager.data.f7SoloClears.add(info);
            }
            ConfigManager.save();
            runRecorded = true;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String colorTime = "§e" + time;
                String puzzleStr = stats.completedPuzzles.isEmpty() ? "None" : String.join(", ", stats.completedPuzzles);
                String princeStr = princeDefeated ? "§a✔" : "§c✘";
                String mimicStr = mimicKilled ? "§a✔" : "§c✘";
                mc.player.displayClientMessage(ChatUtils.getMessage("§b§l" + floorName + " SOLO CLEAR DONE! §r§fTime: " + colorTime + 
                    " §r§fSecrets: §b" + stats.secretsFound + " §r§fPuzzles: §d[" + puzzleStr + "] " + 
                    "§r§fPrince: " + princeStr + " §r§fMimic: " + mimicStr), false);
            }
            NotificationManager.addNotification("Solo Clear", floorName + " Clear Recorded: " + time + " (" + stats.secretsFound + " secrets)", NotificationType.SUCCESS);
        }
    }

    public static void onChatMessage(Component message) {
        String cleanText = message.getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim();
        if (cleanText.contains("A Prince falls. +1 Bonus Score")) {
            princeKilledThisRun = true;
            DungeonScore.onPrinceKill();
        } else if (cleanText.contains("[BOSS] The Watcher: You have proven yourself. You may pass.")) {
            DungeonScore.onBloodRoomPassed();
        }
    }



    public static void dumpDebugInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.displayClientMessage(Component.literal("§b[SoloClears Debug] §fDungeon: " + LocationUtils.inDungeons() + " | Floor: " + LocationUtils.getCurrentFloor()), false);
        
        List<String> scoreboardLines = ScoreboardUtils.getCleanSidebarLines();
        mc.player.displayClientMessage(Component.literal("§e--- Sidebar Lines ---"), false);
        for (String line : scoreboardLines) {
            mc.player.displayClientMessage(Component.literal("§7- " + line), false);
        }

        List<String> tabListLines = TabListUtils.getTabListLines();
        mc.player.displayClientMessage(Component.literal("§e--- Tablist Lines (First 20) ---"), false);
        for (int i = 0; i < Math.min(20, tabListLines.size()); i++) {
            mc.player.displayClientMessage(Component.literal("§7- " + tabListLines.get(i)), false);
        }
    }
}
