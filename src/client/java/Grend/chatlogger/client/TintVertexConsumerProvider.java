package Grend.chatlogger.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Провайдер буферов вершин для подсветки игрока (тело + броня).
 *
 * Живёт в обычном пакете (НЕ в *.mixin), иначе Mixin запрещает загрузку такого
 * класса в рантайме (IllegalClassLoadError).
 *
 * Два режима по силе подсветки (blend):
 *  • обычный — цвет подмешивается к исходному цвету модели (мягкий тон),
 *    яркость растёт с силой (эмиссивность);
 *  • максимум (blend ≈ 1) — рендер модели перенаправляется на слой с белой
 *    текстурой, поэтому КАЖДЫЙ пиксель становится чистым выбранным цветом
 *    (текстура игрока больше не влияет — ровный сплошной силуэт). Тест глубины
 *    обычный, поэтому подсветка не видна сквозь стены.
 */
public class TintVertexConsumerProvider implements VertexConsumerProvider {

    // Порог, с которого включается сплошная заливка (ползунок на максимуме).
    private static final float SOLID_THRESHOLD = 0.995f;

    private final VertexConsumerProvider parent;
    private final float r, g, b, blend;

    public TintVertexConsumerProvider(VertexConsumerProvider parent, float r, float g, float b, float blend) {
        this.parent = parent;
        this.r = r;
        this.g = g;
        this.b = b;
        this.blend = Math.max(0f, Math.min(1f, blend));
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        // На максимуме — перенаправляем геометрию модели (формат сущности) на
        // слой с белой текстурой: каждый пиксель станет ровно цветом подсветки.
        // Текст (ник над головой) имеет другой формат — его не трогаем.
        if (blend >= SOLID_THRESHOLD
                && layer.getVertexFormat() == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
            RenderLayer solid = solidLayer();
            if (solid != null) {
                return new TintVertexConsumer(parent.getBuffer(solid), r, g, b, blend);
            }
        }
        return new TintVertexConsumer(parent.getBuffer(layer), r, g, b, blend);
    }

    // ─── Белая текстура + сплошной слой ──────────────────────────────────────
    private static final Identifier WHITE_ID = Identifier.of("evochat", "highlight_white");
    private static boolean whiteReady = false;
    private static boolean whiteFailed = false;
    private static RenderLayer solidLayerCache = null;

    private static RenderLayer solidLayer() {
        if (!ensureWhiteTexture()) return null;
        if (solidLayerCache == null) {
            solidLayerCache = RenderLayer.getEntityCutoutNoCull(WHITE_ID);
        }
        return solidLayerCache;
    }

    /** Лениво создаёт и регистрирует белую 2×2 текстуру (один раз). */
    private static boolean ensureWhiteTexture() {
        if (whiteReady) return true;
        if (whiteFailed) return false;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getTextureManager() == null) return false;
            NativeImage img = new NativeImage(2, 2, false);
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    img.setColorArgb(x, y, 0xFFFFFFFF);
                }
            }
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            tex.upload();
            mc.getTextureManager().registerTexture(WHITE_ID, tex);
            whiteReady = true;
            return true;
        } catch (Throwable t) {
            whiteFailed = true;
            return false;
        }
    }

    /** Потребитель вершин: смешивает исходный цвет с цветом подсветки. */
    private static final class TintVertexConsumer implements VertexConsumer {
        private final float blend;
        private final VertexConsumer parent;
        private final float r, g, b;

        TintVertexConsumer(VertexConsumer parent, float r, float g, float b, float blend) {
            this.parent = parent;
            this.r = r;
            this.g = g;
            this.b = b;
            this.blend = blend;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return parent.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            int nr = clamp255((int) (red * (1 - blend) + r * 255f * blend));
            int ng = clamp255((int) (green * (1 - blend) + g * 255f * blend));
            int nb = clamp255((int) (blue * (1 - blend) + b * 255f * blend));
            return parent.color(nr, ng, nb, alpha);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return parent.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return parent.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            // Чем сильнее подсветка, тем «эмиссивнее» модель: на максимуме игрок
            // полностью в цвете (светится) даже в тени. Полная яркость = 240/канал.
            int nu = clampLight((int) (u * (1 - blend) + 240f * blend));
            int nv = clampLight((int) (v * (1 - blend) + 240f * blend));
            return parent.light(nu, nv);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return parent.normal(x, y, z);
        }

        private static int clamp255(int x) { return Math.max(0, Math.min(255, x)); }
        private static int clampLight(int x) { return Math.max(0, Math.min(240, x)); }
    }
}
