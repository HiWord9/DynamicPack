package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.InputValidator;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.pack.BaseContent;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.pack.OverrideType;
import com.adamcalculator.dynamicpack.pack.Remote;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.util.PackUtil;
import com.adamcalculator.dynamicpack.util.PathsUtil;
import com.adamcalculator.dynamicpack.util.Out;
import com.adamcalculator.dynamicpack.util.Urls;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DynamicRepoRemote extends Remote {
    public static final String REPO_JSON = "dynamicmcpack.repo.json";
    public static final String REPO_BUILD = "dynamicmcpack.repo.build";


    protected DynamicResourcePack parent; // parent
    private JSONObject cachedCurrentJson; // root.current json
    private JSONObject cachedRemoteJson; // root.remote json
    protected String url;
    protected String buildUrl;
    protected String packUrl;

    /**
     * <pre>
     *  if key exists, value used for override content. If content is required it ignored...
     * </pre>
     */
    private final HashMap<String, Boolean> contentOverrides = new HashMap<>();

    public DynamicRepoRemote() {
    }

    /**
     * Init this remote object and associate with pack
     * @param pack parent
     * @param remote root.remote
     */
    public void init(DynamicResourcePack pack, JSONObject remote) {
        this.parent = pack;
        this.cachedRemoteJson = remote;
        this.cachedCurrentJson = pack.getCurrentJson();
        this.url = remote.getString("url");
        InputValidator.throwIsUrlInvalid(url);
        this.buildUrl = url + "/" + REPO_BUILD;
        this.packUrl = url + "/" + REPO_JSON;

        recalculateContentOverrideFromJson();

        boolean signNoRequired = remote.optBoolean("sign_no_required", false);
        if (signNoRequired == remote.has("public_key")) {
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
     * Update this.contentOverrides from cachedRemoteJson
     */
    private void recalculateContentOverrideFromJson() {
        this.contentOverrides.clear();
        if (cachedRemoteJson.has("content_override")) {
            JSONObject j = cachedRemoteJson.getJSONObject("content_override");
            for (String s : j.keySet()) {
                this.contentOverrides.put(s, j.getBoolean(s));
            }
        }
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
        return cachedCurrentJson.optLong("build", -1);
    }


    /**
     * Update client json <code>root.current.known_contents</code>
     * @param repoContents json block repo json <code>root.contents</code>
     */
    public void updateCurrentKnownContents(JSONArray repoContents) {
        JSONObject newKnown = new JSONObject();
        if (cachedCurrentJson.has("known_contents")) {
            cachedCurrentJson.remove("known_contents");
        }
        cachedCurrentJson.put("known_contents", newKnown);

        for (Object _repoContent : repoContents) {
            JSONObject repoContent = (JSONObject) _repoContent;

            var id = repoContent.getString("id");
            var required = repoContent.optBoolean("required", false);
            var defaultActive = repoContent.optBoolean("default_active", true);
            var hash = repoContent.getString("hash");


            JSONObject cacheJson = new JSONObject()
                    .put("hash", hash)
                    .put("default_active", defaultActive);

            if (required) cacheJson.put("required", true);

            String name = repoContent.optString("name", null);
            if (name != null) {
                if (InputValidator.isContentNameValid(name)) {
                    cacheJson.put("name", name);
                } else {
                    Out.println("Name of content '" + id + "' not valid.");
                }
            }

            newKnown.put(id, cacheJson);
        }
    }

    public String getCurrentPackContentHash(String id) {
        if (cachedCurrentJson.has("known_contents")) {
            try {
                return cachedCurrentJson.getJSONObject("known_contents").getJSONObject(id).getString("hash");

            } catch (Exception e) {
                // if hash not found
                return null;
            }
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public String getPackUrl() {
        return packUrl;
    }

    /**
     * Is content active by contentOverrides (only settings)
     */
    public boolean isContentActive(String id, boolean def) {
        if (contentOverrides.containsKey(id)) {
            return contentOverrides.get(id);
        }
        return def;
    }

    public List<BaseContent> getKnownContents() {
        if (cachedCurrentJson.has("known_contents")) {
            JSONObject known = cachedCurrentJson.getJSONObject("known_contents");
            List<BaseContent> contents = new ArrayList<>();
            for (String contentId : known.keySet()) {
                JSONObject content = known.getJSONObject(contentId);
                boolean required = content.optBoolean("required", false);
                boolean defaultValue = content.optBoolean("default_active", true);
                contents.add(new BaseContent(this, contentId, required, required ? OverrideType.TRUE : getCurrentOverrideStatus(contentId), content.optString("name", null), required || defaultValue));
            }
            return contents;
        }
        return new ArrayList<>();
    }

    private OverrideType getCurrentOverrideStatus(String contentId) {
        if (contentOverrides.containsKey(contentId)) {
            return OverrideType.ofBoolean(contentOverrides.get(contentId));
        }
        return OverrideType.NOT_SET;
    }

    public void setContentOverride(BaseContent baseContent, OverrideType overrideType) throws Exception {
        Out.debug("setContentOverride: " + baseContent.getId() + ": " + overrideType);
        JSONObject override = null;
        if (cachedRemoteJson.has("content_override")) {
            override = cachedRemoteJson.getJSONObject("content_override");

        } else if (overrideType != OverrideType.NOT_SET) {
            override = new JSONObject();
        }

        if (override != null) {
            if (overrideType == OverrideType.NOT_SET) {
                override.remove(baseContent.getId());
            } else {
                override.put(baseContent.getId(), overrideType.asBoolean());
            }
            if (override.keySet().isEmpty()) {
                cachedRemoteJson.remove("content_override");

            } else if (!cachedRemoteJson.has("content_override")) {
                cachedRemoteJson.put("content_override", override);
            }
        }


        recalculateContentOverrideFromJson();
        PackUtil.openPackFileSystem(parent.getLocation(), path -> PathsUtil.nioWriteText(path.resolve(SharedConstrains.CLIENT_FILE), parent.getPackJson().toString(2)));
    }
}
