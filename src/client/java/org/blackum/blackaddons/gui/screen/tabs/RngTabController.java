package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.util.BotIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RngTabController extends ProfileTabController {

    private Map<String, JsonObject> rngRunCounts = null;
    private Map<String, Double> rngPrices = null;
    private Map<String, Integer> rngChestCosts = null;
    private Map<String, List<String>> rngCategories = null;
    private Map<String, List<String>> rngItems = null;
    private Map<String, String> rngItemIds = null;
    private List<String> rngGlobalDrops = null;
    private Map<String, Map<String, Integer>> rngDropCounts = null;

    private String currentRngCategory = null;
    private String currentRngSubcategory = null;

    private Dropdown rngCategoryDropdown;
    private Dropdown rngSubcategoryDropdown;
    private ListView rngItemsList;
    private Widget rngStatsBar;

    private final String playerName;

    public RngTabController(ProfileViewerScreen screen, JsonObject profileData, String playerName) {
        super(screen, profileData);
        this.playerName = playerName;
    }

    @Override
    public void init(TabPanel.Tab tab) {
        if (rngDropCounts != null) {
            buildRngTabUI(tab);
            if (currentRngSubcategory != null) {
                updateRngItemsList();
            }
            return;
        }

        BotIntegration.getRngData(playerName).thenAccept(json -> {
            if (json == null || json.has("error")) {
                Minecraft.getInstance().execute(() -> {
                    tab.widgets.clear();
                    ListView list = new ListView(tab.getParent().getContentX(), tab.getParent().getContentY(),
                            tab.getParent().getContentWidth(), tab.getParent().getMaxContentHeight());
                    tab.addWidget(list);

                    String errorMsg = "Failed to fetch data from bot.";
                    if (json != null && json.has("error")) {
                        errorMsg = json.get("error").getAsString();
                    }

                    addInfoRow(list, "§cError", "");
                    addInfoRow(list, errorMsg, "");
                });
                return;
            }

            if (json.has("data")) {
                parseRngData(json.getAsJsonObject("data"));
                Minecraft.getInstance().execute(() -> {
                    buildRngTabUI(tab);
                });
            }
        });

        ListView loadingList = new ListView(tab.getParent().getContentX(), tab.getParent().getContentY(),
                tab.getParent().getContentWidth(), tab.getParent().getMaxContentHeight());
        tab.addWidget(loadingList);
        addInfoRow(loadingList, "Loading RNG data...", "");
    }

    private void parseRngData(JsonObject data) {
        if (data.has("drops")) {
            JsonObject drops = data.getAsJsonObject("drops");
            rngDropCounts = new java.util.HashMap<>();
            for (String cat : drops.keySet()) {
                if (cat.startsWith("_"))
                    continue;
                JsonObject catData = drops.getAsJsonObject(cat);
                Map<String, Integer> itemMap = new java.util.HashMap<>();
                for (String item : catData.keySet()) {
                    itemMap.put(item, catData.get(item).getAsInt());
                }
                rngDropCounts.put(cat, itemMap);
            }
        }

        if (data.has("prices")) {
            JsonObject prices = data.getAsJsonObject("prices");
            rngPrices = new java.util.HashMap<>();
            for (String id : prices.keySet()) {
                rngPrices.put(id, prices.get(id).getAsDouble());
            }
        }

        if (data.has("run_counts")) {
            JsonObject runs = data.getAsJsonObject("run_counts");
            rngRunCounts = new java.util.HashMap<>();
            for (String floor : runs.keySet()) {
                rngRunCounts.put(floor, runs.getAsJsonObject(floor));
            }
        }

        if (data.has("chest_costs")) {
            JsonObject costs = data.getAsJsonObject("chest_costs");
            rngChestCosts = new java.util.HashMap<>();
            for (String item : costs.keySet()) {
                rngChestCosts.put(item, costs.get(item).getAsInt());
            }
        }

        if (data.has("categories")) {
            JsonObject cats = data.getAsJsonObject("categories");
            rngCategories = new java.util.LinkedHashMap<>();
            for (String cat : cats.keySet()) {
                JsonArray subs = cats.getAsJsonArray(cat);
                List<String> subList = new ArrayList<>();
                for (JsonElement sub : subs) {
                    subList.add(sub.getAsString());
                }
                rngCategories.put(cat, subList);
            }
        }

        if (data.has("items")) {
            JsonObject items = data.getAsJsonObject("items");
            rngItems = new java.util.HashMap<>();
            for (String sub : items.keySet()) {
                JsonArray itemArray = items.getAsJsonArray(sub);
                List<String> itemList = new ArrayList<>();
                for (JsonElement item : itemArray) {
                    itemList.add(item.getAsString());
                }
                rngItems.put(sub, itemList);
            }
        }

        if (data.has("global_drops")) {
            JsonArray globals = data.getAsJsonArray("global_drops");
            rngGlobalDrops = new ArrayList<>();
            for (JsonElement item : globals) {
                rngGlobalDrops.add(item.getAsString());
            }
        }

        if (data.has("item_ids")) {
            JsonObject ids = data.getAsJsonObject("item_ids");
            rngItemIds = new java.util.HashMap<>();
            for (String itemName : ids.keySet()) {
                rngItemIds.put(itemName, ids.get(itemName).getAsString());
            }
        }
    }

    private void buildRngTabUI(TabPanel.Tab tab) {
        tab.widgets.clear();

        int cy = tab.getParent().getContentY();
        int cx = tab.getParent().getContentX();
        int w = tab.getParent().getContentWidth();

        List<String> categoryNames = new ArrayList<>(rngCategories.keySet());
        rngCategoryDropdown = new Dropdown(cx, cy, 150, 20, "Category", categoryNames, (selected) -> {
            currentRngCategory = selected;
            currentRngSubcategory = null;
            updateRngSubcategoryDropdown();
            updateRngItemsList();
        });
        tab.addWidget(rngCategoryDropdown);

        rngSubcategoryDropdown = new Dropdown(cx + 155, cy, 200, 20, "Subcategory", new ArrayList<>(), (selected) -> {
            currentRngSubcategory = selected;
            updateRngItemsList();
        });
        tab.addWidget(rngSubcategoryDropdown);

        rngItemsList = new ListView(cx, cy + 30, w - 10, tab.getParent().getMaxContentHeight() - 85);
        rngItemsList.setItemSpacing(5);
        tab.addWidget(rngItemsList);

        int statsBarY = tab.getParent().getY() + tab.getParent().getHeight() - 30;
        rngStatsBar = new Widget(cx, statsBarY, w - 10, 25) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                RenderHelper.renderRoundedRect(graphics, x, y, width, height,
                        Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);

                String stats = calculateRngStats();
                graphics.drawString(Minecraft.getInstance().font, stats, x + 10, y + 8, Theme.ACCENT);
            }
        };
        tab.addWidget(rngStatsBar);

        addInfoRow(rngItemsList, "Select a category above", "");
    }

    private void updateRngSubcategoryDropdown() {
        if (currentRngCategory == null || rngCategories == null) {
            return;
        }

        List<String> subs = rngCategories.get(currentRngCategory);
        if (subs != null && !subs.isEmpty()) {
            rngSubcategoryDropdown.setOptions(subs);
            currentRngSubcategory = subs.get(0);
            rngSubcategoryDropdown.setSelectedOption(currentRngSubcategory);

            if (subs.size() == 1) {
                rngSubcategoryDropdown.setVisible(false);
            } else {
                rngSubcategoryDropdown.setVisible(true);
            }
        } else {
            rngSubcategoryDropdown.setOptions(new ArrayList<>());
        }
    }

    private void updateRngItemsList() {
        if (rngItemsList == null)
            return;

        rngItemsList.clearItems();

        if (currentRngSubcategory == null || rngItems == null) {
            addInfoRow(rngItemsList, "Select a subcategory", "");
            return;
        }

        List<String> items = rngItems.get(currentRngSubcategory);
        if (items == null || items.isEmpty()) {
            addInfoRow(rngItemsList, "No items in this category", "");
            return;
        }

        List<String> allItems = new ArrayList<>(items);
        if ("Dungeons".equals(currentRngCategory) && rngGlobalDrops != null) {
            for (String global : rngGlobalDrops) {
                if (!allItems.contains(global)) {
                    allItems.add(global);
                }
            }
        }

        for (String itemName : allItems) {
            int count = getRngDropCount(currentRngSubcategory, itemName);
            rngItemsList.addItem(new RngItemRow(rngItemsList.getWidth(), itemName, count, currentRngSubcategory));
        }
    }

    private int getRngDropCount(String subcategory, String item) {
        if (rngDropCounts == null)
            return 0;

        if (rngGlobalDrops != null && rngGlobalDrops.contains(item)) {
            subcategory = "Global";
        }

        Map<String, Integer> catMap = rngDropCounts.get(subcategory);
        if (catMap == null)
            return 0;
        return catMap.getOrDefault(item, 0);
    }

    private String calculateRngStats() {
        if (currentRngSubcategory == null)
            return "No category selected";

        double totalProfit = 0;
        int totalItems = 0;

        List<String> items = rngItems != null ? rngItems.get(currentRngSubcategory) : null;
        if (items != null) {
            List<String> allItems = new ArrayList<>(items);
            if ("Dungeons".equals(currentRngCategory) && rngGlobalDrops != null) {
                for (String global : rngGlobalDrops) {
                    if (!allItems.contains(global)) {
                        allItems.add(global);
                    }
                }
            }

            for (String item : allItems) {
                int count = getRngDropCount(currentRngSubcategory, item);
                if (count > 0) {
                    totalItems += count;
                    double price = getItemPrice(item);
                    int chestCost = getChestCost(item);
                    double profit = Math.max(0, price - chestCost);
                    totalProfit += profit * count;
                }
            }
        }

        String profitStr = formatNumber(totalProfit);
        String stats = String.format("§6Total Profit: §f%s §7| §6Items: §f%d", profitStr, totalItems);

        if ("Dungeons".equals(currentRngCategory) && rngRunCounts != null) {
            String floorKey = getFloorKey(currentRngSubcategory);
            if (floorKey != null && rngRunCounts.containsKey(floorKey)) {
                JsonObject runs = rngRunCounts.get(floorKey);
                int totalRuns = runs.has("normal") ? runs.get("normal").getAsInt() : 0;
                totalRuns += runs.has("master") ? runs.get("master").getAsInt() : 0;

                if (totalRuns > 0) {
                    double profitPerRun = totalProfit / totalRuns;
                    stats += String.format(" §7| §6Runs: §f%,d §7| §6Profit/Run: §f%s", totalRuns,
                            formatNumber(profitPerRun));
                }
            }
        }

        return stats;
    }

    private String getFloorKey(String subcategory) {
        if (subcategory == null)
            return null;

        if (subcategory.contains("Bonzo"))
            return "Floor 1 (Bonzo)";
        if (subcategory.contains("Scarf"))
            return "Floor 2 (Scarf)";
        if (subcategory.contains("Professor"))
            return "Floor 3 (Professor)";
        if (subcategory.contains("Thorn"))
            return "Floor 4 (Thorn)";
        if (subcategory.contains("Livid"))
            return "Floor 5 (Livid)";
        if (subcategory.contains("Sadan"))
            return "Floor 6 (Sadan)";
        if (subcategory.contains("Necron"))
            return "Floor 7 (Necron)";
        return null;
    }

    private double getItemPrice(String itemName) {
        if (rngPrices == null || rngItemIds == null)
            return 0;

        String itemId = rngItemIds.get(itemName);
        if (itemId == null)
            return 0;

        return rngPrices.getOrDefault(itemId, 0.0);
    }

    private int getChestCost(String itemName) {
        if (rngChestCosts == null)
            return 0;
        return rngChestCosts.getOrDefault(itemName, 0);
    }

    private void updateRngDropCountLocal(String category, String item, int newCount) {
        if (rngDropCounts == null) {
            rngDropCounts = new java.util.HashMap<>();
        }
        rngDropCounts.putIfAbsent(category, new java.util.HashMap<>());
        rngDropCounts.get(category).put(item, newCount);
    }

    private class RngItemRow extends Widget {
        private final String itemName;
        private final String subcategory;
        private int count;
        private Animation hoverAnimation;
        private boolean expanded = false;

        private Button minusBtn;
        private Button plusBtn;
        private Button setBtn;

        public RngItemRow(int width, String itemName, int count, String subcategory) {
            super(0, 0, width, 30);
            this.itemName = itemName;
            this.count = count;
            this.subcategory = subcategory;
            this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);

            this.minusBtn = new Button(0, 0, 30, 18, "-", () -> handleDecrement());
            this.plusBtn = new Button(0, 0, 30, 18, "+", () -> handleIncrement());
            this.setBtn = new Button(0, 0, 50, 18, "Set", () -> handleSet());
        }

        private void handleIncrement() {
            String cat = (RngTabController.this.rngGlobalDrops != null
                    && RngTabController.this.rngGlobalDrops.contains(itemName)) ? "Global" : subcategory;
            BotIntegration.updateRngDrop(RngTabController.this.playerName, cat, itemName, "increment", null)
                    .thenAccept(success -> {
                        if (success) {
                            Minecraft.getInstance().execute(() -> {
                                count++;
                                updateRngDropCountLocal(cat, itemName, count);
                            });
                        }
                    });
        }

        private void handleDecrement() {
            if (count <= 0)
                return;
            String cat = (RngTabController.this.rngGlobalDrops != null
                    && RngTabController.this.rngGlobalDrops.contains(itemName)) ? "Global" : subcategory;
            BotIntegration.updateRngDrop(RngTabController.this.playerName, cat, itemName, "decrement", null)
                    .thenAccept(success -> {
                        if (success) {
                            Minecraft.getInstance().execute(() -> {
                                count = Math.max(0, count - 1);
                                updateRngDropCountLocal(cat, itemName, count);
                            });
                        }
                    });
        }

        private void handleSet() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screens.Screen(
                    net.minecraft.network.chat.Component.literal("Set RNG Count")) {

                private net.minecraft.client.gui.components.EditBox inputBox;

                @Override
                protected void init() {
                    super.init();

                    inputBox = new net.minecraft.client.gui.components.EditBox(
                            mc.font,
                            width / 2 - 100,
                            height / 2 - 10,
                            200,
                            20,
                            net.minecraft.network.chat.Component.literal("Count"));
                    inputBox.setMaxLength(10);
                    inputBox.setValue(String.valueOf(count));
                    inputBox.setFilter(s -> s.matches("[0-9]*"));
                    this.addRenderableWidget(inputBox);

                    this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            net.minecraft.network.chat.Component.literal("Confirm"),
                            btn -> {
                                try {
                                    int newCount = Integer.parseInt(inputBox.getValue());
                                    String cat = (RngTabController.this.rngGlobalDrops != null && RngTabController.this.rngGlobalDrops.contains(itemName))
                                            ? "Global"
                                            : subcategory;
                                    BotIntegration.updateRngDrop(RngTabController.this.playerName, cat, itemName, "set", newCount)
                                            .thenAccept(success -> {
                                                if (success) {
                                                    mc.execute(() -> {
                                                        count = newCount;
                                                        updateRngDropCountLocal(cat, itemName, newCount);
                                                        mc.setScreen(RngTabController.this.screen);
                                                    });
                                                }
                                            });
                                } catch (NumberFormatException e) {
                                    mc.setScreen(RngTabController.this.screen);
                                }
                            }).bounds(width / 2 - 100, height / 2 + 20, 95, 20).build());

                    this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            net.minecraft.network.chat.Component.literal("Cancel"),
                            btn -> mc.setScreen(RngTabController.this.screen))
                            .bounds(width / 2 + 5, height / 2 + 20, 95, 20).build());

                    setInitialFocus(inputBox);
                }

                @Override
                public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    super.render(graphics, mouseX, mouseY, partialTick);
                    graphics.drawCenteredString(mc.font, "Set count for " + itemName, width / 2, height / 2 - 35, 0xFFFFFFFF);
                }
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float hover = hoverAnimation.getValue();
            if (hover > 0) {
                int color = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hover * 0.15f);
                graphics.fill(x, y, x + width, y + height, color);
            }

            double price = RngTabController.this.getItemPrice(itemName);
            int chestCost = RngTabController.this.getChestCost(itemName);
            double profit = Math.max(0, price - chestCost);
            double totalProfit = profit * count;

            String displayName = itemName;
            String countStr = count > 0 ? "§a" + count : "§7" + count;
            String profitStr = totalProfit > 0 ? " §6(" + RngTabController.this.formatNumber(totalProfit) + ")" : "";

            int textColor = count > 0 ? Theme.ACCENT : 0xFF888888;
            graphics.drawString(Minecraft.getInstance().font, displayName, x + 5, y + 8, textColor);

            int rightMargin = expanded ? 250 : 120;
            int countX = x + width - rightMargin;
            int profitX = x + width - (rightMargin - 50);

            graphics.drawString(Minecraft.getInstance().font, countStr, countX, y + 8, 0xFFFFFFFF);
            graphics.drawString(Minecraft.getInstance().font, profitStr, profitX, y + 8, 0xFFFFFFFF);

            if (expanded) {
                minusBtn.render(graphics, mouseX, mouseY, partialTick);
                plusBtn.render(graphics, mouseX, mouseY, partialTick);
                setBtn.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void tick() {
            if (expanded) {
                minusBtn.tick();
                plusBtn.tick();
                setBtn.tick();
            }

            if (hovered && hoverAnimation.getProgress() < 1
                    && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            } else if (!hovered && hoverAnimation.getProgress() > 0
                    && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        }

        @Override
        public void updateHoverState(int mouseX, int mouseY) {
            super.updateHoverState(mouseX, mouseY);
            if (expanded) {
                minusBtn.setX(x + width - 140);
                minusBtn.setY(y + 6);
                plusBtn.setX(x + width - 105);
                plusBtn.setY(y + 6);
                setBtn.setX(x + width - 70);
                setBtn.setY(y + 6);

                minusBtn.updateHoverState(mouseX, mouseY);
                plusBtn.updateHoverState(mouseX, mouseY);
                setBtn.updateHoverState(mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (expanded) {
                if (minusBtn.mouseClicked(mouseX, mouseY, button))
                    return true;
                if (plusBtn.mouseClicked(mouseX, mouseY, button))
                    return true;
                if (setBtn.mouseClicked(mouseX, mouseY, button))
                    return true;
            }

            if (visible && isMouseOver(mouseX, mouseY) && button == 0) {
                expanded = !expanded;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (expanded) {
                minusBtn.mouseReleased(mouseX, mouseY, button);
                plusBtn.mouseReleased(mouseX, mouseY, button);
                setBtn.mouseReleased(mouseX, mouseY, button);
            }
            return false;
        }
    }
}