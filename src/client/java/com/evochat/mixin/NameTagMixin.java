package com.evochat.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import Grend.chatlogger.client.NicknameConfig;

/**
 * Заменяет ник над головой сущности (nametag) на пользовательский.
 *
 * Точка входа — EntityRenderer#renderLabelIfPresent. Для игроков
 * PlayerEntityRenderer переопределяет метод, но вызывает super, поэтому
 * реально исполняется именно этот метод — и подмена аргумента-текста работает.
 */
@Mixin(EntityRenderer.class)
public class NameTagMixin {

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text evochat$renameLabel(Text text) {
        try {
            return NicknameConfig.getInstance().replaceInText(text);
        } catch (Throwable t) {
            return text;
        }
    }
}
