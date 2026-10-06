package in.potenfyr.statfyr.analytics;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for session/playtime bookkeeping.
 */
class PlayerProfileTest {

    @Test
    void sessionLifecycle() {

        long now = 1_000_000L;

        PlayerProfile profile =
                new PlayerProfile(UUID.randomUUID(), "Steve", now);

        profile.beginSession(now, new HashMap<>());

        assertEquals(1, profile.totalSessions);

        long duration =
                profile.endSession(now + 60_000L);

        assertEquals(60L, duration);
        assertEquals(60L, profile.totalPlaytimeSeconds);
        assertEquals(60L, profile.longestSessionSeconds);
        assertEquals(0L, profile.currentSessionStart);
    }

    @Test
    void accruesActiveAndAfkTime() {

        long now = 2_000_000L;

        PlayerProfile profile =
                new PlayerProfile(UUID.randomUUID(), "Steve", now);

        profile.beginSession(now, new HashMap<>());

        profile.lastAccrual = now;
        profile.accrue(now + 10_000L, false, true);
        assertEquals(10L, profile.activeSeconds);
        assertEquals(0L, profile.afkSeconds);

        profile.accrue(now + 25_000L, true, true);
        assertEquals(10L, profile.activeSeconds);
        assertEquals(15L, profile.afkSeconds);
    }

    @Test
    void playtimeFallsBackToSessionTotal() {

        PlayerProfile profile =
                new PlayerProfile(UUID.randomUUID(), "Steve", 0L);

        profile.totalPlaytimeSeconds = 120L;

        assertEquals(120L, profile.total(Metrics.PLAYTIME));
    }
}
