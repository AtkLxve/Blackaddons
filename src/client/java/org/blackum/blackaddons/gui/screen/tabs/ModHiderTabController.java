package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.modhider.SpoofMode;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import org.blackum.blackaddons.gui.screen.ModOrganizer;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;

public class ModHiderTabController extends SimpleTabController {
    private ResizableCard spoofModeCard;
    private ResizableCard hideModsCard;
    private ResizableCard disablePayloadsCard;
    private ResizableCard allowedModsCard;
    private final Map<String, Boolean> collapsedGroups = new HashMap<>();

    public ModHiderTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab modHiderTab) {
        if (!ConfigManager.data.useCardLayout) {
            initModHiderTabLegacy(modHiderTab);
            return;
        }

        int contentX = modHiderTab.getParent().getContentX();
        int contentY = modHiderTab.getParent().getContentY();
        int contentWidth = modHiderTab.getParent().getContentWidth();

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("spoofMode", "hideMods", "disablePayloads", "allowedMods");
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

            hideModsCard = createHideModsCard(col1X, currentY);
            currentY += hideModsCard.getHeight() + Theme.CARD_SPACING;

            disablePayloadsCard = createDisablePayloadsCard(col1X, currentY);
            currentY += disablePayloadsCard.getHeight() + Theme.CARD_SPACING;

            allowedModsCard = createAllowedModsCard(col1X, currentY);
        } else {
            int currentY1 = containerY + 20;
            int currentY2 = containerY + 20;

            spoofModeCard = createSpoofModeCard(col1X, currentY1);
            currentY1 += spoofModeCard.getHeight() + Theme.CARD_SPACING;

            allowedModsCard = createAllowedModsCard(col1X, currentY1);

            hideModsCard = createHideModsCard(col2X, currentY2);
            currentY2 += hideModsCard.getHeight() + Theme.CARD_SPACING;

            disablePayloadsCard = createDisablePayloadsCard(col2X, currentY2);
            currentY2 += disablePayloadsCard.getHeight() + Theme.CARD_SPACING;
        }

        modHiderCardContainer.addCard(spoofModeCard);
        modHiderCardContainer.addCard(hideModsCard);
        modHiderCardContainer.addCard(disablePayloadsCard);
        modHiderCardContainer.addCard(allowedModsCard);
    }

    private void initModHiderTabLegacy(TabPanel.Tab modHiderTab) {
        int contentX = modHiderTab.getParent().getContentX();
        int contentY = modHiderTab.getParent().getContentY();
        int contentWidth = modHiderTab.getParent().getContentWidth() - 20;
        int currentY = contentY;

        SpoofMode mode = ConfigManager.data.modHiderSpoofMode;
        boolean isCustom = mode == SpoofMode.CUSTOM;
        boolean isModdedOrCustom = mode == SpoofMode.MODDED || mode == SpoofMode.CUSTOM;

        Label spoofLabel = new Label(contentX, currentY, "Spoof Mode", Label.Style.TITLE);
        modHiderTab.addWidget(spoofLabel);
        currentY += 25;

        List<String> spoofModes = List.of("VANILLA", "MODDED", "CUSTOM", "OFF");
        Dropdown spoofModeDropdown = new Dropdown(contentX, currentY, contentWidth,
                "Spoof Mode", spoofModes, selected -> {
                    try {
                        ConfigManager.data.modHiderSpoofMode = SpoofMode
                                .valueOf(selected.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        ConfigManager.data.modHiderSpoofMode = SpoofMode.VANILLA;
                    }
                    ConfigManager.save();
                    screen.init();
                });
        spoofModeDropdown.setSelectedOption(ConfigManager.data.modHiderSpoofMode.name());
        modHiderTab.addWidget(spoofModeDropdown);
        currentY += 45;

        if (isCustom) {
            Label customClientLabel = new Label(contentX, currentY, "Custom Client Brand", Label.Style.TITLE);
            modHiderTab.addWidget(customClientLabel);
            currentY += 25;

            TextField customClient = new TextField(contentX, currentY, contentWidth - 90, "fabric");
            customClient.setText(ConfigManager.data.modHiderCustomClient == null ? "fabric"
                    : ConfigManager.data.modHiderCustomClient);
            modHiderTab.addWidget(customClient);

            Button applyCustomClient = new Button(contentX + contentWidth - 80, currentY, 70, "Apply", () -> {
                ConfigManager.data.modHiderCustomClient = customClient.getText().isBlank() ? "fabric"
                        : customClient.getText();
                ConfigManager.save();
            });
            modHiderTab.addWidget(applyCustomClient);
            currentY += 45;

            ToggleSwitch hideModsToggle = new ToggleSwitch(contentX, currentY, contentWidth,
                    "Hide Mods",
                    "Prevent servers from reading mod info",
                    ConfigManager.data.modHiderHideMods, value -> {
                        ConfigManager.data.modHiderHideMods = value;
                        ConfigManager.save();
                    });
            modHiderTab.addWidget(hideModsToggle);
            currentY += 50;

            ToggleSwitch disablePayloadsToggle = new ToggleSwitch(contentX, currentY, contentWidth,
                    "Disable Custom Payloads",
                    "Block custom payload channels unless allowed",
                    ConfigManager.data.modHiderDisableCustomPayloads, value -> {
                        ConfigManager.data.modHiderDisableCustomPayloads = value;
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
                    ConfigManager.data.modHiderAllowedCustomPayloadChannels.add(val);
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
                if (channelsList != null
                        && lastChannelSize != ConfigManager.data.modHiderAllowedCustomPayloadChannels
                                .size()) {
                    lastChannelSize = ConfigManager.data.modHiderAllowedCustomPayloadChannels.size();
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
            public void render(GuiGraphics g, int mx, int my, float p) {
            }
        });
    }

    private ResizableCard createSpoofModeCard(int x, int y) {
        spoofModeCard = screen.createResizableCard("spoofMode", x, y, 300, 150, "Spoof Mode");

        int contentX = spoofModeCard.getContentX();
        int contentY = spoofModeCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Control how your client appears to servers", Label.Style.BODY);
        spoofModeCard.addChild(description);

        List<String> spoofModes = List.of("VANILLA", "MODDED", "CUSTOM", "OFF");
        Dropdown spoofModeDropdown = new Dropdown(contentX, contentY + 30, 260,
                "Spoof Mode", spoofModes, selected -> {
                    try {
                        ConfigManager.data.modHiderSpoofMode = SpoofMode
                                .valueOf(selected.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        ConfigManager.data.modHiderSpoofMode = SpoofMode.VANILLA;
                    }
                    ConfigManager.save();
                });
        spoofModeDropdown.setHeight(24);
        spoofModeDropdown.setSelectedOption(ConfigManager.data.modHiderSpoofMode.name());
        spoofModeCard.addChild(spoofModeDropdown);

        spoofModeCard.updateLayout();
        return spoofModeCard;
    }

    private ResizableCard createHideModsCard(int x, int y) {
        hideModsCard = screen.createResizableCard("hideMods", x, y, 300, 110, "Hide Mods");

        int contentX = hideModsCard.getContentX();
        int contentY = hideModsCard.getContentY();

        ToggleSwitch hideModsToggle = new ToggleSwitch(contentX, contentY, 260,
                "Hide Mods",
                "Prevent servers from reading mod info",
                ConfigManager.data.modHiderHideMods, value -> {
                    ConfigManager.data.modHiderHideMods = value;
                    ConfigManager.save();
                });
        hideModsCard.addChild(hideModsToggle);

        hideModsCard.updateLayout();
        return hideModsCard;
    }

    private ResizableCard createDisablePayloadsCard(int x, int y) {
        disablePayloadsCard = screen.createResizableCard("disablePayloads", x, y, 300, 120, "Disable Custom Payloads");

        int contentX = disablePayloadsCard.getContentX();
        int contentY = disablePayloadsCard.getContentY();

        ToggleSwitch disablePayloadsToggle = new ToggleSwitch(contentX, contentY, 260,
                "Disable Custom Payloads",
                "Block custom payload channels unless allowed",
                ConfigManager.data.modHiderDisableCustomPayloads, value -> {
                    ConfigManager.data.modHiderDisableCustomPayloads = value;
                    ConfigManager.save();
                });
        disablePayloadsCard.addChild(disablePayloadsToggle);

        disablePayloadsCard.updateLayout();
        return disablePayloadsCard;
    }

    private ResizableCard createAllowedModsCard(int x, int y) {
        allowedModsCard = screen.createResizableCard("allowedMods", x, y, 300, 320, "Allowed Mods");

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
            public void render(GuiGraphics g, int mx, int my, float p) {
            }
        });

        allowedModsCard.updateLayout();
        return allowedModsCard;
    }

    private void rebuildAllowedModsList(ListView list, TextField searchField) {
        list.clearItems();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        ModOrganizer.OrganizedMods organizedMods = ModOrganizer.organizeMods();

        Consumer<String> enableDependencies = modId -> {
            ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
            if (info != null) {
                for (String depId : info.dependencies) {
                    ConfigManager.data.modHiderAllowedMods.add(depId);
                }
            }
        };

        Consumer<String> disableDependents = modId -> {
            ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
            if (info != null) {
                for (String dependentId : info.dependents) {
                    ConfigManager.data.modHiderAllowedMods.remove(dependentId);
                }
            }
        };

        Label modsSectionLabel = new Label(0, 0, ChatFormatting.GOLD + "Mods", Label.Style.BODY);
        modsSectionLabel.setHeight(20);
        list.addItem(modsSectionLabel);

        for (ModOrganizer.ModGroup group : organizedMods.modGroups) {
            addModGroupToList(list, group, query, enableDependencies, disableDependents, searchField);
        }

        Label libsSectionLabel = new Label(0, 0, ChatFormatting.GOLD + "Libraries", Label.Style.BODY);
        libsSectionLabel.setHeight(20);
        list.addItem(libsSectionLabel);

        for (ModOrganizer.ModGroup group : organizedMods.libraryGroups) {
            addModGroupToList(list, group, query, enableDependencies, disableDependents, searchField);
        }
    }

    private void addModGroupToList(ListView list, ModOrganizer.ModGroup group, String query,
            Consumer<String> enableDependencies,
            Consumer<String> disableDependencies,
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

        if (group.mods.size() > 1 || !query.isEmpty()) {
            String groupKey = "mod_" + group.groupName;
            boolean isCollapsed = collapsedGroups.getOrDefault(groupKey, true);
            if (!query.isEmpty())
                isCollapsed = false;

            boolean allSelected = matchingMods.stream()
                    .allMatch(m -> ConfigManager.data.modHiderAllowedMods.contains(m.id));

            String arrow = isCollapsed ? "▶" : "▼";
            Button groupHeader = new Button(0, 0, 0,
                    arrow + " " + ChatFormatting.AQUA + group.groupName + " (" + matchingMods.size() + ")",
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
                            ConfigManager.data.modHiderAllowedMods.add(info.id);
                            enableDependencies.accept(info.id);
                        } else {
                            ConfigManager.data.modHiderAllowedMods.remove(info.id);
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
            Consumer<String> enableDependencies,
            Consumer<String> disableDependencies,
            TextField searchField) {
        boolean checked = ConfigManager.data.modHiderAllowedMods.contains(info.id);
        String displayName = info.name + " (" + info.id + ")";
        if (!info.dependents.isEmpty())
            displayName += " " + ChatFormatting.GRAY + "[Used by: " + info.dependents.size() + "]";

        Checkbox cb = new Checkbox(0, 0, displayName, checked, value -> {
            if (value) {
                ConfigManager.data.modHiderAllowedMods.add(info.id);
                enableDependencies.accept(info.id);
            } else {
                ConfigManager.data.modHiderAllowedMods.remove(info.id);
                disableDependencies.accept(info.id);
            }
            ConfigManager.save();
            rebuildAllowedModsList(list, searchField);
        });

        Widget wrapper = new Widget(0, 0, 0, 0) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float p) {
                cb.setX(getX());
                cb.setY(getY());
                cb.setWidth(getWidth());
                cb.render(g, mx, my, p);
            }

            @Override
            public void updateHoverState(int mx, int my) {
                cb.setX(getX());
                cb.setY(getY());
                cb.setWidth(getWidth());
                cb.updateHoverState(mx, my);
                super.updateHoverState(mx, my);
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

    private void rebuildChannelsList(ListView list) {
        list.clearItems();
        for (String channel : ConfigManager.data.modHiderAllowedCustomPayloadChannels) {
            GridRow row = new GridRow(list.getWidth() - 20, 20);
            row.addChild(new Label(0, 5, channel, Label.Style.BODY), 0);
            row.addChild(new Button(0, 0, 40, "Del", () -> {
                ConfigManager.data.modHiderAllowedCustomPayloadChannels.remove(channel);
                ConfigManager.save();
                rebuildChannelsList(list);
            }), list.getWidth() - 60);
            list.addItem(row);
        }
    }
}
