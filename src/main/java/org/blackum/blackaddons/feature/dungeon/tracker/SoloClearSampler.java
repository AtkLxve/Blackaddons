package org.blackum.blackaddons.feature.dungeon.tracker;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.common.util.mc.ScoreboardUtils;
import org.blackum.blackaddons.common.util.mc.TabListUtils;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMapSerializer;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;
import org.blackum.blackaddons.feature.dungeon.util.DungeonUtils;
import org.blackum.blackaddons.service.MojangAuthService;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoloClearSampler {

    private static final Path SAMPLES_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(Constants.CONFIG_DIR_NAME)
            .resolve(Constants.DATA_DIR_NAME)
            .resolve("solo_clear_samples.json");

    private static final Pattern STRIP_COLOR = Pattern.compile("§.");
    private static final Pattern SCOREBOARD_TIME = Pattern.compile("(?i)Time Elapsed:\\s*([0-9][0-9msh:\\s]*s?)");
    private static final Pattern SCOREBOARD_CLEARED = Pattern.compile("(?i)Cleared:\\s*(\\d+)%\\s*\\((\\d+)\\)");
    private static final Pattern TABLIST_PUZZLES_EXPECTED = Pattern.compile("(?i)Puzzles:\\s*\\((\\d+)\\)");
    private static final Pattern TABLIST_COMPLETED_ROOMS = Pattern.compile("(?i)Completed Rooms:\\s*(\\d+)");
    private static final Pattern TABLIST_CRYPTS = Pattern.compile("(?i)Crypts:\\s*(\\d+)");
    private static final Pattern TABLIST_TIME = Pattern.compile("(?i)\\bTime:\\s*([0-9][0-9msh:\\s]*s?)");

    public static int captureSample() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getUser() == null) {
            return -1;
        }

        DungeonFloor floor = LocationUtils.getCurrentFloor();
        String floorName = floor != null ? floor.getDisplayName() : "";

        List<String> rawScoreboard = ScoreboardUtils.getSidebarLines();
        List<String> rawTablist = TabListUtils.getRawTabListLines();
        List<String> cleanTablist = TabListUtils.getTabListLines();

        DungeonUtils.DungeonStats stats = DungeonUtils.parseDungeonStats(cleanTablist);
        Map<String, Integer> components = DungeonScore.getScoreComponents();

        JsonObject sample = new JsonObject();
        sample.addProperty("captured_at", System.currentTimeMillis());
        sample.addProperty("player", mc.getUser().getName());
        sample.addProperty("uuid", mc.getUser().getProfileId().toString());
        sample.addProperty("location", LocationUtils.getLocation());
        sample.addProperty("in_dungeon", LocationUtils.inDungeons());
        sample.addProperty("floor", floorName);
        sample.addProperty("mod_score", DungeonScore.getScore());
        sample.addProperty("secrets_found", stats.secretsFound);

        boolean mimicFromChat = DungeonScore.isMimicKilled();
        boolean princeFromChat = DungeonScore.isPrinceKilled();
        sample.addProperty("mimic_killed", mimicFromChat || stats.mimicKilled);
        sample.addProperty("mimic_killed_from_chat", mimicFromChat);
        sample.addProperty("mimic_killed_from_tablist", stats.mimicKilled);
        sample.addProperty("prince_killed", princeFromChat || stats.princeKilled);
        sample.addProperty("prince_killed_from_chat", princeFromChat);
        sample.addProperty("prince_killed_from_tablist", stats.princeKilled);
        sample.addProperty("bat_killed", DungeonScore.isBatKilled() || stats.batKilled);

        sample.add("scoreboard_lines", toJsonArray(rawScoreboard));
        sample.add("tablist_lines", toJsonArray(rawTablist));
        sample.add("completed_puzzles", toJsonArray(stats.completedPuzzles));

        if (!components.isEmpty()) {
            JsonObject comp = new JsonObject();
            for (Map.Entry<String, Integer> e : components.entrySet()) {
                comp.addProperty(e.getKey(), e.getValue());
            }
            sample.add("score_components", comp);
        }

        sample.add("parsed_scoreboard", parseScoreboard(rawScoreboard));
        sample.add("parsed_tablist", parseTablist(rawTablist));

        sample.addProperty("dungeon_enter_tick", SoloClearsTracker.getDungeonEnterTick());
        sample.addProperty("dungeon_enter_clock", SoloClearsTracker.getDungeonEnterClock());
        sample.addProperty("current_tick", SoloClearsTracker.getCurrentTick());
        sample.addProperty("current_clock", System.currentTimeMillis());

        sample.add("mojang", probeMojangSession());

        JsonObject mapData = DungeonMapSerializer.serialize();
        if (mapData != null) {
            sample.add("map_data", mapData);
        }

        return appendSample(sample);
    }

    private static String stripColor(String line) {
        if (line == null) return "";
        return STRIP_COLOR.matcher(line).replaceAll("");
    }

    private static JsonObject parseScoreboard(List<String> rawLines) {
        JsonObject out = new JsonObject();
        boolean hasSolo = false;
        String timeElapsed = null;
        Integer clearedPercent = null;
        Integer clearedScore = null;

        for (String raw : rawLines) {
            String clean = stripColor(raw);
            if (clean.contains("Solo")) hasSolo = true;
            if (timeElapsed == null) {
                Matcher m = SCOREBOARD_TIME.matcher(clean);
                if (m.find()) timeElapsed = m.group(1).trim();
            }
            if (clearedScore == null) {
                Matcher m = SCOREBOARD_CLEARED.matcher(clean);
                if (m.find()) {
                    try {
                        clearedPercent = Integer.parseInt(m.group(1));
                        clearedScore = Integer.parseInt(m.group(2));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        out.addProperty("has_solo", hasSolo);
        if (timeElapsed != null) out.addProperty("time_elapsed", timeElapsed);
        if (clearedPercent != null) out.addProperty("cleared_percent", clearedPercent);
        if (clearedScore != null) out.addProperty("cleared_score", clearedScore);
        return out;
    }

    private static JsonObject parseTablist(List<String> rawLines) {
        JsonObject out = new JsonObject();
        Integer puzzlesExpected = null;
        Integer completedRooms = null;
        Integer crypts = null;
        String tablistTime = null;

        for (String raw : rawLines) {
            String clean = stripColor(raw);
            if (puzzlesExpected == null) {
                Matcher m = TABLIST_PUZZLES_EXPECTED.matcher(clean);
                if (m.find()) {
                    try { puzzlesExpected = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
                }
            }
            if (completedRooms == null) {
                Matcher m = TABLIST_COMPLETED_ROOMS.matcher(clean);
                if (m.find()) {
                    try { completedRooms = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
                }
            }
            if (crypts == null) {
                Matcher m = TABLIST_CRYPTS.matcher(clean);
                if (m.find()) {
                    try { crypts = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
                }
            }
            if (tablistTime == null) {
                Matcher m = TABLIST_TIME.matcher(clean);
                if (m.find()) {
                    String candidate = m.group(1).trim();
                    if (!candidate.equalsIgnoreCase("N/A")) {
                        tablistTime = candidate;
                    }
                }
            }
        }
        if (puzzlesExpected != null) out.addProperty("puzzles_expected", puzzlesExpected);
        if (completedRooms != null) out.addProperty("completed_rooms", completedRooms);
        if (crypts != null) out.addProperty("crypts", crypts);
        if (tablistTime != null) out.addProperty("time", tablistTime);
        return out;
    }

    private static JsonObject probeMojangSession() {
        JsonObject out = new JsonObject();
        String serverId = MojangAuthService.generateServerId();
        out.addProperty("server_id", serverId);
        try {
            boolean verified = MojangAuthService.joinServer(serverId).join();
            out.addProperty("verified", verified);
        } catch (Exception e) {
            out.addProperty("verified", false);
            out.addProperty("error", e.getMessage());
            Blackaddons.LOGGER.warn("[lbsave] Mojang probe failed: {}", e.getMessage());
        }
        return out;
    }

    private static JsonArray toJsonArray(List<String> items) {
        JsonArray arr = new JsonArray();
        if (items != null) {
            for (String s : items) {
                arr.add(s);
            }
        }
        return arr;
    }

    private static int appendSample(JsonObject sample) {
        try {
            Files.createDirectories(SAMPLES_FILE.getParent());
            JsonArray all = new JsonArray();
            if (Files.exists(SAMPLES_FILE)) {
                try (FileReader r = new FileReader(SAMPLES_FILE.toFile())) {
                    JsonElement parsed = JsonParser.parseReader(r);
                    if (parsed != null && parsed.isJsonArray()) {
                        all = parsed.getAsJsonArray();
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.warn("Could not read existing solo_clear_samples.json, starting fresh: {}", e.getMessage());
                }
            }
            all.add(sample);
            try (FileWriter w = new FileWriter(SAMPLES_FILE.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(all, w);
            }
            return all.size();
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to save solo clear sample", e);
            return -1;
        }
    }

    public static Path getSamplesFile() {
        return SAMPLES_FILE;
    }
}
