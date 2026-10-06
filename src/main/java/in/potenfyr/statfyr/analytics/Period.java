package in.potenfyr.statfyr.analytics;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Aggregation window used by leaderboards and history queries.
 */
public enum Period {

    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME;

    /**
     * Parses a user supplied period name.
     *
     * @param value raw value (for example {@code "all-time"})
     * @return the matching period, defaulting to {@link #ALL_TIME}
     */
    public static Period from(String value) {

        if (value == null) {
            return ALL_TIME;
        }

        String normalised =
                value.trim()
                        .toLowerCase()
                        .replace('-', '_');

        switch (normalised) {

            case "day":
            case "daily":
                return DAILY;

            case "week":
            case "weekly":
                return WEEKLY;

            case "month":
            case "monthly":
                return MONTHLY;

            case "all":
            case "alltime":
            case "all_time":
            default:
                return ALL_TIME;
        }
    }

    /**
     * @return the API/config token for this period
     */
    public String token() {

        return name().toLowerCase();
    }

    /**
     * Start instant of the current window.
     *
     * @param now      current epoch millis
     * @param weekDay  configured weekly reset day
     * @param weekHour configured weekly reset hour
     * @param monthDay configured monthly reset day
     * @return epoch millis at which the current window began
     */
    public long start(
            long now,
            DayOfWeek weekDay,
            int weekHour,
            int monthDay
    ) {

        ZoneId zone =
                ZoneId.systemDefault();

        LocalDateTime nowTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(now),
                        zone
                );

        switch (this) {

            case DAILY:
                return nowTime.toLocalDate()
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli();

            case WEEKLY: {

                LocalDateTime reset =
                        nowTime.with(
                                TemporalAdjusters.previousOrSame(weekDay)
                        ).withHour(
                                Math.max(0, Math.min(23, weekHour))
                        ).withMinute(0).withSecond(0).withNano(0);

                if (reset.isAfter(nowTime)) {
                    reset = reset.minusWeeks(1);
                }

                return reset.atZone(zone)
                        .toInstant()
                        .toEpochMilli();
            }

            case MONTHLY: {

                LocalDate date =
                        nowTime.toLocalDate();

                int day =
                        Math.max(1, Math.min(28, monthDay));

                LocalDate resetDate =
                        date.withDayOfMonth(day);

                if (resetDate.isAfter(date)) {
                    resetDate = resetDate.minusMonths(1);
                }

                return resetDate.atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli();
            }

            case ALL_TIME:
            default:
                return 0L;
        }
    }

    /**
     * End instant of the current window (start of the next one).
     *
     * @param now current epoch millis
     * @return epoch millis at which the current window ends
     */
    public long end(long now) {

        ZoneId zone =
                ZoneId.systemDefault();

        LocalDateTime nowTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(now),
                        zone
                );

        switch (this) {

            case DAILY:
                return nowTime.toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli();

            case WEEKLY:
                return start(now, DayOfWeek.MONDAY, 0, 1)
                        + 7L * 24L * 60L * 60L * 1000L;

            case MONTHLY:
                return nowTime.toLocalDate()
                        .withDayOfMonth(1)
                        .plusMonths(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli();

            case ALL_TIME:
            default:
                return Long.MAX_VALUE;
        }
    }
}
