package com.evochat.gui;

import com.evochat.config.EvoConfig;
import Grend.chatlogger.client.gui.CLButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Отдельное меню уведомлений виджета ЛС: звук сообщения и его громкость.
 * Оформление — в фирменной зелёной гамме мода (как главное меню).
 */
public class NotificationScreen extends Screen {

    private static final int COL_DIM    = 0xAA000000;
    private static final int COL_PANEL  = 0xFF0E1512;
    private static final int COL_ACCENT = 0xFF43D17A;
    private static final int COL_GLOW   = 0x1F43D17A;
    private static final int COL_MUTED  = 0xFF8BA596;
    private static final int COL_BORDER = 0xFF23372B;

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 200;

    private final Screen parent;
    private EvoConfig cfg;
    private int px, py;

    public NotificationScreen(Screen parent) {
        super(Text.literal("Уведомления"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cfg = EvoConfig.get();
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;

        int margin = 20;
        int contentX = px + margin;
        int contentW = PANEL_W - margin * 2;

        // Звук сообщения (переключатель)
        int row1 = py + 78;
        addDrawableChild(new EvoToggle(contentX, row1, "Звук сообщения",
                cfg.soundEnabled, v -> { cfg.soundEnabled = v; cfg.save(); }));

        // Громкость уведомления (слайдер, независимо от громкости Minecraft)
        int row2 = row1 + 44;
        addDrawableChild(new EvoSlider(contentX, row2, contentW, 20,
                "Громкость уведомления", 0.0f, 1.0f, cfg.soundVolume,
                v -> String.format("%.0f%%", v * 100),
                v -> { cfg.soundVolume = v; cfg.save(); }));

        addDrawableChild(new CLButton(px + PANEL_W / 2 - 60, py + PANEL_H - 30, 120, 20,
                parent != null ? "Назад" : "Закрыть", b -> close()));
    }

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
        String title = "Уведомления";
        ctx.drawText(tr, title, px + PANEL_W / 2 - tr.getWidth(title) / 2, py + 16, COL_ACCENT, false);
        String sub = "Звук личных сообщений";
        ctx.drawText(tr, sub, px + PANEL_W / 2 - tr.getWidth(sub) / 2, py + 32, COL_MUTED, false);
        ctx.fill(px + 20, py + 48, px + PANEL_W - 20, py + 49, COL_BORDER);

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
