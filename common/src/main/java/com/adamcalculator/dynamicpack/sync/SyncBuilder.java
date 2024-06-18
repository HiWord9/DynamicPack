package com.adamcalculator.dynamicpack.sync;


/**
 * SyncBuilder
 */
public interface SyncBuilder {
    /**
     * Initialize a SyncBuilder, cache updateAvailable status, update size, etc...
     */
    void init(boolean ignoreCaches) throws Exception;

    /**
     * @return downloaded size
     */
    long getDownloadedSize();

    /**
     * @return cached in init() value
     */
    boolean isUpdateAvailable();

    /**
     * @return calculated and cached in init() value
     */
    long getUpdateSize();

    /**
     * Update pack
     * @return is needed a reload
     */
    boolean doUpdate(SyncProgress progress) throws Exception;

    /**
     * Stop update
     */
    void interrupt();
}
