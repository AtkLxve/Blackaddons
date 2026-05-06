package org.blackum.blackaddons.service;

import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.util.mc.MinecraftInstance;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

public class MojangAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateServerId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return HexFormat.of().formatHex(bytes);
        }
    }

    public static CompletableFuture<Boolean> joinServer(String serverId) {
        if (serverId == null || serverId.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        Minecraft mc = MinecraftInstance.mc;
        if (mc == null || mc.getUser() == null || mc.services() == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                mc.services().sessionService().joinServer(
                        mc.getUser().getProfileId(),
                        mc.getUser().getAccessToken(),
                        serverId
                );
                return true;
            } catch (AuthenticationException e) {
                Blackaddons.LOGGER.warn("Mojang joinServer failed: {}", e.getMessage());
                return false;
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Mojang joinServer unexpected error", e);
                return false;
            }
        });
    }
}
