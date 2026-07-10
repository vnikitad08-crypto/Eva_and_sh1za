package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import Grend.chatlogger.data.DataManager;
import Grend.chatlogger.client.ClanHighlightConfig;
import Grend.chatlogger.client.ModConfig;
import Grend.chatlogger.client.ModKeyBindings;
import com.evochat.gui.EvoSettingsScreen;

/**
 * Единый центр управления модом (EvoChat + EvoChat).
 * Кастомный рендер в фирменной зелёной гамме EvoChat.
 */
public class MainScreen extends Screen {

    // ─── Палитра (зелёная айдентика EvoChat) ───────────────────────────
    private static final int COL_DIM      = 0xAA000000;
    private static final int COL_PANEL    = 0xFF0E1512;
    private static final int COL_ACCENT   = 0xFF43D17A;
    private static final int COL_ACCENT_S = 0x1F43D17A;
    private static final int COL_MUTED    = 0xFF8BA596;
    private static final int COL_BORDER   = 0xFF23372B;

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 330;

    private final Screen parent;
    private int px, py;

    public MainScreen(Screen parent) {
        super(Text.literal("EvoChat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;

        int margin = 20;
        int colGap = 10;
        int colW = (PANEL_W - margin * 2 - colGap) / 2;
        int left = px + margin;
        int right = left + colW + colGap;
        int btnH = 20;
        int rowGap = 6;
        int r0 = py + 70;
        int rowStep = btnH + rowGap;

        // ── Навигация по разделам (2 колонки) ─────────────────────────────
        addDrawableChild(new CLButton(left, r0, colW, btnH, "Никнеймы",
                b -> client.setScreen(new NicknameScreen(this))));
        addDrawableChild(new CLButton(right, r0, colW, btnH, "Список кланов",
                b -> client.setScreen(new ClanListScreen(this))));

        addDrawableChild(new CLButton(left, r0 + rowStep, colW, btnH, "Скины",
                b -> client.setScreen(new SkinsScreen(this))));
        String keyName = ModKeyBindings.openGuiKey.getBoundKeyLocalizedText().getString();
        addDrawableChild(new CLButton(right, r0 + rowStep, colW, btnH, "Клавиша: " + keyName,
                b -> client.setScreen(new KeyBindingScreen(this))));

        int fullW = PANEL_W - margin * 2;
        addDrawableChild(new CLButton(left, r0 + rowStep * 2, fullW, btnH, "Виджеты HUD",
                b -> client.setScreen(new com.evochat.gui.WidgetLayoutScreen(this))));

        // Настройки — широкая кнопка внизу списка (открывает единый экран настроек)
        addDrawableChild(new CLButton(left, r0 + rowStep * 3, fullW, btnH, "Настройки",
                b -> client.setScreen(EvoSettingsScreen.general(this))));

        // ── Быстрые переключатели ─────────────────────────────────────────
        int togY = r0 + rowStep * 4 + 8;
        ClanHighlightConfig hl = ClanHighlightConfig.getInstance();
        addDrawableChild(new CLToggle(left, togY, "Подсветка кланов (не сквозь стены)",
                hl.isHighlightEnabled(),
                v -> ClanHighlightConfig.getInstance().setHighlightEnabled(v)));

        addDrawableChild(new CLToggle(left, togY + 26, "Таймер ивентов DW на экране",
                ModConfig.getInstance().isShowEventTimer(),
                v -> ModConfig.getInstance().setShowEventTimer(v)));

        // ── Экспорт / очистка ─────────────────────────────────────────────
        int actY = togY + 56;
        addDrawableChild(new CLButton(left, actY, colW, btnH, "Экспорт данных",
                b -> { exportAll(); close(); }));
        addDrawableChild(new CLButton(right, actY, colW, btnH, "Очистить данные",
                b -> { clearData(); close(); }));

        // ── Закрыть ───────────────────────────────────────────────────────
        addDrawableChild(new CLButton(px + PANEL_W / 2 - 60, py + PANEL_H - 28, 120, btnH,
                parent != null ? "Назад" : "Закрыть", b -> close()));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_DIM);

        // Мягкое свечение вокруг панели
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, COL_ACCENT_S);
        // Панель
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, COL_PANEL);
        // Верхняя акцентная полоса
        ctx.fill(px, py, px + PANEL_W, py + 3, COL_ACCENT);
        // Уголки
        ctx.fill(px, py, px + 6, py + 6, COL_ACCENT);
        ctx.fill(px + PANEL_W - 6, py, px + PANEL_W, py + 6, COL_ACCENT);
        ctx.fill(px, py + PANEL_H - 6, px + 6, py + PANEL_H, COL_ACCENT);
        ctx.fill(px + PANEL_W - 6, py + PANEL_H - 6, px + PANEL_W, py + PANEL_H, COL_ACCENT);
        // Рамка
        ctx.fill(px, py, px + 1, py + PANEL_H, COL_BORDER);
        ctx.fill(px + PANEL_W - 1, py, px + PANEL_W, py + PANEL_H, COL_BORDER);
        ctx.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, COL_BORDER);

        var tr = client.textRenderer;

        // Заголовок (x2)
        String title = "EvoChat";
        int titleX = px + PANEL_W / 2 - tr.getWidth(title);
        ctx.getMatrices().push();
        ctx.getMatrices().scale(2f, 2f, 1f);
        ctx.drawText(tr, title, titleX / 2, (py + 14) / 2, COL_ACCENT, false);
        ctx.getMatrices().pop();

        // Авторы
        String authors = "Авторы: Eva_Elfie и sh1zaExE";
        ctx.drawText(tr, authors, px + PANEL_W / 2 - tr.getWidth(authors) / 2, py + 31, COL_MUTED, false);

        // Спонсоры
        String sponsors = "Спонсоры: lamobron_go1";
        ctx.drawText(tr, sponsors, px + PANEL_W / 2 - tr.getWidth(sponsors) / 2, py + 41, COL_MUTED, false);

        // Статистика
        int playerCount = DataManager.getInstance().getPlayerCount();
        int clanCount = DataManager.getInstance().getAllClans().size();
        String stats = "Игроков: " + playerCount + "   Кланов: " + clanCount;
        ctx.drawText(tr, stats, px + PANEL_W / 2 - tr.getWidth(stats) / 2, py + 52, COL_MUTED, false);

        // Разделитель
        ctx.fill(px + 20, py + 62, px + PANEL_W - 20, py + 63, COL_BORDER);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void exportAll() {
        DataManager manager = DataManager.getInstance();
        try {
            manager.exportToFile(manager.getExportPath());
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§a[EvoChat] Данные экспортированы в " + manager.getExportPath().toAbsolutePath()), false);
            }
        } catch (Exception e) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[EvoChat] Ошибка экспорта: " + e.getMessage()), false);
            }
        }
    }

    private void clearData() {
        DataManager.getInstance().clear();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§a[EvoChat] Данные очищены"), false);
        }
    }

    @Override
    public void close() {
        if (parent != null) {
            client.setScreen(parent);
        } else {
            super.close();
        }
    }
}
