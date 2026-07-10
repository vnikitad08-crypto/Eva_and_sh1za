package Grend.chatlogger.client;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Таймеры еженедельных ивентов DiamondWorld (PrisonEvo).
 * Время серверов ДВ — московское (UTC+3, без перехода на летнее время).
 *
 *  • Мифический ивент:  суббота 15:00–19:00 (мифические рубины с рейд-боссов)
 *  • Золотая лихорадка: воскресенье 15:00–17:00 (бустер продажи блоков ×1.3)
 *
 * Расписание может меняться администрацией — это ориентир, не гарантия.
 */
public final class EventTimers {

    private static final ZoneOffset MSK = ZoneOffset.ofHours(3);

    public static final class Event {
        public final String name;
        public final DayOfWeek day;
        public final int startHour;
        public final int endHour;

        Event(String name, DayOfWeek day, int startHour, int endHour) {
            this.name = name;
            this.day = day;
            this.startHour = startHour;
            this.endHour = endHour;
        }
    }

    public static final Event MYTHIC = new Event("Мифический ивент", DayOfWeek.SATURDAY, 15, 19);
    public static final Event GOLD_RUSH = new Event("Золотая лихорадка", DayOfWeek.SUNDAY, 15, 17);
    public static final Event[] ALL = { MYTHIC, GOLD_RUSH };

    private EventTimers() {}

    /** Идёт ли ивент прямо сейчас. */
    public static boolean isActive(Event e) {
        ZonedDateTime now = ZonedDateTime.now(MSK);
        return now.getDayOfWeek() == e.day
                && now.getHour() >= e.startHour
                && now.getHour() < e.endHour;
    }

    /** Человекочитаемая строка статуса ивента. */
    public static String status(Event e) {
        ZonedDateTime now = ZonedDateTime.now(MSK);
        if (isActive(e)) {
            ZonedDateTime end = now.withHour(e.endHour).withMinute(0).withSecond(0).withNano(0);
            return "идёт, до конца " + fmt(Duration.between(now, end));
        }
        ZonedDateTime start = nextStart(now, e);
        return "через " + fmt(Duration.between(now, start));
    }

    private static ZonedDateTime nextStart(ZonedDateTime now, Event e) {
        ZonedDateTime candidate = now
                .with(TemporalAdjusters.nextOrSame(e.day))
                .withHour(e.startHour).withMinute(0).withSecond(0).withNano(0);
        // Если сегодня нужный день, но время старта уже прошло — берём следующую неделю.
        if (!candidate.isAfter(now)) {
            candidate = now
                    .with(TemporalAdjusters.next(e.day))
                    .withHour(e.startHour).withMinute(0).withSecond(0).withNano(0);
        }
        return candidate;
    }

    private static String fmt(Duration d) {
        long total = Math.max(0, d.getSeconds());
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;
        if (days > 0) return days + "д " + hours + "ч " + minutes + "м";
        if (hours > 0) return hours + "ч " + minutes + "м";
        return minutes + "м";
    }
}
