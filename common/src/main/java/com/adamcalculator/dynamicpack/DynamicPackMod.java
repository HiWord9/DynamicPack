package com.adamcalculator.dynamicpack;

import com.adamcalculator.dynamicpack.client.GameStartSyncing;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.pack.Remote;
import com.adamcalculator.dynamicpack.util.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class DynamicPackMod {

	// singleton
	private static DynamicPackMod INSTANCE;


	private Loader loader = Loader.UNKNOWN;
	private File gameDir; // .minecraft
    private File configDir; // *gamedir*/config/dynamicpack
	private File configFile; // *configDir*/config.json
	private Config config;
	private PacksContainer packsContainer;
	private GameStartSyncing gameStartSyncing;

	protected DynamicPackMod() {
	}

	private boolean minecraftInitialized = false;
	protected static int manuallySyncThreadCounter = 0;


	public void init(File gameDir, Loader loader) {
		if (INSTANCE != null) {
			throw new RuntimeException("Already initialized!");
		}
		INSTANCE = this;
		this.gameDir = gameDir;
		this.loader = loader;
        // *gamedir*/resourcepacks
        File resourcePacks = new File(gameDir, "resourcepacks");
		resourcePacks.mkdirs();
		this.configDir = new File(gameDir, "config/dynamicpack");
		this.configDir.mkdirs();
		this.configFile = new File(configDir, "config.json");

		// load config before logic and after files paths sets
		config = Config.load();

		Remote.initRemoteTypes();
		Out.init(loader);
		Out.println("Mod version: " + SharedConstrains.VERSION_NAME + " build: " + SharedConstrains.VERSION_BUILD);
		this.packsContainer = new PacksContainer(resourcePacks);
		this.packsContainer.rescan();

		this.gameStartSyncing = new GameStartSyncing();
		if (Config.getInstance().isAutoUpdateAtLaunch()) {
			this.gameStartSyncing.start();
		}
	}

	// == ABSTRACT ==
	public abstract boolean isModExists(String id);

	/**
	 * Manually re-sync all supported packs
	 */
	public abstract void startManuallySync();

	public abstract void startManuallySync(DynamicResourcePack pack);

	public abstract void needResourcesReload();

	public abstract String getCurrentGameVersion();

	public abstract boolean checkResourcePackMetaValid(String s) throws Exception;
	// __ ABSTRACT END __


	/**
	 * Singleton
	 */
	public static DynamicPackMod getInstance() {
		return INSTANCE;
	}


	/**
	 * API FOR MODPACKERS and etc all-in-one packs
	 * @param host host to add.
	 * @param requester any object. It is recommended that .toString explicitly give out your name.
	 */
	@ApiStatus.AvailableSince("1.0.30")
	public static void addAllowedHosts(String host, Object requester) throws Exception {
		SharedConstrains.addAllowedHosts(host, requester);
	}
	
	public static boolean isNameIsDynamic(String name) {
		return getDynamicPackByMinecraftName(name) != null;
	}

	@Nullable
	public static DynamicResourcePack getDynamicPackByMinecraftName(String name) {
		for (DynamicResourcePack pack : getPacksContainer().getPacks()) {
			if (("file/" + pack.getName()).equals(name)) {
				return pack;
			}
		}
		return null;
	}

	/**
	 * Check is resourcepack active
	 */
	public static boolean isResourcePackActive(DynamicResourcePack pack) {
		List<String> lines;
		try {
			lines = Files.readAllLines(new File(getGameDir(), "options.txt").toPath(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			Out.println("options.txt not exists or failed to parse.. isResourcePackActive => false.");
			return false;
		}

        for (String readLine : lines) {
			if (readLine.startsWith("resourcePacks:")) {
				String name = "file/" + pack.getLocation().getName();
				if (readLine.contains(name)) {
					return true;
				}
			}
		}
		return false;
	}

	public void minecraftInitialized() {
		this.minecraftInitialized = true;
	}

	public boolean isMinecraftInitialized() {
		return minecraftInitialized;
	}

	// STATIC
	@NotNull
	public static File getGameDir() {
		return INSTANCE.gameDir;
	}

	@NotNull
	public static PacksContainer getPacksContainer() {
		return INSTANCE.packsContainer;
	}

	@NotNull
	public static Loader getLoader() {
		return INSTANCE.loader;
	}

	@NotNull
	public static GameStartSyncing getGameStartSyncing() {
		return INSTANCE.gameStartSyncing;
	}

	@NotNull
	public static File getConfigDir() {
		return INSTANCE.configDir;
	}

	@NotNull
	public static File getConfigFile() {
		return INSTANCE.configFile;
	}

	@NotNull
	public static Config getConfig() {
		return INSTANCE.config;
	}
}