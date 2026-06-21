package org.blackum.blackaddons.gui.widget.row;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.base.Widget;
import org.blackum.blackaddons.gui.screen.feature.SoloClearMapScreen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SoloClearRow extends Widget {
    private final ConfigManager.SoloClearInfo info;
    private final int index;
    private final String floor;

    public SoloClearRow(int width, int index, ConfigManager.SoloClearInfo info, String floor) {
        super(0, 0, width, 40);
        this.index = index;
        this.info = info;
        this.floor = floor;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, hovered);
        if (hovered) {
            RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, Theme.ACCENT);
        }

        Minecraft mc = Minecraft.getInstance();
        int textY = y + 8;
        
        String mapDisplay = (info.type != null && !info.type.isEmpty()) ? info.type : (floor != null ? floor : "Unknown");
        String title = ChatFormatting.AQUA + mapDisplay + " Run #" + (index + 1) + ChatFormatting.WHITE + " | " + ChatFormatting.YELLOW + info.time;
        graphics.text(mc.font, title, x + 10, textY, 0xFFFFFFFF);

        String stats = ChatFormatting.GRAY + "Secrets: " + ChatFormatting.GREEN + info.secrets +
                       ChatFormatting.GRAY + " | Prince: " + (info.princeKilled ? ChatFormatting.GREEN + "✔" : ChatFormatting.RED + "✘") +
                       ChatFormatting.GRAY + " | Mimic: " + (info.mimicKilled ? ChatFormatting.GREEN + "✔" : ChatFormatting.RED + "✘");
        graphics.text(mc.font, stats, x + 10, textY + 12, 0xFFFFFFFF);

        int rightEdge = x + width - 10;
        
        if (info.mapData != null) {
            int mapSize = 30;
            int mapX = x + width - mapSize - 10;
            int mapY = y + (height - mapSize) / 2;
            renderMap(graphics, info.mapData, mapX, mapY);
            rightEdge = mapX - 10;
        }

        if (!info.puzzles.isEmpty()) {
            List<String> coloredPuzzles = new ArrayList<>();
            for (String p : info.puzzles) {
                if (p.equalsIgnoreCase("Quiz")) {
                    coloredPuzzles.add(ChatFormatting.RED + p + ChatFormatting.GRAY);
                } else {
                    coloredPuzzles.add(p);
                }
            }
            String puzzlesJoined = ChatFormatting.GRAY + "Puzzles: " + ChatFormatting.LIGHT_PURPLE + String.join(", ", coloredPuzzles);
            int puzzlesWidth = mc.font.width(puzzlesJoined);
            graphics.text(mc.font, puzzlesJoined, rightEdge - puzzlesWidth, textY + 6, 0xFFFFFFFF);
        }
    }

    private void renderMap(GuiGraphicsExtractor graphics, JsonObject mapData, int mapX, int mapY) {
        if (!mapData.has("rooms")) return;
        JsonArray rooms = mapData.getAsJsonArray("rooms");
        
        int cellSize = 5;
        RenderHelper.renderRoundedRect(graphics, mapX - 2, mapY - 2, 34, 34, 2, 0xAA000000);
        
        for (JsonElement elem : rooms) {
            JsonObject room = elem.getAsJsonObject();
            String type = room.get("type").getAsString();
            String state = room.get("state").getAsString();
            boolean mimic = room.has("mimic") && room.get("mimic").getAsBoolean();
            JsonArray places = room.getAsJsonArray("places");
            
            int color = mimic ? ConfigManager.data.dungeonMapColorMimic : getRoomColor(type, state);
            
            int minGx = Integer.MAX_VALUE, maxGx = Integer.MIN_VALUE;
            int minGz = Integer.MAX_VALUE, maxGz = Integer.MIN_VALUE;
            for (JsonElement pElem : places) {
                JsonArray p = pElem.getAsJsonArray();
                int gx = p.get(0).getAsInt();
                int gz = p.get(1).getAsInt();
                if (gx < minGx) minGx = gx;
                if (gx > maxGx) maxGx = gx;
                if (gz < minGz) minGz = gz;
                if (gz > maxGz) maxGz = gz;
            }

            int bbArea = (maxGx - minGx + 1) * (maxGz - minGz + 1);
            boolean isLShape = places.size() == 3 && bbArea == 4 && (maxGx - minGx) == 1 && (maxGz - minGz) == 1;

            if (bbArea == places.size()) {
                graphics.fill(mapX + minGx * cellSize, mapY + minGz * cellSize, mapX + (maxGx + 1) * cellSize, mapY + (maxGz + 1) * cellSize, color);
            } else if (isLShape) {
                int missingGx = minGx, missingGz = minGz;
                for (int gx = minGx; gx <= maxGx; gx++) {
                    for (int gz = minGz; gz <= maxGz; gz++) {
                        boolean hasPlace = false;
                        for (JsonElement pElem : places) {
                            JsonArray p = pElem.getAsJsonArray();
                            if (p.get(0).getAsInt() == gx && p.get(1).getAsInt() == gz) {
                                hasPlace = true;
                                break;
                            }
                        }
                        if (!hasPlace) {
                            missingGx = gx;
                            missingGz = gz;
                        }
                    }
                }
                int cornerGx = (minGx + maxGx) - missingGx;
                int cornerGz = (minGz + maxGz) - missingGz;
                
                graphics.fill(mapX + minGx * cellSize, mapY + cornerGz * cellSize, mapX + (maxGx + 1) * cellSize, mapY + (cornerGz + 1) * cellSize, color);
                graphics.fill(mapX + cornerGx * cellSize, mapY + minGz * cellSize, mapX + (cornerGx + 1) * cellSize, mapY + (maxGz + 1) * cellSize, color);
            } else {
                for (JsonElement pElem : places) {
                    JsonArray p = pElem.getAsJsonArray();
                    int rx = p.get(0).getAsInt();
                    int rz = p.get(1).getAsInt();
                    int drawX = mapX + rx * cellSize;
                    int drawY = mapY + rz * cellSize;
                    graphics.fill(drawX, drawY, drawX + cellSize, drawY + cellSize, color);
                }
            }
        }
    }
    
    private int getRoomColor(String type, String state) {
        if ("UNDISCOVERED".equals(state)) return ConfigManager.data.dungeonMapColorUndiscovered;
        
        switch (type) {
            case "ENTRANCE": return ConfigManager.data.dungeonMapColorEntrance;
            case "BLOOD": return ConfigManager.data.dungeonMapColorBlood;
            case "FAIRY": return ConfigManager.data.dungeonMapColorFairy;
            case "PUZZLE": return ConfigManager.data.dungeonMapColorPuzzle;
            case "TRAP": return ConfigManager.data.dungeonMapColorTrap;
            case "CHAMPION": return ConfigManager.data.dungeonMapColorChampion;
            case "MIMIC": return ConfigManager.data.dungeonMapColorMimic;
            default: return ConfigManager.data.dungeonMapColorNormal;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (info.mapData != null) {
            int mapSize = 30;
            int mapX = x + width - mapSize - 10;
            int mapY = y + (height - mapSize) / 2;
            if (mouseX >= mapX && mouseX <= mapX + mapSize && mouseY >= mapY && mouseY <= mapY + mapSize) {
                Minecraft.getInstance().setScreen(new SoloClearMapScreen(info.mapData, Minecraft.getInstance().screen));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
