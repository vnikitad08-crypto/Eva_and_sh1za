package Grend.chatlogger.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Убирает размытие фона (blur) для экранов этого мода.
 *
 * Почему через миксин: базовый {@code Screen.render()} сам вызывает
 * {@code renderBackground()} → {@code applyBlur()}, поэтому любой наш экран,
 * который в конце дергает {@code super.render(...)}, всё равно получал блюр —
 * простое удаление явного вызова renderBackground не помогало.
 *
 * Отменяем только для экранов из пакетов мода (Grend.chatlogger / com.evochat),
 * ванильные меню (пауза и т.п.) не трогаем. Затемнение (renderDarkening)
 * остаётся, так что фон по-прежнему приглушён, но без размытия.
 */
@Mixin(Screen.class)
public class ScreenBlurMixin {

    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true, require = 0)
    private void chatlogger$disableBlurForModScreens(CallbackInfo ci) {
        String name = this.getClass().getName();
        if (name.startsWith("Grend.chatlogger.") || name.startsWith("com.evochat.")) {
            ci.cancel();
        }
    }
}
