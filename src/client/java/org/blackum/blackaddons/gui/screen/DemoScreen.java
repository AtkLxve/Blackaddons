package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;

public class DemoScreen extends BaseScreen {

        public DemoScreen() {
                super(Component.literal("GUI Demo"));
        }

        private void sendMessage(String message) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                        player.displayClientMessage(Component.literal("§b[GUI] §f" + message), false);
                }
        }

        @Override
        protected void initWidgets() {
                int contentX = containerX + 30;
                int contentY = containerY + 60;
                int contentWidth = containerWidth - 60;

                initGrid(contentX, contentY, contentWidth, 2, 40, 10);

                addToGrid(new Button(0, 0, 0, "Primary Button",
                                () -> sendMessage("Primary clicked!")), 1);

                addToGrid(new Button(0, 0, 0, "Secondary Button",
                                () -> sendMessage("Secondary clicked!")), 1);

                addToGrid(new TextField(0, 0, 0, 40, "Tall text field..."), 2);

                addToGrid(new Slider(0, 0, 0, 16, 0f, 100f, 50f,
                                value -> sendMessage("Slider value: " + String.format("%.1f", value))), 2);

                addToGrid(new Checkbox(0, 0, 32, "Custom Height Checkbox", false,
                                checked -> sendMessage("Checkbox: " + (checked ? "Enabled" : "Disabled"))), 1);

                addToGrid(new Checkbox(0, 0, "Default Checkbox", false,
                                checked -> sendMessage("Feature B: " + (checked ? "Enabled" : "Disabled"))), 1);

                addToGrid(new Dropdown(0, 0, 0, "Select Option",
                                java.util.Arrays.asList("Option 1", "Option 2", "Option 3"),
                                selected -> sendMessage("Selected: " + selected)), 2);

                addToGrid(new ColorPicker(0, 0,
                                color -> sendMessage("Color changed: "
                                                + org.blackum.blackaddons.gui.util.ColorUtils.toRGBA(color))),
                                1);

                currentGridRow += 4;
                currentGridColumn = 0;

                int currentGridBottom = gridStartY + (currentGridRow) * (gridRowHeight + gridGap) + gridRowHeight + 20;

                addWidget(new RadioButton(
                                contentX, currentGridBottom, "Option 1", "grp1", false,
                                selected -> {
                                        if (selected)
                                                sendMessage("Option 1 selected");
                                }));

                addWidget(new RadioButton(
                                contentX + 100, currentGridBottom, "Option 2", "grp1", false,
                                selected -> {
                                        if (selected)
                                                sendMessage("Option 2 selected");
                                }));

                addWidget(new RadioButton(
                                contentX + 200, currentGridBottom, "Option 3", "grp1", false,
                                selected -> {
                                        if (selected)
                                                sendMessage("Option 3 selected");
                                }));

                int controlY = currentGridBottom + 40;

                addWidget(new Button(
                                contentX, controlY, (contentWidth - 20) / 2, "Toggle Hitboxes",
                                () -> {
                                        BaseScreen.showHitboxes = !BaseScreen.showHitboxes;
                                        sendMessage("Hitboxes: " + BaseScreen.showHitboxes);
                                }));

                addWidget(new Button(
                                contentX + (contentWidth + 20) / 2, controlY, (contentWidth - 20) / 2,
                                "Toggle Overlay",
                                () -> {
                                        BaseScreen.showDebugOverlay = !BaseScreen.showDebugOverlay;
                                        sendMessage("Overlay: " + BaseScreen.showDebugOverlay);
                                }));

                addWidget(new Button(
                                contentX, controlY + 45, contentWidth,
                                "Open Overlay Editor",
                                () -> {
                                        if (!BaseScreen.showDebugOverlay) {
                                                BaseScreen.showDebugOverlay = true;
                                                sendMessage("Overlay auto-enabled for editing");
                                        }
                                        Minecraft.getInstance().setScreen(new OverlayEditorScreen(this));
                                }));

                int startY = controlY + 90;
                for (int i = 0; i < 10; i++) {
                        int yPos = startY + (i * 40);
                        final int idx = i + 1;
                        addWidget(new Button(contentX, yPos, 120, "Scroll Test Item " + idx,
                                        () -> sendMessage("Clicked Item " + idx)));
                }

                addWidget(new Button(
                                containerX + (containerWidth - 100) / 2, containerY + containerHeight, 100,
                                "Close",
                                () -> this.onClose()));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                int contentX = containerX + 30;
                int contentY = containerY + 60;

                String titleText = "BlackAddons GUI Control Panel";
                int titleWidth = this.font.width(titleText);
                graphics.drawString(this.font, titleText, containerX + (containerWidth - titleWidth) / 2,
                                containerY + 20, -1);

                graphics.drawString(this.font, "Interaction Tests:", contentX, contentY - 20, Theme.TEXT_SECONDARY);
                graphics.drawString(this.font, "Selection Controls:", contentX, contentY + 130, Theme.TEXT_SECONDARY);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
