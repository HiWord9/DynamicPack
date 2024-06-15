package com.adamcalculator.dynamicpack.pack;

import com.adamcalculator.dynamicpack.pack.dynamicrepo.DynamicRepoRemote;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Abstract remote of pack
 */
public abstract class Remote {
    private static boolean initialized = false;
    public static final HashMap<String, Supplier<Remote>> REMOTES = new HashMap<>();

    public static void initRemoteTypes() {
        if (initialized) {
            return;
        }
        initialized = true;
        REMOTES.put("modrinth", ModrinthRemote::new);
        REMOTES.put("dynamic_repo", DynamicRepoRemote::new);
    }

    /**
     * Init this remote object and associate with pack
     * @param pack parent
     * @param remote root.remote
     */
    public abstract void init(DynamicResourcePack pack, JsonObject remote);

    public abstract SyncBuilder syncBuilder();

    public abstract boolean checkUpdateAvailable() throws IOException;

}
