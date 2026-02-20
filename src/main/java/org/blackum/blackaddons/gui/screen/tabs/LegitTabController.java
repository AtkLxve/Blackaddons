package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.*;

public class LegitTabController extends SimpleTabController {
    private ResizableCard fullbrightCard;

    public LegitTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab legitTab) {
        int contentX = legitTab.getParent().getContentX();
        int contentY = legitTab.getParent().getContentY();
        int contentWidth = legitTab.getParent().getContentWidth();

        if (ConfigManager.data.useCardLayout) {
            Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
                screen.resetCardStates("fullbright");
            });
            legitTab.addWidget(resetLayout);

            CardContainer legitCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
            legitTab.addWidget(legitCardContainer);

            int currentY = contentY + 50;
            fullbrightCard = createFullbrightCard(contentX + 20, currentY);

            legitCardContainer.addCard(fullbrightCard);
            return;
        }

        legitTab.addWidget(new Label(contentX, contentY, "Fullbright", Label.Style.TITLE));

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                ConfigManager.data.legitFullbrightEnabled, value -> {
                    ConfigManager.data.legitFullbrightEnabled = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fullbrightToggle);
    }

    private ResizableCard createFullbrightCard(int x, int y) {
        fullbrightCard = screen.createResizableCard("fullbright", x, y, 300, 100, "Fullbright");
        int contentX = fullbrightCard.getContentX();
        int contentY = fullbrightCard.getContentY();

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                ConfigManager.data.legitFullbrightEnabled, value -> {
                    ConfigManager.data.legitFullbrightEnabled = value;
                    ConfigManager.save();
                });
        fullbrightCard.addChild(fullbrightToggle);

        fullbrightCard.updateLayout();
        return fullbrightCard;
    }
}
