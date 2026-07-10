package com.evochat.mixin;

import com.evochat.chat.EvoChatWindow;
import com.evochat.sound.EvoChatSound;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Intercepts every chat message received by the client.
 *
 * If the plain text of a message looks like a private message (ЛС),
 * it is routed into {@link EvoChatWindow} and hidden from the vanilla chat.
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    /**
     * Заменяет ники игроков в приходящих сообщениях чата на пользовательские
     * (см. {@link Grend.chatlogger.client.NicknameConfig}). Стили сообщения
     * сохраняются; если замен нет — возвращается исходный объект.
     */
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true)
    private Text evochat$replaceNicknames(Text message) {
        try {
            return Grend.chatlogger.client.NicknameConfig.getInstance().replaceInText(message);
        } catch (Throwable t) {
            return message;
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD")
    )
    private void evochat$onAddMessage(Text message,
                                      net.minecraft.network.message.MessageSignatureData signature,
                                      net.minecraft.client.gui.hud.MessageIndicator indicator,
                                      CallbackInfo ci) {
        if (message == null) return;

        String raw = message.getString();
        if (raw == null || raw.isEmpty()) return;

        if (isPrivateMessage(raw)) {
            // Дублируем ЛС в EvoChat, но оставляем его и в обычном чате.
            EvoChatWindow.getInstance().addMessage(raw.trim());
            EvoChatSound.playIfEnabled();
        }
    }

    /**
     * Heuristic for detecting private messages labeled as "ЛС".
     * Works with formats like:
     * "ЛС от Ник: текст", "[ЛС] Ник: текст", "   [ЛС] текст" etc.
     */
    private static boolean isPrivateMessage(String text) {
        String trimmed = text.stripLeading();
        if (trimmed.length() < 2) return false;

        String upper = trimmed.toUpperCase(Locale.ROOT);

        // Direct "ЛС..." at start
        if (upper.startsWith("ЛС")) return true;

        // Common bracketed prefixes: "[ЛС]" / "[ЛС " etc.
        if (upper.startsWith("[ЛС")) return true;

        // If "ЛС" appears very early (e.g. color tags/brackets before it)
        int idx = upper.indexOf("ЛС");
        return idx >= 0 && idx <= 4;
    }
}
