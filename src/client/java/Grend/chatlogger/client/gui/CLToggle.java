package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Переключатель-«пилюля» в зелёной гамме EvoChat.
 */
public class CLToggle extends ClickableWidget {

    private static final int PILL_W = 40;
    private static final int PILL_H = 20;
    private static final int KNOB_D = 14;

    private static final int COLOR_ON_BG  = 0xFF43D17A;
    private static final int COLOR_OFF_BG = 0xFF17251C;
    private static final int COLOR_KNOB   = 0xFFFFFFFF;
    private static final int COLOR_LABEL  = 0xFFE8F5EC;

    private boolean toggled;
    private float knobAnim;
    private final String label;
    private final Consumer<Boolean> onChanged;

    public CLToggle(int x, int y, String label, boolean initial, Consumer<Boolean> onChanged) {
        super(x, y, PILL_W, PILL_H, Text.of(label));
        this.label = label;
        this.toggled = initial;
        this.knobAnim = initial ? 1f : 0f;
        this.onChanged = onChanged;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float target = toggled ? 1f : 0f;
        knobAnim += (target - knobAnim) * 0.25f;

        int cx = getX();
        int cy = getY();

        int bgColor = CLButton.lerpColor(COLOR_OFF_BG, COLOR_ON_BG, knobAnim);
        ctx.fill(cx + 2, cy, cx + PILL_W - 2, cy + PILL_H, bgColor);
        ctx.fill(cx, cy + 2, cx + PILL_W, cy + PILL_H - 2, bgColor);

        int knobTrack = PILL_W - KNOB_D - 4;
        int knobX = cx + 2 + (int) (knobAnim * knobTrack);
        int knobY = cy + (PILL_H - KNOB_D) / 2;
        ctx.fill(knobX + 1, knobY + 1, knobX + KNOB_D + 1, knobY + KNOB_D + 1, 0x44000000);
        ctx.fill(knobX, knobY, knobX + KNOB_D, knobY + KNOB_D, COLOR_KNOB);

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

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
