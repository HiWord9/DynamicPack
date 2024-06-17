package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

public class PackUtil {
    private static final List<OpenedFile> OPENED_ZIP_FILES = new ArrayList<>();
    
    public static void openPackFileSystem(File file, ThrowingConsumer<Exception, Path> consumer) throws Exception {
        openPackFileSystem(file, null, consumer);
    }

    /**
     * Open file (or dir) as nio.Path
     * <p>If exception in consumer filesystem closed normally</p>
     * @param file resourcepack file
     * @param consumer path consumer
     * @throws Exception any exception
     */
    public static void openPackFileSystem(File file, @Nullable Runnable preClose, ThrowingConsumer<Exception, Path> consumer) throws Exception {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getCanonicalPath());
        }

        if (file.isDirectory()) {
            consumer.accept(file.toPath());


        } else if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");

            URI uri = URI.create("jar:" + file.toPath().toUri());
            Exception ex = null;
            FileSystem fs = null;
            try {
                fs = FileSystems.newFileSystem(uri, env);
                try {
                    consumer.accept(fs.getPath(""));
                } catch (Exception e) {
                    ex = e;
                }
                
            } finally {
                if (fs != null) {
                    if (preClose != null) {
                        preClose.run();
                    }
                    fs.close();
                }
            }
            if (ex != null) {
                throw ex;
            }

        } else {
            throw new FailedOpenPackFileSystemException("Failed to recognize pack filesystem: not dir or zip");
        }
    }

    /**
     * Download file for dynamic_repo
     * @throws IOException if latest attempt failed exception rethrown
     */
    public static void downloadPackFile(String url, Path path, String hash, LongConsumer progress) throws IOException {
        final int maxI = SharedConstrains.MAX_ATTEMPTS_TO_DOWNLOAD_FILE;
        int i = maxI;
        while (i > 0) {
            try {
                PathsUtil.createDirsToFile(path);

                if (Files.exists(path)) {
                    PathsUtil.delete(path);
                }
                PathsUtil.createFile(path);

                try {
                    Urls._transferStreamsWithHash(hash, Urls._getInputStreamOfUrl(url, SharedConstrains.DYNAMIC_PACK_HTTPS_FILE_SIZE_LIMIT, progress), Files.newOutputStream(path), progress);
                    FilesLog.writtenByUrl(path, url);

                } catch (Exception e) {
                    throw new RuntimeException("File " + path + " download error. From url: " + url + ". Expected hash: " + hash, e);
                }
                return;

            } catch (Exception e) {
                Out.error("downloadPackFile. Attempt=" + (maxI - i + 1) + "/" + maxI, e);
                if (i == 1) {
                    throw e;
                }
            }

            i--;
        }
    }

    public static Runnable createMcPackFinalizerRunnable(DynamicResourcePack dynamicResourcePack) {
        return () -> {
            File resourcepack = dynamicResourcePack.getLocation();
            closeFiles(resourcepack);
        };
    }

    public static void closeFiles(File file) {
        Out.debug("[PackUtil] closeFiles file=" + file);

        List<OpenedFile> toClose = findAllOpenedFiles(file);
        for (OpenedFile openedFile : toClose) {
            openedFile.close();
        }
        
        OPENED_ZIP_FILES.removeAll(toClose);
    }

    public static void markClosedFile(File file) {
        Out.debug("[PackUtil] markClosedFile file=" + file);
        List<OpenedFile> toRemove = findAllOpenedFiles(file);
        OPENED_ZIP_FILES.removeAll(toRemove);
    }

    public static void addFileToOpened(File file, AutoCloseable closeable) {
        Out.debug("[PackUtil] addFileToOpen file=" + file);
        OPENED_ZIP_FILES.add(new OpenedFile(file, closeable));
    }

    private static List<OpenedFile> findAllOpenedFiles(File file) {
        List<OpenedFile> found = new ArrayList<>();

        for (OpenedFile openedFile : OPENED_ZIP_FILES.toArray(new OpenedFile[0])) {
            if (openedFile.file.equals(file)) {
                found.add(openedFile);
            }
        }

        return found;
    }
    
    private static class OpenedFile implements Closeable {
        File file;
        AutoCloseable closeable;
        
        public OpenedFile(File file, AutoCloseable closeable) {
            this.file = file;
            this.closeable = closeable;
        }

        public void close() {
            Out.debug("[PackUtil] [OpenedFile] close() file=" + file.getName() + "; closable=" + closeable);
            try {
                closeable.close();
            } catch (Exception e) {
                Out.debug("Error while close.. ignore it he " + e);
            }
        }
    }
}
