package org.blackum.blackaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.cheats.CheatsOptions;
import org.blackum.blackaddons.general.GeneralOptions;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.legit.LegitOptions;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final String APP_ID = "blackaddons";
    private static final String CONFIG_FILE_NAME = "blackaddons_config.json";
    private static final String DEFAULT_BOT_URL = "http://hypixel-skyblock-socket.pegle.com:8080";
    private static final String DEFAULT_CLIENT_NAME = "fabric";
    private static final int DEFAULT_NOTIFICATION_DURATION = 4000;
    private static final int DEFAULT_CACHE_DURATION_MINUTES = 5;

    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve(APP_ID);
    private static final File CONFIG_FILE = CONFIG_DIR.resolve(CONFIG_FILE_NAME).toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        if (!CONFIG_DIR.toFile().exists()) {
            CONFIG_DIR.toFile().mkdirs();
        }
    }

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

    // Bot Integration
    public static String botUrl = DEFAULT_BOT_URL;
    public static DataSource dataSource = DataSource.LOCAL;
    public static boolean rngTrackerEnabled = true;
    public static String developerKey = "";
    public static boolean useCardLayout = true;

    public static Map<String, String> knownAliases = new HashMap<>();
    public static Map<String, CardState> lastLoadedCardStates = new HashMap<>();

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
        public String botUrl = DEFAULT_BOT_URL;
        public String dataSource = DataSource.LOCAL.name();
        public boolean rngTrackerEnabled = true;
        public String developerKey = "";

        // Mod Hider (ported from ClientSpoofer)
        public String modHiderSpoofMode = SpoofMode.VANILLA.name();
        public String modHiderCustomClient = DEFAULT_CLIENT_NAME;
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public ArrayList<String> modHiderAllowedMods = new ArrayList<>();
        public ArrayList<String> modHiderAllowedCustomPayloadChannels = new ArrayList<>();

        // Cheats
        public boolean AutoTNTEnabled = false;
        public int AutoTNTDelay = 5;
        public int UnequipDelay = 8;
        public boolean SwapBack = false;

        // Legit
        public boolean legitFullbrightEnabled = false;

        // Settings
        public int notificationDuration = DEFAULT_NOTIFICATION_DURATION;
        public int cacheDurationMinutes = DEFAULT_CACHE_DURATION_MINUTES;

        public Map<String, String> knownAliases = new HashMap<>();
    }

    public static void save() {
        save(lastLoadedCardStates);
    }

    public static void save(Map<String, CardState> cardStates) {
        ConfigData data = new ConfigData();
        populateConfigData(data, cardStates);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to save config", e);
        }
    }

    private static void populateConfigData(ConfigData data, Map<String, CardState> cardStates) {
        data.overlayX = BaseScreen.overlayX;
        data.overlayY = BaseScreen.overlayY;
        data.overlayScale = BaseScreen.overlayScale;
        data.showHitboxes = BaseScreen.showHitboxes;
        data.showDebugOverlay = BaseScreen.showDebugOverlay;
        data.accentColor = Theme.ACCENT;
        data.useCardLayout = ConfigManager.useCardLayout;
        data.cardStates = cardStates != null ? cardStates : new HashMap<>();

        data.botUrl = ConfigManager.botUrl;
        data.dataSource = ConfigManager.dataSource.name();
        data.rngTrackerEnabled = ConfigManager.rngTrackerEnabled;
        data.developerKey = ConfigManager.developerKey;

        data.modHiderSpoofMode = ModHiderOptions.SPOOF_MODE.name();
        data.modHiderCustomClient = ModHiderOptions.CUSTOM_CLIENT;
        data.modHiderHideMods = ModHiderOptions.HIDE_MODS;
        data.modHiderDisableCustomPayloads = ModHiderOptions.DISABLE_CUSTOM_PAYLOADS;
        data.modHiderAllowedMods = new ArrayList<>(ModHiderOptions.ALLOWED_MODS);
        data.modHiderAllowedCustomPayloadChannels = new ArrayList<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS);

        data.AutoTNTEnabled = CheatsOptions.AutoTNTEnabled;
        data.AutoTNTDelay = CheatsOptions.AutoTNTDelay;
        data.UnequipDelay = CheatsOptions.UnequipDelay;
        data.SwapBack = CheatsOptions.SwapBack;

        data.legitFullbrightEnabled = LegitOptions.FullbrightEnabled;

        data.notificationDuration = GeneralOptions.NOTIFICATION_DURATION;
        data.cacheDurationMinutes = GeneralOptions.CACHE_DURATION_MINUTES;

        data.knownAliases = new HashMap<>(ConfigManager.knownAliases);
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null)
                return;

            BaseScreen.overlayX = data.overlayX;
            BaseScreen.overlayY = data.overlayY;
            BaseScreen.overlayScale = data.overlayScale;
            BaseScreen.showHitboxes = data.showHitboxes;
            BaseScreen.showDebugOverlay = data.showDebugOverlay;

            if (data.accentColor != 0) {
                Theme.ACCENT = data.accentColor;
            }
            ConfigManager.useCardLayout = data.useCardLayout;
            ConfigManager.botUrl = data.botUrl != null ? data.botUrl : DEFAULT_BOT_URL;
            ConfigManager.developerKey = data.developerKey != null ? data.developerKey : "";

            try {
                ConfigManager.dataSource = DataSource.valueOf(
                        data.dataSource == null ? DataSource.LOCAL.name() : data.dataSource);
            } catch (IllegalArgumentException ignored) {
                ConfigManager.dataSource = DataSource.LOCAL;
            }

            try {
                ModHiderOptions.SPOOF_MODE = SpoofMode.valueOf(
                        data.modHiderSpoofMode == null ? SpoofMode.VANILLA.name() : data.modHiderSpoofMode);
            } catch (IllegalArgumentException ignored) {
                ModHiderOptions.SPOOF_MODE = SpoofMode.VANILLA;
            }

            ModHiderOptions.CUSTOM_CLIENT = data.modHiderCustomClient == null ? DEFAULT_CLIENT_NAME
                    : data.modHiderCustomClient;
            ModHiderOptions.HIDE_MODS = data.modHiderHideMods;
            ModHiderOptions.DISABLE_CUSTOM_PAYLOADS = data.modHiderDisableCustomPayloads;

            ModHiderOptions.ALLOWED_MODS = new HashSet<>(
                    data.modHiderAllowedMods == null ? List.of() : data.modHiderAllowedMods);
            ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS = new HashSet<>(
                    data.modHiderAllowedCustomPayloadChannels == null ? List.of()
                            : data.modHiderAllowedCustomPayloadChannels);

            CheatsOptions.AutoTNTEnabled = data.AutoTNTEnabled;
            CheatsOptions.AutoTNTDelay = data.AutoTNTDelay;
            if (data.UnequipDelay != 0) {
                CheatsOptions.UnequipDelay = data.UnequipDelay;
            }

            CheatsOptions.SwapBack = data.SwapBack;
            LegitOptions.FullbrightEnabled = data.legitFullbrightEnabled;

            GeneralOptions.NOTIFICATION_DURATION = data.notificationDuration;
            GeneralOptions.CACHE_DURATION_MINUTES = data.cacheDurationMinutes == 0 ? DEFAULT_CACHE_DURATION_MINUTES
                    : data.cacheDurationMinutes;

            ConfigManager.rngTrackerEnabled = data.rngTrackerEnabled;
            lastLoadedCardStates = data.cardStates != null ? data.cardStates : new HashMap<>();
            ConfigManager.knownAliases = data.knownAliases != null ? data.knownAliases : new HashMap<>();

        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to load config", e);
        }
    }
}
