package com.adamcalculator.dynamicpack.util;

public interface ThrowingFunctionRet<E extends Exception, R> {
    R run() throws E;
}
