package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

public class PackUtil {

    public static JSONObject readJson(InputStream inputStream) throws IOException {
        return new JSONObject(readString(inputStream));
    }

    public static String readString(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static JSONObject readJson(Path path) throws IOException {
        return new JSONObject(readString(path));
    }

    public static String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

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
     * Fill buffer with paths to all files (recursively) in path. adding only paths to files. Directories not adding here
     * <pre>
     *  Example buffer content:
     *  * /file.txt
     *  * /folder/texture.png
     *  * /folder/sound.ogg
     *  * /directory/huge.mp4
     * </pre>
     */
    public static void walkScan(Set<String> buffer, Path path) {
        try (Stream<Path> entries = Files.walk(path, Integer.MAX_VALUE)) {
            entries.forEach(path1 -> {
                if (!Files.isDirectory(path1)) {
                    buffer.add(path1.toString());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Exception while walkScan", e);
        }
    }

    // if path exist and isFile
    public static boolean isPathFileExists(Path path) {
        if (Files.exists(path)) {
            return !Files.isDirectory(path);
        }
        return false;
    }

    public static void downloadPackFile(String url, Path path, String hash, LongConsumer progress) throws IOException {
        final int maxI = SharedConstrains.MAX_ATTEMPTS_TO_DOWNLOAD_FILE;
        int i = maxI;
        while (i > 0) {
            try {
                PackUtil.createDirsToFile(path);

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
            }

            i--;
        }
        Out.warn("Failed to download file " + path + " from " + url);
    }

    /**
     * If paths parent not exists, create dirs to file
     */
    public static void createDirsToFile(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(path);
        }
    }
}
