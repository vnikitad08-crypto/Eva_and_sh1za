package Grend.chatlogger.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Автоотправка сообщений в чат с настраиваемым интервалом
 */
public class AutoMessageSender {

    private static int ticksSinceLastMessage = 0;
    private static boolean wasEnabled = false;

    /**
     * Обновление таймера и отправка сообщения если пришло время
     */
    public static void tick() {
        AutoMessageConfig config = AutoMessageConfig.getInstance();

        if (!config.isEnabled()) {
            ticksSinceLastMessage = 0;
            wasEnabled = false;
            return;
        }

        // Если только что включили - сбрасываем таймер
        if (!wasEnabled) {
            ticksSinceLastMessage = 0;
            wasEnabled = true;
        }

        ticksSinceLastMessage++;

        // Проверка интервала (20 тиков = 1 секунда)
        int intervalTicks = config.getIntervalSeconds() * 20;

        if (ticksSinceLastMessage >= intervalTicks) {
            sendMessage(config.getNextMessage());
            ticksSinceLastMessage = 0;
        }
    }

    /**
     * Отправка сообщения в чат
     */
    private static void sendMessage(String message) {
        if (message == null || message.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        // Проверяем, не открыт ли чат или другие GUI
        if (client.currentScreen != null) {
            // Не отправляем, если открыт чат или консоль
            return;
        }

        // Отправляем сообщение
        try {
            player.networkHandler.sendChatMessage(message);
            System.out.println("[EvoChat] Автоотправка: " + message);
        } catch (Exception e) {
            System.err.println("[EvoChat] Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    /**
     * Принудительная отправка следующего сообщения
     */
    public static void forceSend() {
        AutoMessageConfig config = AutoMessageConfig.getInstance();
        String message = config.getNextMessage();
        if (message != null) {
            sendMessage(message);
            ticksSinceLastMessage = 0;
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§a[EvoChat] Сообщение отправлено: §f" + message), false);
            }
        }
    }

    /**
     * Получить время до следующего сообщения (в секундах)
     */
    public static int getTimeUntilNextMessage() {
        AutoMessageConfig config = AutoMessageConfig.getInstance();
        int intervalTicks = config.getIntervalSeconds() * 20;
        int remainingTicks = intervalTicks - ticksSinceLastMessage;
        return Math.max(0, remainingTicks / 20);
    }

    /**
     * Сброс таймера
     */
    public static void resetTimer() {
        ticksSinceLastMessage = 0;
    }
}
