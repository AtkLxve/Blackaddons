package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

public class PayloadsTabController extends SimpleTabController {
    private ResizableCard customClientCard;
    private ResizableCard allowedChannelsCard;

    public PayloadsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab payloadsTab) {
        if (!ConfigManager.data.useCardLayout) {
            return;
        }

        int contentX = payloadsTab.getParent().getContentX();
        int contentY = payloadsTab.getParent().getContentY();
        int contentWidth = payloadsTab.getParent().getContentWidth();

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("customClient", "allowedChannels");
        });
        payloadsTab.addWidget(resetLayout);

        CardContainer payloadsCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        payloadsTab.addWidget(payloadsCardContainer);

        int containerY = contentY + 30;
        boolean isSingleColumn = contentWidth < 680;
        int col1X = contentX + 20;
        int col2X = contentX + 340;

        if (isSingleColumn) {
            customClientCard = createCustomClientCard(col1X, containerY + 20);
            int currentY = containerY + 20 + customClientCard.getHeight() + Theme.CARD_SPACING;

            allowedChannelsCard = createPayloadChannelsCard(col1X, currentY);
        } else {
            customClientCard = createCustomClientCard(col1X, containerY + 20);
            allowedChannelsCard = createPayloadChannelsCard(col2X, containerY + 20);
        }

        payloadsCardContainer.addCard(customClientCard);
        payloadsCardContainer.addCard(allowedChannelsCard);
    }

    private ResizableCard createCustomClientCard(int x, int y) {
        customClientCard = screen.createResizableCard("customClient", x, y, 300, 140, "Custom Client Brand");

        int contentX = customClientCard.getContentX();
        int contentY = customClientCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Set custom client brand (CUSTOM mode only)", Label.Style.BODY);
        customClientCard.addChild(description);

        TextField customClient = new TextField(contentX, contentY + 30, 180, "fabric");
        customClient.setText(ConfigManager.data.modHiderCustomClient == null ? "fabric"
                : ConfigManager.data.modHiderCustomClient);
        customClientCard.addChild(customClient);

        Button applyCustomClient = new Button(contentX + 190, contentY + 30, 70, "Apply", () -> {
            ConfigManager.data.modHiderCustomClient = customClient.getText().isBlank() ? "fabric"
                    : customClient.getText();
            ConfigManager.save();
            NotificationManager.addNotification(
                    "BlackAddons", "Saved custom client brand!",
                    NotificationType.SUCCESS);
        });
        customClientCard.addChild(applyCustomClient);

        customClientCard.updateLayout();
        return customClientCard;
    }

    private ResizableCard createPayloadChannelsCard(int x, int y) {
        allowedChannelsCard = screen.createResizableCard("allowedChannels", x, y, 300, 300,
                "Registered Channels Modifier");

        int contentX = allowedChannelsCard.getContentX();
        int contentY = allowedChannelsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "One channel per line. Example: fabric:recipe_sync", Label.Style.BODY);
        allowedChannelsCard.addChild(description);

        CodeEditorWidget codeEditor = new CodeEditorWidget(contentX, contentY + 30, 260, 170);
        String initialText = String.join("\n", ConfigManager.data.modHiderAllowedCustomPayloadChannels);
        codeEditor.setText(initialText);
        allowedChannelsCard.addChild(codeEditor);

        Button saveBtn = new Button(contentX, contentY + 210, 125, "Save", () -> {
            String text = codeEditor.getText();
            ConfigManager.data.modHiderAllowedCustomPayloadChannels.clear();
            if (text != null && !text.isBlank()) {
                String[] lines = text.split("\n", -1);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        ConfigManager.data.modHiderAllowedCustomPayloadChannels.add(line.trim());
                    }
                }
            }
            ConfigManager.save();
            NotificationManager.addNotification(
                    "BlackAddons", "Saved registered channels!",
                    NotificationType.SUCCESS);
        });
        allowedChannelsCard.addChild(saveBtn);

        Button addDefaultsBtn = new Button(contentX + 135, contentY + 210, 125, "+ Fabric Default", () -> {
            StringBuilder sb = new StringBuilder();
            if (!codeEditor.getText().isEmpty()) {
                sb.append(codeEditor.getText());
                if (!codeEditor.getText().endsWith("\n")) {
                    sb.append("\n");
                }
            }

            for (String ch : ConfigManager.FABRIC_DEFAULT_CHANNELS) {
                if (!ConfigManager.data.modHiderAllowedCustomPayloadChannels.contains(ch)) {
                    sb.append(ch).append("\n");
                }
            }
            codeEditor.setText(sb.toString());
        });
        allowedChannelsCard.addChild(addDefaultsBtn);

        allowedChannelsCard.updateLayout();
        return allowedChannelsCard;
    }
}
