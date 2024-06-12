package com.adamcalculator.dynamicpack.client.fabric;

import com.adamcalculator.dynamicpack.client.ConfigScreenBuilder;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory<Screen>) ConfigScreenBuilder::create;
    }

}
