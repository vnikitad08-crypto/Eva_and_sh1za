package Grend.chatlogger.client;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ClanHighlightConfig {
    private static ClanHighlightConfig instance;
    private static final Path CONFIG_PATH = Paths.get("chatlogger_highlight_config.properties");

    private boolean highlightEnabled = true;
    // Прозрачность/сила подсветки: 0 — почти невидимо, 1 — полностью в цвет.
    private float highlightStrength = 0.5f;
    // Пользовательский цвет-переопределение для клана (необязательно).
    private final Map<String, String> clanColors = new HashMap<>();

    // Отношение к клану: "enemy" (враг), "friend" (друг) или отсутствие = не обозначено.
    private final Map<String, String> clanRelation = new HashMap<>();
    // Кланы, добавленные/видимые в списке вручную (чтобы клан без игроков не пропадал).
    private final java.util.Set<String> managedClans = new java.util.LinkedHashSet<>();

    // Цвета по умолчанию: враги — красный, друзья — зелёный (можно переопределить).
    private String enemyColor = "#FF5555";
    private String friendColor = "#55FF55";

    // Устаревшее (оставлено для совместимости со старым конфигом).
    private boolean enemyHighlightEnabled = false;
    private final java.util.Set<String> friendlyClans = new java.util.HashSet<>();

    private ClanHighlightConfig() { load(); }

    public static synchronized ClanHighlightConfig getInstance() {
        if (instance == null) instance = new ClanHighlightConfig();
        return instance;
    }

    public boolean isHighlightEnabled() { return highlightEnabled; }
    public void setHighlightEnabled(boolean enabled) {
        this.highlightEnabled = enabled;
        save();
    }

    /** Сила/прозрачность подсветки (0..1). */
    public float getHighlightStrength() { return highlightStrength; }
    public void setHighlightStrength(float v) {
        this.highlightStrength = Math.max(0f, Math.min(1f, v));
        save();
    }

    public String getClanColor(String clan) { 
        return clanColors.get(clan.toUpperCase().trim()); 
    }

    public void setClanColor(String clan, String color) { 
        clanColors.put(clan.toUpperCase().trim(), color); 
        save();
    }

    public void removeClanColor(String clan) { 
        clanColors.remove(clan.toUpperCase().trim()); 
        save();
    }

    public Map<String, String> getAllClanColors() { 
        return new HashMap<>(clanColors); 
    }

    public boolean hasClanColor(String clan) {
        return clanColors.containsKey(clan.toUpperCase().trim());
    }

    // ─── Отношения кланов (враг / друг / не обозначено) ───────────────────────

    /** Возвращает "enemy", "friend" или "none". */
    public String getClanRelation(String clan) {
        if (clan == null) return "none";
        return clanRelation.getOrDefault(clan.toUpperCase().trim(), "none");
    }

    public void setClanRelation(String clan, String rel) {
        if (clan == null) return;
        String key = clan.toUpperCase().trim();
        if (key.isEmpty()) return;
        if (rel == null || rel.equals("none")) clanRelation.remove(key);
        else clanRelation.put(key, rel);
        managedClans.add(key);
        save();
    }

    /**
     * Итоговый цвет подсветки для клана: пользовательский цвет-переопределение,
     * иначе цвет по умолчанию для отношения. null — если клан не обозначен.
     */
    public String getEffectiveColor(String clan) {
        String rel = getClanRelation(clan);
        if ("enemy".equals(rel)) {
            String c = getClanColor(clan);
            return c != null ? c : enemyColor;
        }
        if ("friend".equals(rel)) {
            String c = getClanColor(clan);
            return c != null ? c : friendColor;
        }
        return null;
    }

    public java.util.Set<String> getManagedClans() {
        return new java.util.LinkedHashSet<>(managedClans);
    }

    public void addManagedClan(String clan) {
        if (clan == null) return;
        String key = clan.toUpperCase().trim();
        if (!key.isEmpty() && managedClans.add(key)) save();
    }

    public void removeManagedClan(String clan) {
        if (clan == null) return;
        String key = clan.toUpperCase().trim();
        boolean changed = managedClans.remove(key);
        changed |= clanRelation.remove(key) != null;
        changed |= clanColors.remove(key) != null;
        if (changed) save();
    }

    public String getFriendColor() { return friendColor; }
    public void setFriendColor(String c) { this.friendColor = c; save(); }
    public String getDefaultEnemyColor() { return enemyColor; }
    public void setDefaultEnemyColor(String c) { this.enemyColor = c; save(); }

    // ─── Подсветка врагов ────────────────────────────────────────────────
    public boolean isEnemyHighlightEnabled() { return enemyHighlightEnabled; }
    public void setEnemyHighlightEnabled(boolean v) { this.enemyHighlightEnabled = v; save(); }

    public String getEnemyColor() { return enemyColor; }
    public void setEnemyColor(String color) { this.enemyColor = color; save(); }

    public boolean isFriendlyClan(String clan) {
        if (clan == null) return false;
        return friendlyClans.contains(clan.toUpperCase().trim());
    }

    public void addFriendlyClan(String clan) {
        friendlyClans.add(clan.toUpperCase().trim());
        save();
    }

    public void removeFriendlyClan(String clan) {
        friendlyClans.remove(clan.toUpperCase().trim());
        save();
    }

    public java.util.Set<String> getFriendlyClans() {
        return new java.util.HashSet<>(friendlyClans);
    }

    public void save() {
        try {
            Properties props = new Properties();
            props.setProperty("highlightEnabled", String.valueOf(highlightEnabled));
            props.setProperty("highlightStrength", String.valueOf(highlightStrength));
            props.setProperty("enemyColor", enemyColor);
            props.setProperty("friendColor", friendColor);
            props.setProperty("managedClans", String.join(",", managedClans));
            for (Map.Entry<String, String> entry : clanColors.entrySet()) {
                props.setProperty("clan.color." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : clanRelation.entrySet()) {
                props.setProperty("clan.rel." + entry.getKey(), entry.getValue());
            }
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "EvoChat Highlight Configuration");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения конфига подсветки: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) { props.load(is); }
            highlightEnabled = Boolean.parseBoolean(props.getProperty("highlightEnabled", "true"));
            try {
                highlightStrength = Math.max(0f, Math.min(1f,
                        Float.parseFloat(props.getProperty("highlightStrength", "0.5"))));
            } catch (NumberFormatException ex) {
                highlightStrength = 0.5f;
            }
            enemyColor = props.getProperty("enemyColor", "#FF5555");
            friendColor = props.getProperty("friendColor", "#55FF55");

            managedClans.clear();
            String managed = props.getProperty("managedClans", "");
            if (!managed.isEmpty()) {
                for (String c : managed.split(",")) {
                    String t = c.trim();
                    if (!t.isEmpty()) managedClans.add(t.toUpperCase());
                }
            }

            clanColors.clear();
            clanRelation.clear();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("clan.color.")) {
                    clanColors.put(key.substring("clan.color.".length()).toUpperCase(), props.getProperty(key));
                } else if (key.startsWith("clan.rel.")) {
                    String clan = key.substring("clan.rel.".length()).toUpperCase();
                    clanRelation.put(clan, props.getProperty(key));
                    managedClans.add(clan);
                }
            }

            // Миграция старого списка дружественных кланов в отношения.
            String friendly = props.getProperty("friendlyClans", "");
            if (!friendly.isEmpty()) {
                for (String c : friendly.split(",")) {
                    String t = c.trim().toUpperCase();
                    if (!t.isEmpty() && !clanRelation.containsKey(t)) {
                        clanRelation.put(t, "friend");
                        managedClans.add(t);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки конфига подсветки: " + e.getMessage());
        }
    }

    /**
     * Преобразует название цвета в hex-формат
     */
    public static String normalizeColor(String color) {
        String lower = color.toLowerCase().trim();
        return switch (lower) {
            case "white", "белый" -> "#FFFFFF";
            case "red", "красный" -> "#FF0000";
            case "green", "зелёный", "зеленый" -> "#00FF00";
            case "blue", "синий" -> "#0000FF";
            case "yellow", "жёлтый", "желтый" -> "#FFFF00";
            case "cyan", "голубой" -> "#00FFFF";
            case "magenta", "фиолетовый" -> "#FF00FF";
            case "black", "чёрный", "черный" -> "#000000";
            case "gray", "серый" -> "#808080";
            case "orange", "оранжевый" -> "#FFA500";
            case "pink", "розовый" -> "#FFC0CB";
            case "purple", "пурпурный" -> "#800080";
            case "lime", "лайм" -> "#00FF00";
            case "teal", "бирюзовый" -> "#008080";
            case "navy", "тёмно-синий", "темно-синий" -> "#000080";
            case "brown", "коричневый" -> "#A52A2A";
            case "gold", "золотой" -> "#FFD700";
            case "silver", "серебряный" -> "#C0C0C0";
            default -> {
                // Проверяем, является ли строка hex-цветом
                if (color.matches("^#?[0-9A-Fa-f]{6}$")) {
                    yield color.startsWith("#") ? color : "#" + color;
                }
                yield "#FFFFFF"; // цвет по умолчанию
            }
        };
    }

    /**
     * Преобразует hex-цвет в RGB массив [r, g, b]
     */
    public static float[] hexToRgb(String hex) {
        if (hex == null || !hex.matches("^#?[0-9A-Fa-f]{6}$")) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(cleanHex.substring(0, 2), 16);
        int g = Integer.parseInt(cleanHex.substring(2, 4), 16);
        int b = Integer.parseInt(cleanHex.substring(4, 6), 16);
        return new float[]{r / 255.0f, g / 255.0f, b / 255.0f};
    }
}
