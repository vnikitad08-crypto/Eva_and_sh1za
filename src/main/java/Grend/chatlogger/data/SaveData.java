package Grend.chatlogger.data;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Класс для сохранения и загрузки данных игроков
 */
public class SaveData {
    
    /**
     * Сохраняет данные всех игроков в файл
     */
    public static void save(DataManager manager, Path savePath) throws IOException {
        List<PlayerSaveData> saveList = new ArrayList<>();
        
        for (PlayerData player : manager.getAllPlayers()) {
            saveList.add(new PlayerSaveData(
                player.getNickname(),
                player.getClan(),
                player.getLevel(),
                player.isOnline()
            ));
        }
        
        // Записываем в файл в формате: ник|клан|уровень|онлайн
        StringBuilder sb = new StringBuilder();
        sb.append("# EvoChat Save Data\n");
        sb.append("# Format: nickname|clan|level|online\n");
        sb.append("# Generated: ").append(new Date()).append("\n");
        sb.append("# Total players: ").append(saveList.size()).append("\n\n");
        
        for (PlayerSaveData data : saveList) {
            sb.append(data.nickname)
              .append("|")
              .append(data.clan)
              .append("|")
              .append(data.level)
              .append("|")
              .append(data.online ? "1" : "0")
              .append("\n");
        }
        
        Files.writeString(savePath, sb.toString());
    }
    
    /**
     * Загружает данные игроков из файла
     */
    public static void load(DataManager manager, Path savePath) throws IOException {
        if (!Files.exists(savePath)) {
            return; // Файла нет - ничего не загружаем
        }
        
        List<String> lines = Files.readAllLines(savePath);
        
        for (String line : lines) {
            line = line.trim();
            
            // Пропускаем комментарии и пустые строки
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            String[] parts = line.split("\\|");
            if (parts.length >= 4) {
                try {
                    String nickname = parts[0];
                    String clan = parts[1];
                    int level = Integer.parseInt(parts[2]);
                    boolean online = parts[3].equals("1");
                    
                    // Добавляем игрока в менеджер
                    manager.addOrUpdatePlayer(nickname, clan, level);
                    
                    if (online) {
                        manager.setPlayerOnline(nickname);
                    } else {
                        PlayerData player = manager.getPlayer(nickname);
                        if (player != null) {
                            player.markOffline();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[EvoChat] Ошибка загрузки строки: " + line);
                }
            }
        }
    }
    
    /**
     * Внутренний класс для хранения данных игрока
     */
    private static class PlayerSaveData {
        String nickname;
        String clan;
        int level;
        boolean online;
        
        PlayerSaveData(String nickname, String clan, int level, boolean online) {
            this.nickname = nickname;
            this.clan = clan;
            this.level = level;
            this.online = online;
        }
    }
}
