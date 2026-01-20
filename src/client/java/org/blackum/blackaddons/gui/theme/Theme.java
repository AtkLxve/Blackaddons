package org.blackum.blackaddons.gui.theme;

public class Theme {

    public static final int BACKGROUND = 0xFF121212;
    public static final int SURFACE = 0xFF2A2A2A;
    public static final int ACCENT = 0xFF50C8FF;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;

    public static final int GLASS_FILL = 0x801A1A1A;
    public static final int GLASS_BORDER = 0x40FFFFFF;
    public static final int GLASS_HIGHLIGHT = 0x60FFFFFF;

    public static final int BORDER_RADIUS = 12;
    public static final int BORDER_RADIUS_SMALL = 8;
    public static final int BORDER_RADIUS_LARGE = 16;

    public static final int ANIM_HOVER = 200;
    public static final int ANIM_CLICK = 100;
    public static final int ANIM_FOCUS = 150;
    public static final int ANIM_DIALOG = 300;
    public static final int ANIM_SCROLL = 250;

    public static final int BUTTON_HEIGHT = 36;
    public static final int TEXTFIELD_HEIGHT = 32;
    public static final int CHECKBOX_SIZE = 20;
    public static final int RADIO_SIZE = 18;
    public static final int SLIDER_HEIGHT = 6;
    public static final int SLIDER_THUMB_SIZE = 16;
    public static final int SCROLLBAR_WIDTH = 8;

    public static final int PADDING_SMALL = 8;
    public static final int PADDING_MEDIUM = 12;
    public static final int PADDING_LARGE = 16;
    public static final int MARGIN = 8;

    public static int withAlpha(int rgb, float alpha) {
        int a = (int) (alpha * 255) << 24;
        return a | (rgb & 0x00FFFFFF);
    }

    public static int lerpColor(int colorA, int colorB, float t) {
        int ar = (colorA >> 16) & 0xFF;
        int ag = (colorA >> 8) & 0xFF;
        int ab = colorA & 0xFF;

        int br = (colorB >> 16) & 0xFF;
        int bg = (colorB >> 8) & 0xFF;
        int bb = colorB & 0xFF;

        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int b = (int) (ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | b;
    }
}
