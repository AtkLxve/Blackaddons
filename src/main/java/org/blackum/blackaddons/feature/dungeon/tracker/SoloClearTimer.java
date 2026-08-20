package org.blackum.blackaddons.feature.dungeon.tracker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;

import java.util.Locale;
import java.util.regex.Pattern;

public class SoloClearTimer {
    private static final long NORMAL_TICK_MS = 50L;
    private static final long FREEZE_THRESHOLD_MS = 100L;
    private static final String MORT_START_MESSAGE = "[NPC] Mort: Here, I found this map when I first entered the dungeon.";
    private static final Pattern CLEAR_CHAT_PATTERN = Pattern.compile("(?i)Team Score:\\s*\\d+|Clear Time:\\s*[0-9]");

    private static long startTimeMs = 0L;
    private static long lastPacketTime = 0L;
    private static long totalDesyncMs = 0L;
    private static boolean running = false;
    private static boolean completed = false;
    private static boolean wasInDungeon = false;

    private static long finalRtaMs = 0L;
    private static long finalIgtMs = 0L;
    private static long finalDesyncMs = 0L;

    public static void onPacket() {
        long now = System.currentTimeMillis();
        if (running && lastPacketTime > 0L) {
            long diff = now - lastPacketTime;
            if (diff > FREEZE_THRESHOLD_MS) {
                totalDesyncMs += (diff - NORMAL_TICK_MS);
            }
        }
        lastPacketTime = now;
    }

    public static void tick() {
        if (!LocationUtils.inDungeons()) {
            if (wasInDungeon) {
                reset();
            }
            wasInDungeon = false;
            return;
        }

        wasInDungeon = true;

        if (!SoloClearsTracker.isSoloThisRun()) {
            return;
        }

        if (running) {
            if (SoloClearsTracker.isRunRecorded() || DungeonScore.getScore() >= 300) {
                completeRun();
            }
        }
    }

    public static void onChatMessage(Component message) {
        if (message == null) {
            return;
        }
        String cleanText = ChatFormatting.stripFormatting(message.getString());
        if (cleanText == null) {
            return;
        }
        cleanText = cleanText.trim();
        if (cleanText.contains(MORT_START_MESSAGE)) {
            startRun();
            return;
        }
        if (running && CLEAR_CHAT_PATTERN.matcher(cleanText).find()) {
            completeRun();
        }
    }

    private static void startRun() {
        startTimeMs = System.currentTimeMillis();
        lastPacketTime = System.currentTimeMillis();
        totalDesyncMs = 0L;
        running = true;
        completed = false;
        finalRtaMs = 0L;
        finalIgtMs = 0L;
        finalDesyncMs = 0L;
    }

    private static void completeRun() {
        long now = System.currentTimeMillis();
        long activeDesync = getCurrentDesyncMs(now);
        long elapsed = Math.max(0L, now - startTimeMs);

        finalIgtMs = elapsed;
        finalDesyncMs = activeDesync;
        finalRtaMs = Math.max(0L, finalIgtMs - finalDesyncMs);
        running = false;
        completed = true;
    }

    public static void reset() {
        startTimeMs = 0L;
        lastPacketTime = System.currentTimeMillis();
        totalDesyncMs = 0L;
        running = false;
        completed = false;
        finalRtaMs = 0L;
        finalIgtMs = 0L;
        finalDesyncMs = 0L;
    }

    public static boolean shouldRender() {
        return LocationUtils.inDungeons() && SoloClearsTracker.isSoloThisRun() && (running || completed);
    }

    private static long getCurrentDesyncMs(long now) {
        if (completed) {
            return finalDesyncMs;
        }
        long freezeExtra = 0L;
        if (running && lastPacketTime > 0L) {
            long diff = now - lastPacketTime;
            if (diff > FREEZE_THRESHOLD_MS) {
                freezeExtra = diff - NORMAL_TICK_MS;
            }
        }
        return totalDesyncMs + freezeExtra;
    }

    public static long getRtaMs() {
        if (completed) {
            return finalRtaMs;
        }
        if (!running) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long igt = Math.max(0L, now - startTimeMs);
        long desync = getCurrentDesyncMs(now);
        return Math.max(0L, igt - desync);
    }

    public static long getIgtMs() {
        if (completed) {
            return finalIgtMs;
        }
        if (!running) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - startTimeMs);
    }

    public static long getDesyncMs() {
        if (completed) {
            return finalDesyncMs;
        }
        if (!running) {
            return 0L;
        }
        return getCurrentDesyncMs(System.currentTimeMillis());
    }

    public static String formatTimeWithMinutes(long ms) {
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long millis = ms % 1000L;
        return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis);
    }

    public static String formatDesync(long ms) {
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long millis = ms % 1000L;
        if (minutes > 0L) {
            return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis);
        }
        return String.format(Locale.ROOT, "%02d.%03d", seconds, millis);
    }
}
