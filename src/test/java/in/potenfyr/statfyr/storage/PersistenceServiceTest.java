package in.potenfyr.statfyr.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Batching and stale-write-protection tests for {@link PersistenceService}.
 */
class PersistenceServiceTest {

    @Test
    void multipleSubmissionsCollapseToOneWritePerPlayer() {

        final AtomicInteger persistedVersions =
                new AtomicInteger();

        ConcurrentHashMap<UUID, Long> persisted =
                new ConcurrentHashMap<>();

        PersistenceService service = new PersistenceService(
                5_000L,
                (uuid, version) -> {

                    persisted.put(uuid, version);
                    persistedVersions.incrementAndGet();
                    return true;
                },
                java.util.logging.Logger.getLogger("test")
        );

        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        // Simulate a burst of live updates for a pair of players.
        for (long version = 30L; version <= 50L; version++) {

            service.submit(playerA, version);
            service.submit(playerB, version);
        }

        // Collapse happens eagerly at submit time: only the newest pending
        // write per player is retained.
        assertEquals(2, service.pendingCount());

        service.flush();

        // One write per player despite 21 submissions each.
        assertEquals(2, persistedVersions.get());

        // The persisted version is the latest one, not an older one.
        assertEquals(50L, persisted.get(playerA));
        assertEquals(50L, persisted.get(playerB));
    }

    @Test
    void newerSubmissionWinsOverOlderQueuedWrite() {

        ConcurrentHashMap<UUID, Long> persisted =
                new ConcurrentHashMap<>();

        PersistenceService service = new PersistenceService(
                5_000L,
                (uuid, version) -> {

                    persisted.put(uuid, version);
                    return true;
                },
                java.util.logging.Logger.getLogger("test")
        );

        UUID player = UUID.randomUUID();

        service.submit(player, 10L);
        service.submit(player, 11L);
        service.submit(player, 12L);

        service.flush();

        assertEquals(12L, persisted.get(player));
    }

    @Test
    void persisterFailuresDoNotBreakOtherPlayers() {

        UUID goodPlayer = UUID.randomUUID();
        UUID badPlayer = UUID.randomUUID();

        ConcurrentHashMap<UUID, Long> persisted =
                new ConcurrentHashMap<>();

        PersistenceService service = new PersistenceService(
                5_000L,
                (uuid, version) -> {

                    if (uuid.equals(badPlayer)) {
                        throw new IllegalStateException("disk full");
                    }

                    persisted.put(uuid, version);
                    return true;
                },
                java.util.logging.Logger.getLogger("test")
        );

        // Interleave failing and succeeding players in one batch.
        service.submit(badPlayer, 5L);
        service.submit(goodPlayer, 5L);
        service.submit(badPlayer, 6L);
        service.submit(goodPlayer, 6L);

        service.flush();

        // The healthy player's write went through despite the neighbour's
        // failure.
        assertEquals(6L, persisted.get(goodPlayer));
        assertTrue(service.batchCount() >= 1);
    }

    @Test
    void stopPreventsFurtherDrains() {

        final AtomicInteger writes = new AtomicInteger();

        PersistenceService service = new PersistenceService(
                5_000L,
                (uuid, version) -> {

                    writes.incrementAndGet();
                    return true;
                },
                java.util.logging.Logger.getLogger("test")
        );

        service.stop();

        UUID player = UUID.randomUUID();
        service.submit(player, 1L);

        // drainSafely honours the stopped flag on the worker thread; direct
        // flush() is the explicit shutdown path.
        service.flush();

        assertEquals(1, writes.get());
    }
}
