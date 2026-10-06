package in.potenfyr.statfyr.analytics;

/**
 * A single leaderboard row.
 */
public final class LeaderboardEntry {

    public final String uuid;
    public final String name;
    public final long value;
    public final double decimalValue;
    public final boolean online;

    public LeaderboardEntry(
            String uuid,
            String name,
            long value,
            double decimalValue,
            boolean online
    ) {

        this.uuid = uuid;
        this.name = name;
        this.value = value;
        this.decimalValue = decimalValue;
        this.online = online;
    }
}
