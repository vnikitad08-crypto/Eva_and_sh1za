package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import Grend.chatlogger.client.NicknameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Меню управления заменами никнеймов.
 *
 * Позволяет указать оригинальный ник игрока и ник, который будет
 * отображаться вместо него (в чате, над головой и в списке игроков),
 * а также просмотреть, отредактировать и удалить существующие замены.
 *
 * Оформление — в фирменной зелёной гамме EvoChat (как MainScreen).
 */
public class NicknameScreen extends Screen {

    // ─── Палитра ─────────────────────────────────────────────────────────────
    private static final int COL_DIM    = 0xAA000000;
    private static final int COL_PANEL  = 0xFF0E1512;
    private static final int COL_ACCENT = 0xFF43D17A;
    private static final int COL_GLOW   = 0x1F43D17A;
    private static final int COL_MUTED  = 0xFF8BA596;
    private static final int COL_BORDER = 0xFF23372B;
    private static final int COL_ROW    = 0x14FFFFFF;
    private static final int COL_ROW_H  = 0x2AFFFFFF;

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 320;

    private final Screen parent;
    private final NicknameConfig config = NicknameConfig.getInstance();

    private int px, py;

    private TextFieldWidget fromField;
    private TextFieldWidget toField;

    private int scrollOffset = 0;
    private int maxVisible = 6;

    // Геометрия списка (пересчитывается в init)
    private int listX, listY, listW, rowH = 24;

    public NicknameScreen(Screen parent) {
        super(Text.literal("Замена никнеймов"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;

        int margin = 16;
        int inX = px + margin;
        int inW = PANEL_W - margin * 2;

        // Поля ввода
        int fieldY = py + 70;
        int halfW = (inW - 8) / 2;

        fromField = new TextFieldWidget(textRenderer, inX, fieldY, halfW, 18, Text.literal(""));
        fromField.setMaxLength(32);
        fromField.setPlaceholder(Text.literal("Ник игрока"));
        addSelectableChild(fromField);
        addDrawableChild(fromField);

        toField = new TextFieldWidget(textRenderer, inX + halfW + 8, fieldY, halfW, 18, Text.literal(""));
        toField.setMaxLength(32);
        toField.setPlaceholder(Text.literal("Показывать как"));
        addSelectableChild(toField);
        addDrawableChild(toField);

        // Кнопка добавить / сохранить
        int btnY = fieldY + 24;
        addDrawableChild(new CLButton(inX, btnY, halfW, 18, "Добавить", b -> addNick()));

        // Переключатель включения всей функции
        addDrawableChild(new CLToggle(inX + halfW + 8, btnY - 1,
                config.isEnabled() ? "Вкл" : "Выкл",
                config.isEnabled(),
                v -> config.setEnabled(v)));

        // Область списка
        listX = inX;
        listY = btnY + 30;
        listW = inW;
        maxVisible = 6;

        // Нижние кнопки
        addDrawableChild(new CLButton(px + PANEL_W / 2 - 62, py + PANEL_H - 28, 120, 20,
                parent != null ? "Назад" : "Закрыть", b -> close()));
    }

    private void addNick() {
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        if (from.isEmpty() || to.isEmpty()) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[EvoChat] Укажите ник и замену"), false);
            }
            return;
        }
        config.set(from, to);
        fromField.setText("");
        toField.setText("");
        scrollOffset = 0;
    }

    private void editNick(String from, String to) {
        fromField.setText(from);
        toField.setText(to);
        // Удаляем старую запись — при нажатии «Добавить» она пересоздастся.
        config.remove(from);
        scrollOffset = 0;
    }

    private void deleteNick(String from) {
        config.remove(from);
        scrollOffset = 0;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_DIM);

        // Панель
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, COL_GLOW);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, COL_PANEL);
        ctx.fill(px, py, px + PANEL_W, py + 3, COL_ACCENT);
        ctx.fill(px, py, px + 1, py + PANEL_H, COL_BORDER);
        ctx.fill(px + PANEL_W - 1, py, px + PANEL_W, py + PANEL_H, COL_BORDER);
        ctx.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, COL_BORDER);

        var tr = textRenderer;

        // Заголовок
        String title = "Замена никнеймов";
        ctx.drawText(tr, title, px + PANEL_W / 2 - tr.getWidth(title) / 2, py + 14, COL_ACCENT, false);

        int count = config.getAll().size();
        String sub = "Заменяет ники в чате, над головой и в Tab. Всего: " + count;
        ctx.drawText(tr, sub, px + PANEL_W / 2 - tr.getWidth(sub) / 2, py + 30, COL_MUTED, false);

        ctx.fill(px + 16, py + 44, px + PANEL_W - 16, py + 45, COL_BORDER);

        // Подписи к полям
        ctx.drawText(tr, "Оригинал → отображаемый ник (цвета: &4 &a &l …)", px + 16, py + 58, COL_MUTED, false);

        // Список
        int listHeight = maxVisible * rowH;
        ctx.fill(listX - 2, listY - 2, listX + listW + 2, listY + listHeight + 2, 0x40000000);

        List<Map.Entry<String, String>> entries = new ArrayList<>(config.getAll().entrySet());

        if (entries.isEmpty()) {
            String empty = "§7Пока нет замен";
            ctx.drawText(tr, empty, listX + listW / 2 - tr.getWidth(empty) / 2, listY + 10, COL_MUTED, false);
        } else {
            int visible = Math.min(entries.size() - scrollOffset, maxVisible);
            for (int i = 0; i < visible; i++) {
                int index = scrollOffset + i;
                if (index >= entries.size()) break;
                Map.Entry<String, String> e = entries.get(index);
                int y = listY + i * rowH;

                boolean hovered = mouseX >= listX && mouseX <= listX + listW
                        && mouseY >= y && mouseY <= y + rowH - 2;
                ctx.fill(listX, y, listX + listW, y + rowH - 2, hovered ? COL_ROW_H : COL_ROW);

                // Оригинал + стрелка, затем цветной предпросмотр замены (&-коды).
                String prefix = "§f" + e.getKey() + " §7→ ";
                int ty = y + rowH / 2 - 5;
                ctx.drawText(tr, prefix, listX + 6, ty, 0xFFFFFF, false);
                int cx = listX + 6 + tr.getWidth(prefix);
                ctx.drawText(tr, NicknameConfig.parseColorCodes(e.getValue()), cx, ty, 0xFFFFFF, false);

                // Кнопка удаления (X)
                int delX = listX + listW - 22;
                boolean delHov = mouseX >= delX && mouseX <= delX + 18 && mouseY >= y + 4 && mouseY <= y + rowH - 6;
                ctx.fill(delX, y + 4, delX + 18, y + rowH - 6, delHov ? 0xFFAA2222 : 0xFF7A1B1B);
                ctx.drawText(tr, "§fX", delX + 7, y + rowH / 2 - 5, 0xFFFFFF, false);

                // Кнопка редактирования
                int edX = delX - 44;
                boolean edHov = mouseX >= edX && mouseX <= edX + 40 && mouseY >= y + 4 && mouseY <= y + rowH - 6;
                ctx.fill(edX, y + 4, edX + 40, y + rowH - 6, edHov ? 0xFF2E7D46 : 0xFF1F5A33);
                ctx.drawText(tr, "§fРед.", edX + 9, y + rowH / 2 - 5, 0xFFFFFF, false);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        List<Map.Entry<String, String>> entries = new ArrayList<>(config.getAll().entrySet());
        int visible = Math.min(entries.size() - scrollOffset, maxVisible);
        for (int i = 0; i < visible; i++) {
            int index = scrollOffset + i;
            if (index >= entries.size()) break;
            Map.Entry<String, String> e = entries.get(index);
            int y = listY + i * rowH;

            int delX = listX + listW - 22;
            if (mouseX >= delX && mouseX <= delX + 18 && mouseY >= y + 4 && mouseY <= y + rowH - 6) {
                deleteNick(e.getKey());
                return true;
            }

            int edX = delX - 44;
            if (mouseX >= edX && mouseX <= edX + 40 && mouseY >= y + 4 && mouseY <= y + rowH - 6) {
                editNick(e.getKey(), e.getValue());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int size = config.getAll().size();
        if (size > maxVisible) {
            scrollOffset -= (int) verticalAmount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, size - maxVisible));
        }
        return true;
    }

    @Override
    public void close() {
        if (parent != null) client.setScreen(parent);
        else super.close();
    }
}
