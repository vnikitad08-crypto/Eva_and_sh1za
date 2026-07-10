package Grend.chatlogger.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import Grend.chatlogger.data.*;
import Grend.chatlogger.parser.ChatMessageParser;
import Grend.chatlogger.parser.ChatMessageParser.ParseResult;
import Grend.chatlogger.client.util.OnlineChecker;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        String messageText = message.getString();

        // Парсим сообщение из чата (для сбора игроков)
        ParseResult result = ChatMessageParser.parse(messageText);

        if (result.success) {
            DataManager manager = DataManager.getInstance();
            // Только собираем/обновляем игрока (ник, клан, уровень).
            // Статус «онлайн» НЕ выставляем: сбор из чата не означает, что игрок
            // сейчас в сети. Онлайн определяется только явной проверкой через ЛС
            // (OnlineChecker), иначе все собранные игроки ложно горят онлайном.
            manager.addOrUpdatePlayer(result.nickname, result.clan, result.level);
        }

        // Проверяем ответ на ЛС (для проверки онлайна)
        OnlineChecker.handleServerResponse(messageText);
    }
}
