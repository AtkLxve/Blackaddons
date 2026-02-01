package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

public class FloorCardWidget extends Widget {
    private final String title;
    private final int runs;
    private final int bestScore;
    private final String sPlus;
    private final String s;

    public FloorCardWidget(int width, String title, int runs, int bestScore, String sPlus, String s) {
        super(0, 0, width, 50);
        this.title = title;
        this.runs = runs;
        this.bestScore = bestScore;
        this.sPlus = sPlus;
        this.s = s;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);

        graphics.drawString(Minecraft.getInstance().font, "§l" + title, x + 6, y + 6, Theme.ACCENT);

        String runsText = "§f" + runs + " Runs";
        int runsWidth = Minecraft.getInstance().font.width(runsText);
        graphics.drawString(Minecraft.getInstance().font, runsText, x + width - runsWidth - 6, y + 6, 0xFFFFFFFF);

        graphics.fill(x + 6, y + 18, x + width - 6, y + 19, Theme.BACKGROUND_TERTIARY);

        int statY = y + 24;
        int col1X = x + 6;
        int col2X = x + width / 2 + 4;

        graphics.drawString(Minecraft.getInstance().font, "§7Best Score: §f" + bestScore, col1X, statY, 0xFFFFFFFF);

        graphics.drawString(Minecraft.getInstance().font, "§7S+: §f" + sPlus, col1X, statY + 12, 0xFFFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, "§7S: §f" + s, col2X, statY + 12, 0xFFFFFFFF);
    }
}
