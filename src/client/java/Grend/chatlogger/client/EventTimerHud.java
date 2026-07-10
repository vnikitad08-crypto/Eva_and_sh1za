package Grend.chatlogger.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Отрисовка HUD-виджета «Таймер ивентов DiamondWorld».
 * Вынесено из ChatloggerClient, чтобы позицию можно было менять из редактора
 * расположения виджетов и рендер оставался единым в обоих местах.
 */
public final class EventTimerHud {

    private EventTimerHud() {}

    private static String header() { return "§b☀ Ивенты DW"; }
    private static String line1()  { return "§f" + EventTimers.MYTHIC.name + ": §7" + EventTimers.status(EventTimers.MYTHIC); }
    private static String line2()  { return "§f" + EventTimers.GOLD_RUSH.name + ": §7" + EventTimers.status(EventTimers.GOLD_RUSH); }

    /** Ширина виджета в пикселях (зависит от текста таймеров). */
    public static int width(TextRenderer tr) {
        return Math.max(tr.getWidth(line1()), Math.max(tr.getWidth(line2()), tr.getWidth(header()))) + 12;
    }

    /** Высота виджета в пикселях (фикс). */
    public static int height() {
        return 12 + 11 * 2 + 8;
    }

    /** Рисует виджет с левым-верхним углом в (x, y). */
    public static void render(DrawContext ctx, int x, int y) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int wpx = width(tr);
        int hpx = height();

        ctx.fill(x, y, x + wpx, y + hpx, 0xA00D1117);
        ctx.fill(x, y, x + wpx, y + 1, 0xFF43D17A);
        ctx.fill(x, y, x + 1, y + hpx, 0xFF43D17A);
        ctx.drawText(tr, header(), x + 6, y + 5, 0xFF43D17A, false);
        ctx.drawText(tr, line1(),  x + 6, y + 18, 0xFFE8F5EC, false);
        ctx.drawText(tr, line2(),  x + 6, y + 30, 0xFFE8F5EC, false);
    }
}
