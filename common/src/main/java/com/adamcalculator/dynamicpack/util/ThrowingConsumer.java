package com.adamcalculator.dynamicpack.util;

public interface ThrowingConsumer <E extends Exception, T> {
    void accept(T t) throws E;
}
