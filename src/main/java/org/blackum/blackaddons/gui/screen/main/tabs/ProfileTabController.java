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
import net.minecraft.ChatFormatting;
import org.blackum.blackaddons.gui.screen.feature.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.base.Label;
import org.blackum.blackaddons.gui.widget.layout.ListView;
import org.blackum.blackaddons.gui.widget.layout.TabPanel;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.base.SectionHeader;
import org.blackum.blackaddons.common.util.format.FormatUtils;
import org.blackum.blackaddons.common.util.io.JsonUtils;

public abstract class ProfileTabController implements LazyLoadable {
    protected final ProfileViewerScreen screen;
    protected final JsonObject profileData;

    public ProfileTabController(ProfileViewerScreen screen, JsonObject profileData) {
        this.screen = screen;
        this.profileData = profileData;
    }

    public abstract void init(TabPanel.Tab tab);

    @Override
    public void onSelected() {
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    protected double getDouble(JsonObject json, String key) {
        return JsonUtils.getDouble(json, key);
    }

    protected int getInt(JsonObject json, String key) {
        return JsonUtils.getInt(json, key);
    }

    protected void addSectionHeader(ListView list, String title) {
        list.addItem(new SectionHeader(list.getWidth(), title));
    }

    protected String formatMs(int ms) {
        return FormatUtils.formatMs(ms);
    }

    protected void addInfoRow(ListView list, String labelText, String valueText) {
        String fullText = labelText + (valueText.isEmpty() ? "" : " " + ChatFormatting.WHITE + valueText);
        Label label = new Label(0, 0, fullText, Label.Style.BODY);
        label.setHeight(15);
        list.addItem(label);
    }

    protected String formatRelativeTime(long timestamp) {
        return FormatUtils.formatRelativeTime(timestamp);
    }

    protected String formatNumber(double value) {
        return FormatUtils.formatNumber(value);
    }
}
