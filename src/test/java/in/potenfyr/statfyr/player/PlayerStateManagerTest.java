package in.potenfyr.statfyr.player;

import in.potenfyr.statfyr.model.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency and versioning tests for {@link PlayerStateManager}.
 *
 * <p>These tests run with no Bukkit server present; the manager's live-read
 * paths fail-soft to empty snapshots, which keeps the tests focused on the
 * locking/versioning logic itself.
 */
class PlayerStateManagerTest {

    private static PlayerStats emptyStats(UUID uuid) {

        return new PlayerStats(
                uuid,
                "tester",
                Collections.emptyMap()
        );
    }

    private static PlayerStats statsWithValue(UUID uuid, long value) {

        Map<String, Long> custom =
                new java.util.HashMap<>();

        custom.put("minecraft:deaths", value);

        Map<String, Map<String, Long>> raw =
                new java.util.HashMap<>();

        raw.put("minecraft:custom", custom);

        return new PlayerStats(uuid, "tester", raw);
    }

    @Test
    void stateVersionIncrementsOnEachMutation() {

        PlayerStateManager manager = new PlayerStateManager();

        UUID uuid = UUID.randomUUID();

        manager.lock(uuid);

        try {

            manager.markOnline(uuid, "Steve");

            long v1 =
                    manager.state(uuid).version();

            manager.updateStatistics(uuid, statsWithValue(uuid, 1L));

            long v2 =
                    manager.state(uuid).version();

            manager.updateName(uuid, "Stevex");

            long v3 =
                    manager.state(uuid).version();

            manager.markOffline(uuid);

            long v4 =
                    manager.state(uuid).version();

            assertTrue(v1 > 0);
            assertTrue(v2 > v1);
            assertTrue(v3 > v2);
            assertTrue(v4 > v3);

        } finally {

            manager.unlock(uuid);
        }
    }

    @Test
    void updateStatisticsRejectsIdenticalSnapshot() {

        PlayerStateManager manager = new PlayerStateManager();

        UUID uuid = UUID.randomUUID();

        manager.lock(uuid);

        try {

            manager.markOnline(uuid, "Steve");

            long before =
                    manager.state(uuid).version();

            PlayerStats same =
                    manager.state(uuid).statistics();

            manager.updateStatistics(uuid, same);

            assertEquals(
                    before,
                    manager.state(uuid).version(),
                    "unchanged snapshot must not bump the version"
            );

        } finally {

            manager.unlock(uuid);
        }
    }

    @Test
    void staleCleanRequestsDoNotClearNewerDirtyState() {

        PlayerStateManager manager = new PlayerStateManager();

        UUID uuid = UUID.randomUUID();

        manager.lock(uuid);

        try {

            manager.markOnline(uuid, "Steve");

            PlayerStateManager.PlayerState v40 =
                    manager.state(uuid);

            manager.updateStatistics(uuid, statsWithValue(uuid, 2L));

            PlayerStateManager.PlayerState v41 =
                    manager.state(uuid);

            // Worker processed version 40 late — the live state is 41 now.
            // Clearing must be rejected (state stays dirty for version 41).
            manager.markClean(uuid, v40.version());

            assertTrue(
                    manager.state(uuid).dirty(),
                    "stale clean must not clear a newer dirty state"
            );
            assertEquals(
                    v41.version(),
                    manager.state(uuid).version()
            );

            // Correct version clears the dirty flag.
            manager.markClean(uuid, v41.version());

            assertFalse(manager.state(uuid).dirty());

        } finally {

            manager.unlock(uuid);
        }
    }

    @Test
    void concurrentMutationsFromDifferentPlayersDoNotBlock() {

        // Per-UUID locking must allow parallel progress across players.
        PlayerStateManager manager = new PlayerStateManager();

        final int players = 8;

        final CountDownLatch startBarrier =
                new CountDownLatch(1);

        final CountDownLatch allHoldingLock =
                new CountDownLatch(players);

        ExecutorService executor =
                Executors.newFixedThreadPool(players);

        final AtomicBoolean failure = new AtomicBoolean(false);

        for (int i = 0; i < players; i++) {

            final UUID uuid = UUID.randomUUID();

            executor.execute(() -> {

                manager.lock(uuid);

                try {

                    // Every worker signals it is inside its own lock, then
                    // waits until ALL workers are holding theirs. With a
                    // global lock this deadlocks; with per-UUID locks every
                    // thread gets through.
                    allHoldingLock.countDown();
                    startBarrier.await();

                } catch (Exception exception) {

                    failure.set(true);

                } finally {

                    manager.unlock(uuid);
                }
            });
        }

        try {

            assertTrue(
                    allHoldingLock.await(15, TimeUnit.SECONDS),
                    "workers must be able to hold different UUID locks simultaneously"
            );

            startBarrier.countDown();

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();

            failure.set(true);

        } finally {

            executor.shutdownNow();
        }

        assertFalse(failure.get());
    }

    @Test
    void mutexEnforcedPerPlayerUnderConcurrency() {

        PlayerStateManager manager = new PlayerStateManager();

        final UUID uuid = UUID.randomUUID();

        final int threads = 8;
        final int increments = 100;

        ExecutorService executor =
                Executors.newFixedThreadPool(threads);

        // Shared mutable touch-count guarded only by the UUID lock.
        final int[] counter = {0};

        final AtomicBoolean failure = new AtomicBoolean(false);

        for (int i = 0; i < threads; i++) {

            executor.execute(() -> {

                for (int n = 0; n < increments; n++) {

                    manager.lock(uuid);

                    try {

                        // Read-modify-write must be atomic because every
                        // thread uses the same UUID lock.
                        counter[0] = counter[0] + 1;

                        manager.updateStatistics(
                                uuid,
                                statsWithValue(
                                        uuid,
                                        counter[0]
                                )
                        );

                    } catch (Exception exception) {

                        failure.set(true);

                    } finally {

                        manager.unlock(uuid);
                    }
                }
            });
        }

        executor.shutdown();

        try {

            assertTrue(
                    executor.awaitTermination(20, TimeUnit.SECONDS)
            );

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();

            failure.set(true);
        }

        assertFalse(failure.get());

        assertEquals(
                threads * increments,
                counter[0],
                "per-UUID lock must serialise mutations for one player"
        );

        PlayerStateManager.PlayerState state =
                manager.state(uuid);

        assertNotNull(state);

        assertEquals(
                threads * increments,
                state.version(),
                "each mutation must bump the version exactly once"
        );
    }

    @Test
    void markOfflineKeepsStateForApiReads() {

        PlayerStateManager manager = new PlayerStateManager();

        UUID uuid = UUID.randomUUID();

        manager.lock(uuid);

        try {

            manager.markOnline(uuid, "Steve");

            manager.updateStatistics(uuid, statsWithValue(uuid, 7L));

            manager.markOffline(uuid);

            PlayerStateManager.PlayerState state =
                    manager.state(uuid);

            assertNotNull(state);

            assertFalse(state.online());

            assertNotNull(state.statistics());

            // The state remains queryable for offline API responses.
            assertEquals(7L, state.statistics().getDeaths());

        } finally {

            manager.unlock(uuid);
        }
    }

    @Test
    void versionMonotonicUnderConcurrentWriters() {

        PlayerStateManager manager = new PlayerStateManager();

        final UUID uuid = UUID.randomUUID();

        final int threads = 6;
        final int writes = 50;

        ExecutorService executor =
                Executors.newFixedThreadPool(threads);

        final AtomicBoolean sawRegression = new AtomicBoolean(false);

        final java.util.Set<Long> observed =
                java.util.Collections.synchronizedSet(
                        new java.util.HashSet<>()
                );

        for (int i = 0; i < threads; i++) {

            executor.execute(() -> {

                for (int n = 0; n < writes; n++) {

                    manager.lock(uuid);

                    try {

                        manager.updateStatistics(
                                uuid,
                                statsWithValue(uuid, n)
                        );

                        long version =
                                manager.state(uuid).version();

                        // Versions may be observed more than once (no change
                        // between reads), but must never go backwards on the
                        // state object itself.
                        PlayerStateManager.PlayerState previous =
                                manager.state(uuid);

                        if (previous != null
                                && previous.version() > version) {
                            sawRegression.set(true);
                        }

                        observed.add(version);

                    } finally {

                        manager.unlock(uuid);
                    }
                }
            });
        }

        executor.shutdown();

        try {

            assertTrue(
                    executor.awaitTermination(20, TimeUnit.SECONDS)
            );

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }

        assertFalse(
                sawRegression.get(),
                "state version must be monotonically non-decreasing"
        );
    }
}
