package org.blackum.blackaddons.gui.screen;

import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.cheats.CheatsOptions;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.blackum.blackaddons.general.GeneralOptions;

public class BlackAddonsGUI extends BaseScreen {

    private TabPanel tabPanel;
    private static int lastTabIndex = 0;
    private String currentTooltip = null;
    private final java.util.Map<String, Boolean> collapsedGroups = new java.util.HashMap<>();

    private ResizableCard spoofModeCard;
    private ResizableCard customClientCard;
    private ResizableCard hideModsCard;
    private ResizableCard disablePayloadsCard;
    private ResizableCard allowedChannelsCard;
    private ResizableCard allowedModsCard;
    private ResizableCard autoTntCard;

    private ResizableCard fullbrightCard;

    public BlackAddonsGUI() {
        this(null);
    }

    public BlackAddonsGUI(net.minecraft.client.gui.screens.Screen parent) {
        super(Component.literal("BlackAddons Settings"), parent);
    }

    @Override
    protected void initWidgets() {
        tabPanel = new TabPanel(containerX, containerY + 40, containerWidth, containerHeight - 40);
        tabPanel.setOnTabChange(index -> lastTabIndex = index);
        addWidget(tabPanel);

        initSettingsTab();
        initModHiderTab();
        initCheatsTab();
        initLegitTab();

        initAboutTab();

        tabPanel.selectTab(lastTabIndex);

        int tabContentHeight = tabPanel.getMaxContentHeight();
        this.contentHeight = Math.max(this.contentHeight, tabContentHeight + 40);
    }

    @Override
    public void tick() {
        super.tick();
        if (tabPanel != null) {
            int tabContentHeight = tabPanel.getMaxContentHeight();
            this.contentHeight = Math.max(this.contentHeight, tabContentHeight + 40);
        }
        updateCardVisibility();
    }

    private void initSettingsTab() {
        TabPanel.Tab settingsTab = tabPanel.addTab("Settings");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();

        settingsTab.addWidget(new Label(contentX, contentY, "Data Source", Label.Style.TITLE));

        java.util.List<String> dataSources = java.util.List.of("LOCAL", "BOT");
        Dropdown dataSourceDropdown = new Dropdown(contentX, contentY + 30, 200, "Data Source", dataSources,
                selected -> {
                    try {
                        ConfigManager.dataSource = ConfigManager.DataSource.valueOf(selected);
                        ConfigManager.save();
                    } catch (Exception e) {
                        ConfigManager.dataSource = ConfigManager.DataSource.LOCAL;
                    }
                });
        dataSourceDropdown.setSelectedOption(ConfigManager.dataSource.name());
        settingsTab.addWidget(dataSourceDropdown);

        int offsetY = 80;

        settingsTab.addWidget(new Label(contentX, contentY + offsetY, "App Appearance", Label.Style.TITLE));

        settingsTab
                .addWidget(new Label(contentX, contentY + offsetY + 30, "Accent Color (Main Theme)", Label.Style.BODY));

        ColorPicker accentPicker = new ColorPicker(contentX, contentY + offsetY + 50, color -> Theme.ACCENT = color);

        settingsTab.addWidget(accentPicker);

        ToggleSwitch layoutToggle = new ToggleSwitch(contentX, contentY + offsetY + 280, 400,
                "Use Card Layout",
                "Enable resizable card-based layout for Mod Hider",
                ConfigManager.useCardLayout, value -> {
                    ConfigManager.useCardLayout = value;
                    ConfigManager.save();
                    this.init(this.width, this.height);
                });
        settingsTab.addWidget(layoutToggle);

        Label durationLabel = new Label(contentX, contentY + offsetY + 320,
                "Notification Duration: " + GeneralOptions.NOTIFICATION_DURATION + "ms", Label.Style.BODY);
        settingsTab.addWidget(durationLabel);

        Slider durationSlider = new Slider(contentX, contentY + offsetY + 330, tabPanel.getContentWidth() - 20, 500f,
                10000f,
                GeneralOptions.NOTIFICATION_DURATION, val -> {
                    int duration = Math.round(val);
                    if (duration != GeneralOptions.NOTIFICATION_DURATION) {
                        GeneralOptions.NOTIFICATION_DURATION = duration;
                        durationLabel.setText("Notification Duration: " + duration + "ms");
                        ConfigManager.save();
                    }
                });
        settingsTab.addWidget(durationSlider);

        settingsTab
                .addWidget(new Label(contentX, contentY + offsetY + 390, "Profile Cache Duration", Label.Style.BODY));

        java.util.List<String> cacheOptions = java.util.List.of("1 Minute", "5 Minutes", "10 Minutes", "30 Minutes",
                "1 Hour");
        Dropdown cacheDropdown = new Dropdown(contentX, contentY + offsetY + 410, tabPanel.getContentWidth() - 20, 20,
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

                    if (GeneralOptions.CACHE_DURATION_MINUTES != minutes) {
                        GeneralOptions.CACHE_DURATION_MINUTES = minutes;
                        ConfigManager.save();
                    }
                });

        String currentOption = GeneralOptions.CACHE_DURATION_MINUTES + " Minutes";
        if (GeneralOptions.CACHE_DURATION_MINUTES == 1)
            currentOption = "1 Minute";
        else if (GeneralOptions.CACHE_DURATION_MINUTES == 60)
            currentOption = "1 Hour";

        cacheDropdown.setSelectedOption(currentOption);
        settingsTab.addWidget(cacheDropdown);
    }

    private void initAboutTab() {
        TabPanel.Tab aboutTab = tabPanel.addTab("About");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();

        aboutTab.addWidget(new Label(contentX, contentY, "BlackAddons", Label.Style.TITLE));

        String version = "Unknown";
        try {
            java.util.Optional<net.fabricmc.loader.api.ModContainer> mod = net.fabricmc.loader.api.FabricLoader
                    .getInstance().getModContainer("blackaddons");
            if (mod.isPresent()) {
                version = mod.get().getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        aboutTab.addWidget(new Label(contentX, contentY + 30, "Version: " + version, Label.Style.BODY));
        aboutTab.addWidget(new Label(contentX, contentY + 50, "Created by Blackum", Label.Style.BODY));
    }

    private void initModHiderTab() {
        if (!ConfigManager.useCardLayout) {
            initModHiderTabLegacy();
            return;
        }

        TabPanel.Tab modHiderTab = tabPanel.addTab("Mod Hider");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            resetCardStates("spoofMode", "customClient", "hideMods", "disablePayloads", "allowedChannels",
                    "allowedMods");
        });
        modHiderTab.addWidget(resetLayout);

        CardContainer modHiderCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        modHiderTab.addWidget(modHiderCardContainer);

        int containerY = contentY + 30;
        boolean isSingleColumn = contentWidth < 680;
        int col1X = contentX + 20;
        int col2X = contentX + 340;

        if (isSingleColumn) {
            spoofModeCard = createSpoofModeCard(col1X, containerY + 20);
            int currentY = containerY + 20 + spoofModeCard.getHeight() + Theme.CARD_SPACING;

            customClientCard = createCustomClientCard(col1X, currentY);
            currentY += customClientCard.getHeight() + Theme.CARD_SPACING;

            hideModsCard = createHideModsCard(col1X, currentY);
            currentY += hideModsCard.getHeight() + Theme.CARD_SPACING;

            disablePayloadsCard = createDisablePayloadsCard(col1X, currentY);
            currentY += disablePayloadsCard.getHeight() + Theme.CARD_SPACING;

            allowedChannelsCard = createAllowedChannelsCard(col1X, currentY);
            currentY += allowedChannelsCard.getHeight() + Theme.CARD_SPACING;

            allowedModsCard = createAllowedModsCard(col1X, currentY);
        } else {
            int currentY1 = containerY + 20;
            int currentY2 = containerY + 20;

            spoofModeCard = createSpoofModeCard(col1X, currentY1);
            currentY1 += spoofModeCard.getHeight() + Theme.CARD_SPACING;

            customClientCard = createCustomClientCard(col1X, currentY1);
            currentY1 += customClientCard.getHeight() + Theme.CARD_SPACING;

            allowedChannelsCard = createAllowedChannelsCard(col1X, currentY1);

            hideModsCard = createHideModsCard(col2X, currentY2);
            currentY2 += hideModsCard.getHeight() + Theme.CARD_SPACING;

            disablePayloadsCard = createDisablePayloadsCard(col2X, currentY2);
            currentY2 += disablePayloadsCard.getHeight() + Theme.CARD_SPACING;

            allowedModsCard = createAllowedModsCard(col2X, currentY2);
        }

        modHiderCardContainer.addCard(spoofModeCard);
        modHiderCardContainer.addCard(customClientCard);
        modHiderCardContainer.addCard(hideModsCard);
        modHiderCardContainer.addCard(disablePayloadsCard);
        modHiderCardContainer.addCard(allowedChannelsCard);
        modHiderCardContainer.addCard(allowedModsCard);
    }

    private void initModHiderTabLegacy() {
        TabPanel.Tab modHiderTab = tabPanel.addTab("Mod Hider");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth() - 20;
        int currentY = contentY;

        SpoofMode mode = ModHiderOptions.SPOOF_MODE;
        boolean isCustom = mode == SpoofMode.CUSTOM;
        boolean isModdedOrCustom = mode == SpoofMode.MODDED || mode == SpoofMode.CUSTOM;

        Label spoofLabel = new Label(contentX, currentY, "Spoof Mode", Label.Style.TITLE);
        modHiderTab.addWidget(spoofLabel);
        currentY += 25;

        List<String> spoofModes = List.of("VANILLA", "MODDED", "CUSTOM", "OFF");
        Dropdown spoofModeDropdown = new Dropdown(contentX, currentY, contentWidth,
                "Spoof Mode", spoofModes, selected -> {
                    try {
                        ModHiderOptions.SPOOF_MODE = SpoofMode.valueOf(selected.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        ModHiderOptions.SPOOF_MODE = SpoofMode.VANILLA;
                    }
                    ConfigManager.save();
                    this.init(this.width, this.height);
                });
        spoofModeDropdown.setSelectedOption(ModHiderOptions.SPOOF_MODE.name());
        modHiderTab.addWidget(spoofModeDropdown);
        currentY += 45;

        if (isCustom) {
            Label customClientLabel = new Label(contentX, currentY, "Custom Client Brand", Label.Style.TITLE);
            modHiderTab.addWidget(customClientLabel);
            currentY += 25;

            TextField customClient = new TextField(contentX, currentY, contentWidth - 90, "fabric");
            customClient.setText(ModHiderOptions.CUSTOM_CLIENT == null ? "fabric" : ModHiderOptions.CUSTOM_CLIENT);
            modHiderTab.addWidget(customClient);

            Button applyCustomClient = new Button(contentX + contentWidth - 80, currentY, 70, "Apply", () -> {
                ModHiderOptions.CUSTOM_CLIENT = customClient.getText().isBlank() ? "fabric" : customClient.getText();
                ConfigManager.save();
            });
            modHiderTab.addWidget(applyCustomClient);
            currentY += 45;

            ToggleSwitch hideModsToggle = new ToggleSwitch(contentX, currentY, contentWidth,
                    "Hide Mods",
                    "Prevent servers from reading mod info",
                    ModHiderOptions.HIDE_MODS, value -> {
                        ModHiderOptions.HIDE_MODS = value;
                        ConfigManager.save();
                    });
            modHiderTab.addWidget(hideModsToggle);
            currentY += 50;

            ToggleSwitch disablePayloadsToggle = new ToggleSwitch(contentX, currentY, contentWidth,
                    "Disable Custom Payloads",
                    "Block custom payload channels unless allowed",
                    ModHiderOptions.DISABLE_CUSTOM_PAYLOADS, value -> {
                        ModHiderOptions.DISABLE_CUSTOM_PAYLOADS = value;
                        ConfigManager.save();
                    });
            modHiderTab.addWidget(disablePayloadsToggle);
            currentY += 50;
        }

        ListView channelsList = null;
        ListView allowedModsList = null;
        TextField modSearch = null;

        if (isCustom) {
            Label channelsLabel = new Label(contentX, currentY, "Allowed Payload Channels", Label.Style.TITLE);
            modHiderTab.addWidget(channelsLabel);
            currentY += 25;

            TextField channelField = new TextField(contentX, currentY, contentWidth - 100,
                    "example: minecraft:register");
            modHiderTab.addWidget(channelField);

            ListView finalChannelsList = new ListView(contentX, currentY + 40, contentWidth, 100);
            channelsList = finalChannelsList;

            Button addChannel = new Button(contentX + contentWidth - 90, currentY, 80, "Add", () -> {
                String val = channelField.getText() == null ? "" : channelField.getText().trim();
                if (!val.isBlank()) {
                    ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.add(val);
                    channelField.setText("");
                    ConfigManager.save();
                    rebuildChannelsList(finalChannelsList);
                }
            });
            modHiderTab.addWidget(addChannel);
            currentY += 40;

            modHiderTab.addWidget(finalChannelsList);
            currentY += 110;
        }

        if (isModdedOrCustom) {
            Label modsLabel = new Label(contentX, currentY, "Allowed Mods", Label.Style.TITLE);
            modHiderTab.addWidget(modsLabel);
            currentY += 25;

            modSearch = new TextField(contentX, currentY, contentWidth, "Search mods...");
            modHiderTab.addWidget(modSearch);
            currentY += 40;

            allowedModsList = new ListView(contentX, currentY, contentWidth, 200);
            modHiderTab.addWidget(allowedModsList);
        }

        setupLegacyAutoRebuild(modHiderTab, channelsList, allowedModsList, modSearch);
    }

    private void setupLegacyAutoRebuild(TabPanel.Tab tab, ListView channelsList, ListView allowedModsList,
            TextField modSearch) {
        if (channelsList != null)
            rebuildChannelsList(channelsList);
        if (allowedModsList != null && modSearch != null)
            rebuildAllowedModsList(allowedModsList, modSearch);

        tab.addWidget(new Widget(0, 0, 0, 0) {
            private int lastChannelSize = -1;
            private String lastSearch = "";

            @Override
            public void tick() {
                if (channelsList != null && lastChannelSize != ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size()) {
                    lastChannelSize = ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size();
                    rebuildChannelsList(channelsList);
                }

                if (modSearch != null && allowedModsList != null) {
                    String currentSearch = modSearch.getText() == null ? "" : modSearch.getText();
                    if (!lastSearch.equals(currentSearch)) {
                        lastSearch = currentSearch;
                        rebuildAllowedModsList(allowedModsList, modSearch);
                    }
                }
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float p) {
            }
        });
    }

    private ResizableCard createSpoofModeCard(int x, int y) {
        spoofModeCard = createResizableCard("spoofMode", x, y, 300, 150, "Spoof Mode");

        int contentX = spoofModeCard.getContentX();
        int contentY = spoofModeCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Control how your client appears to servers", Label.Style.BODY);
        spoofModeCard.addChild(description);

        List<String> spoofModes = List.of("VANILLA", "MODDED", "CUSTOM", "OFF");
        Dropdown spoofModeDropdown = new Dropdown(contentX, contentY + 30, 260,
                "Spoof Mode", spoofModes, selected -> {
                    try {
                        ModHiderOptions.SPOOF_MODE = SpoofMode.valueOf(selected.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        ModHiderOptions.SPOOF_MODE = SpoofMode.VANILLA;
                    }
                    ConfigManager.save();
                });
        spoofModeDropdown.setHeight(24);
        spoofModeDropdown.setSelectedOption(ModHiderOptions.SPOOF_MODE.name());
        spoofModeCard.addChild(spoofModeDropdown);

        spoofModeCard.updateLayout();
        return spoofModeCard;
    }

    private ResizableCard createCustomClientCard(int x, int y) {
        customClientCard = createResizableCard("customClient", x, y, 300, 140, "Custom Client Brand");

        int contentX = customClientCard.getContentX();
        int contentY = customClientCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Set custom client brand (CUSTOM mode only)", Label.Style.BODY);
        customClientCard.addChild(description);

        TextField customClient = new TextField(contentX, contentY + 30, 180, "fabric");
        customClient.setText(ModHiderOptions.CUSTOM_CLIENT == null ? "fabric" : ModHiderOptions.CUSTOM_CLIENT);
        customClientCard.addChild(customClient);

        Button applyCustomClient = new Button(contentX + 190, contentY + 30, 70, "Apply", () -> {
            ModHiderOptions.CUSTOM_CLIENT = customClient.getText().isBlank() ? "fabric" : customClient.getText();
            ConfigManager.save();
        });
        customClientCard.addChild(applyCustomClient);

        customClientCard.updateLayout();
        return customClientCard;
    }

    private ResizableCard createHideModsCard(int x, int y) {
        hideModsCard = createResizableCard("hideMods", x, y, 300, 110, "Hide Mods");

        int contentX = hideModsCard.getContentX();
        int contentY = hideModsCard.getContentY();

        ToggleSwitch hideModsToggle = new ToggleSwitch(contentX, contentY, 260,
                "Hide Mods",
                "Prevent servers from reading mod info",
                ModHiderOptions.HIDE_MODS, value -> {
                    ModHiderOptions.HIDE_MODS = value;
                    ConfigManager.save();
                });
        hideModsCard.addChild(hideModsToggle);

        hideModsCard.updateLayout();
        return hideModsCard;
    }

    private ResizableCard createDisablePayloadsCard(int x, int y) {
        disablePayloadsCard = createResizableCard("disablePayloads", x, y, 300, 120, "Disable Custom Payloads");

        int contentX = disablePayloadsCard.getContentX();
        int contentY = disablePayloadsCard.getContentY();

        ToggleSwitch disablePayloadsToggle = new ToggleSwitch(contentX, contentY, 260,
                "Disable Custom Payloads",
                "Block custom payload channels unless allowed",
                ModHiderOptions.DISABLE_CUSTOM_PAYLOADS, value -> {
                    ModHiderOptions.DISABLE_CUSTOM_PAYLOADS = value;
                    ConfigManager.save();
                });
        disablePayloadsCard.addChild(disablePayloadsToggle);

        disablePayloadsCard.updateLayout();
        return disablePayloadsCard;
    }

    private ResizableCard createAllowedChannelsCard(int x, int y) {
        allowedChannelsCard = createResizableCard("allowedChannels", x, y, 300, 220, "Allowed Payload Channels");

        int contentX = allowedChannelsCard.getContentX();
        int contentY = allowedChannelsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Whitelist custom payload channels (CUSTOM mode)", Label.Style.BODY);
        allowedChannelsCard.addChild(description);

        TextField channelField = new TextField(contentX, contentY + 30, 180, "example: minecraft:register");
        allowedChannelsCard.addChild(channelField);

        Button addChannel = new Button(contentX + 190, contentY + 30, 90, "Add", () -> {
            String val = channelField.getText() == null ? "" : channelField.getText().trim();
            if (!val.isBlank()) {
                ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.add(val);
                channelField.setText("");
                ConfigManager.save();
            }
        });
        allowedChannelsCard.addChild(addChannel);

        ListView channelsList = new ListView(contentX, contentY + 70, 260, 110);
        allowedChannelsCard.addChild(channelsList);

        rebuildChannelsList(channelsList);

        allowedChannelsCard.addChild(new Widget(0, 0, 0, 0) {
            private int lastSize = -1;

            @Override
            public void tick() {
                if (lastSize != ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size()) {
                    lastSize = ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size();
                    rebuildChannelsList(channelsList);
                }
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float p) {
            }
        });

        allowedChannelsCard.updateLayout();
        return allowedChannelsCard;
    }

    private ResizableCard createAllowedModsCard(int x, int y) {
        allowedModsCard = createResizableCard("allowedMods", x, y, 300, 320, "Allowed Mods");

        int contentX = allowedModsCard.getContentX();
        int contentY = allowedModsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Select mods to allow (MODDED/CUSTOM modes)", Label.Style.BODY);
        allowedModsCard.addChild(description);

        TextField modSearch = new TextField(contentX, contentY + 30, 260, "Search mods...");
        allowedModsCard.addChild(modSearch);

        ListView allowedModsList = new ListView(contentX, contentY + 70, 260, 210);
        allowedModsCard.addChild(allowedModsList);

        rebuildAllowedModsList(allowedModsList, modSearch);

        allowedModsCard.addChild(new Widget(0, 0, 0, 0) {
            private String lastSearch = "";

            @Override
            public void tick() {
                String currentSearch = modSearch.getText() == null ? "" : modSearch.getText();
                if (!lastSearch.equals(currentSearch)) {
                    lastSearch = currentSearch;
                    rebuildAllowedModsList(allowedModsList, modSearch);
                }
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float p) {
            }
        });

        allowedModsCard.updateLayout();
        return allowedModsCard;
    }

    private void initLegitTab() {
        TabPanel.Tab legitTab = tabPanel.addTab("Legit");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        if (ConfigManager.useCardLayout) {
            Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
                resetCardStates("fullbright");
            });
            legitTab.addWidget(resetLayout);

            CardContainer legitCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
            legitTab.addWidget(legitCardContainer);

            int currentY = contentY + 50;
            fullbrightCard = createFullbrightCard(contentX + 20, currentY);

            legitCardContainer.addCard(fullbrightCard);
            return;
        }

        legitTab.addWidget(new Label(contentX, contentY, "Fullbright", Label.Style.TITLE));

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                org.blackum.blackaddons.legit.LegitOptions.FullbrightEnabled, value -> {
                    org.blackum.blackaddons.legit.LegitOptions.FullbrightEnabled = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fullbrightToggle);
    }

    private ResizableCard createFullbrightCard(int x, int y) {
        fullbrightCard = createResizableCard("fullbright", x, y, 300, 100, "Fullbright");
        int contentX = fullbrightCard.getContentX();
        int contentY = fullbrightCard.getContentY();

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                org.blackum.blackaddons.legit.LegitOptions.FullbrightEnabled, value -> {
                    org.blackum.blackaddons.legit.LegitOptions.FullbrightEnabled = value;
                    ConfigManager.save();
                });
        fullbrightCard.addChild(fullbrightToggle);

        fullbrightCard.updateLayout();
        return fullbrightCard;
    }

    private void initCheatsTab() {
        TabPanel.Tab cheatsTab = tabPanel.addTab("Cheats");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        if (!ConfigManager.useCardLayout) {
            cheatsTab.addWidget(new Label(contentX, contentY, "AutoTnt", Label.Style.TITLE));

            ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                    "Enable AutoTnt",
                    "Automatically places TNT",
                    CheatsOptions.AutoTNTEnabled, value -> {
                        CheatsOptions.AutoTNTEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(enableToggle);

            Label tickLabel = new Label(contentX, contentY + 80,
                    "Tick Delay: " + CheatsOptions.AutoTNTDelay + " ticks", Label.Style.BODY);
            cheatsTab.addWidget(tickLabel);

            Slider tickSlider = new Slider(contentX, contentY + 100, contentWidth - 20, 5, 10,
                    CheatsOptions.AutoTNTDelay, val -> {
                        int ticks = Math.round(val);
                        if (ticks != CheatsOptions.AutoTNTDelay) {
                            CheatsOptions.AutoTNTDelay = ticks;
                            tickLabel.setText("Tick Delay: " + ticks + " ticks");
                            ConfigManager.save();
                        }
                    });
            cheatsTab.addWidget(tickSlider);
            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            resetCardStates("autoTnt");
        });
        cheatsTab.addWidget(resetLayout);

        CardContainer cheatsCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        cheatsTab.addWidget(cheatsCardContainer);

        int currentY = contentY + 50;
        autoTntCard = createAutoTntCard(contentX + 20, currentY);
        cheatsCardContainer.addCard(autoTntCard);
    }

    private ResizableCard createAutoTntCard(int x, int y) {
        autoTntCard = createResizableCard("autoTnt", x, y, 300, 260, "AutoTnt");

        int contentX = autoTntCard.getContentX();
        int contentY = autoTntCard.getContentY();

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable AutoTnt",
                "Automatically places TNT",
                CheatsOptions.AutoTNTEnabled, value -> {
                    CheatsOptions.AutoTNTEnabled = value;
                    ConfigManager.save();
                });
        autoTntCard.addChild(enableToggle);

        Label tickLabel = new Label(contentX, contentY + 50,
                "Tick Delay: " + CheatsOptions.AutoTNTDelay + " ticks", Label.Style.BODY);
        autoTntCard.addChild(tickLabel);

        Slider tickSlider = new Slider(contentX, contentY + 70, 260, 5, 10, CheatsOptions.AutoTNTDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != CheatsOptions.AutoTNTDelay) {
                CheatsOptions.AutoTNTDelay = ticks;
                tickLabel.setText("Tick Delay: " + ticks + " ticks");
                ConfigManager.save();
            }
        });
        autoTntCard.addChild(tickSlider);

        Label unequipLabel = new Label(contentX, contentY + 100,
                "Unequip Delay: " + CheatsOptions.UnequipDelay + " ticks", Label.Style.BODY);
        autoTntCard.addChild(unequipLabel);

        Slider unequipSlider = new Slider(contentX, contentY + 120, 260, 5, 10, CheatsOptions.UnequipDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != CheatsOptions.UnequipDelay) {
                CheatsOptions.UnequipDelay = ticks;
                unequipLabel.setText("Unequip Delay: " + ticks + " ticks");
                ConfigManager.save();
            }
        });
        autoTntCard.addChild(unequipSlider);

        ToggleSwitch swapBackToggle = new ToggleSwitch(contentX, contentY + 150, 260,
                "Swap Back",
                "Switch to original item after interaction",
                CheatsOptions.SwapBack, value -> {
                    CheatsOptions.SwapBack = value;
                    ConfigManager.save();
                });
        autoTntCard.addChild(swapBackToggle);

        autoTntCard.updateLayout();
        return autoTntCard;
    }

    private void rebuildChannelsList(ListView channelsList) {
        channelsList.clearItems();
        List<String> channels = new ArrayList<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS);
        channels.sort(String::compareToIgnoreCase);
        for (String ch : channels) {
            Button remove = new Button(0, 0, channelsList.getWidth() - 8, "Remove: " + ch, () -> {
                ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.remove(ch);
                ConfigManager.save();
                rebuildChannelsList(channelsList);
            });
            channelsList.addItem(remove);
        }
    }

    private void rebuildAllowedModsList(ListView allowedModsList, TextField modSearch) {
        allowedModsList.clearItems();
        String query = modSearch.getText() == null ? "" : modSearch.getText().trim().toLowerCase(Locale.ROOT);

        ModOrganizer.OrganizedMods organizedMods = ModOrganizer.organizeMods();

        java.util.function.Consumer<String> enableDependencies = modId -> {
            ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
            if (info != null) {
                for (String depId : info.dependencies) {
                    ModHiderOptions.ALLOWED_MODS.add(depId);
                }
            }
        };

        java.util.function.Consumer<String> disableDependents = modId -> {
            ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
            if (info != null) {
                for (String dependentId : info.dependents) {
                    ModHiderOptions.ALLOWED_MODS.remove(dependentId);
                }
            }
        };

        Label modsSectionLabel = new Label(0, 0, "§6Mods", Label.Style.BODY);
        modsSectionLabel.setHeight(20);
        allowedModsList.addItem(modsSectionLabel);

        for (ModOrganizer.ModGroup group : organizedMods.modGroups) {
            addModGroupToList(allowedModsList, group, query, enableDependencies, disableDependents, modSearch);
        }

        Label libsSectionLabel = new Label(0, 0, "§6Libraries", Label.Style.BODY);
        libsSectionLabel.setHeight(20);
        allowedModsList.addItem(libsSectionLabel);

        for (ModOrganizer.ModGroup group : organizedMods.libraryGroups) {
            addModGroupToList(allowedModsList, group, query, enableDependencies, disableDependents, modSearch);
        }
    }

    private void addModGroupToList(ListView list, ModOrganizer.ModGroup group, String query,
            java.util.function.Consumer<String> enableDependencies,
            java.util.function.Consumer<String> disableDependencies,
            TextField searchField) {
        List<ModOrganizer.ModInfo> matchingMods = new ArrayList<>();
        for (ModOrganizer.ModInfo info : group.mods) {
            boolean matches = true;
            for (String term : query.split(" ")) {
                if (term.isBlank())
                    continue;
                if (!info.name.toLowerCase(Locale.ROOT).contains(term) &&
                        !info.id.toLowerCase(Locale.ROOT).contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches)
                matchingMods.add(info);
        }

        if (matchingMods.isEmpty())
            return;

        if (group.mods.size() > 1) {
            String groupKey = "mod_" + group.groupName;
            boolean isCollapsed = collapsedGroups.getOrDefault(groupKey, true);
            boolean allSelected = matchingMods.stream().allMatch(m -> ModHiderOptions.ALLOWED_MODS.contains(m.id));

            String arrow = isCollapsed ? "▶" : "▼";
            Button groupHeader = new Button(0, 0, 0, arrow + " §b" + group.groupName + " (" + matchingMods.size() + ")",
                    () -> {
                        collapsedGroups.put(groupKey, !collapsedGroups.getOrDefault(groupKey, true));
                        rebuildAllowedModsList(list, searchField);
                    });
            groupHeader.setHeight(20);
            list.addItem(groupHeader);

            if (!isCollapsed) {
                Checkbox selectAll = new Checkbox(10, 0, "Select All", allSelected, value -> {
                    for (ModOrganizer.ModInfo info : matchingMods) {
                        if (value) {
                            ModHiderOptions.ALLOWED_MODS.add(info.id);
                            enableDependencies.accept(info.id);
                        } else {
                            ModHiderOptions.ALLOWED_MODS.remove(info.id);
                            disableDependencies.accept(info.id);
                        }
                    }
                    ConfigManager.save();
                    rebuildAllowedModsList(list, searchField);
                });
                list.addItem(selectAll);

                for (ModOrganizer.ModInfo info : matchingMods) {
                    addModCheckboxToList(list, info, enableDependencies, disableDependencies, searchField);
                }
            }
        } else {
            for (ModOrganizer.ModInfo info : matchingMods) {
                addModCheckboxToList(list, info, enableDependencies, disableDependencies, searchField);
            }
        }
    }

    private void addModCheckboxToList(ListView list, ModOrganizer.ModInfo info,
            java.util.function.Consumer<String> enableDependencies,
            java.util.function.Consumer<String> disableDependencies,
            TextField searchField) {
        boolean checked = ModHiderOptions.ALLOWED_MODS.contains(info.id);
        String displayName = info.name + " (" + info.id + ")";
        if (!info.dependents.isEmpty())
            displayName += " §7[Used by: " + info.dependents.size() + "]";

        Checkbox cb = new Checkbox(0, 0, displayName, checked, value -> {
            if (value) {
                ModHiderOptions.ALLOWED_MODS.add(info.id);
                enableDependencies.accept(info.id);
            } else {
                ModHiderOptions.ALLOWED_MODS.remove(info.id);
                disableDependencies.accept(info.id);
            }
            ConfigManager.save();
            rebuildAllowedModsList(list, searchField);
        });

        Widget wrapper = new Widget(0, 0, 0, 0) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float p) {
                cb.setX(getX());
                cb.setY(getY());
                cb.setWidth(getWidth());
                cb.render(g, mx, my, p);
                if (cb.isHovered() && !info.dependencies.isEmpty()) {
                    currentTooltip = "Dependencies: " + String.join(", ", info.dependencies);
                }
            }

            @Override
            public void updateHoverState(int mx, int my) {
                cb.setX(getX());
                cb.setY(getY());
                cb.setWidth(getWidth());
                cb.updateHoverState(mx, my);
                super.updateHoverState(mx, my);
                if (!cb.isHovered())
                    currentTooltip = null;
            }

            @Override
            public boolean mouseClicked(double mx, double my, int b) {
                return cb.mouseClicked(mx, my, b);
            }

            @Override
            public void tick() {
                cb.tick();
            }
        };
        wrapper.setHeight(cb.getHeight());
        list.addItem(wrapper);
    }

    private void updateCardVisibility() {
        if (!ConfigManager.useCardLayout || customClientCard == null)
            return;
        SpoofMode mode = ModHiderOptions.SPOOF_MODE;
        boolean isCustom = mode == SpoofMode.CUSTOM;
        boolean isModdedOrCustom = mode == SpoofMode.MODDED || mode == SpoofMode.CUSTOM;
        customClientCard.setVisible(isCustom);
        allowedChannelsCard.setVisible(isCustom);
        allowedModsCard.setVisible(isModdedOrCustom);
    }

    @Override
    protected void renderScrolledContent(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        String title = "BlackAddons Control Panel";
        int titleWidth = font.width(title);
        graphics.drawString(font, title, containerX + (containerWidth - titleWidth) / 2, containerY + 15,
                Theme.TEXT_PRIMARY);
        currentTooltip = null;
    }

    @Override
    protected void renderTooltips(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        if (currentTooltip != null && !currentTooltip.isEmpty()) {
            int tooltipWidth = font.width(currentTooltip) + 8;
            int tooltipXPos = mouseX + 10;
            int tooltipYPos = mouseY - 20;
            if (tooltipXPos + tooltipWidth > width)
                tooltipXPos = mouseX - tooltipWidth - 10;
            if (tooltipYPos < 0)
                tooltipYPos = mouseY + 10;
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos + 10 + 2,
                    Theme.TOOLTIP_BG);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos - 1,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos + 11, tooltipXPos + tooltipWidth + 2, tooltipYPos + 12,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos - 1, tooltipYPos + 12, Theme.ACCENT);
            graphics.fill(tooltipXPos + tooltipWidth + 1, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2,
                    tooltipYPos + 12, Theme.ACCENT);
            graphics.drawString(font, currentTooltip, tooltipXPos, tooltipYPos, Theme.TEXT_PRIMARY);
        }
    }

    @Override
    public void onClose() {
        if (ConfigManager.useCardLayout) {
            saveCardLayout();
        } else {
            ConfigManager.save();
        }
        super.onClose();
    }

    private void resetCardStates(String... ids) {
        for (String id : ids) {
            ConfigManager.lastLoadedCardStates.remove(id);
        }
        ConfigManager.save(ConfigManager.lastLoadedCardStates);
        this.init(this.width, this.height);
    }

    private void addCardToMap(java.util.Map<String, ConfigManager.CardState> map, String id, ResizableCard card) {
        if (card != null)
            map.put(id, new ConfigManager.CardState(card.getX(), card.getY(), card.getWidth(), card.getHeight(),
                    card.isCollapsed(), card.getInitialWidth(), card.getExpandedHeight()));
    }

    private void saveCardLayout() {
        java.util.Map<String, ConfigManager.CardState> states = new java.util.HashMap<>();
        addCardToMap(states, "spoofMode", spoofModeCard);
        addCardToMap(states, "customClient", customClientCard);
        addCardToMap(states, "hideMods", hideModsCard);
        addCardToMap(states, "disablePayloads", disablePayloadsCard);
        addCardToMap(states, "allowedChannels", allowedChannelsCard);
        addCardToMap(states, "allowedMods", allowedModsCard);
        addCardToMap(states, "autoTnt", autoTntCard);

        addCardToMap(states, "fullbright", fullbrightCard);
        ConfigManager.save(states);
        ConfigManager.lastLoadedCardStates = states;
    }

    private ResizableCard createResizableCard(String id, int defaultX, int defaultY, int defaultW, int defaultH,
            String title) {
        ConfigManager.CardState state = ConfigManager.lastLoadedCardStates.get(id);
        if (state != null) {
            ResizableCard card = new ResizableCard(state.x, state.y, state.width, state.height, title);
            card.setCollapsed(state.collapsed);
            if (state.initialWidth > 0) {
                card.setInitialWidth(state.initialWidth);
            }
            if (state.expandedHeight > 0) {
                card.setExpandedHeight(state.expandedHeight);
            }
            card.setOnLayoutChange(this::saveCardLayout);
            return card;
        }
        ResizableCard card = new ResizableCard(defaultX, defaultY, defaultW, defaultH, title);
        card.setOnLayoutChange(this::saveCardLayout);
        return card;
    }
}
