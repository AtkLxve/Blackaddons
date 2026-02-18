package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.manager.PartyFinderManager;
import org.blackum.blackaddons.util.*;

import java.util.List;

public class PartyCreationScreen extends BaseScreen {

    private String selectedFloor = "M7";

    public PartyCreationScreen(Screen parent) {
        super(Component.literal("Create Dungeon Party"), parent);
    }

    @Override
    public void init() {
        this.width = MinecraftInstance.mc.getWindow().getGuiScaledWidth();
        this.height = MinecraftInstance.mc.getWindow().getGuiScaledHeight();

        this.containerWidth = 200;
        this.containerHeight = 150;
        this.containerX = (this.width - this.containerWidth) / 2;
        this.containerY = (this.height - this.containerHeight) / 2;

        widgets.clear();
        initWidgets();

        int maxWidgetY = 0;
        for (Widget w : widgets) {
            int relativeBottom = (w.getY() + w.getHeight()) - this.containerY;
            if (relativeBottom > maxWidgetY) {
                maxWidgetY = relativeBottom;
            }
        }
        this.baseContentHeight = Math.max(0, maxWidgetY + 20);
        this.contentHeight = this.baseContentHeight;
    }

    @Override
    protected void initWidgets() {
        int contentX = containerX + 15;
        int contentY = containerY + 15;

        widgets.add(new Label(contentX, contentY, "Create Party", Label.Style.TITLE));

        widgets.add(new Label(contentX, contentY + 25, "Select Floor:", Label.Style.BODY));

        List<String> floors = List.of("M7", "M6", "M5", "M4", "M3", "M2", "M1", "F7", "F6", "F5", "F4", "F3", "F2",
                "F1", "Entrance");
        Dropdown floorDropdown = new Dropdown(contentX, contentY + 40, 170, 25, "Select Floor", floors, floor -> {
            this.selectedFloor = floor;
        });
        floorDropdown.setSelectedOption(selectedFloor);

        Button createBtn = new Button(contentX, contentY + 75, 80, 25, "Create", () -> {
            PartyFinderManager.getInstance().createParty(selectedFloor, new JsonObject());
            onClose();
        });
        widgets.add(createBtn);

        Button cancelBtn = new Button(contentX + 90, contentY + 75, 80, 25, "Cancel", this::onClose);
        widgets.add(cancelBtn);

        widgets.add(floorDropdown);
    }
}
