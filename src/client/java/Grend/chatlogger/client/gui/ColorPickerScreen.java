package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import Grend.chatlogger.client.ClanHighlightConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран выбора цвета для подсветки кланов
 */
public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final String clanName;
    private final Runnable onColorSelected;

    // Предустановленные цвета
    private static final ColorPreset[] COLOR_PRESETS = {
        new ColorPreset("Белый", "#FFFFFF", 0xFFFFFFFF),
        new ColorPreset("Красный", "#FF0000", 0xFFFF0000),
        new ColorPreset("Зелёный", "#00FF00", 0xFF00FF00),
        new ColorPreset("Синий", "#0000FF", 0xFF0000FF),
        new ColorPreset("Жёлтый", "#FFFF00", 0xFFFFFF00),
        new ColorPreset("Голубой", "#00FFFF", 0xFF00FFFF),
        new ColorPreset("Фиолетовый", "#FF00FF", 0xFFFF00FF),
        new ColorPreset("Чёрный", "#000000", 0xFF000000),
        new ColorPreset("Серый", "#808080", 0xFF808080),
        new ColorPreset("Оранжевый", "#FFA500", 0xFFFFA500),
        new ColorPreset("Розовый", "#FFC0CB", 0xFFFFC0CB),
        new ColorPreset("Бирюзовый", "#008080", 0xFF008080),
        new ColorPreset("Золотой", "#FFD700", 0xFFFFD700),
        new ColorPreset("Серебряный", "#C0C0C0", 0xFFC0C0C0),
        new ColorPreset("Коричневый", "#A52A2A", 0xFFA52A2A),
        new ColorPreset("Лайм", "#00FF00", 0xFF00FF00),
        new ColorPreset("Тёмно-синий", "#000080", 0xFF000080),
        new ColorPreset("Тёмно-красный", "#8B0000", 0xFF8B0000),
        new ColorPreset("Тёмно-зелёный", "#006400", 0xFF006400),
        new ColorPreset("Светло-голубой", "#87CEEB", 0xFF87CEEB),
        new ColorPreset("Светло-зелёный", "#90EE90", 0xFF90EE90),
        new ColorPreset("Коралловый", "#FF7F50", 0xFFFF7F50),
        new ColorPreset("Индиго", "#4B0082", 0xFF4B0082),
        new ColorPreset("Мятный", "#98FF98", 0xFF98FF98),
    };

    private int scrollOffset = 0;
    private int maxVisibleColors = 6;
    private String selectedColorHex = "#FFFFFF";
    private String selectedColorName = "Белый";

    public ColorPickerScreen(Screen parent, String clanName, Runnable onColorSelected) {
        super(Text.literal("Выбор цвета"));
        this.parent = parent;
        this.clanName = clanName;
        this.onColorSelected = onColorSelected;
    }

    @Override
    protected void init() {
        super.init();

        maxVisibleColors = (height - 150) / 22;

        int centerX = width / 2;

        // Кнопка подтверждения
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§aПрименить цвет"),
                button -> applyColor())
                .dimensions(centerX - 100, height - 55, 200, 20)
                .build());

        // Кнопка "Назад"
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§7Назад"),
                button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX - 100, height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, "§aВыберите цвет для клана §b" + clanName,
                width / 2, 15, Formatting.GREEN.getColorValue());

        // Текущий выбранный цвет
        context.drawCenteredTextWithShadow(textRenderer, "§7Выбран: §f" + selectedColorName,
                width / 2, 28, Formatting.GRAY.getColorValue());

        // Список цветов
        int listX = width / 2 - 120;
        int listY = 40;
        int listWidth = 240;
        int listHeight = maxVisibleColors * 22;

        context.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2, 0x80000000);

        int visibleCount = Math.min(COLOR_PRESETS.length - scrollOffset, maxVisibleColors);

        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= COLOR_PRESETS.length) break;

            ColorPreset preset = COLOR_PRESETS[index];
            int y = listY + i * 22;

            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth &&
                               mouseY >= y && mouseY <= y + 20;
            int bgColor = isHovered ? 0x30FFFFFF : 0x10FFFFFF;
            context.fill(listX, y, listX + listWidth, y + 20, bgColor);

            // Квадрат с цветом
            int colorBoxX = listX + 5;
            context.fill(colorBoxX, y + 3, colorBoxX + 16, y + 18, preset.colorValue);
            context.fill(colorBoxX, y + 3, colorBoxX + 16, y + 18, 0xFF000000); // рамка

            // Название цвета
            context.drawText(textRenderer, preset.name, listX + 26, y + 7, 0xFFFFFF, true);

            // HEX значение
            context.drawText(textRenderer, "§7" + preset.hex, listX + 140, y + 7, 0xAAAAAA, true);

            // Индикатор выбора
            if (selectedColorHex.equals(preset.hex)) {
                context.drawText(textRenderer, "§a✓", listX + 215, y + 7, 0x00FF00, true);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int listX = width / 2 - 120;
        int listY = 40;
        int listWidth = 240;

        int visibleCount = Math.min(COLOR_PRESETS.length - scrollOffset, maxVisibleColors);

        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= COLOR_PRESETS.length) break;

            ColorPreset preset = COLOR_PRESETS[index];
            int y = listY + i * 22;

            if (mouseX >= listX && mouseX <= listX + listWidth &&
                mouseY >= y && mouseY <= y + 20) {
                selectedColorHex = preset.hex;
                selectedColorName = preset.name;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (COLOR_PRESETS.length > maxVisibleColors) {
            scrollOffset -= (int) verticalAmount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, COLOR_PRESETS.length - maxVisibleColors));
        }
        return true;
    }

    private void applyColor() {
        ClanHighlightConfig config = ClanHighlightConfig.getInstance();
        config.setClanColor(clanName, selectedColorHex);

        if (client != null && client.player != null) {
            client.player.sendMessage(
                Text.literal("§a[EvoChat] Цвет клана §b" + clanName + " §aустановлен: §b" + selectedColorName),
                false
            );
        }

        onColorSelected.run();
        MinecraftClient.getInstance().setScreen(parent);
    }

    // Класс для хранения пресета цвета
    private static class ColorPreset {
        final String name;
        final String hex;
        final int colorValue;

        ColorPreset(String name, String hex, int colorValue) {
            this.name = name;
            this.hex = hex;
            this.colorValue = colorValue;
        }
    }
}
