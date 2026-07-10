package Grend.chatlogger.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Менеджер для управления рунами
 * Мешок рун - GUI с 9 слотами (0-8)
 * Сеты привязаны к слотам: 1→сет1, 3→сет2, 4→сет3, 5→сет4, 6→сет5
 */
public class RunesManager {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean isApplying = false;
    private static int targetSlot = -1;
    private static int delayTicks = 0;
    private static boolean menuOpened = false;
    private static boolean slotClicked = false;

    /**
     * Открыть мешок с рунами командой
     */
    public static void openRunesBag() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        player.networkHandler.sendChatCommand("runesbag");
        System.out.println("[EvoChat] Открытие мешка с рунами...");
    }

    /**
     * Применить сет рун - открывает мешок, кликает по слоту и закрывает
     * @param setIndex индекс сета (0-4)
     */
    public static void applyRuneSet(int setIndex) {
        RunesConfig config = RunesConfig.getInstance();
        
        if (!config.isEnabled(setIndex)) {
            System.out.println("[EvoChat] Сет " + (setIndex + 1) + " отключен");
            return;
        }
        
        // Проверяем, есть ли руны в сете
        boolean hasRunes = false;
        String[] runes = config.getRuneSet(setIndex);
        for (String rune : runes) {
            if (!rune.isEmpty()) {
                hasRunes = true;
                break;
            }
        }
        
        if (!hasRunes) {
            System.out.println("[EvoChat] Сет " + (setIndex + 1) + " пуст");
            return;
        }
        
        // Определяем слот в меню для этого сета
        targetSlot = switch (setIndex) {
            case 0 -> 1;  // Сет 1 → слот 1
            case 1 -> 3;  // Сет 2 → слот 3
            case 2 -> 4;  // Сет 3 → слот 4
            case 3 -> 5;  // Сет 4 → слот 5
            case 4 -> 6;  // Сет 5 → слот 6
            default -> 1;
        };
        
        isApplying = true;
        menuOpened = false;
        slotClicked = false;
        delayTicks = 5;
        
        System.out.println("[EvoChat] Применение сета " + (setIndex + 1) + " в слот " + targetSlot);
        
        // Открываем мешок
        openRunesBag();
    }

    /**
     * Обновление менеджера рун (вызывается каждый тик)
     */
    public static void tick() {
        if (!isApplying) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) {
            reset();
            return;
        }

        ScreenHandler handler = player.currentScreenHandler;
        
        // Ждем открытия меню
        if (!menuOpened) {
            if (handler != null && handler != player.playerScreenHandler) {
                // Меню открыто!
                menuOpened = true;
                delayTicks = 3; // Небольшая задержка перед кликом
                System.out.println("[EvoChat] Меню рун открыто");
            } else {
                if (delayTicks > 0) {
                    delayTicks--;
                } else {
                    // Пробуем открыть снова
                    openRunesBag();
                    delayTicks = 10;
                }
            }
            return;
        }
        
        // Меню открыто - кликаем по слоту
        if (!slotClicked) {
            if (delayTicks > 0) {
                delayTicks--;
                return;
            }
            
            // Кликаем по слоту
            try {
                handler.onSlotClick(targetSlot, 0, SlotActionType.PICKUP, player);
                slotClicked = true;
                delayTicks = 3;
                System.out.println("[EvoChat] Клик по слоту " + targetSlot);
            } catch (Exception e) {
                System.err.println("[EvoChat] Ошибка клика: " + e.getMessage());
                reset();
            }
            return;
        }
        
        // Закрываем меню
        if (slotClicked) {
            if (delayTicks > 0) {
                delayTicks--;
                return;
            }
            
            player.closeHandledScreen();
            System.out.println("[EvoChat] Меню закрыто");
            reset();
        }
    }

    /**
     * Сброс состояния
     */
    private static void reset() {
        isApplying = false;
        targetSlot = -1;
        delayTicks = 0;
        menuOpened = false;
        slotClicked = false;
    }

    /**
     * Обработка нажатия клавиши для рун
     */
    public static boolean handleKeyPress(int keyCode) {
        if (isApplying) return false;
        
        RunesConfig config = RunesConfig.getInstance();
        
        for (int i = 0; i < 5; i++) {
            if (config.isEnabled(i) && config.getSetKeyCode(i) == keyCode) {
                applyRuneSet(i);
                return true;
            }
        }
        
        return false;
    }
}
