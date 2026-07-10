package com.evochat.gui;

import com.evochat.config.EvoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Small HUD widget showing auto-message status.
 * Example text: "Автосообщение: Вкл" / "Автосообщение: Выкл".
 */
public class AutoMessageWidget {

    public static void render(DrawContext ctx, float tickDelta) {
        EvoConfig cfg = EvoConfig.get();
        if (!cfg.autoMessageWidgetVisible) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        // Respect F1 (hide HUD) — hide widget together with vanilla HUD
        if (mc.options.hudHidden) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int x = Math.round(cfg.autoMessageWidgetX * sw);
        int y = Math.round(cfg.autoMessageWidgetY * sh);

        String label = cfg.autoMessageEnabled
                ? "Автосообщение: Вкл"
                : "Автосообщение: Выкл";

        var tr = mc.textRenderer;
        int textW = tr.getWidth(label);
        int padX = 6;
        int padY = 3;
        int w = textW + padX * 2;
        int h = 12 + padY * 2;

        int bg = cfg.autoMessageEnabled ? 0xB000D4C8 : 0xB01C2333;
        int border = cfg.autoMessageEnabled ? 0xFF00D4C8 : 0xFF8B0000;
        int textColor = cfg.autoMessageEnabled ? 0xFF55FF55 : 0xFFFF5555;

        // Shadow
        ctx.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);

        // Background
        ctx.fill(x, y, x + w, y + h, bg);

        // Border
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);

        // Text
        ctx.drawText(tr, label, x + padX, y + padY + 2, textColor, false);
    }
}

