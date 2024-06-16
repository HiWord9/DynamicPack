package com.adamcalculator.dynamicpack.util;

import java.nio.file.Path;

/**
 * Notify this class about all changes in files by mod.
 */
public class FilesLog {
    private static final String PREFIX = "[FilesLog] ";
    public static boolean LOG_ALL_CHANGES = false;

    public static void deleted(Path path) {
        if (LOG_ALL_CHANGES) {
            Out.debug(PREFIX + "-deleted: " + path);
        }
    }

    public static void created(Path path) {
        if (LOG_ALL_CHANGES) {
            Out.debug(PREFIX + "+created: " + path);
        }
    }

    public static void writtenByUrl(Path path, String url) {
        if (LOG_ALL_CHANGES) {
            Out.debug(PREFIX + "=written: (" + url + ")-> " + path);
        }
    }
}
