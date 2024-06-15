package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.google.common.base.MoreObjects;

import java.nio.file.Path;

public class DynamicFile {
    private final String url;
    private final String path;
    private final int size; // size from remote! May a Integer.MAX_VALUE if remote deprecated!
    private final String hash;
    private Path downloadedPath;

    public DynamicFile(String url, String path, int size, String hash) {
        this.url = url;
        this.path = path;
        this.size = size;
        this.hash = hash;
    }

    public void setDownloadPath(Path downloadedPath) {
        this.downloadedPath = downloadedPath;
    }

    public Path getDownloadedPath() {
        return downloadedPath;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    /**
     * size from remote! May a Integer.MAX_VALUE if remote deprecated!
     */
    public int getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("url", url)
                .add("path", path)
                .add("size", size)
                .add("hash", hash)
                .add("tempPath", downloadedPath)
                .toString();
    }
}
