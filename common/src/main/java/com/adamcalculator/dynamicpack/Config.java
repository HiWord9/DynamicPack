package com.adamcalculator.dynamicpack;


import com.adamcalculator.dynamicpack.util.JsonUtils;
import com.adamcalculator.dynamicpack.util.Out;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class Config {
    private final int formatVersion = 1; // do not touch

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
            JsonObject json = JsonUtils.readJson(file.toPath());
            int formatVersion = JsonUtils.optInt(json, "formatVersion", 0);
            if (formatVersion == 1) {
                // sets variables here

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
            JsonObject json = new JsonObject();

            json.addProperty("formatVersion", formatVersion);
            // put variables here

            File file = DynamicPackMod.getConfigFile();
            file.createNewFile();
            Files.writeString(file.toPath(), JsonUtils.toString(json), StandardOpenOption.WRITE);

        } catch (Exception e) {
            Out.error("Config save failed :(", e);
        }
    }
}
