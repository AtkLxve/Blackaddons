package org.blackum.blackaddons.gui.screen;

import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.ColorPicker;
import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.widget.Widget;

public class BlackAddonsGUI extends BaseScreen {

    private TabPanel tabPanel;

    public BlackAddonsGUI() {
        this(null);
    }

    public BlackAddonsGUI(net.minecraft.client.gui.screens.Screen parent) {
        super(Component.literal("BlackAddons Settings"), parent);
    }

    @Override
    protected void initWidgets() {
        tabPanel = new TabPanel(containerX, containerY + 40, containerWidth, containerHeight - 40);
        addWidget(tabPanel);

        initSettingsTab();
        initAboutTab();
    }

    private void initSettingsTab() {
        TabPanel.Tab settingsTab = tabPanel.addTab("Settings");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        settingsTab.addWidget(new Label(contentX, contentY, "App Appearance", Label.Style.TITLE));

        settingsTab.addWidget(new Label(contentX, contentY + 30, "Accent Color (Main Theme)", Label.Style.BODY));

        ColorPicker accentPicker = new ColorPicker(contentX, contentY + 50, color -> {
            Theme.ACCENT = color;
        });

        settingsTab.addWidget(accentPicker);
    }

    private void initAboutTab() {
        TabPanel.Tab aboutTab = tabPanel.addTab("About");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();

        aboutTab.addWidget(new Label(contentX, contentY, "BlackAddons", Label.Style.TITLE));
        aboutTab.addWidget(new Label(contentX, contentY + 30, "Version: 1.0.0", Label.Style.BODY));
        aboutTab.addWidget(new Label(contentX, contentY + 50, "Created by Blackum", Label.Style.BODY));
    }

    @Override
    protected void renderScrolledContent(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        String title = "BlackAddons Control Panel";
        int titleWidth = font.width(title);
        graphics.drawString(font, title, containerX + (containerWidth - titleWidth) / 2, containerY + 15,
                Theme.TEXT_PRIMARY);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }
}
