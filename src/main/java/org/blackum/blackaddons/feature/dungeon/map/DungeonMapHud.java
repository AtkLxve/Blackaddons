package org.blackum.blackaddons.feature.dungeon.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.LocationUtils;

import java.util.List;
import java.util.Set;

public class DungeonMapHud {

    public static void render(GuiGraphics g) {
        if (!ConfigManager.data.dungeonMapEnabled) return;
        if (!LocationUtils.inDungeons()) return;

        int x = ConfigManager.data.dungeonMapX;
        int y = ConfigManager.data.dungeonMapY;
        int size = ConfigManager.data.dungeonMapSize;

        g.fill(x, y, x + size, y + size, 0xC0111111);

        Vec2i sc = DungeonMap.getStartCoords();
        Integer roomSizeI = DungeonMap.getRoomSize();
        Vec2i ms = DungeonMap.getMapSize();

        if (sc != null && roomSizeI != null && ms != null) {
            int rs = roomSizeI;
            int cellSize = rs + 4;

            int padding = 4;
            int innerSize = size - padding * 2;
            int contentW = (ms.x - 1) * cellSize + rs;
            int contentH = (ms.z - 1) * cellSize + rs;
            float scale = Math.min(innerSize / (float) contentW, innerSize / (float) contentH);

            int offsetX = (innerSize - (int) (contentW * scale)) / 2;
            int offsetY = (innerSize - (int) (contentH * scale)) / 2;

            int roomPx = Math.max(1, (int)(rs * scale));

            g.enableScissor(x, y, x + size, y + size);

            int drawX = x + padding + offsetX;
            int drawY = y + padding + offsetY;

            Set<Room> rooms = DungeonMap.getRooms();

            boolean funnyMap = ConfigManager.data.dungeonFunnyMap;
            for (Room room : rooms) {
                if (!funnyMap && room.state == Room.State.UNDISCOVERED) continue;
                int roomColor = getRoomColor(room, funnyMap);
                for (Room.Tile tile : room.tiles) {
                    int gx = (tile.pos.x + 185) / 32;
                    int gz = (tile.pos.z + 185) / 32;

                    int x1 = drawX + (int) (gx * cellSize * scale);
                    int x2 = drawX + (int) ((gx * cellSize + rs) * scale);
                    int z1 = drawY + (int) (gz * cellSize * scale);
                    int z2 = drawY + (int) ((gz * cellSize + rs) * scale);

                    g.fill(x1, z1, x2, z2, roomColor);

                    if (hasTileAt(room, gx + 1, gz)) {
                        int nextX1 = drawX + (int) ((gx + 1) * cellSize * scale);
                        g.fill(x2, z1, nextX1, z2, roomColor);
                    }
                    if (hasTileAt(room, gx, gz + 1)) {
                        int nextZ1 = drawY + (int) ((gz + 1) * cellSize * scale);
                        g.fill(x1, z2, x2, nextZ1, roomColor);
                    }
                    if (hasTileAt(room, gx + 1, gz) && hasTileAt(room, gx, gz + 1) && hasTileAt(room, gx + 1, gz + 1)) {
                        int nextX1 = drawX + (int) ((gx + 1) * cellSize * scale);
                        int nextZ1 = drawY + (int) ((gz + 1) * cellSize * scale);
                        g.fill(x2, z2, nextX1, nextZ1, roomColor);
                    }
                }
            }

            for (Door door : DungeonMap.getDoors()) {
                if (!door.isSeen()) continue;
                float[] dp = door.placement(16f, rs);
                Vec2i ds = door.size(16f, rs);
                int dpx1 = drawX + (int)(dp[0] * scale);
                int dpz1 = drawY + (int)(dp[1] * scale);
                int dpx2 = drawX + (int)((dp[0] + ds.x) * scale);
                int dpz2 = drawY + (int)((dp[1] + ds.z) * scale);
                g.fill(dpx1, dpz1, dpx2, dpz2, getDoorColor(door));
            }

            Minecraft mc = Minecraft.getInstance();

            for (Room room : rooms) {
                boolean isHidden = room.state == Room.State.UNDISCOVERED || room.state == Room.State.UNOPENED;
                if (isHidden && !funnyMap) continue;
                if (room.type == Room.Type.ENTRANCE) continue;
                if (room.tiles.isEmpty()) continue;

                int cx, cz;
                if (room.shape == Room.Shape.SL) {
                    Room.Tile corner = findCornerTile(room);
                    if (corner != null) {
                        int gx = (corner.pos.x + 185) / 32;
                        int gz = (corner.pos.z + 185) / 32;
                        cx = drawX + (int)((gx * cellSize + rs / 2) * scale);
                        cz = drawY + (int)((gz * cellSize + rs / 2) * scale);
                    } else {
                        cx = drawX + (int)(centroidX(room, cellSize, rs) * scale);
                        cz = drawY + (int)(centroidZ(room, cellSize, rs) * scale);
                    }
                } else {
                    cx = drawX + (int)(centroidX(room, cellSize, rs) * scale);
                    cz = drawY + (int)(centroidZ(room, cellSize, rs) * scale);
                }

                String mark = getStateMark(room);
                if (mark != null) {
                    g.drawCenteredString(mc.font, mark, cx, cz - mc.font.lineHeight / 2, getMarkColor(room));
                } else if (room.data != null && (room.state == Room.State.DISCOVERED || isHidden)) {
                    String name = room.data.name.length() > 8 ? room.data.name.substring(0, 8) : room.data.name;
                    g.pose().pushMatrix();
                    g.pose().translate((float) cx, (float) cz);
                    g.pose().scale(0.5f, 0.5f);
                    g.drawCenteredString(mc.font, name, 0, -mc.font.lineHeight / 2, 0xFFCCCCCC);
                    g.pose().popMatrix();
                }
            }

            DungeonScoreboard.DungeonPlayer self = DungeonScoreboard.selfPlayer;
            if (self != null && self.mapPos != null) {
                int px = drawX + (int)((self.mapPos.x - sc.x) * scale);
                int pz = drawY + (int)((self.mapPos.z - sc.z) * scale);
                drawPlayerDot(g, px, pz, 0xFF00FF00, self.yaw);
            }

            List<DungeonScoreboard.DungeonPlayer> teammates = DungeonScoreboard.teammates;
            for (DungeonScoreboard.DungeonPlayer p : teammates) {
                if (p.dead || p.mapPos == null) continue;
                int px = drawX + (int)((p.mapPos.x - sc.x) * scale);
                int pz = drawY + (int)((p.mapPos.z - sc.z) * scale);
                drawPlayerDot(g, px, pz, getClassColor(p.dungeonClass), p.yaw);
            }

            g.disableScissor();
        }

        g.fill(x, y, x + size, y + 1, 0xFF4A5568);
        g.fill(x, y + size - 1, x + size, y + size, 0xFF4A5568);
        g.fill(x, y, x + 1, y + size, 0xFF4A5568);
        g.fill(x + size - 1, y, x + size, y + size, 0xFF4A5568);
    }

    private static float centroidX(Room room, int cellSize, int rs) {
        float sum = 0;
        for (Room.Tile tile : room.tiles) sum += (tile.pos.x + 185) / 32 * cellSize + rs / 2f;
        return sum / room.tiles.size();
    }

    private static float centroidZ(Room room, int cellSize, int rs) {
        float sum = 0;
        for (Room.Tile tile : room.tiles) sum += (tile.pos.z + 185) / 32 * cellSize + rs / 2f;
        return sum / room.tiles.size();
    }

    private static Room.Tile findCornerTile(Room room) {
        Room.Tile best = null;
        int bestNeighbors = 0;
        for (Room.Tile t : room.tiles) {
            int gx = (t.pos.x + 185) / 32;
            int gz = (t.pos.z + 185) / 32;
            int n = 0;
            for (Room.Tile o : room.tiles) {
                int ox = (o.pos.x + 185) / 32;
                int oz = (o.pos.z + 185) / 32;
                if (Math.abs(ox - gx) + Math.abs(oz - gz) == 1) n++;
            }
            if (n > bestNeighbors) { bestNeighbors = n; best = t; }
        }
        return best;
    }

    private static int getRoomColor(Room room, boolean funnyMap) {
        int base;
        switch (room.type) {
            case ENTRANCE:  base = 0xFF20C020; break;
            case BLOOD:     base = 0xFFCC2020; break;
            case PUZZLE:    base = 0xFFb04bd5; break;
            case CHAMPION:  base = 0xFFFEDF00; break;
            case RARE:      base = 0xFFFFCB59; break;
            case FAIRY:     base = 0xFFFF88FF; break;
            case TRAP:      base = 0xFFCC8822; break;
            default:        base = 0xFF808080; break;
        }
        if (room.state == Room.State.UNOPENED) return darken(base, 0.55f);
        if (funnyMap && room.state == Room.State.UNDISCOVERED) return darken(base, 0.55f);
        return base;
    }

    private static int darken(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = (int)(((argb >> 16) & 0xFF) * factor);
        int g = (int)(((argb >> 8)  & 0xFF) * factor);
        int b = (int)((argb         & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getDoorColor(Door door) {
        if (door.type == Door.Type.BLOOD)  return 0xFFCC2020;
        if (door.type == Door.Type.WITHER) return 0xFF111111;
        return 0xFF888888;
    }

    private static String getStateMark(Room room) {
        switch (room.state) {
            case GREEN:
            case CLEARED: return "\u2713";
            case FAILED:  return "\u2717";
            default:      return null;
        }
    }

    private static int getMarkColor(Room room) {
        switch (room.state) {
            case GREEN:   return 0xFF00FF00;
            case CLEARED: return 0xFFFFFFFF;
            case FAILED:  return 0xFFFF3333;
            default:      return 0xFFAAAAAA;
        }
    }

    private static int getClassColor(String cls) {
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

    private static boolean hasTileAt(Room room, int gx, int gz) {
        for (Room.Tile t : room.tiles) {
            if ((t.pos.x + 185) / 32 == gx && (t.pos.z + 185) / 32 == gz) return true;
        }
        return false;
    }

    private static void drawPlayerDot(GuiGraphics g, int cx, int cy, int color, float yaw) {
        int r = (color >> 16) & 0xFF;
        int ge = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        int borderColor = 0xFF000000 | ((r/2) << 16) | ((ge/2) << 8) | (b/2);
        
        g.fill(cx - 3, cy, cx + 4, cy + 1, borderColor);
        g.fill(cx - 2, cy - 1, cx + 3, cy, borderColor);
        g.fill(cx - 2, cy + 1, cx + 3, cy + 2, borderColor);
        g.fill(cx - 1, cy - 2, cx + 2, cy - 1, borderColor);
        g.fill(cx - 1, cy + 2, cx + 2, cy + 3, borderColor);
        
        g.fill(cx - 2, cy, cx + 3, cy + 1, color);
        g.fill(cx - 1, cy - 1, cx + 2, cy, color);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 2, color);
        g.fill(cx, cy - 2, cx + 1, cy - 1, color);
        g.fill(cx, cy + 2, cx + 1, cy + 3, color);

        double rad = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(rad) * 2);
        int dz = (int) Math.round(Math.cos(rad) * 2);
        g.fill(cx + dx, cy + dz, cx + dx + 1, cy + dz + 1, 0xFFFFFFFF);
    }
}
