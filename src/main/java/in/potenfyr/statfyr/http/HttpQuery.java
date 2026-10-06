package in.potenfyr.statfyr.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Small helpers for parsing HTTP query parameters and time ranges.
 */
public final class HttpQuery {

    private HttpQuery() {
    }

    /**
     * @param uri request URI
     * @return parsed query parameters
     */
    public static Map<String, String> parse(URI uri) {

        Map<String, String> params =
                new HashMap<>();

        String query =
                uri == null ? null : uri.getQuery();

        if (query == null || query.trim().isEmpty()) {
            return params;
        }

        for (String pair : query.split("&")) {

            int equals =
                    pair.indexOf('=');

            if (equals > 0) {

                params.put(
                        pair.substring(0, equals),
                        pair.substring(equals + 1)
                );
            }
        }

        return params;
    }

    public static int parseInt(
            Map<String, String> params,
            String key,
            int defaultValue,
            int min,
            int max
    ) {

        String value =
                params.get(key);

        if (value == null) {
            return defaultValue;
        }

        try {

            int parsed =
                    Integer.parseInt(value);

            return Math.max(min, Math.min(max, parsed));

        } catch (NumberFormatException ignored) {

            return defaultValue;
        }
    }

    /**
     * Parses an epoch-millis timestamp or a relative range such as
     * {@code "7d"}, {@code "24h"} or {@code "30m"} (meaning "ago").
     *
     * @param params       query parameters
     * @param key          parameter name
     * @param defaultValue fallback value
     * @return epoch millis
     */
    public static long parseTime(
            Map<String, String> params,
            String key,
            long defaultValue
    ) {

        String value =
                params.get(key);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        value = value.trim();

        try {

            char unit =
                    value.charAt(value.length() - 1);

            long amount =
                    Long.parseLong(value.substring(0, value.length() - 1));

            long now =
                    System.currentTimeMillis();

            switch (unit) {

                case 'm':
                    return now - amount * 60L * 1000L;

                case 'h':
                    return now - amount * 60L * 60L * 1000L;

                case 'd':
                    return now - amount * 24L * 60L * 60L * 1000L;

                case 'w':
                    return now - amount * 7L * 24L * 60L * 60L * 1000L;

                default:
                    break;
            }

        } catch (Exception ignored) {
        }

        try {

            return Long.parseLong(value);

        } catch (NumberFormatException ignored) {

            return defaultValue;
        }
    }
}
