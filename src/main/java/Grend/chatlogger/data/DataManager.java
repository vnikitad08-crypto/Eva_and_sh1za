package Grend.chatlogger.data;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Менеджер для управления данными игроков
 */
public class DataManager {
    private static DataManager instance;
    
    // Карта: никнейм -> данные игрока
    private final Map<String, PlayerData> players;
    
    // Путь к файлу экспорта
    private Path exportPath;
    
    private DataManager() {
        players = new HashMap<>();
        exportPath = Paths.get("chat_logs.txt");
    }
    
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }
    
    /**
     * Добавляет или обновляет игрока
     */
    public void addOrUpdatePlayer(String nickname, String clan, int level) {
        if (players.containsKey(nickname)) {
            // Игрок уже есть - обновляем данные
            PlayerData existing = players.get(nickname);
            existing.update(clan, level);
        } else {
            // Новый игрок
            players.put(nickname, new PlayerData(nickname, clan, level));
        }
        
        // Автосохранение после изменения
        save();
    }
    
    /**
     * Помечает игрока как онлайн
     */
    public void setPlayerOnline(String nickname) {
        if (players.containsKey(nickname)) {
            PlayerData player = players.get(nickname);
            player.setOnline(true);
            player.setLastSeenTimestamp(System.currentTimeMillis());
        }
    }
    
    /**
     * Помечает всех игроков как оффлайн (для сброса перед новой сессией)
     */
    public void markAllOffline() {
        for (PlayerData player : players.values()) {
            player.markOffline();
        }
    }
    
    /**
     * Получает игрока по нику
     */
    public PlayerData getPlayer(String nickname) {
        return players.get(nickname);
    }
    
    /**
     * Получает всех игроков из указанного клана
     */
    public List<PlayerData> getPlayersByClan(String clan) {
        return players.values().stream()
            .filter(p -> p.getClan().equalsIgnoreCase(clan))
            .collect(Collectors.toList());
    }
    
    /**
     * Получает все кланы
     */
    public Set<String> getAllClans() {
        return players.values().stream()
            .map(PlayerData::getClan)
            .collect(Collectors.toSet());
    }
    
    /**
     * Получает всех игроков
     */
    public Collection<PlayerData> getAllPlayers() {
        return players.values();
    }
    
    /**
     * Получает количество игроков
     */
    public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * Очищает все данные
     */
    public void clear() {
        players.clear();
    }
    
    /**
     * Экспортирует данные в TXT файл с группировкой по кланам
     */
    public void exportToFile(Path outputPath) throws IOException {
        // Группируем игроков по кланам
        Map<String, List<PlayerData>> playersByClan = players.values().stream()
            .collect(Collectors.groupingBy(PlayerData::getClan));
        
        // Сортируем кланы по алфавиту
        List<String> sortedClans = new ArrayList<>(playersByClan.keySet());
        Collections.sort(sortedClans);
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== EvoChat Export ===\n");
        sb.append("Generated: ").append(new Date()).append("\n");
        sb.append("Total players: ").append(players.size()).append("\n");
        sb.append("Total clans: ").append(playersByClan.size()).append("\n\n");
        sb.append("=".repeat(50)).append("\n\n");
        
        for (String clan : sortedClans) {
            List<PlayerData> clanPlayers = playersByClan.get(clan);
            // Сортируем игроков по нику
            clanPlayers.sort(Comparator.comparing(PlayerData::getNickname));
            
            sb.append("[").append(clan).append("]\n");
            sb.append("-".repeat(30)).append("\n");
            
            for (PlayerData player : clanPlayers) {
                String status = player.isOnline() ? "ОНЛАЙН" : "оффлайн";
                sb.append(String.format("  %s [%d] - %s\n", 
                    player.getNickname(), player.getLevel(), status));
            }
            
            sb.append("\n");
        }
        
        // Записываем в файл
        Files.writeString(outputPath, sb.toString());
    }
    
    /**
     * Экспортирует данные конкретного клана в файл
     */
    public void exportClanToFile(String clan, Path outputPath) throws IOException {
        List<PlayerData> clanPlayers = getPlayersByClan(clan);
        
        if (clanPlayers.isEmpty()) {
            Files.writeString(outputPath, String.format("Клан [%s] не найден или пуст.\n", clan));
            return;
        }
        
        // Сортируем по нику
        clanPlayers.sort(Comparator.comparing(PlayerData::getNickname));
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Клан: ").append(clan).append(" ===\n");
        sb.append("Generated: ").append(new Date()).append("\n");
        sb.append("Players: ").append(clanPlayers.size()).append("\n\n");
        
        int onlineCount = 0;
        for (PlayerData player : clanPlayers) {
            if (player.isOnline()) onlineCount++;
            String status = player.isOnline() ? "ОНЛАЙН" : "оффлайн";
            sb.append(String.format("%s [%d] - %s\n", 
                player.getNickname(), player.getLevel(), status));
        }
        
        sb.append("\n").append("=".repeat(30)).append("\n");
        sb.append("Онлайн: ").append(onlineCount).append(" / ").append(clanPlayers.size()).append("\n");
        
        Files.writeString(outputPath, sb.toString());
    }
    
    /**
     * Устанавливает путь для экспорта
     */
    public void setExportPath(Path exportPath) {
        this.exportPath = exportPath;
    }
    
    /**
     * Получает путь для экспорта
     */
    public Path getExportPath() {
        return exportPath;
    }
    
    /**
     * Сохраняет данные в файл сохранения
     */
    public void save() {
        try {
            SaveData.save(this, getSavePath());
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения: " + e.getMessage());
        }
    }
    
    /**
     * Загружает данные из файла сохранения
     */
    public void load() {
        try {
            SaveData.load(this, getSavePath());
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки: " + e.getMessage());
        }
    }
    
    /**
     * Получает путь к файлу сохранения
     */
    private Path getSavePath() {
        return Paths.get("chatlogger_data.txt");
    }

    /**
     * Добавляет игрока в указанный клан (вручную)
     * Если игрок уже есть - обновляет его клан
     * Если уровень не указан (-1), сохраняет текущий
     */
    public void addPlayerToClan(String nickname, String clan, int level) {
        if (players.containsKey(nickname)) {
            PlayerData player = players.get(nickname);
            if (level >= 0) {
                player.setLevel(level);
            }
            player.setClan(clan);
        } else {
            if (level < 0) {
                level = 1; // уровень по умолчанию
            }
            players.put(nickname, new PlayerData(nickname, clan, level));
        }
        save();
    }

    /**
     * Полностью удаляет игрока из данных
     */
    public void removePlayer(String nickname) {
        if (players.remove(nickname) != null) {
            save();
        }
    }

    /**
     * Удаляет игрока из клана (помечает как без клана)
     */
    public void removePlayerFromClan(String nickname) {
        if (players.containsKey(nickname)) {
            players.get(nickname).setClan("Без клана");
            save();
        }
    }

    /**
     * Удаляет весь клан (помечает всех игроков как "Без клана")
     */
    public void deleteClan(String clan) {
        List<PlayerData> clanPlayers = getPlayersByClan(clan);
        for (PlayerData player : clanPlayers) {
            player.setClan("Без клана");
        }
        save();
    }

    /**
     * Получает средний уровень игроков указанного клана
     */
    public int getAverageLevelByClan(String clan) {
        List<PlayerData> clanPlayers = getPlayersByClan(clan);
        if (clanPlayers.isEmpty()) {
            return 0;
        }
        int totalLevel = clanPlayers.stream().mapToInt(PlayerData::getLevel).sum();
        return totalLevel / clanPlayers.size();
    }
}
