package org.blackum.blackaddons.feature.dungeon.tracker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.common.util.mc.ScoreboardUtils;
import org.blackum.blackaddons.common.util.mc.TabListUtils;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMapSerializer;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;
import org.blackum.blackaddons.feature.dungeon.util.DungeonUtils;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.service.BotIntegration;
import org.blackum.blackaddons.service.MojangAuthService;

public class SoloClearsTracker {
    private static boolean runRecorded = false;
    private static boolean princeKilledThisRun = false;
    private static int ticks = 0;
    private static String lastLocation = "";
    private static long dungeonEnterTick = -1L;
    private static long dungeonEnterClock = 0L;
    private static boolean isSoloThisRun = false;
    private static int chatScore = -1;
    private static String chatTime = null;

    private static final String SOLO_TEXT = "Solo";
    private static final String PARTY_ONE_TEXT = "Party (1)";
    private static final Pattern TIME_PATTERN = Pattern.compile("(?i)Time Elapsed:\\s*([0-9][0-9msh:\\s]*s?)");
    private static final Pattern TABLIST_TIME_PATTERN = Pattern.compile("(?i)\\bTime:\\s*([0-9][0-9msh:\\s]*s?)");
    private static final Pattern CHAT_SCORE_PATTERN = Pattern.compile("(?i)Team Score:\\s*(\\d+)");
    private static final Pattern CHAT_TIME_PATTERN = Pattern.compile("(?i)Clear Time:\\s*([0-9][0-9msh:\\s]*s?)");

    public static void tick() {
        ticks++;
        if (ticks % 10 != 0)
            return;

        String currentLocation = LocationUtils.getLocation();
        if (!LocationUtils.inDungeons()) {
            runRecorded = false;
            lastLocation = currentLocation;
            princeKilledThisRun = false;
            dungeonEnterTick = -1L;
            dungeonEnterClock = 0L;
            isSoloThisRun = false;
            chatScore = -1;
            chatTime = null;
            DungeonScore.reset();
            return;
        }

        if (dungeonEnterTick < 0) {
            dungeonEnterTick = ticks;
            dungeonEnterClock = System.currentTimeMillis();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§d[SoloClears Debug] Dungeon start detected"));
            }
        }

        if (!lastLocation.equals(currentLocation)) {
            runRecorded = false;
            lastLocation = currentLocation;
            princeKilledThisRun = false;
            isSoloThisRun = false;
            chatScore = -1;
            chatTime = null;
            DungeonScore.reset();
        }

        DungeonScore.update();
        if (runRecorded)
            return;

        DungeonFloor floor = LocationUtils.getCurrentFloor();
        if (floor == null)
            return;

        String floorName = floor.getDisplayName();
        if (!floorName.equals("F7") && !floorName.equals("M7"))
            return;

        List<String> scoreboardLines = ScoreboardUtils.getCleanSidebarLines();
        List<String> tabListLines = new ArrayList<>(TabListUtils.getTabListLines());
        tabListLines.addAll(TabListUtils.getFooterLines());

        boolean isSolo = false;
        String time = "Unknown";

        for (String line : scoreboardLines) {
            String cleanLine = line.trim();
            if (cleanLine.contains(SOLO_TEXT) || cleanLine.contains(PARTY_ONE_TEXT))
                isSolo = true;
            Matcher timeMatcher = TIME_PATTERN.matcher(cleanLine);
            if (timeMatcher.find()) {
                time = timeMatcher.group(1).trim();
            }
        }

        for (String line : tabListLines) {
            String cleanLine = line.trim();
            if (cleanLine.contains(SOLO_TEXT) || cleanLine.contains(PARTY_ONE_TEXT))
                isSolo = true;
            if (time.equals("Unknown")) {
                Matcher timeMatcher = TABLIST_TIME_PATTERN.matcher(cleanLine);
                if (timeMatcher.find()) {
                    time = timeMatcher.group(1).trim();
                }
            }
        }

        if (isSolo) {
            isSoloThisRun = true;
        }

        DungeonUtils.DungeonStats stats = DungeonUtils.parseDungeonStats(tabListLines);

        int finalScore = DungeonScore.getScore();
        if (chatScore >= 300 && chatScore > finalScore) {
            finalScore = chatScore;
        }

        String finalTime = time;
        if (finalTime.equals("Unknown") && chatTime != null) {
            finalTime = chatTime;
        }

        if (ticks % 100 == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(
                        "§d[SoloClears Debug] Floor: " + floorName +
                                " | isSolo: " + isSolo + " (Stored: " + isSoloThisRun + ")" +
                                " | Time: " + finalTime +
                                " | Score: " + finalScore +
                                " | runRecorded: " + runRecorded));
            }
        }

        boolean mimicKilled = DungeonScore.isMimicKilled() || stats.mimicKilled;
        boolean princeDefeated = DungeonScore.isPrinceKilled() || stats.princeKilled || princeKilledThisRun;

        if (finalScore >= 300 && isSoloThisRun && !finalTime.equals("Unknown") && !finalTime.equals("00m 00s")
                && !finalTime.equals("00:00")) {
            List<ConfigManager.SoloClearInfo> floorClears = floorName.equals("M7")
                    ? ConfigManager.data.m7SoloClears
                    : ConfigManager.data.f7SoloClears;

            int newTimeSeconds = parseTimeToSeconds(finalTime);
            boolean isNewPB = true;
            if (newTimeSeconds == Integer.MAX_VALUE) {
                isNewPB = false;
            } else {
                for (ConfigManager.SoloClearInfo existing : floorClears) {
                    int existingSeconds = parseTimeToSeconds(existing.time);
                    if (existingSeconds != Integer.MAX_VALUE && existingSeconds <= newTimeSeconds) {
                        isNewPB = false;
                        break;
                    }
                }
            }

            final com.google.gson.JsonObject mapData = DungeonMapSerializer.serialize();
            ConfigManager.SoloClearInfo info = new ConfigManager.SoloClearInfo(floorName, finalTime, stats.secretsFound,
                    stats.completedPuzzles, princeDefeated, mimicKilled, mapData);
            if (floorName.equals("M7")) {
                ConfigManager.data.m7SoloClears.add(info);
            } else {
                ConfigManager.data.f7SoloClears.add(info);
            }
            ConfigManager.save();
            runRecorded = true;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String colorTime = "§e" + finalTime;
                String puzzleStr = stats.completedPuzzles.isEmpty() ? "None"
                        : String.join(", ", stats.completedPuzzles);
                String princeStr = princeDefeated ? "§a✔" : "§c✘";
                String mimicStr = mimicKilled ? "§a✔" : "§c✘";
                mc.player.sendSystemMessage(
                        ChatUtils.getMessage("§b§l" + floorName + " SOLO CLEAR DONE! §r§fTime: " + colorTime +
                                " §r§fSecrets: §b" + stats.secretsFound + " §r§fPuzzles: §d[" + puzzleStr + "] " +
                                "§r§fPrince: " + princeStr + " §r§fMimic: " + mimicStr));

                if (isNewPB) {
                    final String player = mc.getUser().getName();
                    final String normalizedTime = normalizeTimeForBot(finalTime);
                    final String submittedFloor = floorName;
                    final int submittedSecrets = stats.secretsFound;
                    final int submittedDeaths = stats.deaths;
                    final int submittedCrypts = stats.crypts;
                    final List<String> submittedPuzzles = new ArrayList<>(stats.completedPuzzles);
                    final boolean submittedPrince = princeDefeated;
                    final boolean submittedMimic = mimicKilled;
                    final boolean needsVerification = newTimeSeconds < 180;

                    final List<String> rawScoreboardLines = new ArrayList<>(ScoreboardUtils.getSidebarLines());
                    final List<String> rawTablistLines = new ArrayList<>(TabListUtils.getRawTabListLines());
                    final Map<String, Integer> components = new LinkedHashMap<>(DungeonScore.getScoreComponents());
                    final long enterTick = dungeonEnterTick;
                    final long enterClock = dungeonEnterClock;
                    final long clearTick = ticks;
                    final long clearClock = System.currentTimeMillis();
                    final String serverId = MojangAuthService.generateServerId();
                    final String playerUuid = mc.getUser().getProfileId().toString();

                    MojangAuthService.joinServer(serverId)
                            .thenCompose(ok -> {
                                if (!ok) {
                                    Blackaddons.LOGGER.warn("[SoloClears] joinServer failed, skipping pre-verify");
                                    return CompletableFuture.<Boolean>completedFuture(false);
                                }
                                return BotIntegration.preVerifyMojang(player, playerUuid, serverId);
                            })
                            .thenCompose(preVerified -> {
                                Blackaddons.LOGGER.info("[SoloClears] Mojang pre-verify: {}", preVerified);
                                return BotIntegration.sendSoloClear(player, playerUuid, submittedFloor, normalizedTime,
                                        submittedSecrets, submittedDeaths, submittedCrypts,
                                        submittedPuzzles, submittedPrince, submittedMimic, needsVerification,
                                        rawScoreboardLines, rawTablistLines, components,
                                        enterTick, clearTick, enterClock, clearClock, serverId, mapData);
                            })
                            .thenAccept(res -> {
                                if (res != null && mc.player != null) {
                                    mc.execute(() -> mc.player.sendSystemMessage(
                                            ChatUtils.getMessage("§a[SoloClears] New PB submitted to leaderboard!")));
                                }
                            });
                }
            }
            NotificationManager.addNotification("Solo Clear",
                    floorName + " Clear Recorded: " + finalTime + " (" + stats.secretsFound + " secrets)",
                    NotificationType.SUCCESS);
        }
    }

    private static String normalizeTimeForBot(String raw) {
        if (raw == null)
            return "00:00";
        if (raw.matches("\\d+:\\d+.*"))
            return raw;
        Matcher m = Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?").matcher(raw);
        if (m.find()) {
            int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            return String.format("%02d:%02d", mins, secs);
        }
        return raw;
    }

    private static int parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty() || timeStr.equals("Unknown"))
            return Integer.MAX_VALUE;
        try {
            if (timeStr.contains("m") || timeStr.contains("s")) {
                Matcher m = Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?").matcher(timeStr);
                if (m.find()) {
                    int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
                    int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                    int total = mins * 60 + secs;
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            } else if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                if (parts.length >= 2) {
                    int total = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            }
        } catch (Exception ignored) {
        }
        return Integer.MAX_VALUE;
    }

    public static long getDungeonEnterTick() {
        return dungeonEnterTick;
    }

    public static long getDungeonEnterClock() {
        return dungeonEnterClock;
    }

    public static int getCurrentTick() {
        return ticks;
    }

    public static void onChatMessage(Component message) {
        String cleanText = message.getString().replaceAll("§.", "").trim();
        if (cleanText.contains("A Prince falls. +1 Bonus Score")) {
            princeKilledThisRun = true;
            DungeonScore.onPrinceKill();
        } else if (cleanText.contains("[BOSS] The Watcher: You have proven yourself. You may pass.")) {
            DungeonScore.onBloodRoomPassed();
        }

        Matcher scoreMatcher = CHAT_SCORE_PATTERN.matcher(cleanText);
        if (scoreMatcher.find()) {
            try {
                chatScore = Integer.parseInt(scoreMatcher.group(1));
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("§d[SoloClears Debug] Chat score: " + chatScore));
                }
            } catch (Exception ignored) {
            }
        }

        Matcher timeMatcher = CHAT_TIME_PATTERN.matcher(cleanText);
        if (timeMatcher.find()) {
            chatTime = timeMatcher.group(1).trim();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§d[SoloClears Debug] Chat time: " + chatTime));
            }
        }
    }

    public static void dumpDebugInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        mc.player.sendSystemMessage(Component.literal("§b§l=== SoloClears Debug ==="));
        mc.player.sendSystemMessage(Component.literal("§7inDungeons: §e" + LocationUtils.inDungeons()
                + "  floor: §e" + LocationUtils.getCurrentFloor()
                + "  runRecorded: §e" + runRecorded));

        List<String> scoreboardLines = ScoreboardUtils.getCleanSidebarLines();
        DungeonFloor floor = LocationUtils.getCurrentFloor();
        String floorName = floor != null ? floor.getDisplayName() : "null";

        boolean isSolo = false;
        String time = "Unknown";
        for (String line : scoreboardLines) {
            String cl = line.trim();
            if (cl.contains(SOLO_TEXT) || cl.contains(PARTY_ONE_TEXT))
                isSolo = true;
            Matcher tm = TIME_PATTERN.matcher(cl);
            if (tm.find())
                time = tm.group(1).trim();
        }

        List<String> tabListLines = new ArrayList<>(TabListUtils.getTabListLines());
        tabListLines.addAll(TabListUtils.getFooterLines());
        for (String line : tabListLines) {
            String cl = line.trim();
            if (cl.contains(SOLO_TEXT) || cl.contains(PARTY_ONE_TEXT))
                isSolo = true;
            if (time.equals("Unknown")) {
                Matcher tm = TABLIST_TIME_PATTERN.matcher(cl);
                if (tm.find())
                    time = tm.group(1).trim();
            }
        }

        int finalScore = DungeonScore.getScore();

        mc.player.sendSystemMessage(Component.literal("§7floor: §e" + floorName
                + "  isSolo: §e" + isSolo
                + "  time: §e" + time
                + "  score: §e" + finalScore));
        mc.player.sendSystemMessage(Component.literal("§7Conditions: score>=300=§e" + (finalScore >= 300)
                + "  timeOk=§e" + (!time.equals("Unknown") && !time.equals("00m 00s") && !time.equals("00:00"))
                + "  F7orM7=§e" + (floorName.equals("F7") || floorName.equals("M7"))));

        mc.player.sendSystemMessage(Component.literal("§e--- Sidebar Lines ---"));
        for (String line : scoreboardLines) {
            mc.player.sendSystemMessage(Component.literal("§7| " + line));
        }

        List<String> footerLines = TabListUtils.getFooterLines();
        mc.player.sendSystemMessage(Component.literal("§e--- Footer Lines (" + footerLines.size() + ") ---"));
        for (String line : footerLines) {
            mc.player.sendSystemMessage(Component.literal("§7| " + line));
        }

        List<String> rawTab = TabListUtils.getTabListLines();
        mc.player.sendSystemMessage(Component.literal("§e--- Tablist (first 20 of " + rawTab.size() + ") ---"));
        for (int i = 0; i < Math.min(20, rawTab.size()); i++) {
            mc.player.sendSystemMessage(Component.literal("§7| " + rawTab.get(i)));
        }
    }
}
