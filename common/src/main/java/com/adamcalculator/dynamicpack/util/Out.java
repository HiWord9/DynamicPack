package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;

/**
 * Mod logger
 */
public class Out {
    public static final Logger LOGGER = LogManager.getLogger("dynamicpack");
    private static final String DEFAULT_PREFIX = "[DynamicPack] ";

    public static boolean ENABLE = true;
    public static boolean USE_SOUT = false;
    private static String PREFIX = "";

    public static void println(Object o) {
        if (!ENABLE) return;
        if (USE_SOUT) {
            System.out.println(PREFIX + o);
            return;
        }
        LOGGER.info("{}{}", PREFIX, o);
    }

    public static void error(String s, Throwable e) {
        if (!ENABLE) return;
        boolean stacktrace = isPrintErrorStackTrace(e);
        if (USE_SOUT) {
            System.err.println(PREFIX + s);
            if (stacktrace) {
                e.printStackTrace();
            }
            return;
        }
        if (stacktrace) {
            LOGGER.error("{}{}", PREFIX, s, e);
        } else {
            LOGGER.error("{}{}: {}", PREFIX, s, e.toString());
        }
    }

    private static boolean isPrintErrorStackTrace(Throwable e) {
        return !(e instanceof FileNotFoundException || e instanceof UnknownHostException);
    }

    public static void warn(String s) {
        if (!ENABLE) return;
        if (USE_SOUT) {
            System.out.println(PREFIX + "WARN: " + s);
            return;
        }
        LOGGER.warn("{}{}", PREFIX, s);
    }

    /**
     * Always enable! Ignore enable/disable
     */
    public static void securityWarning(String s) {
        if (USE_SOUT) {
            System.out.println("[DynamicPack] " + s);
            return;
        }

        try {
            LOGGER.warn("[DynamicPack] {}", s);
        } catch (Exception ignored) {
            System.out.println("[DynamicPack] " + s);
        }
    }

    public static void debug(String s) {
        if (SharedConstrains.isDebugLogs()) {
            println("DEBUG: " + s);
        }
    }

    /**
     * Always enable! Ignore enable/disable
     */
    public static void securityStackTrace() {
        if (USE_SOUT) {
            System.out.println("[DynamicPack] Stacktrace");
            new Throwable("StackTrace printer").printStackTrace();
            return;
        }
        LOGGER.error("[DynamicPack] No error. This is stacktrace printer", new Throwable("StackTrace printer"));
    }

    public static void init(Loader loader) {
        // add DynamicPack prefix in Fabric releases
        if (loader == Loader.FABRIC && SharedConstrains.isRelease()) {
            PREFIX = DEFAULT_PREFIX;
        }
    }
}
