package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.pack.BaseContent;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.pack.OverrideType;
import com.adamcalculator.dynamicpack.util.JsonUtils;
import com.adamcalculator.dynamicpack.util.Out;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DynamicRepoPreferences {
    private final DynamicResourcePack pack;
    private final DynamicRepoRemote remote;

     // if key exists, value used for override content. If content is required it ignored...
    private final HashMap<String, Boolean> contentOverrides = new HashMap<>();

    private final JsonObject cachedRemoteJson;
    private final JsonObject cachedCurrentJson;

    public DynamicRepoPreferences(DynamicResourcePack pack, DynamicRepoRemote remote) {
        this.pack = pack;
        this.remote = remote;
        this.cachedRemoteJson = remote.getCachedRemoteJson();
        this.cachedCurrentJson = remote.getCachedCurrentJson();

        initInternally();
    }

    private void initInternally() {
        recalculateContentOverrideFromJson();
    }

    /**
     * Update this.contentOverrides from cachedRemoteJson
     */
    private void recalculateContentOverrideFromJson() {
        this.contentOverrides.clear();
        if (cachedRemoteJson.has("content_override")) {
            JsonObject j = cachedRemoteJson.getAsJsonObject("content_override");
            for (String s : j.keySet()) {
                this.contentOverrides.put(s, JsonUtils.getBoolean(j, s));
            }
        }
    }

    public void notifyNewRemoteJson(JsonObject repoJson) {
        JsonArray contents = JsonUtils.getJsonArray(repoJson, "contents");
        JsonArray guis = JsonUtils.getJsonArray(repoJson, "gui");

        if (guis == null) {
            Out.debug("Repo not using 'gui' features.");
        }


        cachedCurrentJson.add("known_contents", contents);
        if (guis != null) {
            cachedCurrentJson.add("known_guis", guis);
        } else {
            cachedCurrentJson.remove("known_guis");
        }
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
        try {
            if (cachedCurrentJson.has("known_contents")) {
                JsonArray known = cachedCurrentJson.getAsJsonArray("known_contents");
                List<BaseContent> contents = new ArrayList<>();
                for (JsonElement _element : known) {
                    JsonObject content = (JsonObject) _element;
                    String contentId = JsonUtils.getString(content, "id");
                    boolean required = JsonUtils.optBoolean(content, "required", false);
                    boolean defaultValue = JsonUtils.optBoolean(content, "default_active", true);
                    boolean hidden = JsonUtils.optBoolean(content, "hidden", false);
                    var name = JsonUtils.optString(content, "name", null);
                    var resultOverride = required ? OverrideType.TRUE : getCurrentOverrideStatus(contentId);

                    contents.add(new BaseContent(remote, contentId, required, resultOverride, name, defaultValue, hidden));
                }
                return contents;
            }
        } catch (Exception e) {
            pack.setLatestException(e);
            Out.error("Error while getKnownContents()", e);
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
        JsonObject override = null;
        if (cachedRemoteJson.has("content_override")) {
            override = cachedRemoteJson.getAsJsonObject("content_override");

        } else if (overrideType != OverrideType.NOT_SET) {
            override = new JsonObject();
        }

        if (override != null) {
            if (overrideType == OverrideType.NOT_SET) {
                override.remove(baseContent.getId());
            } else {
                override.addProperty(baseContent.getId(), overrideType.asBoolean());
            }
            if (override.keySet().isEmpty()) {
                cachedRemoteJson.remove("content_override");

            } else if (!cachedRemoteJson.has("content_override")) {
                cachedRemoteJson.add("content_override", override);
            }
        }


        recalculateContentOverrideFromJson();
        pack.saveClientFile();
    }


}
