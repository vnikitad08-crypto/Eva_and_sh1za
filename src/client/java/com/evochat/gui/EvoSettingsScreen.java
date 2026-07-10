package com.evochat.gui;

import com.evochat.chat.EvoChatWindow;
import com.evochat.config.EvoConfig;
import Grend.chatlogger.client.ModConfig;
import Grend.chatlogger.client.gui.CLButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Единый экран настроек в фирменной зелёной гамме мода (как главное меню).
 *
 * Разделы (вкладки внутри панели): «Основные», «Чат» (виджет ЛС),
 * «Автосообщение». Раздел «Чат» больше не содержит настройку позиции —
 * положение виджета меняется перетаскиванием в редакторе «Виджеты HUD».
 * Настройки звука вынесены в отдельное меню «Уведомления».
 */
public class EvoSettingsScreen extends Screen {

    private enum Section { GENERAL, CHAT, AUTO_MESSAGE }

    // ─── Colors (зелёная айдентика, как в главном меню) ───────────────────────
    private static final int COL_DIM     = 0xAA000000;
    private static final int COL_PANEL   = 0xFF0E1512;
    private static final int COL_ACCENT  = 0xFF43D17A;
    private static final int COL_GLOW    = 0x1F43D17A;
    private static final int COL_MUTED   = 0xFF8BA596;
    private static final int COL_BORDER  = 0xFF23372B;

    // Размер как у главного меню
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 330;

    private final Screen parent;
    private final Section section;
    private EvoConfig cfg;

    private int px, py;

    // Auto-message text field
    private TextFieldWidget autoMessageField;
    // General (EvoChat) section fields
    private TextFieldWidget tellCommandField;
    private TextFieldWidget tellMessageField;
    private TextFieldWidget clanChatSymbolField;

    public EvoSettingsScreen(Screen parent) {
        this(parent, Section.CHAT);
    }

    /** Открывает экран на разделе «Основные». */
    public static EvoSettingsScreen general(Screen parent) {
        return new EvoSettingsScreen(parent, Section.GENERAL);
    }

    /** Открывает экран на разделе «Чат» (виджет ЛС). */
    public static EvoSettingsScreen chat(Screen parent) {
        return new EvoSettingsScreen(parent, Section.CHAT);
    }

    private EvoSettingsScreen(Screen parent, Section section) {
        super(Text.of("EvoChat Settings"));
        this.parent = parent;
        this.section = section;
    }

    @Override
    protected void init() {
        cfg = EvoConfig.get();

        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;

        int margin = 20;
        int contentX = px + margin;
        int contentW = PANEL_W - margin * 2;

        // ── Вкладки внутри панели (оформление как в главном меню) ─────────────
        int tabY = py + 54;
        int tabGap = 6;
        int tabW = (contentW - tabGap * 2) / 3;
        addDrawableChild(new CLButton(contentX, tabY, tabW, 18, "Основные",
                b -> client.setScreen(new EvoSettingsScreen(parent, Section.GENERAL))));
        addDrawableChild(new CLButton(contentX + tabW + tabGap, tabY, tabW, 18, "Чат",
                b -> client.setScreen(new EvoSettingsScreen(parent, Section.CHAT))));
        addDrawableChild(new CLButton(contentX + (tabW + tabGap) * 2, tabY, tabW, 18, "Автосообщение",
                b -> client.setScreen(new EvoSettingsScreen(parent, Section.AUTO_MESSAGE))));

        int top = py + 84;

        if (section == Section.GENERAL) {
            ModConfig mc = ModConfig.getInstance();

            int row1 = top + 12;
            tellCommandField = new TextFieldWidget(client.textRenderer, contentX, row1, contentW, 18, Text.of("tellCommand"));
            tellCommandField.setMaxLength(16);
            tellCommandField.setText(mc.getTellCommand());
            tellCommandField.setChangedListener(t -> { if (!t.trim().isEmpty()) ModConfig.getInstance().setTellCommand(t.trim()); });
            addSelectableChild(tellCommandField);
            addDrawableChild(tellCommandField);

            int row2 = row1 + 44;
            tellMessageField = new TextFieldWidget(client.textRenderer, contentX, row2, contentW, 18, Text.of("tellMessage"));
            tellMessageField.setMaxLength(64);
            tellMessageField.setText(mc.getTellMessage());
            tellMessageField.setChangedListener(t -> { if (!t.trim().isEmpty()) ModConfig.getInstance().setTellMessage(t.trim()); });
            addSelectableChild(tellMessageField);
            addDrawableChild(tellMessageField);

            int row3 = row2 + 44;
            clanChatSymbolField = new TextFieldWidget(client.textRenderer, contentX, row3, contentW, 18, Text.of("clanChatSymbol"));
            clanChatSymbolField.setMaxLength(2);
            clanChatSymbolField.setText(mc.getClanChatSymbol());
            clanChatSymbolField.setChangedListener(t -> { if (!t.isEmpty()) ModConfig.getInstance().setClanChatSymbol(t.substring(0, 1)); });
            addSelectableChild(clanChatSymbolField);
            addDrawableChild(clanChatSymbolField);

            int row4 = row3 + 40;
            addDrawableChild(new EvoToggle(contentX, row4, "Таймер ивентов DW на экране",
                    mc.isShowEventTimer(), v -> ModConfig.getInstance().setShowEventTimer(v)));

        } else if (section == Section.CHAT) {
            int halfW = (contentW - 12) / 2;

            int row1 = top + 8;
            addDrawableChild(new EvoSlider(contentX, row1, contentW, 20,
                    "Прозрачность чата", 0.1f, 1.0f, cfg.chatOpacity,
                    v -> String.format("%.0f%%", v * 100),
                    v -> { cfg.chatOpacity = v; cfg.save(); }));

            int row2 = row1 + 40;
            addDrawableChild(new EvoSlider(contentX, row2, contentW, 20,
                    "Прозрачность фона", 0.0f, 1.0f, cfg.backgroundOpacity,
                    v -> String.format("%.0f%%", v * 100),
                    v -> { cfg.backgroundOpacity = v; cfg.save(); }));

            int row3 = row2 + 40;
            addDrawableChild(new EvoSlider(contentX, row3, halfW, 20,
                    "Ширина", 160f, 600f, cfg.chatWidth,
                    v -> (int) v + "px",
                    v -> { cfg.chatWidth = Math.round(v); cfg.save(); }));
            addDrawableChild(new EvoSlider(contentX + halfW + 12, row3, halfW, 20,
                    "Высота", 80f, 400f, cfg.chatHeight,
                    v -> (int) v + "px",
                    v -> { cfg.chatHeight = Math.round(v); cfg.save(); }));

            int row4 = row3 + 40;
            addDrawableChild(new EvoSlider(contentX, row4, contentW, 20,
                    "Прозрачность окна Minecraft",
                    com.evochat.EvoChatClient.MIN_WINDOW_OPACITY, 1.0f, cfg.windowOpacity,
                    v -> String.format("%.0f%%", v * 100),
                    v -> { cfg.windowOpacity = v; cfg.save(); com.evochat.EvoChatClient.applyWindowOpacity(v); }));

            int row5 = row4 + 34;
            addDrawableChild(new CLButton(contentX, row5, contentW, 18, "Уведомления",
                    b -> client.setScreen(new NotificationScreen(this))));

        } else {
            int halfW = (contentW - 12) / 2;

            int row1 = top + 8;
            addDrawableChild(new EvoToggle(contentX, row1, "Включить автосообщение",
                    cfg.autoMessageEnabled, v -> { cfg.autoMessageEnabled = v; cfg.save(); }));

            int row2 = row1 + 34;
            int currentMinutes = cfg.autoMessageIntervalSeconds / 60;
            int currentSeconds = cfg.autoMessageIntervalSeconds % 60;
            addDrawableChild(new EvoSlider(contentX, row2, halfW, 20,
                    "Минуты", 0f, 30f, currentMinutes, v -> (int) v + " мин",
                    v -> { int mins = Math.round(v); cfg.autoMessageIntervalSeconds = mins * 60 + (cfg.autoMessageIntervalSeconds % 60); cfg.save(); }));
            addDrawableChild(new EvoSlider(contentX + halfW + 12, row2, halfW, 20,
                    "Секунды", 0f, 59f, currentSeconds, v -> (int) v + " сек",
                    v -> { int secs = Math.round(v); int mins = cfg.autoMessageIntervalSeconds / 60; cfg.autoMessageIntervalSeconds = mins * 60 + secs; cfg.save(); }));

            int row3 = row2 + 42;
            autoMessageField = new TextFieldWidget(client.textRenderer, contentX, row3, contentW, 20, Text.of("AutoMessage"));
            autoMessageField.setMaxLength(256);
            autoMessageField.setText(cfg.autoMessageText == null ? "" : cfg.autoMessageText);
            autoMessageField.setChangedListener(t -> { cfg.autoMessageText = t; cfg.save(); });
            addSelectableChild(autoMessageField);
            addDrawableChild(autoMessageField);

            int row4 = row3 + 34;
            addDrawableChild(new EvoToggle(contentX, row4, "Показывать виджет на экране",
                    cfg.autoMessageWidgetVisible, v -> { cfg.autoMessageWidgetVisible = v; cfg.save(); }));

            int row5 = row4 + 30;
            int arrowSize = 18;
            int midX = contentX + contentW / 2;
            addDrawableChild(new EvoButton(midX - 10, row5, arrowSize, arrowSize, "▲",
                    b -> { cfg.autoMessageWidgetY = Math.max(0f, cfg.autoMessageWidgetY - 0.02f); cfg.save(); }));
            addDrawableChild(new EvoButton(midX - 10, row5 + arrowSize + 2, arrowSize, arrowSize, "▼",
                    b -> { cfg.autoMessageWidgetY = Math.min(0.95f, cfg.autoMessageWidgetY + 0.02f); cfg.save(); }));
            addDrawableChild(new EvoButton(midX - 10 - arrowSize - 2, row5 + (arrowSize + 2), arrowSize, arrowSize, "◄",
                    b -> { cfg.autoMessageWidgetX = Math.max(0f, cfg.autoMessageWidgetX - 0.02f); cfg.save(); }));
            addDrawableChild(new EvoButton(midX - 10 + arrowSize + 2, row5 + (arrowSize + 2), arrowSize, arrowSize, "►",
                    b -> { cfg.autoMessageWidgetX = Math.min(0.95f, cfg.autoMessageWidgetX + 0.02f); cfg.save(); }));
        }

        // ── Нижние кнопки ─────────────────────────────────────────────────────
        int closeY = py + PANEL_H - 28;
        addDrawableChild(new CLButton(px + PANEL_W / 2 - 92, closeY, 110, 20, "Закрыть", b -> close()));
        addDrawableChild(new CLButton(px + PANEL_W / 2 + 24, closeY, 68, 20, "Очистить",
                b -> EvoChatWindow.getInstance().clear()));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_DIM);
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, COL_GLOW);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, COL_PANEL);
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

        String title;
        String sub;
        switch (section) {
            case GENERAL -> { title = "Основные"; sub = "Основные настройки мода"; }
            case CHAT    -> { title = "Виджет ЛС"; sub = "Настройки окна личных сообщений"; }
            default      -> { title = "Автосообщение"; sub = "Автоматическая отправка сообщений"; }
        }

        ctx.getMatrices().push();
        ctx.getMatrices().scale(1.6f, 1.6f, 1f);
        int tScaledX = (int) ((px + PANEL_W / 2 - tr.getWidth(title) * 1.6f / 2) / 1.6f);
        ctx.drawText(tr, title, tScaledX, (int) ((py + 14) / 1.6f), COL_ACCENT, false);
        ctx.getMatrices().pop();

        ctx.drawText(tr, sub, px + PANEL_W / 2 - tr.getWidth(sub) / 2, py + 36, COL_MUTED, false);
        ctx.fill(px + 20, py + 48, px + PANEL_W - 20, py + 49, COL_BORDER);

        if (section == Section.GENERAL) {
            ctx.drawText(tr, "Команда для ЛС (без /)", px + 20, py + 86, COL_MUTED, false);
            ctx.drawText(tr, "Сообщение для проверки", px + 20, py + 130, COL_MUTED, false);
            ctx.drawText(tr, "Символ клан-чата", px + 20, py + 174, COL_MUTED, false);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (parent != null) client.setScreen(parent);
        else super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
