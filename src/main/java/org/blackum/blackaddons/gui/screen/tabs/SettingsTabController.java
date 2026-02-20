package org.blackum.blackaddons.gui.screen.tabs;

import java.util.List;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

public class SettingsTabController extends SimpleTabController {

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
        Dropdown dataSourceDropdown = new Dropdown(contentX, currentY, Theme.DROPDOWN_WIDTH, Theme.BUTTON_HEIGHT,
                "Data Source",
                dataSources, (selected) -> {
                    ConfigManager.data.dataSource = ConfigManager.DataSource.valueOf(selected);
                    ConfigManager.save();
                });
        dataSourceDropdown.setSelectedOption(ConfigManager.data.dataSource.name());
        settingsTab.addWidget(dataSourceDropdown);
        currentY += Theme.BUTTON_HEIGHT + Theme.SPACING_NORMAL;

        settingsTab.addWidget(new Label(contentX, currentY, "Party Finder", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        ToggleSwitch pfAutoInviteToggle = new ToggleSwitch(contentX, currentY, Theme.DROPDOWN_WIDTH,
                "Auto-Invite Join Requests",
                "Automatically invite players who send a join request",
                ConfigManager.data.partyFinderAutoInvite, (val) -> {
                    ConfigManager.data.partyFinderAutoInvite = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(pfAutoInviteToggle);
        currentY += Theme.TOGGLE_HEIGHT + Theme.SPACING_NORMAL;

        ToggleSwitch pfAutoAcceptToggle = new ToggleSwitch(contentX, currentY, Theme.DROPDOWN_WIDTH,
                "Auto-Accept Party Invites",
                "Automatically accept party invites from join requests",
                ConfigManager.data.partyFinderAutoAcceptInvite, (val) -> {
                    ConfigManager.data.partyFinderAutoAcceptInvite = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(pfAutoAcceptToggle);
        currentY += Theme.TOGGLE_HEIGHT + Theme.SPACING_NORMAL;

        ToggleSwitch pfShowStatsJoinToggle = new ToggleSwitch(contentX, currentY, Theme.DROPDOWN_WIDTH,
                "Show Stats on Join",
                "Show player stats in chat when they join your dungeon group",
                ConfigManager.data.partyFinderShowStatsOnJoin, (val) -> {
                    ConfigManager.data.partyFinderShowStatsOnJoin = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(pfShowStatsJoinToggle);
        currentY += Theme.TOGGLE_HEIGHT + Theme.SPACING_NORMAL;

        ToggleSwitch pfShowStatsReqToggle = new ToggleSwitch(contentX, currentY, Theme.DROPDOWN_WIDTH,
                "Show Stats on Request",
                "Show player stats in chat when you receive a join request",
                ConfigManager.data.partyFinderShowStatsOnRequest, (val) -> {
                    ConfigManager.data.partyFinderShowStatsOnRequest = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(pfShowStatsReqToggle);
        currentY += Theme.TOGGLE_HEIGHT + Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "IRC Chat", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        ToggleSwitch ircEnabledToggle = new ToggleSwitch(contentX, currentY, Theme.DROPDOWN_WIDTH,
                "Enable IRC",
                "Enable the in-game IRC chat client",
                ConfigManager.data.ircEnabled, (val) -> {
                    ConfigManager.data.ircEnabled = val;
                    ConfigManager.save();
                    if (val) {
                        org.blackum.blackaddons.feature.chat.IrcClient.getInstance().connect();
                    } else {
                        org.blackum.blackaddons.feature.chat.IrcClient.getInstance().disconnect();
                    }
                });
        settingsTab.addWidget(ircEnabledToggle);
        currentY += Theme.TOGGLE_HEIGHT + Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "Profiles & Cache", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        settingsTab.addWidget(new Label(contentX, currentY, "Cache Duration", Label.Style.BODY));
        currentY += Theme.SPACING_NORMAL;

        List<String> cacheOptions = List.of("5 Minutes", "10 Minutes", "30 Minutes", "1 Hour");
        Dropdown cacheDropdown = new Dropdown(contentX, currentY, Theme.DROPDOWN_WIDTH, Theme.BUTTON_HEIGHT,
                "Cache Duration",
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
        currentY += Theme.COLOR_PICKER_HEIGHT + Theme.SPACING_NORMAL;

        ToggleSwitch layoutToggle = new ToggleSwitch(contentX, currentY, Theme.DROPDOWN_WIDTH, "Use Card Layout",
                "Enable card-based layout for various mod screens", ConfigManager.data.useCardLayout,
                (val) -> {
                    ConfigManager.data.useCardLayout = val;
                    ConfigManager.save();
                });
        settingsTab.addWidget(layoutToggle);
        currentY += Theme.TOGGLE_HEIGHT + Theme.SPACING_LARGE;

        settingsTab.addWidget(new Label(contentX, currentY, "Interface", Label.Style.TITLE));
        currentY += Theme.SPACING_NORMAL;

        Label durationLabel = new Label(contentX, currentY,
                "Notification Duration: " + ConfigManager.data.notificationDuration + "ms", Label.Style.BODY);
        settingsTab.addWidget(durationLabel);
        currentY += Theme.SPACING_NORMAL;

        Slider durationSlider = new Slider(contentX, currentY, Theme.DROPDOWN_WIDTH, 1000f, 10000f,
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

        TextField devKeyField = new TextField(contentX, currentY, Theme.DROPDOWN_WIDTH, Theme.TEXTFIELD_HEIGHT,
                "Developer Key");
        devKeyField.setText(ConfigManager.data.developerKey != null ? ConfigManager.data.developerKey : "");
        devKeyField.setMaxLength(128);
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
