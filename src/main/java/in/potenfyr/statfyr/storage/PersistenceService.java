package in.potenfyr.statfyr.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Batched, single-writer persistence queue with stale-write protection.
 *
 * <p>Live statistic changes must not hit storage on every tick, so callers
 * only <em>submit</em> a (uuid, version) pair here. The worker drains the
 * queue every few seconds and — crucially — keeps only the
 * <strong>latest version per player</strong> from each drain window:
 * intermediate versions become no-ops instead of pending disk writes.
 *
 * <p>Because the worker persists one snapshot per player per batch, an older
 * write can never overtake a newer one: once version N+1 has been enqueued,
 * a queued version N is dropped, and the callback implementations re-check
 * the live version under the player's UUID lock before writing.
 *
 * <p>All disk I/O happens on the persistence worker thread; the Minecraft
 * main thread only ever calls the non-blocking {@link #submit} method.
 *
 * <p>This batches the JSON profile/state writes from
 * {@code AnalyticsManager}/{@code StatsManager}; the existing
 * {@link Storage} backend (and any future SQL backend) is untouched.
 */
public final class PersistenceService {

    /**
     * Work item: persist this player's state captured at the given version.
     */
    public static final class PendingWrite {

        private final UUID uuid;
        private final long version;

        PendingWrite(UUID uuid, long version) {

            this.uuid = uuid;
            this.version = version;
        }

        public UUID uuid() {
            return uuid;
        }

        public long version() {
            return version;
        }
    }

    /**
     * Callback that performs the actual (safe) persistence for one player.
     * Implementations must re-validate the version under the player's UUID
     * lock and skip the write when the live state has already moved on.
     */
    public interface Persister {

        /**
         * @param uuid    player to persist
         * @param version the version this write was scheduled for
         * @return {@code true} when a write actually happened
         */
        boolean persist(UUID uuid, long version);
    }

    /**
     * How often the worker drains the queue (milliseconds). Configurable via
     * {@code collection.persistence-interval-seconds}.
     */
    private final long intervalMillis;

    private final ConcurrentHashMap<UUID, PendingWrite> pending =
            new ConcurrentHashMap<>();

    private final Persister persister;

    private final Logger logger;

    private final AtomicLong batches = new AtomicLong();

    private final AtomicLong writes = new AtomicLong();

    private final AtomicLong droppedStale = new AtomicLong();

    private volatile boolean running;

    public PersistenceService(
            long intervalMillis,
            Persister persister,
            Logger logger
    ) {

        this.intervalMillis = Math.max(1000L, intervalMillis);
        this.persister = persister;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Submission (non-blocking, thread-safe)
    // -------------------------------------------------------------------------

    /**
     * Schedules a persistence write for the player's current state.
     *
     * <p>Multiple submissions between worker passes collapse into one write;
     * the highest version always wins. Never blocks the caller.
     *
     * @param uuid    player whose state changed
     * @param version the version observed for that state
     */
    public void submit(UUID uuid, long version) {

        if (uuid == null) {
            return;
        }

        pending.merge(
                uuid,
                new PendingWrite(uuid, version),
                (existing, candidate) ->
                        candidate.version >= existing.version
                                ? candidate
                                : existing
        );
    }

    // -------------------------------------------------------------------------
    // Worker control
    // -------------------------------------------------------------------------

    /**
     * Starts the background worker on the provided executor.
     *
     * @param executor executor to run the worker on
     */
    public void start(java.util.concurrent.ScheduledExecutorService executor) {

        running = true;

        executor.scheduleWithFixedDelay(
                this::drainSafely,
                intervalMillis,
                intervalMillis,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    public void stop() {

        running = false;
    }

    /**
     * Drains everything currently pending synchronously; used at shutdown so
     * the last live values reach storage before the process exits.
     */
    public void flush() {

        drain();
    }

    // -------------------------------------------------------------------------
    // Drain
    // -------------------------------------------------------------------------

    private void drainSafely() {

        if (!running) {
            return;
        }

        try {

            drain();

        } catch (Throwable throwable) {

            logger.warning(
                    "Persistence batch failed: " + throwable.getMessage()
            );
        }
    }

    /**
     * One persistence pass: the latest pending write per player goes to the
     * persister, intermediate versions are discarded as stale.
     */
    void drain() {

        if (pending.isEmpty()) {
            return;
        }

        List<PendingWrite> batch = new ArrayList<>(pending.size());

        for (Map.Entry<UUID, PendingWrite> entry : pending.entrySet()) {

            PendingWrite write = entry.getValue();

            // Only the newest queued version per player is kept; earlier
            // submissions are stale by definition.
            if (pending.remove(entry.getKey(), write)
                    && write != null) {

                // Re-check: a newer write may have raced in after our remove.
                PendingWrite newest = pending.get(entry.getKey());

                if (newest != null
                        && newest.version > write.version) {
                    continue;
                }

                batch.add(write);
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        batches.incrementAndGet();

        for (PendingWrite write : batch) {

            try {

                if (persister.persist(write.uuid(), write.version())) {
                    writes.incrementAndGet();
                }

            } catch (Throwable throwable) {

                logger.warning(
                        "Failed to persist player " + write.uuid()
                                + ": " + throwable.getMessage()
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    public long batchCount() {
        return batches.get();
    }

    public long writeCount() {
        return writes.get();
    }

    public long droppedStaleCount() {
        return droppedStale.get();
    }

    public int pendingCount() {
        return pending.size();
    }
}
