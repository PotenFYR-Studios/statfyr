package in.potenfyr.statfyr.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.http.handlers.HealthHandler;
import in.potenfyr.statfyr.http.handlers.LeaderboardHandler;
import in.potenfyr.statfyr.http.handlers.PlayerStatsHandler;
import in.potenfyr.statfyr.http.handlers.PlayerSummaryHandler;
import in.potenfyr.statfyr.http.handlers.PlayersListHandler;
import in.potenfyr.statfyr.util.ResponseUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

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

        // API Docs placeholder
        // API Root


// API Docs
        router.addHandler(
                "/api/docs",
                exchange -> {

                    ResponseUtil.sendJson(
                            exchange,
                            200,
                            """
                                    {
                                      "name":"Statfyr",
                                      "version":"1.0.0",
                                      "base_url":"/api",
                                      "authentication":"Bearer token optional",
                                      "endpoints":[
                                        {
                                          "path":"/api/health",
                                          "method":"GET",
                                          "description":"API health information"
                                        },
                                        {
                                          "path":"/api/players",
                                          "method":"GET",
                                          "description":"List all players"
                                        },
                                        {
                                          "path":"/api/player/{player}",
                                          "method":"GET",
                                          "description":"Full player stats"
                                        },
                                        {
                                          "path":"/api/player/{player}/summary",
                                          "method":"GET",
                                          "description":"Player summary"
                                        },
                                        {
                                          "path":"/api/leaderboard/{type}",
                                          "method":"GET",
                                          "description":"Leaderboard endpoint"
                                        }
                                      ]
                                    }
                                    """
                    );
                }
        );
        router.addHandler(
                "/api",
                exchange -> {

                    ResponseUtil.sendJson(
                            exchange,
                            200,
                            """
                                    {
                                      "name":"Statfyr",
                                      "version":"1.0.0",
                                      "docs":"/api/docs"
                                    }
                                    """
                    );
                }
        );

        server.createContext(
                "/api/",
                router
        );
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

        private final PlayerStatsHandler statsHandler;

        private final PlayerSummaryHandler summaryHandler;

        PlayerRouteHandler(Statfyr plugin) {

            this.statsHandler =
                    new PlayerStatsHandler(plugin);

            this.summaryHandler =
                    new PlayerSummaryHandler(plugin);
        }

        @Override
        public void handle(
                HttpExchange exchange
        ) throws IOException {

            String path =
                    exchange.getRequestURI()
                            .getPath();

            if (path.endsWith("/summary")) {

                summaryHandler.handle(exchange);

            } else {

                statsHandler.handle(exchange);
            }
        }
    }
}