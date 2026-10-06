package in.potenfyr.statfyr.analytics;

import in.potenfyr.statfyr.model.PlayerStats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical metric extraction.
 *
 * <p>StatfyR exposes a stable set of numeric metrics regardless of the server
 * version. Everything is derived from {@link PlayerStats} so the values stay
 * consistent between the REST API, PlaceholderAPI, commands and Prometheus.
 */
public final class Metrics {

    private Metrics() {
    }

    // Stable metric keys used across the whole platform.
    public static final String PLAYTIME = "playtime";
    public static final String ACTIVE_TIME = "active_time";
    public static final String AFK_TIME = "afk_time";
    public static final String SESSIONS = "sessions";
    public static final String KILLS = "kills";
    public static final String DEATHS = "deaths";
    public static final String PLAYER_KILLS = "player_kills";
    public static final String MOB_KILLS = "mob_kills";
    public static final String KDR = "kdr";
    public static final String DAMAGE_DEALT = "damage_dealt";
    public static final String DAMAGE_TAKEN = "damage_taken";
    public static final String BLOCKS_MINED = "blocks_mined";
    public static final String BLOCKS_BROKEN = "blocks_broken";
    public static final String ITEMS_CRAFTED = "items_crafted";
    public static final String ITEMS_USED = "items_used";
    public static final String ITEMS_PICKED_UP = "items_picked_up";
    public static final String ITEMS_DROPPED = "items_dropped";
    public static final String CHESTS_OPENED = "chests_opened";
    public static final String JUMPS = "jumps";
    public static final String DISTANCE_TRAVELED = "distance_traveled";
    public static final String DISTANCE_WALKED = "distance_walked";
    public static final String DISTANCE_SPRINTED = "distance_sprinted";
    public static final String DISTANCE_SWUM = "distance_swum";
    public static final String DISTANCE_FLOWN = "distance_flown";
    public static final String BALANCE = "balance";

    /**
     * Human readable aliases accepted by commands/API.
     *
     * @param alias user supplied stat name
     * @return canonical metric key, or the lower-cased alias when unknown
     */
    public static String canonical(String alias) {

        if (alias == null) {
            return "";
        }

        String key =
                alias.trim()
                        .toLowerCase()
                        .replace('-', '_');

        switch (key) {

            case "kill":
            case "kills":
                return KILLS;

            case "death":
            case "deaths":
                return DEATHS;

            case "playtime":
            case "play_time":
            case "time":
                return PLAYTIME;

            case "active":
            case "active_time":
            case "activetime":
                return ACTIVE_TIME;

            case "afk":
            case "afk_time":
                return AFK_TIME;

            case "session":
            case "sessions":
                return SESSIONS;

            case "kdr":
            case "kd":
            case "ratio":
                return KDR;

            case "mined":
            case "blocks":
            case "blocks_mined":
                return BLOCKS_MINED;

            case "broken":
            case "blocks_broken":
                return BLOCKS_BROKEN;

            case "crafted":
            case "items_crafted":
                return ITEMS_CRAFTED;

            case "used":
            case "items_used":
                return ITEMS_USED;

            case "pickedup":
            case "picked_up":
            case "items_picked_up":
                return ITEMS_PICKED_UP;

            case "dropped":
            case "items_dropped":
                return ITEMS_DROPPED;

            case "chests":
            case "chests_opened":
                return CHESTS_OPENED;

            case "jumps":
            case "jump":
                return JUMPS;

            case "distance":
            case "distance_traveled":
                return DISTANCE_TRAVELED;

            case "walked":
            case "distance_walked":
                return DISTANCE_WALKED;

            case "sprinted":
            case "distance_sprinted":
                return DISTANCE_SPRINTED;

            case "swum":
            case "distance_swum":
                return DISTANCE_SWUM;

            case "flown":
            case "distance_flown":
                return DISTANCE_FLOWN;

            case "balance":
            case "money":
                return BALANCE;

            case "player_kills":
            case "playerkills":
                return PLAYER_KILLS;

            case "mob_kills":
            case "mobkills":
                return MOB_KILLS;

            case "damage_dealt":
                return DAMAGE_DEALT;

            case "damage_taken":
                return DAMAGE_TAKEN;

            default:
                return key;
        }
    }

    /**
     * @return every metric key that can be ranked on a leaderboard
     */
    public static String[] leaderboardKeys() {

        return new String[]{
                KILLS,
                DEATHS,
                KDR,
                PLAYER_KILLS,
                MOB_KILLS,
                DAMAGE_DEALT,
                DAMAGE_TAKEN,
                PLAYTIME,
                ACTIVE_TIME,
                SESSIONS,
                BLOCKS_MINED,
                BLOCKS_BROKEN,
                ITEMS_CRAFTED,
                ITEMS_USED,
                ITEMS_PICKED_UP,
                ITEMS_DROPPED,
                CHESTS_OPENED,
                DISTANCE_TRAVELED,
                DISTANCE_WALKED,
                DISTANCE_SPRINTED,
                DISTANCE_SWUM,
                DISTANCE_FLOWN,
                JUMPS,
                BALANCE
        };
    }

    /**
     * Extracts all canonical metrics from a stats snapshot.
     *
     * @param stats player stats
     * @return ordered metric map
     */
    public static Map<String, Long> extract(PlayerStats stats) {

        Map<String, Long> metrics =
                new LinkedHashMap<>();

        if (stats == null) {
            return metrics;
        }

        long playerKills =
                stats.getPlayerKills();

        long mobKills =
                stats.getMobKills();

        metrics.put(KILLS, playerKills + mobKills);
        metrics.put(DEATHS, stats.getDeaths());
        metrics.put(PLAYER_KILLS, playerKills);
        metrics.put(MOB_KILLS, mobKills);
        metrics.put(DAMAGE_DEALT, stats.getDamageDealt());
        metrics.put(DAMAGE_TAKEN, stats.getDamageTaken());
        metrics.put(PLAYTIME, stats.getPlayTimeSeconds());
        metrics.put(BLOCKS_MINED, stats.getBlocksMined());
        metrics.put(BLOCKS_BROKEN, stats.getItemsBroken());
        metrics.put(ITEMS_CRAFTED, stats.getItemsCrafted());
        metrics.put(ITEMS_USED, stats.getItemsUsed());
        metrics.put(ITEMS_PICKED_UP, stats.getItemsPickedUp());
        metrics.put(ITEMS_DROPPED, stats.getItemsDropped());
        metrics.put(CHESTS_OPENED, stats.getChestsOpened());
        metrics.put(JUMPS, stats.getJumps());
        metrics.put(DISTANCE_TRAVELED, stats.getTotalDistanceCm());
        metrics.put(DISTANCE_WALKED, stats.getWalkDistanceCm());
        metrics.put(DISTANCE_SPRINTED, stats.getSprintDistanceCm());
        metrics.put(DISTANCE_SWUM, stats.getSwimDistanceCm());
        metrics.put(DISTANCE_FLOWN, stats.getFlyDistanceCm());

        return metrics;
    }
}
