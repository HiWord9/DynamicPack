package com.adamcalculator.dynamicpack;


import com.adamcalculator.dynamicpack.policy.PackTrustLevel;
import com.adamcalculator.dynamicpack.util.Out;
import com.adamcalculator.dynamicpack.util.PathsUtil;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class Config {
    private final int formatVersion = 1; // do not touch
    private PackTrustLevel defaultTrustLevel = PackTrustLevel.ASK_TO_UPDATE;

    public static void processChanges(Consumer<Config> itConsumer) {
        var cfg = DynamicPackMod.getConfig();
        if (cfg == null) {
            Out.warn("Failed to get config for processChanges. Do nothing...");
            return;
        }
        itConsumer.accept(cfg);
        cfg.save();
    }

    public static Config load() {
        File file = DynamicPackMod.getConfigFile();
        Config cfg = new Config();
        if (!file.exists()) {
            return cfg;
        }


        try {
            JSONObject json = PathsUtil.readJson(file.toPath());

            int formatVersion = json.optInt("formatVersion", 0);
            if (formatVersion == 1) {
                cfg.defaultTrustLevel = PackTrustLevel.valueOf(json.optString("defaultTrustLevel", cfg.defaultTrustLevel.name()).toUpperCase());

            } else {
                Out.warn("Unsupported formatVersion of config: " + formatVersion + " (default loaded)");
            }

            return cfg;

        } catch (Exception e) {
            Out.error("Config load failed (return default config)", e);
        }
        return new Config();
    }

    public void save() {
        try {
            JSONObject json = new JSONObject();

            json.put("formatVersion", formatVersion)
                    .put("defaultTrustLevel", defaultTrustLevel.name().toLowerCase());

            File file = DynamicPackMod.getConfigFile();
            file.createNewFile();
            Files.writeString(file.toPath(), json.toString(SharedConstrains.JSON_INDENTS), StandardOpenOption.WRITE);

        } catch (Exception e) {
            Out.error("Config save failed :(", e);
        }
    }
}
