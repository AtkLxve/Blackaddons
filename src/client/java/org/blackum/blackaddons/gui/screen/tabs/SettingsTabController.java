package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;

import java.util.List;

public class SettingsTabController extends SimpleTabController {

    public SettingsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab settingsTab) {
        int contentX = settingsTab.getParent().getContentX();
        int contentY = settingsTab.getParent().getContentY();

        settingsTab.addWidget(new Label(contentX, contentY, "Data Source", Label.Style.TITLE));

        List<String> dataSources = List.of("LOCAL", "BOT");
        Dropdown dataSourceDropdown = new Dropdown(contentX, contentY + 30, 200, "Data Source", dataSources,
                selected -> {
                    try {
                        ConfigManager.data.dataSource = ConfigManager.DataSource.valueOf(selected);
                        ConfigManager.save();
                    } catch (Exception e) {
                        ConfigManager.data.dataSource = ConfigManager.DataSource.LOCAL;
                    }
                });
        dataSourceDropdown.setSelectedOption(ConfigManager.data.dataSource.name());
        settingsTab.addWidget(dataSourceDropdown);

        settingsTab.addWidget(new Label(contentX, contentY + 80, "Developer Key", Label.Style.TITLE));
        TextField devKeyField = new TextField(contentX, contentY + 110, 200, "Enter key...");
        devKeyField.setText(ConfigManager.data.developerKey);
        settingsTab.addWidget(devKeyField);

        Button saveKeyBtn = new Button(contentX + 210, contentY + 110, 60, "Save", () -> {
            ConfigManager.data.developerKey = devKeyField.getText();
            ConfigManager.save();
        });
        settingsTab.addWidget(saveKeyBtn);

        int offsetY = 160;

        settingsTab.addWidget(new Label(contentX, contentY + offsetY, "App Appearance", Label.Style.TITLE));

        settingsTab
                .addWidget(new Label(contentX, contentY + offsetY + 30, "Accent Color (Main Theme)", Label.Style.BODY));

        ColorPicker accentPicker = new ColorPicker(contentX, contentY + offsetY + 50, color -> Theme.ACCENT = color);

        settingsTab.addWidget(accentPicker);

        ToggleSwitch layoutToggle = new ToggleSwitch(contentX, contentY + offsetY + 280, 400,
                "Use Card Layout",
                "Enable resizable card-based layout for Mod Hider",
                ConfigManager.data.useCardLayout, value -> {
                    ConfigManager.data.useCardLayout = value;
                    ConfigManager.save();
                    screen.init();
                });
        settingsTab.addWidget(layoutToggle);

        Label durationLabel = new Label(contentX, contentY + offsetY + 320,
                "Notification Duration: " + ConfigManager.data.notificationDuration + "ms", Label.Style.BODY);
        settingsTab.addWidget(durationLabel);

        Slider durationSlider = new Slider(contentX, contentY + offsetY + 330,
                settingsTab.getParent().getContentWidth() - 20, 500f,
                10000f,
                ConfigManager.data.notificationDuration, val -> {
                    int duration = Math.round(val);
                    if (duration != ConfigManager.data.notificationDuration) {
                        ConfigManager.data.notificationDuration = duration;
                        durationLabel.setText("Notification Duration: " + duration + "ms");
                        ConfigManager.save();
                    }
                });
        settingsTab.addWidget(durationSlider);

        settingsTab
                .addWidget(new Label(contentX, contentY + offsetY + 390, "Profile Cache Duration", Label.Style.BODY));

        List<String> cacheOptions = List.of("1 Minute", "5 Minutes", "10 Minutes", "30 Minutes",
                "1 Hour");
        Dropdown cacheDropdown = new Dropdown(contentX, contentY + offsetY + 410,
                settingsTab.getParent().getContentWidth() - 20, 20,
                "Cache Duration", cacheOptions, selected -> {
                    int minutes = 5;
                    if (selected.contains("1 Minute"))
                        minutes = 1;
                    else if (selected.contains("5 Minutes"))
                        minutes = 5;
                    else if (selected.contains("10 Minutes"))
                        minutes = 10;
                    else if (selected.contains("30 Minutes"))
                        minutes = 30;
                    else if (selected.contains("1 Hour"))
                        minutes = 60;

                    if (ConfigManager.data.cacheDurationMinutes != minutes) {
                        ConfigManager.data.cacheDurationMinutes = minutes;
                        ConfigManager.save();
                    }
                });

        String currentOption = ConfigManager.data.cacheDurationMinutes + " Minutes";
        if (ConfigManager.data.cacheDurationMinutes == 1)
            currentOption = "1 Minute";
        else if (ConfigManager.data.cacheDurationMinutes == 60)
            currentOption = "1 Hour";

        cacheDropdown.setSelectedOption(currentOption);
        settingsTab.addWidget(cacheDropdown);
    }
}
