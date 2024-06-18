package com.adamcalculator.dynamicpack;

import com.adamcalculator.dynamicpack.util.Out;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.util.internal.MathUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class SharedConstrains {


    public static final boolean DEBUG = true; // Don't forget to disable in release
    public static final long VERSION_BUILD = 32;
    public static final String VERSION_NAME_MOD = "1.0.32";
    public static final String VERSION_NAME_BRANCH = "mc1.20";
    public static final String VERSION_NAME =  VERSION_NAME_MOD + "+" + VERSION_NAME_BRANCH + (DEBUG ? "-debug" : "");
    public static final String MOD_ID = "dynamicpack";


    // NOTE: for increase contact to mod developer.
    public static final long DYNAMIC_PACK_HTTPS_FILE_SIZE_LIMIT = megabyte(8); // kb -> mb -> 5MB (for files in resourcepack)
    public static final long MODRINTH_HTTPS_FILE_SIZE_LIMIT = megabyte(1024); // 1 GB (for .zip files from modrinth)
    public static final long MOD_MODTINTH_API_LIMIT = megabyte(8); // 8 MB of api
    public static final long GZIP_LIMIT = megabyte(50); // 50 MB of .gz file
    public static final long MOD_FILES_LIMIT = megabyte(8);
    public static final String MODRINTH_URL = "https://modrinth.com/mod/dynamicpack";
    public static final long NETWORK_STAT_RESET_LIMIT = megabyte(3);

    // Settings
    public static final int MAX_ATTEMPTS_TO_DOWNLOAD_FILE = 3;

    public static final boolean USE_ZIP4J_FOR_UNZIP = false;
    public static int URLS_BUFFER_SIZE = 1024;

    // const
    public static final long HTTP_MINIMAL_HEADER_SIZE = 24;
    public static final String CLIENT_FILE = "dynamicmcpack.json";
    public static final String MINECRAFT_META = "pack.mcmeta";
    public static final String UNKNOWN_PACK_MCMETA = """
                {
                  "pack": {
                    "pack_format": 17,
                    "description": "Unknown DynamicPack resource-pack..."
                  }
                }
                """;

    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> ALLOWED_HOSTS = new HashSet<>();
    static {
        ALLOWED_HOSTS.add("modrinth.com");
        ALLOWED_HOSTS.add("github.com");
        ALLOWED_HOSTS.add("github.io");
        ALLOWED_HOSTS.add("githubusercontent.com"); // use GitHub pages instead of this
        if (isLocalHostAllowed()) {
            ALLOWED_HOSTS.add("localhost");
        }
    }

    /**
     * API FOR MODPACKERS etc all-in-one packs
     * @param host host to add.
     * @param requester any object. It is recommended that .toString explicitly give out your name.
     */
    protected static void addAllowedHosts(String host, Object requester) throws Exception {
        if (host == null || requester == null) {
            Out.securityWarning("Try to add allowed hosts is failed: null host or requester");
            throw new Exception("Try to add allowed hosts is failed: null host or requester");
        }

        Out.securityWarning("==== SECURITY WARNING ====");
        Out.securityWarning("# The DynamicPack mod limits the hosts it can interact with.");
        Out.securityWarning("# But a certain requester allowed the mod another host to interact with");
        Out.securityWarning("# ");
        Out.securityWarning("# Host: " + host);
        Out.securityWarning("# Requester: " + requester);
        Out.securityWarning("# StackTrace:");
        Out.securityStackTrace();
        Out.securityWarning("# ");
        Out.securityWarning("===========================");

        ALLOWED_HOSTS.add(host);
    }

    public static String getUrlHost(String url) throws URISyntaxException {
        URI uri = new URI(url);
        return uri.getHost();
    }

    public static boolean isUrlHostTrusted(String url) throws IOException {
        try {
            String host = getUrlHost(url);
            for (String allowedHost : ALLOWED_HOSTS) {
                if (host.equals(allowedHost)) {
                    return true;
                }
                if (host.endsWith("." + allowedHost)) {
                    return true;
                }
            }
            Out.warn("Check trusted(false): " + host);
            return false;

        } catch (Exception e) {
            throw new IOException("Error while check url for trust", e);
        }
    }

    public static long megabyte(long mb) {
        return 1024L * 1024L * mb;
    }

    public static String speedToString(long bytesPerSec) {
        if (bytesPerSec >= 1024 * 1024) {
            return (bytesPerSec / 1024 / 1024) + " MiB/s";

        } else if (bytesPerSec >= 1024) {
            return (bytesPerSec / 1024) + " KiB/s";

        } else  {
            return bytesPerSec + " B/s";
        }
    }

    public static String secondsToString(long s) {
        if (s > 3600) {
            return (s / (3600)) + "h";

        } else if (s > 60) {
            return (s / 60) + "m";

        } else {
            return s + "s";
        }
    }

    public static boolean isBlockAllNotTrustedNetworks() {
        return true;
    }

    // TRUE FOR ALL PUBLIC VERSION!!!!!!
    // false is equal not safe!1!!! RELEASE=true
    public static boolean isRelease() {
        return !DEBUG;
    }

    // localhost allowed RELEASE=false
    public static boolean isLocalHostAllowed() {
        return DEBUG;
    }

    // file_debug_only:// allowed RELEASE=false
    public static boolean isFileDebugSchemeAllowed() {
        return DEBUG;
    }

    // http:// allowed RELEASE=false
    public static boolean isHTTPTrafficAllowed() {
        return false;
    }


    public static void debugNetwork(int bytesRead, long total) {
        if (isRelease()) return;
        if (true) return;

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isDebugLogs() {
        return DEBUG;
    }

    public static boolean isDebugMessageOnWorldJoin() {
        return DEBUG;
    }
}
