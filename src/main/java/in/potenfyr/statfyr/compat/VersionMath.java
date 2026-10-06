package in.potenfyr.statfyr.compat;

/**
 * Pure version parsing/comparison helpers.
 *
 * <p>Kept free of any Bukkit dependency so it can be unit tested without a
 * server present. Handles both the classic {@code 1.21.4} scheme and the
 * calendar scheme introduced in 2026 ({@code 26.3}).
 */
public final class VersionMath {

    private VersionMath() {
    }

    /**
     * Parses a version string into numeric components.
     *
     * @param version version string
     * @return numeric components (never empty)
     */
    public static int[] parse(String version) {

        if (version == null) {
            return new int[]{0};
        }

        String value =
                version.trim();

        int start = 0;

        while (start < value.length()
                && !Character.isDigit(value.charAt(start))) {
            start++;
        }

        if (start >= value.length()) {
            return new int[]{0};
        }

        value = value.substring(start);

        // Drop build metadata such as "-R0.1-SNAPSHOT" or "-rc1".
        int cut =
                value.indexOf('-');

        if (cut < 0) {
            cut = value.indexOf('+');
        }

        if (cut > 0) {
            value = value.substring(0, cut);
        }

        String[] parts =
                value.split("[^0-9]+");

        int length =
                Math.min(parts.length, 4);

        int[] components =
                new int[length];

        for (int i = 0; i < length; i++) {

            try {

                components[i] =
                        Integer.parseInt(parts[i]);

            } catch (NumberFormatException ignored) {

                components[i] = 0;
            }
        }

        return components.length == 0
                ? new int[]{0}
                : components;
    }

    /**
     * Lexicographic comparison, treating missing components as zero.
     *
     * @param left  left version
     * @param right right version
     * @return negative, zero or positive
     */
    public static int compare(int[] left, int[] right) {

        int length =
                Math.max(left.length, right.length);

        for (int i = 0; i < length; i++) {

            int a = i < left.length ? left[i] : 0;
            int b = i < right.length ? right[i] : 0;

            if (a != b) {
                return a < b ? -1 : 1;
            }
        }

        return 0;
    }

    /**
     * @param version version to test
     * @param major   major component
     * @param minor   minor component
     * @return {@code true} when the version is at least that value
     */
    public static boolean atLeast(
            String version,
            int major,
            int minor
    ) {

        return compare(parse(version), new int[]{major, minor}) >= 0;
    }
}
