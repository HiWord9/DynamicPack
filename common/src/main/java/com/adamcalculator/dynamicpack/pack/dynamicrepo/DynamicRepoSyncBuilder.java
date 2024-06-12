package com.adamcalculator.dynamicpack.pack.dynamicrepo;

import com.adamcalculator.dynamicpack.InputValidator;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.util.PackUtil;
import com.adamcalculator.dynamicpack.sync.SyncProgress;
import com.adamcalculator.dynamicpack.util.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class DynamicRepoSyncBuilder extends SyncBuilder {
    private static ExecutorService executor;

    private final DynamicResourcePack pack;
    private final DynamicRepoRemote remote;

    private final Set<String> oldestFilesList = new HashSet<>();
    private final boolean doNotDeleteOldestFiles = false;
    private final HashMap<String, DynamicFile> dynamicFiles = new HashMap<>();
    private boolean updateAvailable;
    private long updateSize;
    private long downloadedSize;
    private JSONObject repoJson; // remote repo json (dynamicmcpack.repo.json)

    private boolean isReloadRequired;

    public DynamicRepoSyncBuilder(DynamicResourcePack pack, DynamicRepoRemote dynamicRepoRemote) {
        this.pack = pack;
        this.remote = dynamicRepoRemote;
    }


    @Override
    public void init() throws Exception {
        updateAvailable = remote.checkUpdateAvailable();
        if (updateAvailable) {
            String packUrlContent = Urls.parseTextContent(remote.getPackUrl(), SharedConstrains.MOD_FILES_LIMIT, null);
            repoJson = new JSONObject(packUrlContent);
            long formatVersion;
            if ((formatVersion = repoJson.getLong("formatVersion")) != 1) {
                throw new RuntimeException("Incompatible formatVersion: " + formatVersion);
            }

            long minBuildForWork;
            if ((minBuildForWork = repoJson.optLong("minimal_mod_build", SharedConstrains.VERSION_BUILD)) > SharedConstrains.VERSION_BUILD) {
                throw new RuntimeException("Incompatible DynamicPack Mod version for this pack: required minimal_mod_build=" + minBuildForWork + ", but currently mod build is " + SharedConstrains.VERSION_BUILD);
            }

            String remoteName = repoJson.getString("name");
            if (!InputValidator.isDynamicPackNameValid(remoteName)) {
                throw new RuntimeException("Remote name of pack not valid.");
            }

            for (JSONObject jsonContent : calcActiveContents()) {
                processContentInit(jsonContent);
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
        PackUtil.openPackFileSystem(pack.getLocation(), packFileSystem -> {
            PathsUtil.walkScan(oldestFilesList, packFileSystem);


            internalProcessDynamicFiles(progress, packFileSystem);

            if (!doNotDeleteOldestFiles) {
                progress.setPhase("Deleting unnecessary files");
                for (String s : oldestFilesList) {
                    final Path pathToFile = packFileSystem.resolve(s);
                    final String fileName = pathToFile.getFileName().toString();
                    if (fileName.equalsIgnoreCase(SharedConstrains.CLIENT_FILE)) {
                        continue;
                    }

                    progress.deleted(packFileSystem);
                    PathsUtil.nioSmartDelete(pathToFile);
                    markReloadRequired();
                }
            }

            progress.setPhase("Updating metadata...");
            remote.updateCurrentKnownContents(repoJson.getJSONArray("contents"));
            remote.parent.getPackJson().getJSONObject("current").put("build", repoJson.getLong("build"));
            remote.parent.updateJsonLatestUpdate();

            PathsUtil.nioWriteText(packFileSystem.resolve(SharedConstrains.CLIENT_FILE), pack.getPackJson().toString(SharedConstrains.JSON_INDENTS));

        });
        progress.setPhase("Success");
        return isReloadRequired();
    }

    @Override
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    @Override
    public long getUpdateSize() {
        return updateSize;
    }

    private void processContentInit(JSONObject jsonContentD1) throws Exception {
        // dynamicmcpack.repo.json["contents"][*jsonContent*]
        var id = jsonContentD1.getString("id");
        InputValidator.throwIsContentIdInvalid(id);
        var url = jsonContentD1.getString("url");
        var urlCompressed = jsonContentD1.optString("url_compressed", null);
        boolean compressSupported = urlCompressed != null;

        checkPathSafety(url);

        var hash = jsonContentD1.getString("hash");
        var localHash = remote.getCurrentPackContentHash(id);
        Out.debug("[DynamicRepoSyncBuilder] id=" + id + " localHash == hash: " + Objects.equals(localHash, hash));

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
        JSONObject jsonContentD2 = new JSONObject(content);
        PackUtil.openPackFileSystem(remote.parent.getLocation(), (packFileSystem) -> {
            long formatVersion;
            if ((formatVersion = jsonContentD2.getLong("formatVersion")) != 1) {
                throw new RuntimeException("Incompatible formatVersion: " + formatVersion);
            }

            JSONObject c = jsonContentD2.getJSONObject("content");
            String par = c.optString("parent", "");
            String rem = c.optString("remote_parent", "");
            JSONObject files = c.getJSONObject("files");

            int processedFiles = 0;
            for (final String _relativePath : files.keySet()) {
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
                        Out.warn("File " + SharedConstrains.CLIENT_FILE + " in pack " + pack.getName() + " can't be updated remotely!");
                        continue;
                    }

                    // full URL to file
                    var fileRemoteUrl = getUrlFromPathAndCheck(rem, path);

                    // JSON-entry of file {"hash": "*hash*", "size": 1234"}
                    JSONObject fileExtra = files.getJSONObject(_relativePath);
                    String fileHash = fileExtra.getString("hash");
                    int fileSize = fileExtra.optInt("size", Integer.MAX_VALUE);
                    if (!InputValidator.isHashValid(fileHash)) {
                        Out.warn("Hash not valid for file Example: \"file/path\": {\"hash\": \"not valid here\"}" + path);
                        continue;
                    }

                    boolean isNeedOverwrite = false;
                    if (Files.exists(filePath)) {
                        String localFileHash = Hashes.sha1sum(filePath);
                        if (!localFileHash.equals(fileHash)) {
                            isNeedOverwrite = true;
                        }
                    } else {
                        isNeedOverwrite = true;
                    }

                    if (isNeedOverwrite) {
                        if (dynamicFiles.containsKey(path)) {
                            Out.warn("File from pack " + pack.getName() + " duplicates in multiple content packs: " + path);
                            updateSize -= dynamicFiles.get(path).getSize();
                        }
                        DynamicFile dynamicFile = new DynamicFile(fileRemoteUrl, path, fileSize, fileHash);
                        updateSize += fileSize;
                        dynamicFiles.put(path, dynamicFile);
                    }

                    processedFiles++;

                } catch (Exception e) {
                    String errorFileName = pathValidated ? _relativePath : "(failed to validate)";
                    Out.error("Error while process file " + errorFileName + " in pack...", e);
                }
            }
        });
    }

    private void internalProcessDynamicFiles(SyncProgress progress, Path packFileSystem) {
        NetworkStat.speedMultiplier = 10;
        CompletableFuture.supplyAsync(dynamicFiles::values).thenCompose(dynamicFiles -> {
            List<CompletableFuture<DynamicFile>> downloadedFiles = dynamicFiles.stream()
                    .map(file -> {
                        var s = CompletableFuture.supplyAsync(() -> {
                            try {
                                downloadFile(packFileSystem, file, progress);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return file;
                        }, getExecutor()).exceptionally(th -> {
                            Out.error("Error while download a file", th);
                            return null;
                        });

                        return s.thenApply(r -> file);
                    }).toList();

            CompletableFuture<Void> done = CompletableFuture.allOf(downloadedFiles.toArray(new CompletableFuture[0]));
            return done.thenApply(v -> downloadedFiles.stream().map(CompletableFuture::join).collect(Collectors.toList()));


        }).whenComplete((files, th) -> {
            if (th == null) {
                for (DynamicFile file : files) {
                    oldestFilesList.remove(file.getPath());
                }

            } else {
                throw new RuntimeException(th);
            }
        }).toCompletableFuture().join();
        NetworkStat.speedMultiplier = 1;
    }

    private void downloadFile(Path rootPath, DynamicFile dynamicFile, SyncProgress progress) throws Exception {
        Path filePath = rootPath.resolve(dynamicFile.getPath());

        // block to remotely patch a client file dynamicmcpack.json
        if (filePath.getFileName().toString().contains(SharedConstrains.CLIENT_FILE)) {
            Out.warn("File " + SharedConstrains.CLIENT_FILE + " in pack " + pack.getName() + " can't be updated remotely!");
            return;
        }

        PackUtil.downloadPackFile(dynamicFile.getUrl(), filePath, dynamicFile.getHash(), new FileDownloadConsumer() {
            private long fileSize = 0;

            @Override
            public void onUpdate(FileDownloadConsumer it) {
                progress.downloading(filePath.getFileName().toString(), it.getPercentage());
                downloadedSize -= fileSize;
                fileSize = it.getLatest();
                downloadedSize += fileSize;
            }
        });
        dynamicFile.setTempPath(filePath);

        markReloadRequired();
    }

    private ExecutorService getExecutor() {
        if (executor != null) {
            return executor;
        }

        return executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
            int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                return new Thread(runnable, "DownloadWorker-" + count++);
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
    private List<JSONObject> calcActiveContents() {
        List<JSONObject> activeContents = new ArrayList<>();
        JSONArray contents = repoJson.getJSONArray("contents");
        int i = 0;
        while (i < contents.length()) {
            var content = contents.getJSONObject(i);
            var id = content.getString("id");
            InputValidator.throwIsContentIdInvalid(id);
            var defaultActive = content.optBoolean("default_active", true);

            // is active in settings or required
            if (remote.isContentActive(id, defaultActive) || content.optBoolean("required", false)) {
                activeContents.add(content);
            }
            i++;
        }
        return activeContents;
    }

    public boolean isReloadRequired() {
        return isReloadRequired;
    }

    private void markReloadRequired() {
        if (!isReloadRequired) {
            Out.debug("Now reload is required in " + this);
        }
        this.isReloadRequired = true;
    }
}
