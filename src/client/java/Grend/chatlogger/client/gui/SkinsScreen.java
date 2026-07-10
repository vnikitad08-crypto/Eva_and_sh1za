package Grend.chatlogger.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import Grend.chatlogger.client.SkinConfig;

/**
 * Выбор кастомных скинов DiamondWorld во вкладках: Меч, Призма (меч со
 * стадиями), Броня. Клиентская косметика — видно только тебе.
 */
public class SkinsScreen extends Screen {

    private static final int COL_DIM    = 0xAA000000;
    private static final int COL_PANEL  = 0xFF0E1512;
    private static final int COL_ACCENT = 0xFF43D17A;
    private static final int COL_MUTED  = 0xFF8BA596;
    private static final int COL_BORDER = 0xFF23372B;
    private static final int COL_TEXT   = 0xFFE8F5EC;

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 250;

    private static final String[] TABS = { "Меч", "Призма", "Броня" };

    private final Screen parent;
    private final SkinConfig cfg = SkinConfig.getInstance();
    private int px, py, contentX, contentW;
    private int activeTab = 0;

    private final int[] tabX = new int[TABS.length];
    private int tabW, tabY;

    // Y-координаты рядов/подписей активной вкладки (заполняются в init)
    private int rowY0;                       // одиночный ряд (Меч/Броня)
    private int lblY1, rowY1, lblY2, rowY2;  // двойной ряд (Призма)

    public SkinsScreen(Screen parent) {
        super(Text.literal("Скины"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        int margin = 20;
        contentX = px + margin;
        contentW = PANEL_W - margin * 2;
        int rx = contentX + contentW - 28;

        // ── Вкладки ─────────────────────────────────────────────────────────
        int gap = 6;
        tabW = (contentW - gap * (TABS.length - 1)) / TABS.length;
        tabY = py + 64;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            tabX[i] = contentX + i * (tabW + gap);
            addDrawableChild(new CLButton(tabX[i], tabY, tabW, 18, TABS[i], b -> {
                activeTab = idx;
                clearAndInit();
            }));
        }

        int toggleY = py + 92;

        switch (activeTab) {
            case 0 -> { // Меч
                addDrawableChild(new CLToggle(contentX, toggleY, "Скин меча",
                        cfg.isSwordEnabled(), v -> cfg.setSwordEnabled(v)));
                rowY0 = py + 118;
                addArrows(rowY0, rx,
                        () -> cfg.setSwordSkin(cycle(SkinConfig.SWORD_SKINS, cfg.getSwordSkin(), -1)),
                        () -> cfg.setSwordSkin(cycle(SkinConfig.SWORD_SKINS, cfg.getSwordSkin(), 1)));
            }
            case 1 -> { // Призматический меч
                addDrawableChild(new CLToggle(contentX, toggleY, "Призматический меч",
                        cfg.isPrismaticEnabled(), v -> cfg.setPrismaticEnabled(v)));
                lblY1 = py + 114; rowY1 = py + 123;
                addArrows(rowY1, rx,
                        () -> cfg.setPrismaticTheme(cycle(SkinConfig.PRISMATIC_THEMES, cfg.getPrismaticTheme(), -1)),
                        () -> cfg.setPrismaticTheme(cycle(SkinConfig.PRISMATIC_THEMES, cfg.getPrismaticTheme(), 1)));
                lblY2 = py + 149; rowY2 = py + 158;
                addArrows(rowY2, rx,
                        () -> cfg.setPrismaticStage(cycle(SkinConfig.PRISMATIC_STAGES, cfg.getPrismaticStage(), -1)),
                        () -> cfg.setPrismaticStage(cycle(SkinConfig.PRISMATIC_STAGES, cfg.getPrismaticStage(), 1)));
            }
            default -> { // Броня
                addDrawableChild(new CLToggle(contentX, toggleY, "Скин брони",
                        cfg.isArmorEnabled(), v -> cfg.setArmorEnabled(v)));
                rowY0 = py + 118;
                addArrows(rowY0, rx,
                        () -> cfg.setArmorSkin(cycle(SkinConfig.ARMOR_SKINS, cfg.getArmorSkin(), -1)),
                        () -> cfg.setArmorSkin(cycle(SkinConfig.ARMOR_SKINS, cfg.getArmorSkin(), 1)));
            }
        }

        addDrawableChild(new CLButton(px + PANEL_W / 2 - 60, py + PANEL_H - 28, 120, 20,
                parent != null ? "Назад" : "Закрыть", b -> close()));
    }

    private void addArrows(int rowY, int rx, Runnable onLeft, Runnable onRight) {
        addDrawableChild(new CLButton(contentX, rowY, 28, 20, "◄", b -> onLeft.run()));
        addDrawableChild(new CLButton(rx, rowY, 28, 20, "►", b -> onRight.run()));
    }

    private String cycle(String[] arr, String current, int dir) {
        int idx = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(current)) { idx = i; break; }
        }
        idx = (idx + dir + arr.length) % arr.length;
        return arr[idx];
    }

    private ItemStack swordPreview(String itemModelId) {
        ItemStack s = new ItemStack(Items.NETHERITE_SWORD);
        Identifier id = Identifier.tryParse(itemModelId);
        if (id != null) s.set(DataComponentTypes.ITEM_MODEL, id);
        return s;
    }

    private static String stageLabel(String stage) {
        return "2_3".equals(stage) ? "2+3" : stage;
    }

    private void drawRow(DrawContext ctx, int rowY, ItemStack preview, String name) {
        var tr = client.textRenderer;
        int left  = contentX + 28 + 6;
        int right = contentX + contentW - 28 - 6;
        int center = (left + right) / 2;
        int tw = tr.getWidth(name);
        int groupW = (preview != null ? 20 : 0) + tw;
        int sx = center - groupW / 2;
        if (preview != null) {
            try { ctx.drawItem(preview, sx, rowY + 2); } catch (Throwable ignored) {}
            sx += 20;
        }
        ctx.drawText(tr, name, sx, rowY + 6, COL_TEXT, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_DIM);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, COL_PANEL);
        ctx.fill(px, py, px + PANEL_W, py + 3, COL_ACCENT);
        ctx.fill(px, py, px + 1, py + PANEL_H, COL_BORDER);
        ctx.fill(px + PANEL_W - 1, py, px + PANEL_W, py + PANEL_H, COL_BORDER);
        ctx.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, COL_BORDER);

        var tr = client.textRenderer;

        String title = "Скины";
        int titleX = px + PANEL_W / 2 - tr.getWidth(title);
        ctx.getMatrices().push();
        ctx.getMatrices().scale(2f, 2f, 1f);
        ctx.drawText(tr, title, titleX / 2, (py + 16) / 2, COL_ACCENT, false);
        ctx.getMatrices().pop();

        String sub = "Кастомные скины DiamondWorld";
        ctx.drawText(tr, sub, px + PANEL_W / 2 - tr.getWidth(sub) / 2, py + 44, COL_MUTED, false);
        ctx.fill(px + 20, py + 58, px + PANEL_W - 20, py + 59, COL_BORDER);

        // Подчёркивание активной вкладки
        ctx.fill(tabX[activeTab], tabY + 19, tabX[activeTab] + tabW, tabY + 21, COL_ACCENT);

        switch (activeTab) {
            case 0 -> drawRow(ctx, rowY0, swordPreview(cfg.getSwordItemModelId()), cfg.getSwordSkin());
            case 1 -> {
                ctx.drawText(tr, "Тема:", contentX, lblY1, COL_MUTED, false);
                drawRow(ctx, rowY1, swordPreview(cfg.getPrismaticItemModelId()), cfg.getPrismaticTheme());
                ctx.drawText(tr, "Улучшение:", contentX, lblY2, COL_MUTED, false);
                drawRow(ctx, rowY2, null, "Стадия " + stageLabel(cfg.getPrismaticStage()));
            }
            default -> {
                drawRow(ctx, rowY0, null, cfg.getArmorSkin());
                String hint = "Применяется ко всей надетой броне.";
                ctx.drawText(tr, hint, px + PANEL_W / 2 - tr.getWidth(hint) / 2, rowY0 + 26, COL_MUTED, false);
            }
        }

        String note = "Видно только тебе — на других игроках не показывается.";
        ctx.drawText(tr, note, px + PANEL_W / 2 - tr.getWidth(note) / 2, py + PANEL_H - 44, COL_MUTED, false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (parent != null) client.setScreen(parent);
        else super.close();
    }
}
