package com.evochat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class EvoConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("evochat.json");

    private static EvoConfig instance;

    // Chat window position (0.0 to 1.0 = relative to screen)
    public float chatX = 0.7f;
    public float chatY = 0.05f;

    // Chat window dimensions
    public int chatWidth = 320;
    public int chatHeight = 200;

    // Chat opacity 0.0 = fully transparent, 1.0 = fully opaque
    public float chatOpacity = 0.85f;

    // Background opacity (panel behind messages)
    public float backgroundOpacity = 0.65f;

    // Opacity of the whole Minecraft OS window (1.0 = opaque, lower = see-through desktop).
    // Applied via GLFW.glfwSetWindowOpacity. Clamped to a safe minimum so the game
    // can never be made fully invisible.
    public float windowOpacity = 1.0f;

    // Sound enabled
    public boolean soundEnabled = true;
    // Notification sound volume (0.0 - 1.0), independent from Minecraft volume
    public float soundVolume = 0.7f;

    // Maximum stored messages
    public int maxMessages = 100;

    // Font scale factor
    public float fontScale = 1.0f;

    // Window visible
    public boolean windowVisible = true;

    // ─── Auto-message settings ───────────────────────────────────────────────
    // Whether auto-message is enabled
    public boolean autoMessageEnabled = false;
    // Interval between messages in seconds
    public int autoMessageIntervalSeconds = 60;
    // Text of the auto-message
    public String autoMessageText = "";
    // HUD widget visibility and position (relative 0.0–1.0)
    public boolean autoMessageWidgetVisible = true;
    // Defaults approximately match the position on the provided screenshot
    public float autoMessageWidgetX = 0.80f;
    public float autoMessageWidgetY = 0.74f;

    public static EvoConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static EvoConfig load() {
        File file = CONFIG_FILE.toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                EvoConfig loaded = GSON.fromJson(reader, EvoConfig.class);
                if (loaded != null) {
                    // Migrate old default widget position to the new one if user
                    // hasn't moved it manually.
                    if (Math.abs(loaded.autoMessageWidgetX - 0.02f) < 0.0001f
                            && Math.abs(loaded.autoMessageWidgetY - 0.90f) < 0.0001f) {
                        loaded.autoMessageWidgetX = 0.80f;
                        loaded.autoMessageWidgetY = 0.74f;
                    }
                    instance = loaded;
                    return instance;
                }
            } catch (Exception e) {
                System.err.println("[EvoChat] Failed to load config: " + e.getMessage());
            }
        }
        instance = new EvoConfig();
        instance.save();
        return instance;
    }

    public void save() {
        try {
            File file = CONFIG_FILE.toFile();
            file.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[EvoChat] Failed to save config: " + e.getMessage());
        }
    }
}
