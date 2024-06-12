package com.adamcalculator.dynamicpack.client;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.status.StatusChecker;
import com.adamcalculator.dynamicpack.util.Out;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.metadata.pack.PackMetadataSectionSerializer;
import net.minecraft.util.GsonHelper;


/**
 * Base impl for DynamicPack mod (minecraft logic)
 * <pre>
 * == inheritance tree ==
 * DynamicPackMod - mod-related logic
 * | DynamicPackModBase - minecraft-related logic
 * . | fabric impl...
 * . | forge impl...
 * . | ...
 * </pre>
 */
public abstract class DynamicPackModBase extends DynamicPackMod {
    private SystemToast toast = null;
    private long toastUpdated = 0;

    public void setToastContent(Component title, Component text) {
        if (!isMinecraftInitialized()) {
            return;
        }

        if (toast == null || (System.currentTimeMillis() - toastUpdated > 1000*5)) {
            ToastComponent toastManager = Minecraft.getInstance().getToasts();
            toastManager.addToast(toast = new SystemToast(SystemToast.SystemToastIds.NARRATOR_TOGGLE, title, text));
        } else {
            toast.reset(title, text);
        }
        toastUpdated = System.currentTimeMillis();
    }

    private Component createDownloadComponent() {
        return Component.translatable("dynamicpack.status_checker.download")
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("dynamicpack.status_checker.download.hover",
                                Component.literal(SharedConstrains.MODRINTH_URL).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.AQUA))))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SharedConstrains.MODRINTH_URL))
                )
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
    }

    public void onWorldJoinForUpdateChecks(LocalPlayer player) {
        if (SharedConstrains.isDebugMessageOnWorldJoin()) {
            player.sendSystemMessage(Component.literal("Debug message on world join").withStyle(ChatFormatting.GREEN));
        }

        if (player == null) {
            Out.warn("player == null on world join");

        } else if (!StatusChecker.isSafe()) {
            player.sendSystemMessage(Component.translatable("dynamicpack.status_checker.not_safe", createDownloadComponent()));
            setToastContent(Component.translatable("dynamicpack.status_checker.not_safe.toast.title"),
                    Component.translatable("dynamicpack.status_checker.not_safe.toast.description"));

        } else if (!StatusChecker.isFormatActual()) {
            player.sendSystemMessage(Component.translatable("dynamicpack.status_checker.format_not_actual", createDownloadComponent()));

        } else if (StatusChecker.isModUpdateAvailable()) {
            Out.println("DynamicPack mod update available: " + SharedConstrains.MODRINTH_URL);

        } else if (!StatusChecker.isChecked()) {
            Out.warn("StatusChecker isChecked = false :(");
        } else {
            Out.println("Mod in actual state in current date!");
        }
    }

    @Override
    public void startManuallySync() {
        Thread thread = new Thread(() -> {
//            createSyncTask(true).run();
            Out.println("Haha debug!");
        });
        thread.setName("DynamicPack-ManuallySyncThread" + (DynamicPackMod.manuallySyncThreadCounter++));
        thread.start();
    }

    @Override
    public String getCurrentGameVersion() {
        SharedConstants.tryDetectVersion();
        return SharedConstants.getCurrentVersion().getId();
    }

    @Override
    public boolean checkResourcePackMetaValid(String s) {
        new PackMetadataSectionSerializer().fromJson(GsonHelper.parse(s).getAsJsonObject("pack"));
        return true;
    }

    @Override
    public void needResourcesReload() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            if (client.level == null) {
                client.execute(client::reloadResourcePacks);

            } else {
                setToastContent(Component.translatable("dynamicpack.toast.needReload"),
                        Component.translatable("dynamicpack.toast.needReload.description"));
            }
        }
    }
}
