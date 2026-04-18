package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public interface HudElement {
    String id();

    String displayName();

    boolean enabled();

    int x();

    int y();

    void setPos(int x, int y);

    int width();

    int height();

    void render(GuiGraphics graphics, DeltaTracker tracker);

    default void reset() {}

    default boolean resizable() {
        return false;
    }

    default void setSize(int width, int height) {}
}
