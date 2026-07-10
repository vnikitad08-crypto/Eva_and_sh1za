package Grend.chatlogger.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Менеджер тиков для обработки автоотправки и рун
 */
public class TickHandler {
    
    // Для отслеживания нажатий клавиш
    private static final boolean[] keyStates = new boolean[512];

    public static void register() {
        // Обработка тиков клиента
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Автоотправка сообщений
            AutoMessageSender.tick();
            
            // Руны
            RunesManager.tick();
        });

        // Обработка нажатий клавиш
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            handleKeyPress(client);
        });
    }

    private static void handleKeyPress(MinecraftClient client) {
        if (client.player == null) return;
        
        // Обработка нажатий для рун (только если меню не открыто)
        if (client.currentScreen == null) {
            RunesConfig config = RunesConfig.getInstance();
            
            for (int i = 0; i < 5; i++) {
                if (config.isEnabled(i)) {
                    int keyCode = config.getSetKeyCode(i);
                    if (keyCode != 0 && keyCode < keyStates.length) {
                        boolean isPressed = InputUtil.isKeyPressed(
                                client.getWindow().getHandle(), keyCode);
                        
                        // Detect key press (not hold)
                        if (isPressed && !keyStates[keyCode]) {
                            RunesManager.applyRuneSet(i);
                            keyStates[keyCode] = true;
                        } else if (!isPressed) {
                            keyStates[keyCode] = false;
                        }
                    }
                }
            }
        }
    }
}
