package com.adamcalculator.dynamicpack.client.mixin;

import com.adamcalculator.dynamicpack.util.PackUtil;
import net.minecraft.server.packs.FilePackResources;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.zip.ZipFile;

@Mixin(FilePackResources.class)
public class FilePackResourcesMixin {
    @Shadow @Final private File file;
    @Shadow private @Nullable ZipFile zipFile;

    private Object zipFile_getOrCreateZipFile_head;

    @Inject(at = @At("HEAD"), method = "getOrCreateZipFile")
    public void dynamicpack_head$getOrCreateZipFile(CallbackInfoReturnable<ZipFile> cir) {
        zipFile_getOrCreateZipFile_head = zipFile;
    }

    @Inject(at = @At("RETURN"), method = "getOrCreateZipFile")
    public void dynamicpack_return$getOrCreateZipFile(CallbackInfoReturnable<ZipFile> cir) {
        ZipFile ret = cir.getReturnValue();

        if (ret != null && zipFile != zipFile_getOrCreateZipFile_head) {
            PackUtil.addFileToOpened(file, (AutoCloseable) this);
        }
    }

    @Inject(at = @At("HEAD"), method = "close")
    public void dynamicpack$close(CallbackInfo ci) {
        if (zipFile != null) {
            PackUtil.markClosedFile(file);
        }
    }
}
