package com.adamcalculator.dynamicpack.client;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.status.StatusChecker;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.sync.SyncProgress;
import com.adamcalculator.dynamicpack.sync.SyncingTask;
import com.adamcalculator.dynamicpack.util.LoopLog;
import com.adamcalculator.dynamicpack.util.NetworkStat;
import com.adamcalculator.dynamicpack.util.Out;

import java.nio.file.Path;

/**
 * Syncing on game launch
 */
public class GameStartSyncing extends Thread {
    private static final long MAX_LOCK_MS = 1000 * 15;

    private long updateStartTime; // thread started time ms
    private long lockStartTime; // lock started time ms
    public boolean lockResourcesLoading = true; // locking resources loading (firstly true!
    public SyncBuilder syncBuilder;
    public final LoopLog etaLoopLog = new LoopLog(1000);

    public GameStartSyncing() {
        setName("GameStartSyncingThread");
    }

    /**
     * Staring syncing... (thread entrypoint)
     */
    @Override
    public void run() {
        updateStartTime = System.currentTimeMillis();
        Out.debug("[GameStartSyncing] thread started");

        SyncingTask syncingTask = new SyncingTask();
        SyncingTask.launchTaskAsSyncing(() -> {
            try {
                StatusChecker.check(); // <-- don't forget check a status
                syncBuilder = syncingTask.rootSyncBuilder();
                if (syncBuilder.isUpdateAvailable()) {
                    boolean reloadRequired = syncBuilder.doUpdate(createSyncProgress());
                    if (!lockResourcesLoading && reloadRequired) {
                        DynamicPackMod.INSTANCE.needResourcesReload();
                    }
                }

            } catch (Exception e) {
                Out.error("Error while GameStartSyncing...", e);
            }
            unlock(); // <--- Unlock main thread!
        });
    }

    private SyncProgress createSyncProgress() {
        return new SyncProgress() {
            @Override
            public void setPhase(String phase) {
                Out.debug("Phase: " + phase);
            }

            @Override
            public void downloading(String name, float percentage) {
                long remainsBytes = syncBuilder.getUpdateSize() - syncBuilder.getDownloadedSize();
                long eta = SyncingTask.eta = NetworkStat.remainingETA(remainsBytes);
                if (etaLoopLog.tick()) {
                    Out.debug("(" + SyncingTask.currentPackName + ") ETA=" + eta + "s");
                }

                // if locked and updating > 5 seconds
                if (updateTime() > 1000 * 5 && isLocked()) {
                    boolean unlock = eta > (untilForceUnlock() / 1.5f);

                    if (unlock) {
                        Out.debug("[GameStartSyncing] ETA " + eta + "s. Unlocking main thread...");
                        unlock();
                    }
                }
            }

            @Override
            public void deleted(Path name) {
                Out.debug("Deleted: " + name);
            }
        };
    }

    // if conflict mods loaded return false
    public boolean isLockSupported() {
        return true;
    }

    public boolean lockedTick() {
        if (lockTime() > MAX_LOCK_MS) {
            Out.warn("Main thread unlocked forcibly because lock time >= 10s");
            return false;
        }
        return true;
    }

    private long lockTime() {
        return System.currentTimeMillis() - lockStartTime;
    }

    private long updateTime() {
        return System.currentTimeMillis() - updateStartTime;
    }

    private long untilForceUnlock() {
        return MAX_LOCK_MS - lockTime();
    }

    public void endGameLocking() {
        Out.println("Main thread locked for " + (lockTime() / 1000) + " seconds");
    }

    public void startGameLocking() {
        Out.println("Main thread locked by DynamicPack for updating resource packs...");
        lockStartTime = System.currentTimeMillis();
    }

    public boolean isLockStarted() {
        return lockStartTime != 0;
    }

    public boolean isLocked() {
        return isLockStarted() && lockResourcesLoading;
    }

    private void unlock() {
        lockResourcesLoading = false;
    }
}
