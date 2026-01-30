package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.payload.PayloadManager;
import org.blackum.blackaddons.payload.PayloadOverride;

public class PayloadEditorScreen extends BaseScreen {

    private final PayloadOverride override;
    private final boolean isNew;
    private final Runnable onSave;

    private TextField channelField;
    private CodeEditorWidget originalField;
    private CodeEditorWidget replacementField;

    public PayloadEditorScreen(Screen parent, PayloadOverride override, boolean isNew, Runnable onSave) {
        super(Component.literal("Edit Payload Override"), parent);
        this.override = override;
        this.isNew = isNew;
        this.onSave = onSave;
    }

    @Override
    protected void initWidgets() {
        int contentX = containerX + 20;
        int contentY = containerY + 20;
        int width = containerWidth - 40;

        addWidget(new Label(contentX, contentY, isNew ? "Create Override" : "Edit Override", Label.Style.TITLE));

        addWidget(new Label(contentX, contentY + 30, "Channel", Label.Style.BODY));
        channelField = new TextField(contentX, contentY + 45, width, "Channel ID (e.g. firmament:handshake)");
        channelField.setText(override.channel != null ? override.channel : "");
        addWidget(channelField);

        addWidget(new Label(contentX, contentY + 80, "Original Data (Hex)", Label.Style.BODY));
        originalField = new CodeEditorWidget(contentX, contentY + 95, width, 100);
        originalField.setText(override.originalData != null ? override.originalData : "");
        addWidget(originalField);

        addWidget(new Label(contentX, contentY + 210, "Replacement Data (Hex)", Label.Style.BODY));
        replacementField = new CodeEditorWidget(contentX, contentY + 225, width, 100);
        replacementField.setText(override.replacementData != null ? override.replacementData : "");
        addWidget(replacementField);

        Button saveButton = new Button(contentX, containerY + containerHeight - 40, 100, "Save", () -> {
            override.channel = channelField.getText();
            override.originalData = originalField.getText().replace("\n", "").replace(" ", "");
            override.replacementData = replacementField.getText().replace("\n", "").replace(" ", "");
            override.enabled = true;

            if (isNew) {
                PayloadManager.addOverride(override);
            }
            ConfigManager.save();
            if (onSave != null)
                onSave.run();
            onClose();
        });
        addWidget(saveButton);

        Button cancelButton = new Button(contentX + 110, containerY + containerHeight - 40, 100, "Cancel",
                this::onClose);
        addWidget(cancelButton);

        this.contentHeight = containerHeight;
    }
}
