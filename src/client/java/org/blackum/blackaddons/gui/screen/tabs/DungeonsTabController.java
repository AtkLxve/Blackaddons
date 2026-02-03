package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DungeonsTabController extends ProfileTabController {

    public DungeonsTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int w = tab.getParent().getContentWidth() - 20;
        ListView list = new ListView(tab.getParent().getContentX(), tab.getParent().getContentY(), w,
                tab.getParent().getMaxContentHeight());
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
                graphics.drawCenteredString(Minecraft.getInstance().font, label, x + w / 2, y + 10, Theme.ACCENT);
                graphics.drawCenteredString(Minecraft.getInstance().font, "§f" + value, x + w / 2, y + 25, 0xFFFFFFFF);
            }
        };
        list.addItem(generalStats);

        if (profileData.has("classes")) {
            JsonObject classes = profileData.getAsJsonObject("classes");
            java.util.Map<String, Double> classData = new java.util.HashMap<>();
            List<Map.Entry<String, JsonElement>> sorted = new ArrayList<>(classes.entrySet());
            sorted.sort((e1, e2) -> Double.compare(e2.getValue().getAsDouble(), e1.getValue().getAsDouble()));

            double totalLevel = 0;

            for (Map.Entry<String, JsonElement> entry : sorted) {
                double xp = entry.getValue().getAsDouble();
                double lvl = org.blackum.blackaddons.util.DungeonUtils.getCataLevel(xp);
                classData.put(entry.getKey(), lvl);

                if (List.of("archer", "berserk", "healer", "mage", "tank").contains(entry.getKey().toLowerCase())) {
                    totalLevel += lvl;
                }
            }

            double classAvg = totalLevel / 5.0;
            if (classAvg >= 50.0) {
                screen.startConfetti();
                list.addItem(new Widget(0, 0, effectiveW, 25) {
                    @Override
                    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                            float partialTick) {
                        graphics.drawCenteredString(Minecraft.getInstance().font, "§6§l🎉 CLASS AVERAGE 50! 🎉",
                                x + width / 2, y + 8,
                                0xFFFFFFFF);
                    }
                });
                list.addItem(new Widget(0, 0, effectiveW, 20) {
                    @Override
                    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                            float partialTick) {
                        graphics.drawCenteredString(Minecraft.getInstance().font,
                                "§eCongratulations! You need to touch some grass!", x + width / 2, y + 5,
                                0xFFFFD700);
                    }
                });
            }

            BarGraphWidget graph = new BarGraphWidget(
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
            FloorCardWidget entCard = createFloorCard(effectiveW, "Entrance", data);
            list.addItem(entCard);
        }

        if (!runDistribution.isEmpty()) {
            java.util.Map<String, Double> formattedRunDist = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : runDistribution.entrySet()) {
                String k = entry.getKey();
                String label = (k.equals("F0") || k.equals("Entrance")) ? "Entrance" : k;
                formattedRunDist.put(label, entry.getValue());
            }

            BarGraphWidget runGraph = new BarGraphWidget(
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

    private FloorCardWidget createFloorCard(int width, String name, JsonObject data) {
        int runs = getInt(data, "runs");
        int best = getInt(data, "best_score");
        String sPlus = formatMs(getInt(data, "fastest_s_plus"));
        String s = formatMs(getInt(data, "fastest_s"));
        return new FloorCardWidget(width, name, runs, best, sPlus, s);
    }
}
