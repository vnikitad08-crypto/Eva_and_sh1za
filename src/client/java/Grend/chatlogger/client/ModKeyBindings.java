package Grend.chatlogger.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import Grend.chatlogger.client.gui.EvoChatGui;

/**
 * Регистрация клавиш мода
 */
public class ModKeyBindings {

    // Единая категория «EvoChat» в настройках управления Minecraft.
    public static final String KEY_CATEGORY = "category.evochat";
    public static final String KEY_OPEN_GUI = "key.chatlogger.open_gui";

    public static KeyBinding openGuiKey;
    
    private static boolean wasGuiPressedLastTick = false;

    public static void register() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_OPEN_GUI,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,   // ] на US-раскладке == Ъ на русской
            KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Не обрабатывать бинды если открыт чат или другой экран
            if (client.currentScreen != null) return;
            
            // Обработка клавиши GUI
            if (openGuiKey.isPressed()) {
                if (!wasGuiPressedLastTick) {
                    wasGuiPressedLastTick = true;
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("§a[EvoChat] Открываю GUI..."),
                            false
                        );
                        EvoChatGui.openMainScreen();
                    });
                }
            } else {
                wasGuiPressedLastTick = false;
            }
        });
    }
}
