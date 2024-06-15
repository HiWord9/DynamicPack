package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.SharedConstrains;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.UnzipParameters;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class PathsUtil {
    /**
     * Delete path-file and log
     */
    public static void delete(Path path) throws IOException {
        Files.delete(path);
        FilesLog.deleted(path);
    }

    /**
     * Create path-file and log
     */
    public static void createFile(Path path) throws IOException {
        Files.createFile(path);
        FilesLog.created(path);
    }


    public static File[] listFiles(File file) {
        return file.listFiles();
    }

    /**
     * Move a file source to dest place
     * @param source file from
     * @param dest file to
     */
    public static void moveFile(File source, File dest) throws IOException {
        Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Extract a zipFilePath to dir
     * @param zipFilePath file.zip
     * @param dir for example /resourcepacks/Pack1/
     */
    public static void unzip(File zipFilePath, File dir) throws Exception {
        if (SharedConstrains.USE_ZIP4J_FOR_UNZIP) {
            UnzipParameters unzipParameters = new UnzipParameters();
            unzipParameters.setExtractSymbolicLinks(false);
            ZipFile zip = new ZipFile(zipFilePath);
            zip.extractAll(dir.getPath(), unzipParameters);
            zip.close();

        } else {
            // untested code
            PackUtil.openPackFileSystem(zipFilePath, zip -> {
                Set<String> buffer = new HashSet<>();
                walkScan(buffer, zip);

                for (String relative : buffer) {
                    Path path = zip.resolve(relative);
                    Path toPath = dir.toPath().resolve(relative);

                    createDirsToFile(toPath);

                    Files.copy(path, toPath, StandardCopyOption.REPLACE_EXISTING);
                }
            });
        }
    }


    /**
     * Force delete a directory
     * @param file directory only!
     */
    public static void recursiveDeleteDirectory(File file) {
        try {
            if (!file.isDirectory()) {
                throw new RuntimeException("File not a directory.");
            }
            FileUtils.deleteDirectory(file);
            FilesLog.deleted(file.toPath());

        } catch (IOException e) {
            throw new RuntimeException("Exception while recursive delete dir " + file, e);
        }
    }

    private static boolean _nioIsDirExistsAndEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return Files.list(path).count() == 0;
        }

        return false;
    }

    /**
     * Delete path and remove empty parent dirs
     */
    public static void nioSmartDelete(Path toDel) throws IOException {
        FilesLog.deleted(toDel);
        Path toDelParent = toDel.getParent();
        Files.deleteIfExists(toDel);
        if (toDelParent != null && _nioIsDirExistsAndEmpty(toDelParent)) {
            nioSmartDelete(toDelParent);
        }
    }


    /**
     * Write a text to path
     */
    public static void nioWriteText(Path path, String text) {
        try {
            if (Files.exists(path) && !Files.isRegularFile(path)) {
                throw new SecurityException("Try to write text to a not regular file.");
            }
            Files.deleteIfExists(path);
            Files.writeString(path, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        } catch (IOException e) {
            throw new RuntimeException("nioWriteText exception!", e);
        }
    }

    /**
     * Read text from file
     */
    public static String nioReadText(Path path) {
        try {
            if (!Files.exists(path) || Files.isDirectory(path)) {
                throw new RuntimeException("This is not a file. Not found or directory. Cannot be read as text.");
            }
            return Files.readString(path);

        } catch (IOException e) {
            throw new RuntimeException("nioReadText exception!", e);
        }
    }


    /**
     * If paths parent not exists, create dirs to file
     */
    public static void createDirsToFile(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
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
                    buffer.add(path.relativize(path1).toString());
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

    public static String readString(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
