package in.potenfyr.statfyr.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Reads vanilla Minecraft stats JSON files.
 */
public final class StatsReader {

    private final Statfyr plugin;

    /**
     * Parsed file cache.
     */
    private final ConcurrentHashMap<UUID, CachedFile>
            fileCache =
            new ConcurrentHashMap<>();

    public StatsReader(
            Statfyr plugin
    ) {

        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public PlayerStats readStats(
            UUID playerUuid,
            String playerName
    ) {

        try {

            File statsFile =
                    resolveStatsFile(playerUuid);

            if (statsFile == null
                    || !statsFile.exists()) {

                return emptyStats(
                        playerUuid,
                        playerName
                );
            }

            CachedFile cached =
                    fileCache.get(playerUuid);

            long modified =
                    statsFile.lastModified();

            // Cached valid file
            if (cached != null
                    && cached.lastModified == modified) {

                return cached.stats;
            }

            PlayerStats parsed =
                    parseStatsFile(
                            statsFile,
                            playerUuid,
                            playerName
                    );

            fileCache.put(
                    playerUuid,
                    new CachedFile(
                            modified,
                            parsed
                    )
            );

            return parsed;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to read stats for "
                            + playerUuid,
                    exception
            );

            return emptyStats(
                    playerUuid,
                    playerName
            );
        }
    }

    // -------------------------------------------------------------------------
    // Parse
    // -------------------------------------------------------------------------

    private PlayerStats parseStatsFile(
            File statsFile,
            UUID playerUuid,
            String playerName
    ) {

        Map<String, Map<String, Long>> rawStats =
                new HashMap<>();

        try (
                FileReader reader =
                        new FileReader(statsFile)
        ) {

            JsonObject root =
                    new JsonParser()
                            .parse(reader)
                            .getAsJsonObject();

            JsonObject statsNode =
                    root.has("stats")
                            ? root.getAsJsonObject("stats")
                            : null;

            if (statsNode != null) {

                for (Map.Entry<String, JsonElement>
                        categoryEntry
                        : statsNode.entrySet()) {

                    String category =
                            categoryEntry.getKey();

                    JsonObject categoryObject =
                            categoryEntry.getValue()
                                    .getAsJsonObject();

                    Map<String, Long> statMap =
                            new HashMap<>();

                    for (Map.Entry<String, JsonElement>
                            statEntry
                            : categoryObject.entrySet()) {

                        try {

                            String normalizedKey =
                                    statEntry.getKey();

                            if (normalizedKey.equals(
                                    "minecraft:play_one_minute"
                            )) {

                                normalizedKey =
                                        StatKeys.PLAY_TIME;
                            }

                            statMap.put(
                                    normalizedKey,
                                    statEntry.getValue()
                                            .getAsLong()
                            );

                        } catch (Exception ignored) {
                        }
                    }

                    rawStats.put(
                            category,
                            statMap
                    );
                }
            }

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "Corrupted stats file: "
                            + statsFile.getName()
            );

            exception.printStackTrace();
        }

        // Better offline name fallback
        if (playerName == null
                || playerName.isBlank()) {

            try {

                OfflinePlayer offlinePlayer =
                        Bukkit.getOfflinePlayer(
                                playerUuid
                        );

                playerName =
                        offlinePlayer.getName();

            } catch (Exception ignored) {
            }
        }

        return new PlayerStats(
                playerUuid,
                playerName,
                rawStats
        );
    }

    // -------------------------------------------------------------------------
    // Known UUIDs
    // -------------------------------------------------------------------------

    public List<UUID> getAllKnownUuids() {

        File statsDirectory =
                getStatsDirectory();

        List<UUID> uuids =
                new ArrayList<>();

        if (statsDirectory == null
                || !statsDirectory.isDirectory()) {

            return uuids;
        }

        File[] files =
                statsDirectory.listFiles(
                        (dir, name) ->
                                name.endsWith(".json")
                                        && name.length() == 41
                );

        if (files == null) {
            return uuids;
        }

        for (File file : files) {

            try {

                uuids.add(
                        UUID.fromString(
                                file.getName()
                                        .replace(".json", "")
                        )
                );

            } catch (Exception ignored) {
            }
        }

        return uuids;
    }

    // -------------------------------------------------------------------------
    // Player Names
    // -------------------------------------------------------------------------

    public String resolvePlayerName(
            UUID playerUuid
    ) {

        try {

            OfflinePlayer player =
                    Bukkit.getOfflinePlayer(
                            playerUuid
                    );

            return player.getName();

        } catch (Exception ignored) {

            return "unknown";
        }
    }

    // -------------------------------------------------------------------------
    // File Resolution
    // -------------------------------------------------------------------------

    private File resolveStatsFile(
            UUID playerUuid
    ) {

        String fileName =
                playerUuid + ".json";

        for (org.bukkit.World world
                : Bukkit.getWorlds()) {

            File candidate =
                    new File(
                            world.getWorldFolder(),
                            "stats"
                                    + File.separator
                                    + fileName
                    );

            if (candidate.exists()) {
                return candidate;
            }
        }

        return null;
    }

    private File getStatsDirectory() {

        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }

        return new File(
                Bukkit.getWorlds()
                        .get(0)
                        .getWorldFolder(),
                "stats"
        );
    }

    // -------------------------------------------------------------------------
    // Empty
    // -------------------------------------------------------------------------

    private PlayerStats emptyStats(
            UUID uuid,
            String name
    ) {

        return new PlayerStats(
                uuid,
                name,
                new HashMap<>()
        );
    }

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    public void clearCache() {
        fileCache.clear();
    }

    public int getCacheSize() {
        return fileCache.size();
    }

    // -------------------------------------------------------------------------
    // Cached File
    // -------------------------------------------------------------------------

    private static final class CachedFile {

        private final long lastModified;

        private final PlayerStats stats;

        CachedFile(
                long lastModified,
                PlayerStats stats
        ) {

            this.lastModified =
                    lastModified;

            this.stats = stats;
        }
    }
}