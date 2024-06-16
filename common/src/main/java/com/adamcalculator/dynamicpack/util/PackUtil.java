package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;

public class PackUtil {
    /**
     * Open file (or dir) as nio.Path
     * <p>If exception in consumer filesystem closed normally</p>
     * @param file resourcepack file
     * @param consumer path consumer
     * @throws Exception any exception
     */
    public static void openPackFileSystem(File file, ThrowingConsumer<Exception, Path> consumer) throws Exception {
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
            try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                try {
                    consumer.accept(fs.getPath(""));
                } catch (Exception e) {
                    ex = e;
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
}
