package in.potenfyr.statfyr.analytics;

/**
 * A summary of one server in a network, exchanged between StatFYR instances.
 *
 * <p>Servers push these to a hub (or accept them) so {@code /api/network} can
 * aggregate a whole BungeeCord/Velocity network without a shared database.
 */
public final class PeerReport {

    public String serverId = "server-1";
    public String serverName = "Survival";

    public int online;
    public int totalPlayers;
    public long sessions;
    public int peakAllTime;

    /** epoch millis the report was last received. */
    public long updatedAt;

    public PeerReport() {
    }

    public PeerReport(
            String serverId,
            String serverName,
            int online,
            int totalPlayers,
            long sessions,
            int peakAllTime
    ) {

        this.serverId = serverId;
        this.serverName = serverName;
        this.online = online;
        this.totalPlayers = totalPlayers;
        this.sessions = sessions;
        this.peakAllTime = peakAllTime;
    }
}
