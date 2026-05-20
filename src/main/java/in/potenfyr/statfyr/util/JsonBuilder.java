package in.potenfyr.statfyr.util;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON serializer.
 *
 * No external dependencies.
 *
 * Supports:
 * - String
 * - Number
 * - Boolean
 * - null
 * - Map
 * - List
 * - nested JsonBuilder
 */
public final class JsonBuilder {

    private final Map<String, Object> fields =
            new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Add
    // -------------------------------------------------------------------------

    public JsonBuilder add(
            String key,
            Object value
    ) {

        fields.put(key, value);

        return this;
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    /**
     * Builds JSON string.
     */
    public String build() {

        return serializeObject(fields);
    }

    /**
     * Returns underlying map.
     */
    public Map<String, Object> buildMap() {

        return new LinkedHashMap<>(fields);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static String object(
            Map<String, Object> map
    ) {

        return serializeObject(map);
    }

    public static String success(
            String message
    ) {

        return new JsonBuilder()

                .add(
                        "success",
                        true
                )

                .add(
                        "message",
                        message
                )

                .add(
                        "timestamp",
                        Instant.now().toString()
                )

                .build();
    }

    public static String error(
            String message
    ) {

        return error(
                500,
                message
        );
    }

    public static String error(
            int status,
            String message
    ) {

        return new JsonBuilder()

                .add(
                        "success",
                        false
                )

                .add(
                        "status",
                        status
                )

                .add(
                        "error",
                        message
                )

                .add(
                        "timestamp",
                        Instant.now().toString()
                )

                .build();
    }

    // -------------------------------------------------------------------------
    // Serialization
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static String serializeValue(
            Object value
    ) {

        if (value == null) {
            return "null";
        }

        if (value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof String) {

            return "\""
                    + escapeString(
                    (String) value
            )
                    + "\"";
        }

        if (value instanceof JsonBuilder) {

            return ((JsonBuilder) value)
                    .build();
        }

        if (value instanceof Map) {

            return serializeObject(
                    (Map<String, Object>) value
            );
        }

        if (value instanceof List) {

            return serializeArray(
                    (List<?>) value
            );
        }

        // Fallback
        return "\""
                + escapeString(
                value.toString()
        )
                + "\"";
    }

    private static String serializeObject(
            Map<String, Object> map
    ) {

        if (map == null
                || map.isEmpty()) {

            return "{}";
        }

        StringBuilder sb =
                new StringBuilder("{");

        boolean first = true;

        for (Map.Entry<String, Object> entry
                : map.entrySet()) {

            if (!first) {
                sb.append(',');
            }

            sb.append('"')
                    .append(
                            escapeString(
                                    entry.getKey()
                            )
                    )
                    .append('"');

            sb.append(':');

            sb.append(
                    serializeValue(
                            entry.getValue()
                    )
            );

            first = false;
        }

        sb.append('}');

        return sb.toString();
    }

    private static String serializeArray(
            List<?> list
    ) {

        if (list == null
                || list.isEmpty()) {

            return "[]";
        }

        StringBuilder sb =
                new StringBuilder("[");

        boolean first = true;

        for (Object item : list) {

            if (!first) {
                sb.append(',');
            }

            sb.append(
                    serializeValue(item)
            );

            first = false;
        }

        sb.append(']');

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Escaping
    // -------------------------------------------------------------------------

    private static String escapeString(
            String input
    ) {

        if (input == null) {
            return "";
        }

        StringBuilder sb =
                new StringBuilder(
                        input.length() + 16
                );

        for (int i = 0; i < input.length(); i++) {

            char character =
                    input.charAt(i);

            switch (character) {

                case '"':
                    sb.append("\\\"");
                    break;

                case '\\':
                    sb.append("\\\\");
                    break;

                case '\n':
                    sb.append("\\n");
                    break;

                case '\r':
                    sb.append("\\r");
                    break;

                case '\t':
                    sb.append("\\t");
                    break;

                case '\b':
                    sb.append("\\b");
                    break;

                case '\f':
                    sb.append("\\f");
                    break;

                default:

                    if (character < 0x20) {

                        sb.append(
                                String.format(
                                        "\\u%04x",
                                        (int) character
                                )
                        );

                    } else {

                        sb.append(character);
                    }
            }
        }

        return sb.toString();
    }
}