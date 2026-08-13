package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.client.render.DebugBoxRenderer;
import org.blackum.blackaddons.client.render.Render3D;
import org.blackum.blackaddons.client.render.RenderContext;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AutoModule(order = 104)
public class CritterESP {

    public static void register() {
//? if >=26.2 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (!ConfigManager.data.CritterEspEnabled) return;
            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            RenderContext ctx = new RenderContext(context.poseStack(), context.submitNodeCollector(), partialTicks);
            render(ctx);
        });
*///?} else {
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(context -> {
            if (!ConfigManager.data.CritterEspEnabled) return;
            MultiBufferSource.BufferSource bufSource;
            if (context.bufferSource() instanceof MultiBufferSource.BufferSource bs) {
                bufSource = bs;
            } else {
                bufSource = Minecraft.getInstance().renderBuffers().bufferSource();
            }
            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            RenderContext ctx = new RenderContext(context.poseStack(), bufSource, partialTicks);
            render(ctx);
        });
//?}
    }

    public static boolean isCritter(String clean) {
        for (String critter : Constants.CRITTER_NAMES) {
            if (clean.contains(critter)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isShiny(String clean) {
        for (String keyword : Constants.SHINY_CRITTER_KEYWORDS) {
            if (clean.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static void render(RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        double radius = ConfigManager.data.CritterEspRadius;
        AABB searchBox = mc.player.getBoundingBox().inflate(radius);
        List<DebugBoxRenderer.BoxSpec> boxes = new ArrayList<>();

        for (Entity e : mc.level.getEntities(mc.player, searchBox, ent -> ent != null && !ent.isSpectator())) {
            String rawName = e.getName().getString();
            String clean = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim().toLowerCase(Locale.ROOT);
            if (!isCritter(clean)) continue;

            double x = Mth.lerp(ctx.getPartialTicks(), e.xo, e.getX());
            double y = Mth.lerp(ctx.getPartialTicks(), e.yo, e.getY());
            double z = Mth.lerp(ctx.getPartialTicks(), e.zo, e.getZ());
            double widthHalf = e.getBbWidth() / 2.0;
            double height = e.getBbHeight();

            if (ConfigManager.data.CritterEspBox) {
                float alpha = ((ConfigManager.data.CritterEspColor >> 24) & 0xFF) / 255f;
                if (alpha == 0.0f) {
                    alpha = 0.4f;
                }
                boxes.add(new DebugBoxRenderer.BoxSpec(
                        null,
                        x - widthHalf,
                        y,
                        z - widthHalf,
                        x + widthHalf,
                        y + height,
                        z + widthHalf,
                        ConfigManager.data.CritterEspColor,
                        alpha
                ));
            }

            if (ConfigManager.data.CritterEspTracers) {
                Render3D.renderTracer(ctx, new Vec3(x, y + height / 2.0, z), ConfigManager.data.CritterEspColor, Constants.DEFAULT_CRITTER_TRACER_THICKNESS);
            }

            if (ConfigManager.data.CritterEspNametag) {
                Render3D.renderString(ctx, rawName, new Vec3(x, y + height + 0.3, z), 1.0f, true);
            }
        }

        if (!boxes.isEmpty()) {
//? if >=26.2 {
            /*DebugBoxRenderer.render(
                    ctx.getPoseStack(),
                    ctx.getBufferSource(),
                    mc.gameRenderer.getMainCamera().position(),
                    boxes
            );
*///?} else {
            DebugBoxRenderer.render(
                    ctx.getMatrix(),
                    ctx.getBufferSource(),
                    mc.gameRenderer.getMainCamera().position(),
                    boxes
            );
//?}
        }
    }

    public static void scanAndReport() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double radius = 20.0;
        AABB searchBox = mc.player.getBoundingBox().inflate(radius);
        List<String> found = new ArrayList<>();

        for (Entity e : mc.level.getEntities(mc.player, searchBox, ent -> ent != null && !ent.isSpectator())) {
            String rawName = e.getName().getString();
            String clean = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim().toLowerCase(Locale.ROOT);
            if (isCritter(clean)) {
                if (isShiny(clean)) {
                    String cleanName = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
                    found.add("§6§l" + cleanName + "§r");
                } else {
                    found.add(rawName);
                }
            }
        }

        if (!found.isEmpty()) {
            String msg = "Critters nearby: " + String.join(", ", found);
            ChatUtils.send_debug(msg);
        } else {
            ChatUtils.send_debug("Critter ESP: none found");
        }
    }
}
