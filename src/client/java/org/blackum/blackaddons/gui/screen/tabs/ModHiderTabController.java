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
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class ModHiderTabController extends SimpleTabController {
    private ResizableCard spoofModeCard;
    private ResizableCard hideModsCard;
    private ResizableCard disablePayloadsCard;
    private ResizableCard allowedModsCard;

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
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);

        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
        mods.sort((m1, m2) -> m1.getMetadata().getName().compareToIgnoreCase(m2.getMetadata().getName()));

        for (ModContainer mod : mods) {
            String id = mod.getMetadata().getId();
            String name = mod.getMetadata().getName();

            if (!search.isEmpty() && !id.toLowerCase().contains(search) && !name.toLowerCase().contains(search)) {
                continue;
            }

            ToggleSwitch toggle = new ToggleSwitch(0, 0, list.getWidth() - 20, name, id,
                    ConfigManager.data.modHiderAllowedMods.contains(id), value -> {
                        if (value) {
                            ConfigManager.data.modHiderAllowedMods.add(id);
                        } else {
                            ConfigManager.data.modHiderAllowedMods.remove(id);
                        }
                        ConfigManager.save();
                    });
            list.addItem(toggle);
        }
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
