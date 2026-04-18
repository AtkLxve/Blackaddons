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
public interface LazyLoadable {
    void onSelected();

    boolean isLoaded();
}
