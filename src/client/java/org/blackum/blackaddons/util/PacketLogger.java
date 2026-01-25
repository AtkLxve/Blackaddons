package org.blackum.blackaddons.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.blackum.blackaddons.Blackaddons;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PacketLogger {
    private static final java.nio.file.Path LOG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
            .resolve("blackaddons");
    private static final File LOG_FILE = LOG_DIR.resolve("blocked_packets.log").toFile();

    static {
        if (!LOG_DIR.toFile().exists()) {
            LOG_DIR.toFile().mkdirs();
        }
    }
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logBlockedPacket(String source, CustomPacketPayload payload) {
        logPacket(source, "Blocked Packet", payload);
    }

    public static void logPacket(String source, String action, CustomPacketPayload payload) {
        new Thread(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                String timestamp = LocalDateTime.now().format(DATE_FORMAT);
                String id = payload.type().id().toString();
                String data = payload.toString();

                StringBuilder logEntry = new StringBuilder();
                logEntry.append("[").append(timestamp).append("] ");
                logEntry.append("[").append(source).append("] ");
                logEntry.append(action).append(": ").append(id).append("\n");
                logEntry.append("    Payload Content: ").append(data).append("\n");
                logEntry.append("--------------------------------------------------\n");

                writer.write(logEntry.toString());
            } catch (IOException e) {
                Blackaddons.LOGGER.error("Failed to log packet", e);
            }
        }).start();
    }
}
