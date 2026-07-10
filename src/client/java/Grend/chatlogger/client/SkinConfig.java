package Grend.chatlogger.client;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Конфиг клиентского выбора скинов для оружия/посохов DiamondWorld.
 *
 * Мечи: скин задаётся компонентом item_model = "minecraft:skins/&lt;имя&gt;".
 *
 * ВАЖНО: это чисто визуально и только на твоём клиенте. Сервер и другие
 * игроки видят то, что выдал сервер. Скин не выдаётся и не сохраняется на сервере.
 */
public class SkinConfig {

    private static SkinConfig instance;
    private static final Path CONFIG_PATH = Paths.get("chatlogger_skins.properties");

    // Список скинов мечей (item_model = minecraft:skins/<имя>)
    public static final String[] SWORD_SKINS = {
            "ancientsaber", "axe", "cursed_sword", "deaths_dance", "deaths_dance_anim",
            "endersword", "firesword", "flamedeath", "forgedhammer", "god_blood",
            "hammer", "icicle", "jadehammer", "katana", "legendary",
            "rubykatana", "shadowsword", "steel_club", "trident", "watcher"
    };

    // Призматический меч: 12 тем, каждая улучшается по стадиям 1 → 2 → 2_3 → 3.
    // item_model = minecraft:skins/<тема>_<стадия>. Модели лежат в
    // prisonevo/skins/prismatic/<тема>_<стадия>.
    public static final String[] PRISMATIC_THEMES = {
            "crystal_energy", "crystal_fire", "crystal_thunder",
            "cyber_dawn", "cyber_shadow", "cyber_space",
            "nature_bronze", "nature_flower", "nature_magic",
            "wind_druid", "wind_mountain", "wind_void"
    };
    public static final String[] PRISMATIC_STAGES = { "1", "2", "2_3", "3" };

    // Скины брони: id equipment-ассета (assets/minecraft/equipment/<имя>.json).
    // Применяется ко всей надетой броне сразу (шлем/нагрудник/поножи/ботинки).
    public static final String[] ARMOR_SKINS = {
            "archangel", "bloodyeye", "cast", "cursed", "druid", "ender",
            "enderdragon", "energy", "fogguard", "ghast", "glaucus", "hydra",
            "ironguard", "knight", "legion", "paladin", "phoenix", "rome",
            "scorpion", "shadow", "space", "twofaces", "warden", "wendigo",
            "winter", "wither"
    };

    // ─── Состояние ───────────────────────────────────────────────────────
    private boolean swordEnabled = false;
    private String swordSkin = "katana";     // имя из SWORD_SKINS
    // Призматический меч (имеет приоритет над обычным скином меча).
    private boolean prismaticEnabled = false;
    private String prismaticTheme = "crystal_energy";
    private String prismaticStage = "1";
    // Скин брони.
    private boolean armorEnabled = false;
    private String armorSkin = "archangel";

    private SkinConfig() { load(); }

    public static synchronized SkinConfig getInstance() {
        if (instance == null) instance = new SkinConfig();
        return instance;
    }

    public boolean isSwordEnabled() { return swordEnabled; }
    public void setSwordEnabled(boolean v) { this.swordEnabled = v; save(); }
    public String getSwordSkin() { return swordSkin; }
    public void setSwordSkin(String s) { this.swordSkin = s; save(); }

    /** item_model-идентификатор для выбранного скина меча. */
    public String getSwordItemModelId() {
        return "minecraft:skins/" + swordSkin;
    }

    // ─── Призматический меч ───────────────────────────────────────────────
    public boolean isPrismaticEnabled() { return prismaticEnabled; }
    public void setPrismaticEnabled(boolean v) { this.prismaticEnabled = v; save(); }
    public String getPrismaticTheme() { return prismaticTheme; }
    public void setPrismaticTheme(String s) { this.prismaticTheme = s; save(); }
    public String getPrismaticStage() { return prismaticStage; }
    public void setPrismaticStage(String s) { this.prismaticStage = s; save(); }

    /** Имя скина = тема_стадия. В паке опечатка: у crystal_fire стадия 1 = crystal_firer_1. */
    public String getPrismaticSkinName() {
        if ("crystal_fire".equals(prismaticTheme) && "1".equals(prismaticStage)) {
            return "crystal_firer_1";
        }
        return prismaticTheme + "_" + prismaticStage;
    }

    public String getPrismaticItemModelId() {
        return "minecraft:skins/" + getPrismaticSkinName();
    }

    // ─── Скин брони ───────────────────────────────────────────────────────
    public boolean isArmorEnabled() { return armorEnabled; }
    public void setArmorEnabled(boolean v) { this.armorEnabled = v; save(); }
    public String getArmorSkin() { return armorSkin; }
    public void setArmorSkin(String s) { this.armorSkin = s; save(); }
    /** id equipment-ассета для выбранного скина брони. */
    public String getArmorAssetId() { return "minecraft:" + armorSkin; }

    public void save() {
        try {
            Properties p = new Properties();
            p.setProperty("swordEnabled", String.valueOf(swordEnabled));
            p.setProperty("swordSkin", swordSkin);
            p.setProperty("prismaticEnabled", String.valueOf(prismaticEnabled));
            p.setProperty("prismaticTheme", prismaticTheme);
            p.setProperty("prismaticStage", prismaticStage);
            p.setProperty("armorEnabled", String.valueOf(armorEnabled));
            p.setProperty("armorSkin", armorSkin);
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                p.store(os, "EvoChat Skin Selector");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения конфига скинов: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try {
            Properties p = new Properties();
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) { p.load(is); }
            swordEnabled = Boolean.parseBoolean(p.getProperty("swordEnabled", "false"));
            swordSkin = p.getProperty("swordSkin", "katana");
            prismaticEnabled = Boolean.parseBoolean(p.getProperty("prismaticEnabled", "false"));
            prismaticTheme = p.getProperty("prismaticTheme", "crystal_energy");
            prismaticStage = p.getProperty("prismaticStage", "1");
            armorEnabled = Boolean.parseBoolean(p.getProperty("armorEnabled", "false"));
            armorSkin = p.getProperty("armorSkin", "archangel");
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки конфига скинов: " + e.getMessage());
        }
    }
}
