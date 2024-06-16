package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.InputValidator;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.pack.OverrideType;
import com.adamcalculator.dynamicpack.util.JsonUtils;
import com.adamcalculator.dynamicpack.util.Out;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.Validate;

import java.util.*;

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
            if (j.size() == 0) {
                cachedCurrentJson.remove("content_override");
            }
        }
    }

    public void notifyNewRemoteJson(JsonObject repoJson) {
        JsonArray contents = JsonUtils.getJsonArray(repoJson, "contents");
        JsonArray guis = JsonUtils.getJsonArray(repoJson, "gui");

        if (guis == null) {
            Out.debug("Repo not using 'gui' features.");
        }

        updateKnownContents(contents);
        updateKnownGuis(guis);
    }

    private void updateKnownGuis(JsonArray guis) {

        if (guis != null) {
            validateGuis(guis);
            cachedCurrentJson.add("known_guis", guis);
        } else {
            cachedCurrentJson.remove("known_guis");
        }
    }

    private void validateGuis(JsonArray guis) {
        for (JsonElement _gui : guis) {
            JsonObject gui = _gui.getAsJsonObject();
            var type = JsonUtils.getString(gui, "type");
            var id = JsonUtils.getString(gui, "id");

            InputValidator.throwIsContentIdInvalid(id);
            InputValidator.throwIsContentIdInvalid(type); // use contentId filter for type.. warning

            if (type.equalsIgnoreCase("enum")) {
                var enums = gui.getAsJsonObject("enum");
                for (String key : enums.keySet()) {
                    InputValidator.throwIsContentIdInvalid(key);
                    var anEnum = enums.getAsJsonObject(key);

                    if (!InputValidator.isDynamicPackNameValid(JsonUtils.getString(anEnum, "name"))) {
                        throw new RuntimeException("Name of enum element invalid :( enumKey=" + key);
                    }

                    JsonObject contents = anEnum.getAsJsonObject("contents");
                    for (String contentId : contents.keySet()) {
                        // call get for validate is boolean
                        JsonUtils.getBoolean(contents, contentId);

                        BaseContent content = BaseContent.findById(getKnownContents(), contentId);
                        Validate.notNull(content, "Content from enum not found :(");

                        if (content.isRequired()) {
                            throw new RuntimeException("Override 'required':true content in enum not allowed!");
                        }
                    }
                }
            }
        }
    }

    private JsonObject getContentJsonById(JsonArray contents, String findId) {
        Out.debug("getContentJsonById findId=" + findId);
        for (JsonElement content : contents) {
            JsonObject object = content.getAsJsonObject();
            var id = JsonUtils.getString(object, "id");
            if (id.equalsIgnoreCase(findId)) {
                return object;
            }
        }
        return null;
    }

    private void updateKnownContents(JsonArray contents) {
        JsonObject knownContents = new JsonObject();
        for (JsonElement content : contents) {
            JsonObject jsonObject = (JsonObject) content;
            var id = JsonUtils.getString(jsonObject, "id");
            var required = JsonUtils.optBoolean(jsonObject, "required", false);

            if (required) {
                setContentOverride(id, OverrideType.NOT_SET);
            }
            Set<String> excludeContent = parseContentList(jsonObject, "exclude_content");

            for (String s : excludeContent) {
                JsonObject cont = getContentJsonById(contents, s);
                if (cont == null) {
                    throw new RuntimeException("exclude_content contains id of not found content :(");
                }

                if (JsonUtils.optBoolean(cont, "required", false)) {
                    throw new RuntimeException("Exclude a required content not allowed!");
                }
            }
            if (excludeContent.contains(id)) {
                throw new RuntimeException("Self id in exclude list. Not allowed!");
            }

            // unlink objects for remove id
            JsonObject newJsonObject = jsonObject.deepCopy();
            newJsonObject.remove("id");
            knownContents.add(id, newJsonObject);
        }
        cachedCurrentJson.add("known_contents", knownContents);
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

    public BaseContent[] getKnownContents() {
        try {
            if (cachedCurrentJson.has("known_contents")) {
                JsonObject known = cachedCurrentJson.getAsJsonObject("known_contents");
                List<BaseContent> contents = new ArrayList<>();
                for (String contentId : known.keySet()) {
                    JsonObject content = known.getAsJsonObject(contentId);
                    boolean required = JsonUtils.optBoolean(content, "required", false);
                    boolean defaultValue = JsonUtils.optBoolean(content, "default_active", true);
                    boolean hidden = JsonUtils.optBoolean(content, "hidden", false);
                    var name = JsonUtils.optString(content, "name", null);
                    var resultOverride = required ? OverrideType.TRUE : getCurrentOverrideStatus(contentId);
                    Set<String> exclude = parseContentList(content, "exclude_content");

                    contents.add(new BaseContent(remote, contentId, required, resultOverride, name, defaultValue, hidden, exclude));
                }
                return contents.toArray(new BaseContent[0]);
            }
        } catch (Exception e) {
            pack.setLatestException(e);
            Out.error("Error while getKnownContents()", e);
        }
        return new BaseContent[0];
    }

    private Set<String> parseContentList(JsonObject content, String s) {
        Set<String> set = new HashSet<>();
        if (content.has(s)) {
            JsonElement element = content.get(s);
            if (element.isJsonArray()) {
                for (JsonElement jsonElement : element.getAsJsonArray()) {
                    var str = jsonElement.getAsString();
                    if (set.contains(str)) {
                        throw new RuntimeException(s + ": duplicated!");
                    }
                    set.add(str);
                }
            } else if (element.isJsonPrimitive()) {
                set.add(element.getAsString());
            }
        }
        return set;
    }

    private OverrideType getCurrentOverrideStatus(String contentId) {
        if (contentOverrides.containsKey(contentId)) {
            return OverrideType.ofBoolean(contentOverrides.get(contentId));
        }
        return OverrideType.NOT_SET;
    }

    public void setContentOverride(BaseContent content, OverrideType overrideType) {
        if (content.isRequired()) {
            overrideType = OverrideType.NOT_SET;
        }
        setContentOverride(content.getId(), overrideType);
    }

    public void setContentOverride(String id, OverrideType overrideType) {
        Out.debug("setContentOverride: " + id + ": " + overrideType);
        JsonObject override = null;
        if (cachedRemoteJson.has("content_override")) {
            override = cachedRemoteJson.getAsJsonObject("content_override");

        } else if (overrideType != OverrideType.NOT_SET) {
            override = new JsonObject();
        }

        if (override != null) {
            if (overrideType == OverrideType.NOT_SET) {
                override.remove(id);
            } else {
                override.addProperty(id, overrideType.asBoolean());
            }
            if (override.keySet().isEmpty()) {
                cachedRemoteJson.remove("content_override");

            } else if (!cachedRemoteJson.has("content_override")) {
                cachedRemoteJson.add("content_override", override);
            }
        }

        recalculateContentOverrideFromJson();
    }


    public BaseEnum[] getKnownEnums() {
        try {
            if (cachedCurrentJson.has("known_guis")) {
                JsonArray known = cachedCurrentJson.getAsJsonArray("known_guis");
                List<BaseEnum> enums = new ArrayList<>();
                for (JsonElement _element : known) {
                    JsonObject jsonEnum = _element.getAsJsonObject();

                    // if type != enum, continue (known_guis) may contain not only enums (in future)
                    if (!JsonUtils.getString(jsonEnum, "type").equals("enum")) {
                        continue;
                    }

                    enums.add(BaseEnum.ofJson(jsonEnum));
                }
                return enums.toArray(new BaseEnum[0]);
            }
        } catch (Exception e) {
            pack.setLatestException(e);
            Out.error("Error while getKnownEnums()", e);
        }
        return new BaseEnum[0];
    }
}
