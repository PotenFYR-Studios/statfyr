package in.potenfyr.statfyr.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Human readable duration and timestamp formatting (Java 8 compatible).
 */
public final class TimeFormat {

    private TimeFormat() {
    }

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    /**
     * Formats seconds as {@code 82h 14m} / {@code 5d 2h 3m}.
     *
     * @param seconds duration in seconds
     * @return compact duration string
     */
    public static String duration(long seconds) {

        if (seconds <= 0L) {
            return "0m";
        }

        long days =
                seconds / 86400L;

        long hours =
                (seconds % 86400L) / 3600L;

        long minutes =
                (seconds % 3600L) / 60L;

        long secs =
                seconds % 60L;

        if (days > 0L) {
            return days + "d " + hours + "h " + minutes + "m";
        }

        if (hours > 0L) {
            return hours + "h " + minutes + "m " + secs + "s";
        }

        if (minutes > 0L) {
            return minutes + "m " + secs + "s";
        }

        return secs + "s";
    }

    /**
     * Formats an epoch millis timestamp as a date-time.
     *
     * @param epochMillis timestamp
     * @return formatted timestamp, or {@code "unknown"} when unset
     */
    public static String dateTime(long epochMillis) {

        if (epochMillis <= 0L) {
            return "unknown";
        }

        return DATE_TIME.format(
                Instant.ofEpochMilli(epochMillis)
        );
    }

    /**
     * Formats a timestamp as a coarse relative string such as {@code "42d ago"}.
     *
     * @param epochMillis timestamp
     * @return relative age
     */
    public static String relative(long epochMillis) {

        if (epochMillis <= 0L) {
            return "unknown";
        }

        long seconds =
                Duration.between(
                        Instant.ofEpochMilli(epochMillis),
                        Instant.now()
                ).getSeconds();

        if (seconds < 60L) {
            return "just now";
        }

        return duration(seconds) + " ago";
    }

    /**
     * @param value raw numeric value
     * @return value grouped with thousands separators
     */
    public static String number(long value) {

        return String.format("%,d", value);
    }
}
