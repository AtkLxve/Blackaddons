package org.blackum.blackaddons.gui.animation;

import java.util.function.Function;

public class Animation {
    private final float startValue;
    private final float endValue;
    private final long duration;
    private final Function<Float, Float> easingFunction;
    private long startTime;
    private boolean running;
    private boolean finished;

    public Animation(float startValue, float endValue, long duration, Function<Float, Float> easingFunction) {
        this.startValue = startValue;
        this.endValue = endValue;
        this.duration = duration;
        this.easingFunction = easingFunction;
        this.running = false;
        this.finished = false;
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
        this.finished = false;
    }

    public float getValue() {
        if (finished)
            return endValue;
        if (!running)
            return startValue;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) {
            finished = true;
            running = false;
            return endValue;
        }

        float t = (float) elapsed / duration;
        float easedT = easingFunction.apply(t);
        return startValue + (endValue - startValue) * easedT;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public void reset() {
        running = false;
        finished = false;
    }

    public float getProgress() {
        if (finished)
            return 1;
        if (!running)
            return 0;

        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, (float) elapsed / duration);
    }
}
