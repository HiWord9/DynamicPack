package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.pack.OverrideType;

import java.util.Set;

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
    private final Set<String> exclude;

    public BaseContent(DynamicRepoRemote parentRemote, String id, boolean required, OverrideType overrideType, String name, boolean defaultStatus, boolean hidden, Set<String> exclude) {
        this.parentRemote = parentRemote;
        this.id = id;
        this.required = required;
        this.overrideType = overrideType;
        this.name = name;
        this.defaultStatus = defaultStatus;
        this.hidden = hidden;
        this.exclude = exclude;
    }

    public String getId() {
        return id;
    }

    public boolean isRequired() {
        return required;
    }

    public void nextOverride() {
        setOverrideType(overrideType.next());
    }

    public void setOverrideType(OverrideType overrideType) {
        this.overrideType = overrideType;
    }

    public OverrideType getOverride() {
        return overrideType;
    }

    public boolean getDefaultState() {
        return defaultStatus;
    }

    public String getName() {
        return name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Set<String> getExclude() {
        return exclude;
    }

    public static BaseContent findById(BaseContent[] contents, String findId) {
        for (BaseContent content : contents) {
            if (content.getId().equalsIgnoreCase(findId)) {
                return content;
            }
        }
        return null;
    }
}
