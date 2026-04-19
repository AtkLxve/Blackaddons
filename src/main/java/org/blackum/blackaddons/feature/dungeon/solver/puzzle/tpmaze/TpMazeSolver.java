package org.blackum.blackaddons.feature.dungeon.solver.puzzle.tpmaze;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.client.render.RenderContext;
import org.blackum.blackaddons.client.render.Render3D;

public class TpMazeSolver {

    private static final int SCAN_Y = 69;
    private static final double TELEPORT_Y = 69.5;
    private static final float PAD_BLOCK_HEIGHT = 0.8125f;
    private static final double PAD_INFLATE = 1.0;
    private static final int CELL_SIZE = 8;
    private static final int SCAN_RADIUS = 32;
    private static final int MIN_PADS = 20;

    private static final CopyOnWriteArrayList<TpPad> pads = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<BlockPos> currentCandidates = new CopyOnWriteArrayList<>();
    private static int minX = Integer.MAX_VALUE;
    private static int minZ = Integer.MAX_VALUE;
    private static BlockPos bestPad = null;
    private static boolean active = false;
    private static double lastPktX = Double.NaN;
    private static double lastPktZ = Double.NaN;
    private static long lastPktTime = 0;

    private TpMazeSolver() {}

    public static boolean isActive() {
        return active;
    }

    public static BlockPos getMazeCenter() {
        if (pads.isEmpty()) return null;
        int sx = 0, sz = 0;
        for (TpPad p : pads) { sx += p.pos.getX(); sz += p.pos.getZ(); }
        return new BlockPos(sx / pads.size(), SCAN_Y, sz / pads.size());
    }

    public static void scan(BlockPos center) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<BlockPos> found = new ArrayList<>();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                BlockPos pos = new BlockPos(center.getX() + dx, SCAN_Y, center.getZ() + dz);
                if (mc.level.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) found.add(pos);
            }
        }

        if (found.size() < MIN_PADS) {
            Blackaddons.LOGGER.info("[TpMaze] scan too few pads=" + found.size() + ", retrying");
            return;
        }

        pads.clear();
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        for (BlockPos p : found) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getZ() < minZ) minZ = p.getZ();
        }
        for (BlockPos p : found) pads.add(new TpPad(p));

        active = true;
        currentCandidates.clear();

        bestPad = null;

        Blackaddons.LOGGER.info("[TpMaze] scan ok pads=" + pads.size() + " minX=" + minX + " minZ=" + minZ);
    }

    public static void reset() {
        active = false;
        pads.clear();
        currentCandidates.clear();
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        bestPad = null;
        lastPktX = Double.NaN;
        lastPktZ = Double.NaN;
        lastPktTime = 0;
    }

    public static void onServerTeleportPacket(ClientboundPlayerPositionPacket packet) {
        if (!active || !ConfigManager.data.teleportMazeSolverEnabled) return;

        double nx = packet.change().position().x;
        double ny = packet.change().position().y;
        double nz = packet.change().position().z;
        float yaw = packet.change().yRot();

        if (nx % 0.5 != 0.0 || ny != TELEPORT_Y || nz % 0.5 != 0.0) return;

        long now = System.currentTimeMillis();
        if (nx == lastPktX && nz == lastPktZ && now - lastPktTime < 500) return;
        lastPktX = nx; lastPktZ = nz; lastPktTime = now;

        Blackaddons.LOGGER.info("[TpMaze] pkt nx=" + nx + " nz=" + nz);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AABB destArea = new AABB(nx - PAD_INFLATE, ny - 1, nz - PAD_INFLATE, nx + PAD_INFLATE, ny + 1, nz + PAD_INFLATE);
        AABB srcArea = mc.player.getBoundingBox().inflate(PAD_INFLATE, 0, PAD_INFLATE);

        TpPad landedPad = null;
        for (TpPad pad : pads) {
            AABB box = new AABB(pad.pos);
            if (destArea.intersects(box)) { pad.visited = true; landedPad = pad; }
            else if (srcArea.intersects(box)) pad.visited = true;
        }

        if (landedPad == null) {
            Blackaddons.LOGGER.info("[TpMaze] landedPad null");
            return;
        }

        int lcx = cellX(landedPad), lcz = cellZ(landedPad);
        int cellPadCount = countCell(lcx, lcz);

        if (cellPadCount <= 2) {
            bestPad = null;
            Blackaddons.LOGGER.info("[TpMaze] final cell, done");
            return;
        }

        List<TpPad> candidates = new ArrayList<>();
        for (TpPad p : pads) {
            if (!p.visited && cellX(p) == lcx && cellZ(p) == lcz) candidates.add(p);
        }

        currentCandidates.clear();
        for (TpPad p : candidates) currentCandidates.add(p.pos);

        Blackaddons.LOGGER.info("[TpMaze] landed=" + landedPad.pos.toShortString() + " cell=(" + lcx + "," + lcz + ") candidates=" + candidates.size());

        if (candidates.isEmpty()) { bestPad = null; return; }

        boolean diag = ConfigManager.data.teleportMazePrioritizeDiagonal;
        List<TpPad> filtered = new ArrayList<>();
        for (TpPad p : candidates) {
            boolean isDiag = p.pos.getX() != landedPad.pos.getX() && p.pos.getZ() != landedPad.pos.getZ();
            if (isDiag == diag) filtered.add(p);
        }
        if (filtered.isEmpty()) filtered = candidates;

        double minAngle = Double.MAX_VALUE;
        TpPad best = null;
        for (TpPad p : filtered) {
            double dx = p.pos.getX() + 0.5 - nx;
            double dz = p.pos.getZ() + 0.5 - nz;
            double angle = angleDiff(dx, dz, yaw);
            if (angle < minAngle) { minAngle = angle; best = p; }
        }

        bestPad = best != null ? best.pos : null;
        Blackaddons.LOGGER.info("[TpMaze] bestPad=" + (bestPad != null ? bestPad.toShortString() : "null"));
    }

    public static void onServerTeleportPost() {
        if (!active || !ConfigManager.data.teleportMazeAutoRotate || bestPad == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        org.blackum.blackaddons.feature.rotation.RotationManager.getInstance()
                .rotateToBlock(bestPad.getX(), SCAN_Y, bestPad.getZ(), ConfigManager.data.teleportMazeAutoRotateSpeed);
    }

    public static void onRenderWorld(RenderContext context) {
        if (!active || !ConfigManager.data.teleportMazeSolverEnabled) return;

        BlockPos activeBest = bestPad;

        for (TpPad pad : pads) {
            if (countCell(cellX(pad), cellZ(pad)) <= 2) continue;
            int color = pad.visited
                    ? ConfigManager.data.teleportMazeVisitedColor
                    : ConfigManager.data.teleportMazeMultipleColor;
            renderPadOutline(context, pad.pos, color);
        }

        if (activeBest != null) {
            renderFilledBlock(context, activeBest, ConfigManager.data.teleportMazeCorrectColor);
            renderPadOutline(context, activeBest, ConfigManager.data.teleportMazeCorrectColor);

            Vec3 target = new Vec3(activeBest.getX() + 0.5, SCAN_Y + PAD_BLOCK_HEIGHT, activeBest.getZ() + 0.5);
            Render3D.renderTracer(context, target, ConfigManager.data.teleportMazeTracerColor, ConfigManager.data.teleportMazeTracerWidth);
        }
    }

    private static void renderFilledBlock(RenderContext ctx, BlockPos pos, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        float ox = (float) (pos.getX() - camPos.x);
        float oy = (float) (pos.getY() - camPos.y);
        float oz = (float) (pos.getZ() - camPos.z);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0) a = 1f;

        com.mojang.blaze3d.vertex.VertexConsumer buf = org.blackum.blackaddons.client.render.BlackaddonsRenderTypes.getWaypointBuffer(ctx.getBufferSource());
        ctx.getPoseStack().pushPose();
        ctx.getPoseStack().translate(ox, oy, oz);
        org.joml.Matrix4f m = ctx.getPoseStack().last().pose();
        
        float e = 0.005f;
        float x1 = -e, y1 = -e, z1 = -e;
        float x2 = 1.0f + e, y2 = PAD_BLOCK_HEIGHT + e, z2 = 1.0f + e;
        
        addFace(buf, m, x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2, r,g,b,a); // bottom
        addFace(buf, m, x1,y2,z1, x1,y2,z2, x2,y2,z2, x2,y2,z1, r,g,b,a); // top
        addFace(buf, m, x1,y1,z1, x1,y2,z1, x2,y2,z1, x2,y1,z1, r,g,b,a); // north
        addFace(buf, m, x1,y1,z2, x2,y1,z2, x2,y2,z2, x1,y2,z2, r,g,b,a); // south
        addFace(buf, m, x1,y1,z1, x1,y1,z2, x1,y2,z2, x1,y2,z1, r,g,b,a); // west
        addFace(buf, m, x2,y1,z1, x2,y2,z1, x2,y2,z2, x2,y1,z2, r,g,b,a); // east
        
        ctx.getPoseStack().popPose();
    }

    private static void addFace(com.mojang.blaze3d.vertex.VertexConsumer buf, org.joml.Matrix4f m,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4,
            float r, float g, float b, float a) {
        buf.addVertex(m, x1, y1, z1).setColor(r,g,b,a).setUv(0,0).setOverlay(15).setLight(15728880).setNormal(0,1,0);
        buf.addVertex(m, x2, y2, z2).setColor(r,g,b,a).setUv(1,0).setOverlay(15).setLight(15728880).setNormal(0,1,0);
        buf.addVertex(m, x3, y3, z3).setColor(r,g,b,a).setUv(1,1).setOverlay(15).setLight(15728880).setNormal(0,1,0);
        buf.addVertex(m, x4, y4, z4).setColor(r,g,b,a).setUv(0,1).setOverlay(15).setLight(15728880).setNormal(0,1,0);
    }

    private static void renderPadOutline(RenderContext ctx, BlockPos pos, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        double ox = pos.getX() - camPos.x;
        double oy = pos.getY() - camPos.y;
        double oz = pos.getZ() - camPos.z;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0) a = 1f;

        com.mojang.blaze3d.vertex.VertexConsumer buf = org.blackum.blackaddons.client.render.BlackaddonsRenderTypes.getWaypointBuffer(ctx.getBufferSource());
        ctx.getPoseStack().pushPose();
        ctx.getPoseStack().translate(ox, oy, oz);
        org.joml.Matrix4f m = ctx.getPoseStack().last().pose();
        float s = 1.0f, h = PAD_BLOCK_HEIGHT;
        drawLine(buf, m, 0, 0, 0, s, 0, 0, r, g, b, a);
        drawLine(buf, m, s, 0, 0, s, 0, s, r, g, b, a);
        drawLine(buf, m, s, 0, s, 0, 0, s, r, g, b, a);
        drawLine(buf, m, 0, 0, s, 0, 0, 0, r, g, b, a);
        drawLine(buf, m, 0, h, 0, s, h, 0, r, g, b, a);
        drawLine(buf, m, s, h, 0, s, h, s, r, g, b, a);
        drawLine(buf, m, s, h, s, 0, h, s, r, g, b, a);
        drawLine(buf, m, 0, h, s, 0, h, 0, r, g, b, a);
        drawLine(buf, m, 0, 0, 0, 0, h, 0, r, g, b, a);
        drawLine(buf, m, s, 0, 0, s, h, 0, r, g, b, a);
        drawLine(buf, m, s, 0, s, s, h, s, r, g, b, a);
        drawLine(buf, m, 0, 0, s, 0, h, s, r, g, b, a);
        ctx.getPoseStack().popPose();
    }

    private static void drawLine(com.mojang.blaze3d.vertex.VertexConsumer buf, org.joml.Matrix4f m,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float r, float g, float b, float a) {
        buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        buf.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        buf.addVertex(m, x2, y2 + 0.02f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
        buf.addVertex(m, x1, y1 + 0.02f, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(15).setLight(15728880).setNormal(0, 1, 0);
    }

    private static int cellX(TpPad p) { return (p.pos.getX() - minX) / CELL_SIZE; }
    private static int cellZ(TpPad p) { return (p.pos.getZ() - minZ) / CELL_SIZE; }

    private static int countCell(int cx, int cz) {
        int n = 0;
        for (TpPad p : pads) if (cellX(p) == cx && cellZ(p) == cz) n++;
        return n;
    }

    private static double angleDiff(double dx, double dz, float yaw) {
        double yr = Math.toRadians(yaw);
        double lx = -Math.sin(yr), lz = Math.cos(yr);
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d == 0) return 180.0;
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, (lx * dx + lz * dz) / d))));
    }

    private static class TpPad {
        final BlockPos pos;
        boolean visited = false;
        TpPad(BlockPos pos) { this.pos = pos; }
    }
}
