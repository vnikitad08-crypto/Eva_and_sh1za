package com.evochat.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A pill-shaped toggle switch with an animated knob,
 * matching EvoChat's teal-on-dark design language.
 */
public class EvoToggle extends ClickableWidget {

    private static final int PILL_W  = 40;
    private static final int PILL_H  = 20;
    private static final int KNOB_D  = 14;

    private static final int COLOR_ON_BG   = 0xFF43D17A;
    private static final int COLOR_OFF_BG  = 0xFF17251C;
    private static final int COLOR_KNOB    = 0xFFFFFFFF;
    private static final int COLOR_LABEL   = 0xFFE8F5EC;

    private boolean toggled;
    private float   knobAnim; // 0.0 = off position, 1.0 = on position
    private final String label;
    private final Consumer<Boolean> onChanged;

    public EvoToggle(int x, int y, String label, boolean initial, Consumer<Boolean> onChanged) {
        super(x, y, PILL_W, PILL_H, Text.of(label));
        this.label     = label;
        this.toggled   = initial;
        this.knobAnim  = initial ? 1f : 0f;
        this.onChanged = onChanged;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Animate knob
        float target = toggled ? 1f : 0f;
        knobAnim += (target - knobAnim) * 0.25f;

        int cx = getX();
        int cy = getY();

        // Pill background
        int bgColor = lerpColor(COLOR_OFF_BG, COLOR_ON_BG, knobAnim);
        drawRoundRect(ctx, cx, cy, PILL_W, PILL_H, bgColor);

        // Border accent
        if (toggled) {
            drawRoundRectBorder(ctx, cx, cy, PILL_W, PILL_H, 0xFF43D17A, 1);
        }

        // Knob
        int knobTrack = PILL_W - KNOB_D - 4;
        int knobX = cx + 2 + (int)(knobAnim * knobTrack);
        int knobY = cy + (PILL_H - KNOB_D) / 2;
        // Knob shadow
        ctx.fill(knobX + 1, knobY + 1, knobX + KNOB_D + 1, knobY + KNOB_D + 1, 0x44000000);
        // Knob body
        ctx.fill(knobX, knobY, knobX + KNOB_D, knobY + KNOB_D, COLOR_KNOB);

        // Label to the right
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, label, cx + PILL_W + 8, cy + PILL_H / 2 - 4, COLOR_LABEL, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            toggled = !toggled;
            onChanged.accept(toggled);
            return true;
        }
        return false;
    }

    // ─── Drawing helpers ─────────────────────────────────────────────────────
    private static void drawRoundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        // Simulate rounded rect with fill + corner pixels removed
        ctx.fill(x + 2, y,     x + w - 2, y + h, color);
        ctx.fill(x,     y + 2, x + w,     y + h - 2, color);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private static void drawRoundRectBorder(DrawContext ctx, int x, int y,
                                             int w, int h, int color, int t) {
        ctx.fill(x + 2, y,     x + w - 2, y + t, color);   // top
        ctx.fill(x + 2, y + h - t, x + w - 2, y + h, color); // bottom
        ctx.fill(x, y + 2, x + t, y + h - 2, color);   // left
        ctx.fill(x + w - t, y + 2, x + w, y + h - 2, color); // right
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >> 24) & 0xFF;
        int r = ar + (int)((br - ar) * t);
        int g = ag + (int)((bg - ag) * t);
        int bl2 = ab + (int)((bb - ab) * t);
        int alpha = aa + (int)((ba - aa) * t);
        return (alpha << 24) | (r << 16) | (g << 8) | bl2;
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
