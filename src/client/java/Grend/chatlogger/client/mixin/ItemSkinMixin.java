package Grend.chatlogger.client.mixin;

import Grend.chatlogger.client.SkinConfig;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Клиентская подмена скина оружия/посоха при рендере.
 *
 * Механика: при построении модели предмета подменяем item_model (меч) или
 * custom_model_data (посох) на выбранный скин. Видно ТОЛЬКО на этом клиенте.
 *
 * ВАЖНО (только у тебя): в 1.21.4 модель предмета в руке сущности строится
 * в updateRenderState — ДО render(), поэтому старый флаг SkinRenderContext был
 * ненадёжен и скин мог применяться ко всем игрокам. Теперь держателя берём
 * напрямую из аргумента метода: скин применяется, только если предмет держит
 * сам клиент (или держателя нет — GUI/рамка/дроп на твоём экране).
 *
 * ⚠ Конфиг миксина required=false / require=0: при ином маппинге просто не
 * применится, без краша.
 */
@Mixin(ItemModelManager.class)
public class ItemSkinMixin {

    // Методы с живым держателем: применяем скин ТОЛЬКО если держатель — ты сам.
    @ModifyVariable(
            method = { "update", "updateForLivingEntity" },
            at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack chatlogger$applyForLiving(ItemStack stack, @Local(argsOnly = true) LivingEntity holder) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (holder != null && holder != mc.player) return stack; // другой игрок — не трогаем
        return chatlogger$apply(stack);
    }

    // Предмет без живого держателя (GUI, рамка, дроп) — это твой экран, показываем.
    @ModifyVariable(
            method = "updateForNonLivingEntity",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack chatlogger$applyForNonLiving(ItemStack stack) {
        return chatlogger$apply(stack);
    }

    @Unique
    private ItemStack chatlogger$apply(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) return stack;

            SkinConfig cfg = SkinConfig.getInstance();

            // Меч → item_model = minecraft:skins/<имя>. Призма имеет приоритет.
            if (chatlogger$isSword(stack)) {
                String swordModelId = null;
                if (cfg.isPrismaticEnabled()) {
                    swordModelId = cfg.getPrismaticItemModelId();
                } else if (cfg.isSwordEnabled()) {
                    swordModelId = cfg.getSwordItemModelId();
                }
                if (swordModelId != null) {
                    Identifier id = Identifier.tryParse(swordModelId);
                    if (id != null) {
                        ItemStack copy = stack.copy();
                        copy.set(DataComponentTypes.ITEM_MODEL, id);
                        return copy;
                    }
                }
            }

        } catch (Throwable ignored) {
            // косметика не должна ломать рендер
        }
        return stack;
    }

    @Unique
    private static boolean chatlogger$isSword(ItemStack s) {
        return s.isOf(Items.WOODEN_SWORD) || s.isOf(Items.STONE_SWORD)
                || s.isOf(Items.IRON_SWORD) || s.isOf(Items.GOLDEN_SWORD)
                || s.isOf(Items.DIAMOND_SWORD) || s.isOf(Items.NETHERITE_SWORD);
    }
}
