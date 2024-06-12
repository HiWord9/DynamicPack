package com.adamcalculator.dynamicpack.util;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.LongConsumer;

public class FileDownloadConsumer implements LongConsumer {
    public static final float MAGIC_DEV_BY_ZERO = -1000f;
    public static final float MAGIC_OVERMAXING = 101f;


    private long max = 0;
    private long latest = 0;

    @Override
    public void accept(long value) {
        this.latest = value;
        if (value > max) {
            max = value;
        }
        onUpdate(this);
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

    @ApiStatus.OverrideOnly
    public void onUpdate(FileDownloadConsumer it) {
        // override it
    }
}
