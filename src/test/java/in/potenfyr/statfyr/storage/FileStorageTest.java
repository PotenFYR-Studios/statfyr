package in.potenfyr.statfyr.storage;

import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.analytics.ServerState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the file based storage backend.
 */
class FileStorageTest {

    @TempDir
    File tempDir;

    private FileStorage newStorage() {

        FileStorage storage =
                new FileStorage(tempDir, Logger.getLogger("test"));

        storage.init();
        return storage;
    }

    @Test
    void profileRoundTrip() {

        FileStorage storage =
                newStorage();

        UUID uuid =
                UUID.randomUUID();

        PlayerProfile profile =
                new PlayerProfile(uuid, "Steve", 1000L);

        profile.totalSessions = 5;
        profile.totalPlaytimeSeconds = 3600L;
        profile.allTime.put("kills", 42L);

        storage.saveProfile(profile);

        PlayerProfile loaded =
                storage.loadProfile(uuid);

        assertNotNull(loaded);
        assertEquals("Steve", loaded.name);
        assertEquals(5, loaded.totalSessions);
        assertEquals(3600L, loaded.totalPlaytimeSeconds);
        assertEquals(42L, loaded.allTime("kills"));
    }

    @Test
    void snapshotRoundTripAndPrune() {

        FileStorage storage =
                newStorage();

        UUID uuid =
                UUID.randomUUID();

        long now =
                System.currentTimeMillis();

        Map<String, Long> metrics =
                new HashMap<>();

        metrics.put("kills", 10L);

        storage.appendSnapshot(
                uuid,
                new Snapshot(now, metrics)
        );

        storage.appendSnapshot(
                uuid,
                new Snapshot(now - 400L * 24L * 60L * 60L * 1000L, metrics)
        );

        List<Snapshot> all =
                storage.readPlayerHistory(uuid, 0L, now + 1, 100);

        assertEquals(2, all.size());

        storage.prune(365L);

        List<Snapshot> pruned =
                storage.readPlayerHistory(uuid, 0L, now + 1, 100);

        assertEquals(1, pruned.size());
    }

    @Test
    void serverStateRoundTrip() {

        FileStorage storage =
                newStorage();

        ServerState state =
                new ServerState();

        state.serverId = "survival-1";
        state.peakAllTime = 42;
        state.knownPlayers.add(UUID.randomUUID().toString());

        storage.saveServerState(state);

        ServerState loaded =
                storage.loadServerState();

        assertEquals("survival-1", loaded.serverId);
        assertEquals(42, loaded.peakAllTime);
        assertEquals(1, loaded.knownPlayers.size());
    }

    @Test
    void knownProfileIds() {

        FileStorage storage =
                newStorage();

        UUID uuid =
                UUID.randomUUID();

        storage.saveProfile(
                new PlayerProfile(uuid, "Alex", 1L)
        );

        assertTrue(
                storage.knownProfileIds().contains(uuid)
        );
    }

    @Test
    void periodArchiveRoundTrip() {

        FileStorage storage =
                newStorage();

        long now =
                System.currentTimeMillis();

        java.util.List<PeriodArchive.Entry> entries =
                new java.util.ArrayList<>();

        entries.add(
                new PeriodArchive.Entry("uuid-1", "Steve", 42L, 42.0)
        );

        storage.appendPeriodArchive(
                new PeriodArchive(
                        "weekly",
                        "kills",
                        now - 1000L,
                        now,
                        now,
                        entries
                )
        );

        java.util.List<PeriodArchive> archives =
                storage.readPeriodArchives("weekly", 0L, now + 1, 10);

        assertEquals(1, archives.size());
        assertEquals("kills", archives.get(0).metric);
        assertEquals(1, archives.get(0).entries.size());
        assertEquals("Steve", archives.get(0).entries.get(0).name);

        assertEquals(
                0,
                storage.readPeriodArchives("monthly", 0L, now + 1, 10).size()
        );
    }
}
