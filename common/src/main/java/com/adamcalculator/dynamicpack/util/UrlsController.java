package com.adamcalculator.dynamicpack.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongConsumer;

public class UrlsController implements LongConsumer {
    public static final float MAGIC_DEV_BY_ZERO = -1000f;
    public static final float MAGIC_OVERMAXING = 101f;


    private boolean interrupted;
    private long max = 0;
    private long latest = 0;

    public static boolean isInterrupted(@Nullable UrlsController controller) {
        return controller != null && controller.isInterrupted();
    }

    public static void updateCurrent(@Nullable UrlsController controller, long current) {
        if (controller != null) {
            controller.updateCurrent(current);
        }
    }

    public void updateCurrent(long value) {
        this.latest = value;
        onUpdate(this);
    }

    public void updateMax(long value) {
        max = Math.max(value, max);
    }

    /**
     * @deprecated Override a LongConsumer for back compatibility
     * @param value the input argument
     */
    @Deprecated
    @Override
    public void accept(long value) {
        updateCurrent(value);
        updateMax(value);
    }

    public float getPercentage() {
        if (max < latest) {
            return MAGIC_OVERMAXING;
        }

        if (max == 0) {
            return MAGIC_DEV_BY_ZERO;
        }

        if (latest == max) {
            return 100f;
        }
        return (float) latest * 100f / (float) max;
    }

    /**
     * In seconds
     */
    public long getRemaining() {
        return NetworkStat.remainingETA(max - latest);
    }

    public long getLatest() {
        return latest;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void interrupt() {
        interrupted = false;
    }

    @ApiStatus.OverrideOnly
    public void onUpdate(UrlsController it) {
        // override it
    }
}
