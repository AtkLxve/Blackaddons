package org.blackum.blackaddons.gui.screen.tabs;

import java.util.List;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.util.*;

public class SettingsTabController extends SimpleTabController {

    private static final int DROPDOWN_WIDTH = 200;
    private static final int COLOR_PICKER_HEIGHT = 210;

    public SettingsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab settingsTab) {
        int contentX = settingsTab.getParent().getContentX();
        int contentY = settingsTab.getParent().getContentY();
        int currentY = contentY + Theme.PADDING_MEDIUM;

        settingsTab.addWidget(new Label(contentX, currentY, "General Settings", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        settingsTab.addWidget(new Label(contentX, currentY, "Data Source", Label.Style.BODY));
        currentY += Theme.SPACING_NORMAL;

        List<String> dataSources = List.of("BOT", "LOCAL");
        Dropdown dataSourceDropdown = new Dropdown(contentX, currentY, DROPDOWN_WIDTH, Theme.BUTTON_HEIGHT,
                "Data Source",
                dataSources, (selected) -> {
                    ConfigManager.data.dataSource = ConfigManager.DataSource.valueOf(selected);
                    ConfigManager.save();
                });
        dataSourceDropdown.setSelectedOption(ConfigManager.data.dataSource.name());
        settingsTab.addWidget(dataSourceDropdown);
        currentY += Theme.BUTTON_HEIGHT + Theme.SPACING_NORMAL;

        ToggleSwitch autoInviteToggle = new ToggleSwitch(contentX, currentY, DROPDOWN_WIDTH,
                "Auto-Invite Join Requests",
                "Automatically invite players who send a join request",
                ConfigManager.data.autoInvite, (val) -> {
                    ConfigManager.data.autoInvite = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(autoInviteToggle);
        currentY += Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "Profiles & Cache", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        settingsTab.addWidget(new Label(contentX, currentY, "Cache Duration", Label.Style.BODY));
        currentY += Theme.SPACING_NORMAL;

        List<String> cacheOptions = List.of("5 Minutes", "10 Minutes", "30 Minutes", "1 Hour");
        Dropdown cacheDropdown = new Dropdown(contentX, currentY, DROPDOWN_WIDTH, Theme.BUTTON_HEIGHT, "Cache Duration",
                cacheOptions, (selected) -> {
                    int minutes = 5;
                    if (selected.contains("5 Minutes"))
                        minutes = 5;
                    else if (selected.contains("10 Minutes"))
                        minutes = 10;
                    else if (selected.contains("30 Minutes"))
                        minutes = 30;
                    else if (selected.contains("1 Hour"))
                        minutes = 60;
                    ConfigManager.data.cacheDurationMinutes = minutes;
                    ConfigManager.save();
                });

        String currentCache = ConfigManager.data.cacheDurationMinutes + " Minutes";
        if (ConfigManager.data.cacheDurationMinutes == 60)
            currentCache = "1 Hour";
        cacheDropdown.setSelectedOption(currentCache);
        settingsTab.addWidget(cacheDropdown);
        currentY += Theme.BUTTON_HEIGHT + Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "Appearance", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        settingsTab.addWidget(new Label(contentX, currentY, "Accent Color", Label.Style.BODY));
        currentY += Theme.SPACING_NORMAL;

        ColorPicker accentPicker = new ColorPicker(contentX, currentY, (color) -> {
            ConfigManager.data.accentColor = color;
            Theme.ACCENT = color;
            ConfigManager.save();
        });
        settingsTab.addWidget(accentPicker);
        currentY += COLOR_PICKER_HEIGHT + Theme.SPACING_NORMAL;

        ToggleSwitch layoutToggle = new ToggleSwitch(contentX, currentY, DROPDOWN_WIDTH, "Use Card Layout",
                "Enable card-based layout for various mod screens", ConfigManager.data.useCardLayout,
                (val) -> {
                    ConfigManager.data.useCardLayout = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(layoutToggle);
        currentY += Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "Interface", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        Label durationLabel = new Label(contentX, currentY,
                "Notification Duration: " + ConfigManager.data.notificationDuration + "ms", Label.Style.BODY);
        settingsTab.addWidget(durationLabel);
        currentY += Theme.SPACING_NORMAL;

        Slider durationSlider = new Slider(contentX, currentY, DROPDOWN_WIDTH, 1000f, 10000f,
                ConfigManager.data.notificationDuration, (val) -> {
                    int duration = Math.round(val);
                    ConfigManager.data.notificationDuration = duration;
                    durationLabel.setText("Notification Duration: " + duration + "ms");
                    ConfigManager.save();
                });
        settingsTab.addWidget(durationSlider);
        currentY += Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "Developer", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        TextField devKeyField = new TextField(contentX, currentY, DROPDOWN_WIDTH, Theme.TEXTFIELD_HEIGHT,
                "Developer Key");
        devKeyField.setText(ConfigManager.data.developerKey != null ? ConfigManager.data.developerKey : "");
        settingsTab.addWidget(devKeyField);
        currentY += Theme.TEXTFIELD_HEIGHT + Theme.SPACING_SMALL;

        Button saveKeyBtn = new Button(contentX, currentY, 100, Theme.BUTTON_HEIGHT, "Save Key", () -> {
            ConfigManager.data.developerKey = devKeyField.getText();
            ConfigManager.save();
            NotificationManager.addNotification("Config", "Developer key saved.", NotificationType.SUCCESS);
        });
        settingsTab.addWidget(saveKeyBtn);
    }
}
