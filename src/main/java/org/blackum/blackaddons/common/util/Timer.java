package org.blackum.blackaddons.common.util;

import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.util.concurrent.ThreadLocalRandom;

public class Timer {
    private long finishTick = -1;

    public void startRandomTimer(long currentTick, float minMs, float maxMs) {
        float randomMs = ThreadLocalRandom.current().nextFloat(minMs, maxMs);
        this.finishTick = currentTick + Math.round(randomMs / 50.0);
    }

    public boolean isFinished(long currentTick) {
        if (finishTick == -1) return false;
        long ticksLeft = finishTick - currentTick;
        long msLeft = ticksLeft * 50;
        if (ticksLeft > 0) {
            ChatUtils.send_debug("Time remaining: " + msLeft + "ms (" + ticksLeft + " ticks)");
        }
        return currentTick >= finishTick;
    }

    public void reset() {
        this.finishTick = -1;
    }
}
