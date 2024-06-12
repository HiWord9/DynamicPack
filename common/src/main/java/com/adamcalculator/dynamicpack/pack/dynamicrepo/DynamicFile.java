package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import java.nio.file.Path;

public class DynamicFile {
    private final String url;
    private final String path;
    private final int size;
    private final String hash;
    private Path tempPath;

    public DynamicFile(String url, String path, int size, String hash) {
        this.url = url;
        this.path = path;
        this.size = size;
        this.hash = hash;
    }

    public void setTempPath(Path tempPath) {
        this.tempPath = tempPath;
    }

    public Path getTempPath() {
        return tempPath;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public int getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }
}
