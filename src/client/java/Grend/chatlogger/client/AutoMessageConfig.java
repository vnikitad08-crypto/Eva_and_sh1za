package Grend.chatlogger.client;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Конфигурация для автоотправки сообщений
 */
public class AutoMessageConfig {
    private static AutoMessageConfig instance;
    private static final Path CONFIG_PATH = Paths.get("chatlogger_automessage.properties");

    // Включено ли автоотправка
    private boolean enabled = false;
    
    // Интервал между сообщениями (в секундах)
    private int intervalSeconds = 30;
    
    // Список сообщений для отправки
    private List<String> messages = new ArrayList<>();
    
    // Текущий индекс сообщения (для циклической отправки)
    private int currentIndex = 0;

    private AutoMessageConfig() { 
        messages.add("Привет! Это авто-сообщение.");
        load(); 
    }

    public static synchronized AutoMessageConfig getInstance() {
        if (instance == null) instance = new AutoMessageConfig();
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = Math.max(5, intervalSeconds); // Минимум 5 секунд
        save();
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
        save();
    }

    public void addMessage(String message) {
        messages.add(message);
        save();
    }

    public void removeMessage(int index) {
        if (index >= 0 && index < messages.size()) {
            messages.remove(index);
            if (currentIndex >= messages.size()) {
                currentIndex = Math.max(0, messages.size() - 1);
            }
            save();
        }
    }

    public String getNextMessage() {
        if (messages.isEmpty()) return null;
        String message = messages.get(currentIndex);
        currentIndex = (currentIndex + 1) % messages.size();
        save();
        return message;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public void save() {
        try {
            Properties props = new Properties();
            props.setProperty("enabled", String.valueOf(enabled));
            props.setProperty("intervalSeconds", String.valueOf(intervalSeconds));
            props.setProperty("currentIndex", String.valueOf(currentIndex));
            props.setProperty("messageCount", String.valueOf(messages.size()));
            
            for (int i = 0; i < messages.size(); i++) {
                props.setProperty("message." + i, messages.get(i));
            }
            
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "EvoChat Auto Message Configuration");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения конфига автоотправки: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) { props.load(is); }
            
            enabled = Boolean.parseBoolean(props.getProperty("enabled", "false"));
            intervalSeconds = Integer.parseInt(props.getProperty("intervalSeconds", "30"));
            currentIndex = Integer.parseInt(props.getProperty("currentIndex", "0"));
            
            int messageCount = Integer.parseInt(props.getProperty("messageCount", "1"));
            messages.clear();
            for (int i = 0; i < messageCount; i++) {
                String msg = props.getProperty("message." + i);
                if (msg != null) {
                    messages.add(msg);
                }
            }
            
            if (messages.isEmpty()) {
                messages.add("Привет! Это авто-сообщение.");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки конфига автоотправки: " + e.getMessage());
        }
    }
}
