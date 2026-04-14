package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.gui.screen.DungeonMapPositionScreen;
import org.blackum.blackaddons.gui.screen.WaterBoardPositionScreen;
import org.blackum.blackaddons.gui.widget.*;

public class DungeonsSettingsTabController extends SimpleTabController {

    private ResizableCard solverCard;
    private ResizableCard mapCard;

    public DungeonsSettingsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int contentX = tab.getParent().getContentX();
        int contentY = tab.getParent().getContentY();
        int contentWidth = tab.getParent().getContentWidth();

        if (!ConfigManager.data.useCardLayout) {
            tab.addWidget(new Label(contentX, contentY, "Puzzle Solvers", Label.Style.TITLE));

            ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                    "Enable Water Board Solver",
                    "Assists with solving the Water Board puzzle.",
                    ConfigManager.data.waterBoardSolverEnabled, value -> {
                ConfigManager.data.waterBoardSolverEnabled = value;
                ConfigManager.save();
            });
            tab.addWidget(enableToggle);

            Button positionButton = new Button(contentX, contentY + 60, contentWidth - 20, 20, 
                    "Change HUD Position", () -> {
                if (Blackaddons.screenOpener != null) {
                    Blackaddons.screenOpener.accept(new WaterBoardPositionScreen(screen));
                }
            });
            tab.addWidget(positionButton);

            tab.addWidget(new Label(contentX, contentY + 100, "Dungeon Map", Label.Style.TITLE));
            
            ListView list = new ListView(contentX, contentY + 130, contentWidth - 20, 400);
            addMapSettings(list, contentWidth - 40);
            tab.addWidget(list);

            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("puzzleSolvers", "dungeonMap");
        });
        tab.addWidget(resetLayout);

        CardContainer cardContainer = new CardContainer(contentX, contentY + 50, contentWidth, 540);
        tab.addWidget(cardContainer);

        solverCard = createSolverCard(contentX + 20, contentY + 60);
        cardContainer.addCard(solverCard);

        mapCard = createMapCard(contentX + 340, contentY + 60);
        cardContainer.addCard(mapCard);
    }

    private ResizableCard createSolverCard(int x, int y) {
        solverCard = screen.createResizableCard("puzzleSolvers", x, y, 300, 150, "Puzzle Solvers");
        int contentX = solverCard.getContentX();
        int contentY = solverCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 110);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enable Water Board Solver",
                "Assists with solving the Water Board puzzle.",
                ConfigManager.data.waterBoardSolverEnabled, value -> {
            ConfigManager.data.waterBoardSolverEnabled = value;
            ConfigManager.save();
        });
        listView.addItem(enableToggle);

        Button positionButton = new Button(0, 0, 260, 20, "Change HUD Position", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new WaterBoardPositionScreen(screen));
            }
        });
        listView.addItem(positionButton);

        solverCard.addChild(listView);
        solverCard.updateLayout();
        return solverCard;
    }

    private ResizableCard createMapCard(int x, int y) {
        mapCard = screen.createResizableCard("dungeonMap", x, y, 300, 450, "Dungeon Map");
        int contentX = mapCard.getContentX();
        int contentY = mapCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 410);
        addMapSettings(listView, 260);

        mapCard.addChild(listView);
        mapCard.updateLayout();
        return mapCard;
    }

    private void addMapSettings(ListView list, int width) {
        list.addItem(new ToggleSwitch(0, 0, width, "Enable Map", "Shows the dungeon minimap", 
            ConfigManager.data.dungeonMapEnabled, v -> { ConfigManager.data.dungeonMapEnabled = v; ConfigManager.save(); }));
        
        list.addItem(new ToggleSwitch(0, 0, width, "Funny Map", "Shows all rooms scanned regardless of discovery", 
            ConfigManager.data.dungeonFunnyMap, v -> { ConfigManager.data.dungeonFunnyMap = v; ConfigManager.save(); }));

        list.addItem(new Button(0, 0, width, 20, "Set Map Position", () -> {
            if (Blackaddons.screenOpener != null) Blackaddons.screenOpener.accept(new DungeonMapPositionScreen(screen));
        }));

        list.addItem(new Label(0, 0, "Room Style", Label.Style.TITLE));
        list.addItem(new ToggleSwitch(0, 0, width, "Show Room Names", "Replaces checkmarks with names",
            ConfigManager.data.dungeonMapShowRoomNames, v -> { ConfigManager.data.dungeonMapShowRoomNames = v; ConfigManager.save(); }));

        final Label nameScaleLabel = new Label(0, 0, "Name Scale: " + String.format("%.1f", ConfigManager.data.dungeonMapRoomNameScale * 100) + "%", Label.Style.BODY);
        list.addItem(nameScaleLabel);
        list.addItem(new Slider(0, 0, width, 0.1f, 2.0f, ConfigManager.data.dungeonMapRoomNameScale, v -> {
            ConfigManager.data.dungeonMapRoomNameScale = v;
            nameScaleLabel.setText("Name Scale: " + String.format("%.1f", v * 100) + "%");
            ConfigManager.save();
        }));

        list.addItem(new Label(0, 0, "Name Colors", Label.Style.TITLE));
        GridRow nameColGrid = new GridRow(width, 130);
        nameColGrid.addChild(createLabeledPicker("Discovered", ConfigManager.data.dungeonMapColorNameDiscovered, c -> ConfigManager.data.dungeonMapColorNameDiscovered = c, 110), 0);
        nameColGrid.addChild(createLabeledPicker("Cleared", ConfigManager.data.dungeonMapColorNameCleared, c -> ConfigManager.data.dungeonMapColorNameCleared = c, 110), 130);
        list.addItem(nameColGrid);

        list.addItem(new Label(0, 0, "Completed", Label.Style.BODY));
        list.addItem(new SmallColorPicker(0, 0, ConfigManager.data.dungeonMapColorNameCompleted, c -> { ConfigManager.data.dungeonMapColorNameCompleted = c; ConfigManager.save(); }));

        list.addItem(new Label(0, 0, "Background", Label.Style.TITLE));
        GridRow bgColGrid = new GridRow(width, 130);
        bgColGrid.addChild(createLabeledPicker("Color", ConfigManager.data.dungeonMapBackgroundColor, c -> ConfigManager.data.dungeonMapBackgroundColor = c, 110), 0);
        bgColGrid.addChild(createLabeledPicker("Border", ConfigManager.data.dungeonMapColorBorder, c -> ConfigManager.data.dungeonMapColorBorder = c, 110), 130);
        list.addItem(bgColGrid);

        final Label bgOpacityLabel = new Label(0, 0, "Background Opacity: " + (int)(ConfigManager.data.dungeonMapBackgroundOpacity * 100) + "%", Label.Style.BODY);
        list.addItem(bgOpacityLabel);
        list.addItem(new Slider(0, 0, width, 0f, 1f, ConfigManager.data.dungeonMapBackgroundOpacity, v -> {
            ConfigManager.data.dungeonMapBackgroundOpacity = v;
            bgOpacityLabel.setText("Background Opacity: " + (int)(v * 100) + "%");
            ConfigManager.save();
        }));
        list.addItem(new ToggleSwitch(0, 0, width, "Show Border", "Draws a border around the map",
            ConfigManager.data.dungeonMapBorderEnabled, v -> { ConfigManager.data.dungeonMapBorderEnabled = v; ConfigManager.save(); }));

        final Label borderThicknessLabel = new Label(0, 0, "Border Thickness: " + ConfigManager.data.dungeonMapBorderThickness + "px", Label.Style.BODY);
        list.addItem(borderThicknessLabel);
        list.addItem(new Slider(0, 0, width, 1f, 5f, ConfigManager.data.dungeonMapBorderThickness, v -> {
            ConfigManager.data.dungeonMapBorderThickness = v.intValue();
            borderThicknessLabel.setText("Border Thickness: " + v.intValue() + "px");
            ConfigManager.save();
        }));

        list.addItem(new Label(0, 0, "Room Colors", Label.Style.TITLE));
        list.addItem(new Button(0, 0, width, 20, "Reset Map Colors", () -> {
            ConfigManager.resetDungeonMapColors();
            ConfigManager.save();
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null));
        }));
        
        GridRow roomGrid1 = new GridRow(width, 130);
        roomGrid1.addChild(createLabeledPicker("Normal", ConfigManager.data.dungeonMapColorNormal, c -> ConfigManager.data.dungeonMapColorNormal = c, 110), 0);
        roomGrid1.addChild(createLabeledPicker("Entrance", ConfigManager.data.dungeonMapColorEntrance, c -> ConfigManager.data.dungeonMapColorEntrance = c, 110), 130);
        list.addItem(roomGrid1);

        GridRow roomGrid2 = new GridRow(width, 130);
        roomGrid2.addChild(createLabeledPicker("Blood", ConfigManager.data.dungeonMapColorBlood, c -> ConfigManager.data.dungeonMapColorBlood = c, 110), 0);
        roomGrid2.addChild(createLabeledPicker("Fairy", ConfigManager.data.dungeonMapColorFairy, c -> ConfigManager.data.dungeonMapColorFairy = c, 110), 130);
        list.addItem(roomGrid2);

        GridRow roomGrid3 = new GridRow(width, 130);
        roomGrid3.addChild(createLabeledPicker("Puzzle", ConfigManager.data.dungeonMapColorPuzzle, c -> ConfigManager.data.dungeonMapColorPuzzle = c, 110), 0);
        roomGrid3.addChild(createLabeledPicker("Trap", ConfigManager.data.dungeonMapColorTrap, c -> ConfigManager.data.dungeonMapColorTrap = c, 110), 130);
        list.addItem(roomGrid3);

        list.addItem(createLabeledPicker("Champion", ConfigManager.data.dungeonMapColorChampion, c -> ConfigManager.data.dungeonMapColorChampion = c, width));
        list.addItem(createLabeledPicker("Mimic", ConfigManager.data.dungeonMapColorMimic, c -> ConfigManager.data.dungeonMapColorMimic = c, width));
        
        final Label darknessLabel = new Label(0, 0, "Darkness: " + (int)(ConfigManager.data.dungeonMapUndiscoveredDarkness * 100) + "%", Label.Style.BODY);
        list.addItem(darknessLabel);
        list.addItem(new Slider(0, 0, width, 0f, 1f, ConfigManager.data.dungeonMapUndiscoveredDarkness, v -> {
            ConfigManager.data.dungeonMapUndiscoveredDarkness = v;
            darknessLabel.setText("Darkness: " + (int)(v * 100) + "%");
            ConfigManager.save();
        }));
    }

    private Widget createLabeledPicker(String label, int color, Consumer<Integer> onChange, int width) {
        ListView v = new ListView(0, 0, width, 125);
        v.addItem(new Label(0, 0, label, Label.Style.BODY));
        v.addItem(new SmallColorPicker(0, 0, color, c -> { onChange.accept(c); ConfigManager.save(); }));
        return v;
    }
}
