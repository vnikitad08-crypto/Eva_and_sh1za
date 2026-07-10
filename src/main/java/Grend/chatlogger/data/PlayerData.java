package Grend.chatlogger.data;

/**
 * Класс для хранения данных об игроке
 */
public class PlayerData {
    private String nickname;
    private String clan;
    private int level;
    private boolean isOnline;
    private long lastSeenTimestamp;

    public PlayerData(String nickname, String clan, int level) {
        this.nickname = nickname;
        this.clan = clan;
        this.level = level;
        this.isOnline = false; // По умолчанию игрок оффлайн при загрузке
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    public String getNickname() {
        return nickname;
    }

    public String getClan() {
        return clan;
    }

    public void setClan(String clan) {
        this.clan = clan;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public void setLastSeenTimestamp(long lastSeenTimestamp) {
        this.lastSeenTimestamp = lastSeenTimestamp;
    }

    /**
     * Обновляет данные игрока (если изменился клан или уровень)
     * НЕ меняет статус онлайн!
     */
    public void update(String newClan, int newLevel) {
        if (!this.clan.equals(newClan)) {
            this.clan = newClan;
        }
        if (this.level != newLevel) {
            this.level = newLevel;
        }
        // Не меняем isOnline - статус должен обновляться только через setOnline()
    }

    /**
     * Помечает игрока как оффлайн
     */
    public void markOffline() {
        this.isOnline = false;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] [%d] - %s", 
            nickname, clan, level, isOnline ? "онлайн" : "оффлайн");
    }
}
