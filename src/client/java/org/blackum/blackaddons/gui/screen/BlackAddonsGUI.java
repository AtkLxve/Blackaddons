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
    private int tooltipX = 0;
    private int tooltipY = 0;
    private final java.util.Map<String, Boolean> collapsedGroups = new java.util.HashMap<>();

    private ResizableCard spoofModeCard;
    private ResizableCard customClientCard;
    private ResizableCard hideModsCard;
    private ResizableCard disablePayloadsCard;
    private ResizableCard allowedChannelsCard;
    private ResizableCard allowedModsCard;
    private ResizableCard autoTntCard;

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
        initPayloadsTab();
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

        settingsTab.addWidget(new Label(contentX, contentY, "App Appearance", Label.Style.TITLE));

        settingsTab.addWidget(new Label(contentX, contentY + 30, "Accent Color (Main Theme)", Label.Style.BODY));

        ColorPicker accentPicker = new ColorPicker(contentX, contentY + 50, color -> Theme.ACCENT = color);

        settingsTab.addWidget(accentPicker);

        ToggleSwitch layoutToggle = new ToggleSwitch(contentX, contentY + 280, 400,
                "Use Card Layout",
                "Enable resizable card-based layout for Mod Hider",
                ConfigManager.useCardLayout, value -> {
                    ConfigManager.useCardLayout = value;
                    ConfigManager.save();
                    this.init(this.width, this.height);
                });
        settingsTab.addWidget(layoutToggle);

        Label durationLabel = new Label(contentX, contentY + 320,
                "Notification Duration: " + GeneralOptions.NOTIFICATION_DURATION + "ms", Label.Style.BODY);
        settingsTab.addWidget(durationLabel);

        Slider durationSlider = new Slider(contentX, contentY + 330, 200, 500f, 10000f,
                GeneralOptions.NOTIFICATION_DURATION, val -> {
                    int duration = Math.round(val);
                    if (duration != GeneralOptions.NOTIFICATION_DURATION) {
                        GeneralOptions.NOTIFICATION_DURATION = duration;
                        durationLabel.setText("Notification Duration: " + duration + "ms");
                        ConfigManager.save();
                    }
                });
        settingsTab.addWidget(durationSlider);
    }

    private void initAboutTab() {
        TabPanel.Tab aboutTab = tabPanel.addTab("About");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();

        aboutTab.addWidget(new Label(contentX, contentY, "BlackAddons", Label.Style.TITLE));
        aboutTab.addWidget(new Label(contentX, contentY + 30, "Version: 1.0.0", Label.Style.BODY));
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

        Button resetLayout = new Button(contentX + 20, contentY, 100, "Reset Layout", () -> {
            ConfigManager.lastLoadedCardStates.clear();
            ConfigManager.save();
            this.init(this.width, this.height);
        });
        modHiderTab.addWidget(resetLayout);

        CardContainer modHiderCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        modHiderTab.addWidget(modHiderCardContainer);

        int containerY = contentY + 30;
        boolean isSingleColumn = contentWidth < 680;
        int col1X = contentX + 20;
        int col2X = contentX + 340;

        if (isSingleColumn) {
            createSpoofModeCard(col1X, containerY + 20);
            createCustomClientCard(col1X, containerY + 60);
            createHideModsCard(col1X, containerY + 100);
            createDisablePayloadsCard(col1X, containerY + 140);
            createAllowedChannelsCard(col1X, containerY + 180);
            createAllowedModsCard(col1X, containerY + 220);
        } else {
            createSpoofModeCard(col1X, containerY + 20);
            createCustomClientCard(col1X, containerY + 60);
            createHideModsCard(col2X, containerY + 20);
            createDisablePayloadsCard(col2X, containerY + 60);
            createAllowedChannelsCard(col1X, containerY + 100);
            createAllowedModsCard(col2X, containerY + 100);
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

            TextField channelField = new TextField(contentX, currentY, contentWidth - 100, "example: Hypixel");
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

    private void createSpoofModeCard(int x, int y) {
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
    }

    private void createCustomClientCard(int x, int y) {
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
    }

    private void createHideModsCard(int x, int y) {
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
    }

    private void createDisablePayloadsCard(int x, int y) {
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
    }

    private void createAllowedChannelsCard(int x, int y) {
        allowedChannelsCard = createResizableCard("allowedChannels", x, y, 300, 220, "Allowed Payload Channels");

        int contentX = allowedChannelsCard.getContentX();
        int contentY = allowedChannelsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Whitelist custom payload channels (CUSTOM mode)", Label.Style.BODY);
        allowedChannelsCard.addChild(description);

        TextField channelField = new TextField(contentX, contentY + 30, 180, "example: Hypixel");
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
    }

    private void createAllowedModsCard(int x, int y) {
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

        Button resetLayout = new Button(contentX + 20, contentY, 100, "Reset Layout", () -> {
            ConfigManager.lastLoadedCardStates.clear();
            ConfigManager.save();
            this.init(this.width, this.height);
        });
        cheatsTab.addWidget(resetLayout);

        CardContainer cheatsCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        cheatsTab.addWidget(cheatsCardContainer);

        createAutoTntCard(contentX + 20, contentY + 50);
        cheatsCardContainer.addCard(autoTntCard);
    }

    private void createAutoTntCard(int x, int y) {
        autoTntCard = createResizableCard("autoTnt", x, y, 300, 150, "AutoTnt");

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
                    tooltipX = mx;
                    tooltipY = my;
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
            int tooltipXPos = this.tooltipX + 10;
            int tooltipYPos = (int) (this.tooltipY - scrollOffset) - 20;
            if (tooltipXPos + tooltipWidth > containerX + containerWidth)
                tooltipXPos = this.tooltipX - tooltipWidth - 10;
            if (tooltipYPos < containerY)
                tooltipYPos = (int) (this.tooltipY - scrollOffset) + 10;
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

    private void initPayloadsTab() {
        TabPanel.Tab payloadsTab = tabPanel.addTab("Payloads");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        payloadsTab.addWidget(new Label(contentX, contentY, "Recorded Payloads", Label.Style.TITLE));

        Button clearButton = new Button(contentX + 120, contentY - 10, 100, "Clear",
                () -> {
                    org.blackum.blackaddons.payload.PayloadManager.clearRecordedPayloads();
                    this.init(this.width, this.height);
                });
        payloadsTab.addWidget(clearButton);

        ListView recordedList = new ListView(contentX, contentY + 40, contentWidth - 20, 200);
        payloadsTab.addWidget(recordedList);

        synchronized (org.blackum.blackaddons.payload.PayloadManager.recordedPayloads) {
            for (org.blackum.blackaddons.payload.RecordedPayload payload : org.blackum.blackaddons.payload.PayloadManager.recordedPayloads) {
                String label = payload.channel + " (" + payload.data.length() / 2 + " bytes)";
                Button item = new Button(0, 0, 0, label, () -> {
                    org.blackum.blackaddons.payload.PayloadOverride override = new org.blackum.blackaddons.payload.PayloadOverride();
                    override.channel = payload.channel;
                    override.originalData = payload.data;
                    override.replacementData = payload.data;
                    override.enabled = true;

                    net.minecraft.client.Minecraft.getInstance().setScreen(
                            new PayloadEditorScreen(this, override, true, () -> this.init(this.width, this.height)));
                });
                recordedList.addItem(item);
            }
        }

        int overridesY = contentY + 250;
        payloadsTab.addWidget(new Label(contentX, overridesY, "Active Overrides", Label.Style.TITLE));

        ListView overridesList = new ListView(contentX, overridesY + 30, contentWidth - 20, 200);
        payloadsTab.addWidget(overridesList);

        for (org.blackum.blackaddons.payload.PayloadOverride override : org.blackum.blackaddons.payload.PayloadManager.overrides) {
            Widget overrideWidget = createOverrideWidget(override, overridesList);
            overridesList.addItem(overrideWidget);
        }
    }

    private Widget createOverrideWidget(org.blackum.blackaddons.payload.PayloadOverride override, ListView parentList) {
        Widget container = new Widget(0, 0, 0, 24) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float p) {
            }
        };

        Checkbox enableBox = new Checkbox(0, 2, override.channel, override.enabled, val -> {
            override.enabled = val;
            ConfigManager.save();
        });

        Button editButton = new Button(0, 0, 50, "Edit", () -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new PayloadEditorScreen(this, override, false, () -> this.init(this.width, this.height)));
        });
        editButton.setHeight(20);

        Button deleteButton = new Button(0, 0, 20, "X", () -> {
            org.blackum.blackaddons.payload.PayloadManager.removeOverride(override);
            ConfigManager.save();
            this.init(this.width, this.height);
        });
        deleteButton.setHeight(20);

        Widget wrapper = new Widget(0, 0, 0, 24) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float p) {
                int w = getWidth();
                int x = getX();
                int y = getY();

                enableBox.setX(x);
                enableBox.setY(y + 2);
                enableBox.setWidth(w - 80);

                editButton.setX(x + w - 75);
                editButton.setY(y);

                deleteButton.setX(x + w - 22);
                deleteButton.setY(y);

                enableBox.render(g, mx, my, p);
                editButton.render(g, mx, my, p);
                deleteButton.render(g, mx, my, p);
            }

            @Override
            public void tick() {
                enableBox.tick();
                editButton.tick();
                deleteButton.tick();
            }

            @Override
            public boolean mouseClicked(double mx, double my, int b) {
                if (editButton.mouseClicked(mx, my, b))
                    return true;
                if (deleteButton.mouseClicked(mx, my, b))
                    return true;
                if (enableBox.mouseClicked(mx, my, b))
                    return true;
                return false;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int b) {
                if (editButton.mouseReleased(mx, my, b))
                    return true;
                if (deleteButton.mouseReleased(mx, my, b))
                    return true;
                if (enableBox.mouseReleased(mx, my, b))
                    return true;
                return false;
            }

            @Override
            public void updateHoverState(int mx, int my) {
                super.updateHoverState(mx, my);
                enableBox.updateHoverState(mx, my);
                editButton.updateHoverState(mx, my);
                deleteButton.updateHoverState(mx, my);
            }
        };

        return wrapper;
    }

    private void addCardToMap(java.util.Map<String, ConfigManager.CardState> map, String id, ResizableCard card) {
        if (card != null)
            map.put(id, new ConfigManager.CardState(card.getX(), card.getY(), card.getWidth(), card.getHeight(),
                    card.isCollapsed()));
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
        ConfigManager.save(states);
        ConfigManager.lastLoadedCardStates = states;
    }

    private ResizableCard createResizableCard(String id, int defaultX, int defaultY, int defaultW, int defaultH,
            String title) {
        ConfigManager.CardState state = ConfigManager.lastLoadedCardStates.get(id);
        if (state != null) {
            ResizableCard card = new ResizableCard(state.x, state.y, state.width, state.height, title);
            card.setCollapsed(state.collapsed);
            card.setOnLayoutChange(this::saveCardLayout);
            return card;
        }
        ResizableCard card = new ResizableCard(defaultX, defaultY, defaultW, defaultH, title);
        card.setOnLayoutChange(this::saveCardLayout);
        return card;
    }
}
