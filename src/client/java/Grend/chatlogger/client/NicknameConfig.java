package Grend.chatlogger.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Formatting;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Клиентская замена никнеймов игроков.
 *
 * Пользователь задаёт пары «оригинальный ник → отображаемый ник». Замена
 * применяется только визуально на клиенте: в чате, над головой (nametag) и в
 * списке игроков (Tab). Сравнение оригинала регистронезависимое.
 *
 * Хранится в chatlogger_nicknames.properties рядом с остальными конфигами мода.
 */
public class NicknameConfig {

    private static NicknameConfig instance;
    private static final Path CONFIG_PATH = Paths.get("chatlogger_nicknames.properties");

    private boolean enabled = true;

    // Порядок добавления сохраняем — так меню выглядит предсказуемо.
    // Ключ — оригинальный ник в исходном регистре, значение — что показывать.
    private final LinkedHashMap<String, String> nicks = new LinkedHashMap<>();

    // Служебные структуры для быстрой замены (перестраиваются при изменении).
    private final Map<String, String> lowerToCustom = new HashMap<>();
    private Pattern pattern;

    private NicknameConfig() { load(); }

    public static synchronized NicknameConfig getInstance() {
        if (instance == null) instance = new NicknameConfig();
        return instance;
    }

    // ─── Доступ / управление ─────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean e) { this.enabled = e; save(); }

    /** Копия карты замен (оригинал → отображаемый) в порядке добавления. */
    public Map<String, String> getAll() { return new LinkedHashMap<>(nicks); }

    /**
     * Добавляет или обновляет замену. Если оригинал уже есть (без учёта
     * регистра), старая запись заменяется на новую.
     */
    public void set(String original, String custom) {
        if (original == null || custom == null) return;
        final String from = original.trim();
        final String to = custom.trim();
        if (from.isEmpty() || to.isEmpty()) return;

        // Убираем дубликаты без учёта регистра, сохраняя новый регистр ключа.
        nicks.keySet().removeIf(k -> k.equalsIgnoreCase(from));
        nicks.put(from, to);
        rebuild();
        save();
    }

    /** Удаляет замену по оригинальному нику (без учёта регистра). */
    public void remove(String original) {
        if (original == null) return;
        final String o = original.trim();
        if (nicks.keySet().removeIf(k -> k.equalsIgnoreCase(o))) {
            rebuild();
            save();
        }
    }

    public void clear() {
        nicks.clear();
        rebuild();
        save();
    }

    // ─── Замена ──────────────────────────────────────────────────────────────

    /** Заменяет ники в обычной строке (регистронезависимо, по границам слова). */
    public String replaceString(String input) {
        if (!enabled || pattern == null || input == null || input.isEmpty()) return input;
        Matcher m = pattern.matcher(input);
        if (!m.find()) return input;
        m.reset();
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String matched = m.group(1);
            String custom = lowerToCustom.get(matched.toLowerCase(Locale.ROOT));
            m.appendReplacement(out, Matcher.quoteReplacement(custom != null ? custom : matched));
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Заменяет ники внутри Text, сохраняя стили (цвета/форматирование) каждого
     * фрагмента. Если замен нет — возвращает исходный объект без копирования.
     */
    public Text replaceInText(Text text) {
        if (!enabled || pattern == null || text == null) return text;
        // Быстрый путь: если во всём тексте нет совпадений — ничего не пересобираем.
        if (!pattern.matcher(text.getString()).find()) return text;
        return transform(text);
    }

    /** Отображаемый ник для точного (регистронезависимого) имени, либо null. */
    public String getReplacementForName(String name) {
        if (!enabled || name == null) return null;
        return lowerToCustom.get(name.toLowerCase(Locale.ROOT));
    }

    /** Отображаемый ник как готовый Text с цветами (&-коды), либо null. */
    public Text getReplacementTextForName(String name) {
        String custom = getReplacementForName(name);
        return custom != null ? parseColorCodes(custom) : null;
    }

    private Text transform(Text text) {
        TextContent content = text.getContent();
        MutableText result;
        if (content instanceof PlainTextContent ptc) {
            result = buildReplacedLiteral(ptc.string(), text.getStyle());
        } else {
            // Translatable/keybind/прочее — оставляем содержимое как есть.
            result = text.copyContentOnly();
            result.setStyle(text.getStyle());
        }
        for (Text sibling : text.getSiblings()) {
            result.append(transform(sibling));
        }
        return result;
    }

    /**
     * Строит фрагмент текста, заменяя ники внутри одной литеральной строки.
     * Окружающий текст сохраняет исходный стиль, а вставленный ник получает
     * собственные цвета из &-кодов.
     */
    private MutableText buildReplacedLiteral(String literal, Style baseStyle) {
        if (pattern == null) return Text.literal(literal).setStyle(baseStyle);
        Matcher m = pattern.matcher(literal);
        if (!m.find()) return Text.literal(literal).setStyle(baseStyle);

        MutableText root = Text.empty().setStyle(baseStyle);
        m.reset();
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                root.append(Text.literal(literal.substring(last, m.start())).setStyle(baseStyle));
            }
            String matched = m.group(1);
            String custom = lowerToCustom.get(matched.toLowerCase(Locale.ROOT));
            if (custom != null) {
                root.append(parseColorCodes(custom));
            } else {
                root.append(Text.literal(matched).setStyle(baseStyle));
            }
            last = m.end();
        }
        if (last < literal.length()) {
            root.append(Text.literal(literal.substring(last)).setStyle(baseStyle));
        }
        return root;
    }

    /**
     * Преобразует строку с &-кодами (и §-кодами) в стилизованный Text.
     * Поддерживает цвета 0-9/a-f, форматирование k-o и сброс r.
     */
    public static Text parseColorCodes(String s) {
        MutableText result = Text.empty();
        if (s == null || s.isEmpty()) return result;
        Style style = Style.EMPTY;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) {
                Formatting fmt = Formatting.byCode(Character.toLowerCase(s.charAt(i + 1)));
                if (fmt != null) {
                    if (buf.length() > 0) {
                        result.append(Text.literal(buf.toString()).setStyle(style));
                        buf.setLength(0);
                    }
                    if (fmt == Formatting.RESET) {
                        style = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        // Цвет по легаси-правилам сбрасывает форматирование.
                        style = Style.EMPTY.withColor(fmt);
                    } else {
                        style = style.withFormatting(fmt);
                    }
                    i++; // пропускаем символ кода
                    continue;
                }
            }
            buf.append(c);
        }
        if (buf.length() > 0) result.append(Text.literal(buf.toString()).setStyle(style));
        return result;
    }

    private void rebuild() {
        lowerToCustom.clear();
        for (Map.Entry<String, String> e : nicks.entrySet()) {
            lowerToCustom.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        }
        if (nicks.isEmpty()) { pattern = null; return; }

        // Более длинные ники — раньше, чтобы они имели приоритет при совпадении.
        List<String> keys = new ArrayList<>(nicks.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));

        StringBuilder sb = new StringBuilder();
        sb.append("(?<![A-Za-z0-9_])(");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(Pattern.quote(keys.get(i)));
        }
        sb.append(")(?![A-Za-z0-9_])");
        pattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    // ─── Хранение ────────────────────────────────────────────────────────────

    public void save() {
        try {
            Properties props = new Properties();
            props.setProperty("enabled", String.valueOf(enabled));
            int i = 0;
            for (Map.Entry<String, String> e : nicks.entrySet()) {
                props.setProperty("nick." + i + ".from", e.getKey());
                props.setProperty("nick." + i + ".to", e.getValue());
                i++;
            }
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "EvoChat Nickname Replacements");
            }
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка сохранения ников: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) { rebuild(); return; }
        try {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) { props.load(is); }
            enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            nicks.clear();
            // Восстанавливаем по индексу, чтобы сохранить порядок.
            for (int i = 0; ; i++) {
                String from = props.getProperty("nick." + i + ".from");
                String to = props.getProperty("nick." + i + ".to");
                if (from == null || to == null) break;
                from = from.trim();
                to = to.trim();
                if (!from.isEmpty() && !to.isEmpty()) nicks.put(from, to);
            }
            rebuild();
        } catch (IOException e) {
            System.err.println("[EvoChat] Ошибка загрузки ников: " + e.getMessage());
        }
    }
}
