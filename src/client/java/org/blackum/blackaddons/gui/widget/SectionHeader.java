package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.theme.Theme;

public class SectionHeader extends Widget {
    private final String title;

    public SectionHeader(int width, String title) {
        super(0, 0, width, 25);
        this.title = "§l" + title;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;
        graphics.drawString(Minecraft.getInstance().font, title, x, y + 8, Theme.ACCENT);

        int titleWidth = Minecraft.getInstance().font.width(title);
        int lineX = x + titleWidth + 10;
        int lineW = width - titleWidth - 10;
        if (lineW > 0) {
            int centerY = y + 8 + 4;
            graphics.fill(lineX, centerY, x + width, centerY + 1, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.3f));
        }
    }
}
