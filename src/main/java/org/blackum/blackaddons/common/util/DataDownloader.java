package org.blackum.blackaddons.common.util;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;

public class DataDownloader {
    private static final Gson GSON = new Gson();

    public static <T> T loadJson(String fileName, Type type) {
        try {
            InputStream stream = DataDownloader.class.getResourceAsStream("/assets/blackaddons/puzzles/" + fileName);
            if (stream == null) {
                Minecraft.getInstance().gui.getChat().addMessage(
                        net.minecraft.network.chat.Component.literal("§d[BlackAddons Debug] §cResource Missing: §f/assets/blackaddons/puzzles/" + fileName)
                );
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, type);
            }
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(
                    net.minecraft.network.chat.Component.literal("§d[BlackAddons Debug] §cError Loading JSON: §f" + fileName + " (" + e.getMessage() + ")")
            );
            e.printStackTrace();
            return null;
        }
    }
}
