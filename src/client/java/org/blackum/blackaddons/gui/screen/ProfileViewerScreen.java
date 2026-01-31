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
        super(Component.literal("Profile: " + player), parent);
        this.player = player;
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
        tab.addWidget(list);

        addSectionHeader(list, "General Stats");

        double cataXp = getDouble(profileData, "catacombs");
        int secretCount = getInt(profileData, "secrets");
        int bloodKills = getInt(profileData, "blood_mob_kills");

        addInfoRow(list, "Catacombs XP:", String.format("%,.0f", cataXp));
        addInfoRow(list, "Secrets Found:", String.format("%,d", secretCount));
        addInfoRow(list, "Blood Mob Kills:", String.format("%,d", bloodKills));

        addSectionHeader(list, "Classes");

        if (profileData.has("classes")) {
            JsonObject classes = profileData.getAsJsonObject("classes");
            List<Map.Entry<String, JsonElement>> sortedClasses = new ArrayList<>(classes.entrySet());
            sortedClasses.sort((e1, e2) -> Double.compare(e2.getValue().getAsDouble(), e1.getValue().getAsDouble()));

            for (Map.Entry<String, JsonElement> entry : sortedClasses) {
                String className = entry.getKey();
                double xp = entry.getValue().getAsDouble();
                addInfoRow(list, className + ":", String.format("%,.0f XP", xp));
            }
        }

        addSectionHeader(list, "Dungeon Floors");

        if (profileData.has("floors")) {
            JsonObject floors = profileData.getAsJsonObject("floors");
            List<String> keys = new ArrayList<>(floors.keySet());
            keys.sort((k1, k2) -> {
                boolean m1 = k1.startsWith("M");
                boolean m2 = k2.startsWith("M");
                if (m1 && !m2)
                    return -1;
                if (!m1 && m2)
                    return 1;
                return k2.compareTo(k1);
            });

            for (String key : keys) {
                JsonObject floorData = floors.getAsJsonObject(key);
                int runs = getInt(floorData, "runs");
                if (runs == 0)
                    continue;

                int bestScore = getInt(floorData, "best_score");
                String sPlus = formatMs(getInt(floorData, "fastest_s_plus"));
                String s = formatMs(getInt(floorData, "fastest_s"));

                String left = "§b" + key + "§r";
                String right = String.format("%d Runs | %d Score | §7S+: %s | S: %s", runs, bestScore, sPlus, s);
                addInfoRow(list, left, right);
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

        public TeammateRow(int width, Teammate tm) {
            super(0, 0, width, 18);
            this.tm = tm;
            this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
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

            String ignText = "§b" + tm.ign;
            graphics.drawString(minecraft.font, ignText, cx, cy, 0xFFFFFFFF);
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
        }

        @Override
        public void tick() {
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
            if (visible && isMouseOver(mouseX, mouseY) && button == 0) {
                if (minecraft.player != null) {
                    minecraft.player.connection.sendCommand("ba pv " + tm.ign);
                    return true;
                }
            }
            return false;
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
        TabPanel.Tab tab = tabPanel.addTab("Teammates");

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
        Label label = new Label(0, 0, "§6§l" + title, Label.Style.TITLE);
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
