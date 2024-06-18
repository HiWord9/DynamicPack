package com.adamcalculator.dynamicpack;

import com.adamcalculator.dynamicpack.util.Out;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates for [user/resourecepack-creator] input values
 */
public class InputValidator {
    private static final Pattern CONTENT_ID_PATTERN = Pattern.compile("^[a-z0-9_:-]{2,128}$");
    private static final Pattern PATH_PATTERN = Pattern.compile("^[A-Za-z0-9_./() +-]{0,255}$");
    private static final Pattern URL_PATTERN = Pattern.compile("[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)");


    /**
     * is dynamic content id valid
     * @param contentId content id
     * @return is valid
     */
    public static boolean isDynamicContentIdValid(String contentId) {
        if (contentId == null) {
            return false;
        }
        Matcher matcher = CONTENT_ID_PATTERN.matcher(contentId);
        return matcher.matches();
    }

    /**
     * Throw is content id invalid
     * @param contentId id
     */
    public static void throwIsContentIdInvalid(String contentId) {
        if (!isDynamicContentIdValid(contentId)) {
            throw new RuntimeException("Id of content is not valid: " + safeOutput(contentId));
        }
    }

    /**
     * Is dynamic_repo content name valid
     * @param contentName content name
     * @return is valid
     */
    public static boolean isDynamicContentNameValid(String contentName) {
        if (contentName == null) {
            return false;
        }

        return contentName.trim().length() < 64 && !contentName.trim().isEmpty() && !contentName.contains("\n") && !contentName.contains("\r") && !contentName.contains("\b");
    }

    /**
     * Check dynamic_repo name valid
     * @param name name
     * @return is valid
     */
    public static boolean isDynamicPackNameValid(String name) {
        if (name == null) {
            return false;
        }

        return name.trim().length() < 64 && !name.trim().isEmpty() && !name.contains("\n") && !name.contains("\r") && !name.contains("\b");
    }


    /**
     * Throw is local path not valid
     * @param path path ex. dir/dir/dir/dir/dir/file.txt or dir/dir/dir
     */
    public static void throwIsPathInvalid(String path) {
        if (path == null) {
            throw new SecurityException("Null path", new NullPointerException("path to valid is null"));
        }

        String trim = path.trim();
        if (trim.length() < 2 || !PATH_PATTERN.matcher(path).matches()) {
            throw new SecurityException("Not valid path: " + new String(path.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII));
        }
    }

    /**
     * @param url url string e.g. https://google.com/dir/dir/dir/folder.txt?username=HelloWorld
     * @return is url string is valid
     */
    public static boolean isUrlValid(String url) {
        try {
            if (SharedConstrains.isLocalHostAllowed() && SharedConstrains.getUrlHost(url).equals("localhost")) {
                Out.warn("isUrlValid return true for localhost! It behavior only when isLocalHostAllowed()=true");
                return true;
            }
        } catch (URISyntaxException ignored) {}

        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * Throw is url not valid
     * @param url url
     */
    public static void throwIsUrlInvalid(String url) {
        if (url == null) {
            throw new SecurityException("null", new NullPointerException("url to valid is null"));
        }
        if (!isUrlValid(url)) {
            throw new SecurityException("Not valid url: " + safeOutput(url));
        }
    }

    /**
     * @param hash sha1 string
     * @return is sha1 string valid
     */
    public static boolean isHashValid(String hash) {
        return hash != null && hash.length() == 40 && !hash.contains(" ");
    }

    private static String safeOutput(String s) {
        if (s.length() >= 100) {
            s = s.substring(0, 100);
        }
        return new String(s.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII);
    }
}
