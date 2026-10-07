package in.potenfyr.statfyr.stats;

import in.potenfyr.statfyr.compat.StatisticCompat;
import in.potenfyr.statfyr.model.PlayerStats;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a player's live statistics from the running Minecraft server.
 *
 * <p>This is the real-time source of truth for online players, replacing the
 * previous reliance on {@code world/stats/<uuid>.json} files which Minecraft
 * does not flush immediately.
 *
 * <p>Fully version agnostic: the statistic category is derived from the stable
 * enum name and material/entity filtering is resolved through
 * {@link StatisticCompat}, keeping the same binary working on Minecraft
 * 1.8.x through 26.x across Bukkit, Spigot, Paper, Purpur and Folia.
 *
 * <p><strong>Threading:</strong> vanilla statistic maps are plain hash maps
 * mutated by the server thread, so {@link #read(Player)} must be invoked on
 * the main thread (or the global/region scheduler on Folia). The result is an
 * immutable {@link PlayerStats} snapshot that is safe to hand to any thread.
 */
public final class LiveStatisticsReader {

    private LiveStatisticsReader() {
    }

    /**
     * Reads every non-zero statistic for the given online player into an
     * immutable snapshot. A single failing statistic never aborts the read.
     *
     * @param player online player (must be on the main thread)
     * @return immutable statistics snapshot
     */
    public static PlayerStats read(Player player) {

        Map<String, Map<String, Long>> stats =
                new HashMap<>();

        Map<String, Long> custom =
                new HashMap<>();

        Map<String, Long> mined =
                new HashMap<>();

        Map<String, Long> crafted =
                new HashMap<>();

        Map<String, Long> used =
                new HashMap<>();

        Map<String, Long> broken =
                new HashMap<>();

        Map<String, Long> pickedUp =
                new HashMap<>();

        Map<String, Long> dropped =
                new HashMap<>();

        Map<String, Long> killed =
                new HashMap<>();

        Map<String, Long> killedBy =
                new HashMap<>();

        Material[] itemMaterials =
                StatisticCompat.getItemMaterials();

        Material[] blockMaterials =
                StatisticCompat.getBlockMaterials();

        EntityType[] entityTypes =
                StatisticCompat.getLivingEntityTypes();

        for (Statistic statistic
                : Statistic.values()) {

            try {

                String category =
                        StatisticCompat.categoryOf(statistic);

                if (StatisticCompat.UNTYPED.equals(category)) {

                    collectUntyped(
                            player,
                            statistic,
                            custom
                    );

                } else if (StatisticCompat.BLOCK.equals(category)) {

                    collectMaterials(
                            player,
                            statistic,
                            blockMaterials,
                            mined
                    );

                } else if (StatisticCompat.ITEM.equals(category)) {

                    collectMaterials(
                            player,
                            statistic,
                            itemMaterials,
                            itemTarget(statistic, crafted, used, broken, pickedUp, dropped)
                    );

                } else if (StatisticCompat.ENTITY.equals(category)) {

                    collectEntities(
                            player,
                            statistic,
                            entityTypes,
                            entityTarget(statistic, killed, killedBy)
                    );
                }

            } catch (Throwable ignored) {
                // One bad statistic must never break the whole snapshot.
            }
        }

        stats.put(StatKeys.CATEGORY_CUSTOM, custom);
        stats.put(StatKeys.CATEGORY_MINED, mined);
        stats.put(StatKeys.CATEGORY_CRAFTED, crafted);
        stats.put(StatKeys.CATEGORY_USED, used);
        stats.put(StatKeys.CATEGORY_BROKEN, broken);
        stats.put(StatKeys.CATEGORY_PICKED_UP, pickedUp);
        stats.put(StatKeys.CATEGORY_DROPPED, dropped);
        stats.put(StatKeys.CATEGORY_KILLED, killed);
        stats.put(StatKeys.CATEGORY_KILLED_BY, killedBy);

        return new PlayerStats(
                player.getUniqueId(),
                player.getName(),
                stats
        );
    }

    // -------------------------------------------------------------------------
    // Collection helpers
    // -------------------------------------------------------------------------

    private static void collectUntyped(
            Player player,
            Statistic statistic,
            Map<String, Long> target
    ) {

        int value =
                player.getStatistic(statistic);

        if (value <= 0) {
            return;
        }

        String key =
                StatisticCompat.isPlayTime(statistic)
                        ? StatKeys.PLAY_TIME
                        : "minecraft:" + statistic.name()
                        .toLowerCase(Locale.ROOT);

        target.put(
                key,
                (long) value
        );
    }

    private static void collectMaterials(
            Player player,
            Statistic statistic,
            Material[] materials,
            Map<String, Long> target
    ) {

        if (target == null) {
            return;
        }

        for (Material material : materials) {

            try {

                int value =
                        player.getStatistic(
                                statistic,
                                material
                        );

                if (value <= 0) {
                    continue;
                }

                target.put(
                        "minecraft:"
                                + material.name()
                                .toLowerCase(Locale.ROOT),
                        (long) value
                );

            } catch (Throwable ignored) {
                // Material is not valid for this statistic on this version.
            }
        }
    }

    private static void collectEntities(
            Player player,
            Statistic statistic,
            EntityType[] entityTypes,
            Map<String, Long> target
    ) {

        if (target == null) {
            return;
        }

        for (EntityType entityType : entityTypes) {

            try {

                int value =
                        player.getStatistic(
                                statistic,
                                entityType
                        );

                if (value <= 0) {
                    continue;
                }

                target.put(
                        "minecraft:"
                                + entityType.name()
                                .toLowerCase(Locale.ROOT),
                        (long) value
                );

            } catch (Throwable ignored) {
                // Entity type is not valid for this statistic on this version.
            }
        }
    }

    private static Map<String, Long> itemTarget(
            Statistic statistic,
            Map<String, Long> crafted,
            Map<String, Long> used,
            Map<String, Long> broken,
            Map<String, Long> pickedUp,
            Map<String, Long> dropped
    ) {

        switch (statistic.name()) {

            case "CRAFT_ITEM":
                return crafted;

            case "USE_ITEM":
                return used;

            case "BREAK_ITEM":
                return broken;

            case "PICKUP":
                return pickedUp;

            case "DROP":
                return dropped;

            default:
                return null;
        }
    }

    private static Map<String, Long> entityTarget(
            Statistic statistic,
            Map<String, Long> killed,
            Map<String, Long> killedBy
    ) {

        switch (statistic.name()) {

            case "KILL_ENTITY":
                return killed;

            case "ENTITY_KILLED_BY":
                return killedBy;

            default:
                return null;
        }
    }
}
