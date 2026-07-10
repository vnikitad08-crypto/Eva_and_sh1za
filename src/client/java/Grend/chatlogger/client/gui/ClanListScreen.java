package Grend.chatlogger.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import Grend.chatlogger.client.ClanHighlightConfig;
import Grend.chatlogger.client.util.OnlineChecker;
import Grend.chatlogger.data.DataManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Экран списка кланов в стиле главного меню (зелёная айдентика).
 *
 * Возможности: добавить клан, задать отношение (Враг/Друг/Нейтрал) для
 * подсветки игроков, выбрать свой цвет, посмотреть игроков, удалить клан.
 */
public class ClanListScreen extends Screen {

    private static final int COL_DIM    = 0xAA000000;
    private static final int COL_PANEL  = 0xFF0E1512;
    private static final int COL_ACCENT = 0xFF43D17A;
    private static final int COL_GLOW   = 0x1F43D17A;
    private static final int COL_MUTED  = 0xFF8BA596;
    private static final int COL_BORDER = 0xFF23372B;
    private static final int COL_ROW    = 0x14FFFFFF;
    private static final int COL_ROW_H  = 0x24FFFFFF;

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 330;
    private static final int ROW_H = 26;

    private final Screen parent;
    private final ClanHighlightConfig hl = ClanHighlightConfig.getInstance();

    private List<String> clans = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxVisible = 6;

    private TextFieldWidget addField;
    private int px, py, listX, listY, listW;

    public ClanListScreen(Screen parent) {
        super(Text.literal("Список кланов"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;

        int margin = 16;
        int contentX = px + margin;
        int contentW = PANEL_W - margin * 2;

        refreshClans();

        listX = contentX;
        listW = contentW;
        listY = py + 114;
        maxVisible = 6;

        // Поле добавления клана + кнопка
        int addY = py + 52;
        int addBtnW = 80;
        int fieldW = contentW - addBtnW - 6;
        addField = new TextFieldWidget(textRenderer, contentX, addY, fieldW, 18, Text.literal(""));
        addField.setMaxLength(32);
        addField.setPlaceholder(Text.literal("Название клана"));
        addSelectableChild(addField);
        addDrawableChild(addField);

        addDrawableChild(new CLButton(contentX + fieldW + 6, addY, addBtnW, 18, "Добавить", b -> addClan()));

        // Слайдер прозрачности подсветки. Инвертирован: слева (0%) — максимально
        // яркая сплошная заливка, справа (100%) — подсветки нет.
        addDrawableChild(new com.evochat.gui.EvoSlider(contentX, py + 88, contentW, 20,
                "Прозрачность подсветки", 0.0f, 1.0f, 1.0f - hl.getHighlightStrength(),
                v -> String.format("%.0f%%", v * 100),
                v -> hl.setHighlightStrength(1.0f - v)));

        // Нижние кнопки
        int btnY1 = py + PANEL_H - 52;
        int btnY2 = py + PANEL_H - 28;
        int halfW = (contentW - 6) / 2;
        addDrawableChild(new CLButton(contentX, btnY1, halfW, 20, "Проверить все", b -> {
            OnlineChecker.checkAllPlayers();
            msg("§7[EvoChat] Запущена проверка всех игроков");
        }));
        addDrawableChild(new CLButton(contentX + halfW + 6, btnY1, halfW, 20, "Стоп", b -> {
            OnlineChecker.stopChecking();
            msg("§7[EvoChat] Проверка остановлена");
        }));
        addDrawableChild(new CLButton(contentX, btnY2, contentW, 20, parent != null ? "Назад" : "Закрыть", b -> close()));
    }

    private void refreshClans() {
        LinkedHashMap<String, String> byUpper = new LinkedHashMap<>();
        for (String c : DataManager.getInstance().getAllClans()) {
            if (c == null) continue;
            String u = c.toUpperCase().trim();
            if (u.isEmpty() || u.equals("БЕЗ КЛАНА")) continue;
            byUpper.putIfAbsent(u, c);
        }
        for (String u : hl.getManagedClans()) {
            if (u != null && !u.isEmpty()) byUpper.putIfAbsent(u, u);
        }
        clans = new ArrayList<>(byUpper.values());
        clans.sort(String.CASE_INSENSITIVE_ORDER);
    }

    private void addClan() {
        String name = addField.getText().trim();
        if (name.isEmpty()) { msg("§c[EvoChat] Введите название клана"); return; }
        hl.addManagedClan(name);
        addField.setText("");
        scrollOffset = 0;
        refreshClans();
    }

    private void cycleRelation(String clan) {
        String rel = hl.getClanRelation(clan);
        String next = switch (rel) {
            case "none" -> "enemy";
            case "enemy" -> "friend";
            default -> "none";
        };
        hl.setClanRelation(clan, next);
        refreshClans();
    }

    private void deleteClan(String clan) {
        hl.removeManagedClan(clan);
        DataManager.getInstance().deleteClan(clan);
        scrollOffset = 0;
        refreshClans();
        msg("§7[EvoChat] Клан '" + clan + "' удалён");
    }

    private void msg(String text) {
        if (client != null && client.player != null) client.player.sendMessage(Text.literal(text), false);
    }

    // ─── Геометрия кнопок строки ──────────────────────────────────────────────
    private int delX()     { return listX + listW - 18; }
    private int playersX() { return delX() - 6 - 40; }
    private int colorX()   { return playersX() - 6 - 44; }
    private int relX()     { return colorX() - 6 - 78; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_DIM);
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, COL_GLOW);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, COL_PANEL);
        ctx.fill(px, py, px + PANEL_W, py + 3, COL_ACCENT);
        ctx.fill(px, py, px + 1, py + PANEL_H, COL_BORDER);
        ctx.fill(px + PANEL_W - 1, py, px + PANEL_W, py + PANEL_H, COL_BORDER);
        ctx.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, COL_BORDER);

        var tr = textRenderer;
        String title = "Список кланов";
        ctx.drawText(tr, title, px + PANEL_W / 2 - tr.getWidth(title) / 2, py + 14, COL_ACCENT, false);
        String sub = "Отношение клана задаёт подсветку игроков. Всего: " + clans.size();
        ctx.drawText(tr, sub, px + PANEL_W / 2 - tr.getWidth(sub) / 2, py + 30, COL_MUTED, false);
        ctx.fill(px + 16, py + 44, px + PANEL_W - 16, py + 45, COL_BORDER);

        // Список
        int listHeight = maxVisible * ROW_H;
        ctx.fill(listX - 2, listY - 2, listX + listW + 2, listY + listHeight + 2, 0x40000000);

        if (clans.isEmpty()) {
            String empty = "§7Кланов нет — добавь клан выше";
            ctx.drawText(tr, empty, listX + listW / 2 - tr.getWidth(empty) / 2, listY + 10, COL_MUTED, false);
        } else {
            int visible = Math.min(clans.size() - scrollOffset, maxVisible);
            for (int i = 0; i < visible; i++) {
                int index = scrollOffset + i;
                if (index >= clans.size()) break;
                String clan = clans.get(index);
                int y = listY + i * ROW_H;
                drawRow(ctx, tr, clan, y, mouseX, mouseY);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawRow(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                         String clan, int y, int mouseX, int mouseY) {
        boolean rowHov = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY <= y + ROW_H - 2;
        ctx.fill(listX, y, listX + listW, y + ROW_H - 2, rowHov ? COL_ROW_H : COL_ROW);

        String rel = hl.getClanRelation(clan);
        int relColor = relColor(rel);

        // Индикатор отношения
        ctx.fill(listX + 6, y + ROW_H / 2 - 4, listX + 12, y + ROW_H / 2 + 2, relColor);

        // Имя клана + количество (обрезаем, чтобы не наезжало на кнопки)
        String name = clan.length() > 12 ? clan.substring(0, 12) + "…" : clan;
        int count = DataManager.getInstance().getPlayersByClan(clan).size();
        ctx.drawText(tr, "§f" + name, listX + 18, y + ROW_H / 2 - 4, 0xFFFFFF, false);
        ctx.drawText(tr, "§7" + count + " игр", listX + 18 + tr.getWidth(name) + 6, y + ROW_H / 2 - 4, 0xAAAAAA, false);

        int by = y + 4, bh = ROW_H - 10;

        // Кнопка отношения
        String relLabel = "enemy".equals(rel) ? "Враг" : "friend".equals(rel) ? "Друг" : "Нейтрал";
        drawBtn(ctx, tr, relX(), by, 78, bh, relLabel, dim(relColor), mouseX, mouseY);

        // Кнопка выбора цвета
        drawBtn(ctx, tr, colorX(), by, 44, bh, "Цвет", 0xFF2E7D46, mouseX, mouseY);

        // Кнопка игроков
        drawBtn(ctx, tr, playersX(), by, 40, bh, "Игр.", 0xFF1F5A33, mouseX, mouseY);

        // Кнопка удаления
        drawBtn(ctx, tr, delX(), by, 18, bh, "X", 0xFF7A1B1B, mouseX, mouseY);
    }

    private void drawBtn(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                         int x, int y, int w, int h, String label, int color, int mouseX, int mouseY) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        ctx.fill(x, y, x + w, y + h, hov ? brighten(color) : color);
        ctx.drawText(tr, label, x + w / 2 - tr.getWidth(label) / 2, y + h / 2 - 4, 0xFFFFFFFF, false);
    }

    private static int relColor(String rel) {
        return "enemy".equals(rel) ? 0xFFFF5555 : "friend".equals(rel) ? 0xFF55FF55 : 0xFF888888;
    }

    private static int dim(int c) {
        int r = ((c >> 16) & 0xFF) * 2 / 3, g = ((c >> 8) & 0xFF) * 2 / 3, b = (c & 0xFF) * 2 / 3;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 40), g = Math.min(255, ((c >> 8) & 0xFF) + 40), b = Math.min(255, (c & 0xFF) + 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int visible = Math.min(clans.size() - scrollOffset, maxVisible);
        for (int i = 0; i < visible; i++) {
            int index = scrollOffset + i;
            if (index >= clans.size()) break;
            String clan = clans.get(index);
            int y = listY + i * ROW_H;
            int by = y + 4, bh = ROW_H - 10;
            if (mouseY < by || mouseY > by + bh) continue;

            if (hit(mouseX, relX(), 78)) { cycleRelation(clan); return true; }
            if (hit(mouseX, colorX(), 44)) {
                client.setScreen(new ColorPickerScreen(this, clan, () -> {}));
                return true;
            }
            if (hit(mouseX, playersX(), 40)) { client.setScreen(new ClanPlayersScreen(this, clan)); return true; }
            if (hit(mouseX, delX(), 18)) { deleteClan(clan); return true; }
        }
        return false;
    }

    private boolean hit(double mouseX, int x, int w) {
        return mouseX >= x && mouseX <= x + w;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (clans.size() > maxVisible) {
            scrollOffset -= (int) v;
            scrollOffset = Math.max(0, Math.min(scrollOffset, clans.size() - maxVisible));
        }
        return true;
    }

    @Override
    public void close() {
        if (parent != null) client.setScreen(parent);
        else super.close();
    }
}
