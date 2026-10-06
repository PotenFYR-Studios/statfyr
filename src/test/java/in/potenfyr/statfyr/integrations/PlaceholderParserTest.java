package in.potenfyr.statfyr.integrations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PlaceholderAPI request parsing.
 */
class PlaceholderParserTest {

    // Longest-first, exactly as the expansion builds them.
    private static final List<String> KEYS =
            Arrays.asList(
                    "distance_traveled",
                    "blocks_broken",
                    "player_kills",
                    "active_time",
                    "blocks_mined",
                    "items_crafted",
                    "first_seen",
                    "last_seen",
                    "playtime",
                    "sessions",
                    "deaths",
                    "kills",
                    "kdr",
                    "rank"
            );

    @Test
    void parsesSelfPlaceholders() {

        PlaceholderParser.Result result =
                PlaceholderParser.parse("kills", KEYS);

        assertEquals("kills", result.key);
        assertTrue(result.isSelf());
    }

    @Test
    void parsesOtherPlayerPlaceholders() {

        PlaceholderParser.Result result =
                PlaceholderParser.parse("kills_Steve", KEYS);

        assertEquals("kills", result.key);
        assertEquals("Steve", result.playerName);
        assertFalse(result.isSelf());
    }

    @Test
    void matchesLongestUnderscoreKeyFirst() {

        PlaceholderParser.Result result =
                PlaceholderParser.parse("active_time_Steve", KEYS);

        assertEquals("active_time", result.key);
        assertEquals("Steve", result.playerName);
    }

    @Test
    void unknownReturnsNull() {

        assertNull(PlaceholderParser.parse("does_not_exist", KEYS));
        assertNull(PlaceholderParser.parse("", KEYS));
        assertNull(PlaceholderParser.parse(null, KEYS));
    }
}
