package com.adamcalculator.dynamicpack.sync;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.util.Out;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Sync task.
 * Re-check all packs and update packs with update available
 */
public class SyncingTask {
    private static boolean isSyncing = false;
    @NotNull private static String syncingLog1 = "";
    @NotNull private static String syncingLog2 = "";
    @NotNull private static String syncingLog3 = "";
    public static long eta;
    @Nullable public static String currentPackName;


    /**
     * @param runnable to run
     * @throws RuntimeException if task already running (check isSyncing before call)
     */
    public static void launchTaskAsSyncing(Runnable runnable) {
        if (isSyncing()) {
            throw new RuntimeException("Failed to launchTaskAsSyncing. Other task currently working...");
        }
        var mod = DynamicPackMod.INSTANCE;
        setSyncing(true);
        mod.rescanPacks();
        mod.blockRescan(true);
        log("[SyncingTask] launchTaskAsSyncing start!");
        runnable.run();
        log("[SyncingTask] launchTaskAsSyncing end!");
        setSyncing(false);
        mod.blockRescan(false);
        SyncingTask.currentPackName = null;
    }

    public static void log(String s) {
        Out.debug("[SyncingTask] log: " + s);
        syncingLog1 = syncingLog2;
        syncingLog2 = syncingLog3;
        syncingLog3 = s;
    }

    public static void clearLog() {
        syncingLog3 = "";
        syncingLog2 = "";
        syncingLog1 = "";
    }

    public static String getLogs() {
        return syncingLog1 + "\n" + syncingLog2 + "\n" + syncingLog3;
    }

    public static void setSyncing(boolean isSyncing) {
        SyncingTask.isSyncing = isSyncing;
    }

    public static boolean isSyncing() {
        return isSyncing;
    }

    public SyncingTask() {
    }


    public SyncBuilder rootSyncBuilder() throws Exception {
        Set<SyncBuilder> set = new HashSet<>();
        long totalSize = 0;
        boolean updateAvailable = false;
        for (DynamicResourcePack pack : DynamicPackMod.getPacks()) {
            var builder = pack.syncBuilder();
            builder.init();
            set.add(builder);

            totalSize += builder.getUpdateSize();
            if (builder.isUpdateAvailable()) {
                updateAvailable = true;
            }
        }

        Out.debug("[SyncingTask] rootSyncBuilder() totalSize=" + totalSize + " updateAvailable=" + updateAvailable);


        final boolean finalUpdateAvailable = updateAvailable;
        final long finalTotalSize = totalSize;
        return new SyncBuilder() {
            @Override
            public void init() throws Exception {
                // do nothing here!!!
            }

            @Override
            public boolean isUpdateAvailable() {
                return finalUpdateAvailable;
            }

            @Override
            public long getUpdateSize() {
                return finalTotalSize;
            }

            @Override
            public long getDownloadedSize() {
                long updated = 0;
                for (SyncBuilder syncBuilder : set) {
                    updated += syncBuilder.getDownloadedSize();
                }
                return updated;
            }

            @Override
            public boolean doUpdate(SyncProgress progress) throws Exception {
                boolean reload = false;
                for (SyncBuilder syncBuilder : set) {
                    if (syncBuilder.isUpdateAvailable()) {
                        boolean rel = syncBuilder.doUpdate(progress);
                        if (rel) {
                            reload = true;
                        }
                    }
                }
                return reload;
            }
        };
    }
}
