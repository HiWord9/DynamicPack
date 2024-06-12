package com.adamcalculator.dynamicpack.policy;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashMap;

public class PackPolicyManager {
    private final HashMap<String, PackPolicyEntry> packPolicies = new HashMap<>();

    public static PackPolicyManager load() {
        var file = getFile();
        if (!file.exists()) {
            return loadDefaultPackPolicy();
        }

        // todo
        return null;
    }

    private static PackPolicyManager loadDefaultPackPolicy() {
        var file = getFile();
        if (file.exists()) {
            throw new RuntimeException(new FileAlreadyExistsException("File already exists"));
        }

        var manager = new PackPolicyManager();
        manager.save();
        return manager;
    }

    public void save() {
        var file = getFile();

        JSONObject json = new JSONObject();
    }

    private static File getFile() {
        return new File(DynamicPackMod.getConfigDir(), "packs_preferences.json");
    }
}
