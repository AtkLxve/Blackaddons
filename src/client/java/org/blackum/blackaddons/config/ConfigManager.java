package org.blackum.blackaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.cheats.AutoTNT;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("blackaddons");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class CardState {
        public int x;
        public int y;
        public int width;
        public int height;
        public boolean collapsed;
        public int initialWidth;
        public int expandedHeight;

        public CardState() {
        }

        public CardState(int x, int y, int width, int height, boolean collapsed, int initialWidth, int expandedHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.collapsed = collapsed;
            this.initialWidth = initialWidth;
            this.expandedHeight = expandedHeight;
        }
    }

    public static enum DataSource {
        BOT, LOCAL
    }

    public static class ConfigData {
        public int overlayX = 5;
        public int overlayY = 5;
        public float overlayScale = 1.0f;
        public boolean showHitboxes = false;
        public boolean showDebugOverlay = false;
        public int accentColor = Theme.ACCENT;
        public boolean useCardLayout = true;
        public Map<String, CardState> cardStates = new HashMap<>();

        // Bot
        public String botUrl = "http://hypixel-skyblock-socket.pegle.com:8080";
        public DataSource dataSource = DataSource.LOCAL;
        public boolean rngTrackerEnabled = true;
        public String developerKey = "";

        // Mod Hider (ported from ClientSpoofer)
        public String modHiderSpoofMode = SpoofMode.VANILLA.name();
        public String modHiderCustomClient = "fabric";
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public ArrayList<String> modHiderAllowedMods = new ArrayList<>();
        public ArrayList<String> modHiderAllowedCustomPayloadChannels = new ArrayList<>();

        // Cheats
        public boolean AutoTNTEnabled = false;
        public int AutoTNTDelay = 5;
        public int UnequipDelay = 8;
        public boolean SwapBack = false;

        public AutoTNT.FeatureConfig autoTntConfig = new AutoTNT.FeatureConfig();
        public ModHiderOptions modHiderConfig = new ModHiderOptions();

        // Legit
        public boolean legitFullbrightEnabled = false;

        // Settings
        public int notificationDuration = 4000;
        public int cacheDurationMinutes = 5;

        // Command aliases
        public Map<String, String> knownAliases = new HashMap<>();

        // UI
        public Map<String, CardState> lastLoadedCardStates = new HashMap<>();
    }

    public static ConfigData data = new ConfigData();

    public static void save() {
        try {
            if (!CONFIG_DIR.toFile().exists()) CONFIG_DIR.toFile().mkdirs();

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);
            if (loadedData != null) {
                data = loadedData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
