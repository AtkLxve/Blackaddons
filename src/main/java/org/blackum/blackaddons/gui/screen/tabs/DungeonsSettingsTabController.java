package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.WaterBoardPositionScreen;
import org.blackum.blackaddons.gui.widget.*;

public class DungeonsSettingsTabController extends SimpleTabController {

    private ResizableCard solverCard;

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

            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("puzzleSolvers");
        });
        tab.addWidget(resetLayout);

        CardContainer cardContainer = new CardContainer(contentX, contentY + 50, contentWidth, 540);
        tab.addWidget(cardContainer);

        solverCard = createSolverCard(contentX + 20, contentY + 60);
        cardContainer.addCard(solverCard);
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
}
