package in.potenfyr.statfyr.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
     * How often expired rate-limit entries are swept.
     */
    private static final long RATE_LIMIT_SWEEP_INTERVAL_MS = 60_000L;

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

    /**
     * Guards the periodic rate-limit sweep so it runs on one thread at a time.
     */
    private final AtomicBoolean sweeping =
            new AtomicBoolean(false);

    private volatile long lastSweepAt =
            System.currentTimeMillis();

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

            // GET only (HEAD is answered with the same status and no body)
            boolean isHead =
                    "HEAD".equalsIgnoreCase(
                            exchange.getRequestMethod()
                    );

            if (!isHead && !"GET".equalsIgnoreCase(
                    exchange.getRequestMethod()
            )) {

                ResponseUtil.sendMethodNotAllowed(
                        exchange
                );

                return;
            }

            exchange.setAttribute(
                    "statfyr.head",
                    Boolean.valueOf(isHead)
            );

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

        byte[] provided =
                authorization.getBytes(StandardCharsets.UTF_8);

        byte[] expected =
                ("Bearer " + config.getApiKey())
                        .getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(provided, expected);
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

        boolean allowed;

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

            allowed = entry.requests
                    <= config.getRateLimitRequests();
        }

        sweepRateLimits(now);

        return allowed;
    }

    /**
     * Periodically removes rate-limit entries whose window has expired so the
     * map cannot grow without bound (one entry per client IP otherwise).
     */
    private void sweepRateLimits(long now) {

        if (now - lastSweepAt
                < RATE_LIMIT_SWEEP_INTERVAL_MS) {
            return;
        }

        if (!sweeping.compareAndSet(false, true)) {
            return;
        }

        lastSweepAt = now;

        try {

            long windowMs =
                    plugin.getConfigManager()
                            .getRateLimitWindowSeconds()
                            * 1000L;

            Iterator<Map.Entry<String, RateLimitEntry>> iterator =
                    rateLimitMap.entrySet().iterator();

            while (iterator.hasNext()) {

                Map.Entry<String, RateLimitEntry> mapEntry =
                        iterator.next();

                RateLimitEntry entry =
                        mapEntry.getValue();

                synchronized (entry) {

                    if (now - entry.windowStart > windowMs * 2L) {
                        iterator.remove();
                    }
                }
            }

        } finally {

            sweeping.set(false);
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

        String origin =
                exchange.getRequestHeaders()
                        .getFirst("Origin");

        String allowed =
                resolveAllowedOrigin(config, origin);

        if (allowed == null) {

            // Origin not on the whitelist: send no CORS headers so the
            // browser blocks the request.
            return;
        }

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin",
                allowed
        );

        exchange.getResponseHeaders().set(
                "Vary",
                "Origin"
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

    /**
     * @param config active configuration
     * @param origin request Origin header (may be null)
     * @return the value to echo back, or {@code null} when the origin is not
     *         allowed. A {@code "*"} entry in the whitelist allows everything.
     */
    private static String resolveAllowedOrigin(
            ConfigManager config,
            String origin
    ) {

        if (origin == null || origin.isEmpty()) {

            // Non-browser client (curl, HTTP libs): CORS is irrelevant.
            return "*";
        }

        for (String allowed : config.getAllowedOrigins()) {

            if ("*".equals(allowed) || allowed.equalsIgnoreCase(origin)) {
                return origin;
            }
        }

        return null;
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