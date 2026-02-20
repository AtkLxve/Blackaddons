package org.blackum.blackaddons.gui.animation;

public class Easing {

    public static float linear(float t) {
        return t;
    }

    public static float easeIn(float t) {
        return t * t;
    }

    public static float easeOut(float t) {
        return t * (2 - t);
    }

    public static float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    public static float easeInCubic(float t) {
        return t * t * t;
    }

    public static float easeOutCubic(float t) {
        float f = t - 1;
        return f * f * f + 1;
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
    }

    public static float easeOutElastic(float t) {
        if (t == 0 || t == 1)
            return t;
        float p = 0.3f;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1);
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    public static float easeOutBounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            return n1 * (t -= 1.5f / d1) * t + 0.75f;
        } else if (t < 2.5 / d1) {
            return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        } else {
            return n1 * (t -= 2.625f / d1) * t + 0.984375f;
        }
    }
}
