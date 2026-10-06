package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.ServerStats;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Prometheus exposition endpoint ({@code GET /metrics}).
 *
 * <p>Follows the Prometheus text exposition format
 * ({@code text/plain; version=0.0.4}). No Prometheus dependency is required —
 * the format is generated directly.
 */
public final class MetricsHandler implements HttpHandler {

    private final Statfyr plugin;

    public MetricsHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        ServerStats stats =
                plugin.getAnalytics().serverStats();

        StringBuilder builder =
                new StringBuilder();

        gauge(
                builder,
                "statfyr_online_players",
                "Players currently online",
                stats.online
        );

        gauge(
                builder,
                "statfyr_unique_players_today",
                "Distinct players seen today",
                stats.uniqueToday
        );

        gauge(
                builder,
                "statfyr_unique_players_week",
                "Distinct players seen this week",
                stats.uniqueWeek
        );

        gauge(
                builder,
                "statfyr_unique_players_month",
                "Distinct players seen this month",
                stats.uniqueMonth
        );

        gauge(
                builder,
                "statfyr_peak_players_today",
                "Peak concurrent players today",
                stats.peakToday
        );

        gauge(
                builder,
                "statfyr_peak_players_all_time",
                "Peak concurrent players all time",
                stats.peakAllTime
        );

        gauge(
                builder,
                "statfyr_player_sessions",
                "Total sessions recorded",
                stats.sessionsTotal
        );

        gauge(
                builder,
                "statfyr_server_playtime_seconds",
                "Total tracked playtime in seconds",
                stats.totalPlaytimeSeconds
        );

        gauge(
                builder,
                "statfyr_new_players_today",
                "Players joining for the first time today",
                stats.newPlayersToday
        );

        gauge(
                builder,
                "statfyr_returning_players_today",
                "Players seen today that are not new",
                stats.returningPlayersToday
        );

        gauge(
                builder,
                "statfyr_profiles_total",
                "Player profiles tracked",
                plugin.getAnalytics().profileCount()
        );

        decimal(
                builder,
                "statfyr_average_session_seconds",
                "Average session duration in seconds",
                stats.averageSessionSeconds
        );

        decimal(
                builder,
                "statfyr_average_concurrent_players",
                "Average concurrent players over the last 24h",
                stats.averageConcurrent
        );

        byte[] body =
                builder.toString()
                        .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; version=0.0.4; charset=utf-8"
        );

        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(body);
        }
    }

    private static void gauge(
            StringBuilder builder,
            String name,
            String help,
            long value
    ) {

        builder.append("# HELP ").append(name).append(' ')
                .append(help).append('\n');

        builder.append("# TYPE ").append(name).append(" gauge\n");
        builder.append(name).append(' ').append(value).append('\n');
    }

    private static void decimal(
            StringBuilder builder,
            String name,
            String help,
            double value
    ) {

        builder.append("# HELP ").append(name).append(' ')
                .append(help).append('\n');

        builder.append("# TYPE ").append(name).append(" gauge\n");
        builder.append(name).append(' ')
                .append(String.format(java.util.Locale.ROOT, "%.3f", value))
                .append('\n');
    }
}
