package org.blackum.blackaddons.gui.widget.row;

import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class FloorCardWidget extends Widget {
    private final String title;
    private final int runs;
    private final int bestScore;
    private final String sPlus;
    private final String s;
    private Animation hoverAnimation;

    public FloorCardWidget(int width, String title, int runs, int bestScore, String sPlus, String s) {
        super(0, 0, width, 50);
        this.title = title;
        this.runs = runs;
        this.bestScore = bestScore;
        this.sPlus = sPlus;
        this.s = s;
        this.hoverAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hover = hoverAnimation.getValue();
        int drawX = x + Math.round(hover * 2.0f);
        int drawWidth = width - Math.round(hover * 2.0f);

        RenderHelper.renderRoundedRect(graphics, drawX, y, drawWidth, height, Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);
        if (hover > 0) {
            RenderHelper.renderRoundedRect(graphics, drawX, y, drawWidth, height, Theme.BORDER_RADIUS,
                    Theme.withAlpha(0xFF000000, hover * 0.18f));
        }

        graphics.drawString(Minecraft.getInstance().font, ChatFormatting.BOLD + title, drawX + 6, y + 6, Theme.ACCENT);

        String runsText = ChatFormatting.WHITE + String.valueOf(runs) + " Runs";
        int runsWidth = Minecraft.getInstance().font.width(runsText);
        graphics.drawString(Minecraft.getInstance().font, runsText, drawX + drawWidth - runsWidth - 6, y + 6, 0xFFFFFFFF);

        graphics.fill(drawX + 6, y + 18, drawX + drawWidth - 6, y + 19, Theme.BACKGROUND_TERTIARY);

        int statY = y + 24;
        int col1X = drawX + 6;
        int col2X = drawX + drawWidth / 2 + 4;

        graphics.drawString(Minecraft.getInstance().font,
                ChatFormatting.GRAY + "Best Score: " + ChatFormatting.WHITE + bestScore, col1X, statY, 0xFFFFFFFF);

        graphics.drawString(Minecraft.getInstance().font, ChatFormatting.GRAY + "S+: " + ChatFormatting.WHITE + sPlus,
                col1X, statY + 12, 0xFFFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, ChatFormatting.GRAY + "S: " + ChatFormatting.WHITE + s, col2X,
                statY + 12, 0xFFFFFFFF);
    }

    @Override
    public void tick() {
        if (hovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if (!hovered && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
    }
}
