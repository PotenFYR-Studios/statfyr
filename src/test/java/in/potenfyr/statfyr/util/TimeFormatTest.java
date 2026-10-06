package in.potenfyr.statfyr.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for duration formatting.
 */
class TimeFormatTest {

    @Test
    void formatsDurations() {

        assertEquals("0m", TimeFormat.duration(0));
        assertEquals("0m", TimeFormat.duration(-5));
        assertEquals("45s", TimeFormat.duration(45));
        assertEquals("1m 5s", TimeFormat.duration(65));
        assertEquals("1h 1m 5s", TimeFormat.duration(3665));
        assertEquals("1d 0h 0m", TimeFormat.duration(86400));
    }

    @Test
    void groupsNumbers() {

        assertEquals("1,234", TimeFormat.number(1234));
    }
}
