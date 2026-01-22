package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TabPanel extends Widget {

    public static class Tab {
        public final String name;
        public final List<Widget> widgets = new ArrayList<>();

        public Tab(String name) {
            this.name = name;
        }

        public Tab addWidget(Widget widget) {
            widgets.add(widget);
            return this;
        }
    }

    private List<Tab> tabs = new ArrayList<>();
    private int selectedTabIndex = 0;
    private Consumer<Integer> onTabChange;

    private int tabWidth = 120;
    private int tabHeight = 40;
    private int contentPadding = 10;

    private Map<Integer, Animation> tabHoverAnimations = new HashMap<>();
    private Animation selectionAnimation;

    public TabPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.selectionAnimation = new Animation(0, 0, Theme.ANIM_NORMAL, Easing::easeOut);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY))
            return true;

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.isVisible() && widget.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.isVisible()) {
                    widget.updateHoverState(mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        renderTabs(graphics, mouseX, mouseY);
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabX = x;
        int tabY = y;

        float selectionY = tabY + selectionAnimation.getValue() * tabHeight;
        int selectionColor = Theme.withAlpha(Theme.ACCENT, 0.2f);
        RenderHelper.renderRoundedRect(graphics, tabX, (int) selectionY, tabWidth, tabHeight,
                Theme.BORDER_RADIUS_SMALL, selectionColor);

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isSelected = i == selectedTabIndex;
            boolean isHovered = mouseX >= tabX && mouseX <= tabX + tabWidth &&
                    mouseY >= tabY && mouseY <= tabY + tabHeight;

            Animation hoverAnim = tabHoverAnimations.computeIfAbsent(i,
                    k -> new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut));

            if (isHovered && !isSelected) {
                int hoverColor = Theme.withAlpha(Theme.SURFACE_LIGHT, 0.5f);
                graphics.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, hoverColor);
            }

            int textColor = isSelected ? Theme.ACCENT : Theme.TEXT_SECONDARY;
            if (!isSelected && hoverAnim.getValue() > 0) {
                textColor = Theme.TEXT_PRIMARY;
            }

            int textX = tabX + (tabWidth - Minecraft.getInstance().font.width(tab.name)) / 2;
            int textY = tabY + (tabHeight - 8) / 2;
            graphics.drawString(Minecraft.getInstance().font, tab.name, textX, textY, textColor);

            if (isSelected) {
                int lineX = tabX + tabWidth - 3;
                graphics.fill(lineX, tabY + 5, lineX + 3, tabY + tabHeight - 5, Theme.ACCENT);
            }

            tabY += tabHeight;
        }
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (selectedTabIndex < 0 || selectedTabIndex >= tabs.size())
            return;

        Tab currentTab = tabs.get(selectedTabIndex);
        List<Widget> expandedDropdowns = new ArrayList<>();
        for (Widget widget : currentTab.widgets) {
            if (widget.isVisible()) {
                if (widget instanceof Dropdown && ((Dropdown) widget).isExpanded()) {
                    expandedDropdowns.add(widget);
                } else {
                    widget.render(graphics, mouseX, mouseY, partialTick);
                }
            }
        }

        for (Widget widget : expandedDropdowns) {
            if (widget.isVisible()) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public int getMaxContentHeight() {
        if (selectedTabIndex < 0 || selectedTabIndex >= tabs.size())
            return height;

        Tab currentTab = tabs.get(selectedTabIndex);
        int maxY = 0;
        int contentY = y;

        for (Widget widget : currentTab.widgets) {
            if (widget.isVisible()) {
                int relativeY = widget.getY() - contentY;
                int widgetBottom = relativeY + widget.getHeight();
                if (widgetBottom > maxY) {
                    maxY = widgetBottom;
                }
            }
        }
        return Math.max(height, maxY + 40);
    }

    @Override
    public void tick() {
        for (int i = 0; i < tabs.size(); i++) {
            Animation hoverAnim = tabHoverAnimations.get(i);
            if (hoverAnim != null && hoverAnim.isRunning()) {
            }
        }

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                widget.tick();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        int tabX = x;
        int tabY = y;

        for (int i = 0; i < tabs.size(); i++) {
            if (mouseX >= tabX && mouseX <= tabX + tabWidth &&
                    mouseY >= tabY && mouseY <= tabY + tabHeight) {
                selectTab(i);
                return true;
            }
            tabY += tabHeight;
        }

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + height) {
            int direction = scrollY > 0 ? -1 : 1;
            int newIndex = selectedTabIndex + direction;
            if (newIndex >= 0 && newIndex < tabs.size()) {
                selectTab(newIndex);
            }
            return true;
        }

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.charTyped(character, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(character, modifiers);
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size() && index != selectedTabIndex) {
            selectedTabIndex = index;

            selectionAnimation = new Animation(selectionAnimation.getValue(), (float) index, Theme.ANIM_NORMAL,
                    Easing::easeOut);
            selectionAnimation.start();

            if (onTabChange != null) {
                onTabChange.accept(index);
            }
        }
    }

    public Tab addTab(String name) {
        Tab tab = new Tab(name);
        tabs.add(tab);
        return tab;
    }

    public Tab getTab(int index) {
        return tabs.get(index);
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setOnTabChange(Consumer<Integer> onTabChange) {
        this.onTabChange = onTabChange;
    }

    public int getContentX() {
        return x + tabWidth + contentPadding;
    }

    public int getContentY() {
        return y;
    }

    public int getContentWidth() {
        return width - tabWidth - contentPadding;
    }

    public int getContentHeight() {
        return height;
    }
}
