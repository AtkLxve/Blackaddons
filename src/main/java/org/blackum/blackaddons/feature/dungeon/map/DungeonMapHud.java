package org.blackum.blackaddons.feature.dungeon.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.LocationUtils;

import java.util.Set;

public class DungeonMapHud {

    // ── Base room colors ──────────────────────────────────────────────────────
    private static final int CLR_NORMAL   = 0xFFA07010;
    private static final int CLR_ENTRANCE = 0xFF20C020;
    private static final int CLR_BLOOD    = 0xFFCC2020;
    private static final int CLR_FAIRY    = 0xFFDD44DD;
    private static final int CLR_PUZZLE   = 0xFF9B39C8;
    private static final int CLR_TRAP     = 0xFFCC7700;
    private static final int CLR_CHAMPION = 0xFFD4AF00;
    private static final int CLR_RARE     = 0xFFFFCB59;
    private static final int CLR_GREY     = 0xFF555555;
    private static final int CLR_BORDER   = 0xFF555555;

    public static void render(GuiGraphics g) {
        if (!ConfigManager.data.dungeonMapEnabled) return;
        if (!LocationUtils.inDungeons()) return;

        int x    = ConfigManager.data.dungeonMapX;
        int y    = ConfigManager.data.dungeonMapY;
        int size = ConfigManager.data.dungeonMapSize;

        g.fill(x, y, x + size, y + size, 0xC0111111);

        Vec2i   sc        = DungeonMap.getStartCoords();
        Integer roomSizeI = DungeonMap.getRoomSize();
        Vec2i   ms        = DungeonMap.getMapSize();

        if (sc == null || roomSizeI == null || ms == null) {
            drawBorder(g, x, y, size);
            return;
        }

        int   rs       = roomSizeI;
        int   cellSize = rs + 4;
        int   padding  = 4;
        int   inner    = size - padding * 2;
        int   cW       = (ms.x - 1) * cellSize + rs;
        int   cH       = (ms.z - 1) * cellSize + rs;
        float scale    = Math.min(inner / (float) cW, inner / (float) cH);
        int   offX     = (inner - (int)(cW * scale)) / 2;
        int   offY     = (inner - (int)(cH * scale)) / 2;
        int   dX       = x + padding + offX;
        int   dY       = y + padding + offY;

        g.enableScissor(x, y, x + size, y + size);

        Set<Room> rooms = DungeonMap.getRooms();

        // ── Smart deduction ──────────────────────────────────────────────────
        int     discoveredPuzzles = 0;
        boolean trapDiscovered    = false;
        for (Room r : rooms) {
            Room.State s = r.state;
            if (s == Room.State.UNDISCOVERED || s == Room.State.UNOPENED) continue;
            if (r.type == Room.Type.PUZZLE) discoveredPuzzles++;
            if (r.type == Room.Type.TRAP)   trapDiscovered = true;
        }
        int     totalPuzzles    = DungeonScoreboard.stats.puzzleCount;
        boolean allPuzzlesKnown = totalPuzzles > 0 && discoveredPuzzles >= totalPuzzles;

        // ── Pass 1: draw room tiles ──────────────────────────────────────────
        for (Room room : rooms) {
            if (room.tiles.isEmpty()) continue;
            Room.Type eff = effectiveType(room, allPuzzlesKnown, trapDiscovered);
            drawTiles(g, room, eff, dX, dY, cellSize, rs, scale, allPuzzlesKnown, trapDiscovered);
        }

        // ── Draw doors ───────────────────────────────────────────────────────
        for (Door door : DungeonMap.getDoors()) {
            if (!door.isSeen()) continue;
            float[] dp  = door.placement(8f, rs);
            Vec2i   dsz = door.size(8f, rs);
            g.fill(
                dX + (int)(dp[0] * scale),           dY + (int)(dp[1] * scale),
                dX + (int)((dp[0] + dsz.x) * scale), dY + (int)((dp[1] + dsz.z) * scale),
                doorColor(door)
            );
        }

        // ── Pass 2: overlays (text / icons) ──────────────────────────────────
        Minecraft mc = Minecraft.getInstance();
        for (Room room : rooms) {
            if (room.tiles.isEmpty()) continue;
            if (room.type == Room.Type.ENTRANCE) continue;
            Room.Type eff = effectiveType(room, allPuzzlesKnown, trapDiscovered);

            boolean undiscovered = room.state == Room.State.UNDISCOVERED;
            boolean unopened     = room.state == Room.State.UNOPENED;
            boolean isSpecial = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);

            int cx, cz;
            if (undiscovered || (unopened && !isSpecial)) {
                Room.Tile t = getAdjacentTile(room);
                if (t == null) continue; // not adjacent, skip entirely
                int gx = (t.pos.x + 185) / 32;
                int gz = (t.pos.z + 185) / 32;
                cx = dX + (int)((gx * cellSize + rs / 2f) * scale);
                cz = dY + (int)((gz * cellSize + rs / 2f) * scale);
            } else {
                cx = centerX(room, dX, cellSize, rs, scale);
                cz = centerZ(room, dY, cellSize, rs, scale);
            }
            
            boolean ambiguous = isSpecial && unopened && !allPuzzlesKnown && !trapDiscovered;
            drawOverlay(g, mc, room, eff, cx, cz, scale, ambiguous);
        }

        // ── Player dots ───────────────────────────────────────────────────────
        DungeonScoreboard.DungeonPlayer self = DungeonScoreboard.selfPlayer;
        if (self != null && self.mapPos != null) {
            int px = dX + (int)((self.mapPos.x - sc.x) * scale);
            int pz = dY + (int)((self.mapPos.z - sc.z) * scale);
            drawPlayerDot(g, px, pz, 0xFF00FF00, self.yaw);
        }
        for (DungeonScoreboard.DungeonPlayer p : DungeonScoreboard.teammates) {
            if (p.dead || p.mapPos == null) continue;
            int px = dX + (int)((p.mapPos.x - sc.x) * scale);
            int pz = dY + (int)((p.mapPos.z - sc.z) * scale);
            drawPlayerDot(g, px, pz, classColor(p.dungeonClass), p.yaw);
        }

        g.disableScissor();
        drawBorder(g, x, y, size);
    }

    // ── Tile drawing ──────────────────────────────────────────────────────────

    private static void drawTiles(GuiGraphics g, Room room, Room.Type eff,
                                  int dX, int dY, int cellSize, int rs, float scale,
                                  boolean allPuzzlesKnown, boolean trapDiscovered) {
        boolean undiscovered = room.state == Room.State.UNDISCOVERED;
        boolean unopened     = room.state == Room.State.UNOPENED;
        boolean isSpecial    = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);
        boolean ambiguous    = isSpecial && unopened && !allPuzzlesKnown && !trapDiscovered;

        // ── Case 1: non-special hidden room — show 1x1 grey if adjacent to opened ──
        if (undiscovered || (unopened && !isSpecial)) {
            Room.Tile t = getAdjacentTile(room);
            if (t == null) return;
            int gx = (t.pos.x + 185) / 32;
            int gz = (t.pos.z + 185) / 32;
            int x1 = dX + (int)(gx * cellSize * scale);
            int x2 = dX + (int)((gx * cellSize + rs) * scale);
            int z1 = dY + (int)(gz * cellSize * scale);
            int z2 = dY + (int)((gz * cellSize + rs) * scale);
            g.fill(x1, z1, x2, z2, CLR_GREY);
            return;
        }

        // ── Case 2: unopened special AND ambiguous (puzzle/trap) — full size, 50% purple / 50% orange ──
        if (ambiguous) {
            for (Room.Tile tile : room.tiles) {
                int gx = (tile.pos.x + 185) / 32;
                int gz = (tile.pos.z + 185) / 32;
                int x1 = dX + (int)(gx * cellSize * scale);
                int x2 = dX + (int)((gx * cellSize + rs) * scale);
                int z1 = dY + (int)(gz * cellSize * scale);
                int z2 = dY + (int)((gz * cellSize + rs) * scale);
                int mx = (x1 + x2) / 2;
                g.fill(x1, z1, mx, z2, darken(CLR_PUZZLE, 0.75f)); // left half: purple
                g.fill(mx, z1, x2, z2, darken(CLR_TRAP,   0.75f)); // right half: orange
                // Connectors (use average color)
                int connClr = darken(CLR_PUZZLE, 0.75f);
                if (hasTileAt(room, gx + 1, gz))
                    g.fill(x2, z1, dX + (int)((gx + 1) * cellSize * scale), z2, connClr);
                if (hasTileAt(room, gx, gz + 1))
                    g.fill(x1, z2, x2, dY + (int)((gz + 1) * cellSize * scale), connClr);
                if (hasTileAt(room, gx+1, gz) && hasTileAt(room, gx, gz+1) && hasTileAt(room, gx+1, gz+1))
                    g.fill(x2, z2, dX+(int)((gx+1)*cellSize*scale), dY+(int)((gz+1)*cellSize*scale), connClr);
            }
            return;
        }

        // ── Case 3: discovered/cleared/green/failed — full size, type color ──
        for (Room.Tile tile : room.tiles) {
            int gx = (tile.pos.x + 185) / 32;
            int gz = (tile.pos.z + 185) / 32;
            int x1 = dX + (int)(gx * cellSize * scale);
            int x2 = dX + (int)((gx * cellSize + rs) * scale);
            int z1 = dY + (int)(gz * cellSize * scale);
            int z2 = dY + (int)((gz * cellSize + rs) * scale);
            int clr = baseColor(eff);
            g.fill(x1, z1, x2, z2, clr);
            if (hasTileAt(room, gx + 1, gz))
                g.fill(x2, z1, dX + (int)((gx + 1) * cellSize * scale), z2, clr);
            if (hasTileAt(room, gx, gz + 1))
                g.fill(x1, z2, x2, dY + (int)((gz + 1) * cellSize * scale), clr);
            if (hasTileAt(room, gx+1, gz) && hasTileAt(room, gx, gz+1) && hasTileAt(room, gx+1, gz+1))
                g.fill(x2, z2, dX+(int)((gx+1)*cellSize*scale), dY+(int)((gz+1)*cellSize*scale), clr);
        }
    }

    // ── Overlay drawing ───────────────────────────────────────────────────────

    private static void drawOverlay(GuiGraphics g, Minecraft mc, Room room, Room.Type eff, int cx, int cz, float scale, boolean ambiguous) {
        switch (room.state) {
            case UNDISCOVERED:
                drawQuestionMark(g, mc, cx, cz, 0xFFAAAAAA);
                break;
            case GREEN:
                if (eff != Room.Type.FAIRY) drawCheckmark(g, mc, cx, cz, 0xFF55FF55);
                break;
            case CLEARED:
                if (eff != Room.Type.FAIRY) drawCheckmark(g, mc, cx, cz, 0xFFFFFFFF);
                break;
            case FAILED:
                drawXMark(g, mc, cx, cz, 0xFFFF5555);
                break;
            case UNOPENED:
                if (room.type != Room.Type.PUZZLE && room.type != Room.Type.TRAP) {
                    drawQuestionMark(g, mc, cx, cz, 0xFFAAAAAA);
                }
                break;
            default:
                break;
        }
    }

    private static void drawCheckmark(GuiGraphics g, Minecraft mc, int cx, int cz, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 1.5f, () -> g.drawCenteredString(mc.font, "\u2713", 0, -h, color));
    }

    private static void drawXMark(GuiGraphics g, Minecraft mc, int cx, int cz, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 1.5f, () -> g.drawCenteredString(mc.font, "\u2717", 0, -h, color));
    }

    private static void drawQuestionMark(GuiGraphics g, Minecraft mc, int cx, int cz, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 1.5f, () -> g.drawCenteredString(mc.font, "?", 0, -h, color));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void scaled(GuiGraphics g, int cx, int cz, float s, Runnable draw) {
        g.pose().pushMatrix();
        g.pose().translate((float) cx, (float) cz);
        g.pose().scale(s, s);
        draw.run();
        g.pose().popMatrix();
    }

    private static Room.Type effectiveType(Room room, boolean allPuzzlesKnown, boolean trapDiscovered) {
        if (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP) {
            if (room.state == Room.State.UNOPENED) {
                if (trapDiscovered) return Room.Type.PUZZLE;
                if (allPuzzlesKnown) return Room.Type.TRAP;
            }
        }
        return room.type;
    }

    private static int baseColor(Room.Type type) {
        switch (type) {
            case ENTRANCE:  return CLR_ENTRANCE;
            case BLOOD:     return CLR_BLOOD;
            case PUZZLE:    return CLR_PUZZLE;
            case TRAP:      return CLR_TRAP;
            case CHAMPION:  return CLR_CHAMPION;
            case RARE:      return CLR_RARE;
            case FAIRY:     return CLR_FAIRY;
            default:        return CLR_NORMAL;
        }
    }

    private static int darken(int argb, float f) {
        int a = (argb >> 24) & 0xFF;
        int r = (int)(((argb >> 16) & 0xFF) * f);
        int ge = (int)(((argb >> 8) & 0xFF) * f);
        int b = (int)((argb & 0xFF) * f);
        return (a << 24) | (r << 16) | (ge << 8) | b;
    }

    private static int doorColor(Door door) {
        if (door.type == Door.Type.BLOOD)  return 0xFFCC2020;
        if (door.type == Door.Type.WITHER) return 0xFF111111;
        return 0xFF794600;
    }

    private static boolean hasTileAt(Room room, int gx, int gz) {
        for (Room.Tile t : room.tiles)
            if ((t.pos.x + 185) / 32 == gx && (t.pos.z + 185) / 32 == gz) return true;
        return false;
    }

    private static int centerX(Room room, int dX, int cellSize, int rs, float scale) {
        if (room.shape == Room.Shape.SL) {
            Room.Tile c = cornerTile(room);
            if (c != null) return dX + (int)(((c.pos.x + 185) / 32 * cellSize + rs / 2f) * scale);
        }
        float sum = 0;
        for (Room.Tile t : room.tiles) sum += (t.pos.x + 185) / 32 * cellSize + rs / 2f;
        return dX + (int)((sum / room.tiles.size()) * scale);
    }

    private static int centerZ(Room room, int dY, int cellSize, int rs, float scale) {
        if (room.shape == Room.Shape.SL) {
            Room.Tile c = cornerTile(room);
            if (c != null) return dY + (int)(((c.pos.z + 185) / 32 * cellSize + rs / 2f) * scale);
        }
        float sum = 0;
        for (Room.Tile t : room.tiles) sum += (t.pos.z + 185) / 32 * cellSize + rs / 2f;
        return dY + (int)((sum / room.tiles.size()) * scale);
    }

    private static Room.Tile cornerTile(Room room) {
        Room.Tile best = null; int bestN = 0;
        for (Room.Tile t : room.tiles) {
            int gx = (t.pos.x + 185) / 32, gz = (t.pos.z + 185) / 32, n = 0;
            for (Room.Tile o : room.tiles) {
                int ox = (o.pos.x + 185) / 32, oz = (o.pos.z + 185) / 32;
                if (Math.abs(ox - gx) + Math.abs(oz - gz) == 1) n++;
            }
            if (n > bestN) { bestN = n; best = t; }
        }
        return best;
    }

    /** Returns the tile of this room that is closest to an opened adjacent room, or null if none. */
    private static Room.Tile getAdjacentTile(Room room) {
        for (Door door : room.doors) {
            for (Room adj : door.rooms) {
                if (adj == room) continue;
                Room.State s = adj.state;
                if (s != Room.State.UNDISCOVERED && s != Room.State.UNOPENED) {
                    Room.Tile best = null;
                    double minDist = Double.MAX_VALUE;
                    for (Room.Tile t : room.tiles) {
                        double dx = (t.pos.x + 16.0) - door.pos.x;
                        double dz = (t.pos.z + 16.0) - door.pos.z;
                        double dist = dx * dx + dz * dz;
                        if (dist < minDist) {
                            minDist = dist;
                            best = t;
                        }
                    }
                    if (best != null) return best;
                }
            }
        }
        return null;
    }

    private static int classColor(String cls) {
        if (cls == null) return 0xFFAAAAAA;
        switch (cls.toUpperCase()) {
            case "HEALER":  return 0xFFFF4444;
            case "MAGE":    return 0xFF4444FF;
            case "BERSERK": return 0xFFFF4400;
            case "ARCHER":  return 0xFF44FF44;
            case "TANK":    return 0xFF44AAFF;
            default:        return 0xFFAAAAAA;
        }
    }

    private static void drawPlayerDot(GuiGraphics g, int cx, int cy, int color, float yaw) {
        int r = (color >> 16) & 0xFF, ge = (color >> 8) & 0xFF, b = color & 0xFF;
        int bc = 0xFF000000 | ((r / 2) << 16) | ((ge / 2) << 8) | (b / 2);
        g.fill(cx - 3, cy,     cx + 4, cy + 1, bc);
        g.fill(cx - 2, cy - 1, cx + 3, cy,     bc);
        g.fill(cx - 2, cy + 1, cx + 3, cy + 2, bc);
        g.fill(cx - 1, cy - 2, cx + 2, cy - 1, bc);
        g.fill(cx - 1, cy + 2, cx + 2, cy + 3, bc);
        g.fill(cx - 2, cy,     cx + 3, cy + 1, color);
        g.fill(cx - 1, cy - 1, cx + 2, cy,     color);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 2, color);
        g.fill(cx,     cy - 2, cx + 1, cy - 1, color);
        g.fill(cx,     cy + 2, cx + 1, cy + 3, color);
        double rad = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(rad) * 2);
        int dz = (int) Math.round(Math.cos(rad) * 2);
        g.fill(cx + dx, cy + dz, cx + dx + 1, cy + dz + 1, 0xFFFFFFFF);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int size) {
        g.fill(x,          y,              x + size, y + 1,          CLR_BORDER);
        g.fill(x,          y + size - 1,  x + size, y + size,       CLR_BORDER);
        g.fill(x,          y,              x + 1,    y + size,       CLR_BORDER);
        g.fill(x + size-1, y,              x + size, y + size,       CLR_BORDER);
    }
}
