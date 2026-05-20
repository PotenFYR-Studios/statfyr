package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;

/**
 * Handles the `/api/health` endpoint.
 *
 * <p>This endpoint is mainly used for:
 * <ul>
 *     <li>Health monitoring</li>
 *     <li>Server diagnostics</li>
 *     <li>Uptime tracking</li>
 *     <li>Memory monitoring</li>
 *     <li>Feature status inspection</li>
 * </ul>
 *
 * <p>Example response:
 * <pre>
 * {
 *   "system": {...},
 *   "players": {...},
 *   "memory": {...},
 *   "features": {...},
 *   "executor": {...}
 * }
 * </pre>
 */
public final class HealthHandler implements HttpHandler {

    /**
     * Main plugin instance.
     */
    private final Statfyr plugin;

    /**
     * Timestamp when this handler was created.
     *
     * <p>Used for uptime calculations.
     */
    private final long startTimeMillis =
            System.currentTimeMillis();

    /**
     * Creates a new HealthHandler instance.
     *
     * @param plugin Main plugin instance
     */
    public HealthHandler(Statfyr plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles incoming HTTP requests to `/api/health`.
     *
     * <p>This method gathers:
     * <ul>
     *     <li>System metadata</li>
     *     <li>Player statistics</li>
     *     <li>Memory usage information</li>
     *     <li>Feature toggles</li>
     *     <li>Executor service state</li>
     * </ul>
     *
     * @param exchange HTTP request/response exchange
     * @throws IOException if response writing fails
     */
    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        ConfigManager config = null;

        try {

            // -----------------------------------------------------------------
            // Load Configuration
            // -----------------------------------------------------------------

            config = plugin.getConfigManager();

            // -----------------------------------------------------------------
            // Calculate Server Uptime
            // -----------------------------------------------------------------

            long uptimeSeconds =
                    (System.currentTimeMillis()
                            - startTimeMillis)
                            / 1000L;

            // -----------------------------------------------------------------
            // JVM Runtime Memory Stats
            // -----------------------------------------------------------------

            Runtime runtime =
                    Runtime.getRuntime();

            /*
             * Used memory = allocated memory - free memory
             */
            long usedMemoryMb =
                    (runtime.totalMemory()
                            - runtime.freeMemory())
                            / 1024L / 1024L;

            /*
             * Maximum memory available to JVM.
             */
            long maxMemoryMb =
                    runtime.maxMemory()
                            / 1024L / 1024L;

            /*
             * Currently unused allocated memory.
             */
            long freeMemoryMb =
                    runtime.freeMemory()
                            / 1024L / 1024L;

            // -----------------------------------------------------------------
            // Advanced Heap Memory Statistics
            // -----------------------------------------------------------------

            MemoryMXBean memoryBean =
                    ManagementFactory.getMemoryMXBean();

            MemoryUsage heap =
                    memoryBean.getHeapMemoryUsage();

            // -----------------------------------------------------------------
            // System Information Section
            // -----------------------------------------------------------------

            JsonBuilder system =
                    new JsonBuilder()

                            /*
                             * General API status.
                             */
                            .add(
                                    "status",
                                    "ok"
                            )

                            /*
                             * Current UTC timestamp.
                             */
                            .add(
                                    "timestamp",
                                    Instant.now().toString()
                            )

                            /*
                             * API uptime in seconds.
                             */
                            .add(
                                    "uptime_seconds",
                                    uptimeSeconds
                            )

                            /*
                             * Plugin version from plugin.yml.
                             */
                            .add(
                                    "plugin_version",
                                    plugin.getDescription()
                                            .getVersion()
                            )

                            /*
                             * Clean Minecraft version.
                             */
                            .add(
                                    "minecraft_version",
                                    getMinecraftVer()
                            )

                            /*
                             * Full server version.
                             */
                            .add(
                                    "server_version",
                                    Bukkit.getVersion()
                            )

                            /*
                             * Bukkit API version.
                             */
                            .add(
                                    "bukkit_version",
                                    Bukkit.getBukkitVersion()
                            );

            // -----------------------------------------------------------------
            // Player Information Section
            // -----------------------------------------------------------------

            JsonBuilder players =
                    new JsonBuilder()

                            /*
                             * Number of currently online players.
                             */
                            .add(
                                    "online",
                                    Bukkit.getOnlinePlayers()
                                            .size()
                            )

                            /*
                             * Maximum player slots.
                             */
                            .add(
                                    "max",
                                    Bukkit.getMaxPlayers()
                            );

            // -----------------------------------------------------------------
            // Memory Information Section
            // -----------------------------------------------------------------

            JsonBuilder memory =
                    new JsonBuilder()

                            /*
                             * Used JVM memory.
                             */
                            .add(
                                    "used_mb",
                                    usedMemoryMb
                            )

                            /*
                             * Free allocated JVM memory.
                             */
                            .add(
                                    "free_mb",
                                    freeMemoryMb
                            )

                            /*
                             * JVM maximum memory limit.
                             */
                            .add(
                                    "max_mb",
                                    maxMemoryMb
                            )

                            /*
                             * Heap memory currently in use.
                             */
                            .add(
                                    "heap_used_mb",
                                    heap.getUsed()
                                            / 1024L / 1024L
                            )

                            /*
                             * Heap memory currently allocated.
                             */
                            .add(
                                    "heap_committed_mb",
                                    heap.getCommitted()
                                            / 1024L / 1024L
                            )

                            /*
                             * Maximum heap memory allowed.
                             */
                            .add(
                                    "heap_max_mb",
                                    heap.getMax()
                                            / 1024L / 1024L
                            );

            // -----------------------------------------------------------------
            // Feature Status Section
            // -----------------------------------------------------------------

            JsonBuilder features =
                    new JsonBuilder()

                            /*
                             * HTTPS support enabled/disabled.
                             */
                            .add(
                                    "https_enabled",
                                    config.isHttpsEnabled()
                            )

                            /*
                             * Compression enabled/disabled.
                             */
                            .add(
                                    "compression_enabled",
                                    config.isCompressionEnabled()
                            )

                            /*
                             * Async execution enabled/disabled.
                             */
                            .add(
                                    "async_enabled",
                                    config.isAsyncEnabled()
                            )

                            /*
                             * Rate limiting enabled/disabled.
                             */
                            .add(
                                    "rate_limit_enabled",
                                    config.isEnableRateLimit()
                            )

                            /*
                             * API authentication enabled/disabled.
                             */
                            .add(
                                    "api_auth_enabled",
                                    config.isAuthEnabled()
                            )

                            /*
                             * Documentation endpoint enabled/disabled.
                             */
                            .add(
                                    "docs_enabled",
                                    config.isDocsEnabled()
                            );

            // -----------------------------------------------------------------
            // Executor Service Section
            // -----------------------------------------------------------------

            JsonBuilder executor =
                    new JsonBuilder()

                            /*
                             * Checks whether the async executor
                             * is still running.
                             */
                            .add(
                                    "active",
                                    plugin.getExecutorService() != null
                                            && !plugin.getExecutorService()
                                            .isShutdown()
                            );

            // -----------------------------------------------------------------
            // Build Final JSON Response
            // -----------------------------------------------------------------

            String json = new JsonBuilder()

                    .add(
                            "system",
                            system.buildMap()
                    )

                    .add(
                            "players",
                            players.buildMap()
                    )

                    .add(
                            "memory",
                            memory.buildMap()
                    )

                    .add(
                            "features",
                            features.buildMap()
                    )

                    .add(
                            "executor",
                            executor.buildMap()
                    )

                    .build();

            // -----------------------------------------------------------------
            // Send Successful Response
            // -----------------------------------------------------------------

            ResponseUtil.sendOk(
                    exchange,
                    json
            );

        } catch (Exception exception) {

            // -----------------------------------------------------------------
            // Error Handling
            // -----------------------------------------------------------------

            plugin.getLogger().severe(
                    "Health endpoint failure"
            );

            /*
             * Print full stacktrace only when debug mode is enabled.
             */
            if (config != null && config.isDebug()) {

                plugin.getLogger().log(
                        java.util.logging.Level.SEVERE,
                        "Failed to generate health response",
                        exception
                );
            }

            // Send generic internal server error response
            ResponseUtil.sendInternalError(
                    exchange,
                    "Failed to generate health response"
            );
        }
    }

    /**
     * Attempts to retrieve a clean Minecraft version string.
     *
     * <p>This method supports both:
     * <ul>
     *     <li>Modern Bukkit APIs</li>
     *     <li>Older Bukkit/Paper versions</li>
     * </ul>
     *
     * <p>Example output:
     * <pre>
     * 1.21.3
     * </pre>
     *
     * @return Clean Minecraft version string
     */
    private static @NotNull String getMinecraftVer() {

        String mc_version = null;

        try {

            /*
             * Available on newer Bukkit versions.
             */
            mc_version = Bukkit.getMinecraftVersion();

        } catch (NoSuchMethodError | NoClassDefFoundError ignored) {

            /*
             * Ignore errors for older server versions.
             */
        }

        // ---------------------------------------------------------------------
        // Fallback for older Bukkit versions
        // ---------------------------------------------------------------------

        if (mc_version == null) {

            /*
             * Example:
             * 1.21.3-R0.1-SNAPSHOT
             */
            String bukkitVersion =
                    Bukkit.getBukkitVersion();

            /*
             * Extract only the clean MC version.
             */
            int dashIndex =
                    bukkitVersion.indexOf('-');

            if (dashIndex != -1) {

                bukkitVersion =
                        bukkitVersion.substring(
                                0,
                                dashIndex
                        );
            }

            mc_version = bukkitVersion;
        }

        return mc_version;
    }
}