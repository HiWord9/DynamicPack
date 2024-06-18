package com.adamcalculator.dynamicpack;


import com.adamcalculator.dynamicpack.pack.dynamicrepo.DynamicRepoSyncBuilder;
import com.adamcalculator.dynamicpack.util.FilesLog;
import com.adamcalculator.dynamicpack.util.Out;

import com.adamcalculator.dynamicpack.util.PathsUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Config {
    public static final Config DEF = createDefConfig();

    private transient boolean def;

    private int formatVersion = 1; // do not touch
    private int networkBufferSize = 1024;
    private int networkMultithreadDownloadThreads = 8;
    private boolean logAllFilesChanges = false;
    private boolean autoUpdateAtLaunch = true;
    private boolean updateOnlyEnabledPacks = true;
    private boolean debugIgnoreHiddenFlagInContents = false;

    private static Config createDefConfig() {
        Config config = new Config();
        config.def = true;
        return config;
    }


    public static Config load() {
        //noinspection ConstantValue
        if (getInstance() != null) {
            throw new RuntimeException("Config already loaded");
        }

        File file = DynamicPackMod.getConfigFile();
        if (!file.exists()) {
            Config cfg = new Config();
            updateStaticVariables(cfg);
            return cfg;
        }


        try {
            Config config = SharedConstrains.GSON.fromJson(PathsUtil.readString(file.toPath()), Config.class);
            updateStaticVariables(config);

            return config;

        } catch (Exception e) {
            Out.error("Config load failed (return default config)", e);
        }

        Config config = new Config();
        updateStaticVariables(config);
        return config;
    }

    private static void updateStaticVariables(Config config) {
        config.checkAndValidateConfig();

        SharedConstrains.URLS_BUFFER_SIZE = config.networkBufferSize;
        DynamicRepoSyncBuilder.DOWNLOAD_THREADS_COUNT = config.networkMultithreadDownloadThreads;
        FilesLog.LOG_ALL_CHANGES = config.logAllFilesChanges;
    }

    public static Config getInstance() {
        return DynamicPackMod.getConfig();
    }

    private void checkAndValidateConfig() {
        boolean save = false;

        if (networkBufferSize < 256) {
            networkBufferSize = 256;
            save = true;
            Out.warn("Config invalid 'networkBufferSize'. Sets to " + networkBufferSize);
        }

        if (networkMultithreadDownloadThreads <= 0 || networkMultithreadDownloadThreads >= 256) {
            networkMultithreadDownloadThreads = 8;
            save = true;
            Out.warn("Config invalid 'networkMultithreadDownloadThreads'. Sets to " + networkMultithreadDownloadThreads);
        }

        if (save) {
            save();
        }
    }

    public void save() {
        if (def) {
            throw new RuntimeException("Can't be save a DEF config!");
        }

        try {
            String json = SharedConstrains.GSON.toJson(this);

            Path file = DynamicPackMod.getConfigFile().toPath();
            PathsUtil.delete(file);
            PathsUtil.createDirsToFile(file);
            PathsUtil.createFile(file);
            Files.writeString(file, json, StandardOpenOption.WRITE);

        } catch (Exception e) {
            Out.error("Config save failed :(", e);
        }
    }

    public void setNetworkBufferSize(int bufferSize) {
        this.networkBufferSize = bufferSize;
        updateStaticVariables(this);
    }

    public int getNetworkBufferSize() {
        return networkBufferSize;
    }

    public int getNetworkMultithreadDownloadThreads() {
        return networkMultithreadDownloadThreads;
    }

    public void setNetworkMultithreadDownloadThreads(int networkMultithreadDownloadThreads) {
        this.networkMultithreadDownloadThreads = networkMultithreadDownloadThreads;
        updateStaticVariables(this);
    }

    public boolean dynamicRepoIsIgnoreHiddenContentFlag() {
        return debugIgnoreHiddenFlagInContents;
    }

    public void setDebugIgnoreHiddenFlagInContents(boolean debugIgnoreHiddenFlagInContents) {
        this.debugIgnoreHiddenFlagInContents = debugIgnoreHiddenFlagInContents;
    }

    public void setLogAllFilesChanges(boolean logAllFilesChanges) {
        this.logAllFilesChanges = logAllFilesChanges;
        updateStaticVariables(this);
    }

    public boolean isLogAllFilesChanges() {
        return logAllFilesChanges;
    }

    public boolean isAutoUpdateAtLaunch() {
        return autoUpdateAtLaunch;
    }

    public void setAutoUpdateAtLaunch(boolean autoUpdateAtLaunch) {
        this.autoUpdateAtLaunch = autoUpdateAtLaunch;
    }

    public boolean isUpdateOnlyEnabledPacks() {
        return updateOnlyEnabledPacks;
    }

    public void setUpdateOnlyEnabledPacks(boolean updateOnlyEnabledPacks) {
        this.updateOnlyEnabledPacks = updateOnlyEnabledPacks;
    }
}
