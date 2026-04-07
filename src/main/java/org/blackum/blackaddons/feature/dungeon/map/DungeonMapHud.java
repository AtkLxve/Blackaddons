package org.blackum.blackaddons.feature.dungeon.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.LocationUtils;

import java.util.Set;

public class DungeonMapHud {

    private static final int CLR_NORMAL   = 0xFFA07010;
    private static final int CLR_ENTRANCE = 0xFF20C020;
    private static final int CLR_BLOOD    = 0xFFCC2020;
    private static final int CLR_FAIRY    = 0xFFDD44DD;
    private static final int CLR_PUZZLE   = 0xFF9B39C8;
    private static final int CLR_TRAP     = 0xFFCC7700;
    private static final int CLR_CHAMPION = 0xFFD4AF00;
    private static final int CLR_RARE     = 0xFFFFCB59;
    private static final int CLR_MIMIC    = 0xFFFF6600;
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
        boolean funnyMap  = ConfigManager.data.dungeonFunnyMap;

        if (funnyMap) {
            if (roomSizeI == null) roomSizeI = 16;
            if (ms == null) {
                int maxGx = 0, maxGz = 0;
                for (Room r : DungeonMap.getRooms()) {
                    for (Room.Tile t : r.tiles) {
                        int gx = (t.pos.x + 185) / 32;
                        int gz = (t.pos.z + 185) / 32;
                        if (gx > maxGx) maxGx = gx;
                        if (gz > maxGz) maxGz = gz;
                    }
                }
                if (maxGx > 0 || maxGz > 0) ms = new Vec2i(maxGx + 1, maxGz + 1);
            }
        }

        if (roomSizeI == null || ms == null) {
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

        for (Room room : rooms) {
            if (room.tiles.isEmpty()) continue;
            Room.Type eff = effectiveType(room, allPuzzlesKnown, trapDiscovered);
            drawTiles(g, room, eff, dX, dY, cellSize, rs, scale, allPuzzlesKnown, trapDiscovered, funnyMap);
        }

        for (Door door : DungeonMap.getDoors()) {
            if (!door.isSeen() && !(funnyMap && door.worldScanned)) continue;
            float[] dp  = door.placement(8f, rs);
            Vec2i   dsz = door.size(8f, rs);
            g.fill(
                dX + (int)(dp[0] * scale),           dY + (int)(dp[1] * scale),
                dX + (int)((dp[0] + dsz.x) * scale), dY + (int)((dp[1] + dsz.z) * scale),
                doorColor(door)
            );
        }

        Minecraft mc = Minecraft.getInstance();
        for (Room room : rooms) {
            if (room.tiles.isEmpty()) continue;
            if (room.type == Room.Type.ENTRANCE) continue;
            Room.Type eff = effectiveType(room, allPuzzlesKnown, trapDiscovered);

            boolean undiscovered = room.state == Room.State.UNDISCOVERED;
            boolean unopened     = room.state == Room.State.UNOPENED;
            boolean isSpecial    = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);

            int cx, cz;
            if (!funnyMap && (undiscovered || (unopened && !isSpecial))) {
                Vec2i gp = room.entryTile;
                if (gp == null) {
                    Room.Tile t = getAdjacentTile(room);
                    if (t != null) gp = new Vec2i((t.pos.x + 185) / 32, (t.pos.z + 185) / 32);
                }

                if (gp == null) continue;

                int gx = gp.x;
                int gz = gp.z;
                int x1 = dX + (int)(gx * cellSize * scale);
                int z1 = dY + (int)(gz * cellSize * scale);
                int x2 = dX + (int)((gx * cellSize + rs) * scale);
                int z2 = dY + (int)((gz * cellSize + rs) * scale);
                g.fill(x1, z1, x2, z2, CLR_GREY);
                cx = (x1 + x2) / 2;
                cz = (z1 + z2) / 2;
            } else {
                cx = centerX(room, dX, cellSize, rs, scale);
                cz = centerZ(room, dY, cellSize, rs, scale);
            }
            
            boolean ambiguous = isSpecial && unopened && !allPuzzlesKnown && !trapDiscovered
                                && !(funnyMap && room.data != null);
            drawOverlay(g, mc, room, eff, cx, cz, scale, ambiguous, funnyMap);
        }

        if (sc != null) {
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
        }

        g.disableScissor();
        drawBorder(g, x, y, size);
    }

    private static void drawTiles(GuiGraphics g, Room room, Room.Type eff,
                                  int dX, int dY, int cellSize, int rs, float scale,
                                  boolean allPuzzlesKnown, boolean trapDiscovered, boolean funnyMap) {
        boolean undiscovered = room.state == Room.State.UNDISCOVERED;
        boolean unopened     = room.state == Room.State.UNOPENED;
        boolean isSpecial    = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);
        boolean ambiguous    = isSpecial && unopened && !allPuzzlesKnown && !trapDiscovered;

        if (!funnyMap && (undiscovered || (unopened && !isSpecial))) {
            return;
        }

        if (ambiguous && !(funnyMap && room.data != null)) {
            for (Room.Tile tile : room.tiles) {
                int gx = (tile.pos.x + 185) / 32;
                int gz = (tile.pos.z + 185) / 32;
                int x1 = dX + (int)(gx * cellSize * scale);
                int x2 = dX + (int)((gx * cellSize + rs) * scale);
                int z1 = dY + (int)(gz * cellSize * scale);
                int z2 = dY + (int)((gz * cellSize + rs) * scale);
                int mx = (x1 + x2) / 2;
                g.fill(x1, z1, mx, z2, darken(CLR_PUZZLE, 0.75f));
                g.fill(mx, z1, x2, z2, darken(CLR_TRAP,   0.75f));
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

        for (Room.Tile tile : room.tiles) {
            int gx = (tile.pos.x + 185) / 32;
            int gz = (tile.pos.z + 185) / 32;
            int x1 = dX + (int)(gx * cellSize * scale);
            int x2 = dX + (int)((gx * cellSize + rs) * scale);
            int z1 = dY + (int)(gz * cellSize * scale);
            int z2 = dY + (int)((gz * cellSize + rs) * scale);
            int clr = room.mimic ? CLR_MIMIC
                    : (funnyMap && undiscovered) ? darken(baseColor(eff), 0.55f)
                    : baseColor(eff);
            g.fill(x1, z1, x2, z2, clr);
            if (hasTileAt(room, gx + 1, gz))
                g.fill(x2, z1, dX + (int)((gx + 1) * cellSize * scale), z2, clr);
            if (hasTileAt(room, gx, gz + 1))
                g.fill(x1, z2, x2, dY + (int)((gz + 1) * cellSize * scale), clr);
            if (hasTileAt(room, gx+1, gz) && hasTileAt(room, gx, gz+1) && hasTileAt(room, gx+1, gz+1))
                g.fill(x2, z2, dX+(int)((gx+1)*cellSize*scale), dY+(int)((gz+1)*cellSize*scale), clr);
        }
    }

    private static void drawOverlay(GuiGraphics g, Minecraft mc, Room room, Room.Type eff,
                                    int cx, int cz, float scale, boolean ambiguous, boolean funnyMap) {
        boolean hasName = room.data != null;
        switch (room.state) {
            case GREEN:
                if (eff != Room.Type.FAIRY) drawCheckmark(g, mc, cx, cz, 0xFF55FF55);
                break;
            case CLEARED:
                if (eff != Room.Type.FAIRY) drawCheckmark(g, mc, cx, cz, 0xFFFFFFFF);
                break;
            case FAILED:
                drawXMark(g, mc, cx, cz, 0xFFFF5555);
                break;
            case DISCOVERED:
                if (hasName) drawName(g, mc, cx, cz, room.data.name, 0xFFFFFFFF);
                break;
            case UNDISCOVERED:
                if (hasName && funnyMap) drawName(g, mc, cx, cz, room.data.name, 0xFFAAAAAA);
                else drawQuestionMark(g, mc, cx, cz, 0xFFAAAAAA);
                break;
            case UNOPENED:
                if (hasName && funnyMap) {
                    drawName(g, mc, cx, cz, room.data.name, 0xFFAAAAAA);
                } else if (ambiguous) {
                    drawQuestionMark(g, mc, cx, cz, 0xFFAAAAAA);
                } else if (room.type != Room.Type.PUZZLE && room.type != Room.Type.TRAP) {
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

    private static void drawName(GuiGraphics g, Minecraft mc, int cx, int cz, String name, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 0.7f, () -> g.drawCenteredString(mc.font, name, 0, -h, color));
    }

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
            Room.Tile anchor = findAnchorTile(room);
            int gx = (anchor.pos.x + 185) / 32;
            return dX + (int)((gx * cellSize + rs / 2f) * scale);
        }
        if (room.tiles.isEmpty()) return dX;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Room.Tile t : room.tiles) {
            int g = (t.pos.x + 185) / 32;
            if (g < min) min = g;
            if (g > max) max = g;
        }
        return dX + (int)(((min + max) / 2f * cellSize + rs / 2f) * scale);
    }

    private static int centerZ(Room room, int dY, int cellSize, int rs, float scale) {
        if (room.shape == Room.Shape.SL) {
            Room.Tile anchor = findAnchorTile(room);
            int gz = (anchor.pos.z + 185) / 32;
            return dY + (int)((gz * cellSize + rs / 2f) * scale);
        }
        if (room.tiles.isEmpty()) return dY;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Room.Tile t : room.tiles) {
            int g = (t.pos.z + 185) / 32;
            if (g < min) min = g;
            if (g > max) max = g;
        }
        return dY + (int)(((min + max) / 2f * cellSize + rs / 2f) * scale);
    }

    private static Room.Tile findAnchorTile(Room room) {
        if (room.tiles.size() == 1) return room.tiles.get(0);

        Room.Tile best = null;
        int maxNeighbors = -1;
        double minDistance = Double.MAX_VALUE;

        double avgGx = 0, avgGz = 0;

        for (Room.Tile t : room.tiles) {
            avgGx += (t.pos.x + 185) / 32.0;
            avgGz += (t.pos.z + 185) / 32.0;
        }
        avgGx /= room.tiles.size();
        avgGz /= room.tiles.size();

        for (Room.Tile t : room.tiles) {
            int gx = (t.pos.x + 185) / 32;
            int gz = (t.pos.z + 185) / 32;
            int neighbors = 0;
            if (hasTileAt(room, gx + 1, gz)) neighbors++;
            if (hasTileAt(room, gx - 1, gz)) neighbors++;
            if (hasTileAt(room, gx, gz + 1)) neighbors++;
            if (hasTileAt(room, gx, gz - 1)) neighbors++;

            double dist = Math.pow(gx - avgGx, 2) + Math.pow(gz - avgGz, 2);

            if (neighbors > maxNeighbors || (neighbors == maxNeighbors && dist < minDistance)) {
                maxNeighbors = neighbors;
                minDistance = dist;
                best = t;
            }
        }
        return best != null ? best : room.tiles.get(0);
    }


    private static Room.Tile getAdjacentTile(Room room) {
        Room.Tile best = null;
        double minDist = Double.MAX_VALUE;
        for (Door door : room.doors) {
            for (Room adj : door.rooms) {
                if (adj == room) continue;
                Room.State s = adj.state;
                if (s != Room.State.UNDISCOVERED && s != Room.State.UNOPENED) {
                    for (Room.Tile t : room.tiles) {
                        double dx = (t.pos.x + 16.0) - door.pos.x;
                        double dz = (t.pos.z + 16.0) - door.pos.z;
                        
                        if (Math.abs(dx) > 36.0 || Math.abs(dz) > 36.0) continue;

                        double dist = dx * dx + dz * dz;

                        if (dist < minDist) {
                            minDist = dist;
                            best = t;
                        } else if (Math.abs(dist - minDist) < 0.1 && best != null) {
                            if (Math.abs(t.pos.x + 16.0 - door.pos.x) < 2.0 || Math.abs(t.pos.z + 16.0 - door.pos.z) < 2.0) {
                                best = t;
                            }
                        }
                    }
                }
            }
        }
        return best;
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
