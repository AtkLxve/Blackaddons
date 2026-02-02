package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.BarGraphWidget;
import org.blackum.blackaddons.gui.widget.Button;
import org.blackum.blackaddons.gui.widget.Dropdown;
import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.ListView;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.widget.TextField;
import org.blackum.blackaddons.gui.widget.Widget;
import org.blackum.blackaddons.util.BotIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileViewerScreen extends BaseScreen {
    private final String player;
    private TabPanel tabPanel;
    private JsonObject profileData;
    private boolean isLoading = true;
    private String errorMessage = null;
    private static int lastTabIndex = 0;

    private ListView simResultsList;

    private ListView dailyLeaderboardList;
    private Button[] dailyModeButtons = new Button[4];
    private String dailyMode = "leaderboard";
    private String dailyPeriod = "daily";
    private String dailyMetric = "xp";
    private int dailyPage = 1;
    private int dailyTotalPages = 1;

    private String dailyFloor = "master_7";
    private ListView dailyPersonalList;
    private Dropdown dailySearchTypeDropdown;
    private TextField dailySearchField;
    private Button dailyShowMeBtn;

    private Dropdown dailyFloorDropdown;
    private Dropdown rtcaFloorDropdown;

    private static final List<String> FLOOR_OPTIONS = List.of(
            "M7", "M6", "M5", "M4", "M3", "M2", "M1",
            "F7", "F6", "F5", "F4", "F3", "F2", "F1", "Entrance");

    private String dailyLastInput = "";
    private long dailyLastInputTime = 0;
    private String dailyExecutedQuery = "";

    public ProfileViewerScreen(net.minecraft.client.gui.screens.Screen parent, String player) {
        this(parent, player, null);
    }

    public ProfileViewerScreen(net.minecraft.client.gui.screens.Screen parent, String player, JsonObject data) {
        super(Component.literal("Profile: " + player), parent);
        this.player = player;
        if (data != null) {
            this.profileData = data;
            this.isLoading = false;
        }
    }

    @Override
    protected void initWidgets() {
        if (isLoading) {
            BotIntegration.getProfileStats(player).thenAccept(json -> {
                isLoading = false;
                if (json == null) {
                    errorMessage = "Failed to fetch data from bot.";
                } else if (json.has("error")) {
                    errorMessage = json.get("error").getAsString();
                } else if (json.has("data")) {
                    profileData = json.getAsJsonObject("data");
                } else {
                    errorMessage = "Invalid data received.";
                }
                net.minecraft.client.Minecraft.getInstance().execute(() -> this.init(this.width, this.height));
            });
        }

        if (isLoading) {
            addWidget(new Label(containerX + containerWidth / 2 - 30, containerY + containerHeight / 2, "Loading...",
                    Label.Style.TITLE));
            return;
        }

        if (errorMessage != null) {
            addWidget(new Label(containerX + containerWidth / 2 - 60, containerY + containerHeight / 2, errorMessage,
                    Label.Style.TITLE));
            return;
        }

        if (profileData == null)
            return;

        tabPanel = new TabPanel(containerX, containerY + 40, containerWidth, containerHeight - 40);
        tabPanel.setOnTabChange(index -> {
            lastTabIndex = index;
            stopConfetti();
        });
        addWidget(tabPanel);

        initDungeonsTab();
        initTeammatesTab();
        initRngTab();
        initDailyTab();
        initRtcaTab();

        tabPanel.selectTab(lastTabIndex);
    }

    private void initDungeonsTab() {
        TabPanel.Tab tab = tabPanel.addTab("Dungeons");
        int w = tabPanel.getContentWidth() - 20;
        ListView list = new ListView(tabPanel.getContentX(), tabPanel.getContentY(), w, tabPanel.getMaxContentHeight());
        list.setItemSpacing(10);
        tab.addWidget(list);

        double cataXp = getDouble(profileData, "catacombs");
        int secretCount = getInt(profileData, "secrets");
        int bloodKills = getInt(profileData, "blood_mob_kills");

        int totalRuns = 0;

        java.util.Map<String, Double> runDistribution = new java.util.LinkedHashMap<>();

        JsonObject floors = profileData.has("floors") ? profileData.getAsJsonObject("floors") : new JsonObject();
        List<String> normalFloors = new ArrayList<>();
        List<String> masterFloors = new ArrayList<>();
        List<String> keys = new ArrayList<>(floors.keySet());
        keys.sort((k1, k2) -> {
            boolean m1 = k1.startsWith("M");
            boolean m2 = k2.startsWith("M");

            if (m1 && !m2)
                return -1;
            if (!m1 && m2)
                return 1;

            int n1 = getFloorNum(k1);
            int n2 = getFloorNum(k2);
            return Integer.compare(n2, n1);
        });

        for (String key : keys) {
            JsonObject f = floors.getAsJsonObject(key);
            int r = getInt(f, "runs");
            if (r > 0) {
                totalRuns += r;
                runDistribution.put(key, (double) r);

                if (key.startsWith("M")) {
                    masterFloors.add(key);
                } else {
                    normalFloors.add(key);
                }
            }
        }

        String entranceKey = null;
        if (normalFloors.contains("F0")) {
            entranceKey = "F0";
            normalFloors.remove("F0");
        } else if (normalFloors.contains("Entrance")) {
            entranceKey = "Entrance";
            normalFloors.remove("Entrance");
        }

        double secretsPerRun = totalRuns > 0 ? (double) secretCount / totalRuns : 0;

        final int finalTotalRuns = totalRuns;
        int effectiveW = w - 8;

        addSectionHeader(list, "General Stats");

        double cataLvl = org.blackum.blackaddons.util.DungeonUtils.getCataLevel(cataXp);

        Widget generalStats = new Widget(0, 0, effectiveW, 120) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                int boxW = (width - 10) / 3;
                int boxW2 = (width - 5) / 2;
                int row2Y = y + 65;

                drawStatBox(graphics, x, y, boxW, "Cata Level", String.format("%.2f", cataLvl));
                drawStatBox(graphics, x + boxW + 5, y, boxW, "Blood Mobs", String.format("%,d", bloodKills));
                drawStatBox(graphics, x + (boxW + 5) * 2, y, boxW, "Total Runs", String.format("%,d", finalTotalRuns));

                drawStatBox(graphics, x, row2Y, boxW2, "Secrets", String.format("%,d", secretCount));
                drawStatBox(graphics, x + boxW2 + 5, row2Y, boxW2, "Secrets/Run", String.format("%.2f", secretsPerRun));
            }

            private void drawStatBox(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, int w, String label,
                    String value) {
                org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, x, y, w, 50,
                        Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);
                graphics.drawCenteredString(minecraft.font, label, x + w / 2, y + 10, Theme.ACCENT);
                graphics.drawCenteredString(minecraft.font, "§f" + value, x + w / 2, y + 25, 0xFFFFFFFF);
            }
        };
        list.addItem(generalStats);

        if (profileData.has("classes")) {
            JsonObject classes = profileData.getAsJsonObject("classes");
            java.util.Map<String, Double> classData = new java.util.HashMap<>();
            List<Map.Entry<String, JsonElement>> sorted = new ArrayList<>(classes.entrySet());
            sorted.sort((e1, e2) -> Double.compare(e2.getValue().getAsDouble(), e1.getValue().getAsDouble()));

            double totalLevel = 0;
            int classCount = 0;

            for (Map.Entry<String, JsonElement> entry : sorted) {
                double xp = entry.getValue().getAsDouble();
                double lvl = org.blackum.blackaddons.util.DungeonUtils.getCataLevel(xp);
                classData.put(entry.getKey(), lvl);

                if (List.of("archer", "berserk", "healer", "mage", "tank").contains(entry.getKey())) {
                    totalLevel += lvl;
                    classCount++;
                }
            }

            double classAvg = classCount > 0 ? totalLevel / classCount : 0;
            if (classAvg >= 50.0) {
                startConfetti();
                list.addItem(new Widget(0, 0, effectiveW, 25) {
                    @Override
                    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                            float partialTick) {
                        graphics.drawCenteredString(minecraft.font, "§6§l🎉 CLASS AVERAGE 50! 🎉", x + width / 2, y + 8,
                                0xFFFFFFFF);
                    }
                });
                list.addItem(new Widget(0, 0, effectiveW, 20) {
                    @Override
                    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                            float partialTick) {
                        graphics.drawCenteredString(minecraft.font,
                                "§eCongratulations! You need to touch some grass!", x + width / 2, y + 5,
                                0xFFFFD700);
                    }
                });
            }

            org.blackum.blackaddons.gui.widget.BarGraphWidget graph = new org.blackum.blackaddons.gui.widget.BarGraphWidget(
                    0, 0, effectiveW, String.format("Class Levels (Avg: %.2f)", classAvg));
            graph.setData(classData, "Lvl");

            java.util.Map<String, Integer> classColors = new java.util.HashMap<>();
            classColors.put("Archer", 0xFF2ECC71); // Green
            classColors.put("Berserk", 0xFFE74C3C); // Red
            classColors.put("Healer", 0xFFF1C40F); // Yellow
            classColors.put("Mage", 0xFF3498DB); // Blue
            classColors.put("Tank", 0xFF95A5A6); // Gray
            graph.setColorMap(classColors);

            list.addItem(graph);
        }

        addSectionHeader(list, "Dungeon Floors");

        int maxRows = Math.max(normalFloors.size(), masterFloors.size());

        for (int i = 0; i < maxRows; i++) {
            GridRow row = new GridRow(effectiveW, 50);

            int cardW1 = (effectiveW - 10) / 2;
            int cardW2 = effectiveW - 10 - cardW1;

            if (i < normalFloors.size()) {
                String key = normalFloors.get(i);
                JsonObject data = floors.getAsJsonObject(key);
                String name = "Floor " + (key.startsWith("F") ? key.substring(1) : key);
                row.addChild(createFloorCard(cardW1, name, data), 0);
            }

            if (i < masterFloors.size()) {
                String key = masterFloors.get(i);
                JsonObject data = floors.getAsJsonObject(key);
                String name = "Master " + key.substring(1);
                row.addChild(createFloorCard(cardW2, name, data), cardW1 + 10);
            }

            list.addItem(row);
        }

        if (entranceKey != null && floors.has(entranceKey)) {
            JsonObject data = floors.getAsJsonObject(entranceKey);
            org.blackum.blackaddons.gui.widget.FloorCardWidget entCard = createFloorCard(effectiveW, "Entrance", data);
            list.addItem(entCard);
        }

        if (!runDistribution.isEmpty()) {
            java.util.Map<String, Double> formattedRunDist = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : runDistribution.entrySet()) {
                String k = entry.getKey();
                String label = (k.equals("F0") || k.equals("Entrance")) ? "Entrance" : k;
                formattedRunDist.put(label, entry.getValue());
            }

            org.blackum.blackaddons.gui.widget.BarGraphWidget runGraph = new org.blackum.blackaddons.gui.widget.BarGraphWidget(
                    0, 0, effectiveW, "Floor Completions");
            runGraph.setData(formattedRunDist, "Runs");

            java.util.Map<String, Integer> floorColors = new java.util.HashMap<>();
            int normalColor = 0xFF9B59B6; // Purple
            int masterColor = 0xFFD35400; // Orange

            floorColors.put("Entrance", normalColor);
            floorColors.put("F1", normalColor);
            floorColors.put("F2", normalColor);
            floorColors.put("F3", normalColor);
            floorColors.put("F4", normalColor);
            floorColors.put("F5", normalColor);
            floorColors.put("F6", normalColor);
            floorColors.put("F7", normalColor);

            floorColors.put("M1", masterColor);
            floorColors.put("M2", masterColor);
            floorColors.put("M3", masterColor);
            floorColors.put("M4", masterColor);
            floorColors.put("M5", masterColor);
            floorColors.put("M6", masterColor);
            floorColors.put("M7", masterColor);

            runGraph.setColorMap(floorColors);

            list.addItem(runGraph);
        }
    }

    private int getFloorNum(String key) {
        if (key.equals("F0") || key.equals("Entrance"))
            return 0;
        try {
            return Integer.parseInt(key.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private org.blackum.blackaddons.gui.widget.FloorCardWidget createFloorCard(int width, String name,
            JsonObject data) {
        int runs = getInt(data, "runs");
        int best = getInt(data, "best_score");
        String sPlus = formatMs(getInt(data, "fastest_s_plus"));
        String s = formatMs(getInt(data, "fastest_s"));
        return new org.blackum.blackaddons.gui.widget.FloorCardWidget(width, name, runs, best, sPlus, s);
    }

    private static class GridRow extends Widget {
        private final List<java.util.Map.Entry<Widget, Integer>> children = new ArrayList<>();

        GridRow(int w, int h) {
            super(0, 0, w, h);
        }

        public void addChild(Widget w, int xOffset) {
            children.add(new java.util.AbstractMap.SimpleEntry<>(w, xOffset));
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (java.util.Map.Entry<Widget, Integer> entry : children) {
                Widget w = entry.getKey();
                int xOff = entry.getValue();
                int originalX = w.getX();
                int originalY = w.getY();

                w.setX(this.x + xOff);
                w.setY(this.y);
                w.render(graphics, mouseX, mouseY, partialTick);

                w.setX(originalX);
                w.setY(originalY);
            }
        }

        @Override
        public void tick() {
            for (java.util.Map.Entry<Widget, Integer> entry : children) {
                entry.getKey().tick();
            }
        }
    }

    private static class Teammate {
        String ign;
        int count;
        String lastFloor;
        String lastClass;
        long lastTs;
        int lastClassLevel;

        Teammate(String ign, JsonObject data) {
            this.ign = ign;
            this.count = data.has("count") ? data.get("count").getAsInt() : 0;
            this.lastFloor = data.has("last_floor") && !data.get("last_floor").isJsonNull()
                    ? data.get("last_floor").getAsString()
                    : "";
            this.lastClass = data.has("last_class") && !data.get("last_class").isJsonNull()
                    ? data.get("last_class").getAsString()
                    : "";
            this.lastTs = data.has("last_ts") && !data.get("last_ts").isJsonNull() ? data.get("last_ts").getAsLong()
                    : 0;
            this.lastClassLevel = data.has("last_class_level") && !data.get("last_class_level").isJsonNull()
                    ? data.get("last_class_level").getAsInt()
                    : 0;
        }
    }

    private class TeammateRow extends Widget {
        private final Teammate tm;
        private Animation hoverAnimation;
        private final Button inviteBtn;

        public TeammateRow(int width, Teammate tm) {
            super(0, 0, width, 18);
            this.tm = tm;
            this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
            this.inviteBtn = new Button(0, 0, 40, 12, "Invite", () -> {
                if (minecraft.player != null) {
                    minecraft.player.connection.sendCommand("party " + tm.ign);
                }
            });
        }

        @Override
        public void updateHoverState(int mouseX, int mouseY) {
            super.updateHoverState(mouseX, mouseY);
            inviteBtn.setX(this.x + this.width - 45);
            inviteBtn.setY(this.y + 3);
            inviteBtn.updateHoverState(mouseX, mouseY);
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float hover = hoverAnimation.getValue();
            if (hover > 0) {
                int color = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hover * 0.2f);
                graphics.fill(x, y, x + width, y + height, color);
            }

            int cx = x + 2;
            int cy = y + 5;

            String ignText = tm.ign;
            graphics.drawString(minecraft.font, ignText, cx, cy, Theme.ACCENT);
            cx += COL_IGN;
            graphics.drawString(minecraft.font, "§f" + tm.count, cx, cy, 0xFFFFFFFF);
            cx += COL_RUNS;

            String classText = String.format("§f%s %d", tm.lastClass, tm.lastClassLevel);
            graphics.drawString(minecraft.font, classText, cx, cy, 0xFFFFFFFF);
            cx += COL_CLASS;

            graphics.drawString(minecraft.font, "§f" + tm.lastFloor, cx, cy, 0xFFFFFFFF);
            cx += COL_FLOOR;

            String timeAgo = formatRelativeTime(tm.lastTs);
            graphics.drawString(minecraft.font, "§7" + timeAgo, cx, cy, 0xFFFFFFFF);

            inviteBtn.setX(this.x + this.width - 45);
            inviteBtn.setY(this.y + 3);
            inviteBtn.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void tick() {
            inviteBtn.tick();
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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (inviteBtn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (visible && isMouseOver(mouseX, mouseY) && button == 0) {
                if (minecraft.player != null) {
                    minecraft.player.connection.sendCommand("ba pv " + tm.ign);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return inviteBtn.mouseReleased(mouseX, mouseY, button);
        }
    }

    private String lastSearchText = "";

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderConfetti(graphics);
    }

    @Override
    public void tick() {
        super.tick();
        tickConfetti();
        if (searchField != null) {
            String currentText = searchField.getText();
            if (!currentText.equals(lastSearchText)) {
                lastSearchText = currentText;
                updateTeammatesList();
            }
        }

        if (dailySearchField != null && dailySearchField.isVisible()) {
            String currentText = dailySearchField.getText();
            if (!currentText.equals(dailyLastInput)) {
                dailyLastInput = currentText;
                dailyLastInputTime = System.currentTimeMillis();
            }

            if (!dailyLastInput.isEmpty() && !dailyLastInput.equals(dailyExecutedQuery)) {
                if (System.currentTimeMillis() - dailyLastInputTime > 500) {
                    dailyExecutedQuery = dailyLastInput;
                    String type = (dailySearchTypeDropdown != null && dailySearchTypeDropdown.getSelectedIndex() == 1)
                            ? "Page"
                            : "IGN";
                    performSearch(dailyLastInput, type);
                }
            }
        }
    }

    private List<Teammate> allTeammates = new ArrayList<>();
    private String filterClass = "All";
    private String filterTime = "Any";
    private String filterSort = "Runs";

    private TextField searchField;
    private ListView teammatesList;

    private static final int COL_IGN = 100;
    private static final int COL_RUNS = 40;
    private static final int COL_CLASS = 80;
    private static final int COL_FLOOR = 35;

    private void initTeammatesTab() {
        TabPanel.Tab tab = tabPanel.addTab("Recent Teammates");

        int controlsHeight = 20;
        int listMarginTop = 30;

        int startY = tabPanel.getContentY();
        int contentWidth = tabPanel.getContentWidth();

        searchField = new TextField(tabPanel.getContentX(), startY, 120, controlsHeight, "Search IGN...");
        searchField.setCharFilter(c -> Character.isLetterOrDigit(c) || c == '_');
        tab.addWidget(searchField);

        int btnY = startY;
        int btnH = controlsHeight;
        int btnW = 75;
        int btnGap = 5;

        int currentBtnX = tabPanel.getContentX() + 120 + btnGap;

        Button classBtn = new Button(currentBtnX, btnY, btnW, btnH, "Class: All", () -> {
            cycleClassFilter();
            updateTeammatesList();
        });
        tab.addWidget(classBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                classBtn.setText("Class: " + filterClass);
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, float p) {
            }
        });
        currentBtnX += btnW + btnGap;

        Button timeBtn = new Button(currentBtnX, btnY, btnW, btnH, "Time: Any", () -> {
            cycleTimeFilter();
            updateTeammatesList();
        });
        tab.addWidget(timeBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                timeBtn.setText("Time: " + filterTime);
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, float p) {
            }
        });
        currentBtnX += btnW + btnGap;

        Button sortBtn = new Button(currentBtnX, btnY, btnW, btnH, "Sort: Runs", () -> {
            cycleSortFilter();
            updateTeammatesList();
        });
        tab.addWidget(sortBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                sortBtn.setText("Sort: " + filterSort);
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, float p) {
            }
        });

        int headerY = startY + controlsHeight + 10;
        int headerX = tabPanel.getContentX();
        tab.addWidget(new Widget(headerX, headerY, contentWidth - 20, 15) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                int x = this.x + 2;
                graphics.drawString(minecraft.font, "§7IGN", x, y + 4, 0xFFFFFFFF);
                x += COL_IGN;
                graphics.drawString(minecraft.font, "§7Runs", x, y + 4, 0xFFFFFFFF);
                x += COL_RUNS;
                graphics.drawString(minecraft.font, "§7Class", x, y + 4, 0xFFFFFFFF);
                x += COL_CLASS;
                graphics.drawString(minecraft.font, "§7Floor", x, y + 4, 0xFFFFFFFF);
                x += COL_FLOOR;
                graphics.drawString(minecraft.font, "§7Last Seen", x, y + 4, 0xFFFFFFFF);

                graphics.fill(this.x, this.y + 14, this.x + width, this.y + 15, 0x40FFFFFF);
            }
        });
        teammatesList = new ListView(tabPanel.getContentX(), headerY + 15,
                contentWidth - 20, tabPanel.getContentHeight() - controlsHeight - 10 - 15);
        teammatesList.setItemSpacing(0);
        tab.addWidget(teammatesList);

        if (profileData.has("teammates")) {
            JsonArray tmArray = profileData.getAsJsonArray("teammates");
            allTeammates.clear();
            for (JsonElement tmElem : tmArray) {
                if (tmElem.isJsonArray()) {
                    JsonArray tuple = tmElem.getAsJsonArray();
                    if (tuple.size() >= 2) {
                        String ign = tuple.get(0).getAsString();
                        JsonObject data = tuple.get(1).getAsJsonObject();
                        allTeammates.add(new Teammate(ign, data));
                    }
                }
            }
            updateTeammatesList();
        } else {
            addInfoRow(teammatesList, "No teammate data available.", "");
        }
    }

    private void cycleClassFilter() {
        switch (filterClass) {
            case "All" -> filterClass = "Archer";
            case "Archer" -> filterClass = "Berserk";
            case "Berserk" -> filterClass = "Healer";
            case "Healer" -> filterClass = "Mage";
            case "Mage" -> filterClass = "Tank";
            default -> filterClass = "All";
        }
    }

    private void cycleTimeFilter() {
        switch (filterTime) {
            case "Any" -> filterTime = "24h";
            case "24h" -> filterTime = "7d";
            case "7d" -> filterTime = "30d";
            default -> filterTime = "Any";
        }
    }

    private void cycleSortFilter() {
        filterSort = filterSort.equals("Runs") ? "Recent" : "Runs";
    }

    private void updateTeammatesList() {
        if (teammatesList == null)
            return;
        teammatesList.clearItems();

        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        long now = System.currentTimeMillis() / 1000;
        long timeThreshold = 0;

        if (filterTime.equals("24h"))
            timeThreshold = now - 86400;
        else if (filterTime.equals("7d"))
            timeThreshold = now - 604800;
        else if (filterTime.equals("30d"))
            timeThreshold = now - 2592000;

        List<Teammate> filtered = new ArrayList<>();
        for (Teammate tm : allTeammates) {
            if (!search.isEmpty() && !tm.ign.toLowerCase().contains(search))
                continue;

            if (!filterClass.equals("All") && !filterClass.equalsIgnoreCase(tm.lastClass))
                continue;

            if (timeThreshold > 0 && tm.lastTs < timeThreshold)
                continue;

            filtered.add(tm);
        }

        if (filterSort.equals("Runs")) {
            filtered.sort((t1, t2) -> Integer.compare(t2.count, t1.count));
        } else {
            filtered.sort((t1, t2) -> Long.compare(t2.lastTs, t1.lastTs));
        }

        if (filtered.isEmpty()) {
            addInfoRow(teammatesList, "No teammates found.", "");
        } else {
            for (Teammate tm : filtered) {
                teammatesList.addItem(new TeammateRow(teammatesList.getWidth(), tm));
            }
        }
    }

    private String formatRelativeTime(long timestamp) {
        if (timestamp == 0)
            return "Unknown";
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;

        if (diff < 60)
            return diff + "s";
        if (diff < 3600)
            return (diff / 60) + "m";
        if (diff < 86400)
            return (diff / 3600) + "h";
        return (diff / 86400) + "d";
    }

    private void initRngTab() {
        TabPanel.Tab tab = tabPanel.addTab("RNG");
        ListView list = new ListView(tabPanel.getContentX(), tabPanel.getContentY(), tabPanel.getContentWidth(),
                tabPanel.getMaxContentHeight());
        tab.addWidget(list);
        addInfoRow(list, "RNG Tracking", "Use /ba rng to track drops.");
        addInfoRow(list, "History", "Coming soon...");
    }

    private void initDailyTab() {
        TabPanel.Tab tab = tabPanel.addTab("Daily");
        int w = tabPanel.getContentWidth();
        int cx = tabPanel.getContentX();
        int cy = tabPanel.getContentY();

        int btnW = (w - 34) / 4;
        int btnH = 20;
        int gap = 5;

        dailyModeButtons[0] = new Button(cx, cy, btnW, btnH, "Daily", () -> setDailyMode("leaderboard", "daily"));
        dailyModeButtons[1] = new Button(cx + btnW + gap, cy, btnW, btnH, "Monthly",
                () -> setDailyMode("leaderboard", "monthly"));
        dailyModeButtons[2] = new Button(cx + (btnW + gap) * 2, cy, btnW, btnH, "Personal",
                () -> setDailyMode("personal", "daily"));
        dailyModeButtons[3] = new Button(cx + (btnW + gap) * 3, cy, btnW, btnH, "Runs", this::toggleDailyMetric);

        for (Button b : dailyModeButtons)
            tab.addWidget(b);
        int searchY = cy + 25;

        dailySearchTypeDropdown = new Dropdown(cx, searchY, 60, 20, "Search By", java.util.List.of("IGN", "Page"),
                (val) -> {
                    if (val.equals("Page")) {
                        dailySearchField.setPlaceholder("Page #");
                        dailySearchField.setCharFilter(Character::isDigit);
                        String txt = dailySearchField.getText();
                        if (!txt.matches("\\d*")) {
                            dailySearchField.setText(txt.replaceAll("\\D", ""));
                        }
                    } else {
                        dailySearchField.setPlaceholder("IGN...");
                        dailySearchField.setCharFilter(c -> true);
                    }
                });
        dailySearchTypeDropdown.setSelectedIndex(0);

        int showMeW = 70;
        int showMeX = cx + w - showMeW - 20;
        dailyShowMeBtn = new Button(showMeX, searchY, showMeW, 20, "Show Me", () -> {
            if (minecraft.player != null) {
                if (!dailySearchTypeDropdown.isExpanded()) {
                    performSearch(minecraft.player.getName().getString(), "IGN");
                }
            }
        });

        int searchFieldX = cx + 65;
        int searchFieldW = showMeX - searchFieldX - 10;

        dailySearchField = new TextField(searchFieldX, searchY, searchFieldW, 20, "IGN...") {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 257) {
                    String type = dailySearchTypeDropdown.getSelectedIndex() == 1 ? "Page" : "IGN";
                    dailyExecutedQuery = getText();
                    performSearch(getText(), type);
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };

        tab.addWidget(dailySearchField);
        tab.addWidget(dailyShowMeBtn);
        tab.addWidget(dailySearchTypeDropdown);

        tab.addWidget(dailySearchTypeDropdown);

        dailyFloorDropdown = new Dropdown(cx, cy + 50, w - 20, 20, "Floor: M7", FLOOR_OPTIONS, (val) -> {
            dailyFloor = getDailyFloorKey(val);
            if (dailyMetric.startsWith("runs")) {
                this.dailyMetric = "runs_" + dailyFloor;
                fetchDailyData();
            }
        });
        dailyFloorDropdown.setVisible(false);
        dailyFloorDropdown.setSelectedOption("M7");
        tab.addWidget(dailyFloorDropdown);

        int listY = cy + 75;
        int listH = tabPanel.getMaxContentHeight() - 75;

        dailyLeaderboardList = new ListView(cx, listY, w - 20, listH);
        dailyPersonalList = new ListView(cx, listY, w - 20, listH);
        dailyPersonalList.setVisible(false);

        tab.addWidget(dailyLeaderboardList);
        tab.addWidget(dailyPersonalList);

        updateDailyButtons();
        fetchDailyData();
    }

    private void performSearch(String query, String type) {
        if (query == null || query.trim().isEmpty())
            return;
        query = query.trim();

        if (type.equals("Page")) {
            if (query.matches("\\d+")) {
                int page = Integer.parseInt(query);
                this.dailyPage = page;
                if (this.dailyPage < 1)
                    this.dailyPage = 1;
                fetchDailyData();
            }
            return;
        } else {
            dailyLeaderboardList.clearItems();
            addInfoRow(dailyLeaderboardList, "Searching...", "");

            BotIntegration.getLeaderboardWithPlayer(dailyPeriod, dailyMetric, query).thenAccept(json -> {
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    if (json == null || json.has("error")) {
                        dailyLeaderboardList.clearItems();
                        if (json != null && json.has("error")) {
                            addInfoRow(dailyLeaderboardList, json.get("error").getAsString(), "");
                        } else {
                            addInfoRow(dailyLeaderboardList, "Not found.", "");
                        }
                        return;
                    }

                    if (json.has("page")) {
                        this.dailyPage = json.get("page").getAsInt();
                    }
                    if (json.has("total_pages")) {
                        this.dailyTotalPages = json.get("total_pages").getAsInt();
                    }

                    dailyLeaderboardList.clearItems();
                    renderLeaderboard(json);
                });
            });
        }
    }

    private void setDailyMode(String mode, String period) {
        this.dailyMode = mode;
        if (period != null)
            this.dailyPeriod = period;
        this.dailyPage = 1;

        if (mode.equals("personal")) {
            dailyLeaderboardList.setVisible(false);
            dailyPersonalList.setVisible(true);
        } else {
            dailyLeaderboardList.setVisible(true);
            dailyPersonalList.setVisible(false);
        }
        updateDailyButtons();
        fetchDailyData();
    }

    private void toggleDailyMetric() {
        if (this.dailyMetric.equals("xp")) {
            this.dailyMetric = "runs_" + dailyFloor;
            if (this.dailyMode.equals("personal")) {
            } else {
            }
        } else {
            this.dailyMetric = "xp";
        }
        updateDailyButtons();
        fetchDailyData();
    }

    private String getDailyFloorKey(String display) {
        if (display.equals("Entrance") || display.equals("Ent"))
            return "normal_0";
        if (display.startsWith("M"))
            return "master_" + display.substring(1);
        if (display.startsWith("F"))
            return "normal_" + display.substring(1);
        return "master_7";
    }

    private String getDailyFloorDisplay(String key) {
        if (key.equals("normal_0"))
            return "Entrance";
        if (key.startsWith("master_"))
            return "M" + key.substring(7);
        if (key.startsWith("normal_"))
            return "F" + key.substring(7);
        return "M7";
    }

    private void updateDailyButtons() {
        boolean isLb = dailyMode.equals("leaderboard");
        boolean isRuns = dailyMetric.startsWith("runs");

        dailyModeButtons[0].setEnabled(!isLb || !dailyPeriod.equals("daily"));
        dailyModeButtons[1].setEnabled(!isLb || !dailyPeriod.equals("monthly"));
        dailyModeButtons[2].setEnabled(!dailyMode.equals("personal"));

        dailyModeButtons[3].setText(isRuns ? "Show XP" : "Runs");

        if (dailyFloorDropdown != null) {
            dailyFloorDropdown.setVisible(isRuns && isLb);
            if (isRuns) {
                dailyFloorDropdown.setSelectedOption(getDailyFloorDisplay(dailyFloor));
            }
        }

        if (dailySearchField != null)
            dailySearchField.setVisible(isLb);
        if (dailyShowMeBtn != null)
            dailyShowMeBtn.setVisible(isLb);
        if (dailySearchTypeDropdown != null)
            dailySearchTypeDropdown.setVisible(isLb);
    }

    private void fetchDailyData() {
        if (dailyMode.equals("leaderboard")) {
            dailyLeaderboardList.clearItems();
            addInfoRow(dailyLeaderboardList, "Loading...", "");

            String metricToSend = dailyMetric;

            BotIntegration.getLeaderboard(dailyPeriod, metricToSend, dailyPage).thenAccept(json -> {
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    dailyLeaderboardList.clearItems();
                    if (json == null || json.has("error")) {
                        addInfoRow(dailyLeaderboardList, "Error fetching data.", "");
                        return;
                    }

                    if (json.has("total_pages")) {
                        dailyTotalPages = json.get("total_pages").getAsInt();
                    } else {
                        dailyTotalPages = 1;
                    }

                    renderLeaderboard(json);
                });
            });
        } else {
            renderPersonalStats();
        }
    }

    private void renderLeaderboard(JsonObject json) {
        if (!json.has("data") || json.get("data").isJsonNull()) {
            addInfoRow(dailyLeaderboardList, "No data found.", "");
            return;
        }

        com.google.gson.JsonArray data = json.getAsJsonArray("data");
        if (data.size() == 0) {
            addInfoRow(dailyLeaderboardList, "No entries yet.", "");
            return;
        }

        int rank = (dailyPage - 1) * 10 + 1;
        for (JsonElement e : data) {
            JsonObject entry = e.getAsJsonObject();
            String ign = entry.get("ign").getAsString();
            double val = entry.get("gained").getAsDouble();

            dailyLeaderboardList.addItem(new LeaderboardRow(dailyLeaderboardList.getWidth(), rank++, ign, val,
                    dailyMetric.startsWith("runs")));
        }

        if (json.has("last_updated") && !json.get("last_updated").isJsonNull()) {
            long ts = json.get("last_updated").getAsLong();
            addInfoRow(dailyLeaderboardList, "Last Updated: " + formatRelativeTime(ts) + " ago", "");
        }

        addPaginationControls(dailyLeaderboardList);
        addDiscordLinkButton(dailyLeaderboardList);
    }

    private void renderPersonalStats() {
        dailyPersonalList.clearItems();

        if (profileData == null) {
            addInfoRow(dailyPersonalList, "No profile data loaded.", "");
            return;
        }

        JsonObject daily = profileData.has("daily_stats") ? profileData.getAsJsonObject("daily_stats")
                : new JsonObject();
        JsonObject monthly = profileData.has("monthly_stats") ? profileData.getAsJsonObject("monthly_stats")
                : new JsonObject();

        if (daily.size() == 0 && monthly.size() == 0) {
            addInfoRow(dailyPersonalList, "No personal data available.", "Link Discord with /link to track.");
            return;
        }

        Widget header = new Widget(0, 0, dailyPersonalList.getWidth(), 30) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                graphics.drawCenteredString(minecraft.font, "§l📊 Personal Stats: " + player, x + width / 2, y + 10,
                        Theme.ACCENT);
            }
        };
        dailyPersonalList.addItem(header);

        renderStatGroup(dailyPersonalList, "Catacombs", daily, monthly, null);

        addSectionHeader(dailyPersonalList, "Class Progress");
        String[] classes = { "archer", "berserk", "healer", "mage", "tank" };
        for (String cls : classes) {
            renderStatGroup(dailyPersonalList, cls.substring(0, 1).toUpperCase() + cls.substring(1), daily, monthly,
                    cls);
        }

        addSectionHeader(dailyPersonalList, "Runs Gained");
        renderRunsGroup(dailyPersonalList, daily, monthly);

        dailyPersonalList.addItem(new Widget(0, 0, 0, 40) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
            }
        });
    }

    private void renderStatGroup(ListView list, String title, JsonObject dailyRoot, JsonObject monthlyRoot,
            String classKey) {
        double dGained = 0, dStart = 0, dEnd = 0;
        double mGained = 0, mStart = 0, mEnd = 0;
        boolean hasDaily = false, hasMonthly = false;

        if (classKey == null) {
            if (dailyRoot != null && dailyRoot.has("cata_gained")) {
                dGained = getDouble(dailyRoot, "cata_gained");
                dStart = getDouble(dailyRoot, "cata_start_lvl");
                dEnd = getDouble(dailyRoot, "cata_current_lvl");
                hasDaily = true;
            }
        } else {
            if (dailyRoot != null && dailyRoot.has("classes") && dailyRoot.getAsJsonObject("classes").has(classKey)) {
                JsonObject cls = dailyRoot.getAsJsonObject("classes").getAsJsonObject(classKey);
                dGained = getDouble(cls, "gained");
                dStart = getDouble(cls, "start_lvl");
                dEnd = getDouble(cls, "current_lvl");
                hasDaily = true;
            }
        }

        if (classKey == null) {
            if (monthlyRoot != null && monthlyRoot.has("cata_gained")) {
                mGained = getDouble(monthlyRoot, "cata_gained");
                mStart = getDouble(monthlyRoot, "cata_start_lvl");
                mEnd = getDouble(monthlyRoot, "cata_current_lvl");
                hasMonthly = true;
            }
        } else {
            if (monthlyRoot != null && monthlyRoot.has("classes")
                    && monthlyRoot.getAsJsonObject("classes").has(classKey)) {
                JsonObject cls = monthlyRoot.getAsJsonObject("classes").getAsJsonObject(classKey);
                mGained = getDouble(cls, "gained");
                mStart = getDouble(cls, "start_lvl");
                mEnd = getDouble(cls, "current_lvl");
                hasMonthly = true;
            }
        }

        if (dGained <= 0 && mGained <= 0)
            return;

        final double fdGained = dGained, fdStart = dStart, fdEnd = dEnd;
        final double fmGained = mGained, fmStart = mStart, fmEnd = mEnd;
        final boolean fHasDaily = hasDaily && dGained > 0;
        final boolean fHasMonthly = hasMonthly && mGained > 0;

        Widget w = new Widget(0, 0, list.getWidth(), 55) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, x, y, width - 4,
                        height - 2, 3, Theme.BACKGROUND_SECONDARY);

                graphics.drawString(minecraft.font, "§b" + title, x + 5, y + 5, 0xFFFFFFFF);

                int rowY = y + 18;
                if (fHasDaily) {
                    String dStr = "Day: §a+" + String.format("%,.0f", fdGained) + " XP §7("
                            + String.format("%.2f", fdStart) + " ➤ " + String.format("%.2f", fdEnd) + ")";
                    graphics.drawString(minecraft.font, dStr, x + 10, rowY, 0xFFE0E0E0);
                    rowY += 12;
                } else {
                    graphics.drawString(minecraft.font, "Day: §7No Gain", x + 10, rowY, 0xFFAAAAAA);
                    rowY += 12;
                }

                if (fHasMonthly) {
                    String mStr = "Month: §a+" + String.format("%,.0f", fmGained) + " XP §7("
                            + String.format("%.2f", fmStart) + " ➤ " + String.format("%.2f", fmEnd) + ")";
                    graphics.drawString(minecraft.font, mStr, x + 10, rowY, 0xFFE0E0E0);
                } else {
                    graphics.drawString(minecraft.font, "Month: §7No Gain", x + 10, rowY, 0xFFAAAAAA);
                }
            }
        };
        list.addItem(w);
    }

    private void renderRunsGroup(ListView list, JsonObject daily, JsonObject monthly) {
        StringBuilder dailyRuns = new StringBuilder();
        StringBuilder monthlyRuns = new StringBuilder();

        if (daily != null && daily.has("runs")) {
            appendRuns(dailyRuns, daily.getAsJsonObject("runs"));
        }
        if (monthly != null && monthly.has("runs")) {
            appendRuns(monthlyRuns, monthly.getAsJsonObject("runs"));
        }

        if (dailyRuns.length() == 0 && monthlyRuns.length() == 0) {
            addInfoRow(list, "No runs recorded recently.", "");
            return;
        }

        final String dText = dailyRuns.length() > 0 ? dailyRuns.toString() : "None";
        final String mText = monthlyRuns.length() > 0 ? monthlyRuns.toString() : "None";

        Widget w = new Widget(0, 0, list.getWidth(), 45) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, x, y, width - 4,
                        height - 2, 3, Theme.BACKGROUND_SECONDARY);

                graphics.drawString(minecraft.font, "Daily: " + dText, x + 5, y + 8, 0xFFE0E0E0);
                graphics.drawString(minecraft.font, "Monthly: " + mText, x + 5, y + 25, 0xFFE0E0E0);
            }
        };
        list.addItem(w);
    }

    private void appendRuns(StringBuilder sb, JsonObject runsObj) {
        if (runsObj.has("master")) {
            JsonObject m = runsObj.getAsJsonObject("master");
            for (String key : m.keySet()) {
                int val = m.get(key).getAsInt();
                if (val > 0) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    String label = key.equalsIgnoreCase("total") ? "Total" : "M" + key;
                    sb.append(label).append(" (+").append(val).append(")");
                }
            }
        }
        if (runsObj.has("normal")) {
            JsonObject n = runsObj.getAsJsonObject("normal");
            for (String key : n.keySet()) {
                int val = n.get(key).getAsInt();
                if (val > 0) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    String fName;
                    if (key.equalsIgnoreCase("total")) {
                        fName = "Total";
                    } else {
                        fName = key.equals("0") ? "Ent" : "F" + key;
                    }
                    sb.append(fName).append(" (+").append(val).append(")");
                }
            }
        }
    }

    private void addPaginationControls(ListView list) {
        int w = list.getWidth();
        Widget paging = new Widget(0, 0, w, 25) {
            Button prev;
            Button next;

            {
                prev = new Button(0, 0, 80, 20, "< Prev", () -> changePage(-1));
                next = new Button(0, 0, 80, 20, "Next >", () -> changePage(1));
            }

            private void updateLayout() {
                int mid = x + w / 2;
                prev.setX(mid - 120);
                prev.setY(y + 2);
                prev.setEnabled(dailyPage > 1);

                next.setX(mid + 40);
                next.setY(y + 2);
                next.setEnabled(dailyPage < dailyTotalPages);
            }

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                updateLayout();
                prev.render(graphics, mouseX, mouseY, partialTick);
                next.render(graphics, mouseX, mouseY, partialTick);

                String pageStr = dailyPage + " / " + dailyTotalPages;
                graphics.drawCenteredString(minecraft.font, pageStr, x + w / 2, y + 8, 0xFFAAAAAA);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                updateLayout();
                if (prev.mouseClicked(mouseX, mouseY, button))
                    return true;
                if (next.mouseClicked(mouseX, mouseY, button))
                    return true;
                return false;
            }

            @Override
            public void updateHoverState(int mouseX, int mouseY) {
                updateLayout();
                prev.updateHoverState(mouseX, mouseY);
                next.updateHoverState(mouseX, mouseY);
            }

            @Override
            public void tick() {
                prev.tick();
                next.tick();
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (prev.mouseReleased(mouseX, mouseY, button))
                    return true;
                if (next.mouseReleased(mouseX, mouseY, button))
                    return true;
                return false;
            }
        };
        list.addItem(paging);
    }

    private void changePage(int delta) {
        this.dailyPage += delta;
        if (this.dailyPage < 1)
            this.dailyPage = 1;
        if (dailyTotalPages > 0 && this.dailyPage > this.dailyTotalPages)
            this.dailyPage = this.dailyTotalPages;

        fetchDailyData();
    }

    private void addDiscordLinkButton(ListView list) {
        Button linkBtn = new Button(0, 0, list.getWidth() - 20, 20, "§bWant to be on leaderboard? Link Discord", () -> {
            String url = "https://discord.com/oauth2/authorize?client_id=1134507219220713472";
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Widget wrapper = new Widget(0, 0, list.getWidth(), 30) {

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                linkBtn.setX(this.x + 10);
                linkBtn.setY(this.y + 5);
                linkBtn.setWidth(this.width - 20);
                linkBtn.render(g, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return linkBtn.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public void updateHoverState(int mouseX, int mouseY) {
                linkBtn.updateHoverState(mouseX, mouseY);
            }

            @Override
            public void tick() {
                linkBtn.tick();
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return linkBtn.mouseReleased(mouseX, mouseY, button);
            }
        };
        list.addItem(wrapper);
    }

    private class LeaderboardRow extends Widget {
        private final int rank;
        private final String ign;
        private final double value;
        private final boolean isRuns;

        public LeaderboardRow(int w, int rank, String ign, double value, boolean isRuns) {
            super(0, 0, w, 20);
            this.rank = rank;
            this.ign = ign;
            this.value = value;
            this.isRuns = isRuns;
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = Theme.BACKGROUND_SECONDARY;
            String selfName = minecraft.getUser().getName();

            if (ign.equalsIgnoreCase(selfName)) {
                color = 0xFF228822;
            } else if (ign.equalsIgnoreCase(player)) {
                color = 0xFF555500;
            }

            org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, x, y, width, height - 2, 3,
                    color);

            String rankStr = "#" + rank;
            if (rank == 1)
                rankStr = "§6🥇";
            if (rank == 2)
                rankStr = "§7🥈";
            if (rank == 3)
                rankStr = "§c🥉";

            graphics.drawString(minecraft.font, rankStr, x + 5, y + 6, 0xFFFFFFFF);
            graphics.drawString(minecraft.font, ign, x + 30, y + 6, Theme.ACCENT);

            String valStr = isRuns ? String.format("%,.0f Runs", value) : String.format("%,.0f XP", value);
            int valW = minecraft.font.width(valStr);
            graphics.drawString(minecraft.font, "§f" + valStr, x + width - valW - 5, y + 6, 0xFFFFFFFF);
        }
    }

    private boolean simRing = true;
    private int simHecatombLvl = 10;
    private int simScarfAccIndex = 3;
    private int simScarfAttrLvl = 10;
    private int simGlobalIndex = 0;
    private int simMayorIndex = 0;
    private String rtcaFloor = "M7";

    private void initRtcaTab() {
        TabPanel.Tab tab = tabPanel.addTab("RTCA");
        int w = tabPanel.getContentWidth();
        int cx = tabPanel.getContentX();
        int cy = tabPanel.getContentY();

        Label settingsLabel = new Label(cx, cy, "§lSimulation Settings", Label.Style.TITLE);
        settingsLabel.setColor(Theme.ACCENT);
        tab.addWidget(settingsLabel);

        int currentY = cy + 25;
        int btnW = (w - 30) / 2;
        int btnH = 20;

        Button[] ringBtnRef = new Button[1];
        ringBtnRef[0] = new Button(cx, currentY, btnW, btnH, getRingLabel(), () -> {
            simRing = !simRing;
            if (ringBtnRef[0] != null)
                ringBtnRef[0].setText(getRingLabel());
        });

        Button[] hecaBtnRef = new Button[1];
        hecaBtnRef[0] = new Button(cx + btnW + 10, currentY, btnW, btnH, getHecatombLabel(), () -> {
            simHecatombLvl = (simHecatombLvl + 1) % 11;
            if (hecaBtnRef[0] != null)
                hecaBtnRef[0].setText(getHecatombLabel());
        });
        tab.addWidget(ringBtnRef[0]);
        tab.addWidget(hecaBtnRef[0]);
        currentY += btnH + 5;

        Button[] scarfAccBtnRef = new Button[1];
        scarfAccBtnRef[0] = new Button(cx, currentY, btnW, btnH, getScarfAccLabel(), () -> {
            simScarfAccIndex = (simScarfAccIndex + 1) % 4;
            if (scarfAccBtnRef[0] != null)
                scarfAccBtnRef[0].setText(getScarfAccLabel());
        });

        Button[] scarfAttrBtnRef = new Button[1];
        scarfAttrBtnRef[0] = new Button(cx + btnW + 10, currentY, btnW, btnH, getScarfAttrLabel(), () -> {
            simScarfAttrLvl = (simScarfAttrLvl + 1) % 11;
            if (scarfAttrBtnRef[0] != null)
                scarfAttrBtnRef[0].setText(getScarfAttrLabel());
        });
        tab.addWidget(scarfAccBtnRef[0]);
        tab.addWidget(scarfAttrBtnRef[0]);
        currentY += btnH + 5;

        Button[] globalBtnRef = new Button[1];
        globalBtnRef[0] = new Button(cx, currentY, btnW, btnH, getGlobalLabel(), () -> {
            simGlobalIndex = (simGlobalIndex + 1) % 6;
            if (globalBtnRef[0] != null)
                globalBtnRef[0].setText(getGlobalLabel());
        });

        Button[] mayorBtnRef = new Button[1];
        mayorBtnRef[0] = new Button(cx + btnW + 10, currentY, btnW, btnH, getMayorLabel(), () -> {
            simMayorIndex = (simMayorIndex + 1) % 3;
            if (mayorBtnRef[0] != null)
                mayorBtnRef[0].setText(getMayorLabel());
        });
        tab.addWidget(globalBtnRef[0]);
        tab.addWidget(mayorBtnRef[0]);
        currentY += btnH + 10;

        rtcaFloorDropdown = new Dropdown(cx, currentY, w - 20, 20, "Floor: M7", FLOOR_OPTIONS, (val) -> {
            rtcaFloor = val;
            updateSimulateButtonText();
        });
        rtcaFloorDropdown.setSelectedOption(rtcaFloor);
        tab.addWidget(rtcaFloorDropdown);
        currentY += 25;

        simulateBtn = new Button(cx, currentY, w - 20, 20, "Simulate Runs (" + rtcaFloor + ")", this::runSimulation);
        tab.addWidget(simulateBtn);
        currentY += 25;
        Label resultsLabel = new Label(cx, currentY, "§lResults", Label.Style.TITLE);
        resultsLabel.setColor(Theme.ACCENT);
        tab.addWidget(resultsLabel);
        currentY += 20;

        int listHeight = tabPanel.getMaxContentHeight() - (currentY - cy) - 10;
        simResultsList = new ListView(cx, currentY, w - 10, listHeight);
        tab.addWidget(simResultsList);
    }

    private Button simulateBtn;

    private void updateSimulateButtonText() {
        if (simulateBtn != null) {
            simulateBtn.setText("Simulate Runs (" + rtcaFloor + ")");
        }
    }

    private String getRingLabel() {
        return "Expert Ring: " + (simRing ? "Yes (+10%)" : "No");
    }

    private String getHecatombLabel() {
        if (simHecatombLvl == 0)
            return "Hecatomb: None";
        return "Hecatomb: " + intToRoman(simHecatombLvl);
    }

    private String getScarfAccLabel() {
        return switch (simScarfAccIndex) {
            case 1 -> "Scarf: Studies";
            case 2 -> "Scarf: Thesis";
            case 3 -> "Scarf: Grimoire";
            default -> "Scarf: None";
        };
    }

    private String getScarfAttrLabel() {
        if (simScarfAttrLvl == 0)
            return "Scarf Attr: None";
        return "Scarf Attr: " + intToRoman(simScarfAttrLvl);
    }

    private String getGlobalLabel() {
        double[] vals = { 0, 0.05, 0.1, 0.15, 0.2, 0.3 };
        return "Global: " + String.format("%.0f%%", vals[simGlobalIndex] * 100);
    }

    private String getMayorLabel() {
        return switch (simMayorIndex) {
            case 1 -> "Mayor: Derpy";
            case 2 -> "Mayor: Aura";
            default -> "Mayor: None";
        };
    }

    private String intToRoman(int num) {
        String[] roman = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        if (num >= 0 && num < roman.length)
            return roman[num];
        return String.valueOf(num);
    }

    private void runSimulation() {
        if (simResultsList != null)
            simResultsList.clearItems();

        if (player == null || player.isEmpty()) {
            if (simResultsList != null)
                addInfoRow(simResultsList, "Error:", "No player selected.");
            return;
        }

        if (simResultsList != null)
            addInfoRow(simResultsList, "Status:", "Requesting API...");

        java.util.Map<String, Double> bonuses = new java.util.HashMap<>();

        bonuses.put("ring", simRing ? 0.1 : 0.0);

        double hecaVal = 0.0;
        if (simHecatombLvl > 0)
            hecaVal = 0.004 + (simHecatombLvl * 0.0016);
        bonuses.put("hecatomb", hecaVal);

        double[] scarfAccVals = { 0.0, 0.02, 0.04, 0.06 };
        bonuses.put("scarf_accessory", scarfAccVals[simScarfAccIndex]);

        bonuses.put("scarf_attribute", simScarfAttrLvl * 0.02);

        double[] globalVals = { 1.0, 1.05, 1.1, 1.15, 1.2, 1.3 };
        bonuses.put("global", globalVals[simGlobalIndex]);

        double[] mayorVals = { 1.0, 1.5, 1.55 };
        bonuses.put("mayor", mayorVals[simMayorIndex]);

        BotIntegration.getRtcaStats(player, rtcaFloor, bonuses).thenAccept(json -> {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                if (simResultsList == null)
                    return;
                simResultsList.clearItems();

                if (json == null) {
                    addInfoRow(simResultsList, "Error:", "API Unavailable or Failed.");
                    return;
                }

                if (json.has("error")) {
                    addInfoRow(simResultsList, "Error:", json.get("error").getAsString());
                    return;
                }

                try {
                    int totalRuns = json.get("total_runs").getAsInt();
                    if (totalRuns == 0) {
                        startConfetti();
                        simResultsList.addItem(new Widget(0, 0, simResultsList.getWidth(), 25) {
                            @Override
                            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick) {
                                graphics.drawCenteredString(minecraft.font,
                                        "§6§l🎉 Congratulations " + player + ", you already hit Class Average 50! 🎉",
                                        x + width / 2, y + 8, 0xFFFFFFFF);
                            }
                        });
                        simResultsList.addItem(new Widget(0, 0, simResultsList.getWidth(), 30) {
                            @Override
                            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick) {
                                graphics.drawCenteredString(minecraft.font,
                                        "§eYou don't need this simulation anymore. Go touch some grass! 🌱",
                                        x + width / 2, y + 5,
                                        0xFFFFD700);
                            }
                        });
                        return;
                    }
                    addInfoRow(simResultsList, "Total Runs Needed:", String.format("%,d", totalRuns));

                    simResultsList.addItem(new Widget(0, 0, 0, 10) {

                        @Override
                        public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, float p) {
                        }
                    });

                    JsonObject results = json.getAsJsonObject("results");
                    List<String> classes = new ArrayList<>(results.keySet());
                    classes.sort(String::compareTo);

                    java.util.Map<String, Double> xpData = new java.util.HashMap<>();

                    WidgetRow header = new WidgetRow(simResultsList.getWidth() - 10, 15);
                    header.addChild(new Label(0, 0, "Class", Label.Style.BODY), 5);
                    header.addChild(new Label(0, 0, "Remaining Runs", Label.Style.BODY), 80);
                    header.addChild(new Label(0, 0, "XP to Class Lvl 50", Label.Style.BODY), 180);
                    simResultsList.addItem(header);

                    for (String cls : classes) {
                        JsonObject clsData = results.getAsJsonObject(cls);
                        int runs = clsData.get("runs_done").getAsInt();
                        double remaining = clsData.get("remaining_xp").getAsDouble();

                        xpData.put(cls, remaining);

                        WidgetRow row = new WidgetRow(simResultsList.getWidth() - 10, 15);
                        String name = cls.substring(0, 1).toUpperCase() + cls.substring(1);

                        Label nameLabel = new Label(0, 0, name, Label.Style.BODY);
                        nameLabel.setColor(Theme.ACCENT);
                        row.addChild(nameLabel, 5);

                        row.addChild(new Label(0, 0, String.format("%,d", runs), Label.Style.BODY), 80);

                        String xpText = remaining >= 1_000_000 ? String.format("%.2fM", remaining / 1_000_000)
                                : String.format("%,.0f", remaining);
                        row.addChild(new Label(0, 0, xpText, Label.Style.BODY), 180);

                        simResultsList.addItem(row);
                    }

                    simResultsList.addItem(new Widget(0, 0, 0, 10) {
                        @Override
                        public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, float p) {
                        }
                    });

                    BarGraphWidget xpGraph = new BarGraphWidget(0, 0, simResultsList.getWidth() - 10,
                            "Remaining XP per Class");
                    xpGraph.setData(xpData, "XP");

                    java.util.Map<String, Integer> classColors = new java.util.HashMap<>();
                    classColors.put("archer", 0xFFFFAA00); // Orange
                    classColors.put("berserk", 0xFFFF5555); // Red
                    classColors.put("healer", 0xFFFF55FF); // Pink
                    classColors.put("mage", 0xFF55FFFF); // Aqua
                    classColors.put("tank", 0xFF00AA00); // Green
                    xpGraph.setColorMap(classColors);

                    simResultsList.addItem(xpGraph);

                } catch (Exception e) {
                    addInfoRow(simResultsList, "Error:", "Failed to parse results.");
                    e.printStackTrace();
                }
            });
        });
    }

    private void addSectionHeader(ListView list, String title) {
        Label label = new Label(0, 0, "§l" + title, Label.Style.TITLE);
        label.setColor(Theme.ACCENT);
        label.setHeight(25);
        list.addItem(label);
    }

    private void addInfoRow(ListView list, String labelText, String valueText) {
        String fullText = labelText + (valueText.isEmpty() ? "" : " §f" + valueText);
        Label label = new Label(0, 0, fullText, Label.Style.BODY);
        label.setHeight(15);
        list.addItem(label);
    }

    private double getDouble(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : 0.0;
    }

    private int getInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : 0;
    }

    private String formatMs(int ms) {
        if (ms == 0)
            return "-";
        int seconds = ms / 1000;
        int millis = ms % 1000;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d.%03d", m, s, millis);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tabPanel != null && tabPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static class WidgetRow extends Widget {
        private final java.util.List<Widget> children = new java.util.ArrayList<>();

        public WidgetRow(int width, int height) {
            super(0, 0, width, height);
        }

        public void addChild(Widget widget, int xOffset) {
            widget.setX(x + xOffset);
            widget.setY(y);
            children.add(widget);
        }

        @Override
        public void setX(int x) {
            int diff = x - this.x;
            super.setX(x);
            for (Widget w : children)
                w.setX(w.getX() + diff);
        }

        @Override
        public void setY(int y) {
            int diff = y - this.y;
            super.setY(y);
            for (Widget w : children)
                w.setY(w.getY() + diff);
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (Widget w : children) {
                w.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void tick() {
            for (Widget w : children) {
                w.tick();
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (Widget w : children) {
                if (w.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            for (Widget w : children) {
                if (w.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void updateHoverState(int mouseX, int mouseY) {
            super.updateHoverState(mouseX, mouseY);
            for (Widget w : children) {
                w.updateHoverState(mouseX, mouseY);
            }
        }
    }

    private static class Confetti {
        double x, y;
        double speedX, speedY;
        int color;
        int life;
        int maxLife;
        float size;

        Confetti(double x, double y) {
            this.x = x;
            this.y = y;
            this.speedX = (Math.random() - 0.5) * 5;
            this.speedY = -(Math.random() * 3 + 2);
            java.awt.Color c = java.awt.Color.getHSBColor((float) Math.random(), 1f, 1f);
            this.color = c.getRGB();
            this.maxLife = 100 + (int) (Math.random() * 100);
            this.life = this.maxLife;
            this.size = (float) (Math.random() * 3 + 2);
        }
    }

    private java.util.List<Confetti> confettiParticles = new java.util.ArrayList<>();
    private boolean confettiActive = false;
    private int confettiTimer = 0;

    private void startConfetti() {
        if (confettiActive)
            return;
        stopConfetti();
        confettiActive = true;
        confettiTimer = 100;
        spawnConfettiBurst();
    }

    private void stopConfetti() {
        confettiActive = false;
        confettiTimer = 0;
        confettiParticles.clear();
    }

    private void spawnConfettiBurst() {
        for (int i = 0; i < 100; i++) {
            double startX = width / 2.0;
            double startY = height;
            double sx = (Math.random() - 0.5) * 10;
            double sy = -(Math.random() * 5 + 5);
            Confetti c = new Confetti(startX, startY);
            c.speedX = sx;
            c.speedY = sy;
            confettiParticles.add(c);
        }
    }

    private void tickConfetti() {
        if (confettiActive) {
            confettiTimer--;
            if (confettiTimer <= 0) {
                confettiActive = false;
            } else if (confettiTimer % 5 == 0 && confettiParticles.size() < 200) {
                for (int k = 0; k < 2; k++) {
                    double startX = width / 2.0 + (Math.random() - 0.5) * 100;
                    double startY = height;
                    Confetti c = new Confetti(startX, startY);
                    c.speedX = (Math.random() - 0.5) * 5;
                    c.speedY = -(Math.random() * 5 + 5);
                    confettiParticles.add(c);
                }
            }
        }

        if (confettiParticles.isEmpty())
            return;

        java.util.Iterator<Confetti> it = confettiParticles.iterator();
        while (it.hasNext()) {
            Confetti c = it.next();
            c.x += c.speedX;
            c.y += c.speedY;
            c.speedY += 0.2;
            c.life--;

            if (c.y > height + 20 || c.life <= 0) {
                it.remove();
            }
        }
    }

    private void renderConfetti(net.minecraft.client.gui.GuiGraphics graphics) {
        if (confettiParticles.isEmpty())
            return;

        for (Confetti c : confettiParticles) {
            graphics.fill((int) c.x, (int) c.y, (int) (c.x + c.size), (int) (c.y + c.size), c.color);
        }
    }
}
