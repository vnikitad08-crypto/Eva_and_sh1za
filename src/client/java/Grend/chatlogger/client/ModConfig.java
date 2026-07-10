package Grend.chatlogger.client;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ModConfig {
    private static ModConfig instance;
    private static final Path CONFIG_PATH = Paths.get("chatlogger_config.properties");

    private String tellCommand = "m";
    private String tellMessage = "1";
    private String clanChatSymbol = "@";
    private boolean showEventTimer = true;
    // Позиция HUD-виджета таймера ивентов (относительная, 0.0–1.0)
    private float eventTimerX = 0.004f;
    private float eventTimerY = 0.01f;

    private ModConfig() { load(); }

    public static synchronized ModConfig getInstance() {
        if (instance == null) instance = new ModConfig();
        return instance;
    }

    public String getTellCommand() { return tellCommand; }
    public void setTellCommand(String tellCommand) { this.tellCommand = tellCommand; save(); }
    public String getTellMessage() { return tellMessage; }
    public void setTellMessage(String tellMessage) { this.tellMessage = tellMessage; save(); }
    public String getClanChatSymbol() { return clanChatSymbol; }
    public void setClanChatSymbol(String clanChatSymbol) { this.clanChatSymbol = clanChatSymbol; save(); }
    public boolean isShowEventTimer() { return showEventTimer; }
    public void setShowEventTimer(boolean showEventTimer) { this.showEventTimer = showEventTimer; save(); }

    public float getEventTimerX() { return eventTimerX; }
    public float getEventTimerY() { return eventTimerY; }
    /** Задаёт позицию таймера без записи на диск (для плавного перетаскивания). */
    public void setEventTimerPos(float x, float y) { this.eventTimerX = x; this.eventTimerY = y; }

    public String buildTellCommand(String nickname) { return tellCommand + " " + nickname + " " + tellMessage; }

    private static float parseFloat(String s, float def) {
        try { return s == null ? def : Float.parseFloat(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    public void save() {
        try {
            Properties props = new Properties();
            props.setProperty("tellCommand", tellCommand);
            props.setProperty("tellMessage", tellMessage);
            props.setProperty("clanChatSymbol", clanChatSymbol);
            props.setProperty("showEventTimer", String.valueOf(showEventTimer));
            props.setProperty("eventTimerX", String.valueOf(eventTimerX));
            props.setProperty("eventTimerY", String.valueOf(eventTimerY));
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "EvoChat Configuration");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения конфига: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) { props.load(is); }
            tellCommand = props.getProperty("tellCommand", "m");
            tellMessage = props.getProperty("tellMessage", "1");
            clanChatSymbol = props.getProperty("clanChatSymbol", "@");
            showEventTimer = Boolean.parseBoolean(props.getProperty("showEventTimer", "true"));
            eventTimerX = parseFloat(props.getProperty("eventTimerX"), 0.004f);
            eventTimerY = parseFloat(props.getProperty("eventTimerY"), 0.01f);
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки конфига: " + e.getMessage());
        }
    }
}
