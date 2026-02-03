package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

public class LeaderboardRow extends Widget {
    private final int rank;
    private final String ign;
    private final double value;
    private final boolean isRuns;
    private boolean isCurrentPlayer = false;

    public LeaderboardRow(int w, int rank, String ign, double value, boolean isRuns) {
        super(0, 0, w, 20);
        this.rank = rank;
        this.ign = ign;
        this.value = value;
        this.isRuns = isRuns;
    }

    public void setCurrentPlayer(boolean current) {
        this.isCurrentPlayer = current;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            Minecraft.getInstance().setScreen(new ProfileViewerScreen(null, ign));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int color = hovered ? Theme.withAlpha(Theme.GLASS_HIGHLIGHT, 0.4f) : 0;
        if (isCurrentPlayer) {
            color = Theme.withAlpha(Theme.ACCENT, 0.3f);
        }
        if (color != 0)
            RenderHelper.renderRoundedRect(graphics, x, y, width, height, 3, color);

        Minecraft mc = Minecraft.getInstance();
        String rankStr = "#" + rank;
        if (rank == 1)
            rankStr = "§6🥇";
        else if (rank == 2)
            rankStr = "§7🥈";
        else if (rank == 3)
            rankStr = "§c🥉";

        graphics.drawString(mc.font, rankStr, x + 5, y + 6, 0xFFFFFFFF);
        graphics.drawString(mc.font, ign, x + 30, y + 6, isCurrentPlayer ? 0xFFFFFFFF : Theme.ACCENT);

        String valStr = isRuns ? String.format("%,.0f Runs", value) : String.format("%,.0f XP", value);
        int valW = mc.font.width(valStr);
        graphics.drawString(mc.font, "§f" + valStr, x + width - valW - 5, y + 6, 0xFFFFFFFF);
    }
}
