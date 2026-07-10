package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import Grend.chatlogger.client.ModKeyBindings;

/**
 * Экран для переназначения клавиши открытия GUI
 */
public class KeyBindingScreen extends Screen {
    private final Screen parent;

    public KeyBindingScreen(Screen parent) {
        super(Text.literal("EvoChat - Назначение клавиши"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int buttonWidth = 250;
        int buttonHeight = 20;
        int startY = height / 3;

        addDrawableChild(ButtonWidget.builder(Text.literal("§aОткрыть настройки управления"), button -> {
                    MinecraftClient.getInstance().setScreen(
                        new net.minecraft.client.gui.screen.option.OptionsScreen(
                            this,
                            MinecraftClient.getInstance().options
                        )
                    );
                })
                .dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§7Назад"), button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX - buttonWidth / 2, startY + 60, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 20, Formatting.GREEN.getColorValue());

        // Текущая клавиша
        String keyName = ModKeyBindings.openGuiKey.getBoundKeyLocalizedText().getString();
        context.drawCenteredTextWithShadow(textRenderer, "§fТекущая клавиша: §b" + keyName, 
            width / 2, height / 3 - 50, Formatting.WHITE.getColorValue());

        // Инструкция
        context.drawCenteredTextWithShadow(textRenderer, "§7Нажмите \"Открыть настройки управления\"", 
            width / 2, height / 3 - 25, Formatting.GRAY.getColorValue());
        
        context.drawCenteredTextWithShadow(textRenderer, "§7→ Управление → найдите \"EvoChat\"", 
            width / 2, height / 3 + 25, Formatting.GRAY.getColorValue());
        
        context.drawCenteredTextWithShadow(textRenderer, "§7→ Нажмите на клавишу и назначьте новую", 
            width / 2, height / 3 + 45, Formatting.GRAY.getColorValue());
    }
}
