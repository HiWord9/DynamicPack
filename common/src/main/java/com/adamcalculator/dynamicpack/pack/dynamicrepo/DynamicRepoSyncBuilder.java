package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.InputValidator;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.sync.SyncProgress;
import com.adamcalculator.dynamicpack.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class DynamicRepoSyncBuilder implements SyncBuilder {
    public static int DOWNLOAD_THREADS_COUNT = 8;
    private static int executorCounter = 0;

    private final DynamicResourcePack pack;
    private final DynamicRepoRemote remote;

    private final Set<String> oldestFilesList = new HashSet<>();
    private boolean doNotDeleteOldestFiles = false;
    private final HashMap<String, DynamicFile> dynamicFiles = new HashMap<>();
    private boolean updateAvailable;
    private long updateSize;
    private long downloadedSize;
    private JsonObject repoJson; // remote repo json (dynamicmcpack.repo.json)

    private boolean isReloadRequired;
    private boolean interrupted;

    public DynamicRepoSyncBuilder(DynamicResourcePack pack, DynamicRepoRemote dynamicRepoRemote) {
        this.pack = pack;
        this.remote = dynamicRepoRemote;
    }


    @Override
    public void init(boolean ignoreCaches) throws Exception {
        // == about ignoreCaches ==
        // Triggers a full re-check dynamic_repo because user
        // may a change contents in ContentScreen.
        updateAvailable = ignoreCaches || remote.checkUpdateAvailable();


        if (updateAvailable) {
            String packUrlContent = Urls.parseTextContent(remote.getPackUrl(), SharedConstrains.MOD_FILES_LIMIT, new UrlsController() {
                @Override
                public boolean isInterrupted() {
                    return interrupted;
                }
            });

            repoJson = JsonUtils.fromString(packUrlContent);
            long formatVersion;
            if ((formatVersion = JsonUtils.getLong(repoJson, "formatVersion")) != 1) {
                throw new RuntimeException("Incompatible formatVersion: " + formatVersion);
            }

            long minBuildForWork;
            if ((minBuildForWork = JsonUtils.optLong(repoJson, "minimal_mod_build", SharedConstrains.VERSION_BUILD)) > SharedConstrains.VERSION_BUILD) {
                throw new RuntimeException("Incompatible DynamicPack Mod version for this pack: required minimal_mod_build=" + minBuildForWork + ", but currently mod build is " + SharedConstrains.VERSION_BUILD);
            }

            String remoteName = JsonUtils.getString(repoJson, "name");
            if (!InputValidator.isDynamicPackNameValid(remoteName)) {
                throw new RuntimeException("Remote name of pack not valid.");
            }

            // init oldestFilesList before init contents
            PackUtil.openPackFileSystem(pack.getLocation(), packFileSystem -> PathsUtil.walkScan(oldestFilesList, packFileSystem));

            try {
                // check
                checkContents(repoJson.getAsJsonArray("contents"));

                // notify DynamicPackRemote about actual repoJson
                remote.notifyNewRemoteJson(repoJson);

                // init all active contents
                for (JsonObject jsonContent : calcActiveContents()) {
                    processContentInit(jsonContent);
                }

            } catch (Exception e) {
                // save the resource pack from disintegration
                doNotDeleteOldestFiles = true;
                throw e;
            }
        }
    }

    public void checkContents(JsonArray contents) {
        Set<String> ids = new HashSet<>();

        for (JsonElement content : contents) {
            JsonObject object = content.getAsJsonObject();
            var id = JsonUtils.getString(object, "id");
            InputValidator.throwIsContentIdInvalid(id);

            if (!ids.contains(id)) {
                ids.add(id);
            } else {
                throw new RuntimeException("Duplicate content found! Invalid format. Id: " + id);
            }
        }
    }

    @Override
    public long getDownloadedSize() {
        return downloadedSize;
    }

    @Override
    public boolean doUpdate(SyncProgress progress) throws Exception {
        progress.setPhase("Opening a pack file-system");
        PackUtil.openPackFileSystem(pack.getLocation(), LockUtils.createFileFinalizer(pack.getLocation()), packFileSystem -> {
            internalProcessDynamicFiles(progress, packFileSystem);

            debug("DELETE LIST: " + oldestFilesList);
            if (!doNotDeleteOldestFiles && !interrupted) {
                progress.setPhase("Deleting unnecessary files");
                for (String s : oldestFilesList) {
                    final Path pathToFile = packFileSystem.resolve(s);
                    final String fileName = pathToFile.getFileName().toString();
                    if (fileName.equalsIgnoreCase(SharedConstrains.CLIENT_FILE)) {
                        continue;
                    }
                    progress.deleted(pathToFile);
                    PathsUtil.nioSmartDelete(pathToFile);
                    markReloadRequired(s);
                }
            }

            progress.setPhase("Updating metadata...");
            pack.getPackJson().getAsJsonObject("current").addProperty("build", JsonUtils.getLong(repoJson, "build"));
            pack.updateJsonLatestUpdate();
            pack.saveClientFile(packFileSystem);
        });
        progress.setPhase("Success");
        return isReloadRequired();
    }

    @Override
    public void interrupt() {
        this.interrupted = true;
    }

    @Override
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    @Override
    public long getUpdateSize() {
        return updateSize;
    }

    private void processContentInit(JsonObject jsonContentD1) throws Exception {
        // dynamicmcpack.repo.json["contents"][*jsonContent*]
        var id = JsonUtils.getString(jsonContentD1, "id");
        InputValidator.throwIsContentIdInvalid(id);
        var url = JsonUtils.getString(jsonContentD1, "url");
        var urlCompressed = JsonUtils.optString(jsonContentD1, "url_compressed", null);
        boolean compressSupported = urlCompressed != null;

        checkPathSafety(url);

        var hash = JsonUtils.getString(jsonContentD1, "hash");

        url = remote.getUrl() + "/" + url;
        if (compressSupported) {
            checkPathSafety(urlCompressed);
            urlCompressed = remote.getUrl() + "/" + urlCompressed;
        }

        String content = compressSupported ? Urls.parseTextGZippedContent(urlCompressed, SharedConstrains.GZIP_LIMIT, null) : Urls.parseTextContent(url, SharedConstrains.MOD_FILES_LIMIT, null);
        String receivedHash = Hashes.sha1sum(content.getBytes(StandardCharsets.UTF_8));
        if (!hash.equals(receivedHash)) {
            throw new SecurityException("Hash of content at " + url + " not verified. remote: " + hash + "; received: " + receivedHash);
        }

        // content.json
        JsonObject jsonContentD2 = JsonUtils.fromString(content);
        PackUtil.openPackFileSystem(remote.parent.getLocation(), LockUtils.createFileFinalizer(pack.getLocation()), (packFileSystem) -> {
            long formatVersion;
            if ((formatVersion = JsonUtils.getLong(jsonContentD2, "formatVersion")) != 1) {
                throw new RuntimeException("Incompatible formatVersion: " + formatVersion);
            }

            JsonObject c = jsonContentD2.getAsJsonObject("content");
            String par = JsonUtils.optString(c, "parent", "");
            String rem = JsonUtils.optString(c, "remote_parent", "");
            JsonObject files = c.getAsJsonObject("files");

            int processedFiles = 0;
            for (final String _relativePath : files.keySet()) {
                if (interrupted) {
                    return;
                }

                boolean pathValidated = false;
                try {
                    // string path *parent*/*key_name*
                    var path = getAndCheckPath(par, _relativePath);
                    InputValidator.throwIsPathInvalid(path);
                    pathValidated = true;

                    // nio.Path from path (location in pack)
                    var filePath = packFileSystem.resolve(path);

                    // block to remotely patch a client file dynamicmcpack.json
                    if (filePath.getFileName().toString().contains(SharedConstrains.CLIENT_FILE)) {
                        warn("File " + SharedConstrains.CLIENT_FILE + " can't be updated remotely!");
                        continue;
                    }

                    // full URL to file
                    var fileRemoteUrl = getUrlFromPathAndCheck(rem, path);

                    // JSON-entry of file {"hash": "*hash*", "size": 1234"}
                    JsonObject fileExtra = files.getAsJsonObject(_relativePath);
                    String fileHash = JsonUtils.getString(fileExtra, "hash");
                    int fileSize = JsonUtils.optInt(fileExtra, "size", Integer.MAX_VALUE);
                    if (!InputValidator.isHashValid(fileHash)) {
                        warn("Hash not valid for file Example: \"file/path\": {\"hash\": \"not valid here\"}" + path);
                        continue;
                    }

                    // the file continues to exist
                    oldestFilesList.remove(path);

                    boolean isNeedOverwrite = false;
                    if (Files.exists(filePath)) {
                        String localFileHash = Hashes.sha1sum(filePath);
                        if (!localFileHash.equals(fileHash)) {
                            isNeedOverwrite = true;
                        }
                    } else {
                        isNeedOverwrite = true;
                    }

                    if (dynamicFiles.containsKey(path)) {
                        warn("File duplicates in multiple content packs: " + path);
                        updateSize -= dynamicFiles.get(path).getSize();
                        isNeedOverwrite = true;
                    }

                    if (isNeedOverwrite) {
                        DynamicFile dynamicFile = new DynamicFile(fileRemoteUrl, path, fileSize, fileHash);
                        updateSize += fileSize;
                        dynamicFiles.put(path, dynamicFile);
                    }

                    processedFiles++;

                } catch (Exception e) {
                    String errorFileName = pathValidated ? _relativePath : "(failed to validate)";
                    error("Error while process file " + errorFileName + " in pack...", e);
                }
            }
            
            println("Total initialized files in content '" + id + "': " + processedFiles);
        });
    }

    private void internalProcessDynamicFiles(SyncProgress progress, Path packFileSystem) throws Exception {
        debug("internalProcessDynamicFiles begin");
        NetworkStat.speedMultiplier = DOWNLOAD_THREADS_COUNT;
        Path tempPath;
        if (pack.isZip()) {
            tempPath = new File(System.getProperty("java.io.tmpdir") + File.separator + SharedConstrains.TEMP_DIR_NAME, pack.getName()).toPath();
            if (!Files.exists(tempPath)) {
                PathsUtil.createDirsToFile(tempPath);
                Files.createDirectory(tempPath);
            }
        } else {
            tempPath = null;
        }


        ExecutorService executor = getExecutor();
        CompletableFuture.supplyAsync(dynamicFiles::values).thenCompose(dynamicFiles -> {
            List<CompletableFuture<DynamicFile>> downloadedFiles = dynamicFiles.stream()
                    .map(file -> {
                        var s = CompletableFuture.supplyAsync(() -> {
                            try {
                                if (interrupted) {
                                    throw new InterruptedException("Interrupted");
                                }

                                downloadFile((tempPath != null ? tempPath : packFileSystem), file, progress);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return file;

                        }, executor).exceptionally(th -> {
                            error("Error while download a file", th);
                            return null;
                        });

                        return s.thenApply(r -> file);
                    }).toList();

            CompletableFuture<Void> done = CompletableFuture.allOf(downloadedFiles.toArray(new CompletableFuture[0]));
            return done.thenApply(v -> downloadedFiles.stream().map(CompletableFuture::join).collect(Collectors.toList()));


        }).whenComplete((files, th) -> {
            if (interrupted) {
                return;
            }

            if (th == null) {
                for (DynamicFile file : files) {
                    if (file == null || file.getDownloadedPath() == null) {
                        warn("DynamicFile null or downloadPath null in whenComplete...");
                        continue;
                    }
                    markReloadRequired(file);

                    if (tempPath != null) {
                        try {
                            Path dest = packFileSystem.resolve(file.getPath());
                            Path source = file.getDownloadedPath();

                            PathsUtil.createDirsToFile(dest);

                            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                            PathsUtil.nioSmartDelete(source);

                        } catch (Exception e) {
                            error("Error while moving file " + file.getPath() + " from temp", e);
                        }
                    }
                }

            } else {
                throw new RuntimeException(th);
            }
        }).toCompletableFuture().join();
        NetworkStat.speedMultiplier = 1;
        debug("internalProcessDynamicFiles end");
    }

    private void downloadFile(Path rootPath, DynamicFile dynamicFile, SyncProgress progress) throws Exception {
        Path filePath = rootPath.resolve(dynamicFile.getPath());

        // block to remotely patch a client file dynamicmcpack.json
        if (filePath.getFileName().toString().contains(SharedConstrains.CLIENT_FILE)) {
            warn("File " + SharedConstrains.CLIENT_FILE + " can't be updated remotely!");
            return;
        }

        if (PathsUtil.isPathFileExists(filePath)) {
            if (Hashes.sha1sum(filePath).equalsIgnoreCase(dynamicFile.getHash())) {
                warn("File " + dynamicFile.getPath() + " not downloaded(shadow): already exists with equals hashes!");
                dynamicFile.setDownloadPath(filePath);
                downloadedSize += Files.size(filePath);
                return;
            }
        }

        PackUtil.downloadPackFile(dynamicFile.getUrl(), filePath, dynamicFile.getHash(), new UrlsController() {
            private long fileSize = 0;

            @Override
            public void onUpdate(UrlsController it) {
                progress.downloading(filePath.getFileName().toString(), it.getPercentage());
                downloadedSize -= fileSize;
                fileSize = it.getLatest();
                downloadedSize += fileSize;
            }

            @Override
            public boolean isInterrupted() {
                return interrupted;
            }
        });

        if (interrupted) {
            progress.setPhase("Interrupted");
            return;
        }
        dynamicFile.setDownloadPath(filePath);
        progress.setPhase("File " + dynamicFile.getPath() + " downloaded!");
    }

    private ExecutorService getExecutor() {
        return Executors.newFixedThreadPool(DOWNLOAD_THREADS_COUNT, new ThreadFactory() {
            int count = 1;
            final int executorNum = (executorCounter++);

            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                return new Thread(runnable, "DownloadWorker"+ executorNum +"-" + count++);
            }
        });
    }

    private String getUrlFromPathAndCheck(String remoteParent, String path) {
        checkPathSafety(remoteParent);

        if (remoteParent.isEmpty()) {
            return remote.getUrl() + "/" + path;
        }

        return remote.getUrl() + "/" + remoteParent + "/" + path;
    }

    public static String getAndCheckPath(String parent, String path) {
        checkPathSafety(path);
        checkPathSafety(parent);

        if (parent.isEmpty()) {
            return path;
        }
        return parent + "/" + path;
    }

    public static void checkPathSafety(String s) {
        if (s.contains("://") || s.contains("..") || s.contains("  ") || s.contains(".exe") || s.contains(":") || s.contains(".jar")) {
            throw new SecurityException("This path not safe: " + s);
        }
    }

    /**
     * @return active contents from repoJson
     */
    private List<JsonObject> calcActiveContents() {
        List<JsonObject> activeContents = new ArrayList<>();
        JsonArray contents = repoJson.getAsJsonArray("contents");
        int i = 0;
        while (i < contents.size()) {
            var content = contents.get(i).getAsJsonObject();
            var id = JsonUtils.getString(content, "id");
            InputValidator.throwIsContentIdInvalid(id);
            var defaultActive = JsonUtils.optBoolean(content, "default_active", true);
            var required = JsonUtils.optBoolean(content, "required", false);

            // is active in settings or required
            if (required || remote.getPreferences().isContentActive(id, defaultActive)) {
                activeContents.add(content);
            }
            i++;
        }
        return activeContents;
    }

    public boolean isReloadRequired() {
        return isReloadRequired;
    }

    private void markReloadRequired(Object object) {
        if (!isReloadRequired) {
            debug("Now reload is required because " + object);
        }
        this.isReloadRequired = true;
    }

    private void debug(String s) {
        pack.debug(s);
    }

    private void error(String s, Throwable e) {
        pack.error(s, e);
    }

    private void warn(String s) {
        pack.warn(s);
    }


    private void println(String s) {
        pack.println(s);
    }
}
