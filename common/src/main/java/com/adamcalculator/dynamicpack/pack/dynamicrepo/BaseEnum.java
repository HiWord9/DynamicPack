package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.pack.OverrideType;
import com.adamcalculator.dynamicpack.util.JsonUtils;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

public class BaseEnum {
    private final String id;
    private final String name;
    private final LinkedHashMap<String, Element> elements = new LinkedHashMap<>();

    private BaseEnum(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static BaseEnum ofJson(JsonObject json) {
        BaseEnum baseEnum = new BaseEnum(JsonUtils.getString(json, "id"), JsonUtils.optString(json, "name"));

        JsonObject enumJson = json.getAsJsonObject("enum");
        for (String key : enumJson.keySet()) {
            Element element = Element.ofJson(enumJson.getAsJsonObject(key));
            baseEnum.elements.put(key, element);
        }

        return baseEnum;
    }

    public String getCurrentState(BaseContent[] contents) {
        try {
            return getCurrentElement(contents).name;
        } catch (Exception ignored) {
            return "Unknown";
        }
    }

    public Element getCurrentElement(BaseContent[] contents) {
        for (String s : elements.keySet()) {
            Element element = elements.get(s);

            boolean found = true;
            for (String contentId : element.contents.keySet()) {
                boolean requiredBool = element.contents.get(contentId);
                BaseContent baseContent = BaseContent.findById(contents, contentId);
                boolean actualBool = baseContent.getOverride().asBoolean(baseContent.getDefaultState());
                if (actualBool != requiredBool) {
                    found = false;
                }
            }

            if (found) {
                return element;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void applyNext(BaseContent[] contents) throws Exception{
        Element curr = getCurrentElement(contents);
        if (curr == null) {
            Optional<Element> first = elements.values().stream().findFirst();
            if (first.isPresent()) {
                first.get().apply(contents);
            }
            return;
        }

        int index = -1;
        int i = 0;
        for (String s : elements.keySet()) {
            Element element = elements.get(s);
            if (element == curr) {
                index = i;
                break;
            }
            i++;
        }

        Element[] elementsArray = elements.values().toArray(new Element[0]);
        Element next = elementsArray[(index + 1) % elementsArray.length];
        next.apply(contents);
    }

    public static class Element {
        private String name;
        private final HashMap<String, Boolean> contents = new HashMap<>();

        public static Element ofJson(JsonObject json) {
            Element element = new Element();
            element.name = JsonUtils.getString(json, "name");

            JsonObject jsonContents = json.getAsJsonObject("contents");

            for (String contentId : jsonContents.keySet()) {
                boolean bool = JsonUtils.getBoolean(jsonContents, contentId);
                element.contents.put(contentId, bool);
            }

            return element;
        }

        public void apply(BaseContent[] contents) throws Exception {
            for (String contentId : this.contents.keySet()) {
                boolean bool = this.contents.get(contentId);
                BaseContent.findById(contents, contentId).setOverrideType(OverrideType.ofBoolean(bool), contents);
            }
        }

        public HashMap<String, Boolean> getContents() {
            return contents;
        }

        public String getName() {
            return name;
        }
    }
}
