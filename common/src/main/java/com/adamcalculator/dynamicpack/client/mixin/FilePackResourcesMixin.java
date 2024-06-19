package com.adamcalculator.dynamicpack.client.mixin;

import com.adamcalculator.dynamicpack.client.FilePackResourcesAccessor;
import com.adamcalculator.dynamicpack.util.LockUtils;
import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.zip.ZipFile;

@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin implements FilePackResourcesAccessor {
    @Shadow @Final private File file;
    @Unique private boolean dynamicpack$opened;

    @Shadow public abstract void close();

    @Inject(at = @At("RETURN"), method = "getOrCreateZipFile")
    public void dynamicpack_return$getOrCreateZipFile(CallbackInfoReturnable<ZipFile> cir) {
        ZipFile ret = cir.getReturnValue();

        if (ret != null) {
            LockUtils.addFileToOpened(this);
        }
    }

    @Inject(at = @At("RETURN"), method = "close")
    public void dynamicpack_return$close(CallbackInfo ci) {
        dynamicpack$opened = false;
    }

    @Override
    public boolean dynamicpack$isClosed() {
        return dynamicpack$opened;
    }

    @Override
    public File dynamicpack$getFile() {
        return file;
    }

    @Override
    public void dynamicpack$close() {
        close();
    }
}
