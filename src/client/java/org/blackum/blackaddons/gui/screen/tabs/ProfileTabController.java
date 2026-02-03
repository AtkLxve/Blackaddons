package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonObject;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.ListView;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.theme.Theme;

public abstract class ProfileTabController {
    protected final ProfileViewerScreen screen;
    protected final JsonObject profileData;

    public ProfileTabController(ProfileViewerScreen screen, JsonObject profileData) {
        this.screen = screen;
        this.profileData = profileData;
    }

    public abstract void init(TabPanel.Tab tab);

    protected double getDouble(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : 0.0;
    }

    protected int getInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : 0;
    }

    protected void addSectionHeader(ListView list, String title) {
        Label label = new Label(0, 0, "§l" + title, Label.Style.TITLE);
        label.setColor(Theme.ACCENT);
        label.setHeight(25);
        list.addItem(label);
    }

    protected String formatMs(int ms) {
        if (ms == 0)
            return "-";
        int seconds = ms / 1000;
        int millis = ms % 1000;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d.%03d", m, s, millis);
    }

    protected void addInfoRow(ListView list, String labelText, String valueText) {
        String fullText = labelText + (valueText.isEmpty() ? "" : " §f" + valueText);
        Label label = new Label(0, 0, fullText, Label.Style.BODY);
        label.setHeight(15);
        list.addItem(label);
    }

    protected String formatRelativeTime(long timestamp) {
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

    protected String formatNumber(double value) {
        if (value >= 1_000_000_000) {
            return String.format("%.1fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format("%.0fk", value / 1_000);
        } else {
            return String.format("%,.0f", value);
        }
    }
}
