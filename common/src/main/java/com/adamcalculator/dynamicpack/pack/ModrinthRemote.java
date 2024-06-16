package com.adamcalculator.dynamicpack.pack;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.sync.SyncProgress;
import com.adamcalculator.dynamicpack.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Remote for remote.type = "modrinth"
 */
public class ModrinthRemote extends Remote {
    private DynamicResourcePack parent; // parent
    private JsonObject cachedCurrentJson; // root.current json
    private String projectId;
    private String gameVersion;
    private boolean usesCurrentGameVersion; // uses launched game ver
    private boolean noSpecifyGameVersion; // use latest-latest-latest modrinth ver without check game_version


    public ModrinthRemote() {
    }

    /**
     * Init this remote object and associate with pack
     * @param pack parent
     * @param remote root.remote
     */
    public void init(DynamicResourcePack pack, JsonObject remote) {
        this.parent = pack;
        this.cachedCurrentJson = pack.getCurrentJson();

        if (remote.has("project_id")) {
            this.projectId = JsonUtils.getString(remote, "project_id");
        } else {
            this.projectId = JsonUtils.getString(remote, "modrinth_project_id");
        }

        var ver = JsonUtils.optString(remote, "game_version", "no_specify");
        this.usesCurrentGameVersion = ver.equalsIgnoreCase("current");
        this.noSpecifyGameVersion = ver.equalsIgnoreCase("no_specify");
        this.gameVersion = usesCurrentGameVersion ? DynamicPackMod.getInstance().getCurrentGameVersion() : ver;
    }

    /**
     * @return SyncBuilder
     */
    @Override
    public SyncBuilder syncBuilder() {
        return new SyncBuilder() {
            private LatestModrinthVersion latest;
            private JsonObject latestJson;
            private long downloaded;

            @Override
            public void init(boolean ignoreCaches) throws Exception {
                latestJson = parseModrinthLatestVersionJson();
                latest = LatestModrinthVersion.ofJson(latestJson);
            }

            @Override
            public long getDownloadedSize() {
                return downloaded;
            }

            @Override
            public boolean isUpdateAvailable() {
                return _isUpdateAvailable(latestJson);
            }

            @Override
            public long getUpdateSize() {
                if (isUpdateAvailable()) {
                    return latest.size;
                }
                return 0;
            }

            @Override
            public boolean doUpdate(SyncProgress progress) throws Exception {
                if (!isUpdateAvailable()) {
                    Out.warn("Call doUpdate in modrinth-remote when update not available");
                    return false;
                }

                progress.setPhase("Downloading resourcepack from modrinth");
                String[] urlSplit = latest.url.split("/");
                String fileName = urlSplit[urlSplit.length - 1];
                File tempFile = null;
                int attempts = SharedConstrains.MAX_ATTEMPTS_TO_DOWNLOAD_FILE;
                while (attempts > 0) {
                    tempFile = Urls.downloadFileToTemp(latest.url, "dynamicpack_download", ".zip", SharedConstrains.MODRINTH_HTTPS_FILE_SIZE_LIMIT, new FileDownloadConsumer() {
                        @Override
                        public void onUpdate(FileDownloadConsumer it) {
                            float percentage = it.getPercentage();
                            progress.downloading(fileName, percentage);
                            downloaded = getLatest();
                        }
                    });

                    if (Hashes.sha1sum(tempFile).equals(latest.fileHash)) {
                        break;
                    }
                    progress.setPhase("Failed. Downloading again...");
                    attempts--;
                }
                if (attempts == 0) {
                    progress.setPhase("Fatal error.");
                    throw new RuntimeException("Failed to download correct file from modrinth.");
                }

                progress.setPhase("Updating metadata...");
                cachedCurrentJson.addProperty("version", latest.id);
                cachedCurrentJson.remove("version_number");
                parent.updateJsonLatestUpdate();

                // save client json
                PackUtil.openPackFileSystem(tempFile, packRootPath -> parent.saveClientFile(packRootPath));

                if (parent.isZip()) {
                    progress.setPhase("Move files...");
                    PathsUtil.moveFile(tempFile, parent.getLocation());

                } else {
                    progress.setPhase("Extracting files...");
                    PathsUtil.recursiveDeleteDirectory(parent.getLocation());
                    PathsUtil.unzip(tempFile, parent.getLocation());
                    PathsUtil.delete(tempFile.toPath());
                }

                // save client json
                progress.setPhase("Saving dynamicmcpack.json");
                parent.saveClientFile();
                progress.setPhase("Success");
                return true;
            }
        };
    }


    public String getCurrentUnique() {
        return JsonUtils.optString(cachedCurrentJson, "version", "");
    }

    public String getCurrentVersionNumber() {
        return JsonUtils.optString(cachedCurrentJson, "version_number", "");
    }

    public String getApiVersionsUrl() {
        return "https://api.modrinth.com/v2/project/" + projectId + "/version";
    }

    public String getProjectId() {
        return projectId;
    }

    public boolean isUsesCurrentGameVersion() {
        return usesCurrentGameVersion;
    }

    public JsonObject parseModrinthLatestVersionJson() throws IOException {
        String content = Urls.parseTextContent(getApiVersionsUrl(), SharedConstrains.MOD_MODTINTH_API_LIMIT);
        Out.println(content);
        JsonArray versions = JsonUtils.arrayFromString(content);
        for (JsonElement o : versions) {
            JsonObject version = (JsonObject) o;
            if (noSpecifyGameVersion) {
                return version;
            }

            JsonArray gameVersions = JsonUtils.getJsonArray(version, "game_versions");
            boolean supportGameVersion = false;
            for (JsonElement jsonGameVersion : gameVersions) {
                String gameVersion = jsonGameVersion.getAsString();
                if (this.gameVersion.equals(gameVersion)) {
                    supportGameVersion = true;
                    break;
                }
            }
            if (supportGameVersion) {
                return version;
            }
        }
        throw new TranslatableException("Could not find the latest version on modrinth with suitable parameters",
                "dynamicpack.exceptions.pack.remote.modrinth.not_found_latest_version");
    }

    @Override
    public boolean checkUpdateAvailable() throws IOException {
        JsonObject latest = parseModrinthLatestVersionJson();
        return _isUpdateAvailable(latest);
    }

    private boolean _isUpdateAvailable(JsonObject latest) {
        if (latest == null) {
            Out.warn("Latest version of " + parent.getLocation().getName() + " not available for this game_version");
            return false;
        }
        if (JsonUtils.optString(latest, "version_number", "").equals(getCurrentVersionNumber())) {
            Out.debug("Version number equal. Update not available");
            return false;
        }
        var id = JsonUtils.getString(latest, "id");
        Out.debug("Version remote.id="+id + "; current=" + getCurrentUnique());

        return !getCurrentUnique().equals(id);
    }


    /**
     * Data-object for version in Modrinth API. Uses only for latest version.
     */
    public static class LatestModrinthVersion {
        public final String id;
        public final String versionNumber;
        public final String url;
        public final String fileHash;
        public final int size;

        public LatestModrinthVersion(String id,
                                     String versionNumber,
                                     String url,
                                     String fileHash,
                                     int size) {
            this.id = id;
            this.versionNumber = versionNumber;
            this.url = url;
            this.fileHash = fileHash;
            this.size = size;
        }

        /**
         * Create object from version json segment
         * @param latest json
         */
        public static LatestModrinthVersion ofJson(JsonObject latest) {
            String latestId = JsonUtils.getString(latest, "id");
            String latestVersionNumber = JsonUtils.getString(latest, "version_number");

            JsonArray files = JsonUtils.getJsonArray(latest, "files");
            int i = 0;
            while (i < files.size()) {
                var file = (JsonObject) files.get(i);
                if (JsonUtils.getBoolean(file, "primary")) {
                    String url = JsonUtils.getString(file, "url");
                    int size = JsonUtils.getInt(file, "size");
                    String hash = JsonUtils.getString(file.getAsJsonObject("hashes"), "sha1");
                    return new LatestModrinthVersion(latestId, latestVersionNumber, url, hash, size);
                }
                i++;
            }
            throw new NoSuchElementException("File json-object with primary=true not found... Modrinth API???");
        }
    }
}
