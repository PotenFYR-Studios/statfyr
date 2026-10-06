package in.potenfyr.statfyr.storage;

import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.analytics.ServerState;

import java.util.List;
import java.util.UUID;

/**
 * Persistent analytics storage abstraction.
 *
 * <p>The default implementation is an append-only, file based time-series
 * store. Keeping this behind an interface lets a SQL backend be added later
 * without touching the analytics layer.
 */
public interface Storage {

    /**
     * Prepares the storage backend.
     *
     * @throws Exception when the backend cannot be initialised
     */
    void init() throws Exception;

    // -- profiles ------------------------------------------------------------

    PlayerProfile loadProfile(UUID uuid);

    void saveProfile(PlayerProfile profile);

    List<UUID> knownProfileIds();

    void deleteProfile(UUID uuid);

    // -- player history ------------------------------------------------------

    void appendSnapshot(UUID uuid, Snapshot snapshot);

    List<Snapshot> readPlayerHistory(UUID uuid, long from, long to, int limit);

    // -- server history ------------------------------------------------------

    void appendServerSnapshot(ServerSnapshot snapshot);

    List<ServerSnapshot> readServerHistory(long from, long to, int limit);

    // -- activity timeline ---------------------------------------------------

    void appendActivity(UUID uuid, ActivityEvent event);

    List<ActivityEvent> readActivity(UUID uuid, long from, long to, int limit);

    // -- archived leaderboards ----------------------------------------------

    void appendPeriodArchive(PeriodArchive archive);

    List<PeriodArchive> readPeriodArchives(
            String period,
            long from,
            long to,
            int limit
    );

    // -- server state --------------------------------------------------------

    ServerState loadServerState();

    void saveServerState(ServerState state);

    // -- maintenance ---------------------------------------------------------

    /**
     * Removes history older than the given retention window.
     *
     * @param retentionDays days to keep, or {@code 0} for unlimited
     */
    void prune(long retentionDays);

    /** Deletes all historical snapshots and activity while keeping profiles. */
    void purgeHistory();

    /** Flushes and releases resources. */
    void close();
}
