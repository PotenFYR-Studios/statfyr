package in.potenfyr.statfyr.model;

import in.potenfyr.statfyr.stats.StatKeys;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable player stats snapshot.
 */
public final class PlayerStats {

    private final UUID playerUuid;

    private final String playerName;

    private final Map<String, Map<String, Long>>
            rawStats;

    private final long readTimestamp;

    public PlayerStats(
            UUID playerUuid,
            String playerName,
            Map<String, Map<String, Long>> rawStats
    ) {

        this.playerUuid = playerUuid;
        this.playerName = playerName;

        Map<String, Map<String, Long>> defensiveCopy =
                new HashMap<>();

        for (Map.Entry<String, Map<String, Long>> entry
                : rawStats.entrySet()) {

            defensiveCopy.put(
                    entry.getKey(),
                    Collections.unmodifiableMap(
                            new HashMap<>(
                                    entry.getValue()
                            )
                    )
            );
        }

        this.rawStats =
                Collections.unmodifiableMap(
                        defensiveCopy
                );

        this.readTimestamp =
                System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // Base Access
    // -------------------------------------------------------------------------

    public long getStat(
            String category,
            String key
    ) {

        Map<String, Long> categoryMap =
                rawStats.get(category);

        if (categoryMap == null) {
            return 0L;
        }

        return categoryMap.getOrDefault(
                key,
                0L
        );
    }

    public Map<String, Long> getCategory(
            String category
    ) {

        return rawStats.getOrDefault(
                category,
                Collections.emptyMap()
        );
    }

    public Map<String, Map<String, Long>>
    getRawStats() {

        return rawStats;
    }

    // -------------------------------------------------------------------------
    // Playtime
    // -------------------------------------------------------------------------

    public long getPlayTimeTicks() {

        long playTime =
                getStat(
                        StatKeys.CATEGORY_CUSTOM,
                        StatKeys.PLAY_TIME
                );

        if (playTime <= 0) {

            playTime =
                    getStat(
                            StatKeys.CATEGORY_CUSTOM,
                            "minecraft:play_one_minute"
                    );
        }

        return playTime;
    }

    public long getPlayTimeSeconds() {
        return getPlayTimeTicks() / 20L;
    }

    public long getPlayTimeMinutes() {
        return getPlayTimeSeconds() / 60L;
    }

    public double getPlayTimeHours() {
        return getPlayTimeSeconds() / 3600.0;
    }

    public String getFormattedPlayTime() {

        long totalSeconds =
                getPlayTimeSeconds();

        long days =
                totalSeconds / 86400;

        long hours =
                (totalSeconds % 86400) / 3600;

        long minutes =
                (totalSeconds % 3600) / 60;

        long seconds =
                totalSeconds % 60;

        StringBuilder builder =
                new StringBuilder();

        if (days > 0) {
            builder.append(days).append("d ");
        }

        if (hours > 0 || days > 0) {
            builder.append(hours).append("h ");
        }

        if (minutes > 0 || hours > 0 || days > 0) {
            builder.append(minutes).append("m ");
        }

        builder.append(seconds).append("s");

        return builder.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Combat
    // -------------------------------------------------------------------------

    public long getDeaths() {
        return custom(StatKeys.DEATHS);
    }

    public long getPlayerKills() {
        return custom(StatKeys.PLAYER_KILLS);
    }

    public long getMobKills() {
        return custom(StatKeys.MOB_KILLS);
    }

    public long getDamageDealt() {
        return custom(StatKeys.DAMAGE_DEALT);
    }

    public long getDamageTaken() {
        return custom(StatKeys.DAMAGE_TAKEN);
    }

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    public long getJumps() {
        return custom(StatKeys.JUMPS);
    }

    public long getWalkDistanceCm() {
        return custom(StatKeys.WALK_ONE_CM);
    }

    public long getSprintDistanceCm() {
        return custom(StatKeys.SPRINT_ONE_CM);
    }

    public long getFlyDistanceCm() {
        return custom(StatKeys.FLY_ONE_CM);
    }

    public long getSwimDistanceCm() {
        return custom(StatKeys.SWIM_ONE_CM);
    }

    public long getTotalDistanceCm() {

        return getWalkDistanceCm()
                + getSprintDistanceCm()
                + getFlyDistanceCm()
                + getSwimDistanceCm()
                + custom(StatKeys.WALK_UNDER_WATER_ONE_CM)
                + custom(StatKeys.CLIMB_ONE_CM)
                + custom(StatKeys.HORSE_ONE_CM)
                + custom(StatKeys.BOAT_ONE_CM)
                + custom(StatKeys.MINECART_ONE_CM)
                + custom(StatKeys.PIG_ONE_CM)
                + custom(StatKeys.STRIDER_ONE_CM);
    }

    public double getTotalDistanceKm() {
        return getTotalDistanceCm() / 100000.0;
    }

    // -------------------------------------------------------------------------
    // Activity
    // -------------------------------------------------------------------------

    public long getChestsOpened() {

        return custom(StatKeys.OPEN_CHEST)
                + custom(StatKeys.OPEN_SHULKER_BOX);
    }

    // -------------------------------------------------------------------------
    // Aggregates
    // -------------------------------------------------------------------------

    public long getItemsCrafted() {
        return sumCategory(StatKeys.CATEGORY_CRAFTED);
    }

    public long getItemsBroken() {
        return sumCategory(StatKeys.CATEGORY_BROKEN);
    }

    public long getItemsUsed() {
        return sumCategory(StatKeys.CATEGORY_USED);
    }

    public long getItemsPickedUp() {
        return sumCategory(StatKeys.CATEGORY_PICKED_UP);
    }

    public long getItemsDropped() {
        return sumCategory(StatKeys.CATEGORY_DROPPED);
    }

    public long getBlocksMined() {
        return sumCategory(StatKeys.CATEGORY_MINED);
    }

    // -------------------------------------------------------------------------
    // Category Maps
    // -------------------------------------------------------------------------

    public Map<String, Long> getCraftedItems() {
        return getCategory(StatKeys.CATEGORY_CRAFTED);
    }

    public Map<String, Long> getMinedBlocks() {
        return getCategory(StatKeys.CATEGORY_MINED);
    }

    public Map<String, Long> getUsedItems() {
        return getCategory(StatKeys.CATEGORY_USED);
    }

    public Map<String, Long> getBrokenItems() {
        return getCategory(StatKeys.CATEGORY_BROKEN);
    }

    public Map<String, Long> getPickedUpItems() {
        return getCategory(StatKeys.CATEGORY_PICKED_UP);
    }

    public Map<String, Long> getDroppedItems() {
        return getCategory(StatKeys.CATEGORY_DROPPED);
    }

    public Map<String, Long> getKilledEntities() {
        return getCategory(StatKeys.CATEGORY_KILLED);
    }

    public Map<String, Long> getKilledByEntities() {
        return getCategory(StatKeys.CATEGORY_KILLED_BY);
    }

    // -------------------------------------------------------------------------
    // Advanced Helpers
    // -------------------------------------------------------------------------

    public List<Map.Entry<String, Long>>
    getTopEntries(
            String category,
            int limit
    ) {

        return getCategory(category)
                .entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Long>comparingByValue()
                                .reversed()
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean hasCategory(
            String category
    ) {

        return rawStats.containsKey(category);
    }

    public int getCategorySize(
            String category
    ) {

        return getCategory(category)
                .size();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private long custom(
            String key
    ) {

        return getStat(
                StatKeys.CATEGORY_CUSTOM,
                key
        );
    }

    private long sumCategory(
            String category
    ) {

        long total = 0L;

        for (long value
                : getCategory(category)
                .values()) {

            total += value;
        }

        return total;
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getReadTimestamp() {
        return readTimestamp;
    }

    /**
     * Get how many times a specific block was mined.
     * Example: getMined("minecraft:sand")
     */
    public long getMined(String material) {
        return getStat(StatKeys.CATEGORY_MINED, material);
    }

    /**
     * Get how many times a specific item was picked up.
     * Example: getPickedUp("minecraft:dirt")
     */
    public long getPickedUp(String material) {
        return getStat(StatKeys.CATEGORY_PICKED_UP, material);
    }

    /**
     * Get how many times a specific item was used.
     */
    public long getUsed(String material) {
        return getStat(StatKeys.CATEGORY_USED, material);
    }

    /**
     * Get how many times a specific item was broken.
     */
    public long getBroken(String material) {
        return getStat(StatKeys.CATEGORY_BROKEN, material);
    }

    /**
     * Get how many times a specific item was crafted.
     */
    public long getCrafted(String material) {
        return getStat(StatKeys.CATEGORY_CRAFTED, material);
    }

    /**
     * Get how many times a specific item was dropped.
     */
    public long getDropped(String material) {
        return getStat(StatKeys.CATEGORY_DROPPED, material);
    }

    /**
     * Get how many times a specific entity was killed.
     */
    public long getKilled(String entity) {
        return getStat(StatKeys.CATEGORY_KILLED, entity);
    }

    /**
     * Get how many times the player was killed by a specific entity.
     */
    public long getKilledBy(String entity) {
        return getStat(StatKeys.CATEGORY_KILLED_BY, entity);
    }

    // -------------------------------------------------------------------------
    // Convenience methods with "minecraft:" prefix handling
    // -------------------------------------------------------------------------

    /**
     * Flexible mined method - accepts both "minecraft:sand" and "sand"
     */
    public long getMinedFlexible(String input) {
        return getStat(StatKeys.CATEGORY_MINED, normalizeKey(input));
    }

    /**
     * Flexible picked up method
     */
    public long getPickedUpFlexible(String input) {
        return getStat(StatKeys.CATEGORY_PICKED_UP, normalizeKey(input));
    }

    /**
     * Normalizes key by ensuring it has "minecraft:" prefix if missing
     */
    private String normalizeKey(String key) {
        if (key == null) return "";
        String trimmed = key.trim();
        if (trimmed.contains(":")) {
            return trimmed;
        }
        return "minecraft:" + trimmed;
    }
}