package com.adamcalculator.dynamicpack.util;

public interface ThrowingFunction<E extends Exception> {
    void run() throws E;
}
