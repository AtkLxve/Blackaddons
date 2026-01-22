package org.blackum.blackaddons.gui.screen;

import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlackAddonsGUI extends BaseScreen {

    private TabPanel tabPanel;
    private String currentTooltip = null;
    private int tooltipX = 0;
    private int tooltipY = 0;
    private java.util.Map<String, Boolean> collapsedGroups = new java.util.HashMap<>();

    private CardContainer modHiderCardContainer;
    private ResizableCard spoofModeCard;
    private ResizableCard customClientCard;
    private ResizableCard hideModsCard;
    private ResizableCard disablePayloadsCard;
    private ResizableCard allowedChannelsCard;
    private ResizableCard allowedModsCard;

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
        initModHiderTab();
        initAboutTab();

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

    private void initModHiderTab() {
        TabPanel.Tab modHiderTab = tabPanel.addTab("Mod Hider");

        int contentX = tabPanel.getContentX();
        int contentY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        modHiderCardContainer = new CardContainer(contentX, contentY, contentWidth, 600);
        modHiderTab.addWidget(modHiderCardContainer);

        createSpoofModeCard(contentX + 20, contentY + 20);
        createCustomClientCard(contentX + 20, contentY + 200);
        createHideModsCard(contentX + 340, contentY + 20);
        createDisablePayloadsCard(contentX + 340, contentY + 140);
        createAllowedChannelsCard(contentX + 20, contentY + 340);
        createAllowedModsCard(contentX + 340, contentY + 280);

        modHiderCardContainer.addCard(spoofModeCard);
        modHiderCardContainer.addCard(customClientCard);
        modHiderCardContainer.addCard(hideModsCard);
        modHiderCardContainer.addCard(disablePayloadsCard);
        modHiderCardContainer.addCard(allowedChannelsCard);
        modHiderCardContainer.addCard(allowedModsCard);
    }

    private void createSpoofModeCard(int x, int y) {
        spoofModeCard = new ResizableCard(x, y, 300, 150, "Spoof Mode");

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
        customClientCard = new ResizableCard(x, y, 300, 120, "Custom Client Brand");

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
        hideModsCard = new ResizableCard(x, y, 300, 110, "Hide Mods");

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
        disablePayloadsCard = new ResizableCard(x, y, 300, 120, "Disable Custom Payloads");

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
        allowedChannelsCard = new ResizableCard(x, y, 380, 220, "Allowed Payload Channels");

        int contentX = allowedChannelsCard.getContentX();
        int contentY = allowedChannelsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Whitelist custom payload channels (CUSTOM mode)", Label.Style.BODY);
        allowedChannelsCard.addChild(description);

        TextField channelField = new TextField(contentX, contentY + 30, 260, "example: hypixel");
        allowedChannelsCard.addChild(channelField);

        Button addChannel = new Button(contentX + 270, contentY + 30, 90, "Add", () -> {
            String val = channelField.getText() == null ? "" : channelField.getText().trim();
            if (!val.isBlank()) {
                ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.add(val);
                channelField.setText("");
                ConfigManager.save();
            }
        });
        allowedChannelsCard.addChild(addChannel);

        ListView channelsList = new ListView(contentX, contentY + 70, 340, 110);
        allowedChannelsCard.addChild(channelsList);

        final Runnable[] rebuildChannelsRef = new Runnable[1];
        rebuildChannelsRef[0] = () -> {
            channelsList.clearItems();
            List<String> channels = new ArrayList<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS);
            channels.sort(String::compareToIgnoreCase);
            for (String ch : channels) {
                Button remove = new Button(0, 0, channelsList.getWidth() - 8, "Remove: " + ch, () -> {
                    ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.remove(ch);
                    ConfigManager.save();
                    if (rebuildChannelsRef[0] != null)
                        rebuildChannelsRef[0].run();
                });
                channelsList.addItem(remove);
            }
        };

        allowedChannelsCard.addChild(new Widget(0, 0, 0, 0) {
            private int lastSize = -1;

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
            }

            @Override
            public void tick() {
                if (lastSize != ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size()) {
                    lastSize = ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size();
                    if (rebuildChannelsRef[0] != null)
                        rebuildChannelsRef[0].run();
                }
            }
        });

        rebuildChannelsRef[0].run();
    }

    private void createAllowedModsCard(int x, int y) {
        allowedModsCard = new ResizableCard(x, y, 380, 300, "Allowed Mods");

        int contentX = allowedModsCard.getContentX();
        int contentY = allowedModsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Select mods to allow (MODDED/CUSTOM modes)", Label.Style.BODY);
        allowedModsCard.addChild(description);

        TextField modSearch = new TextField(contentX, contentY + 30, 340, "Search mods...");
        allowedModsCard.addChild(modSearch);

        ListView allowedModsList = new ListView(contentX, contentY + 70, 340, 200);
        allowedModsCard.addChild(allowedModsList);

        ModOrganizer.OrganizedMods organizedMods = ModOrganizer.organizeMods();

        java.util.function.Consumer<String> enableDependencies = new java.util.function.Consumer<String>() {
            private java.util.Set<String> processing = new java.util.HashSet<>();

            @Override
            public void accept(String modId) {
                if (processing.contains(modId))
                    return;
                processing.add(modId);

                ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
                if (info != null) {
                    for (String depId : info.dependencies) {
                        if (!ModHiderOptions.ALLOWED_MODS.contains(depId)) {
                            ModHiderOptions.ALLOWED_MODS.add(depId);
                            accept(depId);
                        }
                    }
                }
                processing.remove(modId);
            }
        };

        java.util.function.Consumer<String> disableDependents = new java.util.function.Consumer<String>() {
            private java.util.Set<String> processing = new java.util.HashSet<>();

            @Override
            public void accept(String modId) {
                if (processing.contains(modId))
                    return;
                processing.add(modId);

                ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
                if (info != null) {
                    for (String dependentId : info.dependents) {
                        if (ModHiderOptions.ALLOWED_MODS.contains(dependentId)) {
                            ModHiderOptions.ALLOWED_MODS.remove(dependentId);
                            accept(dependentId);
                        }
                    }
                }
                processing.remove(modId);
            }
        };

        final Runnable[] rebuildAllowedModsRef = new Runnable[1];
        rebuildAllowedModsRef[0] = () -> {
            allowedModsList.clearItems();
            String query = modSearch.getText() == null ? "" : modSearch.getText().trim().toLowerCase(Locale.ROOT);

            Label modsSectionLabel = new Label(0, 0, "§6Mods", Label.Style.BODY);
            modsSectionLabel.setHeight(20);
            allowedModsList.addItem(modsSectionLabel);

            for (ModOrganizer.ModGroup group : organizedMods.modGroups) {
                boolean groupMatches = false;
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
                    if (matches) {
                        matchingMods.add(info);
                        groupMatches = true;
                    }
                }

                if (!groupMatches)
                    continue;

                if (group.mods.size() > 1) {
                    String groupKey = "mod_" + group.groupName;
                    boolean isCollapsed = collapsedGroups.getOrDefault(groupKey, true);
                    boolean allSelected = matchingMods.stream()
                            .allMatch(m -> ModHiderOptions.ALLOWED_MODS.contains(m.id));

                    String arrow = isCollapsed ? "▶" : "▼";
                    Button groupHeader = new Button(0, 0, 0,
                            arrow + " §b" + group.groupName + " (" + matchingMods.size() + ")", () -> {
                                boolean currentState = collapsedGroups.getOrDefault(groupKey, true);
                                collapsedGroups.put(groupKey, !currentState);
                                if (rebuildAllowedModsRef[0] != null)
                                    rebuildAllowedModsRef[0].run();
                            });
                    groupHeader.setHeight(20);
                    allowedModsList.addItem(groupHeader);

                    if (!isCollapsed) {
                        Checkbox groupCheckbox = new Checkbox(10, 0,
                                "Select All",
                                allSelected, value -> {
                                    for (ModOrganizer.ModInfo info : matchingMods) {
                                        if (value) {
                                            ModHiderOptions.ALLOWED_MODS.add(info.id);
                                            enableDependencies.accept(info.id);
                                        } else {
                                            ModHiderOptions.ALLOWED_MODS.remove(info.id);
                                            disableDependents.accept(info.id);
                                        }
                                    }
                                    ConfigManager.save();
                                    if (rebuildAllowedModsRef[0] != null)
                                        rebuildAllowedModsRef[0].run();
                                });
                        allowedModsList.addItem(groupCheckbox);

                        for (ModOrganizer.ModInfo info : matchingMods) {
                            boolean checked = ModHiderOptions.ALLOWED_MODS.contains(info.id);
                            String displayName = info.name + " (" + info.id + ")";
                            if (!info.dependents.isEmpty()) {
                                displayName += " §7[Used by: " + info.dependents.size() + "]";
                            }

                            final ModOrganizer.ModInfo finalInfo = info;
                            Checkbox cb = new Checkbox(20, 0, displayName, checked, value -> {
                                if (value) {
                                    ModHiderOptions.ALLOWED_MODS.add(info.id);
                                    enableDependencies.accept(info.id);
                                } else {
                                    ModHiderOptions.ALLOWED_MODS.remove(info.id);
                                    disableDependents.accept(info.id);
                                }
                                ConfigManager.save();
                                if (rebuildAllowedModsRef[0] != null)
                                    rebuildAllowedModsRef[0].run();
                            });

                            Widget checkboxWithTooltip = new Widget(0, 0, 0, 0) {
                                @Override
                                public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX,
                                        int mouseY,
                                        float partialTick) {
                                    cb.setX(getX());
                                    cb.setY(getY());
                                    cb.setWidth(getWidth());
                                    cb.render(graphics, mouseX, mouseY, partialTick);

                                    if (cb.isHovered() && !finalInfo.dependencies.isEmpty()) {
                                        currentTooltip = "Dependencies: " + String.join(", ", finalInfo.dependencies);
                                        tooltipX = mouseX;
                                        tooltipY = mouseY;
                                    }
                                }

                                @Override
                                public void updateHoverState(int mouseX, int mouseY) {
                                    cb.setX(getX());
                                    cb.setY(getY());
                                    cb.setWidth(getWidth());

                                    cb.updateHoverState(mouseX, mouseY);
                                    super.updateHoverState(mouseX, mouseY);
                                    if (!cb.isHovered()) {
                                        currentTooltip = null;
                                    }
                                }

                                @Override
                                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                                    return cb.mouseClicked(mouseX, mouseY, button);
                                }

                                @Override
                                public void tick() {
                                    cb.tick();
                                }
                            };
                            checkboxWithTooltip.setHeight(cb.getHeight());
                            checkboxWithTooltip.setWidth(cb.getWidth());
                            allowedModsList.addItem(checkboxWithTooltip);
                        }
                    }
                } else {
                    for (ModOrganizer.ModInfo info : matchingMods) {
                        boolean checked = ModHiderOptions.ALLOWED_MODS.contains(info.id);
                        String displayName = info.name + " (" + info.id + ")";
                        if (!info.dependents.isEmpty()) {
                            displayName += " §7[Used by: " + info.dependents.size() + "]";
                        }

                        final ModOrganizer.ModInfo finalInfo = info;
                        Checkbox cb = new Checkbox(0, 0, displayName, checked, value -> {
                            if (value) {
                                ModHiderOptions.ALLOWED_MODS.add(info.id);
                                enableDependencies.accept(info.id);
                            } else {
                                ModHiderOptions.ALLOWED_MODS.remove(info.id);
                                disableDependents.accept(info.id);
                            }
                            ConfigManager.save();
                            if (rebuildAllowedModsRef[0] != null)
                                rebuildAllowedModsRef[0].run();
                        });

                        Widget checkboxWithTooltip = new Widget(0, 0, 0, 0) {
                            @Override
                            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick) {
                                cb.setX(getX());
                                cb.setY(getY());
                                cb.setWidth(getWidth());
                                cb.render(graphics, mouseX, mouseY, partialTick);

                                if (cb.isHovered() && !finalInfo.dependencies.isEmpty()) {
                                    currentTooltip = "Dependencies: " + String.join(", ", finalInfo.dependencies);
                                    tooltipX = mouseX;
                                    tooltipY = mouseY;
                                }
                            }

                            @Override
                            public void updateHoverState(int mouseX, int mouseY) {
                                cb.setX(getX());
                                cb.setY(getY());
                                cb.setWidth(getWidth());

                                cb.updateHoverState(mouseX, mouseY);
                                super.updateHoverState(mouseX, mouseY);
                                if (!cb.isHovered()) {
                                    currentTooltip = null;
                                }
                            }

                            @Override
                            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                                return cb.mouseClicked(mouseX, mouseY, button);
                            }

                            @Override
                            public void tick() {
                                cb.tick();
                            }
                        };
                        checkboxWithTooltip.setHeight(cb.getHeight());
                        checkboxWithTooltip.setWidth(cb.getWidth());
                        allowedModsList.addItem(checkboxWithTooltip);
                    }
                }
            }

            Label libsSectionLabel = new Label(0, 0, "§6Libraries", Label.Style.BODY);
            libsSectionLabel.setHeight(20);
            allowedModsList.addItem(libsSectionLabel);

            for (ModOrganizer.ModGroup group : organizedMods.libraryGroups) {
                boolean groupMatches = false;
                List<ModOrganizer.ModInfo> matchingLibs = new ArrayList<>();

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
                    if (matches) {
                        matchingLibs.add(info);
                        groupMatches = true;
                    }
                }

                if (!groupMatches)
                    continue;

                if (group.mods.size() > 1) {
                    String groupKey = "lib_" + group.groupName;
                    boolean isCollapsed = collapsedGroups.getOrDefault(groupKey, true);
                    boolean allSelected = matchingLibs.stream()
                            .allMatch(m -> ModHiderOptions.ALLOWED_MODS.contains(m.id));

                    String arrow = isCollapsed ? "▶" : "▼";
                    Button groupHeader = new Button(0, 0, 0,
                            arrow + " §b" + group.groupName + " (" + matchingLibs.size() + ")", () -> {
                                boolean currentState = collapsedGroups.getOrDefault(groupKey, true);
                                collapsedGroups.put(groupKey, !currentState);
                                if (rebuildAllowedModsRef[0] != null)
                                    rebuildAllowedModsRef[0].run();
                            });
                    groupHeader.setHeight(20);
                    allowedModsList.addItem(groupHeader);

                    if (!isCollapsed) {
                        Checkbox groupCheckbox = new Checkbox(10, 0,
                                "Select All",
                                allSelected, value -> {
                                    for (ModOrganizer.ModInfo info : matchingLibs) {
                                        if (value) {
                                            ModHiderOptions.ALLOWED_MODS.add(info.id);
                                            enableDependencies.accept(info.id);
                                        } else {
                                            ModHiderOptions.ALLOWED_MODS.remove(info.id);
                                            disableDependents.accept(info.id);
                                        }
                                    }
                                    ConfigManager.save();
                                    if (rebuildAllowedModsRef[0] != null)
                                        rebuildAllowedModsRef[0].run();
                                });
                        allowedModsList.addItem(groupCheckbox);

                        for (ModOrganizer.ModInfo info : matchingLibs) {
                            boolean checked = ModHiderOptions.ALLOWED_MODS.contains(info.id);
                            String displayName = info.name + " (" + info.id + ")";
                            if (!info.dependents.isEmpty()) {
                                displayName += " §7[Used by: " + info.dependents.size() + "]";
                            }

                            Checkbox cb = new Checkbox(20, 0, displayName, checked, value -> {
                                if (value) {
                                    ModHiderOptions.ALLOWED_MODS.add(info.id);
                                    enableDependencies.accept(info.id);
                                } else {
                                    ModHiderOptions.ALLOWED_MODS.remove(info.id);
                                    disableDependents.accept(info.id);
                                }
                                ConfigManager.save();
                                if (rebuildAllowedModsRef[0] != null)
                                    rebuildAllowedModsRef[0].run();
                            });

                            final ModOrganizer.ModInfo finalInfo = info;
                            Widget checkboxWithTooltip = new Widget(0, 0, 0, 0) {
                                @Override
                                public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX,
                                        int mouseY,
                                        float partialTick) {
                                    cb.setX(getX());
                                    cb.setY(getY());
                                    cb.setWidth(getWidth());
                                    cb.render(graphics, mouseX, mouseY, partialTick);

                                    if (cb.isHovered() && !finalInfo.dependencies.isEmpty()) {
                                        currentTooltip = "Dependencies: " + String.join(", ", finalInfo.dependencies);
                                        tooltipX = mouseX;
                                        tooltipY = mouseY;
                                    }
                                }

                                @Override
                                public void updateHoverState(int mouseX, int mouseY) {
                                    cb.setX(getX());
                                    cb.setY(getY());
                                    cb.setWidth(getWidth());

                                    cb.updateHoverState(mouseX, mouseY);
                                    super.updateHoverState(mouseX, mouseY);
                                    if (!cb.isHovered()) {
                                        currentTooltip = null;
                                    }
                                }

                                @Override
                                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                                    return cb.mouseClicked(mouseX, mouseY, button);
                                }

                                @Override
                                public void tick() {
                                    cb.tick();
                                }
                            };
                            checkboxWithTooltip.setHeight(cb.getHeight());
                            checkboxWithTooltip.setWidth(cb.getWidth());
                            allowedModsList.addItem(checkboxWithTooltip);
                        }
                    }
                } else {
                    for (ModOrganizer.ModInfo info : matchingLibs) {
                        boolean checked = ModHiderOptions.ALLOWED_MODS.contains(info.id);
                        String displayName = info.name + " (" + info.id + ")";
                        if (!info.dependents.isEmpty()) {
                            displayName += " §7[Used by: " + info.dependents.size() + "]";
                        }

                        Checkbox cb = new Checkbox(0, 0, displayName, checked, value -> {
                            if (value) {
                                ModHiderOptions.ALLOWED_MODS.add(info.id);
                                enableDependencies.accept(info.id);
                            } else {
                                ModHiderOptions.ALLOWED_MODS.remove(info.id);
                                disableDependents.accept(info.id);
                            }
                            ConfigManager.save();
                            if (rebuildAllowedModsRef[0] != null)
                                rebuildAllowedModsRef[0].run();
                        });

                        final ModOrganizer.ModInfo finalInfo = info;
                        Widget checkboxWithTooltip = new Widget(0, 0, 0, 0) {
                            @Override
                            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick) {
                                cb.setX(getX());
                                cb.setY(getY());
                                cb.setWidth(getWidth());
                                cb.render(graphics, mouseX, mouseY, partialTick);

                                if (cb.isHovered() && !finalInfo.dependencies.isEmpty()) {
                                    currentTooltip = "Dependencies: " + String.join(", ", finalInfo.dependencies);
                                    tooltipX = mouseX;
                                    tooltipY = mouseY;
                                }
                            }

                            @Override
                            public void updateHoverState(int mouseX, int mouseY) {
                                cb.setX(getX());
                                cb.setY(getY());
                                cb.setWidth(getWidth());

                                cb.updateHoverState(mouseX, mouseY);
                                super.updateHoverState(mouseX, mouseY);
                                if (!cb.isHovered()) {
                                    currentTooltip = null;
                                }
                            }

                            @Override
                            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                                return cb.mouseClicked(mouseX, mouseY, button);
                            }

                            @Override
                            public void tick() {
                                cb.tick();
                            }
                        };
                        checkboxWithTooltip.setHeight(cb.getHeight());
                        checkboxWithTooltip.setWidth(cb.getWidth());
                        allowedModsList.addItem(checkboxWithTooltip);
                    }
                }
            }
        };

        allowedModsCard.addChild(new Widget(0, 0, 0, 0) {
            private String last = "";

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
            }

            @Override
            public void tick() {
                String now = modSearch.getText() == null ? "" : modSearch.getText();
                if (!now.equals(last)) {
                    last = now;
                    if (rebuildAllowedModsRef[0] != null)
                        rebuildAllowedModsRef[0].run();
                }
            }
        });

        rebuildAllowedModsRef[0].run();
    }

    private void updateCardVisibility() {
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
        if (currentTooltip != null && currentTooltip.length() > 0) {
            int tooltipWidth = font.width(currentTooltip) + 8;
            int tooltipXPos = this.tooltipX + 10;
            int tooltipYPos = (int) (this.tooltipY - scrollOffset) - 20;

            if (tooltipXPos + tooltipWidth > containerX + containerWidth) {
                tooltipXPos = this.tooltipX - tooltipWidth - 10;
            }
            if (tooltipYPos < containerY) {
                tooltipYPos = (int) (this.tooltipY - scrollOffset) + 10;
            }

            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos + 10 + 2,
                    0xE0000000);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos - 1,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos + 11, tooltipXPos + tooltipWidth + 2, tooltipYPos + 12,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos - 1, tooltipYPos + 12, Theme.ACCENT);
            graphics.fill(tooltipXPos + tooltipWidth + 1, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2,
                    tooltipYPos + 12, Theme.ACCENT);
            graphics.drawString(font, currentTooltip, tooltipXPos, tooltipYPos, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }
}
