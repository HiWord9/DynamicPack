package com.adamcalculator.dynamicpack.util;

import java.nio.file.Path;

public class FilesLog {
    private static final String PREFIX = "[FilesLog] ";

    public static boolean isEnabled() {
        return false;
    }

    public static void deleted(Path path) {
        if (isEnabled()) {
            Out.debug(PREFIX + "-deleted: " + path);
        }
    }

    public static void created(Path path) {
        if (isEnabled()) {
            Out.debug(PREFIX + "+created: " + path);
        }
    }

    public static void writtenByUrl(Path path, String url) {
        if (isEnabled()) {
            Out.debug(PREFIX + "=written: (" + url + ")-> " + path);
        }
    }
}
