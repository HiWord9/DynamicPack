package com.adamcalculator.dynamicpack.status;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.util.Loader;
import com.adamcalculator.dynamicpack.util.Out;
import com.adamcalculator.dynamicpack.util.Urls;
import org.json.JSONObject;

/**
 * Check status from developer.
 */
public class StatusChecker {
    private static final String URL = "https://adamcalculator.github.io/DynamicPack/dynamicpack.status.v1.json";


    private static boolean isUpdateAvailable = false;
    private static boolean isFormatActual = true;
    private static boolean isSafe = true;
    private static boolean isChecked = false;

    public static void check() {
        Out.println("Checking status...");

        try {
            String s = Urls.parseTextContent(URL, 1024 * 512);
            JSONObject j = new JSONObject(s);
            String platformKey;
            JSONObject lat = j.getJSONObject(platformKey = getLatestKeyForPlatform(DynamicPackMod.getLoader()));
            isUpdateAvailable = lat.getLong("build") > SharedConstrains.VERSION_BUILD;
            isSafe = lat.getLong("safe") <= SharedConstrains.VERSION_BUILD;
            isFormatActual = lat.getLong("format") <= SharedConstrains.VERSION_BUILD;

            isChecked = true;
            Out.println(String.format("Status checked! platformKey=%s, isSafe=%s, isFormatActual=%s, isUpdateAvailable=%s", platformKey, isSafe, isFormatActual, isUpdateAvailable));

        } catch (Exception e) {
            Out.error("Error while checking status...", e);
        }
    }

    private static String getLatestKeyForPlatform(Loader loader) {
        if (loader == null) {
            return "latest_version";
        }
        return switch (loader) {
            case UNKNOWN -> "latest_version";
            case FABRIC -> "latest_version_fabric";
            case FORGE -> "latest_version_forge";
            case NEO_FORGE -> "latest_version_neoforge";
        };
    }

    public static boolean isBlockUpdating(String remoteType) {
        if (remoteType.equals("modrinth")) {
            return false;
        }
        return !isSafe();
    }


    public static boolean isModUpdateAvailable() {
        return isUpdateAvailable;
    }

    public static boolean isSafe() {
        return isSafe;
    }

    public static boolean isFormatActual() {
        return isFormatActual;
    }

    public static boolean isChecked() {
        return isChecked;
    }
}
