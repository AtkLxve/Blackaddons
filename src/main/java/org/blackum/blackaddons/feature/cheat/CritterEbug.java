package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AutoModule(order = 103)
public class CritterEbug {
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CritterEbug::onTick);
    }

    private static void onTick(Minecraft mc) {
        if (!ConfigManager.data.CritterEbugEnabled) return;
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter % Math.max(1, ConfigManager.data.CritterEbugIntervalTicks) != 0) return;

        int radius = Math.max(1, ConfigManager.data.CritterEbugRadius);
        AABB searchBox = mc.player.getBoundingBox().inflate(radius);
        List<String> found = new ArrayList<>();

        for (Entity e : mc.level.getEntities(mc.player, searchBox, ent -> ent != null && !ent.isSpectator())) {
            String rawName = e.getName().getString();
            String clean = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim().toLowerCase(Locale.ROOT);
            if (CritterESP.isCritter(clean)) {
                if (CritterESP.isShiny(clean)) {
                    String cleanName = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
                    found.add("§6§l" + cleanName + "§r");
                } else {
                    found.add(rawName);
                }
            }
        }

        if (!found.isEmpty()) {
            String msg = "Critters nearby: " + String.join(", ", found);
            Minecraft.getInstance().execute(() -> ChatUtils.send_debug(msg));
        } else if (ConfigManager.data.CritterEbugDebug) {
            Minecraft.getInstance().execute(() -> ChatUtils.send_debug("CritterEbug: none found"));
        }
    }

    public static void reportNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;

        int radius = Math.max(1, ConfigManager.data.CritterEbugRadius);
        AABB searchBox = mc.player.getBoundingBox().inflate(radius);
        List<String> found = new ArrayList<>();

        for (Entity e : mc.level.getEntities(mc.player, searchBox, ent -> ent != null && !ent.isSpectator())) {
            String rawName = e.getName().getString();
            String clean = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim().toLowerCase(Locale.ROOT);
            if (CritterESP.isCritter(clean)) {
                if (CritterESP.isShiny(clean)) {
                    String cleanName = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
                    found.add("§6§l" + cleanName + "§r");
                } else {
                    found.add(rawName);
                }
            }
        }

        if (!found.isEmpty()) {
            String msg = "Critters nearby: " + String.join(", ", found);
            Minecraft.getInstance().execute(() -> ChatUtils.send_debug(msg));
        } else {
            Minecraft.getInstance().execute(() -> ChatUtils.send_debug("CritterEbug: none found"));
        }
    }
}
