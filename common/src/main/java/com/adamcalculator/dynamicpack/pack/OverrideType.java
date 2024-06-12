package com.adamcalculator.dynamicpack.pack;

public enum OverrideType {
    TRUE,
    FALSE,
    NOT_SET;

    public OverrideType next() {
        return switch (this) {
            case TRUE -> FALSE;
            case FALSE -> NOT_SET;
            case NOT_SET -> TRUE;
        };
    }

    public static OverrideType ofBoolean(boolean b) {
        return b ? TRUE : FALSE;
    }

    public boolean asBoolean() {
        if (this == NOT_SET) {
            throw new UnsupportedOperationException("asBoolean() don't support for NOT_SET");
        }
        return this == TRUE;
    }
}
