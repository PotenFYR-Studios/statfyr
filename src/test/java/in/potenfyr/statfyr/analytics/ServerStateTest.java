package in.potenfyr.statfyr.analytics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for server-level aggregation bookkeeping.
 */
class ServerStateTest {

    @Test
    void recordsSessionsAndUniques() {

        ServerState state =
                new ServerState();

        String uuid =
                UUID.randomUUID().toString();

        state.recordSession("2026-10-05", 18, "MONDAY", uuid, true);
        state.recordSession("2026-10-05", 19, "MONDAY", uuid, false);
        state.recordSession("2026-10-06", 12, "TUESDAY", uuid, false);

        assertEquals(3L, state.totalSessions);
        assertEquals(2, state.sessionsByDay.get("2026-10-05"));
        assertEquals(1, state.newPlayersByDay.get("2026-10-05"));
        assertEquals(1, state.sessionsByHour.get("18"));
        assertEquals(2, state.sessionsByWeekday.get("MONDAY"));

        // The same player twice on one day counts once for that day.
        assertEquals(1, state.playersByDay.get("2026-10-05").size());

        assertEquals(
                1,
                state.uniqueAcross(
                        Arrays.asList("2026-10-05", "2026-10-06")
                ).size()
        );
    }

    @Test
    void recordsPeaks() {

        ServerState state =
                new ServerState();

        state.recordPeak("2026-10-05", 10);
        state.recordPeak("2026-10-05", 7);
        state.recordPeak("2026-10-06", 22);

        assertEquals(22, state.peakAllTime);
        assertEquals(10, state.peakByDay.get("2026-10-05"));
        assertEquals(22, state.peakByDay.get("2026-10-06"));
        assertTrue(state.peakAllTime >= 22);
    }
}
