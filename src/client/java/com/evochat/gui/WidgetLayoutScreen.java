package com.evochat.gui;

import com.evochat.chat.EvoChatWindow;
import com.evochat.config.EvoConfig;
import Grend.chatlogger.client.EventTimerHud;
import Grend.chatlogger.client.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Редактор расположения HUD-виджетов: EvoChat, виджет автосообщения и таймер
 * ивентов можно перетаскивать мышью. Позиции хранятся относительно размера
 * экрана (0.0–1.0), поэтому не сбиваются при смене разрешения/GUI-scale.
 */
public class WidgetLayoutScreen extends Screen {

    private static final int EVO = 0, AUTO = 1, TIMER = 2;

    private final Screen parent;
    private final EvoConfig evo = EvoConfig.get();
    private final ModConfig mod = ModConfig.getInstance();

    private int drag = -1;
    private int grabDX, grabDY;

    public WidgetLayoutScreen(Screen parent) {
        super(Text.literal("Расположение виджетов"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int by = height - 30;
        addDrawableChild(new EvoButton(width / 2 - 125, by, 120, 20,
                "Сбросить позиции", b -> resetPositions()));
        addDrawableChild(new EvoButton(width / 2 + 5, by, 120, 20,
                "Готово", b -> close()));
    }

    // ─── Геометрия виджетов (в пикселях экрана) ───────────────────────────────
    private int[] rect(int idx) {
        int sw = width, sh = height;
        switch (idx) {
            case EVO -> {
                int w = evo.chatWidth, h = evo.chatHeight;
                int x = clamp(Math.round(evo.chatX * sw), 0, sw - w);
                int y = clamp(Math.round(evo.chatY * sh), 0, sh - h);
                return new int[]{x, y, w, h};
            }
            case AUTO -> {
                String label = evo.autoMessageEnabled ? "Автосообщение: Вкл" : "Автосообщение: Выкл";
                int w = textRenderer.getWidth(label) + 12, h = 18;
                int x = clamp(Math.round(evo.autoMessageWidgetX * sw), 0, sw - w);
                int y = clamp(Math.round(evo.autoMessageWidgetY * sh), 0, sh - h);
                return new int[]{x, y, w, h};
            }
            default -> {
                int w = EventTimerHud.width(textRenderer), h = EventTimerHud.height();
                int x = clamp(Math.round(mod.getEventTimerX() * sw), 0, sw - w);
                int y = clamp(Math.round(mod.getEventTimerY() * sh), 0, sh - h);
                return new int[]{x, y, w, h};
            }
        }
    }

    private void setPos(int idx, int px, int py) {
        int sw = width, sh = height;
        int[] r = rect(idx);
        int maxX = Math.max(0, sw - r[2]), maxY = Math.max(0, sh - r[3]);
        px = clamp(px, 0, maxX);
        py = clamp(py, 0, maxY);
        float rx = sw > 0 ? (float) px / sw : 0f;
        float ry = sh > 0 ? (float) py / sh : 0f;
        switch (idx) {
            case EVO -> { evo.chatX = rx; evo.chatY = ry; }
            case AUTO -> { evo.autoMessageWidgetX = rx; evo.autoMessageWidgetY = ry; }
            default -> mod.setEventTimerPos(rx, ry);
        }
    }

    // ─── Рендер ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);

        // Живой предпросмотр самих виджетов (у скрытых видна только рамка ниже)
        try { EvoChatWindow.getInstance().render(ctx, delta); } catch (Throwable ignored) {}
        AutoMessageWidget.render(ctx, delta);
        int[] rt = rect(TIMER);
        EventTimerHud.render(ctx, rt[0], rt[1]);

        // Рамки-хендлы для перетаскивания
        drawHandle(ctx, EVO, "EvoChat", mouseX, mouseY);
        drawHandle(ctx, AUTO, "Автосообщение", mouseX, mouseY);
        drawHandle(ctx, TIMER, "Таймер ивентов", mouseX, mouseY);

        String title = "Расположение виджетов";
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFF00D4C8);
        String hint = "Перетащите виджеты мышью. «Готово» или ESC — выход.";
        ctx.drawCenteredTextWithShadow(textRenderer, hint, width / 2, 20, 0xFF8B949E);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawHandle(DrawContext ctx, int idx, String name, int mouseX, int mouseY) {
        int[] r = rect(idx);
        int x = r[0], y = r[1], w = r[2], h = r[3];
        boolean hot = drag == idx
                || (drag < 0 && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h);

        int border = hot ? 0xFF00D4C8 : 0x8058A6FF;
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);
        if (hot) ctx.fill(x, y, x + w, y + h, 0x1500D4C8);

        int lw = textRenderer.getWidth(name) + 6;
        int ly = y - 11 >= 0 ? y - 11 : y + 1;
        ctx.fill(x, ly, x + lw, ly + 10, 0xC00D1117);
        ctx.drawText(textRenderer, name, x + 3, ly + 1, 0xFF00D4C8, false);
    }

    // ─── Ввод ──────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            // Мелкие виджеты проверяем раньше большого окна EvoChat.
            for (int idx : new int[]{AUTO, TIMER, EVO}) {
                int[] r = rect(idx);
                if (mouseX >= r[0] && mouseX <= r[0] + r[2]
                        && mouseY >= r[1] && mouseY <= r[1] + r[3]) {
                    drag = idx;
                    grabDX = (int) mouseX - r[0];
                    grabDY = (int) mouseY - r[1];
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (drag >= 0) {
            setPos(drag, (int) mouseX - grabDX, (int) mouseY - grabDY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drag >= 0) {
            evo.save();
            mod.save();
            drag = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void resetPositions() {
        evo.chatX = 0.7f;  evo.chatY = 0.05f;
        evo.autoMessageWidgetX = 0.80f; evo.autoMessageWidgetY = 0.74f;
        mod.setEventTimerPos(0.004f, 0.01f);
        evo.save();
        mod.save();
    }

    private static int clamp(int v, int lo, int hi) {
        if (hi < lo) hi = lo;
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
