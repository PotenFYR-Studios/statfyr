package in.potenfyr.statfyr.analytics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for network peer aggregation.
 */
class NetworkRegistryTest {

    @Test
    @SuppressWarnings("unchecked")
    void aggregatesLocalAndPeers() {

        NetworkRegistry registry =
                new NetworkRegistry();

        registry.report(
                new PeerReport("skyblock", "Skyblock", 61, 1200, 500, 90)
        );
        registry.report(
                new PeerReport("prison", "Prison", 47, 900, 400, 70)
        );

        PeerReport local =
                new PeerReport("survival", "Survival", 82, 2000, 900, 128);

        Map<String, Object> aggregate =
                registry.aggregate(local);

        assertEquals(82 + 61 + 47, aggregate.get("network_players"));
        assertEquals(2000 + 1200 + 900, aggregate.get("network_total_players"));
        assertEquals(3, aggregate.get("server_count"));

        List<Object> servers =
                (List<Object>) aggregate.get("servers");

        assertEquals(3, servers.size());

        Map<String, Object> first =
                (Map<String, Object>) servers.get(0);

        assertEquals("survival", first.get("id"));
        assertEquals(true, first.get("local"));
    }

    @Test
    void rejectsInvalidReports() {

        NetworkRegistry registry =
                new NetworkRegistry();

        registry.report(null);
        registry.report(new PeerReport(null, "x", 1, 1, 1, 1));
        registry.report(new PeerReport("", "x", 1, 1, 1, 1));

        assertEquals(0, registry.size());
    }
}
