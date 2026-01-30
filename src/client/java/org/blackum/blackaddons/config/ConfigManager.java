package org.blackum.blackaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;
import org.blackum.blackaddons.cheats.CheatsOptions;
import org.blackum.blackaddons.general.GeneralOptions;
import org.blackum.blackaddons.payload.PayloadManager;
import org.blackum.blackaddons.payload.PayloadOverride;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ConfigManager {

    private static final java.nio.file.Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("blackaddons");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("blackaddons_config.json").toFile();
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

        public CardState() {
        }

        public CardState(int x, int y, int width, int height, boolean collapsed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.collapsed = collapsed;
        }
    }

    public static boolean useCardLayout = true;

    public static class ConfigData {
        public int overlayX = 5;
        public int overlayY = 5;
        public float overlayScale = 1.0f;
        public boolean showHitboxes = false;
        public boolean showDebugOverlay = false;
        public int accentColor = Theme.ACCENT;
        public boolean useCardLayout = true;
        public Map<String, CardState> cardStates = new HashMap<>();

        // Mod Hider (ported from ClientSpoofer)
        public String modHiderSpoofMode = SpoofMode.VANILLA.name();
        public String modHiderCustomClient = "fabric";
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public ArrayList<String> modHiderAllowedMods = new ArrayList<>();
        public ArrayList<String> modHiderAllowedCustomPayloadChannels = new ArrayList<>();

        // cheats
        // AutoTNT
        public boolean AutoTNTEnabled = false;
        public int AutoTNTDelay = 5;

        // Settings
        public int notificationDuration = 4000;

        // Payload Manager
        public ArrayList<PayloadOverride> payloadOverrides = new ArrayList<>();
        public ArrayList<org.blackum.blackaddons.payload.RecordedPayload> recordedPayloads = new ArrayList<>();
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.overlayX = BaseScreen.overlayX;
        data.overlayY = BaseScreen.overlayY;
        data.overlayScale = BaseScreen.overlayScale;
        data.showHitboxes = BaseScreen.showHitboxes;
        data.showDebugOverlay = BaseScreen.showDebugOverlay;
        data.accentColor = Theme.ACCENT;
        data.useCardLayout = ConfigManager.useCardLayout;

        data.modHiderSpoofMode = ModHiderOptions.SPOOF_MODE.name();
        data.modHiderCustomClient = ModHiderOptions.CUSTOM_CLIENT;
        data.modHiderHideMods = ModHiderOptions.HIDE_MODS;
        data.modHiderDisableCustomPayloads = ModHiderOptions.DISABLE_CUSTOM_PAYLOADS;
        data.modHiderAllowedMods = new ArrayList<>(ModHiderOptions.ALLOWED_MODS);
        data.modHiderAllowedCustomPayloadChannels = new ArrayList<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS);

        data.AutoTNTEnabled = CheatsOptions.AutoTNTEnabled;
        data.AutoTNTDelay = CheatsOptions.AutoTNTDelay;

        data.notificationDuration = GeneralOptions.NOTIFICATION_DURATION;

        data.payloadOverrides = new ArrayList<>(org.blackum.blackaddons.payload.PayloadManager.overrides);
        data.recordedPayloads = new ArrayList<>(org.blackum.blackaddons.payload.PayloadManager.recordedPayloads);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to save config", e);
        }
    }

    public static void save(Map<String, CardState> cardStates) {
        ConfigData data = new ConfigData();
        data.overlayX = BaseScreen.overlayX;
        data.overlayY = BaseScreen.overlayY;
        data.overlayScale = BaseScreen.overlayScale;
        data.showHitboxes = BaseScreen.showHitboxes;
        data.showDebugOverlay = BaseScreen.showDebugOverlay;
        data.accentColor = Theme.ACCENT;
        data.useCardLayout = ConfigManager.useCardLayout;
        data.cardStates = cardStates != null ? cardStates : new HashMap<>();

        data.modHiderSpoofMode = ModHiderOptions.SPOOF_MODE.name();
        data.modHiderCustomClient = ModHiderOptions.CUSTOM_CLIENT;
        data.modHiderHideMods = ModHiderOptions.HIDE_MODS;
        data.modHiderDisableCustomPayloads = ModHiderOptions.DISABLE_CUSTOM_PAYLOADS;
        data.modHiderAllowedMods = new ArrayList<>(ModHiderOptions.ALLOWED_MODS);
        data.modHiderAllowedCustomPayloadChannels = new ArrayList<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS);

        data.AutoTNTEnabled = CheatsOptions.AutoTNTEnabled;
        data.AutoTNTDelay = CheatsOptions.AutoTNTDelay;

        data.notificationDuration = GeneralOptions.NOTIFICATION_DURATION;

        data.payloadOverrides = new ArrayList<>(org.blackum.blackaddons.payload.PayloadManager.overrides);
        data.recordedPayloads = new ArrayList<>(org.blackum.blackaddons.payload.PayloadManager.recordedPayloads);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to save config with card states", e);
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                BaseScreen.overlayX = data.overlayX;
                BaseScreen.overlayY = data.overlayY;
                BaseScreen.overlayScale = data.overlayScale;
                BaseScreen.showHitboxes = data.showHitboxes;
                BaseScreen.showDebugOverlay = data.showDebugOverlay;

                if (data.accentColor != 0) {
                    Theme.ACCENT = data.accentColor;
                }
                ConfigManager.useCardLayout = data.useCardLayout;

                try {
                    ModHiderOptions.SPOOF_MODE = SpoofMode.valueOf(
                            data.modHiderSpoofMode == null ? SpoofMode.VANILLA.name() : data.modHiderSpoofMode);
                } catch (IllegalArgumentException ignored) {
                    ModHiderOptions.SPOOF_MODE = SpoofMode.VANILLA;
                }
                ModHiderOptions.CUSTOM_CLIENT = data.modHiderCustomClient == null ? "fabric"
                        : data.modHiderCustomClient;
                ModHiderOptions.HIDE_MODS = data.modHiderHideMods;
                ModHiderOptions.DISABLE_CUSTOM_PAYLOADS = data.modHiderDisableCustomPayloads;

                ModHiderOptions.ALLOWED_MODS = new HashSet<>(
                        data.modHiderAllowedMods == null ? java.util.List.of() : data.modHiderAllowedMods);
                ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS = new HashSet<>(
                        data.modHiderAllowedCustomPayloadChannels == null ? java.util.List.of()
                                : data.modHiderAllowedCustomPayloadChannels);

                CheatsOptions.AutoTNTEnabled = data.AutoTNTEnabled;
                CheatsOptions.AutoTNTDelay = data.AutoTNTDelay;

                CheatsOptions.AutoTNTEnabled = data.AutoTNTEnabled;
                CheatsOptions.AutoTNTDelay = data.AutoTNTDelay;

                GeneralOptions.NOTIFICATION_DURATION = data.notificationDuration;

                PayloadManager.overrides.clear();
                if (data.payloadOverrides != null) {
                    PayloadManager.overrides.addAll(data.payloadOverrides);
                }

                PayloadManager.overrides.clear();
                if (data.payloadOverrides != null) {
                    PayloadManager.overrides.addAll(data.payloadOverrides);
                }

                PayloadManager.recordedPayloads.clear();
                if (data.recordedPayloads != null) {
                    PayloadManager.recordedPayloads.addAll(data.recordedPayloads);
                }

                lastLoadedCardStates = data.cardStates != null ? data.cardStates : new HashMap<>();
            }
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to load config", e);
        }
    }

    public static Map<String, CardState> lastLoadedCardStates = new HashMap<>();
}
