package com.adamcalculator.dynamicpack.sync;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.util.LoopLog;
import com.adamcalculator.dynamicpack.util.NetworkStat;
import com.adamcalculator.dynamicpack.util.Out;

import java.nio.file.Path;

public class SyncThread extends Thread {
    private SyncBuilder syncBuilder;
    private final LoopLog etaLoopLog = new LoopLog(1000);

    public SyncThread(String name) {
        setName(name);
    }

    @Override
    public void run() {
        Out.debug("[SyncThread] thread started");

        if (SyncingTask.isSyncing()) {
            Out.warn("Shutting down SyncThread because other thread already work");
            return;
        }

        SyncingTask.launchTaskAsSyncing(() -> {
            try {
                syncBuilder = SyncingTask.rootSyncBuilder();
                syncBuilder.init(true);

                if (syncBuilder.isUpdateAvailable()) {
                    boolean reloadRequired = syncBuilder.doUpdate(createSyncProgress());
                    if (reloadRequired) {
                        DynamicPackMod.INSTANCE.needResourcesReload();
                    }
                }

            } catch (Exception e) {
                Out.error("Error while SyncThread...", e);
            }
        });
    }

    private SyncProgress createSyncProgress() {
        return new SyncProgress() {
            @Override
            public void setPhase(String phase) {
                Out.debug("Phase: " + phase);
                SyncingTask.log(phase);
            }

            @Override
            public void downloading(String name, float percentage) {
                long remainsBytes = syncBuilder.getUpdateSize() - syncBuilder.getDownloadedSize();
                long eta = SyncingTask.eta = NetworkStat.remainingETA(remainsBytes);
                if (etaLoopLog.tick()) {
                    Out.debug("(" + SyncingTask.currentPackName + ") ETA=" + eta + "s");
                }
            }

            @Override
            public void deleted(Path name) {
                Out.debug("Deleted: " + name);
                SyncingTask.log(name.getFileName().toString());
            }
        };
    }
}
