package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.common.config.ConfigManager;

public class DungeonMapPositionScreen extends Screen {
    private static final int BORDER_COLOR = 0xFF4A5568;
    private static final int BG_COLOR = 0xC0111827;
    private static final int HINT_COLOR = 0xFFAAAAAA;
    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int RESIZE_HANDLE_COLOR = 0xC0FFFFFF;

    private final Screen parent;
    private boolean dragging;
    private boolean resizing;
    private double dragOffsetX;
    private double dragOffsetY;
    private int mapX;
    private int mapY;
    private int mapSize;

    public DungeonMapPositionScreen(Screen parent) {
        super(Component.literal("Dungeon Map Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        mapX = ConfigManager.data.dungeonMapX;
        mapY = ConfigManager.data.dungeonMapY;
        mapSize = ConfigManager.data.dungeonMapSize;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.drawCenteredString(font, "Drag the dungeon map to reposition it.", this.width / 2, this.height / 2, HINT_COLOR);
        g.drawCenteredString(font, "Drag the corner handle or scroll to resize. Press Esc to save.", this.width / 2, this.height / 2 + 12, HINT_COLOR);

        g.fill(mapX, mapY, mapX + mapSize, mapY + mapSize, BG_COLOR);
        g.fill(mapX, mapY, mapX + mapSize, mapY + 1, BORDER_COLOR);
        g.fill(mapX, mapY + mapSize - 1, mapX + mapSize, mapY + mapSize, BORDER_COLOR);
        g.fill(mapX, mapY, mapX + 1, mapY + mapSize, BORDER_COLOR);
        g.fill(mapX + mapSize - 1, mapY, mapX + mapSize, mapY + mapSize, BORDER_COLOR);

        g.drawCenteredString(font, "Dungeon Map", mapX + mapSize / 2, mapY + mapSize / 2 - 4, 0xFF888888);
        g.drawCenteredString(font, "(Preview)", mapX + mapSize / 2, mapY + mapSize / 2 + 6, 0xFF666666);

        int hx = mapX + mapSize - RESIZE_HANDLE_SIZE;
        int hy = mapY + mapSize - RESIZE_HANDLE_SIZE;
        g.fill(hx, hy, mapX + mapSize, mapY + mapSize, RESIZE_HANDLE_COLOR);
    }

    private double getScaledMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
    }

    private double getScaledMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
    }

    private boolean isOverResizeHandle(double mx, double my) {
        int hx = mapX + mapSize - RESIZE_HANDLE_SIZE;
        int hy = mapY + mapSize - RESIZE_HANDLE_SIZE;
        return mx >= hx && mx <= mapX + mapSize && my >= hy && my <= mapY + mapSize;
    }

    private boolean isOverMap(double mx, double my) {
        return mx >= mapX && mx <= mapX + mapSize && my >= mapY && my <= mapY + mapSize;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        double mx = getScaledMouseX();
        double my = getScaledMouseY();
        if (event.button() == 0) {
            if (isOverResizeHandle(mx, my)) {
                resizing = true;
                return true;
            } else if (isOverMap(mx, my)) {
                dragging = true;
                dragOffsetX = mx - mapX;
                dragOffsetY = my - mapY;
                return true;
            }
        }
        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        resizing = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (resizing) {
            double mx = getScaledMouseX();
            double my = getScaledMouseY();
            mapSize = (int) Math.max(32, Math.min(400, Math.max(mx - mapX, my - mapY)));
            return true;
        }
        if (dragging) {
            double mx = getScaledMouseX();
            double my = getScaledMouseY();
            mapX = (int) Math.max(0, Math.min(this.width - mapSize, mx - dragOffsetX));
            mapY = (int) Math.max(0, Math.min(this.height - mapSize, my - dragOffsetY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOverMap(mouseX, mouseY)) {
            mapSize = (int) Math.max(32, Math.min(400, (int)(mapSize + verticalAmount * 5)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        ConfigManager.data.dungeonMapX = mapX;
        ConfigManager.data.dungeonMapY = mapY;
        ConfigManager.data.dungeonMapSize = mapSize;
        ConfigManager.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
