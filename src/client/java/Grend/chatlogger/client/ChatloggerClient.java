package Grend.chatlogger.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import Grend.chatlogger.client.commands.ModCommands;
import Grend.chatlogger.data.DataManager;

public class ChatloggerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ModCommands.register(dispatcher);
        });

        ModKeyBindings.register();
        
        // Регистрируем обработчик тиков
        TickHandler.register();

        DataManager.getInstance().load();
        ModConfig.getInstance();
        RunesConfig.getInstance();

        // HUD-таймер ивентов DiamondWorld (можно выключить в настройках).
        // Позиция берётся из ModConfig и настраивается в редакторе виджетов.
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            if (mc.options.hudHidden) return;
            if (!ModConfig.getInstance().isShowEventTimer()) return;
            if (mc.getWindow() == null) return;

            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            int w = EventTimerHud.width(mc.textRenderer);
            int h = EventTimerHud.height();

            int x = Math.round(ModConfig.getInstance().getEventTimerX() * sw);
            int y = Math.round(ModConfig.getInstance().getEventTimerY() * sh);
            x = Math.max(0, Math.min(sw - w, x));
            y = Math.max(0, Math.min(sh - h, y));

            EventTimerHud.render(ctx, x, y);
        });


        DataManager manager = DataManager.getInstance();
        if (manager.getPlayerCount() > 0) {
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("§a[EvoChat] Мод активен! Загружено " + manager.getPlayerCount() + " игроков."),
                        false
                    );
                }
            });
        } else {
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("§a[EvoChat] Мод активен! Ожидание сообщений в чате..."),
                        false
                    );
                }
            });
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            DataManager.getInstance().save();
            RunesConfig.getInstance().save();
        });
    }
}
