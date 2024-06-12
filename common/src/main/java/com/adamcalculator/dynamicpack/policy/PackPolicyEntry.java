package com.adamcalculator.dynamicpack.policy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PackPolicyEntry {
    @NotNull
    private String hash;

    @Nullable
    private PackTrustLevel trustLevel;

    public PackPolicyEntry(@NotNull String hash, @Nullable PackTrustLevel trustLevel) {
        this.hash = hash;
        this.trustLevel = trustLevel;
    }

    public @NotNull String getHash() {
        return hash;
    }

    public void setHash(@NotNull String hash) {
        this.hash = hash;
    }

    public @Nullable PackTrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(@Nullable PackTrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }
}
