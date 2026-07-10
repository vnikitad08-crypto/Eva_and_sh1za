package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Основной класс для открытия GUI мода EvoChat
 */
public class EvoChatGui {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Открывает главное меню мода
     */
    public static void openMainScreen() {
        Screen mainScreen = new MainScreen(null);
        mc.setScreen(mainScreen);
    }

    /**
     * Открывает главное меню с возможностью возврата на указанный экран
     */
    public static void openMainScreen(Screen parent) {
        Screen mainScreen = new MainScreen(parent);
        mc.setScreen(mainScreen);
    }
}
