package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;
import org.blackum.blackaddons.gui.widget.Button;
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
        tabPanel.setOnTabChange(index -> lastTabIndex = index);
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
        int cataRuns = 0;
        int masterRuns = 0;

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
                    masterRuns += r;
                    masterFloors.add(key);
                } else {
                    cataRuns += r;
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
        final int finalMasterRuns = masterRuns;
        final int finalCataRuns = cataRuns;
        int effectiveW = w - 8;

        addSectionHeader(list, "General Stats");
        Widget generalStats = new Widget(0, 0, effectiveW, 120) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                int boxW = (width - 10) / 3;
                int row2Y = y + 65;

                drawStatBox(graphics, x, y, boxW, "Catacombs XP", String.format("%,.0f", cataXp));
                drawStatBox(graphics, x + boxW + 5, y, boxW, "Secrets Found", String.format("%,d", secretCount));
                drawStatBox(graphics, x + (boxW + 5) * 2, y, boxW, "Secrets/Run", String.format("%.2f", secretsPerRun));
                drawStatBox(graphics, x, row2Y, boxW, "Total Runs", String.format("%,d", finalTotalRuns));
                drawStatBox(graphics, x + boxW + 5, row2Y, boxW, "Master Runs", String.format("%,d", finalMasterRuns));
                drawStatBox(graphics, x + (boxW + 5) * 2, row2Y, boxW, "Cata Runs",
                        String.format("%,d", finalCataRuns));
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

        double cataLvl = org.blackum.blackaddons.util.DungeonUtils.getCataLevel(cataXp);
        Widget cataHeader = new Widget(0, 0, effectiveW, 40) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, x, y, width, height,
                        Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);

                String title = "§lCatacombs Level";
                String val = String.format("§f%.2f", cataLvl);

                graphics.drawString(minecraft.font, title, x + 10, y + 10, Theme.ACCENT);

                int valWidth = minecraft.font.width(val);
                graphics.drawString(minecraft.font, val, x + width - valWidth - 10, y + 10, 0xFFFFFFFF);

                int barX = x + 10;
                int barY = y + 25;
                int barW = width - 20;
                int barH = 6;

                double progress = cataLvl % 1.0;
                int fillW = (int) (barW * progress);

                org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, barX, barY, barW, barH, 3,
                        Theme.BACKGROUND_TERTIARY);

                if (fillW > 0) {
                    org.blackum.blackaddons.gui.util.RenderHelper.renderRoundedRect(graphics, barX, barY, fillW, barH,
                            3, Theme.ACCENT_PRIMARY);
                }
            }
        };
        list.addItem(cataHeader);

        if (profileData.has("classes")) {
            JsonObject classes = profileData.getAsJsonObject("classes");
            java.util.Map<String, Double> classData = new java.util.HashMap<>();
            List<Map.Entry<String, JsonElement>> sorted = new ArrayList<>(classes.entrySet());
            sorted.sort((e1, e2) -> Double.compare(e2.getValue().getAsDouble(), e1.getValue().getAsDouble()));

            for (Map.Entry<String, JsonElement> entry : sorted) {
                double xp = entry.getValue().getAsDouble();
                classData.put(entry.getKey(), org.blackum.blackaddons.util.DungeonUtils.getCataLevel(xp));
            }

            org.blackum.blackaddons.gui.widget.BarGraphWidget graph = new org.blackum.blackaddons.gui.widget.BarGraphWidget(
                    0, 0, effectiveW, "Class Levels");
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
    public void tick() {
        super.tick();
        if (searchField != null) {
            String currentText = searchField.getText();
            if (!currentText.equals(lastSearchText)) {
                lastSearchText = currentText;
                updateTeammatesList();
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
        ListView list = new ListView(tabPanel.getContentX(), tabPanel.getContentY(), tabPanel.getContentWidth(),
                tabPanel.getMaxContentHeight());
        tab.addWidget(list);

        if (profileData.has("daily_stats") && !profileData.get("daily_stats").isJsonNull()) {
            JsonObject daily = profileData.getAsJsonObject("daily_stats");
            if (daily.size() == 0) {
                addInfoRow(list, "No daily stats recorded yet.", "");
            } else {
                double cataGained = getDouble(daily, "cata_gained");
                addInfoRow(list, "Cata XP Gained:", String.format("%,.0f", cataGained));

                addSectionHeader(list, "Class XP Gained");
                if (daily.has("classes")) {
                    JsonObject classes = daily.getAsJsonObject("classes");
                    for (String key : classes.keySet()) {
                        JsonObject clsData = classes.getAsJsonObject(key);
                        double gained = getDouble(clsData, "gained");
                        if (gained > 0) {
                            addInfoRow(list, key + ":", String.format("%,.0f", gained));
                        }
                    }
                }

                addSectionHeader(list, "Runs Gained");
                if (daily.has("runs")) {
                    JsonObject runs = daily.getAsJsonObject("runs");
                    handleDailyRuns(list, runs.getAsJsonObject("normal"), "F");
                    handleDailyRuns(list, runs.getAsJsonObject("master"), "M");
                }
            }
        } else {
            addInfoRow(list, "Daily stats not tracked for this user.", "");
            addInfoRow(list, "Use /link <ign> in Discord to track.", "");
        }
    }

    private void handleDailyRuns(ListView list, JsonObject runs, String prefix) {
        for (String key : runs.keySet()) {
            int count = runs.get(key).getAsInt();
            if (count > 0) {
                String floor = key.equals("0") && prefix.equals("F") ? "Entrance" : prefix + key;
                addInfoRow(list, floor + ":", "+" + count);
            }
        }
    }

    private void initRtcaTab() {
        TabPanel.Tab tab = tabPanel.addTab("RTCA");
        int w = tabPanel.getContentWidth();
        ListView list = new ListView(tabPanel.getContentX(), tabPanel.getContentY(), w, tabPanel.getMaxContentHeight());
        tab.addWidget(list);

        if (profileData.has("classes")) {
            addInfoRow(list, "RTCA Info", "Class Average tracking.");
            addInfoRow(list, "Status", "Simulations available in Discord.");
        }
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
}
