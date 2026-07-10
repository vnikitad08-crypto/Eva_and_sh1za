package com.evochat.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import Grend.chatlogger.client.NicknameConfig;

/**
 * Заменяет ник игрока в списке игроков (Tab) на пользовательский.
 *
 * PlayerListHud#render рисует результат getPlayerName(entry) (сохраняет его в
 * локальную переменную и выводит), поэтому подмена возвращаемого значения
 * этого метода меняет имя в таблице.
 */
@Mixin(PlayerListHud.class)
public class PlayerTabNameMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void evochat$renameTab(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        try {
            NicknameConfig cfg = NicknameConfig.getInstance();
            if (!cfg.isEnabled()) return;

            // Надёжный путь: точное совпадение по имени аккаунта игрока —
            // тогда полностью заменяем имя в TAB на пользовательское (с цветами).
            String account = entry.getProfile() != null ? entry.getProfile().getName() : null;
            Text replacement = cfg.getReplacementTextForName(account);
            if (replacement != null) {
                cir.setReturnValue(replacement);
                return;
            }

            // Запасной путь: замена по подстроке в отображаемом имени.
            Text original = cir.getReturnValue();
            Text renamed = cfg.replaceInText(original);
            if (renamed != original) cir.setReturnValue(renamed);
        } catch (Throwable ignored) {
            // оставляем оригинал
        }
    }
}
