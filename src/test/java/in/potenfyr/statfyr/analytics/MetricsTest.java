package in.potenfyr.statfyr.analytics;

import in.potenfyr.statfyr.model.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for canonical metric extraction and alias resolution.
 */
class MetricsTest {

    @Test
    void resolvesAliases() {

        assertEquals(Metrics.KILLS, Metrics.canonical("kills"));
        assertEquals(Metrics.KILLS, Metrics.canonical("kill"));
        assertEquals(Metrics.BLOCKS_MINED, Metrics.canonical("mined"));
        assertEquals(Metrics.BLOCKS_MINED, Metrics.canonical("blocks_mined"));
        assertEquals(Metrics.DISTANCE_TRAVELED, Metrics.canonical("distance"));
        assertEquals(Metrics.KDR, Metrics.canonical("kdr"));
        assertEquals(Metrics.PLAYTIME, Metrics.canonical("play-time"));
    }

    @Test
    void extractsMetricsFromStats() {

        Map<String, Map<String, Long>> raw =
                new HashMap<>();

        Map<String, Long> custom =
                new HashMap<>();

        custom.put("minecraft:deaths", 3L);
        custom.put("minecraft:player_kills", 10L);
        custom.put("minecraft:mob_kills", 5L);
        custom.put("minecraft:damage_dealt", 100L);
        custom.put("minecraft:damage_taken", 50L);
        custom.put("minecraft:play_time", 1200L);
        custom.put("minecraft:jump", 7L);

        raw.put("minecraft:custom", custom);

        PlayerStats stats =
                new PlayerStats(
                        UUID.randomUUID(),
                        "Steve",
                        raw
                );

        Map<String, Long> metrics =
                Metrics.extract(stats);

        assertEquals(15L, metrics.get(Metrics.KILLS));
        assertEquals(3L, metrics.get(Metrics.DEATHS));
        assertEquals(10L, metrics.get(Metrics.PLAYER_KILLS));
        assertEquals(5L, metrics.get(Metrics.MOB_KILLS));
        assertEquals(7L, metrics.get(Metrics.JUMPS));
        assertEquals(60L, metrics.get(Metrics.PLAYTIME));
    }
}
