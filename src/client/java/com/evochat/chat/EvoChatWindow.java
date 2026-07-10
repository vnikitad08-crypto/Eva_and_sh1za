package com.evochat.chat;

import com.evochat.config.EvoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the EvoChat overlay window on the HUD.
 *
 * Visual style: dark glass panel with a cyan/teal accent bar on top,
 * soft-glowing message lines, and a subtle animated header label.
 */
public class EvoChatWindow {

    // ─── Singleton ──────────────────────────────────────────────────────────
    private static final EvoChatWindow INSTANCE = new EvoChatWindow();
    public static EvoChatWindow getInstance() { return INSTANCE; }

    // ─── State ───────────────────────────────────────────────────────────────
    private final List<ChatMessage> messages = new ArrayList<>();
    /**
     * How many wrapped lines the user has scrolled up from the bottom.
     * 0 = stick to newest messages; increases when scrolling up.
     */
    private int scrollLines = 0;
    private int cachedTotalLines = 0;
    private int cachedMaxVisible = 0;

    // ─── Design constants ────────────────────────────────────────────────────
    private static final int HEADER_HEIGHT   = 18;
    private static final int PADDING         = 5;
    private static final int LINE_HEIGHT     = 11;

    // Accent: green (айдентика мода)  0x43D17A
    private static final int COLOR_ACCENT    = 0x43D17A;
    // Panel background dark green-black
    private static final int COLOR_BG        = 0x0E1512;
    // Header bar slightly lighter (green-dark)
    private static final int COLOR_HEADER_BG = 0x17251C;
    // Text: soft green-white
    private static final int COLOR_TEXT      = 0xE8F5EC;
    // Dimmed text for older messages
    private static final int COLOR_TEXT_DIM  = 0x8BA596;
    // Highlight newest message (light green)
    private static final int COLOR_HIGHLIGHT = 0x6FE0A0;

    private static final String HEADER_LABEL = " EvoChat  \u2022  ЛС";

    // ─── Notification flash ──────────────────────────────────────────────────
    private long lastMessageTime = 0;
    private static final long FLASH_DURATION_MS = 1200;

    // ─── Message storage ─────────────────────────────────────────────────────
    public void addMessage(String text) {
        EvoConfig cfg = EvoConfig.get();
        ChatMessage msg = new ChatMessage(text);
        messages.add(msg);
        while (messages.size() > cfg.maxMessages) {
            messages.remove(0);
        }
        // When a new message arrives, reset scroll so user sees latest.
        scrollLines = 0;
        lastMessageTime = System.currentTimeMillis();
    }

    public void clear() {
        messages.clear();
    }

    /**
     * Returns true if given scaled mouse coordinates are inside EvoChat bounds.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        EvoConfig cfg = EvoConfig.get();
        if (!cfg.windowVisible) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        if (mc.options.hudHidden) return false;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int x = Math.round(cfg.chatX * screenW);
        int y = Math.round(cfg.chatY * screenH);
        int w = cfg.chatWidth;
        int h = cfg.chatHeight;

        // Clamp same as in render()
        x = Math.max(0, Math.min(screenW - w, x));
        y = Math.max(0, Math.min(screenH - h, y));

        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // ─── Rendering ───────────────────────────────────────────────────────────
    public void render(DrawContext ctx, float tickDelta) {
        EvoConfig cfg = EvoConfig.get();
        if (!cfg.windowVisible) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        // Respect F1 (hide HUD) — do not render EvoChat when HUD is hidden
        if (mc.options.hudHidden) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int x = Math.round(cfg.chatX * screenW);
        int y = Math.round(cfg.chatY * screenH);
        int w = cfg.chatWidth;
        int h = cfg.chatHeight;

        // Clamp so window stays on screen
        x = Math.max(0, Math.min(screenW - w, x));
        y = Math.max(0, Math.min(screenH - h, y));

        // Animate message alpha
        long now = System.currentTimeMillis();
        for (ChatMessage msg : messages) {
            if (msg.alpha < 1f) {
                msg.alpha = Math.min(1f, msg.alpha + 0.07f);
            }
        }

        // Opacity:
        //  - backgroundOpacity управляет только панелью/фоном
        //  - chatOpacity управляет только текстом сообщений
        float bgAlpha   = cfg.backgroundOpacity; // 0..1
        float textAlpha = cfg.chatOpacity;       // 0..1

        // ── Outer shadow ─────────────────────────────────────────────────────
        int shadowA = (int)(bgAlpha * 80);
        fillRect(ctx, x + 3, y + 3, w, h, withAlpha(0x000000, shadowA));

        // ── Main panel ───────────────────────────────────────────────────────
        int bgA = (int)(bgAlpha * 255);
        fillRect(ctx, x, y, w, h, withAlpha(COLOR_BG, bgA));

        // ── Header bar ───────────────────────────────────────────────────────
        int hdrA = (int)(bgAlpha * 255);
        fillRect(ctx, x, y, w, HEADER_HEIGHT, withAlpha(COLOR_HEADER_BG, hdrA));

        // Accent stripe on header top
        fillRect(ctx, x, y, w, 2, withAlpha(COLOR_ACCENT, hdrA));

        // Notification flash: accent glow on top bar when new message
        long elapsed = now - lastMessageTime;
        if (elapsed < FLASH_DURATION_MS) {
            float flashT = 1f - (float) elapsed / FLASH_DURATION_MS;
            int flashA = (int)(flashT * 140);
            fillRect(ctx, x, y, w, HEADER_HEIGHT, withAlpha(COLOR_ACCENT, flashA));
        }

        // Border lines: left & right faint (tied to background alpha)
        int borderA = (int)(bgAlpha * 60);
        fillRect(ctx, x, y, 1, h, withAlpha(COLOR_ACCENT, borderA));
        fillRect(ctx, x + w - 1, y, 1, h, withAlpha(COLOR_ACCENT, borderA));
        fillRect(ctx, x, y + h - 1, w, 1, withAlpha(COLOR_ACCENT, borderA));

        TextRenderer tr = mc.textRenderer;
        int textColor = withAlpha(COLOR_ACCENT, hdrA);

        // Header label - render plain string to avoid Text parsing issues
        ctx.drawText(tr, HEADER_LABEL, x + PADDING, y + 5, textColor, false);

        // Unread count badge
        if (!messages.isEmpty()) {
            String badge = String.valueOf(messages.size());
            int badgeW = tr.getWidth(badge) + 6;
            int badgeX = x + w - badgeW - 4;
            int badgeY = y + 4;
            fillRect(ctx, badgeX, badgeY, badgeW, 11, withAlpha(COLOR_ACCENT, hdrA));
            ctx.drawText(tr, badge, badgeX + 3, badgeY + 2,
                    withAlpha(COLOR_BG, hdrA), false);
        }

        // ── Message list ─────────────────────────────────────────────────────
        int contentH = h - HEADER_HEIGHT - PADDING * 2;
        int maxVisible = Math.max(1, contentH / LINE_HEIGHT);

        // Build wrapped lines list
        List<RenderedLine> lines = buildWrappedLines(tr, w, textAlpha);
        cachedTotalLines = lines.size();
        cachedMaxVisible = maxVisible;

        // Clamp scroll range
        if (cachedTotalLines <= cachedMaxVisible) {
            scrollLines = 0;
        } else {
            scrollLines = Math.max(0, Math.min(scrollLines, cachedTotalLines - cachedMaxVisible));
        }

        int total = lines.size();
        int start = Math.max(0, total - maxVisible - scrollLines);
        int end   = Math.min(total, total - scrollLines);

        ctx.enableScissor(x + 1, y + HEADER_HEIGHT, x + w - 1, y + h - 1);

        int drawY = y + HEADER_HEIGHT + PADDING;
        for (int i = start; i < end; i++) {
            RenderedLine line = lines.get(i);

            int msgA = (int)(textAlpha * line.message.alpha * 255);

            // Newest message gets accent highlight tint (for its first line)
            if (line.isNewest && line.isFirstOfMessage) {
                fillRect(ctx, x + 2, drawY - 1, w - 4, LINE_HEIGHT,
                        withAlpha(COLOR_ACCENT, (int) (msgA * 0.07f)));
            }

            // Bullet dot only for first line of each message
            if (line.isFirstOfMessage) {
                int dotColor = line.isNewest
                        ? withAlpha(COLOR_HIGHLIGHT, msgA)
                        : withAlpha(COLOR_ACCENT, (int) (msgA * 0.4f));
                fillRect(ctx, x + PADDING, drawY + 4, 2, 2, dotColor);
            }

            int tCol = line.isNewest
                    ? withAlpha(COLOR_TEXT, msgA)
                    : withAlpha(COLOR_TEXT_DIM, msgA);

            ctx.drawText(tr, line.text, x + PADDING + 6, drawY + 2, tCol, false);

            drawY += LINE_HEIGHT;
        }

        // Scrollbar on the right side so user sees position in history
        if (cachedTotalLines > cachedMaxVisible && bgAlpha > 0.02f) {
            int trackX = x + w - 3;
            int trackY = y + HEADER_HEIGHT + PADDING;
            int trackH = contentH - 2;

            // Track
            fillRect(ctx, trackX, trackY, 2, trackH,
                    withAlpha(COLOR_ACCENT, (int)(bgAlpha * 40)));

            int scrollRange = Math.max(1, cachedTotalLines - cachedMaxVisible);
            int thumbH = Math.max(8, trackH * cachedMaxVisible / cachedTotalLines);

            int maxOffset = trackH - thumbH;
            int startFromBottom = cachedTotalLines - cachedMaxVisible - start;
            float frac = (float) startFromBottom / (float) scrollRange; // 0 = bottom, 1 = top
            int thumbY = trackY + (int)((1.0f - frac) * maxOffset);

            fillRect(ctx, trackX, thumbY, 2, thumbH,
                    withAlpha(COLOR_ACCENT, (int)(bgAlpha * 160)));
        }

        ctx.disableScissor();

        // Empty state hint
        if (messages.isEmpty()) {
            String hint = "Жди сообщений ЛС...";
            int hintColor = withAlpha(0x5A6E60, (int)(textAlpha * 255));
            int hintX = x + w / 2 - tr.getWidth(hint) / 2;
            int hintY = y + h / 2 - 4;
            ctx.drawText(tr, hint, hintX, hintY, hintColor, false);
        }
    }

    /**
     * Scroll chat by a given number of wrapped lines.
     * Positive value scrolls up (older messages), negative — down.
     */
    public void scroll(int lines) {
        if (lines == 0) return;
        if (cachedTotalLines <= cachedMaxVisible) {
            scrollLines = 0;
            return;
        }
        scrollLines = Math.max(0, Math.min(scrollLines + lines, cachedTotalLines - cachedMaxVisible));
    }

    // ─── Line building & wrapping ─────────────────────────────────────────────

    private List<RenderedLine> buildWrappedLines(TextRenderer tr, int panelWidth, float textAlpha) {
        List<RenderedLine> out = new ArrayList<>();
        if (messages.isEmpty()) return out;

        int maxTextW = panelWidth - PADDING * 2 - 6;
        int newestIdx = messages.size() - 1;

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            boolean isNewest = (i == newestIdx);
            List<String> parts = wrapText(msg.text, maxTextW, tr);
            for (int j = 0; j < parts.size(); j++) {
                boolean first = (j == 0);
                out.add(new RenderedLine(msg, parts.get(j), first, isNewest));
            }
        }
        return out;
    }

    private List<String> wrapText(String text, int maxWidth, TextRenderer tr) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            result.add("");
            return result;
        }

        String remaining = text;
        while (!remaining.isEmpty()) {
            int len = remaining.length();
            int low = 1;
            int high = len;
            int best = 1;

            // Binary search max substring that fits width
            while (low <= high) {
                int mid = (low + high) / 2;
                String candidate = remaining.substring(0, mid);
                int w = tr.getWidth(candidate);
                if (w <= maxWidth) {
                    best = mid;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            // Try to break on last space for nicer wrapping
            int split = best;
            String candidate = remaining.substring(0, split);
            int lastSpace = candidate.lastIndexOf(' ');
            if (lastSpace > 0 && split < remaining.length()) {
                candidate = candidate.substring(0, lastSpace);
                split = lastSpace + 1; // skip the space
            }

            result.add(candidate);
            remaining = remaining.substring(split);
        }

        return result;
    }

    private static class RenderedLine {
        final ChatMessage message;
        final String text;
        final boolean isFirstOfMessage;
        final boolean isNewest;

        RenderedLine(ChatMessage message, String text, boolean isFirstOfMessage, boolean isNewest) {
            this.message = message;
            this.text = text;
            this.isFirstOfMessage = isFirstOfMessage;
            this.isNewest = isNewest;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private static void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    /** Pack ARGB: alpha in top byte, rest from hex color. */
    public static int withAlpha(int rgb, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
