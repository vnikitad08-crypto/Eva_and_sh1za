package com.evochat;

import com.evochat.chat.EvoChatWindow;
import com.evochat.config.EvoConfig;
import com.evochat.gui.AutoMessageWidget;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client entry-point for EvoChat.
 *
 * Responsibilities:
 *  1. Register the ] / ъ key-binding (GLFW_KEY_RIGHT_BRACKET = 93)
 *  2. Hook into HUD rendering to draw the EvoChatWindow overlay
 *  3. On each tick check if the key was pressed → open settings screen
 */
public class EvoChatClient implements ClientModInitializer {

    public static KeyBinding toggleAutoMessageKey;
    public static KeyBinding scrollUpKey;
    public static KeyBinding scrollDownKey;
    private static long lastAutoMessageTime = 0L;
    private static boolean windowOpacityApplied = false;

    /** Minimum allowed window opacity so the game can never become fully invisible. */
    public static final float MIN_WINDOW_OPACITY = 0.2f;

    /**
     * Applies whole-window transparency to the Minecraft OS window via GLFW.
     * Value is clamped to [MIN_WINDOW_OPACITY, 1.0]. On platforms that don't
     * support per-window opacity (e.g. some Wayland setups) this is a no-op.
     */
    public static void applyWindowOpacity(float opacity) {
        float clamped = Math.max(MIN_WINDOW_OPACITY, Math.min(1.0f, opacity));
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc == null || mc.getWindow() == null) return;
            long handle = mc.getWindow().getHandle();
            if (handle != 0L) {
                GLFW.glfwSetWindowOpacity(handle, clamped);
            }
        } catch (Throwable t) {
            System.err.println("[EvoChat] Не удалось изменить прозрачность окна: " + t.getMessage());
        }
    }

    @Override
    public void onInitializeClient() {
        // Load config early
        EvoConfig.load();

        // ── Key binding ────────────────────────────────────────────────────
        // Открытие настроек EvoChat теперь только через главное меню мода (K).
        // Клавиша ] удалена. Здесь остаётся лишь бинд автосообщения.

        // Toggle auto-message on/off
        toggleAutoMessageKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.evochat.toggle_auto_message",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.evochat"
        ));

        // Scroll EvoChat history up/down
        scrollUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.evochat.scroll_up",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_UP,
                "category.evochat"
        ));
        scrollDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.evochat.scroll_down",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_DOWN,
                "category.evochat"
        ));

        // ── HUD overlay ────────────────────────────────────────────────────
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float delta = tickCounter.getTickDelta(true);
            EvoChatWindow.getInstance().render(drawContext, delta);
            AutoMessageWidget.render(drawContext, delta);
        });


        // ── Tick: handle key press ─────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Apply the saved window opacity once, after the window exists.
            if (!windowOpacityApplied) {
                applyWindowOpacity(EvoConfig.get().windowOpacity);
                windowOpacityApplied = true;
            }

            while (toggleAutoMessageKey.wasPressed()) {
                EvoConfig cfgLocal = EvoConfig.get();
                cfgLocal.autoMessageEnabled = !cfgLocal.autoMessageEnabled;
                cfgLocal.save();
            }

            while (scrollUpKey.wasPressed()) {
                EvoChatWindow.getInstance().scroll(3);
            }
            while (scrollDownKey.wasPressed()) {
                EvoChatWindow.getInstance().scroll(-3);
            }

            // Auto-message ticking
            EvoConfig cfgLocal = EvoConfig.get();
            if (cfgLocal.autoMessageEnabled
                    && client.player != null
                    && client.world != null
                    && cfgLocal.autoMessageText != null
                    && !cfgLocal.autoMessageText.isEmpty()) {
                long now = System.currentTimeMillis();
                long intervalMs = Math.max(1, cfgLocal.autoMessageIntervalSeconds) * 1000L;
                if (now - lastAutoMessageTime >= intervalMs) {
                    client.player.networkHandler.sendChatMessage(cfgLocal.autoMessageText);
                    lastAutoMessageTime = now;
                }
            }
        });

        System.out.println("[EvoChat] Initialized. Открывается через меню мода (K).");
    }
}
