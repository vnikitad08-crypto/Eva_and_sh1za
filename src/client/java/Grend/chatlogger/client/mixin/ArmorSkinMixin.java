package Grend.chatlogger.client.mixin;

import Grend.chatlogger.client.SkinConfig;
import Grend.chatlogger.client.SkinRenderContext;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Клиентский скин брони: подменяем equipment-ассет (assetId) при отрисовке
 * надетой брони. Сервер задаёт свой ассет, а мы показываем выбранный скин из
 * ресурспака (assets/minecraft/equipment/<имя>.json).
 *
 * Точка входа: ArmorFeatureRenderer#renderArmor берёт EquippableComponent#assetId()
 * и передаёт его в EquipmentRenderer. Перенаправляем этот вызов.
 *
 * Только на тебе: если рисуется другой игрок (SkinRenderContext.playerIsSelf == FALSE),
 * оставляем оригинальный ассет. Миксин необязательный (require=0): если сигнатура
 * в другой версии маппинга не совпадёт — просто не применится, без краша.
 */
@Mixin(ArmorFeatureRenderer.class)
public class ArmorSkinMixin {

    @Redirect(
            method = "renderArmor",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/component/type/EquippableComponent;assetId()Ljava/util/Optional;"),
            require = 0)
    private Optional<RegistryKey<EquipmentAsset>> chatlogger$reskinArmor(EquippableComponent component) {
        Optional<RegistryKey<EquipmentAsset>> original = component.assetId();
        try {
            Boolean self = SkinRenderContext.playerIsSelf;
            if (self != null && !self) return original;      // другой игрок — не трогаем

            SkinConfig cfg = SkinConfig.getInstance();
            if (!cfg.isArmorEnabled() || original.isEmpty()) return original;

            Identifier id = Identifier.tryParse(cfg.getArmorAssetId());
            if (id == null) return original;
            return Optional.of(RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, id));
        } catch (Throwable t) {
            return original;
        }
    }
}
