package org.blackum.blackaddons.gui.screen;

import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.ColorPicker;
import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.ToggleSwitch;
import org.blackum.blackaddons.gui.widget.Dropdown;
import org.blackum.blackaddons.gui.widget.TextField;
import org.blackum.blackaddons.gui.widget.Button;
import org.blackum.blackaddons.gui.widget.ListView;
import org.blackum.blackaddons.gui.widget.Checkbox;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.widget.Widget;
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
        int currentY = contentY;

        modHiderTab.addWidget(new Label(contentX, currentY, "Mod Hider", Label.Style.TITLE));
        modHiderTab.addWidget(new Label(contentX, currentY + 25,
                "Hide/spoof client & mod info sent to servers (ported from ClientSpoofer).", Label.Style.BODY));
        currentY += 50;

        Label spoofModeLabel = new Label(contentX, currentY, "Spoof Mode", Label.Style.BODY);
        modHiderTab.addWidget(spoofModeLabel);
        currentY += 20;

        List<String> spoofModes = List.of("VANILLA", "MODDED", "CUSTOM", "OFF");
        Dropdown spoofModeDropdown = new Dropdown(contentX, currentY, 200,
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
        modHiderTab.addWidget(spoofModeDropdown);
        currentY += 50;

        Label customClientLabel = new Label(contentX, currentY, "Custom Client Brand (CUSTOM mode)", Label.Style.BODY);
        modHiderTab.addWidget(customClientLabel);
        TextField customClient = new TextField(contentX, currentY + 20, 200, "fabric");
        customClient.setText(ModHiderOptions.CUSTOM_CLIENT == null ? "fabric" : ModHiderOptions.CUSTOM_CLIENT);
        modHiderTab.addWidget(customClient);
        Button applyCustomClient = new Button(contentX + 210, currentY + 20, 120, "Apply", () -> {
            ModHiderOptions.CUSTOM_CLIENT = customClient.getText().isBlank() ? "fabric" : customClient.getText();
            ConfigManager.save();
        });
        modHiderTab.addWidget(applyCustomClient);
        currentY += 60;

        ToggleSwitch hideModsToggle = new ToggleSwitch(contentX, currentY, contentWidth - 10,
                "Hide Mods",
                "When enabled, servers can't read your modded translations and mod list-related info.",
                ModHiderOptions.HIDE_MODS, value -> {
                    ModHiderOptions.HIDE_MODS = value;
                    ConfigManager.save();
                });
        modHiderTab.addWidget(hideModsToggle);
        currentY += 50;

        ToggleSwitch disablePayloadsToggle = new ToggleSwitch(contentX, currentY, contentWidth - 10,
                "Disable Custom Payloads",
                "Blocks most custom payload channels unless explicitly allowed below. When enabled, only channels in the allowed list below will be permitted.",
                ModHiderOptions.DISABLE_CUSTOM_PAYLOADS, value -> {
                    ModHiderOptions.DISABLE_CUSTOM_PAYLOADS = value;
                    ConfigManager.save();
                });
        modHiderTab.addWidget(disablePayloadsToggle);
        currentY += 60;

        Label channelsLabel = new Label(contentX, currentY, "Allowed Custom Payload Channels (CUSTOM)",
                Label.Style.BODY);
        modHiderTab.addWidget(channelsLabel);
        currentY += 20;

        TextField channelField = new TextField(contentX, currentY, 260, "example: hypixel");
        modHiderTab.addWidget(channelField);
        Button addChannel = new Button(contentX + 270, currentY, 90, "Add", () -> {
            String val = channelField.getText() == null ? "" : channelField.getText().trim();
            if (!val.isBlank()) {
                ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.add(val);
                channelField.setText("");
                ConfigManager.save();
            }
        });
        modHiderTab.addWidget(addChannel);
        currentY += 35;

        ListView channelsList = new ListView(contentX, currentY, Math.min(360, contentWidth), 110);
        modHiderTab.addWidget(channelsList);

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

        modHiderTab.addWidget(new Widget(0, 0, 0, 0) {
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

                boolean isCustom = ModHiderOptions.SPOOF_MODE == SpoofMode.CUSTOM;
                channelsLabel.setVisible(isCustom);
                channelField.setVisible(isCustom);
                addChannel.setVisible(isCustom);
                channelsList.setVisible(isCustom);
            }
        });

        rebuildChannelsRef[0].run();
        currentY += 130;

        Label allowedModsLabel = new Label(contentX, currentY, "Allowed Mods (MODDED/CUSTOM)", Label.Style.BODY);
        modHiderTab.addWidget(allowedModsLabel);
        currentY += 20;

        TextField modSearch = new TextField(contentX, currentY, 200, "Search mods...");
        modHiderTab.addWidget(modSearch);
        currentY += 40;

        ListView allowedModsList = new ListView(contentX, currentY, contentWidth - 20, 180);
        modHiderTab.addWidget(allowedModsList);

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

        modHiderTab.addWidget(new Widget(0, 0, 0, 0) {
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

                SpoofMode mode = ModHiderOptions.SPOOF_MODE;
                boolean isModdedOrCustom = mode == SpoofMode.MODDED || mode == SpoofMode.CUSTOM;

                allowedModsLabel.setVisible(isModdedOrCustom);
                modSearch.setVisible(isModdedOrCustom);
                allowedModsList.setVisible(isModdedOrCustom);
            }
        });

        rebuildAllowedModsRef[0].run();
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
            // Border (top, bottom, left, right)
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
