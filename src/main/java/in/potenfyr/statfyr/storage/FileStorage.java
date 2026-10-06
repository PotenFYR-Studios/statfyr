package in.potenfyr.statfyr.storage;

import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.analytics.ServerState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File based, append-only analytics storage.
 *
 * <pre>
 * data/
 *   server.json
 *   players/&lt;uuid&gt;.json
 *   history/
 *     server.jsonl
 *     players/&lt;uuid&gt;.jsonl
 *     activity/&lt;uuid&gt;.jsonl
 * </pre>
 *
 * <p>Time-series data is stored as JSON Lines so it can be streamed and
 * pruned without loading everything into memory. Profiles are small and are
 * loaded on demand.
 */
public final class FileStorage implements Storage {

    private final File baseDir;
    private final File playersDir;
    private final File historyDir;
    private final File playerHistoryDir;
    private final File activityDir;
    private final Logger logger;

    public FileStorage(File dataFolder, Logger logger) {

        this.baseDir =
                new File(dataFolder, "data");

        this.playersDir =
                new File(baseDir, "players");

        this.historyDir =
                new File(baseDir, "history");

        this.playerHistoryDir =
                new File(historyDir, "players");

        this.activityDir =
                new File(historyDir, "activity");

        this.logger = logger;
    }

    @Override
    public void init() {

        ensureDir(baseDir);
        ensureDir(playersDir);
        ensureDir(historyDir);
        ensureDir(playerHistoryDir);
        ensureDir(activityDir);
    }

    // -- profiles ------------------------------------------------------------

    @Override
    public PlayerProfile loadProfile(UUID uuid) {

        File file =
                profileFile(uuid);

        if (!file.exists()) {
            return null;
        }

        try {

            String json =
                    readFile(file);

            if (json.trim().isEmpty()) {
                return null;
            }

            return JsonIO.fromJson(json, PlayerProfile.class);

        } catch (Exception exception) {

            logger.log(
                    Level.WARNING,
                    "Failed to read profile " + uuid,
                    exception
            );

            return null;
        }
    }

    @Override
    public void saveProfile(PlayerProfile profile) {

        if (profile == null || profile.uuid == null) {
            return;
        }

        UUID uuid;

        try {
            uuid = UUID.fromString(profile.uuid);

        } catch (Exception exception) {
            return;
        }

        File target =
                profileFile(uuid);

        File temp =
                new File(
                        playersDir,
                        uuid + ".tmp"
                );

        try {

            writeFile(temp, JsonIO.toJson(profile));

            Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception exception) {

            logger.log(
                    Level.WARNING,
                    "Failed to save profile " + uuid,
                    exception
            );
        }
    }

    @Override
    public List<UUID> knownProfileIds() {

        List<UUID> ids =
                new ArrayList<>();

        File[] files =
                playersDir.listFiles(
                        (dir, name) -> name.endsWith(".json")
                );

        if (files == null) {
            return ids;
        }

        for (File file : files) {

            try {

                String name =
                        file.getName()
                                .replace(".json", "");

                ids.add(UUID.fromString(name));

            } catch (Exception ignored) {
            }
        }

        return ids;
    }

    @Override
    public void deleteProfile(UUID uuid) {

        delete(profileFile(uuid));
        delete(playerHistoryFile(uuid));
        delete(activityFile(uuid));
    }

    // -- player history ------------------------------------------------------

    @Override
    public void appendSnapshot(UUID uuid, Snapshot snapshot) {

        appendLine(
                playerHistoryFile(uuid),
                JsonIO.toJson(snapshot)
        );
    }

    @Override
    public List<Snapshot> readPlayerHistory(
            UUID uuid,
            long from,
            long to,
            int limit
    ) {

        List<Snapshot> result =
                new ArrayList<>();

        File file =
                playerHistoryFile(uuid);

        if (!file.exists()) {
            return result;
        }

        try (BufferedReader reader = openReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty()) {
                    continue;
                }

                try {

                    Snapshot snapshot =
                            JsonIO.fromJson(line, Snapshot.class);

                    if (snapshot.ts < from || snapshot.ts > to) {
                        continue;
                    }

                    result.add(snapshot);

                } catch (Exception ignored) {
                }
            }

        } catch (Exception exception) {

            logger.log(
                    Level.FINE,
                    "Failed to read history for " + uuid,
                    exception
            );
        }

        return trimToLimit(result, limit);
    }

    // -- server history ------------------------------------------------------

    @Override
    public void appendServerSnapshot(ServerSnapshot snapshot) {

        appendLine(
                new File(historyDir, "server.jsonl"),
                JsonIO.toJson(snapshot)
        );
    }

    @Override
    public List<ServerSnapshot> readServerHistory(
            long from,
            long to,
            int limit
    ) {

        List<ServerSnapshot> result =
                new ArrayList<>();

        File file =
                new File(historyDir, "server.jsonl");

        if (!file.exists()) {
            return result;
        }

        try (BufferedReader reader = openReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty()) {
                    continue;
                }

                try {

                    ServerSnapshot snapshot =
                            JsonIO.fromJson(line, ServerSnapshot.class);

                    if (snapshot.ts < from || snapshot.ts > to) {
                        continue;
                    }

                    result.add(snapshot);

                } catch (Exception ignored) {
                }
            }

        } catch (Exception exception) {

            logger.log(
                    Level.FINE,
                    "Failed to read server history",
                    exception
            );
        }

        return trimToLimit(result, limit);
    }

    // -- activity ------------------------------------------------------------

    @Override
    public void appendActivity(UUID uuid, ActivityEvent event) {

        appendLine(
                activityFile(uuid),
                JsonIO.toJson(event)
        );
    }

    @Override
    public List<ActivityEvent> readActivity(
            UUID uuid,
            long from,
            long to,
            int limit
    ) {

        List<ActivityEvent> result =
                new ArrayList<>();

        File file =
                activityFile(uuid);

        if (!file.exists()) {
            return result;
        }

        try (BufferedReader reader = openReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty()) {
                    continue;
                }

                try {

                    ActivityEvent event =
                            JsonIO.fromJson(line, ActivityEvent.class);

                    if (event.ts < from || event.ts > to) {
                        continue;
                    }

                    result.add(event);

                } catch (Exception ignored) {
                }
            }

        } catch (Exception exception) {

            logger.log(
                    Level.FINE,
                    "Failed to read activity for " + uuid,
                    exception
            );
        }

        return trimToLimit(result, limit);
    }

    // -- archived leaderboards ----------------------------------------------

    @Override
    public void appendPeriodArchive(PeriodArchive archive) {

        appendLine(
                new File(historyDir, "archives.jsonl"),
                JsonIO.toJson(archive)
        );
    }

    @Override
    public List<PeriodArchive> readPeriodArchives(
            String period,
            long from,
            long to,
            int limit
    ) {

        List<PeriodArchive> result =
                new ArrayList<>();

        File file =
                new File(historyDir, "archives.jsonl");

        if (!file.exists()) {
            return result;
        }

        try (BufferedReader reader = openReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty()) {
                    continue;
                }

                try {

                    PeriodArchive archive =
                            JsonIO.fromJson(line, PeriodArchive.class);

                    if (period != null
                            && !period.equalsIgnoreCase(archive.period)) {
                        continue;
                    }

                    if (archive.generatedAt < from
                            || archive.generatedAt > to) {
                        continue;
                    }

                    result.add(archive);

                } catch (Exception ignored) {
                }
            }

        } catch (Exception exception) {

            logger.log(
                    Level.FINE,
                    "Failed to read period archives",
                    exception
            );
        }

        return trimToLimit(result, limit);
    }

    // -- server state --------------------------------------------------------

    @Override
    public ServerState loadServerState() {

        File file =
                new File(baseDir, "server.json");

        if (!file.exists()) {
            return new ServerState();
        }

        try {

            String json =
                    readFile(file);

            if (json.trim().isEmpty()) {
                return new ServerState();
            }

            ServerState state =
                    JsonIO.fromJson(json, ServerState.class);

            return state == null ? new ServerState() : state;

        } catch (Exception exception) {

            logger.log(
                    Level.WARNING,
                    "Failed to read server state",
                    exception
            );

            return new ServerState();
        }
    }

    @Override
    public void saveServerState(ServerState state) {

        if (state == null) {
            return;
        }

        File target =
                new File(baseDir, "server.json");

        File temp =
                new File(baseDir, "server.json.tmp");

        try {

            writeFile(temp, JsonIO.toJson(state));

            Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception exception) {

            logger.log(
                    Level.WARNING,
                    "Failed to save server state",
                    exception
            );
        }
    }

    // -- maintenance ---------------------------------------------------------

    @Override
    public void prune(long retentionDays) {

        if (retentionDays <= 0) {
            return;
        }

        long cutoff =
                System.currentTimeMillis()
                        - retentionDays * 24L * 60L * 60L * 1000L;

        pruneJsonl(
                new File(historyDir, "server.jsonl"),
                cutoff,
                ServerSnapshot.class
        );

        pruneJsonl(
                new File(historyDir, "archives.jsonl"),
                cutoff,
                PeriodArchive.class
        );

        File[] playerFiles =
                playerHistoryDir.listFiles(
                        (dir, name) -> name.endsWith(".jsonl")
                );

        if (playerFiles != null) {

            for (File file : playerFiles) {
                pruneJsonl(file, cutoff, Snapshot.class);
            }
        }

        File[] activityFiles =
                activityDir.listFiles(
                        (dir, name) -> name.endsWith(".jsonl")
                );

        if (activityFiles != null) {

            for (File file : activityFiles) {
                pruneJsonl(file, cutoff, ActivityEvent.class);
            }
        }
    }

    @Override
    public void purgeHistory() {

        delete(new File(historyDir, "server.jsonl"));
        delete(new File(historyDir, "archives.jsonl"));

        deleteJsonlIn(playerHistoryDir);
        deleteJsonlIn(activityDir);
    }

    @Override
    public void close() {
        // Nothing to flush: every write is flushed on completion.
    }

    // -- internals -----------------------------------------------------------

    private File profileFile(UUID uuid) {

        return new File(playersDir, uuid + ".json");
    }

    private File playerHistoryFile(UUID uuid) {

        return new File(playerHistoryDir, uuid + ".jsonl");
    }

    private File activityFile(UUID uuid) {

        return new File(activityDir, uuid + ".jsonl");
    }

    private void ensureDir(File dir) {

        if (!dir.exists() && !dir.mkdirs()) {
            logger.warning(
                    "Could not create data directory: " + dir
            );
        }
    }

    private void appendLine(File file, String line) {

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(file, true),
                                     StandardCharsets.UTF_8
                             )
                     )) {

            writer.write(line);
            writer.newLine();

        } catch (Exception exception) {

            logger.log(
                    Level.WARNING,
                    "Failed to append to " + file.getName(),
                    exception
            );
        }
    }

    private static BufferedReader openReader(File file)
            throws IOException {

        return new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(file),
                        StandardCharsets.UTF_8
                )
        );
    }

    private static String readFile(File file)
            throws IOException {

        StringBuilder builder =
                new StringBuilder();

        try (BufferedReader reader = openReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }

        return builder.toString();
    }

    private static void writeFile(File file, String content)
            throws IOException {

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(file),
                                     StandardCharsets.UTF_8
                             )
                     )) {

            writer.write(content);
        }
    }

    private static void delete(File file) {

        if (file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    private void deleteJsonlIn(File dir) {

        File[] files =
                dir.listFiles(
                        (d, name) -> name.endsWith(".jsonl")
                );

        if (files != null) {

            for (File file : files) {
                delete(file);
            }
        }
    }

    private <T> void pruneJsonl(
            File file,
            long cutoff,
            Class<T> type
    ) {

        if (!file.exists()) {
            return;
        }

        List<String> kept =
                new ArrayList<>();

        try (BufferedReader reader = openReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {

                    T value =
                            JsonIO.fromJson(line, type);

                    long ts =
                            timestampOf(value);

                    if (ts <= 0 || ts >= cutoff) {
                        kept.add(line);
                    }

                } catch (Exception ignored) {
                    // Drop unreadable lines.
                }
            }

        } catch (Exception exception) {

            logger.log(
                    Level.FINE,
                    "Failed to prune " + file.getName(),
                    exception
            );

            return;
        }

        try {

            File temp =
                    new File(file.getParentFile(), file.getName() + ".tmp");

            StringBuilder builder =
                    new StringBuilder();

            for (String line : kept) {
                builder.append(line).append('\n');
            }

            writeFile(temp, builder.toString());

            Files.move(
                    temp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception exception) {

            logger.log(
                    Level.WARNING,
                    "Failed to rewrite " + file.getName(),
                    exception
            );
        }
    }

    private static long timestampOf(Object value) {

        if (value instanceof Snapshot) {
            return ((Snapshot) value).ts;
        }

        if (value instanceof ServerSnapshot) {
            return ((ServerSnapshot) value).ts;
        }

        if (value instanceof ActivityEvent) {
            return ((ActivityEvent) value).ts;
        }

        if (value instanceof PeriodArchive) {
            return ((PeriodArchive) value).generatedAt;
        }

        return 0L;
    }

    private static <T> List<T> trimToLimit(
            List<T> list,
            int limit
    ) {

        if (limit <= 0 || list.size() <= limit) {
            return list;
        }

        return new ArrayList<>(
                list.subList(
                        list.size() - limit,
                        list.size()
                )
        );
    }
}
