package in.potenfyr.statfyr.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the pure version parsing/comparison helpers.
 */
class VersionMathTest {

    @Test
    void parsesClassicAndCalendarVersions() {

        assertArrayEquals(new int[]{1, 16, 5}, VersionMath.parse("1.16.5"));
        assertArrayEquals(new int[]{1, 21, 4}, VersionMath.parse("1.21.4-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{26, 3}, VersionMath.parse("26.3"));
        assertArrayEquals(new int[]{26, 3}, VersionMath.parse("v26.3"));
    }

    @Test
    void comparesVersions() {

        assertTrue(VersionMath.compare(
                VersionMath.parse("1.16.5"),
                VersionMath.parse("1.17")
        ) < 0);

        assertTrue(VersionMath.compare(
                VersionMath.parse("26.3"),
                VersionMath.parse("1.21.4")
        ) > 0);

        assertEquals(0, VersionMath.compare(
                VersionMath.parse("1.21"),
                VersionMath.parse("1.21.0")
        ));
    }

    @Test
    void atLeastHelper() {

        assertTrue(VersionMath.atLeast("26.3", 1, 13));
        assertFalse(VersionMath.atLeast("1.12.2", 1, 13));
    }
}
