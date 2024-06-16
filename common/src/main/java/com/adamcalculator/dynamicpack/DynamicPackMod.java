package com.adamcalculator.dynamicpack;

import com.adamcalculator.dynamicpack.client.GameStartSyncing;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.pack.Remote;
import com.adamcalculator.dynamicpack.util.*;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public abstract class DynamicPackMod {

	// singleton
	private static DynamicPackMod INSTANCE;


	private Loader loader = Loader.UNKNOWN;
	private File gameDir;
	private File resourcePacks;
	private File configDir;
	private File configFile;
	private Config config;
	private PacksContainer packsContainer;
	private GameStartSyncing gameStartSyncing;

	public DynamicPackMod() {
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
		this.resourcePacks = new File(gameDir, "resourcepacks");
		this.resourcePacks.mkdirs();
		this.configDir = new File(gameDir, "config/dynamicpack");
		this.configDir.mkdirs();
		this.configFile = new File(configDir, "config.json");

		// load config before logic and after files paths sets
		config = Config.load();

		Remote.initRemoteTypes();
		Out.init(loader);
		Out.println("Mod version: " + SharedConstrains.VERSION_NAME + " build: " + SharedConstrains.VERSION_BUILD);
		this.packsContainer = new PacksContainer();
		this.gameStartSyncing = new GameStartSyncing();
		this.gameStartSyncing.start();
	}

	public static DynamicPackMod getInstance() {
		return INSTANCE;
	}
	
	public void rescanPacks() {
		packsContainer.rescan(resourcePacks);
	}

	public abstract boolean isModExists(String id);

	/**
	 * Manually re-sync all supported packs
	 */
	public abstract void startManuallySync();

	public abstract void startManuallySync(DynamicResourcePack pack);

	public abstract void needResourcesReload();

	/**
	 * API FOR MODPACKERS and etc all-in-one packs
	 * @param host host to add.
	 * @param requester any object. It is recommended that .toString explicitly give out your name.
	 */
	@ApiStatus.AvailableSince("1.0.30")
	public static void addAllowedHosts(String host, Object requester) throws Exception {
		SharedConstrains.addAllowedHosts(host, requester);
	}
	
	public boolean isNameIsDynamic(String name) {
		return getDynamicPackByMinecraftName(name) != null;
	}

	public DynamicResourcePack getDynamicPackByMinecraftName(String name) {
		for (DynamicResourcePack pack : getPacks()) {
			if (("file/" + pack.getName()).equals(name)) {
				return pack;
			}
		}
		return null;
	}

	public boolean isResourcePackActive(DynamicResourcePack pack) throws IOException {
		List<String> lines;
		try {
			lines = Files.readAllLines(new File(getGameDir(), "options.txt").toPath(), StandardCharsets.UTF_8);
		} catch (FileNotFoundException | NoSuchFileException e) {
			Out.println("options.txt not exists. isResourcePackActive => false.");
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

	public static File getGameDir() {
		return INSTANCE.gameDir;
	}

	public static DynamicResourcePack[] getPacks() {
		return INSTANCE.packsContainer.getPacks();
	}

	public static PacksContainer getPacksContainer() {
		return INSTANCE.packsContainer;
	}

	public static Loader getLoader() {
		return INSTANCE.loader;
	}

	public void minecraftInitialized() {
		this.minecraftInitialized = true;
	}

	public boolean isMinecraftInitialized() {
		return minecraftInitialized;
	}

	public abstract String getCurrentGameVersion();

	public abstract boolean checkResourcePackMetaValid(String s) throws Exception;

	public Path getResourcePackDir() {
		return resourcePacks.toPath();
	}

	public void blockRescan(boolean b) {
		if (b) {
			packsContainer.lockRescan();
		} else {
			packsContainer.unlockRescan();
		}
	}

	public static GameStartSyncing getGameStartSyncing() {
		return INSTANCE.gameStartSyncing;
	}

	public static File getConfigDir() {
		return INSTANCE.configDir;
	}

	public static File getConfigFile() {
		return INSTANCE.configFile;
	}

	public static Config getConfig() {
		return INSTANCE.config;
	}
}