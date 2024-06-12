package com.adamcalculator.dynamicpack.util;

/**
 * Millis timer
 */
public class LoopLog {
    private long latest;
    private final int interval;

    public LoopLog(int interval) {
        this.interval = interval;
    }

    public boolean tick() {
        long current = System.currentTimeMillis();
        if ((current - latest) > interval) {
            latest = current;
            return true;
        }
        return false;
    }
}
