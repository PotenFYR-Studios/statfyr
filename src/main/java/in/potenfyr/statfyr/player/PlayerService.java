package in.potenfyr.statfyr.player;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * Player resolution service.
 *
 * Responsibilities:
 * - UUID resolution
 * - name resolution
 * - online state checks
 * - player enumeration
 */
public final class PlayerService {

    private final Statfyr plugin;

    /**
     * Simple name cache.
     */
    private final ConcurrentHashMap<UUID, String>
            cachedNames =
            new ConcurrentHashMap<>();

    public PlayerService(Statfyr plugin) {

        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // UUID Resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves:
     * - UUID string
     * - online player name
     * - offline player name
     */
    public UUID resolveUuid(
            String identifier
    ) {

        if (identifier == null
                || identifier.isBlank()) {

            return null;
        }

        // UUID
        try {

            return UUID.fromString(identifier);

        } catch (IllegalArgumentException ignored) {
        }

        // Online lookup
        Player onlinePlayer =
                Bukkit.getPlayerExact(identifier);

        if (onlinePlayer != null) {

            cachePlayer(onlinePlayer);

            return onlinePlayer.getUniqueId();
        }

        // Offline lookup
        try {

            Future<UUID> future =
                    Bukkit.getScheduler()
                            .callSyncMethod(
                                    plugin,
                                    () -> {

                                        OfflinePlayer offlinePlayer =
                                                Bukkit.getOfflinePlayer(
                                                        identifier
                                                );

                                        if (!offlinePlayer.hasPlayedBefore()) {
                                            return null;
                                        }

                                        cachePlayer(offlinePlayer);

                                        return offlinePlayer.getUniqueId();
                                    }
                            );

            return future.get();

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to resolve player: "
                            + identifier,
                    exception
            );

            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    public PlayerStats getStats(
            UUID playerUuid
    ) {

        return plugin.getStatsManager()
                .getPlayerStats(playerUuid);
    }

    // -------------------------------------------------------------------------
    // Online State
    // -------------------------------------------------------------------------

    public boolean isOnline(
            UUID uuid
    ) {

        return Bukkit.getPlayer(uuid) != null;
    }

    // -------------------------------------------------------------------------
    // Name Resolution
    // -------------------------------------------------------------------------

    public String resolvePlayerName(
            UUID uuid
    ) {

        if (uuid == null) {
            return "unknown";
        }

        String cached =
                cachedNames.get(uuid);

        if (cached != null
                && !cached.isBlank()) {

            return cached;
        }

        Player onlinePlayer =
                Bukkit.getPlayer(uuid);

        if (onlinePlayer != null) {

            cachePlayer(onlinePlayer);

            return onlinePlayer.getName();
        }

        try {

            OfflinePlayer offlinePlayer =
                    Bukkit.getOfflinePlayer(uuid);

            String name =
                    offlinePlayer.getName();

            if (name != null
                    && !name.isBlank()) {

                cachedNames.put(uuid, name);

                return name;
            }

        } catch (Exception ignored) {
        }

        return "unknown";
    }

    // -------------------------------------------------------------------------
    // Player Lists
    // -------------------------------------------------------------------------

    public List<ResolvedPlayer> getAllPlayers() {

        List<UUID> allUuids =
                plugin.getStatsReader()
                        .getAllKnownUuids();

        List<ResolvedPlayer> players =
                new ArrayList<>(
                        allUuids.size()
                );

        for (UUID uuid : allUuids) {

            String name =
                    resolvePlayerName(uuid);

            boolean online =
                    isOnline(uuid);

            players.add(
                    new ResolvedPlayer(
                            uuid,
                            name,
                            online
                    )
            );
        }

        return players;
    }

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    private void cachePlayer(
            OfflinePlayer player
    ) {

        if (player == null
                || player.getUniqueId() == null
                || player.getName() == null) {

            return;
        }

        cachedNames.put(
                player.getUniqueId(),
                player.getName()
        );
    }

    // -------------------------------------------------------------------------
    // Inner Data Class
    // -------------------------------------------------------------------------

    public static final class ResolvedPlayer {

        private final UUID uuid;

        private final String name;

        private final boolean online;

        public ResolvedPlayer(
                UUID uuid,
                String name,
                boolean online
        ) {

            this.uuid = uuid;
            this.name = name;
            this.online = online;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public boolean isOnline() {
            return online;
        }
    }
}