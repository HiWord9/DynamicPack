package com.adamcalculator.dynamicpack.sync;

import java.nio.file.Path;

/**
 * Sync pack info interface
 */
public interface SyncProgress {
    void setPhase(String phase);
    void downloading(String name, float percentage);
    void deleted(Path name);
}
