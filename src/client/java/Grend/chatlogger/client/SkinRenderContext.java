package Grend.chatlogger.client;

/**
 * Контекст рендера для скинов: показывает, чью сущность сейчас рисуют.
 *
 * playerIsSelf:
 *   null  — рендер вне живой сущности (GUI, предмет от первого лица) → скин применяем;
 *   TRUE  — рендерится сам клиент (ты) → применяем;
 *   FALSE — рендерится другой игрок/моб → НЕ применяем (скин только на тебе).
 *
 * Флаг выставляет PlayerHighlightMixin вокруг LivingEntityRenderer.render,
 * а читают ItemSkinMixin и ArmorSkinMixin.
 */
public final class SkinRenderContext {
    public static Boolean playerIsSelf = null;
    private SkinRenderContext() {}
}
