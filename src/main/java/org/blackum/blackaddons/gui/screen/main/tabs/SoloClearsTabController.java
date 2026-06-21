package org.blackum.blackaddons.gui.screen.main.tabs;


import net.minecraft.ChatFormatting;

import java.util.List;

import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.config.ConfigManager.SoloClearInfo;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.debug.DemoScreen;
import org.blackum.blackaddons.gui.screen.debug.TestMenuScreen;
import org.blackum.blackaddons.gui.screen.feature.*;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.editor.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;

public class SoloClearsTabController extends SimpleTabController {
    private String selectedFloor = "F7";
    private ListView clearsList;

    public SoloClearsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    private StatBox bestTimeBox;
    private StatBox ao5Box;

    @Override
    public void init(TabPanel.Tab tab) {
        int contentX = tab.getParent().getContentX() + 20;
        int contentY = tab.getParent().getContentY() + 20;
        int contentWidth = tab.getParent().getContentWidth() - 40;

        Dropdown floorDropdown = new Dropdown(contentX, contentY, 120, Theme.BUTTON_HEIGHT, "Floor", List.of("F7", "M7"), value -> {
            selectedFloor = value;
            rebuildList();
        });
        floorDropdown.setSelectedOption(selectedFloor);
        tab.addWidget(floorDropdown);

        Button clearButton = new Button(contentX + 130, contentY, 120, Theme.BUTTON_HEIGHT, "Clear Data", () -> {
            if ("F7".equals(selectedFloor)) {
                ConfigManager.data.f7SoloClears.clear();
            } else {
                ConfigManager.data.m7SoloClears.clear();
            }
            ConfigManager.save();
            rebuildList();
        });
        tab.addWidget(clearButton);

        int statsY = contentY + 45;
        int boxWidth = (contentWidth - 20) / 2;
        bestTimeBox = new StatBox(contentX, statsY, boxWidth, "Best Time", "None");
        tab.addWidget(bestTimeBox);

        ao5Box = new StatBox(contentX + boxWidth + 20, statsY, boxWidth, "Average of 5", "None");
        tab.addWidget(ao5Box);

        clearsList = new ListView(contentX, statsY + 60, contentWidth, tab.getParent().getContentHeight() - (statsY + 60 - tab.getParent().getContentY()) - 20);
        tab.addWidget(clearsList);

        rebuildList();
    }

    private int parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty() || timeStr.equals("Unknown")) return Integer.MAX_VALUE;
        try {
            if (timeStr.contains("m") || timeStr.contains("s")) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?");
                java.util.regex.Matcher m = p.matcher(timeStr);
                if (m.find()) {
                    int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
                    int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                    int total = mins * 60 + secs;
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            } else if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                if (parts.length == 2) {
                    int total = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            }
        } catch (Exception ignored) {}
        return Integer.MAX_VALUE;
    }

    private String formatSecondsToTime(int totalSeconds) {
        if (totalSeconds == Integer.MAX_VALUE) return "Unknown";
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02dm %02ds", mins, secs);
    }

    private void rebuildList() {
        if (clearsList == null) return;
        clearsList.clearItems();

        List<SoloClearInfo> clears = "M7".equals(selectedFloor) ?
                ConfigManager.data.m7SoloClears : ConfigManager.data.f7SoloClears;

        int bestSeconds = Integer.MAX_VALUE;
        int sumLast5 = 0;
        int countLast5 = 0;

        for (int i = 0; i < clears.size(); i++) {
            int secs = parseTimeToSeconds(clears.get(i).time);
            if (secs < bestSeconds) bestSeconds = secs;

            if (i >= clears.size() - 5) {
                if (secs != Integer.MAX_VALUE) {
                    sumLast5 += secs;
                    countLast5++;
                }
            }
        }

        String bestTimeStr = bestSeconds == Integer.MAX_VALUE ? "None" : formatSecondsToTime(bestSeconds);
        String ao5Str = countLast5 == 0 ? "None" : formatSecondsToTime(sumLast5 / countLast5);

        if (bestTimeBox != null) bestTimeBox.setValue(bestTimeStr);
        if (ao5Box != null) ao5Box.setValue(ao5Str);

        if (clears.isEmpty()) {
            clearsList.addItem(new Label(0, 0, ChatFormatting.GRAY + "No " + selectedFloor + " clears recorded yet.", Label.Style.BODY));
            return;
        }

        int startIdx = Math.max(0, clears.size() - 20);
        for (int i = clears.size() - 1; i >= startIdx; i--) {
            clearsList.addItem(new SoloClearRow(clearsList.getWidth(), i, clears.get(i), selectedFloor));
            clearsList.addItem(new Widget(0, 0, 0, 5) {
                @Override
                public void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
            });
        }
    }
}
