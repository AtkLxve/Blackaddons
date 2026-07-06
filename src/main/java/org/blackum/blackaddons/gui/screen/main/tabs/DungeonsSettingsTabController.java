package org.blackum.blackaddons.gui.screen.main.tabs;

import org.blackum.blackaddons.common.util.mc.McCompat;
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
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;
import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;
import org.blackum.blackaddons.feature.dungeon.map.Room;

public class DungeonsSettingsTabController extends SimpleTabController {

    private ResizableCard solverCard;
    private ResizableCard mapCard;
    private ResizableCard keyTimerCard;

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

            ToggleSwitch tpMazeRotateToggle = new ToggleSwitch(contentX, contentY + 90, contentWidth - 20,
                    "Auto Rotate TP Maze",
                    "Automatically rotates your camera to the correct pad.",
                    ConfigManager.data.teleportMazeAutoRotate, value -> {
                        ConfigManager.data.teleportMazeAutoRotate = value;
                        ConfigManager.save();
                    });
            tpMazeRotateToggle.setMarginRight(40);
            tpMazeRotateToggle.setVisible(ConfigManager.data.teleportMazeSolverEnabled);

            final Label speedLabel = new Label(contentX, contentY + 180, "Rotation Speed: " + String.format("%.1f", ConfigManager.data.teleportMazeAutoRotateSpeed), Label.Style.BODY);
            speedLabel.setVisible(ConfigManager.data.teleportMazeSolverEnabled && ConfigManager.data.teleportMazeSmoothSnap);
            speedLabel.setMarginRight(40);

            Slider speedSlider = new Slider(contentX, contentY + 195, contentWidth - 20, 10f, 180f, ConfigManager.data.teleportMazeAutoRotateSpeed, v -> {
                ConfigManager.data.teleportMazeAutoRotateSpeed = v;
                speedLabel.setText("Rotation Speed: " + String.format("%.1f", v));
                ConfigManager.save();
            });
            speedSlider.setVisible(ConfigManager.data.teleportMazeSolverEnabled && ConfigManager.data.teleportMazeSmoothSnap);
            speedSlider.setMarginRight(40);

            ToggleSwitch tpMazeSmoothSnapToggle = new ToggleSwitch(contentX, contentY + 120, contentWidth - 20,
                    "Smooth Snap",
                    "Smooth rotation instead of instant snap.",
                    ConfigManager.data.teleportMazeSmoothSnap, value -> {
                        ConfigManager.data.teleportMazeSmoothSnap = value;
                        speedLabel.setVisible(value && ConfigManager.data.teleportMazeSolverEnabled);
                        speedSlider.setVisible(value && ConfigManager.data.teleportMazeSolverEnabled);
                        ConfigManager.save();
                    });
            tpMazeSmoothSnapToggle.setMarginRight(40);
            tpMazeSmoothSnapToggle.setVisible(ConfigManager.data.teleportMazeSolverEnabled);

            ToggleSwitch tpMazeToggle = new ToggleSwitch(contentX, contentY + 60, contentWidth - 20,
                    "Enable TP Maze Solver",
                    "Assists with solving the Teleport Maze puzzle.",
                    ConfigManager.data.teleportMazeSolverEnabled, value -> {
                        ConfigManager.data.teleportMazeSolverEnabled = value;
                        tpMazeRotateToggle.setVisible(value);
                        tpMazeSmoothSnapToggle.setVisible(value);
                        speedLabel.setVisible(value && ConfigManager.data.teleportMazeSmoothSnap);
                        speedSlider.setVisible(value && ConfigManager.data.teleportMazeSmoothSnap);
                        ConfigManager.save();
                    });
            tab.addWidget(tpMazeToggle);
            tab.addWidget(tpMazeRotateToggle);
            tab.addWidget(tpMazeSmoothSnapToggle);
            tab.addWidget(speedLabel);
            tab.addWidget(speedSlider);

            Button positionButton = new Button(contentX, contentY + 195, contentWidth - 20, 20,
                    "Change HUD Position", () -> {
                        if (Blackaddons.screenOpener != null) {
                            Blackaddons.screenOpener.accept(new OverlayEditScreen(screen, "waterboard"));
                        }
                    });
            tab.addWidget(positionButton);

            tab.addWidget(new Label(contentX, contentY + 235, "Dungeon Map", Label.Style.TITLE));

            ListView list = new ListView(contentX, contentY + 265, contentWidth - 20, 330);
            addMapSettings(list, contentWidth - 40);
            addKeyTimerSettings(list, contentWidth - 40, true);
            tab.addWidget(list);

            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("puzzleSolvers", "dungeonMap", "keyTimer");
        });
        tab.addWidget(resetLayout);

        CardContainer cardContainer = new CardContainer(contentX, contentY + 50, contentWidth, 540);
        tab.addWidget(cardContainer);

        int containerY = contentY + 60;
        int numCols = contentWidth < 680 ? 1 : (contentWidth < 1000 ? 2 : 3);
        int colWidth = 300;
        int spacing = 20;
        int[] colY = new int[numCols];
        for (int i = 0; i < numCols; i++) {
            colY[i] = containerY;
        }

        solverCard = createSolverCard(0, 0);
        mapCard = createMapCard(0, 0);
        keyTimerCard = createKeyTimerCard(0, 0);

        List<ResizableCard> cards = List.of(solverCard, mapCard, keyTimerCard);
        for (ResizableCard card : cards) {
            int shortestCol = 0;
            for (int i = 1; i < numCols; i++) {
                if (colY[i] < colY[shortestCol]) {
                    shortestCol = i;
                }
            }

            card.setX(contentX + spacing + shortestCol * (colWidth + spacing));
            card.setY(colY[shortestCol]);
            colY[shortestCol] += card.getHeight() + spacing;
        }

        cardContainer.addCard(solverCard);
        cardContainer.addCard(mapCard);
        cardContainer.addCard(keyTimerCard);
    }

    private ResizableCard createSolverCard(int x, int y) {
        solverCard = screen.createResizableCard("puzzleSolvers", x, y, 300, 290, "Puzzle Solvers");
        int contentX = solverCard.getContentX();
        int contentY = solverCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 250);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enable Water Board Solver",
                "Assists with solving the Water Board puzzle.",
                ConfigManager.data.waterBoardSolverEnabled, value -> {
                    ConfigManager.data.waterBoardSolverEnabled = value;
                    ConfigManager.save();
                });
        listView.addItem(enableToggle);

        // Pre-create widgets for the main toggle
        ToggleSwitch tpMazeRotateToggle = new ToggleSwitch(0, 0, 260,
                "Auto Rotate TP Maze",
                "Automatically rotates your camera to the correct pad.",
                ConfigManager.data.teleportMazeAutoRotate, value -> {
                    ConfigManager.data.teleportMazeAutoRotate = value;
                    ConfigManager.save();
                });
        tpMazeRotateToggle.setMarginRight(40);
        tpMazeRotateToggle.setVisible(ConfigManager.data.teleportMazeSolverEnabled);

        final Label cSpeedLabel = new Label(0, 0, "Rotation Speed: " + String.format("%.1f", ConfigManager.data.teleportMazeAutoRotateSpeed), Label.Style.BODY);
        cSpeedLabel.setVisible(ConfigManager.data.teleportMazeSolverEnabled && ConfigManager.data.teleportMazeSmoothSnap);
        cSpeedLabel.setMarginRight(40);

        Slider cSpeedSlider = new Slider(0, 0, 260, 10f, 180f, ConfigManager.data.teleportMazeAutoRotateSpeed, v -> {
            ConfigManager.data.teleportMazeAutoRotateSpeed = v;
            cSpeedLabel.setText("Rotation Speed: " + String.format("%.1f", v));
            ConfigManager.save();
        });
        cSpeedSlider.setVisible(ConfigManager.data.teleportMazeSolverEnabled && ConfigManager.data.teleportMazeSmoothSnap);
        cSpeedSlider.setMarginRight(40);

        ToggleSwitch tpMazeSmoothSnapToggle = new ToggleSwitch(0, 0, 260,
                "Smooth Snap",
                "Smooth rotation instead of instant snap.",
                ConfigManager.data.teleportMazeSmoothSnap, value -> {
                    ConfigManager.data.teleportMazeSmoothSnap = value;
                    cSpeedLabel.setVisible(value && ConfigManager.data.teleportMazeSolverEnabled);
                    cSpeedSlider.setVisible(value && ConfigManager.data.teleportMazeSolverEnabled);
                    ConfigManager.save();
                });
        tpMazeSmoothSnapToggle.setMarginRight(40);
        tpMazeSmoothSnapToggle.setVisible(ConfigManager.data.teleportMazeSolverEnabled);

        ToggleSwitch tpMazeToggle = new ToggleSwitch(0, 0, 260,
                "Enable TP Maze Solver",
                "Assists with solving the Teleport Maze puzzle.",
                ConfigManager.data.teleportMazeSolverEnabled, value -> {
                    ConfigManager.data.teleportMazeSolverEnabled = value;
                    tpMazeRotateToggle.setVisible(value);
                    tpMazeSmoothSnapToggle.setVisible(value);
                    cSpeedLabel.setVisible(value && ConfigManager.data.teleportMazeSmoothSnap);
                    cSpeedSlider.setVisible(value && ConfigManager.data.teleportMazeSmoothSnap);
                    ConfigManager.save();
                });
        listView.addItem(tpMazeToggle);
        listView.addItem(tpMazeRotateToggle);
        listView.addItem(tpMazeSmoothSnapToggle);
        listView.addItem(cSpeedLabel);
        listView.addItem(cSpeedSlider);

        Button positionButton = new Button(0, 0, 260, 20, "Change HUD Position", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen, "waterboard"));
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
                ConfigManager.data.dungeonMapEnabled, v -> {
                    ConfigManager.data.dungeonMapEnabled = v;
                    ConfigManager.save();
                }));

        list.addItem(new ToggleSwitch(0, 0, width, "Funny Map", "Shows all rooms scanned regardless of discovery",
                ConfigManager.data.dungeonFunnyMap, v -> {
                    ConfigManager.data.dungeonFunnyMap = v;
                    ConfigManager.save();
                }));

        list.addItem(new Button(0, 0, width, 20, "Set Map Position", () -> {
            if (Blackaddons.screenOpener != null)
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen, "dungeon_map"));
        }));

        list.addItem(new Label(0, 0, "Room Style", Label.Style.TITLE));
        list.addItem(new ToggleSwitch(0, 0, width, "Show Room Names", "Replaces checkmarks with names",
                ConfigManager.data.dungeonMapShowRoomNames, v -> {
                    ConfigManager.data.dungeonMapShowRoomNames = v;
                    ConfigManager.save();
                }));

        final Label nameScaleLabel = new Label(0, 0,
                "Name Scale: " + String.format("%.1f", ConfigManager.data.dungeonMapRoomNameScale * 100) + "%",
                Label.Style.BODY);
        list.addItem(nameScaleLabel);
        list.addItem(new Slider(0, 0, width, 0.1f, 2.0f, ConfigManager.data.dungeonMapRoomNameScale, v -> {
            ConfigManager.data.dungeonMapRoomNameScale = v;
            nameScaleLabel.setText("Name Scale: " + String.format("%.1f", v * 100) + "%");
            ConfigManager.save();
        }));

        list.addItem(new Label(0, 0, "Name Colors", Label.Style.TITLE));
        GridRow nameColGrid = new GridRow(width, 130);
        nameColGrid.addChild(createLabeledPicker("Discovered", ConfigManager.data.dungeonMapColorNameDiscovered,
                c -> ConfigManager.data.dungeonMapColorNameDiscovered = c, 110), 0);
        nameColGrid.addChild(createLabeledPicker("Cleared", ConfigManager.data.dungeonMapColorNameCleared,
                c -> ConfigManager.data.dungeonMapColorNameCleared = c, 110), 130);
        list.addItem(nameColGrid);

        list.addItem(new Label(0, 0, "Completed", Label.Style.BODY));
        list.addItem(new SmallColorPicker(0, 0, ConfigManager.data.dungeonMapColorNameCompleted, c -> {
            ConfigManager.data.dungeonMapColorNameCompleted = c;
            ConfigManager.save();
        }));

        list.addItem(new Label(0, 0, "Background", Label.Style.TITLE));
        GridRow bgColGrid = new GridRow(width, 130);
        bgColGrid.addChild(createLabeledPicker("Color", ConfigManager.data.dungeonMapBackgroundColor,
                c -> ConfigManager.data.dungeonMapBackgroundColor = c, 110), 0);
        bgColGrid.addChild(createLabeledPicker("Border", ConfigManager.data.dungeonMapColorBorder,
                c -> ConfigManager.data.dungeonMapColorBorder = c, 110), 130);
        list.addItem(bgColGrid);

        final Label bgOpacityLabel = new Label(0, 0,
                "Background Opacity: " + (int) (ConfigManager.data.dungeonMapBackgroundOpacity * 100) + "%",
                Label.Style.BODY);
        list.addItem(bgOpacityLabel);
        list.addItem(new Slider(0, 0, width, 0f, 1f, ConfigManager.data.dungeonMapBackgroundOpacity, v -> {
            ConfigManager.data.dungeonMapBackgroundOpacity = v;
            bgOpacityLabel.setText("Background Opacity: " + (int) (v * 100) + "%");
            ConfigManager.save();
        }));
        list.addItem(new ToggleSwitch(0, 0, width, "Show Border", "Draws a border around the map",
                ConfigManager.data.dungeonMapBorderEnabled, v -> {
                    ConfigManager.data.dungeonMapBorderEnabled = v;
                    ConfigManager.save();
                }));

        final Label borderThicknessLabel = new Label(0, 0,
                "Border Thickness: " + ConfigManager.data.dungeonMapBorderThickness + "px", Label.Style.BODY);
        list.addItem(borderThicknessLabel);
        list.addItem(new Slider(0, 0, width, 1f, 5f, ConfigManager.data.dungeonMapBorderThickness, v -> {
            ConfigManager.data.dungeonMapBorderThickness = v.intValue();
            borderThicknessLabel.setText("Border Thickness: " + v.intValue() + "px");
            ConfigManager.save();
        }));

        final Label cornerRadiusLabel = new Label(0, 0,
                "Corner Radius: " + String.format("%.1f", ConfigManager.data.dungeonMapCornerRadius) + "px",
                Label.Style.BODY);
        list.addItem(cornerRadiusLabel);
        list.addItem(new Slider(0, 0, width, 0f, 5.0f, ConfigManager.data.dungeonMapCornerRadius, v -> {
            ConfigManager.data.dungeonMapCornerRadius = v;
            cornerRadiusLabel.setText("Corner Radius: " + String.format("%.1f", v) + "px");
            ConfigManager.save();
        }));

        list.addItem(new Label(0, 0, "Room Colors", Label.Style.TITLE));
        list.addItem(new Button(0, 0, width, 20, "Reset Map Colors", () -> {
            ConfigManager.resetDungeonMapColors();
            ConfigManager.save();
            Minecraft.getInstance().execute(() -> McCompat.setScreen(Minecraft.getInstance(), null));
        }));

        GridRow roomGrid1 = new GridRow(width, 130);
        roomGrid1.addChild(createLabeledPicker("Normal", ConfigManager.data.dungeonMapColorNormal,
                c -> ConfigManager.data.dungeonMapColorNormal = c, 110), 0);
        roomGrid1.addChild(createLabeledPicker("Entrance", ConfigManager.data.dungeonMapColorEntrance,
                c -> ConfigManager.data.dungeonMapColorEntrance = c, 110), 130);
        list.addItem(roomGrid1);

        GridRow roomGrid2 = new GridRow(width, 130);
        roomGrid2.addChild(createLabeledPicker("Blood", ConfigManager.data.dungeonMapColorBlood,
                c -> ConfigManager.data.dungeonMapColorBlood = c, 110), 0);
        roomGrid2.addChild(createLabeledPicker("Fairy", ConfigManager.data.dungeonMapColorFairy,
                c -> ConfigManager.data.dungeonMapColorFairy = c, 110), 130);
        list.addItem(roomGrid2);

        GridRow roomGrid3 = new GridRow(width, 130);
        roomGrid3.addChild(createLabeledPicker("Puzzle", ConfigManager.data.dungeonMapColorPuzzle,
                c -> ConfigManager.data.dungeonMapColorPuzzle = c, 110), 0);
        roomGrid3.addChild(createLabeledPicker("Trap", ConfigManager.data.dungeonMapColorTrap,
                c -> ConfigManager.data.dungeonMapColorTrap = c, 110), 130);
        list.addItem(roomGrid3);

        list.addItem(createLabeledPicker("Champion", ConfigManager.data.dungeonMapColorChampion,
                c -> ConfigManager.data.dungeonMapColorChampion = c, width));
        list.addItem(createLabeledPicker("Mimic", ConfigManager.data.dungeonMapColorMimic,
                c -> ConfigManager.data.dungeonMapColorMimic = c, width));

        final Label darknessLabel = new Label(0, 0,
                "Darkness: " + (int) (ConfigManager.data.dungeonMapUndiscoveredDarkness * 100) + "%", Label.Style.BODY);
        list.addItem(darknessLabel);
        list.addItem(new Slider(0, 0, width, 0f, 1f, ConfigManager.data.dungeonMapUndiscoveredDarkness, v -> {
            ConfigManager.data.dungeonMapUndiscoveredDarkness = v;
            darknessLabel.setText("Darkness: " + (int) (v * 100) + "%");
            ConfigManager.save();
        }));

    }

    private void addKeyTimerSettings(ListView list, int width, boolean showTitle) {
        if (showTitle) {
            list.addItem(new Label(0, 0, "Auto Pickup Key Timer", Label.Style.TITLE));
        }
        list.addItem(new ToggleSwitch(0, 0, width, "Enable Auto Pickup Key Timer", "Shows a 10s timer when Wither/Blood keys drop",
                ConfigManager.data.keyTimerEnabled, v -> {
                    ConfigManager.data.keyTimerEnabled = v;
                    ConfigManager.save();
                }));
        list.addItem(new Button(0, 0, width, 20, "Set Position", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen, "key_timer"));
            }
        }));
    }

    private ResizableCard createKeyTimerCard(int x, int y) {
        keyTimerCard = screen.createResizableCard("keyTimer", x, y, 300, 120, "Auto Pickup Key Timer");
        int contentX = keyTimerCard.getContentX();
        int contentY = keyTimerCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 80);
        addKeyTimerSettings(listView, 260, false);

        keyTimerCard.addChild(listView);
        keyTimerCard.updateLayout();
        return keyTimerCard;
    }

    private Widget createLabeledPicker(String label, int color, Consumer<Integer> onChange, int width) {
        ListView v = new ListView(0, 0, width, 125);
        v.addItem(new Label(0, 0, label, Label.Style.BODY));
        v.addItem(new SmallColorPicker(0, 0, color, c -> {
            onChange.accept(c);
            ConfigManager.save();
        }));
        return v;
    }
}
