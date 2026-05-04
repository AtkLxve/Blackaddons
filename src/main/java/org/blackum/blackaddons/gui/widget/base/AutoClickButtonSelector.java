package org.blackum.blackaddons.gui.widget.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class AutoClickButtonSelector extends Widget {
    public static final String LEFT = "left";
    public static final String RIGHT = "right";

    private static final int CHIP_HEIGHT = 34;
    private static final int GAP = 6;
    private static final String[] ORDER = {LEFT, RIGHT};

    private final Consumer<List<String>> onChange;
    private final Set<String> selected = new LinkedHashSet<>();
    private final List<Chip> chips = new ArrayList<>();

    public AutoClickButtonSelector(int x, int y, int width, List<String> initialSelection, Consumer<List<String>> onChange) {
        super(x, y, width, CHIP_HEIGHT);
        this.onChange = onChange;
        if (initialSelection != null) {
            selected.addAll(initialSelection);
        }
        rebuildChips();
    }

    public void setSelection(List<String> values) {
        selected.clear();
        if (values != null) {
            selected.addAll(values);
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        rebuildChips();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        rebuildChips();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        rebuildChips();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        for (Chip chip : chips) {
            boolean active = selected.contains(chip.id);
            float hoverProgress = chip.hoverAnimation.getValue();

            int fill = active
                    ? Theme.withAlpha(Theme.ACCENT, 0.28f + hoverProgress * 0.10f)
                    : Theme.withAlpha(Theme.SURFACE_LIGHT, 0.72f + hoverProgress * 0.18f);
            int border = active ? Theme.withAlpha(Theme.ACCENT, 0.95f) : Theme.GLASS_BORDER;
            int glow = active ? Theme.withAlpha(Theme.ACCENT, 0.14f + hoverProgress * 0.08f) : 0;

            if (glow != 0) {
                RenderHelper.renderRoundedRect(graphics, chip.x - 1, chip.y - 1, chip.width + 2, chip.height + 2,
                        Theme.BORDER_RADIUS_SMALL, glow);
            }

            RenderHelper.renderRoundedRect(graphics, chip.x, chip.y, chip.width, chip.height, Theme.BORDER_RADIUS_SMALL, fill);
            RenderHelper.renderRoundedOutline(graphics, chip.x, chip.y, chip.width, chip.height, Theme.BORDER_RADIUS_SMALL, border);

            int titleColor = active ? Theme.TEXT_PRIMARY : Theme.withAlpha(Theme.TEXT_PRIMARY, 0.92f);
            int detailColor = active ? Theme.withAlpha(Theme.TEXT_PRIMARY, 0.8f) : Theme.withAlpha(Theme.TEXT_SECONDARY, 0.75f);
            int centerX = chip.x + chip.width / 2;
            int titleWidth = Minecraft.getInstance().font.width(chip.label);
            int detailWidth = Minecraft.getInstance().font.width(chip.detail);
            graphics.drawString(Minecraft.getInstance().font, chip.label, centerX - titleWidth / 2, chip.y + 8, titleColor);
            graphics.drawString(Minecraft.getInstance().font, chip.detail, centerX - detailWidth / 2, chip.y + 19, detailColor);
        }
    }

    @Override
    public void tick() {
        for (Chip chip : chips) {
            chip.tick();
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        for (Chip chip : chips) {
            chip.updateHover(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible || button != 0) return false;

        for (Chip chip : chips) {
            if (chip.contains(mouseX, mouseY)) {
                if (!selected.add(chip.id)) {
                    selected.remove(chip.id);
                }
                onChange.accept(new ArrayList<>(selected));
                return true;
            }
        }
        return false;
    }

    private void rebuildChips() {
        chips.clear();
        int halfWidth = (width - GAP) / 2;
        chips.add(new Chip(LEFT, "Left Click", "ATTACK", x, y, halfWidth, CHIP_HEIGHT));
        chips.add(new Chip(RIGHT, "Right Click", "USE", x + halfWidth + GAP, y, width - halfWidth - GAP, CHIP_HEIGHT));
    }

    public static List<String> sanitizeSelection(List<String> values) {
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    for (String id : ORDER) {
                        if (id.equalsIgnoreCase(value)) {
                            sanitized.add(id);
                            break;
                        }
                    }
                }
            }
        }
        return new ArrayList<>(sanitized);
    }

    private static class Chip {
        private final String id;
        private final String label;
        private final String detail;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private Animation hoverAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);
        private boolean hovered;

        private Chip(String id, String label, String detail, int x, int y, int width, int height) {
            this.id = id;
            this.label = label;
            this.detail = detail;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private void updateHover(int mouseX, int mouseY) {
            hovered = contains(mouseX, mouseY);
        }

        private void tick() {
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
}
