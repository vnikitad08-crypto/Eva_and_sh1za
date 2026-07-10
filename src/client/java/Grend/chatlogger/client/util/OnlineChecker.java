package Grend.chatlogger.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import Grend.chatlogger.data.*;
import Grend.chatlogger.client.ModConfig;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Утилита для проверки онлайна игроков через отправку ЛС
 */
public class OnlineChecker {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DataManager manager = DataManager.getInstance();
    private static final ModConfig config = ModConfig.getInstance();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Ожидание между проверками (мс). Больше задержка — реже пакеты на сервер
    // (меньше нагрузки и риска спам-кика при проверке многих игроков).
    private static final int CHECK_DELAY = 2000;
    
    // Список игроков на проверку
    private static List<String> checkQueue = new ArrayList<>();
    private static int currentIndex = 0;
    private static boolean isChecking = false;

    /**
     * Останавливает текущую проверку
     */
    public static void stopChecking() {
        isChecking = false;
        checkQueue.clear();
        currentIndex = 0;
        // Сбрасываем состояние клан-проверки, иначе отложенная задача может
        // ещё раз отчитаться о «завершении» и вызвать callback уже после стопа.
        clanCheckCallback = null;
        clanCheckClanName = null;
        lastCheckedPlayer = null;
        sendMessage("§7[EvoChat] Проверка остановлена пользователем");
    }
    // Callback после проверки клана
    private static Runnable clanCheckCallback = null;
    private static String clanCheckClanName = null;
    
    // Последний проверяемый игрок
    private static String lastCheckedPlayer = null;

    /**
     * Запускает проверку всех игроков
     */
    public static void checkAllPlayers() {
        if (isChecking) {
            sendMessage("§7[EvoChat] Проверка уже выполняется...");
            return;
        }
        
        checkQueue.clear();
        for (PlayerData player : manager.getAllPlayers()) {
            checkQueue.add(player.getNickname());
        }
        
        if (checkQueue.isEmpty()) {
            sendMessage("§7[EvoChat] Нет игроков для проверки");
            return;
        }
        
        // Сначала помечаем всех как оффлайн
        manager.markAllOffline();
        
        sendMessage("§a[EvoChat] Начинаю проверку " + checkQueue.size() + " игроков через ЛС...");
        currentIndex = 0;
        isChecking = true;
        
        scheduler.schedule(OnlineChecker::checkNextPlayer, 100, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Запускает проверку игроков конкретного клана
     */
    public static void checkClan(String clan, Runnable callback) {
        if (isChecking) {
            sendMessage("§7[EvoChat] Проверка уже выполняется...");
            if (callback != null) callback.run();
            return;
        }
        
        List<PlayerData> clanPlayers = manager.getPlayersByClan(clan);
        if (clanPlayers.isEmpty()) {
            sendMessage("§7[EvoChat] Клан '" + clan + "' не найден или пуст");
            if (callback != null) callback.run();
            return;
        }
        
        checkQueue.clear();
        for (PlayerData player : clanPlayers) {
            checkQueue.add(player.getNickname());
        }
        
        // Помечаем игроков клана как оффлайн
        for (PlayerData player : clanPlayers) {
            player.markOffline();
        }
        
        clanCheckClanName = clan;
        clanCheckCallback = callback;
        
        sendMessage("§a[EvoChat] Проверка клана '" + clan + "' (" + checkQueue.size() + " игроков) через ЛС...");
        currentIndex = 0;
        isChecking = true;
        
        scheduler.schedule(OnlineChecker::checkNextPlayer, 100, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Проверяет следующего игрока
     */
    private static void checkNextPlayer() {
        // Проверку остановили (stopChecking) — отменяем эту отложенную итерацию.
        if (!isChecking) {
            return;
        }
        if (currentIndex >= checkQueue.size()) {
            isChecking = false;
            checkQueue.clear();
            
            // Показываем результат
            if (clanCheckClanName != null) {
                int online = 0;
                for (PlayerData p : manager.getPlayersByClan(clanCheckClanName)) {
                    if (p.isOnline()) online++;
                }
                sendMessage("§a[EvoChat] Проверка клана завершена! Онлайн: " + online + " / " + manager.getPlayersByClan(clanCheckClanName).size());
                
                if (clanCheckCallback != null) {
                    clanCheckCallback.run();
                }
                
                clanCheckCallback = null;
                clanCheckClanName = null;
            } else {
                sendMessage("§a[EvoChat] Проверка завершена!");
                int online = 0;
                for (PlayerData p : manager.getAllPlayers()) {
                    if (p.isOnline()) online++;
                }
                sendMessage("§aОнлайн: " + online + " / " + manager.getPlayerCount());
            }
            return;
        }
        
        String nickname = checkQueue.get(currentIndex);
        lastCheckedPlayer = nickname;
        sendTellMessage(nickname);
        
        currentIndex++;
        scheduler.schedule(OnlineChecker::checkNextPlayer, CHECK_DELAY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Отправляет ЛС игроку (команду от имени игрока)
     */
    private static void sendTellMessage(String nickname) {
        // Проверку онлайна планирует фоновый поток, но работа с сетью
        // должна выполняться в основном потоке клиента, иначе возможен краш
        mc.execute(() -> {
            if (mc.player == null || mc.player.networkHandler == null) return;

            // Отправляем как команду (сервер воспримет как команду от игрока)
            mc.player.networkHandler.sendChatCommand(config.buildTellCommand(nickname));
        });
    }
    
    /**
     * Обработка ответа сервера
     */
    public static void handleServerResponse(String message) {
        if (!isChecking || lastCheckedPlayer == null) return;

        // Игнорируем сообщения от EvoChat
        if (message.contains("[EvoChat")) return;

        // Игнорируем сообщения которые сами отправили
        String sentCommand = config.buildTellCommand(lastCheckedPlayer);
        if (message.contains("/" + sentCommand) || message.contains(sentCommand)) return;

        // Игрок оффлайн если сервер сообщил, что его нет в сети / не найден
        boolean isOffline = message.contains("не найден") ||
                           message.contains("не существует") ||
                           message.contains("не в сети") ||
                           message.contains("оффлайн") ||
                           message.contains("offline");

        // Игрок онлайн ТОЛЬКО если сервер подтвердил доставку нашего ЛС
        // («ЛС | Я »» — эхо отправленного личного сообщения) либо явно сказал,
        // что игрок в сети, но не принимает ЛС. Раньше здесь стоял голый "»",
        // из-за чего онлайном помечало почти любую строку чата (клан-чат, ЛС и т.п.).
        boolean isOnline = message.contains("ЛС | Я »") ||
                          message.contains("не может получить") ||
                          message.contains("отключил личные сообщения") ||
                          message.contains("игнорирует");

        if (isOffline || isOnline) {
            PlayerData player = manager.getPlayer(lastCheckedPlayer);
            if (player != null) {
                player.setOnline(isOnline);
            }

            if (isOnline) {
                sendMessage("§a[EvoChat] " + lastCheckedPlayer + " - онлайн");
            } else {
                sendMessage("§7[EvoChat] " + lastCheckedPlayer + " - оффлайн");
            }

            lastCheckedPlayer = null;

            // Одиночная проверка (checkPlayer) не использует очередь -
            // снимаем флаг, иначе последующие проверки навсегда заблокируются
            if (checkQueue.isEmpty()) {
                isChecking = false;
            }
        }
    }
    
    /**
     * Проверяет конкретного игрока
     */
    public static void checkPlayer(String nickname) {
        if (mc.player == null) return;
        
        sendMessage("§7[EvoChat] Проверка игрока: " + nickname);
        lastCheckedPlayer = nickname;
        isChecking = true;
        sendTellMessage(nickname);

        // Страховка: если ответ сервера не распознан, снимаем флаг через 5с,
        // иначе одиночная проверка навсегда заблокирует последующие.
        scheduler.schedule(() -> {
            if (isChecking && checkQueue.isEmpty()) {
                isChecking = false;
                lastCheckedPlayer = null;
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    private static void sendMessage(String text) {
        if (mc.player != null) {
            mc.execute(() -> mc.player.sendMessage(Text.literal(text), false));
        }
    }
}
