package com.adamcalculator.dynamicpack;

import com.adamcalculator.dynamicpack.pack.Pack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Date;

public class DynamicPackScreen extends Screen {
    private final Screen parent;
    private final Pack pack;
    private final Text screenDescText;

    public DynamicPackScreen(Screen parent, Pack pack) {
        super(Text.literal(pack.getName()).formatted(Formatting.BOLD));
        this.pack = pack;
        this.client = MinecraftClient.getInstance();
        this.parent = parent;
        this.screenDescText = Text.translatable("dynamicpack.screen.pack.description");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawTextWithShadow(this.textRenderer, this.title, 20, 8, 16777215);
        context.drawTextWithShadow(this.textRenderer, screenDescText, 20, 20, 16777215);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("dynamicpack.screen.pack.remote_type", pack.getRemoteType()), 20, 36, 16777215);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("dynamicpack.screen.pack.latestUpdated", pack.getLatestUpdated() < 0 ? "-" : new Date(pack.getLatestUpdated() * 1000)), 20, 52, 16777215);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        addDrawableChild(Compat.createButton(
                Text.of("Manually sync"),
                        () -> {
                            DynamicPackModBase.INSTANCE.startManuallySync();
                            close();
                        },
                100, 20, width - 120, 10
        ));

        addDrawableChild(Compat.createButton(ScreenTexts.DONE, this::close, 150, 20, this.width / 2 + 4, this.height - 48));
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
