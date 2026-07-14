package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.awt.Color;
import net.minecraft.client.renderer.texture.OverlayTexture;

//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}

@AutoModule(order = 410)
public class ChinaHatRenderer {

    public static void register() {
//? if >=26.2 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (!ConfigManager.data.chinaHatEnabled) return;
            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            RenderContext ctx = new RenderContext(context.poseStack(), context.submitNodeCollector(), partialTicks);
            render(ctx);
        });
*///?} else {
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(context -> {
            if (!ConfigManager.data.chinaHatEnabled) return;
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

    private static void render(RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameRenderer == null) return;

        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
        if (isFirstPerson && !Freecam.getInstance().isActive()) {
            return;
        }

        Vec3 camPos = McCompat.getCamera(mc.gameRenderer).position();
        float partialTicks = ctx.getPartialTicks();

        double px = Mth.lerp(partialTicks, player.xo, player.getX()) - camPos.x;
        double py = Mth.lerp(partialTicks, player.yo, player.getY()) - camPos.y;
        double pz = Mth.lerp(partialTicks, player.zo, player.getZ()) - camPos.z;

        double heightOffset = 0.15;
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            heightOffset += 0.15;
        }

        double headX = px;
        double headY = py + player.getBbHeight() + heightOffset;
        double headZ = pz;

        if (player.isShiftKeyDown()) {
            headY -= 0.20;
            float bodyYawRad = (float) Math.toRadians(player.yBodyRot);
            headX -= Math.sin(bodyYawRad) * 0.15;
            headZ += Math.cos(bodyYawRad) * 0.15;
        }

        PoseStack poseStack = ctx.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(headX, headY, headZ);

        float headYaw = Mth.lerp(partialTicks, player.yHeadRotO, player.yHeadRot);
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-headYaw)));

        float radius = 0.6f;
        float peak = 0.3f;
        int segments = 64;

        McCompat.drawGeometry(ctx.getBufferSource(), poseStack, McCompat.getWaypointRenderType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            for (int i = 0; i < segments; i++) {
                float angle = (float) (i * 2.0 * Math.PI / segments);
                float nextAngle = (float) ((i + 1) * 2.0 * Math.PI / segments);

                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float nextCos = (float) Math.cos(nextAngle);
                float nextSine = (float) Math.sin(nextAngle);

                float hue = (float) ((System.currentTimeMillis() % 4000) / 4000.0 + (double) i / segments);
                int rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f);
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;
                float a = 0.6f;

                buffer.addVertex(matrix, cos * radius, 0, sin * radius).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
                buffer.addVertex(matrix, nextCos * radius, 0, nextSine * radius).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);

                buffer.addVertex(matrix, nextCos * radius, 0, nextSine * radius).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
                buffer.addVertex(matrix, cos * radius, 0, sin * radius).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
            }
        });

        poseStack.popPose();
    }
}
