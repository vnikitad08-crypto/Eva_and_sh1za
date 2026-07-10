package com.evochat.mixin;

import com.evochat.chat.EvoChatWindow;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void evochat$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical == 0.0D) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        // Convert raw mouse coords to scaled GUI coords.
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        // If cursor is over EvoChat, consume wheel input and scroll EvoChat,
        // preventing vanilla chat/history from handling the same wheel event.
        if (EvoChatWindow.getInstance().isMouseOver(mx, my)) {
            int lines = vertical > 0.0D ? 3 : -3;
            EvoChatWindow.getInstance().scroll(lines);
            ci.cancel();
        }
    }
}

