package org.blackum.blackaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class ConfigManager {

    private static final File CONFIG_FILE = new File("blackaddons_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public int overlayX = 5;
        public int overlayY = 5;
        public float overlayScale = 1.0f;
        public boolean showHitboxes = false;
        public boolean showDebugOverlay = false;
        public int accentColor = Theme.ACCENT;

        // Mod Hider (ported from ClientSpoofer)
        public String modHiderSpoofMode = SpoofMode.VANILLA.name();
        public String modHiderCustomClient = "fabric";
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public ArrayList<String> modHiderAllowedMods = new ArrayList<>();
        public ArrayList<String> modHiderAllowedCustomPayloadChannels = new ArrayList<>();
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.overlayX = BaseScreen.overlayX;
        data.overlayY = BaseScreen.overlayY;
        data.overlayScale = BaseScreen.overlayScale;
        data.showHitboxes = BaseScreen.showHitboxes;
        data.showDebugOverlay = BaseScreen.showDebugOverlay;
        data.accentColor = Theme.ACCENT;

        data.modHiderSpoofMode = ModHiderOptions.SPOOF_MODE.name();
        data.modHiderCustomClient = ModHiderOptions.CUSTOM_CLIENT;
        data.modHiderHideMods = ModHiderOptions.HIDE_MODS;
        data.modHiderDisableCustomPayloads = ModHiderOptions.DISABLE_CUSTOM_PAYLOADS;
        data.modHiderAllowedMods = new ArrayList<>(ModHiderOptions.ALLOWED_MODS);
        data.modHiderAllowedCustomPayloadChannels = new ArrayList<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
