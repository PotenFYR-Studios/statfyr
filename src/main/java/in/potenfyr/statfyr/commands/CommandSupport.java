package in.potenfyr.statfyr.commands;

import in.potenfyr.statfyr.analytics.Metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Pure, Bukkit-free command helpers.
 *
 * <p>Kept separate from {@link StatfyrCommand} so the permission gating,
 * metric validation and tab-completion filtering can be unit tested without a
 * running server.
 */
public final class CommandSupport {

    private CommandSupport() {
    }

    public static final List<String> SUBCOMMANDS =
            Collections.unmodifiableList(
                    java.util.Arrays.asList(
                            "stats",
                            "top",
                            "help",
                            "reload",
                            "status",
                            "database",
                            "purge",
                            "purge-history",
                            "debug"
                    )
            );

    /**
     * @param canPermission predicate that answers whether a permission is held
     * @param sub           subcommand name
     * @return {@code true} when the sender may run the subcommand
     */
    public static boolean canRun(
            Predicate<String> canPermission,
            String sub
    ) {

        if (sub == null) {
            return false;
        }

        // Administrators can run every subcommand (mirrors require()).
        if (canPermission.test("statfyr.admin")) {
            return true;
        }

        switch (sub.toLowerCase(Locale.ROOT)) {

            case "stats":
                return canPermission.test("statfyr.stats")
                        || canPermission.test("statfyr.stats.self")
                        || canPermission.test("statfyr.stats.others");

            case "top":
                return canPermission.test("statfyr.top");

            case "help":
                return canPermission.test("statfyr.help");

            case "reload":
                return canPermission.test("statfyr.reload");

            case "status":
                return canPermission.test("statfyr.status");

            case "database":
                return canPermission.test("statfyr.database");

            case "purge":
            case "purge-history":
                return canPermission.test("statfyr.purge");

            case "debug":
                return canPermission.test("statfyr.debug");

            default:
                return false;
        }
    }

    /**
     * @param metric canonical metric key
     * @return {@code true} when it is a rankable metric
     */
    public static boolean isKnownMetric(String metric) {

        if (metric == null) {
            return false;
        }

        for (String key : Metrics.leaderboardKeys()) {

            if (key.equals(metric)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Case-insensitive prefix filter used for tab completion.
     *
     * @param options candidate values
     * @param prefix  typed prefix
     * @return matching values
     */
    public static List<String> filter(
            List<String> options,
            String prefix
    ) {

        List<String> result =
                new ArrayList<>();

        String lower =
                prefix == null
                        ? ""
                        : prefix.toLowerCase(Locale.ROOT);

        for (String option : options) {

            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }

        return result;
    }

    /**
     * @param canPermission predicate that answers whether a permission is held
     * @param prefix        typed prefix
     * @return runnable subcommands matching the prefix
     */
    public static List<String> completions(
            Predicate<String> canPermission,
            String prefix
    ) {

        List<String> allowed =
                new ArrayList<>();

        for (String sub : SUBCOMMANDS) {

            if (canRun(canPermission, sub)) {
                allowed.add(sub);
            }
        }

        Collections.sort(allowed);

        return filter(allowed, prefix);
    }
}
