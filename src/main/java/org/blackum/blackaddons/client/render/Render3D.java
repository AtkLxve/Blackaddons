package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.joml.Matrix4f;

public class Render3D {
    public static void renderTracer(RenderContext ctx, Vec3 pos, int color, float thickness) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 camPos = McCompat.getCamera(mc.gameRenderer).position();
        Vec3 look = mc.player.getViewVector(ctx.getPartialTicks());
        Vec3 start = camPos.add(look.scale(1.5));
        renderLine(ctx, start, pos, color, thickness);
    }

    public static void renderLine(RenderContext ctx, Vec3 start, Vec3 end, int color, float thickness) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = McCompat.getCamera(mc.gameRenderer).position();
        float x1 = (float) (start.x - camPos.x);
        float y1 = (float) (start.y - camPos.y);
        float z1 = (float) (start.z - camPos.z);
        float x2 = (float) (end.x - camPos.x);
        float y2 = (float) (end.y - camPos.y);
        float z2 = (float) (end.z - camPos.z);

        Vec3 lineDir = end.subtract(start).normalize();
        Vec3 camDir = start.subtract(camPos).normalize();
        Vec3 cross = lineDir.cross(camDir).normalize().scale(thickness * 0.05);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0) a = 1f;

        float cx = (float) cross.x;
        float cy = (float) cross.y;
        float cz = (float) cross.z;

        float finalA = a;
        McCompat.drawGeometry(ctx.getBufferSource(), ctx.getPoseStack(), McCompat.getWaypointRenderType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            buffer.addVertex(matrix, x1 - cx, y1 - cy, z1 - cz).setColor(r, g, b, finalA).setUv(0, 0).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x2 - cx, y2 - cy, z2 - cz).setColor(r, g, b, finalA).setUv(1, 1).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x2 + cx, y2 + cy, z2 + cz).setColor(r, g, b, finalA).setUv(1, 0).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x1 + cx, y1 + cy, z1 + cz).setColor(r, g, b, finalA).setUv(0, 1).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        });
    }

    public static void renderString(RenderContext ctx, String text, Vec3 pos, float scale, boolean phase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = McCompat.getCamera(mc.gameRenderer).position();
        double x = pos.x - camPos.x;
        double y = pos.y - camPos.y;
        double z = pos.z - camPos.z;

        PoseStack poseStack = ctx.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(McCompat.getCamera(mc.gameRenderer).rotation());
        poseStack.scale(-0.025f * scale, -0.025f * scale, 1.0f);

        Font font = mc.font;
        float width = -font.width(text) / 2f;

        McCompat.drawString(ctx.getBufferSource(), poseStack, text, width, 0, 0xFFFFFFFF, true, font, phase);

        //? if <26.2 {
        if (ctx.getBufferSource() != null) {
            ctx.getBufferSource().endBatch();
        }
        //?}

        poseStack.popPose();
    }

    public static void renderBox(RenderContext ctx, BlockPos pos, int color, float thickness, boolean fill) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        Vec3 camPos = McCompat.getCamera(mc.gameRenderer).position();
        double x = pos.getX() - camPos.x;
        double y = pos.getY() - camPos.y;
        double z = pos.getZ() - camPos.z;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0) a = 1f;

        float finalA = a;
        PoseStack poseStack = ctx.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        McCompat.drawGeometry(ctx.getBufferSource(), poseStack, McCompat.getWaypointRenderType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float s = 1.0f;
            drawLine(buffer, matrix, 0, 0, 0, s, 0, 0, r, g, b, finalA);
            drawLine(buffer, matrix, s, 0, 0, s, 0, s, r, g, b, finalA);
            drawLine(buffer, matrix, s, 0, s, 0, 0, s, r, g, b, finalA);
            drawLine(buffer, matrix, 0, 0, s, 0, 0, 0, r, g, b, finalA);
            drawLine(buffer, matrix, 0, s, 0, s, s, 0, r, g, b, finalA);
            drawLine(buffer, matrix, s, s, 0, s, s, s, r, g, b, finalA);
            drawLine(buffer, matrix, s, s, s, 0, s, s, r, g, b, finalA);
            drawLine(buffer, matrix, 0, s, s, 0, s, 0, r, g, b, finalA);
            drawLine(buffer, matrix, 0, 0, 0, 0, s, 0, r, g, b, finalA);
            drawLine(buffer, matrix, s, 0, 0, s, s, 0, r, g, b, finalA);
            drawLine(buffer, matrix, s, 0, s, s, s, s, r, g, b, finalA);
            drawLine(buffer, matrix, 0, 0, s, 0, s, s, r, g, b, finalA);
        });

        poseStack.popPose();
    }

    private static void drawLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x2, y2 + 0.02f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x1, y1 + 0.02f, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
    }
}
