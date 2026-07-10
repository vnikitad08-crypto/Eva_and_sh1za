package Grend.chatlogger.client.events;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import Grend.chatlogger.data.DataManager;

/**
 * Менеджер событий (сейчас не используется, миксины работают напрямую)
 */
public class ChatEventListener {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Регистрирует слушатели событий
     */
    public static void register() {
        // Используем миксины вместо событий Fabric API
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§a[EvoChat] Мод активен!"), false);
        }
    }
}
