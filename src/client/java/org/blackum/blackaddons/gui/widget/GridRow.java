package org.blackum.blackaddons.gui.widget;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GridRow extends Widget {
    private final List<Map.Entry<Widget, Integer>> children = new ArrayList<>();

    public GridRow(int w, int h) {
        super(0, 0, w, h);
    }

    public void addChild(Widget w, int xOffset) {
        children.add(new AbstractMap.SimpleEntry<>(w, xOffset));
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Map.Entry<Widget, Integer> entry : children) {
            Widget w = entry.getKey();
            int xOff = entry.getValue();
            int originalX = w.getX();
            int originalY = w.getY();

            w.setX(this.x + xOff);
            w.setY(this.y);
            w.render(graphics, mouseX, mouseY, partialTick);

            w.setX(originalX);
            w.setY(originalY);
        }
    }

    @Override
    public void tick() {
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().tick();
        }
    }
}
