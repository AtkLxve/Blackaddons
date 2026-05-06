package org.blackum.blackaddons.gui.widget.input;

import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StringListWidget extends Widget {
    private final List<String> items;
    private final Runnable onChange;
    
    private final TextField inputField;
    private final Button addButton;
    
    private final List<Row> rows = new ArrayList<>();
    private final int rowHeight = 22;

    public StringListWidget(int x, int y, int width, List<String> initialItems, String placeholder, Supplier<List<String>> suggestionProvider, Runnable onChange) {
        super(x, y, width, 20);
        this.items = initialItems == null ? new ArrayList<>() : initialItems;
        this.onChange = onChange;
        
        int buttonWidth = 20;
        int fieldWidth = width - buttonWidth - 4;
        
        if (suggestionProvider != null) {
            AutocompleteTextField autoField = new AutocompleteTextField(x, y, fieldWidth, 20, placeholder, suggestionProvider);
            autoField.setOnSelect(val -> {
                autoField.setText(val);
            });
            this.inputField = autoField;
        } else {
            this.inputField = new TextField(x, y, fieldWidth, 20, placeholder);
        }
        
        this.addButton = new Button(x + width - buttonWidth, y, buttonWidth, 20, "+", () -> {
            String val = inputField.getText();
            if (val != null && !val.trim().isEmpty()) {
                items.add(val.trim());
                inputField.setText("");
                rebuildRows();
                triggerChange();
            }
        });
        
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        int currentY = y + 24;
        int buttonWidth = 20;
        int fieldWidth = width - buttonWidth - 4;
        
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            TextField field = new TextField(x, currentY, fieldWidth, 20, "");
            field.setText(items.get(i));
            field.setOnValueChange(val -> {
                items.set(index, val);
                triggerChange();
            });
            
            Button removeBtn = new Button(x + width - buttonWidth, currentY, buttonWidth, 20, "-", () -> {
                items.remove(index);
                rebuildRows();
                triggerChange();
            });
            
            rows.add(new Row(field, removeBtn));
            currentY += rowHeight;
        }
        
        this.height = 20 + (items.size() * rowHeight) + (items.isEmpty() ? 0 : 4);
    }

    private void triggerChange() {
        if (onChange != null) onChange.run();
    }

    public void setPlaceholder(String placeholder) {
        if (inputField != null) {
            inputField.setPlaceholder(placeholder);
        }
    }

    public void refreshItems() {
        rebuildRows();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        inputField.setX(x);
        addButton.setX(x + width - addButton.getWidth());
        for (Row row : rows) {
            row.field.setX(x);
            row.removeBtn.setX(x + width - row.removeBtn.getWidth());
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        inputField.setY(y);
        addButton.setY(y);
        int currentY = y + 24;
        for (Row row : rows) {
            row.field.setY(currentY);
            row.removeBtn.setY(currentY);
            currentY += rowHeight;
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        int buttonWidth = 20;
        int fieldWidth = width - buttonWidth - 4;
        inputField.setWidth(fieldWidth);
        addButton.setX(x + width - buttonWidth);
        for (Row row : rows) {
            row.field.setWidth(fieldWidth);
            row.removeBtn.setX(x + width - buttonWidth);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        inputField.render(graphics, mouseX, mouseY, partialTick);
        addButton.render(graphics, mouseX, mouseY, partialTick);
        for (Row row : rows) {
            row.field.render(graphics, mouseX, mouseY, partialTick);
            row.removeBtn.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY, float partialTick) {
        if (!visible) return;
        inputField.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
        for (Row row : rows) {
            row.field.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        inputField.updateHoverState(mouseX, mouseY);
        addButton.updateHoverState(mouseX, mouseY);
        for (Row row : rows) {
            row.field.updateHoverState(mouseX, mouseY);
            row.removeBtn.updateHoverState(mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        inputField.tick();
        addButton.tick();
        for (Row row : rows) {
            row.field.tick();
            row.removeBtn.tick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        
        if (inputField.hasActiveOverlay() && inputField.mouseClicked(mouseX, mouseY, button)) return true;
        
        if (inputField.mouseClicked(mouseX, mouseY, button)) return true;
        if (addButton.mouseClicked(mouseX, mouseY, button)) return true;
        
        for (Row row : rows) {
            if (row.removeBtn.mouseClicked(mouseX, mouseY, button)) return true;
            if (row.field.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (inputField.mouseReleased(mouseX, mouseY, button)) return true;
        if (addButton.mouseReleased(mouseX, mouseY, button)) return true;
        for (Row row : rows) {
            if (row.removeBtn.mouseReleased(mouseX, mouseY, button)) return true;
            if (row.field.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        if (inputField.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (Row row : rows) {
            if (row.field.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!visible || !enabled) return false;
        if (inputField.charTyped(character, modifiers)) return true;
        for (Row row : rows) {
            if (row.field.charTyped(character, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible || !enabled) return false;
        if (inputField.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        for (Row row : rows) {
            if (row.field.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return false;
    }

    @Override
    public boolean hasActiveOverlay() {
        if (inputField.hasActiveOverlay()) return true;
        for (Row row : rows) {
            if (row.field.hasActiveOverlay()) return true;
        }
        return false;
    }

    private static class Row {
        final TextField field;
        final Button removeBtn;

        Row(TextField field, Button removeBtn) {
            this.field = field;
            this.removeBtn = removeBtn;
        }
    }
}
