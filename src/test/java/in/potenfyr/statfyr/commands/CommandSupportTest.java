package in.potenfyr.statfyr.commands;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for permission gating and tab-completion logic.
 */
class CommandSupportTest {

    private static Predicate<String> perms(String... permissions) {

        Set<String> set =
                new HashSet<>(Arrays.asList(permissions));

        return set::contains;
    }

    @Test
    void gatesSubcommandsByPermission() {

        Predicate<String> defaultUser =
                perms("statfyr.use", "statfyr.help", "statfyr.stats.self", "statfyr.top");

        assertTrue(CommandSupport.canRun(defaultUser, "stats"));
        assertTrue(CommandSupport.canRun(defaultUser, "top"));
        assertTrue(CommandSupport.canRun(defaultUser, "help"));
        assertFalse(CommandSupport.canRun(defaultUser, "reload"));
        assertFalse(CommandSupport.canRun(defaultUser, "purge"));
        assertFalse(CommandSupport.canRun(defaultUser, "debug"));

        Predicate<String> admin =
                perms("statfyr.admin");

        assertTrue(CommandSupport.canRun(admin, "reload"));
        assertTrue(CommandSupport.canRun(admin, "purge"));
        assertTrue(CommandSupport.canRun(admin, "database"));
        assertTrue(CommandSupport.canRun(admin, "debug"));
    }

    @Test
    void completionFiltersByPermissionAndPrefix() {

        Predicate<String> defaultUser =
                perms("statfyr.help", "statfyr.stats.self", "statfyr.top");

        List<String> all =
                CommandSupport.completions(defaultUser, "");

        assertTrue(all.contains("stats"));
        assertTrue(all.contains("top"));
        assertTrue(all.contains("help"));
        assertFalse(all.contains("reload"));

        assertEquals(
                Arrays.asList("stats"),
                CommandSupport.completions(defaultUser, "sta")
        );
    }

    @Test
    void validatesMetrics() {

        assertTrue(CommandSupport.isKnownMetric("kills"));
        assertTrue(CommandSupport.isKnownMetric("playtime"));
        assertFalse(CommandSupport.isKnownMetric("nonsense"));
        assertFalse(CommandSupport.isKnownMetric(null));
    }

    @Test
    void filtersPrefixesCaseInsensitively() {

        assertEquals(
                Arrays.asList("Daily", "daily"),
                CommandSupport.filter(
                        Arrays.asList("Daily", "daily", "weekly"),
                        "da"
                )
        );
    }
}
