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
                if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(Component.literal("§b[GUI] §f" + message),
                                        false);
                }
        }

        @Override
        protected void initWidgets() {
                int contentX = containerX + 30;
                int contentY = containerY + 60;
                int contentWidth = containerWidth - 60;

                addWidget(new Button(
                                contentX, contentY, (contentWidth - 20) / 2, "Primary Button",
                                () -> sendMessage("Primary clicked!")));

                addWidget(new Button(
                                contentX + (contentWidth + 20) / 2, contentY, (contentWidth - 20) / 2,
                                "Secondary Button",
                                () -> sendMessage("Secondary clicked!")));

                addWidget(new TextField(
                                contentX, contentY + 50, contentWidth, "Enter text here..."));

                addWidget(new Slider(
                                contentX, contentY + 100, contentWidth, 0f, 100f, 50f,
                                value -> sendMessage("Slider value: " + String.format("%.1f", value))));

                addWidget(new Checkbox(
                                contentX, contentY + 150, "Enable feature A", false,
                                checked -> sendMessage("Feature A: " + (checked ? "Enabled" : "Disabled"))));

                addWidget(new Checkbox(
                                contentX + 150, contentY + 150, "Enable feature B", false,
                                checked -> sendMessage("Feature B: " + (checked ? "Enabled" : "Disabled"))));

                int radioY = contentY + 190;
                addWidget(new RadioButton(
                                contentX, radioY, "Option 1", "grp1", false,
                                selected -> {
                                        if (selected)
                                                sendMessage("Option 1 selected");
                                }));

                addWidget(new RadioButton(
                                contentX + 100, radioY, "Option 2", "grp1", false,
                                selected -> {
                                        if (selected)
                                                sendMessage("Option 2 selected");
                                }));

                addWidget(new RadioButton(
                                contentX + 200, radioY, "Option 3", "grp1", false,
                                selected -> {
                                        if (selected)
                                                sendMessage("Option 3 selected");
                                }));

                addWidget(new Button(
                                containerX + (containerWidth - 100) / 2, containerY + containerHeight - 40, 100,
                                "Close",
                                () -> this.onClose()));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.render(graphics, mouseX, mouseY, partialTick);

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
