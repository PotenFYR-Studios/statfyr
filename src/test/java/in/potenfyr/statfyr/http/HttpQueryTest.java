package in.potenfyr.statfyr.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for query parameter and time-range parsing.
 */
class HttpQueryTest {

    @Test
    void parsesQueryParameters() {

        Map<String, String> params =
                HttpQuery.parse(
                        URI.create("http://x/api?limit=5&page=2&search=ste")
                );

        assertEquals("5", params.get("limit"));
        assertEquals("2", params.get("page"));
        assertEquals("ste", params.get("search"));
    }

    @Test
    void handlesMissingQuery() {

        assertTrue(
                HttpQuery.parse(URI.create("http://x/api")).isEmpty()
        );
    }

    @Test
    void parsesAndClampsIntegers() {

        Map<String, String> params =
                HttpQuery.parse(URI.create("http://x?limit=9999&page=abc"));

        assertEquals(
                100,
                HttpQuery.parseInt(params, "limit", 25, 1, 100)
        );

        assertEquals(
                25,
                HttpQuery.parseInt(params, "page", 25, 1, 100)
        );
    }

    @Test
    void parsesRelativeTimes() {

        long now =
                System.currentTimeMillis();

        long sevenDaysAgo =
                HttpQuery.parseTime(
                        HttpQuery.parse(URI.create("http://x?from=7d")),
                        "from",
                        now
                );

        assertTrue(sevenDaysAgo < now);
        assertTrue(sevenDaysAgo > now - 8L * 24L * 60L * 60L * 1000L);

        long epoch =
                HttpQuery.parseTime(
                        HttpQuery.parse(URI.create("http://x?from=1234567890")),
                        "from",
                        0L
                );

        assertEquals(1234567890L, epoch);
    }
}
