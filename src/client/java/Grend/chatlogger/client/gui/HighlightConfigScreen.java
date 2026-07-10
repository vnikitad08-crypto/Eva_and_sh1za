package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import Grend.chatlogger.client.ClanHighlightConfig;
import Grend.chatlogger.data.DataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Экран настроек подсветки кланов
 */
public class HighlightConfigScreen extends Screen {
    private final Screen parent;
    private ClanHighlightConfig config;
    private int scrollOffset = 0;
    private int maxVisibleClans = 5;
    private TextFieldWidget clanField;
    private TextFieldWidget colorField;

    public HighlightConfigScreen(Screen parent) {
        super(Text.literal("EvoChat - Подсветка кланов"));
        this.parent = parent;
        this.config = ClanHighlightConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        maxVisibleClans = (height - 140) / 30;

        int centerX = width / 2;

        // Поля ввода - сверху
        clanField = new TextFieldWidget(textRenderer, centerX - 150, 10, 120, 18, Text.literal(""));
        clanField.setMaxLength(32);
        clanField.setPlaceholder(Text.literal("Клан"));
        addDrawableChild(clanField);

        colorField = new TextFieldWidget(textRenderer, centerX - 25, 10, 100, 18, Text.literal(""));
        colorField.setMaxLength(32);
        colorField.setPlaceholder(Text.literal("Цвет (red/#FF0000)"));
        addDrawableChild(colorField);

        addDrawableChild(ButtonWidget.builder(Text.literal("§aУстановить"), button -> setColor())
                .dimensions(centerX + 80, 10, 70, 18)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§bВыбрать цвет"), button -> openColorPicker())
                .dimensions(centerX - 150, 32, 120, 18)
                .build());

        boolean enabled = config.isHighlightEnabled();
        addDrawableChild(ButtonWidget.builder(Text.literal(enabled ? "§eВключено" : "§7Выключено"), button -> toggleHighlight())
                .dimensions(centerX - 100, height - 55, 120, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§7Назад"), button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX + 25, height - 55, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 58, Formatting.GREEN.getColorValue());

        // Статус
        String status = config.isHighlightEnabled() ? "§aВключена" : "§7Выключена";
        context.drawCenteredTextWithShadow(textRenderer, "Состояние: " + status, width / 2, 70, Formatting.GRAY.getColorValue());

        // Список кланов с цветами
        int listX = width / 2 - 150;
        int listY = 82;
        int listWidth = 300;
        int listHeight = maxVisibleClans * 30;

        context.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2, 0x80000000);

        var colors = config.getAllClanColors();

        if (colors.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "§7Нет закреплённых цветов", width / 2, listY + 12, Formatting.GRAY.getColorValue());
        } else {
            int visibleCount = Math.min(colors.size() - scrollOffset, maxVisibleClans);

            for (int i = 0; i < visibleCount; i++) {
                int index = scrollOffset + i;
                if (index >= colors.size()) break;

                Map.Entry<String, String> entry = new ArrayList<>(colors.entrySet()).get(index);
                String clan = entry.getKey();
                String color = entry.getValue();
                int y = listY + i * 30;

                boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth &&
                                   mouseY >= y && mouseY <= y + 28;
                int bgColor = isHovered ? 0x30FFFFFF : 0x10FFFFFF;
                context.fill(listX, y, listX + listWidth, y + 28, bgColor);

                // Название клана
                context.drawText(textRenderer, "§b" + clan, listX + 8, y + 11, 0xFFFFFF, true);

                // Цвет (квадрат) + тонкая рамка
                int colorValue = parseColor(color);
                context.fill(listX + 100, y + 8, listX + 122, y + 24, colorValue);
                drawOutline(context, listX + 100, y + 8, listX + 122, y + 24, 0xFF000000);
                context.drawText(textRenderer, "§f" + color, listX + 128, y + 11, 0xAAAAAA, true);

                // Кнопка изменения цвета
                int editBtnX = listX + 180;
                boolean editHovered = mouseX >= editBtnX && mouseX <= editBtnX + 50 &&
                                     mouseY >= y + 6 && mouseY <= y + 22;
                int editBtnColor = editHovered ? 0xFF00AA00 : 0xFF008800;
                context.fill(editBtnX, y + 6, editBtnX + 50, y + 22, editBtnColor);
                context.drawCenteredTextWithShadow(textRenderer, "§aЦвет", editBtnX + 25, y + 10, 0xFFFFFF);

                // Кнопка удаления
                int removeBtnX = listX + listWidth - 45;
                boolean removeHovered = mouseX >= removeBtnX && mouseX <= removeBtnX + 40 &&
                                       mouseY >= y + 6 && mouseY <= y + 24;
                int removeBtnColor = removeHovered ? 0xFFAA0000 : 0xFF880000;
                context.fill(removeBtnX, y + 6, removeBtnX + 40, y + 24, removeBtnColor);
                context.drawCenteredTextWithShadow(textRenderer, "§cX", removeBtnX + 20, y + 10, 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        var colors = config.getAllClanColors();
        int listX = width / 2 - 150;
        int listY = 82;
        int listWidth = 300;

        int visibleCount = Math.min(colors.size() - scrollOffset, maxVisibleClans);

        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= colors.size()) break;

            Map.Entry<String, String> entry = new ArrayList<>(colors.entrySet()).get(index);
            String clan = entry.getKey();
            String color = entry.getValue();
            int y = listY + i * 30;

            // Кнопка изменения цвета
            int editBtnX = listX + 180;
            if (mouseX >= editBtnX && mouseX <= editBtnX + 50 &&
                mouseY >= y + 6 && mouseY <= y + 22) {
                MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, clan, () -> {}));
                return true;
            }

            // Кнопка удаления
            int removeBtnX = listX + listWidth - 45;
            if (mouseX >= removeBtnX && mouseX <= removeBtnX + 40 &&
                mouseY >= y + 6 && mouseY <= y + 24) {
                removeColor(clan);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        var colors = config.getAllClanColors();
        if (colors.size() > maxVisibleClans) {
            scrollOffset -= (int) verticalAmount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, colors.size() - maxVisibleClans));
        }
        return true;
    }

    private void toggleHighlight() {
        config.setHighlightEnabled(!config.isHighlightEnabled());
        if (client != null && client.player != null) {
            String status = config.isHighlightEnabled() ? "включена" : "выключена";
            client.player.sendMessage(Text.literal("§a[EvoChat] Подсветка " + status), false);
        }
        init();
    }

    private void setColor() {
        String clan = clanField.getText().trim();
        String color = colorField.getText().trim();

        if (clan.isEmpty() || color.isEmpty()) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[EvoChat] Введите клан и цвет"), false);
            }
            return;
        }

        String normalizedColor = ClanHighlightConfig.normalizeColor(color);
        config.setClanColor(clan, normalizedColor);

        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§a[EvoChat] Цвет клана §b" + clan + " §aустановлен: §b" + normalizedColor), false);
        }

        clanField.setText("");
        colorField.setText("");
        scrollOffset = 0;
    }

    private void removeColor(String clan) {
        config.removeClanColor(clan);
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§7[EvoChat] Цвет клана §b" + clan + " §7удалён"), false);
        }
        scrollOffset = 0;
    }

    private void openColorPicker() {
        String clan = clanField.getText().trim();
        
        if (clan.isEmpty()) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[EvoChat] Введите название клана"), false);
            }
            return;
        }
        
        MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, clan, () -> {
            clanField.setText("");
            colorField.setText("");
            init();
        }));
    }

    /** Рисует рамку в 1px по периметру прямоугольника (без заливки центра). */
    private void drawOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y1 + 1, color);       // верх
        ctx.fill(x1, y2 - 1, x2, y2, color);       // низ
        ctx.fill(x1, y1, x1 + 1, y2, color);       // лево
        ctx.fill(x2 - 1, y1, x2, y2, color);       // право
    }

    private int parseColor(String color) {
        try {
            if (color.startsWith("#")) {
                return (int) Long.parseLong(color.substring(1), 16) | 0xFF000000;
            }
            return switch (color.toLowerCase()) {
                case "red" -> 0xFFFF0000;
                case "green" -> 0xFF00FF00;
                case "blue" -> 0xFF0000FF;
                case "yellow" -> 0xFFFFFF00;
                case "cyan" -> 0xFF00FFFF;
                case "magenta" -> 0xFFFF00FF;
                case "white" -> 0xFFFFFFFF;
                case "black" -> 0xFF000000;
                case "gray" -> 0xFF888888;
                case "orange" -> 0xFFFFA500;
                case "pink" -> 0xFFFFC0CB;
                case "purple" -> 0xFF800080;
                case "lime" -> 0xFF00FF00;
                case "gold" -> 0xFFFFD700;
                default -> 0xFFFFFFFF;
            };
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}
