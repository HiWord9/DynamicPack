package com.adamcalculator.dynamicpack.client.mixin;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.client.GameStartSyncing;
import com.adamcalculator.dynamicpack.util.Out;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.Minecraft.ON_OSX;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    /**
     * For freeze resource pack loading (only in game starting) until DynamicPack update it
     */
    @Inject(at = @At("HEAD"), method = "reload")
    private void dynamicpack$reload(CallbackInfo ci) {
        // do nothing if minecraft initialized
        if (DynamicPackMod.getInstance().isMinecraftInitialized()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        GameStartSyncing syncing = DynamicPackMod.getGameStartSyncing();
        if (!syncing.isLockSupported()) {
            return;
        }

        if (syncing.lockResourcesLoading) {
            syncing.startGameLocking();
            while (!client.getWindow().shouldClose() && syncing.lockResourcesLoading) {
                if (!syncing.lockedTick()) {
                    break;
                }

                try {
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.clearColor(0.074f, 0.04f, (float) (0.24f + (Math.sin(System.currentTimeMillis() / 300f) / 2)), 1f);
                    RenderSystem.clear(16640, ON_OSX);
                    RenderSystem.flipFrame(client.getWindow().getWindow());
                } catch (Exception e) {
                    Out.error("Error while manipulations with OpenGL", e);
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
            syncing.endGameLocking();
        }
    }
}
