package Grend.chatlogger.client;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Конфигурация для сетов рун
 * 5 сетов, каждый сет содержит 6 слотов (руны)
 */
public class RunesConfig {
    private static RunesConfig instance;
    private static final Path CONFIG_PATH = Paths.get("chatlogger_runes_config.properties");

    // 5 сетов, каждый с 6 слотами
    // Слоты: 0=первый, 1=второй, ..., 5=шестой
    private String[][] runeSets;
    private int[] setKeyCodes;
    private boolean[] enabled;

    private RunesConfig() {
        runeSets = new String[5][6];
        setKeyCodes = new int[5];
        enabled = new boolean[5];
        
        // Инициализация значениями по умолчанию
        for (int i = 0; i < 5; i++) {
            enabled[i] = true;
            setKeyCodes[i] = 0; // По умолчанию нет клавиш
            for (int j = 0; j < 6; j++) {
                runeSets[i][j] = "";
            }
        }
        
        load();
    }

    public static synchronized RunesConfig getInstance() {
        if (instance == null) instance = new RunesConfig();
        return instance;
    }

    /**
     * Получить название руны в указанном сете и слоте
     */
    public String getRune(int setIndex, int slotIndex) {
        if (setIndex < 0 || setIndex >= 5 || slotIndex < 0 || slotIndex >= 6) {
            return "";
        }
        return runeSets[setIndex][slotIndex];
    }

    /**
     * Установить руну в указанный сет и слот
     */
    public void setRune(int setIndex, int slotIndex, String runeName) {
        if (setIndex < 0 || setIndex >= 5 || slotIndex < 0 || slotIndex >= 6) {
            return;
        }
        runeSets[setIndex][slotIndex] = runeName;
        save();
    }

    /**
     * Получить весь сет рун
     */
    public String[] getRuneSet(int setIndex) {
        if (setIndex < 0 || setIndex >= 5) {
            return new String[6];
        }
        return runeSets[setIndex].clone();
    }

    /**
     * Установить весь сет рун
     */
    public void setRuneSet(int setIndex, String[] runes) {
        if (setIndex < 0 || setIndex >= 5 || runes.length != 6) {
            return;
        }
        runeSets[setIndex] = runes.clone();
        save();
    }

    /**
     * Получить клавишу для сета
     */
    public int getSetKeyCode(int setIndex) {
        if (setIndex < 0 || setIndex >= 5) {
            return 0;
        }
        return setKeyCodes[setIndex];
    }

    /**
     * Установить клавишу для сета
     */
    public void setSetKeyCode(int setIndex, int keyCode) {
        if (setIndex < 0 || setIndex >= 5) {
            return;
        }
        setKeyCodes[setIndex] = keyCode;
        save();
    }

    /**
     * Проверить, включен ли сет
     */
    public boolean isEnabled(int setIndex) {
        if (setIndex < 0 || setIndex >= 5) {
            return false;
        }
        return enabled[setIndex];
    }

    /**
     * Включить/выключить сет
     */
    public void setEnabled(int setIndex, boolean enabled) {
        if (setIndex < 0 || setIndex >= 5) {
            return;
        }
        this.enabled[setIndex] = enabled;
        save();
    }

    /**
     * Переключить состояние сета
     */
    public void toggleEnabled(int setIndex) {
        if (setIndex >= 0 && setIndex < 5) {
            enabled[setIndex] = !enabled[setIndex];
            save();
        }
    }

    public void save() {
        try {
            Properties props = new Properties();
            
            for (int i = 0; i < 5; i++) {
                props.setProperty("set." + i + ".enabled", String.valueOf(enabled[i]));
                props.setProperty("set." + i + ".keyCode", String.valueOf(setKeyCodes[i]));
                
                for (int j = 0; j < 6; j++) {
                    String rune = runeSets[i][j];
                    if (!rune.isEmpty()) {
                        props.setProperty("set." + i + ".rune." + j, rune);
                    }
                }
            }
            
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "EvoChat Runes Configuration");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения конфига рун: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        
        try {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) {
                props.load(is);
            }
            
            for (int i = 0; i < 5; i++) {
                enabled[i] = Boolean.parseBoolean(props.getProperty("set." + i + ".enabled", "true"));
                setKeyCodes[i] = Integer.parseInt(props.getProperty("set." + i + ".keyCode", "0"));
                
                for (int j = 0; j < 6; j++) {
                    runeSets[i][j] = props.getProperty("set." + i + ".rune." + j, "");
                }
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки конфига рун: " + e.getMessage());
        }
    }
}
