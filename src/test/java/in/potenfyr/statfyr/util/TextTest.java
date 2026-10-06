package in.potenfyr.statfyr.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the string helper.
 */
class TextTest {

    @Test
    void blankChecks() {

        assertTrue(Text.isBlank(null));
        assertTrue(Text.isBlank(""));
        assertTrue(Text.isBlank("   "));
        assertFalse(Text.isBlank("hi"));
        assertTrue(Text.isNotBlank("hi"));
        assertFalse(Text.isNotBlank("  "));
    }

    @Test
    void nullToEmpty() {

        assertEquals("", Text.nullToEmpty(null));
        assertEquals("x", Text.nullToEmpty("x"));
    }
}
