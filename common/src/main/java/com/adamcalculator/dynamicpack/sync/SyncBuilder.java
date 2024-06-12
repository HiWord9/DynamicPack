package com.adamcalculator.dynamicpack.sync;


/**
 * SyncBuilder
 */
public abstract class SyncBuilder {
    /**
     * Initialize a SyncBuilder, cache updateAvailable status, update size, etc...
     */
    public abstract void init() throws Exception;

    /**
     * @return downloaded size
     */
    public abstract long getDownloadedSize();

    /**
     * @return cached in init() value
     */
    public abstract boolean isUpdateAvailable();

    /**
     * @return calculated & cached in init() value
     */
    public abstract long getUpdateSize();

    /**
     * Update pack
     * @return is needed a reload
     */
    public abstract boolean doUpdate(SyncProgress progress) throws Exception;
}
