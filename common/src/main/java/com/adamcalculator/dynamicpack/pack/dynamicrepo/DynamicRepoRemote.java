package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.InputValidator;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.pack.Remote;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.util.JsonUtils;
import com.adamcalculator.dynamicpack.util.Urls;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DynamicRepoRemote extends Remote {
    public static final String REPO_JSON = "dynamicmcpack.repo.json";
    public static final String REPO_BUILD = "dynamicmcpack.repo.build";


    protected DynamicResourcePack parent; // parent
    private JsonObject cachedCurrentJson; // root.current json
    private JsonObject cachedRemoteJson; // root.remote json
    private String url;
    private String buildUrl;
    private String packUrl;
    private DynamicRepoPreferences preferences;

    public DynamicRepoRemote() {
    }

    /**
     * Init this remote object and associate with pack
     * @param pack parent
     * @param clientJson root.remote
     */
    public void init(@NotNull DynamicResourcePack pack, @NotNull JsonObject clientJson) {
        this.parent = pack;
        this.cachedRemoteJson = clientJson;
        this.cachedCurrentJson = pack.getCurrentJson();
        this.url = JsonUtils.getString(clientJson, "url");
        InputValidator.throwIsUrlInvalid(url);
        this.buildUrl = url + "/" + REPO_BUILD;
        this.packUrl = url + "/" + REPO_JSON;
        this.preferences = new DynamicRepoPreferences(pack, this);

        boolean signNoRequired = JsonUtils.optBoolean(clientJson, "sign_no_required", false);
        if (signNoRequired == clientJson.has("public_key")) {
            throw new RuntimeException("Please add sign_no_required=true");
        }
    }


    /**
     * Sync builder
     */
    public SyncBuilder syncBuilder() {
        return new DynamicRepoSyncBuilder(parent, this);
    }


    /**
     * Check dynamicmcpack.repo.build file
     */
    @Override
    public boolean checkUpdateAvailable() throws IOException {
        String content = Urls.parseTextContent(buildUrl, 64).trim();
        return getCurrentBuild() != Long.parseLong(content);
    }

    /**
     * Get current build from cachedCurrentJson
     */
    public long getCurrentBuild() {
        return JsonUtils.optLong(cachedCurrentJson, "build", -1);
    }

    public JsonObject getCachedCurrentJson() {
        return cachedCurrentJson;
    }

    public JsonObject getCachedRemoteJson() {
        return cachedRemoteJson;
    }

    public DynamicRepoPreferences getPreferences() {
        return preferences;
    }

    public String getUrl() {
        return url;
    }

    public String getPackUrl() {
        return packUrl;
    }

    public void notifyNewRemoteJson(JsonObject repoJson) {
        // unlink with repo because this is notifying!
        repoJson = repoJson.deepCopy();

        preferences.notifyNewRemoteJson(repoJson);
    }
}
