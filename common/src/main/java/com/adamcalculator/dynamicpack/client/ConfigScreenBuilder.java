package com.adamcalculator.dynamicpack.client;

import com.adamcalculator.dynamicpack.Config;
import com.adamcalculator.dynamicpack.DynamicPackMod;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.slider.IntegerSliderController;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConfigScreenBuilder {
    public static Screen create(Screen parent) {
        var config = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("DynamicPack"))
                .category(buildNetworkCategory())
                .category(buildDebugCategory())
                .save(() -> DynamicPackMod.getConfig().save())
                .build();


        return new YACLScreen(config, parent);
    }

    private static ConfigCategory buildDebugCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.nullToEmpty("Debug"))
                .group(OptionGroup.createBuilder()
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Log all files changes"))
                                .binding(Config.DEF.isLogAllFilesChanges(), () -> DynamicPackMod.getConfig().isLogAllFilesChanges(), newVal -> {
                                    DynamicPackMod.getConfig().setLogAllFilesChanges(newVal);
                                })
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())
                .build();
    }

    @NotNull
    private static ConfigCategory buildNetworkCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("dynamicpack.screen.config.category.network"))
                .tooltip(Component.translatable("dynamicpack.screen.config.category.network.tooltip"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("dynamicpack.screen.config.category.network.group.name"))
                        .description(OptionDescription.of(Component.translatable("dynamicpack.screen.config.category.network.group.description")))

                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("dynamicpack.screen.config.category.network.bufferSize.name"))
                                .description(OptionDescription.of(Component.translatable("dynamicpack.screen.config.category.network.bufferSize.description")))
                                .binding(Config.DEF.getNetworkBufferSize(), () -> DynamicPackMod.getConfig().getNetworkBufferSize(), newVal -> {
                                    DynamicPackMod.getConfig().setNetworkBufferSize(newVal);
                                })
                                .controller(integerOption -> IntegerSliderControllerBuilder.create(integerOption)
                                        .step(256)
                                        .range(256, 8192))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("dynamicpack.screen.config.category.network.multithread.threads.name"))
                                .description(OptionDescription.of(Component.translatable("dynamicpack.screen.config.category.network.multithread.threads.description")))
                                .binding(Config.DEF.getNetworkMultithreadDownloadThreads(), () -> DynamicPackMod.getConfig().getNetworkMultithreadDownloadThreads(), newVal -> {
                                    DynamicPackMod.getConfig().setNetworkMultithreadDownloadThreads(newVal);
                                })
                                .controller(integerOption -> IntegerSliderControllerBuilder.create(integerOption)
                                        .step(1)
                                        .formatValue(value -> {
                                            if (value >= 255) {
                                                return Component.literal("^_^ ").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GOLD).append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.RESET, ChatFormatting.BOLD, ChatFormatting.GOLD));
                                            }

                                            if (value > 80) {
                                                return Component.literal("X_X ").withStyle(ChatFormatting.DARK_RED).append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED));
                                            }

                                            if (value > 64) {
                                                return Component.literal("X_0 ").withStyle(ChatFormatting.DARK_RED).append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.BOLD));
                                            }

                                            if (value > 32) {
                                                return Component.literal("0_0 ").withStyle(ChatFormatting.RED).append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.BOLD));
                                            }

                                            if (value > 16) {
                                                return Component.literal("OwO ").withStyle(ChatFormatting.YELLOW).append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.BOLD));
                                            }
                                            return IntegerSliderController.DEFAULT_FORMATTER.apply(value);
                                        })
                                        .range(1, 255))
                                .build())

                        .build())
                .build();
    }
}
