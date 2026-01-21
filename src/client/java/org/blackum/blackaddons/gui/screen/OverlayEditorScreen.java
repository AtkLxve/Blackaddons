package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.Button;

public class OverlayEditorScreen extends BaseScreen {

    private boolean isDragging = false;
    private boolean isResizing = false;
    private int dragOffsetX, dragOffsetY;

    private final net.minecraft.client.gui.screens.Screen parent;

    public OverlayEditorScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(Component.literal("Overlay Editor"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        org.blackum.blackaddons.config.ConfigManager.save();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xAA000000, 0xAA000000);

        renderOverlayPreview(graphics);

        float scale = BaseScreen.overlayScale;
        int x = BaseScreen.overlayX;
        int y = BaseScreen.overlayY;

        int boxWidth = (int) (150 * scale);
        int boxHeight = (int) (50 * scale);

        graphics.renderOutline(x - 2, y - 2, boxWidth + 4, boxHeight + 4, 0xFF00FF00);

        int handleSize = 8;
        graphics.fill(x + boxWidth - handleSize + 2, y + boxHeight - handleSize + 2, x + boxWidth + 2,
                y + boxHeight + 2, 0xFFFFFFFF);

        for (org.blackum.blackaddons.gui.widget.Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.updateHoverState(mouseX, mouseY);
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        graphics.drawCenteredString(this.font, "Drag to Move | Drag Handle to Resize", this.width / 2, 10, 0xFFFFFFFF);
    }

    private void renderOverlayPreview(GuiGraphics graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) BaseScreen.overlayX, (float) BaseScreen.overlayY);
        graphics.pose().scale(BaseScreen.overlayScale, BaseScreen.overlayScale);

        int x = 0;
        int y = 0;

        java.util.List<String> debugInfo = new java.util.ArrayList<>();
        debugInfo.add("§6[BlackAddons Debug]");
        debugInfo.add("VSync: " + this.minecraft.options.enableVsync().get());
        debugInfo.add("Mouse: " + (int) this.minecraft.mouseHandler.xpos() + ", "
                + (int) this.minecraft.mouseHandler.ypos());
        debugInfo.add("Screen: OverlayEditorScreen");

        for (String line : debugInfo) {
            graphics.drawString(this.font, line, x, y, 0xFFFFFFFF);
            y += 10;
        }

        graphics.pose().popMatrix();
    }

    @Override
    protected void init() {
        super.init();
        this.containerX = 0;
        this.containerY = 0;
        this.containerWidth = this.width;
        this.containerHeight = this.height;
        this.widgets.clear();
        initWidgets();
    }

    @Override
    protected void initWidgets() {
        int buttonWidth = 100;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - Theme.BUTTON_HEIGHT - Theme.MARGIN;

        addWidget(new Button(buttonX, buttonY, buttonWidth, "Done", () -> {
            this.onClose();
        }));
    }

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 10.0f;

    private void setOverlayScale(double newScale) {
        if (newScale < MIN_SCALE)
            newScale = MIN_SCALE;
        if (newScale > MAX_SCALE)
            newScale = MAX_SCALE;
        BaseScreen.overlayScale = (float) newScale;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float scale = BaseScreen.overlayScale;
        int x = BaseScreen.overlayX;
        int y = BaseScreen.overlayY;
        int boxWidth = (int) (150 * scale);
        int boxHeight = (int) (50 * scale);

        if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
            setOverlayScale(scale + (scrollY * 0.1));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        if (event.button() == 0) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

            double windowWidth = mc.getWindow().getScreenWidth();
            double windowHeight = mc.getWindow().getScreenHeight();
            double scaledWidth = this.width;
            double scaledHeight = this.height;

            double mouseX = mc.mouseHandler.xpos() * (scaledWidth / windowWidth);
            double mouseY = mc.mouseHandler.ypos() * (scaledHeight / windowHeight);

            float scale = BaseScreen.overlayScale;
            int x = BaseScreen.overlayX;
            int y = BaseScreen.overlayY;
            int boxWidth = (int) (150 * scale);
            int boxHeight = (int) (50 * scale);

            if (mouseX >= x + boxWidth - 10 && mouseX <= x + boxWidth + 5 &&
                    mouseY >= y + boxHeight - 10 && mouseY <= y + boxHeight + 5) {
                isResizing = true;
                return true;
            }

            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - x);
                dragOffsetY = (int) (mouseY - y);
                return true;
            }
        }
        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDragging = false;
        isResizing = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging || isResizing) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            double windowWidth = mc.getWindow().getScreenWidth();
            double scaledWidth = this.width;
            double mouseX = mc.mouseHandler.xpos() * (scaledWidth / windowWidth);
            double mouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());

            if (isDragging) {
                BaseScreen.overlayX = (int) (mouseX - dragOffsetX);
                BaseScreen.overlayY = (int) (mouseY - dragOffsetY);
                return true;
            }

            if (isResizing) {
                double dx = mouseX - BaseScreen.overlayX;
                double newScale = dx / 150.0;
                setOverlayScale(newScale);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }
}
