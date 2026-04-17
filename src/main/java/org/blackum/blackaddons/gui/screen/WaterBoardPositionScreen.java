package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.common.config.ConfigManager;

public class WaterBoardPositionScreen extends Screen {
    private static final int BORDER_COLOR = 0xFF4A4A4A;
    private static final int BG_COLOR = 0xC0202020;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final Screen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int hudX;
    private int hudY;
    private float hudScale;

    public WaterBoardPositionScreen(Screen parent) {
        super(Component.literal("Water Board Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.hudX = ConfigManager.data.waterBoardHudX < 0 ? this.width / 2 + 15 : ConfigManager.data.waterBoardHudX;
        this.hudY = ConfigManager.data.waterBoardHudY < 0 ? this.height / 2 - 20 : ConfigManager.data.waterBoardHudY;
        this.hudScale = ConfigManager.data.waterBoardHudScale <= 0 ? 1.0f : ConfigManager.data.waterBoardHudScale;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        graphics.drawCenteredString(this.font, "Drag the water board timers to reposition them.", this.width / 2, 20, HINT_COLOR);
        graphics.drawCenteredString(this.font, "Scroll to scale. Press Esc to save.", this.width / 2, 32, HINT_COLOR);

        int boxWidth = 100;
        int boxHeight = 40;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) this.hudX, (float) this.hudY);
        graphics.pose().scale(this.hudScale, this.hudScale);

        // Preview box
        graphics.fill(0, -12, boxWidth, boxHeight, BG_COLOR);
        // Border
        graphics.fill(0, -12, boxWidth, -11, BORDER_COLOR);
        graphics.fill(0, boxHeight - 1, boxWidth, boxHeight, BORDER_COLOR);
        graphics.fill(0, -12, 1, boxHeight, BORDER_COLOR);
        graphics.fill(boxWidth - 1, -12, boxWidth, boxHeight, BORDER_COLOR);

        graphics.drawString(this.font, "§b§lWater Board", 4, -8, 0xFFFFFFFF);
        graphics.drawString(this.font, "§9WATER: §f0.0s", 4, 4, HINT_COLOR);
        graphics.drawString(this.font, "§6GOLD: §f2.5s", 4, 14, HINT_COLOR);

        graphics.pose().popMatrix();
    }

    private boolean isOverHud(double mx, double my) {
        float scaledWidth = 100 * hudScale;
        float scaledHeight = 52 * hudScale;
        float scaledYStart = (float) hudY - (12 * hudScale);
        return mx >= hudX && mx <= hudX + scaledWidth && my >= scaledYStart && my <= hudY + (40 * hudScale);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
        double mouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
        if (event.button() == 0 && isOverHud(mouseX, mouseY)) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.hudX;
            this.dragOffsetY = mouseY - this.hudY;
            return true;
        }
        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            Minecraft mc = Minecraft.getInstance();
            double mouseX = mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
            double mouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
            this.hudX = (int) (mouseX - this.dragOffsetX);
            this.hudY = (int) (mouseY - this.dragOffsetY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOverHud(mouseX, mouseY)) {
            this.hudScale = Math.max(0.2f, Math.min(5.0f, this.hudScale + (float) verticalAmount * 0.1f));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        ConfigManager.data.waterBoardHudX = this.hudX;
        ConfigManager.data.waterBoardHudY = this.hudY;
        ConfigManager.data.waterBoardHudScale = this.hudScale;
        ConfigManager.save();
        Minecraft.getInstance().setScreen(this.parent);
    }
}
