package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * <h2>Preserving the psyche during migration from org.json to gson</h2>
 * @author AdamCalculator
 */
public class JsonUtils {
    // GETs
    public static int optInt(JsonObject json, String key, int def) {
        if (json.has(key)) {
            return json.getAsJsonPrimitive(key).getAsInt();
        }

        return def;
    }

    public static int optInt(JsonObject json, String key) {
        return optInt(json, key, 0); // default value as org.json
    }

    public static int getInt(JsonObject json, String key) {
        return json.getAsJsonPrimitive(key).getAsInt();
    }

    public static long optLong(JsonObject json, String key, long def) {
        if (json.has(key)) {
            return getLong(json, key);
        }
        return def;
    }

    public static long optLong(JsonObject json, String key) {
        return optLong(json, key, 0L);
    }

    public static long getLong(JsonObject json, String key) {
        return json.getAsJsonPrimitive(key).getAsLong();
    }



    public static String getString(JsonObject json, String key) {
        return json.getAsJsonPrimitive(key).getAsString();
    }

    public static String optString(JsonObject json, String key, String def) {
        if (json.has(key)) {
            return getString(json, key);
        }
        return def;
    }

    public static String optString(JsonObject json, String key) {
        return optString(json, key, null);
    }


    public static JsonArray getJsonArray(JsonObject json, String key) {
        return json.getAsJsonArray(key);
    }

    public static boolean getBoolean(JsonObject json, String key) {
        return json.getAsJsonPrimitive(key).getAsBoolean();
    }

    public static boolean optBoolean(JsonObject json, String key, boolean def) {
        if (json.has(key)) {
            return json.get(key).getAsBoolean();
        }
        return def;
    }

    // CREATEs
    public static JsonObject fromString(String s) {
        return SharedConstrains.GSON.fromJson(s, JsonObject.class);
    }

    public static JsonObject readJson(InputStream inputStream) throws IOException {
        return JsonUtils.fromString(PathsUtil.readString(inputStream));
    }

    public static JsonObject readJson(Path path) throws IOException {
        return JsonUtils.fromString(PathsUtil.readString(path));
    }

    public static JsonArray arrayFromString(String s) {
        return SharedConstrains.GSON.fromJson(s, JsonArray.class);
    }

    public static String toString(JsonObject json) {
        return SharedConstrains.GSON.toJson(json);
    }
}
