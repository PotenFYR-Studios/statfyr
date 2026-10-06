package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.PeerReport;
import in.potenfyr.statfyr.analytics.ServerStats;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.util.Map;

/**
 * Network / multi-server aggregation endpoint.
 *
 * <p>A single-server install returns just this server. When other StatFYR
 * instances push reports to this node (or this node pushes to a hub), the whole
 * network is aggregated here.
 *
 * <p>{@code GET /api/network}
 */
public final class NetworkHandler implements HttpHandler {

    private final Statfyr plugin;

    public NetworkHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        ServerStats stats =
                plugin.getAnalytics().serverStats();

        PeerReport local =
                new PeerReport(
                        plugin.getAnalytics().serverId(),
                        plugin.getAnalytics().serverName(),
                        stats.online,
                        stats.totalPlayers,
                        stats.sessionsTotal,
                        stats.peakAllTime
                );

        Map<String, Object> response =
                plugin.getNetworkRegistry()
                        .aggregate(local);

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(response)
        );
    }
}
