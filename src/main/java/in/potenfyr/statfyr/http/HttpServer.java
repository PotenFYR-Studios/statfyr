package in.potenfyr.statfyr.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.compat.ServerVersion;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.http.handlers.ArchiveHandler;
import in.potenfyr.statfyr.http.handlers.CustomMetricHandler;
import in.potenfyr.statfyr.http.handlers.DashboardHandler;
import in.potenfyr.statfyr.http.handlers.HealthHandler;
import in.potenfyr.statfyr.http.handlers.LeaderboardHandler;
import in.potenfyr.statfyr.http.handlers.MetricsHandler;
import in.potenfyr.statfyr.http.handlers.NetworkHandler;
import in.potenfyr.statfyr.http.handlers.NetworkReportHandler;
import in.potenfyr.statfyr.http.handlers.PlayerAnalyticsHandler;
import in.potenfyr.statfyr.http.handlers.PlayerStatsHandler;
import in.potenfyr.statfyr.http.handlers.PlayerSummaryHandler;
import in.potenfyr.statfyr.http.handlers.PlayersListHandler;
import in.potenfyr.statfyr.http.handlers.ServerHandler;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Embedded HTTP server.
 * <p>
 * Supports:
 * - HTTP
 * - HTTPS
 * - async routing
 * - future websocket support
 */
public final class HttpServer {

    private static final int STOP_DELAY_SECONDS = 1;

    private final Statfyr plugin;

    private com.sun.net.httpserver.HttpServer server;

    private boolean running = false;

    public HttpServer(Statfyr plugin) {

        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Start
    // -------------------------------------------------------------------------

    public void start() throws Exception {

        ConfigManager config =
                plugin.getConfigManager();

        InetSocketAddress address =
                new InetSocketAddress(
                        config.getBindAddress(),
                        config.getPort()
                );

        // HTTPS
        if (config.isHttpsEnabled()) {

            HttpsServer httpsServer =
                    HttpsServer.create(
                            address,
                            0
                    );

            SSLContext sslContext =
                    createSSLContext(
                            config.getKeystorePath(),
                            config.getKeystorePassword()
                    );

            httpsServer.setHttpsConfigurator(
                    new HttpsConfigurator(
                            sslContext
                    )
            );

            server = httpsServer;

            plugin.getLogger().info(
                    "HTTPS enabled"
            );

        } else {

            server =
                    com.sun.net.httpserver.HttpServer
                            .create(
                                    address,
                                    0
                            );

            plugin.getLogger().info(
                    "HTTPS disabled, using HTTP"
            );
        }

        // Executor
        server.setExecutor(
                plugin.getExecutorService()
        );

        // Routes
        registerHandlers();

        server.start();

        running = true;

        plugin.getLogger().info(
                "HTTP server started on "
                        + config.getBindAddress()
                        + ":"
                        + config.getPort()
        );

    }

    // -------------------------------------------------------------------------
    // Stop
    // -------------------------------------------------------------------------

    public void stop() {

        if (server == null) {
            return;
        }

        server.stop(
                STOP_DELAY_SECONDS
        );

        running = false;

        plugin.getLogger().info(
                "HTTP server stopped"
        );
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    public boolean isRunning() {

        return running;
    }

    // -------------------------------------------------------------------------
    // Routing
    // -------------------------------------------------------------------------

    private void registerHandlers() {

        Router router =
                new Router(plugin);

        // Health
        router.addHandler(
                "/api/health",
                new HealthHandler(plugin)
        );

        // Players
        router.addHandler(
                "/api/players",
                new PlayersListHandler(plugin)
        );

        // Player
        router.addHandler(
                "/api/player/",
                new PlayerRouteHandler(plugin)
        );

        // Leaderboards
        router.addHandler(
                "/api/leaderboard/",
                new LeaderboardHandler(plugin)
        );

        // Server analytics
        router.addHandler(
                "/api/server",
                new ServerHandler(plugin)
        );

        // Network aggregation
        router.addHandler(
                "/api/network",
                new NetworkHandler(plugin)
        );

        // Custom metrics registered by other plugins
        router.addHandler(
                "/api/custom",
                new CustomMetricHandler(plugin)
        );

        // Archived leaderboard results
        router.addHandler(
                "/api/archive/",
                new ArchiveHandler(plugin)
        );

        // API Docs
        final String version =
                plugin.getDescription().getVersion();

        router.addHandler(
                "/api/docs",
                exchange -> ResponseUtil.sendJson(
                        exchange,
                        200,
                        buildApiDocs(version)
                )
        );

        router.addHandler(
                "/api",
                exchange -> ResponseUtil.sendJson(
                        exchange,
                        200,
                        buildApiRoot(version)
                )
        );

        server.createContext(
                "/api/",
                router
        );

        // Optional network report ingestion (POST), bypassing the GET-only
        // router so peers can push their summaries to this hub.
        if (plugin.getConfig().getBoolean(
                "network.accept-reports",
                true
        )) {

            server.createContext(
                    "/api/network/report",
                    new NetworkReportHandler(plugin)
            );
        }

        // Optional Prometheus exposition endpoint (outside /api, so it is not
        // subject to the bearer-token gate unless the admin exposes it).
        if (plugin.getConfig().getBoolean(
                "integrations.prometheus.enabled",
                false
        )) {

            server.createContext(
                    "/metrics",
                    new MetricsHandler(plugin)
            );

            plugin.getLogger().info(
                    "Prometheus metrics endpoint enabled at /metrics"
            );
        }

        // Optional API-first web dashboard, configured in dashboard.yml.
        if (plugin.getDashboardConfig() != null
                && plugin.getDashboardConfig().isEnabled()) {

            server.createContext(
                    "/dashboard",
                    new DashboardHandler(plugin)
            );

            plugin.getLogger().info(
                    "Web dashboard enabled at /dashboard"
            );
        }
    }

    // -------------------------------------------------------------------------
    // API metadata
    // -------------------------------------------------------------------------

    private static String buildApiDocs(String version) {

        List<Object> endpoints =
                new ArrayList<>();

        endpoints.add(
                endpoint(
                        "/api/health",
                        "GET",
                        "API health information"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/players",
                        "GET",
                        "List all players"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/player/{player}",
                        "GET",
                        "Full player stats"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/player/{player}/summary",
                        "GET",
                        "Player summary"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/leaderboard/{type}",
                        "GET",
                        "Leaderboard endpoint (supports ?period=daily|weekly|monthly|all_time)"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/player/{player}/history",
                        "GET",
                        "Historical metric snapshots"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/player/{player}/activity",
                        "GET",
                        "Player activity timeline"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/player/{player}/sessions",
                        "GET",
                        "Session statistics"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/server/summary",
                        "GET",
                        "Server analytics summary"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/server/history",
                        "GET",
                        "Server concurrency history"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/server/activity",
                        "GET",
                        "Activity heatmap data"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/server/retention",
                        "GET",
                        "Retention cohorts"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/network",
                        "GET",
                        "Network / multi-server aggregation"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/custom/{metric}",
                        "GET",
                        "Custom metrics registered by other plugins"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/archive/{metric}",
                        "GET",
                        "Archived leaderboard results for completed periods"
                )
        );

        endpoints.add(
                endpoint(
                        "/api/server/economy",
                        "GET",
                        "Economy analytics (Vault, when present)"
                )
        );

        return new JsonBuilder()
                .add("name", "Statfyr")
                .add("version", version)
                .add("supported_versions", "1.8.x - 26.x")
                .add("platform", ServerVersion.getPlatformLabel())
                .add("base_url", "/api")
                .add("authentication", "Bearer token optional")
                .add("endpoints", endpoints)
                .build();
    }

    private static String buildApiRoot(String version) {

        return new JsonBuilder()
                .add("name", "Statfyr")
                .add("version", version)
                .add("supported_versions", "1.8.x - 26.x")
                .add("platform", ServerVersion.getPlatformLabel())
                .add("docs", "/api/docs")
                .build();
    }

    private static Map<String, Object> endpoint(
            String path,
            String method,
            String description
    ) {

        Map<String, Object> endpoint =
                new LinkedHashMap<>();

        endpoint.put("path", path);
        endpoint.put("method", method);
        endpoint.put("description", description);

        return endpoint;
    }

    // -------------------------------------------------------------------------
    // SSL
    // -------------------------------------------------------------------------

    private SSLContext createSSLContext(
            String keystorePath,
            String keystorePassword
    ) throws Exception {

        KeyStore keyStore =
                KeyStore.getInstance("JKS");

        try (FileInputStream fis =
                     new FileInputStream(
                             keystorePath
                     )) {

            keyStore.load(
                    fis,
                    keystorePassword.toCharArray()
            );
        }

        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(
                        KeyManagerFactory
                                .getDefaultAlgorithm()
                );

        kmf.init(
                keyStore,
                keystorePassword.toCharArray()
        );

        SSLContext sslContext =
                SSLContext.getInstance("TLS");

        sslContext.init(
                kmf.getKeyManagers(),
                null,
                null
        );

        return sslContext;
    }

    // -------------------------------------------------------------------------
    // Player Router
    // -------------------------------------------------------------------------

    private static final class PlayerRouteHandler
            implements com.sun.net.httpserver.HttpHandler {

        private final Statfyr plugin;

        private final PlayerStatsHandler statsHandler;

        private final PlayerSummaryHandler summaryHandler;

        private final PlayerAnalyticsHandler analyticsHandler;

        PlayerRouteHandler(Statfyr plugin) {

            this.plugin = plugin;

            this.statsHandler =
                    new PlayerStatsHandler(plugin);

            this.summaryHandler =
                    new PlayerSummaryHandler(plugin);

            this.analyticsHandler =
                    new PlayerAnalyticsHandler(plugin);
        }

        @Override
        public void handle(
                HttpExchange exchange
        ) throws IOException {

            String path =
                    exchange.getRequestURI()
                            .getPath();

            String subPath =
                    PlayerAnalyticsHandler.extractSubPath(path);

            if ("summary".equals(subPath)) {

                summaryHandler.handle(exchange);
                return;
            }

            if (!subPath.isEmpty()) {

                String identifier =
                        PlayerAnalyticsHandler.extractIdentifier(path);

                UUID uuid =
                        plugin.getPlayerResolver()
                                .resolveUuid(identifier);

                if (uuid == null) {

                    ResponseUtil.sendNotFound(
                            exchange,
                            "Player not found: " + identifier
                    );

                    return;
                }

                String name =
                        plugin.getPlayerResolver()
                                .resolvePlayerName(uuid);

                if (analyticsHandler.handle(
                        exchange,
                        uuid,
                        name,
                        subPath
                )) {
                    return;
                }

                ResponseUtil.sendNotFound(
                        exchange,
                        "Unknown player endpoint: " + path
                );

                return;
            }

            statsHandler.handle(exchange);
        }
    }
}