package in.potenfyr.statfyr.util;

/**
 * Tiny string helpers.
 *
 * <p>Statfyr targets Java 8 bytecode so that a single JAR runs on Minecraft
 * 1.8.x (Java 8) all the way up to 26.x (Java 25+). That means we cannot use
 * Java 11+ conveniences such as {@link String#isBlank()} or
 * {@link String#strip()}.
 */
public final class Text {

    private Text() {
    }

    /**
     * Null-safe {@code String}-blank check.
     *
     * @param value value to test
     * @return {@code true} when the value is null or contains only whitespace
     */
    public static boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }

    /**
     * @param value value to test
     * @return {@code true} when the value is non-null and has non-whitespace content
     */
    public static boolean isNotBlank(String value) {

        return !isBlank(value);
    }

    /**
     * @param value value to normalise
     * @return the value, or an empty string when null
     */
    public static String nullToEmpty(String value) {

        return value == null ? "" : value;
    }
}
