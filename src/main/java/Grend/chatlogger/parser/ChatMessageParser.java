package Grend.chatlogger.parser;

import java.util.regex.*;

/**
 * Парсер сообщений чата для извлечения информации об игроке
 *
 * Формат сообщения: Ⓜ ꀇ o_ToBiTo_o [Ka3Hb] [325]: сообщение
 * или: Ⓜ ꀓ Fleshy ဵ [Жабки] [410]: сообщение
 * или: o_ToBiTo_o [Ka3Hb] [325]: сообщение
 */
public class ChatMessageParser {

    // Регулярное выражение - ищем [Клан] [Уровень]: и берём слово перед этим
    // Ник должен содержать хотя бы одну букву (не только цифры)
    private static final Pattern CHAT_PATTERN = Pattern.compile(
        "([A-Za-zА-Яа-яЁё][A-Za-z0-9_А-Яа-яЁё]*)[^\\[]*\\[([^\\]]+)\\]\\s*\\[(\\d+)\\]:"
    );

    /**
     * Результат парсинга
     */
    public static class ParseResult {
        public final String nickname;
        public final String clan;
        public final int level;
        public final boolean success;

        private ParseResult(String nickname, String clan, int level, boolean success) {
            this.nickname = nickname;
            this.clan = clan;
            this.level = level;
            this.success = success;
        }

        public static ParseResult success(String nickname, String clan, int level) {
            return new ParseResult(nickname, clan, level, true);
        }

        public static ParseResult failure() {
            return new ParseResult(null, null, 0, false);
        }
    }

    /**
     * Парсит сообщение из чата
     */
    public static ParseResult parse(String message) {
        if (message == null || message.isEmpty()) {
            return ParseResult.failure();
        }

        Matcher matcher = CHAT_PATTERN.matcher(message);

        if (matcher.find()) {
            String nickname = matcher.group(1);
            
            // Дополнительная проверка: ник не должен быть чисто числовым
            if (nickname.matches("^\\d+$")) {
                return ParseResult.failure();
            }
            
            // Ник должен быть разумной длины (1-16 символов для Minecraft)
            if (nickname.length() < 1 || nickname.length() > 16) {
                return ParseResult.failure();
            }
            
            String clan = matcher.group(2);
            int level;

            try {
                level = Integer.parseInt(matcher.group(3));
                
                // Уровень должен быть в разумных пределах (1-1000)
                if (level < 1 || level > 1000) {
                    return ParseResult.failure();
                }
            } catch (NumberFormatException e) {
                return ParseResult.failure();
            }

            return ParseResult.success(nickname, clan, level);
        }

        return ParseResult.failure();
    }

    /**
     * Проверяет, является ли сообщение сообщением игрока
     */
    public static boolean isPlayerMessage(String message) {
        return parse(message).success;
    }

    /**
     * Тестовый метод для проверки парсинга
     */
    public static void main(String[] args) {
        String[] testMessages = {
            "Ⓜ ꀇ o_ToBiTo_o [Ka3Hb] [325]: Куплю зелье",
            "Ⓜ ꀓ Fleshy ဵ [Жабки] [410]: Скупаю КСП",
            "PlayerName [TestClan] [100]: Hello",
            "384 [385]: это не игрок", // должно отклонить
            "abc [Clan] [50]: тест" // должно принять
        };

        for (String msg : testMessages) {
            ParseResult result = parse(msg);
            System.out.println("Message: " + msg);
            System.out.println("  Success: " + result.success);
            if (result.success) {
                System.out.println("  Nick: " + result.nickname);
                System.out.println("  Clan: " + result.clan);
                System.out.println("  Level: " + result.level);
            }
            System.out.println();
        }
    }
}
