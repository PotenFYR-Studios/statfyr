package in.potenfyr.statfyr.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of network peers.
 *
 * <p>StatFYR instances can push a {@link PeerReport} to a hub, which stores
 * them here and aggregates the whole network in {@code /api/network}. Reports
 * expire after a configurable TTL so dead servers drop out automatically.
 *
 * <p>A single-server install simply has an empty peer set and returns itself.
 */
public final class NetworkRegistry {

    private final ConcurrentHashMap<String, PeerReport> peers =
            new ConcurrentHashMap<>();

    private volatile long ttlMillis =
            180_000L;

    /**
     * @param ttlSeconds how long a peer report stays valid
     */
    public void setTtlSeconds(long ttlSeconds) {

        this.ttlMillis =
                Math.max(5L, ttlSeconds) * 1000L;
    }

    /**
     * Records (or refreshes) a peer report.
     *
     * @param report incoming report
     */
    public void report(PeerReport report) {

        if (report == null
                || report.serverId == null
                || report.serverId.trim().isEmpty()) {
            return;
        }

        report.updatedAt =
                System.currentTimeMillis();

        peers.put(report.serverId, report);
    }

    /**
     * @return non-expired peers, most recently seen first
     */
    public List<PeerReport> active() {

        long cutoff =
                System.currentTimeMillis() - ttlMillis;

        peers.entrySet().removeIf(
                entry -> entry.getValue().updatedAt < cutoff
        );

        List<PeerReport> result =
                new ArrayList<>(peers.values());

        result.sort(
                Comparator.comparingInt(
                        (PeerReport report) -> report.online
                ).reversed()
        );

        return result;
    }

    public int size() {

        return peers.size();
    }

    /**
     * Aggregates the local server plus all active peers.
     *
     * @param local the local server's report
     * @return JSON-friendly network map
     */
    public Map<String, Object> aggregate(PeerReport local) {

        List<PeerReport> all =
                new ArrayList<>();

        local.updatedAt =
                System.currentTimeMillis();

        all.add(local);
        all.addAll(active());

        List<Object> servers =
                new ArrayList<>();

        int networkPlayers =
                0;

        int networkTotalPlayers =
                0;

        long networkSessions =
                0L;

        for (PeerReport report : all) {

            networkPlayers += report.online;
            networkTotalPlayers += report.totalPlayers;
            networkSessions += report.sessions;

            Map<String, Object> server =
                    new LinkedHashMap<>();

            server.put("id", report.serverId);
            server.put("name", report.serverName);
            server.put("online", report.online);
            server.put("total_players", report.totalPlayers);
            server.put("sessions", report.sessions);
            server.put("peak_all_time", report.peakAllTime);
            server.put("local", report == local);

            servers.add(server);
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("network_players", networkPlayers);
        response.put("network_total_players", networkTotalPlayers);
        response.put("network_sessions", networkSessions);
        response.put("server_count", all.size());
        response.put("servers", servers);

        return response;
    }
}
