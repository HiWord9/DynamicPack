package com.adamcalculator.dynamicpack;

import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.util.FailedOpenPackFileSystemException;
import com.adamcalculator.dynamicpack.util.Out;
import com.adamcalculator.dynamicpack.util.PackUtil;
import com.adamcalculator.dynamicpack.util.PathsUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Container supported packs
 */
public class PacksContainer {

    private boolean rescanPacksBlocked = false;
    private boolean isPacksScanning = false;

    /**
     * Currently dynamic packs
     * <pre>
     * *key* : *value*
     * "ExamplePack.zip" : DynamicResourcePack
     * </pre>
     */
    private final HashMap<String, DynamicResourcePack> packs = new HashMap<>();

    public PacksContainer() {
    }

    public void lockRescan() {
        rescanPacksBlocked = true;
    }

    public void unlockRescan() {
        rescanPacksBlocked = false;
    }

    public void rescan(File resourcePacks) {
        if (isPacksScanning) {
            Out.warn("Already in scanning!");
            return;
        }

        if (rescanPacksBlocked) {
            Out.warn("Rescan blocked! maybe currently syncing");
            return;
        }

        isPacksScanning = true;
        List<String> forDelete = new ArrayList<>(packs.keySet());
        for (File packFile : PathsUtil.listFiles(resourcePacks)) {
            try {
                PackUtil.openPackFileSystem(packFile, packPath -> {
                    Path clientFile = packPath.resolve(SharedConstrains.CLIENT_FILE);

                    if (Files.exists(clientFile)) {
                        Out.println("+ Pack " + packFile.getName() + " supported by mod!");
                        processPack(packFile, clientFile);
                        forDelete.remove(packFile.getName());

                    } else {
                        Out.println("- Pack " + packFile.getName() + " not supported by mod.");
                    }
                });

            } catch (Exception e) {
                if (e instanceof FailedOpenPackFileSystemException) {
                    Out.warn("Error while processing pack " + packFile.getName() + ": " + e.getMessage());
                } else {
                    Out.error("Error while processing pack: " + packFile.getName(), e);
                }
            }
        }
        for (String s : forDelete) {
            Out.println("Pack " + s + " no longer exists!");
            packs.remove(s);
        }
        isPacksScanning = false;
    }

    private void processPack(File location, Path clientFile) throws Exception {
        var json = PathsUtil.readJson(clientFile);
        long formatVersion = json.getLong("formatVersion");
        DynamicResourcePack oldestPack = packs.getOrDefault(location.getName(), null);
        if (formatVersion == 1) {
            var pack = new DynamicResourcePack(location, json);
            if (oldestPack != null) {
                pack.flashback(oldestPack);
            }
            packs.put(location.getName(), pack);

        } else {
            throw new RuntimeException("Unsupported formatVersion for pack " + location.getName() + ": " + formatVersion);
        }
    }

    public DynamicResourcePack[] getPacks() {
        return packs.values().toArray(new DynamicResourcePack[0]);
    }
}
