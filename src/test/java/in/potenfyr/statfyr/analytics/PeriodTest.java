package in.potenfyr.statfyr.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for period parsing and window boundaries.
 */
class PeriodTest {

    @Test
    void parsesPeriodNames() {

        assertEquals(Period.DAILY, Period.from("daily"));
        assertEquals(Period.DAILY, Period.from("day"));
        assertEquals(Period.WEEKLY, Period.from("weekly"));
        assertEquals(Period.MONTHLY, Period.from("monthly"));
        assertEquals(Period.ALL_TIME, Period.from("alltime"));
        assertEquals(Period.ALL_TIME, Period.from("all-time"));
        assertEquals(Period.ALL_TIME, Period.from(null));
    }

    @Test
    void allTimeStartsAtEpoch() {

        assertEquals(0L, Period.ALL_TIME.start(1_000_000L, null, 0, 1));
    }

    @Test
    void dailyWindowIsWithinTheDay() {

        long now =
                System.currentTimeMillis();

        long start =
                Period.DAILY.start(now, null, 0, 1);

        long end =
                Period.DAILY.end(now);

        org.junit.jupiter.api.Assertions.assertTrue(start <= now);
        org.junit.jupiter.api.Assertions.assertTrue(end > now);
        org.junit.jupiter.api.Assertions.assertTrue(end - start <= 25L * 60L * 60L * 1000L);
    }
}
