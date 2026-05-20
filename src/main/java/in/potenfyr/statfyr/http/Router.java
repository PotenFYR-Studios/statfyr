package in.potenfyr.statfyr.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Central API router.
 * Responsibilities:
 * - route dispatching
 * - auth
 * - rate limiting
 * - CORS
 * - request logging
 * - error handling
 */
public final class Router implements HttpHandler {

    /**
     * Longest prefix match wins.
     */
    private final LinkedHashMap<String, HttpHandler> routes =
            new LinkedHashMap<>();

    private final Statfyr plugin;

    /**
     * Rate limit cache.
     */
    private final ConcurrentHashMap<String, RateLimitEntry>
            rateLimitMap =
            new ConcurrentHashMap<>();

    public Router(Statfyr plugin) {

        this.plugin = plugin;
    }

    public void addHandler(
            String pathPrefix,
            HttpHandler handler
    ) {

        routes.put(
                pathPrefix,
                handler
        );
    }

    // -------------------------------------------------------------------------
    // HttpHandler
    // -------------------------------------------------------------------------

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        long start =
                System.currentTimeMillis();

        try {

            ConfigManager config =
                    plugin.getConfigManager();

            applyCors(exchange);

            // OPTIONS preflight
            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod()
            )) {

                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                return;
            }

            // GET only
            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod()
            )) {

                ResponseUtil.sendMethodNotAllowed(
                        exchange
                );

                return;
            }

            // IP whitelist
            if (!isIpAllowed(exchange)) {

                ResponseUtil.sendForbidden(
                        exchange,
                        "IP not allowed"
                );

                return;
            }

            // Authentication
            if (!isAuthorized(exchange)) {

                ResponseUtil.sendUnauthorized(
                        exchange
                );

                return;
            }

            // Rate limiting
            if (!isRateLimitAllowed(exchange)) {

                ResponseUtil.sendTooManyRequests(
                        exchange,
                        "Rate limit exceeded"
                );

                return;
            }

            String requestPath =
                    exchange.getRequestURI()
                            .getPath();

            HttpHandler matchedHandler =
                    findHandler(requestPath);

            if (matchedHandler == null) {

                ResponseUtil.sendNotFound(
                        exchange,
                        "No endpoint found for path: "
                                + requestPath
                );

                return;
            }

            logRequest(exchange);

            matchedHandler.handle(exchange);

            long executionTime =
                    System.currentTimeMillis()
                            - start;

            if (config.isDebug()) {

                plugin.getLogger().info(
                        "[HTTP] "
                                + requestPath
                                + " completed in "
                                + executionTime
                                + "ms"
                );
            }

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Unhandled exception in HTTP request handler",
                    exception
            );

            try {

                ResponseUtil.sendInternalError(
                        exchange,
                        "Internal server error"
                );

            } catch (IOException ignored) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Routing
    // -------------------------------------------------------------------------

    private HttpHandler findHandler(
            String requestPath
    ) {

        // Exact match
        if (routes.containsKey(requestPath)) {

            return routes.get(requestPath);
        }

        HttpHandler bestMatch = null;

        int bestLength = -1;

        for (Map.Entry<String, HttpHandler> entry
                : routes.entrySet()) {

            String prefix =
                    entry.getKey();

            if (requestPath.startsWith(prefix)
                    && prefix.length() > bestLength) {

                bestMatch =
                        entry.getValue();

                bestLength =
                        prefix.length();
            }
        }

        return bestMatch;
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    private boolean isAuthorized(
            HttpExchange exchange
    ) {

        ConfigManager config =
                plugin.getConfigManager();

        if (!config.isAuthEnabled()) {
            return true;
        }

        String authorization =
                exchange.getRequestHeaders()
                        .getFirst("Authorization");

        if (authorization == null) {
            return false;
        }

        String expectedToken =
                "Bearer "
                        + config.getApiKey();

        return expectedToken.equals(
                authorization
        );
    }

    // -------------------------------------------------------------------------
    // Rate Limiting
    // -------------------------------------------------------------------------

    private boolean isRateLimitAllowed(
            HttpExchange exchange
    ) {

        ConfigManager config =
                plugin.getConfigManager();

        if (!config.isEnableRateLimit()) {
            return true;
        }

        String ip =
                getClientIp(exchange);

        long now =
                System.currentTimeMillis();

        RateLimitEntry entry =
                rateLimitMap.computeIfAbsent(
                        ip,
                        ignored -> new RateLimitEntry()
                );

        synchronized (entry) {

            long elapsed =
                    now - entry.windowStart;

            if (elapsed >
                    config.getRateLimitWindowSeconds()
                            * 1000L) {

                entry.windowStart = now;
                entry.requests = 0;
            }

            entry.requests++;

            return entry.requests
                    <= config.getRateLimitRequests();
        }
    }

    // -------------------------------------------------------------------------
    // IP Whitelist
    // -------------------------------------------------------------------------

    private boolean isIpAllowed(HttpExchange exchange) {

        ConfigManager config =
                plugin.getConfigManager();

        if (!config.isEnableIpWhitelist()) {
            return true;
        }

        String ip =
                getClientIp(exchange);

        // Normalize localhost IPv6
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        for (String allowedIp
                : config.getAllowedIps()) {

            if (allowedIp.equalsIgnoreCase(ip)) {
                return true;
            }
        }

        if (config.isDebug()) {

            plugin.getLogger().warning(
                    "[Whitelist] Blocked IP: " + ip
            );
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // CORS
    // -------------------------------------------------------------------------

    private void applyCors(
            HttpExchange exchange
    ) {

        ConfigManager config =
                plugin.getConfigManager();

        if (!config.isEnableCors()) {
            return;
        }

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin",
                "*"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, OPTIONS"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Authorization, Content-Type"
        );
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    private void logRequest(
            HttpExchange exchange
    ) {

        if (!plugin.getConfigManager()
                .isDebug()) {

            return;
        }

        plugin.getLogger().info(
                "[HTTP] "
                        + Instant.now()
                        + " "
                        + exchange.getRequestMethod()
                        + " "
                        + exchange.getRequestURI()
                        + " "
                        + getClientIp(exchange)
        );
    }

    // -------------------------------------------------------------------------
    // Utils
    // -------------------------------------------------------------------------

    private String getClientIp(
            HttpExchange exchange
    ) {

        InetAddress address =
                exchange.getRemoteAddress()
                        .getAddress();

        return address != null
                ? address.getHostAddress()
                : "unknown";
    }

    // -------------------------------------------------------------------------
    // Rate Limit Entry
    // -------------------------------------------------------------------------

    private static final class RateLimitEntry {

        long windowStart =
                System.currentTimeMillis();

        int requests = 0;
    }
}