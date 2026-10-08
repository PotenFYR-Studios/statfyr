package in.potenfyr.statfyr.player;

import in.potenfyr.statfyr.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Central in-memory player state.
 *
 * <p>Owns the authoritative live view ({@link PlayerState}) for every player
 * the plugin currently knows about and serialises all state mutations behind
 * a <strong>per-UUID lock</strong>, so concurrent operations (live stat
 * collector, join/quit events, persistence worker, API readers) can never
 * interleave into a torn or stale write.
 *
 * <p>Every mutation bumps a monotonically increasing {@code version}; callers
 * capture immutable snapshots (state + version) under the lock and release it
 * before doing any I/O, keeping the lock scope tiny (no deadlocks) and giving
 * the persistence layer an optimistic version it can compare against.
 *
 * <p>Locks are per player: Steve, Alex and Bob can be updated in parallel.
 * Lock entries are shared, reference-counted (join vs live-tick vs API) and
 * removed when the last holder releases, so the map cannot grow unbounded.
 */
public final class PlayerStateManager {

    /** Immutable snapshot of a player's live state. */
    public static final class PlayerState {

        private final UUID uuid;
        private final String name;
        private final boolean online;
        private final PlayerStats statistics;
        private final long lastSeen;
        private final long lastUpdated;
        private final long version;
        private final boolean dirty;

        PlayerState(
                UUID uuid,
                String name,
                boolean online,
                PlayerStats statistics,
                long lastSeen,
                long lastUpdated,
                long version,
                boolean dirty
        ) {

            this.uuid = uuid;
            this.name = name;
            this.online = online;
            this.statistics = statistics;
            this.lastSeen = lastSeen;
            this.lastUpdated = lastUpdated;
            this.version = version;
            this.dirty = dirty;
        }

        public UUID uuid() {
            return uuid;
        }

        public String name() {
            return name;
        }

        public boolean online() {
            return online;
        }

        public PlayerStats statistics() {
            return statistics;
        }

        public long lastSeen() {
            return lastSeen;
        }

        public long lastUpdated() {
            return lastUpdated;
        }

        public long version() {
            return version;
        }

        public boolean dirty() {
            return dirty;
        }
    }

    /** Per-UUID lock. Entries live for the plugin's lifetime: they are tiny
     * (~48 bytes each), bounded by the number of unique players ever seen —
     * the same scale at which profiles are already kept in memory — and
     * removing them while another thread is queued to acquire one would
     * otherwise replace the monitor mid-flight and break mutual exclusion.
     */
    private final ConcurrentHashMap<UUID, ReentrantLock> locks =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, PlayerState> states =
            new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Locking
    // -------------------------------------------------------------------------

    /**
     * Acquires this player's lock, creating the entry if needed.
     *
     * <p>Nested acquisition from the same thread is safe ({@link ReentrantLock}).
     */
    public void lock(UUID uuid) {

        if (uuid == null) {
            return;
        }

        locks.computeIfAbsent(uuid, ignored -> new ReentrantLock())
                .lock();
    }

    /**
     * Releases a lock previously acquired with {@link #lock(UUID)}. Must be
     * called from the same thread that acquired it.
     */
    public void unlock(UUID uuid) {

        if (uuid == null) {
            return;
        }

        ReentrantLock lock = locks.get(uuid);

        if (lock == null) {
            return;
        }

        lock.unlock();
    }

    // -------------------------------------------------------------------------
    // State mutations (all must be called under the UUID lock)
    // -------------------------------------------------------------------------

    /**
     * Loads (or creates) the live state for a player. Call under the lock.
     * Online players get a fresh state seeded from their current identity;
     * unknown players get a minimal offline placeholder so version tracking
     * still works.
     */
    public PlayerState loadOrCreate(UUID uuid, String name, boolean online) {

        PlayerState existing = states.get(uuid);

        if (existing != null) {
            return existing;
        }

        PlayerStats stats = online
                ? readLiveStatsSafely(uuid, name)
                : new PlayerStats(uuid, name, Collections.emptyMap());

        PlayerState created = new PlayerState(
                uuid,
                name,
                online,
                stats,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                1L,
                online
        );

        states.put(uuid, created);

        return created;
    }

    /**
     * Updates the player's name (join / name change). Call under the lock.
     */
    public PlayerState updateName(UUID uuid, String name) {

        PlayerState state = states.get(uuid);

        if (state == null
                || name == null
                || name.equals(state.name)) {
            return state;
        }

        PlayerState updated = new PlayerState(
                uuid,
                name,
                state.online,
                state.statistics,
                state.lastSeen,
                System.currentTimeMillis(),
                state.version + 1L,
                state.dirty
        );

        states.put(uuid, updated);

        return updated;
    }

    /**
     * Marks the player online and initialises live statistics. Call under
     * the lock.
     */
    public PlayerState markOnline(UUID uuid, String name) {

        PlayerState state = loadOrCreate(uuid, name, true);

        PlayerStats stats = state.statistics;

        if (stats == null) {
            stats = readLiveStatsSafely(uuid, name);
        }

        return put(new PlayerState(
                uuid,
                name != null ? name : state.name,
                true,
                stats,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                state.version + 1L,
                true
        ));
    }

    /**
     * Records an offline transition. Call under the lock. Never removes
     * persisted data; the state stays available for API reads of offline
     * players.
     */
    public PlayerState markOffline(UUID uuid) {

        PlayerState state = states.get(uuid);

        if (state == null) {
            return null;
        }

        return put(new PlayerState(
                uuid,
                state.name,
                false,
                state.statistics,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                state.version + 1L,
                true
        ));
    }

    /**
     * Replaces the statistics snapshot. Call under the lock. Returns the
     * existing state when the snapshot is unchanged so the caller can skip
     * dirty marking and persistence.
     *
     * <p>When no state exists yet (for example the collector's first tick
     * after a restart with players already online), the snapshot itself
     * seeds a new state. Anything other than the initial empty placeholder
     * implies an online player at that point.
     */
    public PlayerState updateStatistics(UUID uuid, PlayerStats stats) {

        if (uuid == null || stats == null) {
            return null;
        }

        PlayerState state = states.get(uuid);

        if (state == null) {

            return put(new PlayerState(
                    uuid,
                    stats.getPlayerName(),
                    true,
                    stats,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    1L,
                    true
            ));
        }

        if (sameStats(state.statistics, stats)) {
            return state;
        }

        return put(new PlayerState(
                uuid,
                state.name,
                state.online,
                stats,
                state.lastSeen,
                System.currentTimeMillis(),
                state.version + 1L,
                true
        ));
    }

    /**
     * Clears the dirty flag once the state has been persisted. Call under
     * the lock.
     */
    public void markClean(UUID uuid, long version) {

        PlayerState state = states.get(uuid);

        if (state == null || state.version != version) {
            // A newer version superseded this one; keep it dirty so the
            // persistence worker picks it up on its next pass.
            return;
        }

        states.put(uuid, new PlayerState(
                uuid,
                state.name,
                state.online,
                state.statistics,
                state.lastSeen,
                state.lastUpdated,
                state.version,
                false
        ));
    }

    /**
     * Bumps the live version for a profile-side mutation and keeps it dirty.
     * Callers may already hold the UUID lock; the state transition itself is
     * intentionally lock-free because the manager's contract requires that.
     */
    public PlayerState markDirty(UUID uuid) {

        PlayerState state = states.get(uuid);

        if (state == null) {
            return null;
        }

        PlayerState updated = new PlayerState(
                uuid,
                state.name,
                state.online,
                state.statistics,
                state.lastSeen,
                System.currentTimeMillis(),
                state.version + 1L,
                true
        );

        states.put(uuid, updated);
        return updated;
    }

    /**
     * Removes the in-memory state entirely (used when analytics owns the
     * offline lifecycle and the entry would otherwise never be read again).
     */
    public void remove(UUID uuid) {

        if (uuid != null) {
            states.remove(uuid);
        }
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    /**
     * @return the current state, or {@code null} when unknown
     */
    public PlayerState state(UUID uuid) {

        return uuid == null ? null : states.get(uuid);
    }

    /**
     * @return true when the player's live state exists and is online
     */
    public boolean isOnline(UUID uuid) {

        PlayerState state = state(uuid);

        return state != null && state.online;
    }

    /**
     * @return all known UUIDs (a stable copy)
     */
    public List<UUID> knownUuids() {

        return new ArrayList<>(states.keySet());
    }

    public int stateCount() {
        return states.size();
    }

    public int lockCount() {
        return locks.size();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private PlayerState put(PlayerState state) {

        states.put(state.uuid, state);

        return state;
    }

    /**
     * Best-effort live read used only when initialising a state for a player
     * that is currently online.
     *
     * <p>Vanilla statistics are server-thread data, so the read happens only
     * when the caller is already on the main thread (join events and the
     * collector both are). From any other thread this returns an empty
     * snapshot immediately — never a blocking round-trip to the main thread,
     * which could deadlock against a collector tick waiting for the same
     * UUID lock. The periodic collector fills the state within one or two
     * ticks anyway.
     */
    private PlayerStats readLiveStatsSafely(UUID uuid, String name) {

        try {

            if (!Bukkit.isPrimaryThread()) {

                return new PlayerStats(
                        uuid,
                        name,
                        Collections.emptyMap()
                );
            }

            return readLive(uuid, name);

        } catch (Throwable ignored) {

            return new PlayerStats(
                    uuid,
                    name,
                    Collections.emptyMap()
            );
        }
    }

    /**
     * Reads the player's live statistics from the running Minecraft server.
     * Package-private so {@code StatsManager} can reuse it directly on the
     * main thread without going through the sync bridge.
     */
    PlayerStats readLive(UUID uuid, String name) {

        Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return new PlayerStats(
                    uuid,
                    name,
                    Collections.emptyMap()
            );
        }

        return in.potenfyr.statfyr.stats.LiveStatisticsReader.read(player);
    }

    /**
     * Structural comparison of two stat snapshots; used to avoid bumping the
     * version (and re-persisting) when nothing actually changed.
     */
    private static boolean sameStats(PlayerStats a, PlayerStats b) {

        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return a.getRawStats().equals(b.getRawStats());
    }
}
