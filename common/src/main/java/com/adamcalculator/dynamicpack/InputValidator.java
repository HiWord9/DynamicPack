package com.adamcalculator.dynamicpack;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern CONTENT_ID_PATTERN = Pattern.compile("^[a-z0-9_:-]{2,128}$");
    private static final Pattern PATH_PATTERN = Pattern.compile("^[A-Za-z0-9_./() +-]{0,255}$");
    private static final Pattern URL_PATTERN = Pattern.compile("[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)");

    public static boolean isContentIdValid(String input) {
        if (input == null) {
            return false;
        }
        Matcher matcher = CONTENT_ID_PATTERN.matcher(input);
        return matcher.matches();
    }

    public static void checkContentIdValid(String id) {
        if (!isContentIdValid(id)) {
            throw new RuntimeException("Id of content is not valid: " + safeOutput(id));
        }
    }

    public static boolean isContentNameValid(String input) {
        if (input == null) {
            return false;
        }

        return input.trim().length() < 64 && !input.trim().isEmpty() && !input.contains("\n") && !input.contains("\r") && !input.contains("\b");
    }

    public static boolean isPackNameValid(String input) {
        if (input == null) {
            return false;
        }

        return input.trim().length() < 64 && !input.trim().isEmpty() && !input.contains("\n") && !input.contains("\r") && !input.contains("\b");
    }

    public static boolean isHashValid(String hash) {
        return hash != null && hash.length() == 40 && !hash.contains(" ");
    }

    public static void throwIsPathInvalid(String par) {
        if (par == null) {
            throw new SecurityException("null", new NullPointerException("path to valid is null"));
        }

        String trim = par.trim();
        if (trim.length() < 2 || !PATH_PATTERN.matcher(par).matches()) {
            throw new SecurityException("Not valid path: " + new String(par.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII));
        }
    }

    public static void throwIsUrlInvalid(String url) {
        if (url == null) {
            throw new SecurityException("null", new NullPointerException("url to valid is null"));
        }
        if (!isUrlValid(url)) {
            throw new SecurityException("Not valid url: " + safeOutput(url));
        }
    }

    public static boolean isUrlValid(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    private static String safeOutput(String s) {
        if (s.length() >= 100) {
            s = s.substring(0, 100);
        }
        return new String(s.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII);
    }
}
