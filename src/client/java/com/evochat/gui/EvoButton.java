package com.evochat.gui;

import com.evochat.chat.EvoChatWindow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Slim dark-navy button with a teal accent border on hover.
 * Replaces vanilla ButtonWidget in EvoChat GUIs.
 */
public class EvoButton extends ClickableWidget {

    private static final int COLOR_BG       = 0xFF17251C;
    private static final int COLOR_BG_HOV   = 0xFF20362A;
    private static final int COLOR_BORDER   = 0xFF23372B;
    private static final int COLOR_BORDER_A = 0xFF43D17A;
    private static final int COLOR_TEXT     = 0xFFE8F5EC;
    private static final int COLOR_TEXT_HOV = 0xFFFFFFFF;

    private final Consumer<EvoButton> onPress;
    private float hoverAnim = 0f;

    public EvoButton(int x, int y, int width, int height,
                     String label, Consumer<EvoButton> onPress) {
        super(x, y, width, height, Text.of(label));
        this.onPress = onPress;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hov = isMouseOver(mouseX, mouseY);
        hoverAnim += ((hov ? 1f : 0f) - hoverAnim) * 0.2f;

        int cx = getX(), cy = getY(), w = getWidth(), h = getHeight();

        // Shadow
        ctx.fill(cx + 2, cy + 2, cx + w + 2, cy + h + 2, 0x33000000);

        // Background
        int bg = lerpColor(COLOR_BG, COLOR_BG_HOV, hoverAnim);
        ctx.fill(cx, cy, cx + w, cy + h, bg);

        // Border
        int border = lerpColor(COLOR_BORDER, COLOR_BORDER_A, hoverAnim);
        ctx.fill(cx, cy,         cx + w, cy + 1,     border);  // top
        ctx.fill(cx, cy + h - 1, cx + w, cy + h,     border);  // bottom
        ctx.fill(cx, cy,         cx + 1, cy + h,     border);  // left
        ctx.fill(cx + w - 1, cy, cx + w, cy + h,     border);  // right

        // Accent top bar on hover
        if (hoverAnim > 0.01f) {
            ctx.fill(cx + 1, cy, cx + w - 1, cy + 2,
                    EvoChatWindow.withAlpha(0x43D17A, (int)(hoverAnim * 200)));
        }

        // Label
        var tr = MinecraftClient.getInstance().textRenderer;
        int txtColor = lerpColor(COLOR_TEXT, COLOR_TEXT_HOV, hoverAnim);
        int txtX = cx + w / 2 - tr.getWidth(getMessage()) / 2;
        int txtY = cy + h / 2 - 4;
        ctx.drawText(tr, getMessage(), txtX, txtY, txtColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            onPress.accept(this);
            return true;
        }
        return false;
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
