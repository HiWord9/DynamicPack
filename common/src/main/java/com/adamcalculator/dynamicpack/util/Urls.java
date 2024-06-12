package com.adamcalculator.dynamicpack.util;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.InputValidator;
import com.adamcalculator.dynamicpack.SharedConstrains;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.LongConsumer;
import java.util.zip.GZIPInputStream;


/**
 * Safe network utils
 */
public class Urls {
    public static boolean isFileDebugSchemeAllowed() {
        return SharedConstrains.isFileDebugSchemeAllowed();
    }

    public static boolean isHTTPTrafficAllowed() {
        return SharedConstrains.isHTTPTrafficAllowed();
    }

    /**
     * Parse text content from url with no progress
     * @param url url
     */
    public static String parseTextContent(String url, long limit) throws IOException {
        return parseTextContent(url, limit, null);
    }

    /**
     * Parse text content from url
     * @param url url
     * @param progress progress
     */
    public static String parseTextContent(String url, long limit, LongConsumer progress) throws IOException {
        return _parseTextFromStream(_getInputStreamOfUrl(url, limit, progress), progress);
    }


    /**
     * Parse GZip compressed content from url
     * @param url url
     */
    public static String parseTextGZippedContent(String url, long limit, LongConsumer progress) throws IOException {
        return _parseTextFromStream(new GZIPInputStream(_getInputStreamOfUrl(url, limit, progress)), progress);
    }


    /**
     * Create temp zipFile and download to it from url.
     */
    public static File downloadFileToTemp(String url, String tmpPrefix, String tmpSuffix, long limit, LongConsumer progress) throws IOException {
        File file = File.createTempFile(tmpPrefix, tmpSuffix);

        var inputStream = _getInputStreamOfUrl(url, limit, progress);
        var outputStream = Files.newOutputStream(file.toPath());
        _transferStreams(inputStream, outputStream, progress);

        return file;
    }


    /**
     * Getting InputStream of url with checks
     */
    protected static InputStream _getInputStreamOfUrl(String url, long sizeLimit, /*@Nullable*/ LongConsumer progress) throws IOException {
        if (url.contains(" ")) {
            throw new IOException("URL can't contains spaces!");
        }
        InputValidator.isUrlValid(url);


        if (url.startsWith("file_debug_only://")) {
            if (!isFileDebugSchemeAllowed()) {
                throw new RuntimeException("Not allowed scheme.");
            }

            final File gameDir = DynamicPackMod.getGameDir();
            File file = new File(gameDir, url.replace("file_debug_only://", ""));
            if (progress != null){
                progress.accept(file.length());
            }
            return new FileInputStream(file);


        } else if (url.startsWith("http://")) {
            if (!isHTTPTrafficAllowed()) {
                throw new RuntimeException("HTTP (not secure) not allowed scheme.");
            }

            throwIsUrlNotTrust(url);
            return __unsafeInputStreamFromUrl(url, sizeLimit, progress);

        } else if (url.startsWith("https://")) {
            throwIsUrlNotTrust(url);
            return __unsafeInputStreamFromUrl(url, sizeLimit, progress);

        } else {
            throw new RuntimeException("Unsupported scheme for url " + url);
        }
    }

    /**
     * # Do not use!
     * This method return InputStream of url WITHOUT any checks, except sizeLimit
     */
    private static InputStream __unsafeInputStreamFromUrl(String url, long sizeLimit, /*@Nullable*/ LongConsumer progress) throws IOException {
        // +-approximate amount spent on this request
        long size = SharedConstrains.HTTP_MINIMAL_HEADER_SIZE + url.length();

        // wrapped by NetworkStat for speedtest works
        return NetworkStat.runNetworkTask(size, () -> {
            URL urlObj = new URL(url);
            URLConnection connection = urlObj.openConnection();
            long length = connection.getContentLengthLong();
            if (length > sizeLimit) {
                throw new RuntimeException("File size file exceeds limit " + length + "bytes > " + sizeLimit + "bytes. url=" + url);
            }
            if (progress != null){
                progress.accept(length);
                progress.accept(0);
            }
            return connection.getInputStream();
        });
    }

    protected static String _parseTextFromStream(InputStream stream, /* Nullable */ LongConsumer progress) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] dataBuffer = new byte[SharedConstrains.URLS_BUFFER_SIZE];
        int bytesRead;
        long total = 0;
        while (true) {
            long startTime = System.currentTimeMillis();
            if ((bytesRead = stream.read(dataBuffer, 0, SharedConstrains.URLS_BUFFER_SIZE)) == -1) {
                break;
            }

            byteArrayOutputStream.write(dataBuffer, 0, bytesRead);
            total += bytesRead;

            if (progress != null) {
                progress.accept(total);
            }

            SharedConstrains.debugNetwork(bytesRead, total);
            NetworkStat.addLap(System.currentTimeMillis() - startTime, bytesRead);
        }
        String s = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        byteArrayOutputStream.close();
        stream.close();
        return s;
    }

    /**
     * Transfer streams and close all
     */
    private static void _transferStreams(InputStream inputStream, OutputStream outputStream, /*@Nullable*/ LongConsumer progress) throws IOException {
        boolean isNetwork = !(inputStream instanceof ByteArrayInputStream); // may other check?, but only for debug...

        BufferedInputStream in = new BufferedInputStream(inputStream);
        byte[] dataBuffer = new byte[SharedConstrains.URLS_BUFFER_SIZE];
        int bytesRead;
        long total = 0;
        while (true) {
            long startTime = System.currentTimeMillis();
            if ((bytesRead = in.read(dataBuffer, 0, SharedConstrains.URLS_BUFFER_SIZE)) == -1) {
                break;
            }
            outputStream.write(dataBuffer, 0, bytesRead);
            total += bytesRead;
            if (progress != null) {
                progress.accept(total);
            }


            if (isNetwork) {
                SharedConstrains.debugNetwork(bytesRead, total);
                NetworkStat.addLap(System.currentTimeMillis() - startTime, bytesRead);
            }
        }

        outputStream.flush();
        outputStream.close();

        in.close();
        inputStream.close();
    }

    protected static void _transferStreamsWithHash(String hash, InputStream inputStream, OutputStream outputStream, LongConsumer progress) throws IOException {
        boolean isNetwork = !(inputStream instanceof ByteArrayInputStream); // may other check?, but only for debug...


        BufferedInputStream in = new BufferedInputStream(inputStream);
        ByteArrayOutputStream tempBufferOutputStream = new ByteArrayOutputStream();

        byte[] dataBuffer = new byte[SharedConstrains.URLS_BUFFER_SIZE];
        int bytesRead;
        long total = 0;
        while (true) {
            long startTime = System.currentTimeMillis();
            if ((bytesRead = in.read(dataBuffer, 0, SharedConstrains.URLS_BUFFER_SIZE)) == -1) {
                break;
            }
            tempBufferOutputStream.write(dataBuffer, 0, bytesRead);
            total += bytesRead;
            progress.accept(total);
            if (isNetwork) {
                SharedConstrains.debugNetwork(bytesRead, total);
                NetworkStat.addLap(System.currentTimeMillis() - startTime, bytesRead);
            }
        }
        in.close();

        byte[] tempBufferBytes = tempBufferOutputStream.toByteArray();

        String hashOfDownloaded = Hashes.sha1sum(tempBufferBytes);
        if (hashOfDownloaded.equals(hash)) {
            _transferStreams(new ByteArrayInputStream(tempBufferBytes), outputStream, null);
            return;
        }

        throw new SecurityException("Hash of pre-downloaded to buffer file not equal: expected: " + hash + "; actual: " + hashOfDownloaded);
    }

    private static void throwIsUrlNotTrust(String url) throws IOException {
        if (!SharedConstrains.isUrlHostTrusted(url)) {
            if (SharedConstrains.isBlockAllNotTrustedNetworks()) {
                throw new SecurityException("Url '"+url+"' host is not trusted!");
            }
        }
    }
}
