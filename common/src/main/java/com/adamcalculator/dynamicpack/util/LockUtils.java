package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.client.FilePackResourcesAccessor;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Utils for file locked by other process (it a fix crashes in Windows)
 */
public class LockUtils {
    private static final Set<FilePackResourcesAccessor> OPENED_ZIP_FILES = new HashSet<>();


    /**
     * Create file finalizer Runnable
     */
    public static Runnable createFileFinalizer(File file) {
        return () -> closeFile(file);
    }

    public static void closeFile(File file) {
        Out.debug("[PackUtil] closeFile " + file);

        Set<FilePackResourcesAccessor> toDelete = new HashSet<>();
        for (FilePackResourcesAccessor openedZipFile : OPENED_ZIP_FILES.toArray(new FilePackResourcesAccessor[0])) {
            if (openedZipFile.dynamicpack$getFile().equals(file)) {
                openedZipFile.dynamicpack$close();
                toDelete.add(openedZipFile);
                Out.debug("[PackUtil] - " + openedZipFile);
            }
        }

        OPENED_ZIP_FILES.removeAll(toDelete);
    }

    public static void addFileToOpened(FilePackResourcesAccessor resources) {
        OPENED_ZIP_FILES.add(resources);
    }
}
