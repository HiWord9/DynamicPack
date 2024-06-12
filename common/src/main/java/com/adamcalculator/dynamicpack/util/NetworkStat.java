package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;

import java.io.IOException;
import java.util.HashMap;

public class NetworkStat {
    public static final long MAGIC_NO_STATISTIC = -1;

    private static final LoopLog _debugCallLoopLog = new LoopLog(250);
    private static long millis;
    private static long bytes;

    // used for multi-thread correction
    public static long speedMultiplier = 1; // crunch (kostil)

    public static <R> R runNetworkTask(long bytes, ThrowingFunctionRet<IOException, R> runnable) throws IOException {
        long start = System.currentTimeMillis();
        IOException exception = null;
        R result = null;
        try {
            result = runnable.run();
        } catch (IOException e) {
            exception = e;
        }
        long elapsed = System.currentTimeMillis() - start;
        addLap(elapsed, bytes);
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    /**
     * @return speed in bytes/second
     */
    public static long getSpeed() {
        if (millis < 1000 || bytes == 0) {
            return MAGIC_NO_STATISTIC;
        }
        return (long) (bytes / (millis / 1000f)) * speedMultiplier;
    }


    /**
     * Add a lap to statistic
     */
    public static void addLap(long elapsed, long bytesRead) {
        if (elapsed < 0 || bytesRead < 0) {
            throw new IllegalArgumentException("elapsed and bytesRead can't be negative!");
        }

        if (bytes > SharedConstrains.NETWORK_STAT_RESET_LIMIT) {
            bytes /= 3;
            millis /= 3;
            Out.debug("[NetworkStat] reset by divide all by 3");
        }
        millis += elapsed;
        bytes += bytesRead;

        _debugCall();
    }

    /**
     * In seconds
     */
    public static long remainingETA(long bytes) {
        long speed = getSpeed();
        if (speed <= 0) {
            return MAGIC_NO_STATISTIC;
        }
        return bytes / speed;
    }

    private static void _debugCall() {
        if (SharedConstrains.DEBUG && _debugCallLoopLog.tick()) {
            Out.debug("[NetworkStat] speed: " + SharedConstrains.speedToString(getSpeed()) + "; totalTime=" + millis + "; totalBytes="+ bytes);
        }
    }
}
