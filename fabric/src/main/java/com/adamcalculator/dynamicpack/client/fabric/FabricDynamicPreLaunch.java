package com.adamcalculator.dynamicpack.client.fabric;

import com.adamcalculator.dynamicpack.client.DynamicPackModBase;
import com.adamcalculator.dynamicpack.util.Loader;
import com.adamcalculator.dynamicpack.util.Out;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Fabric impl for DynamicPack mod
 */
public class FabricDynamicPreLaunch extends DynamicPackModBase implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        Out.println("DynamicPack loaded. Hello fabric world!");

        var gameDir = FabricLoader.getInstance().getGameDir().toFile();
        init(gameDir, Loader.FABRIC);

        ClientPlayConnectionEvents.JOIN.register(this::onJoin);
    }

    private void onJoin(ClientPacketListener clientPacketListener, PacketSender packetSender, Minecraft minecraft) {
        onWorldJoinForUpdateChecks(Minecraft.getInstance().player);
    }
}
