package org.blackum.blackaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.blackum.blackaddons.gui.screen.BaseScreen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final File CONFIG_FILE = new File("blackaddons_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public int overlayX = 5;
        public int overlayY = 5;
        public float overlayScale = 1.0f;
        public boolean showHitboxes = false;
        public boolean showDebugOverlay = false;
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.overlayX = BaseScreen.overlayX;
        data.overlayY = BaseScreen.overlayY;
        data.overlayScale = BaseScreen.overlayScale;
        data.showHitboxes = BaseScreen.showHitboxes;
        data.showDebugOverlay = BaseScreen.showDebugOverlay;

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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
