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
import net.minecraft.client.renderer.texture.OverlayTexture;

//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}

@AutoModule(order = 410)
public class ChinaHatRenderer {
    private static final int SEGMENTS = 64;
    private static final float RADIUS = 0.6f;
    private static final float[] COS_RADIUS_TABLE = new float[SEGMENTS + 1];
    private static final float[] SIN_RADIUS_TABLE = new float[SEGMENTS + 1];
    private static final float[] HUE_OFFSET_TABLE = new float[SEGMENTS];
    private static final Quaternionf ROTATION = new Quaternionf();

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (i * 2.0 * Math.PI / SEGMENTS);
            COS_RADIUS_TABLE[i] = (float) Math.cos(angle) * RADIUS;
            SIN_RADIUS_TABLE[i] = (float) Math.sin(angle) * RADIUS;
        }
        for (int i = 0; i < SEGMENTS; i++) {
            HUE_OFFSET_TABLE[i] = i * 0.09375f;
        }
    }

    public static void register() {
//? if >=26.2 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (!ConfigManager.data.chinaHatEnabled) return;
            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            render(context.poseStack(), context.submitNodeCollector(), partialTicks);
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
            render(context.poseStack(), bufSource, partialTicks);
        });
//?}
    }

//? if >=26.2 {
    /*private static void render(PoseStack poseStack, SubmitNodeCollector bufferSource, float partialTicks) {
*///?} else {
    private static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks) {
//?}
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameRenderer == null) return;

        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
        if (isFirstPerson && !Freecam.getInstance().isActive()) {
            return;
        }

        Vec3 camPos = McCompat.getCamera(mc.gameRenderer).position();

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

        poseStack.pushPose();
        poseStack.translate(headX, headY, headZ);

        float headYaw = Mth.lerp(partialTicks, player.yHeadRotO, player.yHeadRot);
        poseStack.mulPose(ROTATION.rotationY((float) Math.toRadians(-headYaw)));

        float peak = 0.3f;
        float timeFactor6 = ((float) (System.currentTimeMillis() % 4000) * 0.00025f) * 6.0f;

        McCompat.drawGeometry(bufferSource, poseStack, McCompat.getWaypointRenderType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            for (int i = 0; i < SEGMENTS; i++) {
                float cosR = COS_RADIUS_TABLE[i];
                float sinR = SIN_RADIUS_TABLE[i];
                float nextCosR = COS_RADIUS_TABLE[i + 1];
                float nextSineR = SIN_RADIUS_TABLE[i + 1];

                float h = timeFactor6 + HUE_OFFSET_TABLE[i];
                int h_int = (int) h;
                float f = h - h_int;
                float p = 0.2f;
                float q = 1.0f - 0.8f * f;
                float t = 0.2f + 0.8f * f;
                float r, g, b;
                int selector = h_int >= 6 ? h_int - 6 : h_int;
                switch (selector) {
                    case 0:  r = 1.0f; g = t;    b = p;    break;
                    case 1:  r = q;    g = 1.0f; b = p;    break;
                    case 2:  r = p;    g = 1.0f; b = t;    break;
                    case 3:  r = p;    g = q;    b = 1.0f; break;
                    case 4:  r = t;    g = p;    b = 1.0f; break;
                    default: r = 1.0f; g = p;    b = q;    break;
                }
                float a = 0.6f;

                buffer.addVertex(matrix, cosR, 0, sinR).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
                buffer.addVertex(matrix, nextCosR, 0, nextSineR).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);

                buffer.addVertex(matrix, nextCosR, 0, nextSineR).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
                buffer.addVertex(matrix, cosR, 0, sinR).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
                buffer.addVertex(matrix, 0, peak, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
            }
        });

        poseStack.popPose();
    }
}
