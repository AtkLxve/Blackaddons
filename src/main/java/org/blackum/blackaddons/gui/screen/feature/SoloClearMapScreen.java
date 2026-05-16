package org.blackum.blackaddons.gui.screen.feature;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class SoloClearMapScreen extends BaseScreen {
    private final JsonObject mapData;

    public SoloClearMapScreen(JsonObject mapData, Screen parent) {
        super(Component.literal("Solo Clear Map"), parent);
        this.mapData = mapData;
    }

    @Override
    protected void initWidgets() {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xDD000000);

        if (mapData != null) {
            int maxGridSize = 6;
            if (mapData.has("map_size")) {
                JsonObject ms = mapData.getAsJsonObject("map_size");
                if (ms.has("x") && ms.has("z")) {
                    maxGridSize = Math.max(ms.get("x").getAsInt(), ms.get("z").getAsInt());
                }
            }
            if (maxGridSize <= 0)
                maxGridSize = 6;

            int cellSize = Math.min(this.width, this.height) / (maxGridSize + 4);
            int mapW = maxGridSize * cellSize;
            int mapH = maxGridSize * cellSize;
            int mapX = (this.width - mapW) / 2;
            int mapY = (this.height - mapH) / 2;

            renderMap(graphics, mapData, mapX, mapY, cellSize);
        }
    }

    private void renderMap(GuiGraphics graphics, JsonObject mapData, int mapX, int mapY, int cellSize) {
        if (!mapData.has("rooms"))
            return;
        JsonArray rooms = mapData.getAsJsonArray("rooms");

        int padding = cellSize / 6;
        int innerSize = cellSize - padding;
        int mapBgColor = Theme.withAlpha(
                ConfigManager.data.dungeonMapBackgroundColor, ConfigManager.data.dungeonMapBackgroundOpacity);

        int maxGridSize = 6;
        if (mapData.has("map_size")) {
            JsonObject ms = mapData.getAsJsonObject("map_size");
            if (ms.has("x") && ms.has("z")) {
                maxGridSize = Math.max(ms.get("x").getAsInt(), ms.get("z").getAsInt());
            }
        }
        graphics.fill(mapX - padding, mapY - padding, mapX + maxGridSize * cellSize + padding,
                mapY + maxGridSize * cellSize + padding, mapBgColor);

        for (JsonElement elem : rooms) {
            JsonObject room = elem.getAsJsonObject();
            String type = room.get("type").getAsString();
            String state = room.get("state").getAsString();
            boolean mimic = room.has("mimic") && room.get("mimic").getAsBoolean();
            JsonArray places = room.getAsJsonArray("places");

            int color = mimic ? ConfigManager.data.dungeonMapColorMimic : getRoomColor(type, state);
            int cornerRadius = (int) (ConfigManager.data.dungeonMapCornerRadius * (cellSize / 16f));
            if (cornerRadius < 2)
                cornerRadius = 2;

            renderRoom(graphics, places, mapX, mapY, cellSize, padding, color, cornerRadius);
        }

        if (mapData.has("doors")) {
            JsonArray doors = mapData.getAsJsonArray("doors");
            for (JsonElement elem : doors) {
                JsonObject door = elem.getAsJsonObject();
                String type = door.get("type").getAsString();
                int color = "BLOOD".equals(type) ? ConfigManager.data.dungeonMapColorBlood
                        : "WITHER".equals(type) ? 0xFF111111 : ConfigManager.data.dungeonMapColorNormal;

                if (door.has("adj_a") && door.has("adj_b")) {
                    JsonArray a = door.getAsJsonArray("adj_a");
                    JsonArray b = door.getAsJsonArray("adj_b");
                    int ax = a.get(0).getAsInt();
                    int az = a.get(1).getAsInt();
                    int bx = b.get(0).getAsInt();
                    int bz = b.get(1).getAsInt();

                    int drawX, drawY, dW, dH;
                    if (az == bz) {
                        int minX = Math.min(ax, bx);
                        drawX = mapX + minX * cellSize + padding / 2 + innerSize;
                        drawY = mapY + az * cellSize + cellSize / 2 - innerSize / 4;
                        dW = padding;
                        dH = innerSize / 2;
                    } else {
                        int minZ = Math.min(az, bz);
                        drawX = mapX + ax * cellSize + cellSize / 2 - innerSize / 4;
                        drawY = mapY + minZ * cellSize + padding / 2 + innerSize;
                        dW = innerSize / 2;
                        dH = padding;
                    }
                    graphics.fill(drawX, drawY, drawX + dW, drawY + dH, color);
                }
            }
        }

        for (JsonElement elem : rooms) {
            JsonObject room = elem.getAsJsonObject();
            String type = room.get("type").getAsString();
            String state = room.get("state").getAsString();
            String name = room.has("name") ? room.get("name").getAsString() : null;
            JsonArray places = room.getAsJsonArray("places");

            String displayName = name;
            if (displayName == null || displayName.isEmpty()) {
                switch (type) {
                    case "ENTRANCE":
                        displayName = "Entrance";
                        break;
                    case "BLOOD":
                        displayName = "Blood";
                        break;
                    case "FAIRY":
                        displayName = "Fairy";
                        break;
                }
            }

            int foundSecrets = room.has("found_secrets") ? room.get("found_secrets").getAsInt() : 0;
            if (foundSecrets == -1) foundSecrets = 0;
            int totalSecrets = room.has("secrets") ? room.get("secrets").getAsInt() : 0;
            if (totalSecrets > 0 && foundSecrets < totalSecrets) {
                displayName += "\n" + foundSecrets + "/" + totalSecrets;
            }

            if (displayName != null && !"UNDISCOVERED".equals(state)) {
                int nameColor = ConfigManager.data.dungeonMapColorNameDiscovered;
                if ("GREEN".equals(state))
                    nameColor = ConfigManager.data.dungeonMapColorNameCompleted;
                else if ("CLEARED".equals(state))
                    nameColor = ConfigManager.data.dungeonMapColorNameCleared;

                nameColor |= 0xFF000000;
                drawRoomName(graphics, displayName, places, mapX, mapY, cellSize, nameColor);
            }
        }
    }

    private void renderRoom(GuiGraphics graphics, JsonArray places, int mapX, int mapY, int cellSize,
            int padding, int color, int cornerRadius) {
        if (places == null || places.isEmpty())
            return;

        int minGx = Integer.MAX_VALUE, maxGx = Integer.MIN_VALUE;
        int minGz = Integer.MAX_VALUE, maxGz = Integer.MIN_VALUE;
        for (JsonElement pElem : places) {
            JsonArray p = pElem.getAsJsonArray();
            int gx = p.get(0).getAsInt();
            int gz = p.get(1).getAsInt();
            if (gx < minGx)
                minGx = gx;
            if (gx > maxGx)
                maxGx = gx;
            if (gz < minGz)
                minGz = gz;
            if (gz > maxGz)
                maxGz = gz;
        }

        int bbArea = (maxGx - minGx + 1) * (maxGz - minGz + 1);
        boolean isLShape = places.size() == 3 && bbArea == 4 && (maxGx - minGx) == 1 && (maxGz - minGz) == 1;

        if (bbArea == places.size()) {
            drawRoomRect(graphics, mapX, mapY, cellSize, padding, color, cornerRadius, minGx, minGz, maxGx, maxGz);
        } else if (isLShape) {
            int missingGx = minGx, missingGz = minGz;
            for (int gx = minGx; gx <= maxGx; gx++) {
                for (int gz = minGz; gz <= maxGz; gz++) {
                    if (!hasPlace(places, gx, gz)) {
                        missingGx = gx;
                        missingGz = gz;
                    }
                }
            }
            int cornerGx = (minGx + maxGx) - missingGx;
            int cornerGz = (minGz + maxGz) - missingGz;

            drawRoomRect(graphics, mapX, mapY, cellSize, padding, color, cornerRadius, minGx, cornerGz, maxGx,
                    cornerGz);
            drawRoomRect(graphics, mapX, mapY, cellSize, padding, color, cornerRadius, cornerGx, minGz, cornerGx,
                    maxGz);
        } else {
            for (int gz = minGz; gz <= maxGz; gz++) {
                int startX = -1;
                for (int gx = minGx; gx <= maxGx + 1; gx++) {
                    if (hasPlace(places, gx, gz)) {
                        if (startX == -1)
                            startX = gx;
                    } else {
                        if (startX != -1) {
                            drawRoomRect(graphics, mapX, mapY, cellSize, padding, color, cornerRadius, startX, gz,
                                    gx - 1, gz);
                            startX = -1;
                        }
                    }
                }
            }
        }
    }

    private boolean hasPlace(JsonArray places, int gx, int gz) {
        for (JsonElement pElem : places) {
            JsonArray p = pElem.getAsJsonArray();
            if (p.get(0).getAsInt() == gx && p.get(1).getAsInt() == gz)
                return true;
        }
        return false;
    }

    private void drawRoomRect(GuiGraphics graphics, int mapX, int mapY, int cellSize, int padding, int color,
            int cornerRadius, int minGx, int minGz, int maxGx, int maxGz) {
        int x1 = mapX + minGx * cellSize + padding / 2;
        int y1 = mapY + minGz * cellSize + padding / 2;
        int x2 = mapX + (maxGx + 1) * cellSize - padding / 2;
        int y2 = mapY + (maxGz + 1) * cellSize - padding / 2;
        RenderHelper.renderRoundedRect(graphics, x1, y1, x2 - x1, y2 - y1, cornerRadius, color);
    }

    private void drawRoomName(GuiGraphics graphics, String name, JsonArray places, int mapX, int mapY,
            int cellSize, int color) {
        if (places == null || places.isEmpty() || name == null)
            return;

        float targetX = 0;
        float targetZ = 0;

        int minGx = Integer.MAX_VALUE, maxGx = Integer.MIN_VALUE;
        int minGz = Integer.MAX_VALUE, maxGz = Integer.MIN_VALUE;
        for (JsonElement pElem : places) {
            JsonArray p = pElem.getAsJsonArray();
            int gx = p.get(0).getAsInt();
            int gz = p.get(1).getAsInt();
            if (gx < minGx)
                minGx = gx;
            if (gx > maxGx)
                maxGx = gx;
            if (gz < minGz)
                minGz = gz;
            if (gz > maxGz)
                maxGz = gz;
        }
        int bbArea = (maxGx - minGx + 1) * (maxGz - minGz + 1);
        boolean isLShape = places.size() == 3 && bbArea == 4 && (maxGx - minGx) == 1 && (maxGz - minGz) == 1;

        if (isLShape) {
            for (int i = 0; i < 3; i++) {
                JsonArray p1 = places.get(i).getAsJsonArray();
                int x1 = p1.get(0).getAsInt();
                int z1 = p1.get(1).getAsInt();
                int neighbors = 0;
                for (int j = 0; j < 3; j++) {
                    if (i == j)
                        continue;
                    JsonArray p2 = places.get(j).getAsJsonArray();
                    if (Math.abs(x1 - p2.get(0).getAsInt()) + Math.abs(z1 - p2.get(1).getAsInt()) == 1) {
                        neighbors++;
                    }
                }
                if (neighbors == 2) {
                    targetX = x1;
                    targetZ = z1;
                    break;
                }
            }
        } else {
            for (JsonElement pElem : places) {
                JsonArray p = pElem.getAsJsonArray();
                targetX += p.get(0).getAsInt();
                targetZ += p.get(1).getAsInt();
            }
            targetX /= places.size();
            targetZ /= places.size();
        }

        int cx = mapX + (int) (targetX * cellSize + cellSize / 2);
        int cz = mapY + (int) (targetZ * cellSize + cellSize / 2);

        float scale = ConfigManager.data.dungeonMapRoomNameScale * (cellSize / 32f);
        if (scale < 0.5f)
            scale = 0.5f;

        Minecraft mc = Minecraft.getInstance();
        int fontH = mc.font.lineHeight;

        List<String> lines = new ArrayList<>();
        int maxWidth = (int) (24 / scale);

        String[] explicitLines = name.split("\n");
        for (String eLine : explicitLines) {
            String[] words = eLine.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (sb.length() > 0 && mc.font.width(sb.toString() + " " + w) > maxWidth) {
                    lines.add(sb.toString());
                    sb = new StringBuilder(w);
                } else {
                    if (sb.length() > 0)
                        sb.append(" ");
                    sb.append(w);
                }
            }
            if (sb.length() > 0)
                lines.add(sb.toString());
        }

        float totalH = lines.size() * fontH * scale;
        float startY = cz - totalH / 2f;

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            float curY = startY + i * fontH * scale + (fontH * scale / 2f);

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) cx, curY);
            graphics.pose().scale(scale, scale);
            graphics.drawCenteredString(mc.font, line, 0, -(fontH / 2), color);
            graphics.pose().popMatrix();
        }
    }

    private int getRoomColor(String type, String state) {
        if ("UNDISCOVERED".equals(state))
            return ConfigManager.data.dungeonMapColorUndiscovered;

        switch (type) {
            case "ENTRANCE":
                return ConfigManager.data.dungeonMapColorEntrance;
            case "BLOOD":
                return ConfigManager.data.dungeonMapColorBlood;
            case "FAIRY":
                return ConfigManager.data.dungeonMapColorFairy;
            case "PUZZLE":
                return ConfigManager.data.dungeonMapColorPuzzle;
            case "TRAP":
                return ConfigManager.data.dungeonMapColorTrap;
            case "CHAMPION":
                return ConfigManager.data.dungeonMapColorChampion;
            case "MIMIC":
                return ConfigManager.data.dungeonMapColorMimic;
            default:
                return ConfigManager.data.dungeonMapColorNormal;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        if (pressed) {
            this.onClose();
        }
        return true;
    }
}
