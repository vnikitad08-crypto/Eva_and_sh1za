package Grend.chatlogger.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import Grend.chatlogger.client.ClanHighlightConfig;
import Grend.chatlogger.client.SkinRenderContext;
import Grend.chatlogger.data.DataManager;
import Grend.chatlogger.data.PlayerData;

/**
 * Подсветка игроков по отношению их клана (враг/друг).
 *
 * Красим ВСЮ модель игрока — тело, броню и остальные слои — оборачивая
 * провайдер буферов вершин на время render(). Цвет подмешивается к вершинам,
 * тест глубины обычный, поэтому подсветка НЕ видна сквозь стены и выглядит как
 * мягкое цветное свечение, а не как яркий контур.
 */
@Mixin(LivingEntityRenderer.class)
public class PlayerHighlightMixin<L extends LivingEntity, S extends LivingEntityRenderState> {

    // Сущность, которую сейчас готовят к отрисовке (updateRenderState вызывается
    // прямо перед render). Идентифицируем игрока по самой сущности.
    private static LivingEntity chatlogger$capturedEntity = null;

    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
        at = @At("HEAD"), require = 0
    )
    private void chatlogger$captureEntity(LivingEntity entity,
                                          LivingEntityRenderState state,
                                          float tickDelta, CallbackInfo ci) {
        chatlogger$capturedEntity = entity;
    }

    // Флаг «рисуем самого клиента» — нужен скинам брони (ArmorSkinMixin).
    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void chatlogger$beforeRender(S state,
                                         net.minecraft.client.util.math.MatrixStack matrixStack,
                                         VertexConsumerProvider vertexConsumerProvider,
                                         int i, CallbackInfo ci) {
        try {
            MinecraftClient self = MinecraftClient.getInstance();
            SkinRenderContext.playerIsSelf = (self != null && self.player != null
                    && chatlogger$capturedEntity == self.player);
        } catch (Throwable ignored) {
            SkinRenderContext.playerIsSelf = null;
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("RETURN")
    )
    private void chatlogger$afterRender(S state,
                                        net.minecraft.client.util.math.MatrixStack matrixStack,
                                        VertexConsumerProvider vertexConsumerProvider,
                                        int i, CallbackInfo ci) {
        SkinRenderContext.playerIsSelf = null;
    }

    // Оборачиваем провайдер, чтобы тонировать все слои модели (тело + броня).
    @ModifyVariable(
        method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"), argsOnly = true
    )
    private VertexConsumerProvider chatlogger$tint(VertexConsumerProvider provider) {
        try {
            float[] color = chatlogger$highlightColor();
            if (color != null) {
                float strength = ClanHighlightConfig.getInstance().getHighlightStrength();
                // Ползунок на максимуме прозрачности — подсветки нет вовсе.
                if (strength > 0.02f) {
                    return new Grend.chatlogger.client.TintVertexConsumerProvider(
                            provider, color[0], color[1], color[2], strength);
                }
            }
        } catch (Throwable ignored) {
            // подсветка не должна ломать рендер
        }
        return provider;
    }

    private static float[] chatlogger$highlightColor() {
        ClanHighlightConfig config = ClanHighlightConfig.getInstance();
        if (!config.isHighlightEnabled()) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return null;
        if (!(chatlogger$capturedEntity instanceof PlayerEntity targetPlayer)) return null;
        if (targetPlayer == mc.player) return null;

        PlayerData data = DataManager.getInstance().getPlayer(targetPlayer.getName().getString());
        if (data == null) return null;

        String hex = config.getEffectiveColor(data.getClan());
        if (hex == null) return null;
        return ClanHighlightConfig.hexToRgb(hex);
    }
}
