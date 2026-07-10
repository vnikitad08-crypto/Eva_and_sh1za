package com.evochat.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A sleek custom slider that fits EvoChat's dark-glass visual language.
 *
 * Colors: dark-navy track, cyan-teal thumb/fill, soft-white label.
 */
public class EvoSlider extends ClickableWidget {

    // ─── Design ──────────────────────────────────────────────────────────────
    private static final int TRACK_H      = 4;
    private static final int THUMB_W      = 10;
    private static final int THUMB_H      = 18;
    private static final int COLOR_TRACK  = 0xFF17251C;
    private static final int COLOR_FILL   = 0xFF43D17A;
    private static final int COLOR_THUMB  = 0xFF6FE0A0;
    private static final int COLOR_THUMB_HOV = 0xFF8BE9B0;
    private static final int COLOR_LABEL  = 0xFFE8F5EC;
    private static final int COLOR_VALUE  = 0xFF8BA596;

    // ─── State ───────────────────────────────────────────────────────────────
    private float value;       // 0.0 – 1.0
    private final float min;
    private final float max;
    private final String label;
    private final Consumer<Float> onChanged;
    private boolean dragging = false;
    private final ValueFormatter formatter;

    public interface ValueFormatter {
        String format(float mappedValue);
    }

    public EvoSlider(int x, int y, int width, int height,
                     String label, float min, float max, float initial,
                     ValueFormatter formatter, Consumer<Float> onChanged) {
        super(x, y, width, height, Text.of(label));
        this.label = label;
        this.min = min;
        this.max = max;
        this.value = (initial - min) / (max - min);
        this.formatter = formatter;
        this.onChanged = onChanged;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = getX();
        int cy = getY();
        int w  = getWidth();
        int h  = getHeight();

        int trackY = cy + h / 2 - TRACK_H / 2;
        int trackLeft  = cx + THUMB_W / 2;
        int trackRight = cx + w - THUMB_W / 2;
        int trackW     = trackRight - trackLeft;

        // Track background
        ctx.fill(trackLeft,  trackY, trackRight, trackY + TRACK_H, COLOR_TRACK);

        // Filled portion
        int fillX = trackLeft + (int)(value * trackW);
        ctx.fill(trackLeft, trackY, fillX, trackY + TRACK_H, COLOR_FILL);

        // Thumb
        int thumbX = trackLeft + (int)(value * trackW) - THUMB_W / 2;
        int thumbY = cy + h / 2 - THUMB_H / 2;
        boolean hov = mouseX >= thumbX && mouseX <= thumbX + THUMB_W
                && mouseY >= thumbY && mouseY <= thumbY + THUMB_H;
        int thumbColor = (hov || dragging) ? COLOR_THUMB_HOV : COLOR_THUMB;

        // Thumb shadow
        ctx.fill(thumbX + 2, thumbY + 2, thumbX + THUMB_W + 2, thumbY + THUMB_H + 2,
                0x33000000);
        // Thumb body
        ctx.fill(thumbX, thumbY, thumbX + THUMB_W, thumbY + THUMB_H, thumbColor);
        // Thumb inner line accent
        ctx.fill(thumbX + 4, thumbY + 4, thumbX + 6, thumbY + THUMB_H - 4, 0xFFFFFFFF);

        // Label on left
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, label, cx, cy - 12, COLOR_LABEL, false);

        // Value on right
        String valStr = formatter.format(getMappedValue());
        ctx.drawText(tr, valStr, cx + w - tr.getWidth(valStr), cy - 12, COLOR_VALUE, false);
    }

    public float getMappedValue() {
        return min + value * (max - min);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (dragging) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateValue(double mouseX) {
        int trackLeft  = getX() + THUMB_W / 2;
        int trackRight = getX() + getWidth() - THUMB_W / 2;
        value = (float) Math.max(0, Math.min(1,
                (mouseX - trackLeft) / (double)(trackRight - trackLeft)));
        onChanged.accept(getMappedValue());
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
