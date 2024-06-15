package com.adamcalculator.dynamicpack.pack;

import com.adamcalculator.dynamicpack.pack.dynamicrepo.DynamicRepoRemote;

public class BaseContent {
    private final DynamicRepoRemote parentRemote;
    private final String id;
    private final boolean required;
    private OverrideType overrideType;
    private final String name;

    // BREAKING CHANGES!
    // since 1.0.31 ignoring requied key.
    //
    private final boolean defaultStatus;
    private final boolean hidden;

    public BaseContent(DynamicRepoRemote parentRemote, String id, boolean required, OverrideType overrideType, String name, boolean defaultStatus, boolean hidden) {
        this.parentRemote = parentRemote;
        this.id = id;
        this.required = required;
        this.overrideType = overrideType;
        this.name = name;
        this.defaultStatus = defaultStatus;
        this.hidden = hidden;
    }

    public String getId() {
        return id;
    }

    public boolean isRequired() {
        return required;
    }

    public void nextOverride() throws Exception {
        setOverrideType(overrideType.next());
    }

    public OverrideType getOverride() {
        return overrideType;
    }

    public boolean getWithDefaultState() {
        return defaultStatus;
    }

    public String getName() {
        return name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setOverrideType(OverrideType overrideType) throws Exception {
        this.overrideType = overrideType;
        parentRemote.getPreferences().setContentOverride(this, overrideType);
    }
}
