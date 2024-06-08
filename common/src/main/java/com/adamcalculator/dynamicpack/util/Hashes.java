package com.adamcalculator.dynamicpack.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Hashes {
    public static String sha1sum(File file) throws IOException {
        return sha1sum(file.toPath());
    }

    public static String sha1sum(Path path) throws IOException {
        return DigestUtils.sha1Hex(Files.newInputStream(path));
    }

    public static String sha1sum(InputStream inputStream) throws IOException {
        return DigestUtils.sha1Hex(inputStream);
    }

    public static String sha1sum(byte[] bytes) {
        return DigestUtils.sha1Hex(bytes);
    }
}
