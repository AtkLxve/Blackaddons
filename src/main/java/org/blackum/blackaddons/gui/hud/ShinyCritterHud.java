package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.cheat.CritterESP;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AutoModule(order = 407)
public class ShinyCritterHud implements HudElement {
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int BG_COLOR = 0xAA000000;
    private static final int BORDER_COLOR_GOLD = 0xFFFFD700;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int LINE_HEIGHT = 11;
    private static final double DETECTION_RADIUS = 50.0;

    public static void register() {
        HudRegistry.register(new ShinyCritterHud());
    }

    @Override
    public String id() {
        return Constants.SHINY_CRITTER_HUD_ID;
    }

    @Override
    public String displayName() {
        return Constants.SHINY_CRITTER_HUD_NAME;
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.shinyCritterAlert && !McCompat.isGuiHidden(Minecraft.getInstance());
    }

    @Override
    public int x() {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return ConfigManager.data.shinyCritterAlertX < 0
                ? (screenW - width()) / 2
                : ConfigManager.data.shinyCritterAlertX;
    }

    @Override
    public int y() {
        return ConfigManager.data.shinyCritterAlertY < 0
                ? Constants.SHINY_CRITTER_HUD_DEFAULT_Y
                : ConfigManager.data.shinyCritterAlertY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.shinyCritterAlertX = x;
        ConfigManager.data.shinyCritterAlertY = y;
    }

    @Override
    public void reset() {
        ConfigManager.data.shinyCritterAlertX = -1;
        ConfigManager.data.shinyCritterAlertY = Constants.SHINY_CRITTER_HUD_DEFAULT_Y;
        ConfigManager.data.shinyCritterAlertScale = 1.0f;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float scale = Math.max(0.5f, Math.min(3.0f, (float) width / Constants.SHINY_CRITTER_HUD_BASE_WIDTH));
        ConfigManager.data.shinyCritterAlertScale = scale;
    }

    @Override
    public int width() {
        return Math.max(16, Math.round(Constants.SHINY_CRITTER_HUD_BASE_WIDTH * ConfigManager.data.shinyCritterAlertScale));
    }

    @Override
    public int height() {
        return Math.max(16, Math.round(Constants.SHINY_CRITTER_HUD_BASE_HEIGHT * ConfigManager.data.shinyCritterAlertScale));
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        boolean isEditing = McCompat.getScreen(mc) instanceof OverlayEditScreen;

        List<CritterEntry> entries = new ArrayList<>();
        boolean hasShiny = false;

        if (isEditing) {
            entries.add(new CritterEntry(Constants.SHINY_CRITTER_ALERT_PREVIEW, true));
            entries.add(new CritterEntry(Constants.CRITTER_ALERT_PREVIEW_NORMAL, false));
            hasShiny = true;
        } else {
            if (mc.player == null || mc.level == null) return;
            AABB searchBox = mc.player.getBoundingBox().inflate(DETECTION_RADIUS);
            for (Entity e : mc.level.getEntities(mc.player, searchBox, ent -> ent != null && !ent.isSpectator())) {
                String rawName = e.getName().getString();
                String clean = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim().toLowerCase(Locale.ROOT);
                if (CritterESP.isCritter(clean)) {
                    double dist = mc.player.distanceTo(e);
                    String cleanName = rawName.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
                    boolean shiny = CritterESP.isShiny(clean);
                    if (shiny) {
                        hasShiny = true;
                        String line = String.format(Locale.ROOT, "§6§l✨ %s §7(%.1fm)", cleanName, dist);
                        entries.add(0, new CritterEntry(line, true));
                    } else {
                        String line = String.format(Locale.ROOT, "§a%s §7(%.1fm)", cleanName, dist);
                        entries.add(new CritterEntry(line, false));
                    }
                }
            }
        }

        if (entries.isEmpty()) return;

        Font font = mc.font;
        String title = hasShiny ? Constants.SHINY_CRITTER_ALERT_TITLE : Constants.CRITTER_ALERT_TITLE;
        int titleColor = hasShiny ? COLOR_GOLD : Constants.CRITTER_ALERT_TITLE_COLOR_NORMAL;
        int borderColor = hasShiny ? BORDER_COLOR_GOLD : Constants.CRITTER_ALERT_BORDER_NORMAL;

        int titleWidth = font.width(title);
        int maxContentWidth = titleWidth;
        for (CritterEntry entry : entries) {
            int w = font.width(entry.text());
            if (w > maxContentWidth) {
                maxContentWidth = w;
            }
        }

        int boxWidth = maxContentWidth + PADDING_X * 2;
        int boxHeight = PADDING_Y * 2 + LINE_HEIGHT * (1 + entries.size());

        float scale = ConfigManager.data.shinyCritterAlertScale;
        int overlayX = x();
        int overlayY = y();

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) overlayX, (float) overlayY);
        graphics.pose().scale(scale, scale);

        RenderHelper.renderRoundedRect(graphics, 0, 0, boxWidth, boxHeight, 4, BG_COLOR);
        RenderHelper.renderRoundedOutline(graphics, 0, 0, boxWidth, boxHeight, 4, borderColor);

        int textX = boxWidth / 2;
        int currentY = PADDING_Y;

        RenderHelper.centeredText(graphics, font, title, textX, currentY, titleColor);
        currentY += LINE_HEIGHT;

        for (CritterEntry entry : entries) {
            RenderHelper.centeredText(graphics, font, entry.text(), textX, currentY, COLOR_WHITE);
            currentY += LINE_HEIGHT;
        }

        graphics.pose().popMatrix();
    }

    private record CritterEntry(String text, boolean shiny) {}
}
