package org.blackum.blackaddons.gui.screen.main.tabs;


import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.feature.ModOrganizer;
import org.blackum.blackaddons.gui.screen.feature.WaypointEditScreen;
import org.blackum.blackaddons.gui.screen.feature.WaypointGroupEditScreen;
import org.blackum.blackaddons.gui.screen.feature.WaypointActionEditScreen;
import org.blackum.blackaddons.gui.screen.feature.ChatActionEditScreen;
import org.blackum.blackaddons.gui.screen.feature.IrcScreen;
import org.blackum.blackaddons.gui.screen.feature.ImagePreviewScreen;
import org.blackum.blackaddons.gui.screen.feature.ProfileViewerScreen;
import org.blackum.blackaddons.gui.screen.feature.PartyFinderScreen;
import org.blackum.blackaddons.gui.screen.feature.PartyCreationScreen;
import org.blackum.blackaddons.gui.screen.feature.SoloLeaderboardScreen;
import org.blackum.blackaddons.gui.screen.debug.DemoScreen;
import org.blackum.blackaddons.gui.screen.debug.TestMenuScreen;
import com.google.gson.JsonObject;
import org.blackum.blackaddons.gui.screen.feature.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;
import org.blackum.blackaddons.feature.item.ItemDeserializer;
import org.blackum.blackaddons.common.model.SkyblockItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BackpackTabController extends ProfileTabController {
    private ListView listView;
    private final List<Integer> backpackOffsets = new ArrayList<>();

    public BackpackTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int startX = tab.getParent().getContentX();
        int startY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth();
        int height = tab.getParent().getContentHeight();

        JsonObject inventory = profileData.getAsJsonObject("inventory");
        if (inventory == null || !inventory.has("backpack_contents")) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No Backpack data", Label.Style.TITLE));
            return;
        }

        JsonObject backpacks = inventory.getAsJsonObject("backpack_contents");
        if (backpacks.keySet().isEmpty()) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No backpacks found", Label.Style.TITLE));
            return;
        }

        List<BackpackSection> sections = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : sortedBackpacks(backpacks)) {
            JsonObject bp = entry.getValue().getAsJsonObject();

            List<SkyblockItem> items;
            if (bp.has("data")) {
                items = ItemDeserializer.deserializeList(bp.get("data").getAsString());
            } else if (bp.has("skycrypt_items") && bp.get("skycrypt_items").isJsonArray()) {
                items = ItemDeserializer.deserializeSkyCryptItems(bp.getAsJsonArray("skycrypt_items"));
            } else {
                continue;
            }
            if (items.isEmpty())
                continue;

            sections.add(new BackpackSection(backpackDisplayNumber(entry.getKey()), items));
        }

        if (sections.isEmpty()) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No backpacks found", Label.Style.TITLE));
            return;
        }

        int btnWidth = 30;
        int btnHeight = 20;
        int btnGap = 5;
        int totalBtnWidth = sections.size() * btnWidth + (sections.size() - 1) * btnGap;
        int btnX = startX + (width - totalBtnWidth) / 2;

        for (int i = 0; i < sections.size(); i++) {
            final int bpIdx = i;
            Button btn = new Button(btnX, startY, btnWidth, btnHeight, String.valueOf(sections.get(i).displayNumber), () -> {
                if (listView != null && bpIdx < backpackOffsets.size()) {
                    listView.scrollTo(backpackOffsets.get(bpIdx));
                }
            });
            tab.addWidget(btn);
            btnX += btnWidth + btnGap;
        }

        listView = new ListView(startX, startY + btnHeight + 10, width, height - btnHeight - 10);
        tab.addWidget(listView);

        backpackOffsets.clear();
        int currentOffset = 0;

        for (BackpackSection section : sections) {
            backpackOffsets.add(currentOffset);

            Label bpLabel = new Label(0, 0, "Backpack " + section.displayNumber, Label.Style.TITLE);
            listView.addItem(bpLabel);
            currentOffset += bpLabel.getHeight() + listView.getItemSpacing();

            ItemGridWidget grid = new ItemGridWidget(0, 0, 9, section.items);
            listView.addItem(grid);
            currentOffset += grid.getHeight() + listView.getItemSpacing();

            listView.addItem(new Widget(0, 0, 0, 20) {
                @Override
                public void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                        float partialTick) {
                }
            });
            currentOffset += 20 + listView.getItemSpacing();
        }
    }

    private static List<Map.Entry<String, com.google.gson.JsonElement>> sortedBackpacks(JsonObject backpacks) {
        List<Map.Entry<String, com.google.gson.JsonElement>> entries = new ArrayList<>(backpacks.entrySet());
        entries.sort(Comparator.comparingInt(entry -> backpackSortIndex(entry.getKey())));
        return entries;
    }

    private static int backpackSortIndex(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
        }

        int underscore = key.lastIndexOf('_');
        if (underscore >= 0 && underscore + 1 < key.length()) {
            try {
                return Integer.parseInt(key.substring(underscore + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int backpackDisplayNumber(String key) {
        int sortIndex = backpackSortIndex(key);
        return sortIndex == Integer.MAX_VALUE ? 1 : sortIndex + 1;
    }

    private static class BackpackSection {
        private final int displayNumber;
        private final List<SkyblockItem> items;

        private BackpackSection(int displayNumber, List<SkyblockItem> items) {
            this.displayNumber = displayNumber;
            this.items = items;
        }
    }
}
