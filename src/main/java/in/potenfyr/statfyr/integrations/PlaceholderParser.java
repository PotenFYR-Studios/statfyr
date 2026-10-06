package in.potenfyr.statfyr.integrations;

import java.util.List;
import java.util.Locale;

/**
 * Pure placeholder-string parsing for the PlaceholderAPI expansion.
 *
 * <p>Kept separate so it can be unit tested without PlaceholderAPI present.
 */
public final class PlaceholderParser {

    private PlaceholderParser() {
    }

    /**
     * A parsed placeholder request.
     */
    public static final class Result {

        public final String key;
        public final String playerName;

        Result(String key, String playerName) {

            this.key = key;
            this.playerName = playerName;
        }

        /** @return {@code true} when the placeholder targets the requesting player. */
        public boolean isSelf() {
            return playerName == null;
        }
    }

    /**
     * Parses {@code params} against a list of known keys.
     *
     * <p>Keys must be ordered longest-first so that keys containing underscores
     * (such as {@code active_time}) win over shorter prefixes.
     *
     * @param params raw placeholder params (lower-cased)
     * @param keys   known placeholder keys, longest-first
     * @return the parsed result, or {@code null} when nothing matches
     */
    public static Result parse(String params, List<String> keys) {

        if (params == null || params.isEmpty()) {
            return null;
        }

        String query =
                params.toLowerCase(Locale.ROOT);

        for (String key : keys) {

            if (query.equals(key)) {
                return new Result(key, null);
            }

            if (query.startsWith(key + "_")) {
                return new Result(
                        key,
                        params.substring(key.length() + 1)
                );
            }
        }

        return null;
    }
}
