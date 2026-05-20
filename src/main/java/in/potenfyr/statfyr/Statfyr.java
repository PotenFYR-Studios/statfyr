package in.potenfyr.statfyr;

import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.http.HttpServer;
import in.potenfyr.statfyr.player.PlayerService;
import in.potenfyr.statfyr.stats.StatsManager;
import in.potenfyr.statfyr.stats.StatsReader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Main plugin entry-point.
 * Responsibilities:
 * - Bootstrap core systems
 * - Manage lifecycle
 * - Hold shared services
 * - Start/stop HTTP server
 */
public final class Statfyr extends JavaPlugin {

    private static Statfyr instance;

    // -------------------------------------------------------------------------
    // Core Services
    // -------------------------------------------------------------------------

    private ConfigManager configManager;
    private StatsReader statsReader;
    private PlayerService playerService;
    private StatsManager statsManager;

    // -------------------------------------------------------------------------
    // HTTP
    // -------------------------------------------------------------------------

    private HttpServer httpServer;

    // -------------------------------------------------------------------------
    // Async Systems
    // -------------------------------------------------------------------------

    /**
     * Shared async executor for:
     * - async stats reading
     * - compression
     * - websocket broadcasting
     * - future DB operations
     */
    private ExecutorService executorService;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        try {

            initializeCore();

            initializeHttp();

            getLogger().info("Statfyr enabled successfully.");

        } catch (Exception exception) {

            getLogger().log(
                    Level.SEVERE,
                    "Failed to enable Statfyr",
                    exception
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {

        shutdownHttp();

        shutdownExecutor();

        getLogger().info("Statfyr disabled.");
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    private void initializeCore() {

        // Config
        configManager = new ConfigManager(this);

        // Shared async executor
        executorService =
                Executors.newFixedThreadPool(
                        Math.max(
                                2,
                                Runtime.getRuntime()
                                        .availableProcessors() / 2
                        )
                );

        // Stats
        statsReader = new StatsReader(this);

        statsManager =
                new StatsManager(
                        this,
                        statsReader
                );

        statsManager.start();

        // Players
        playerService = new PlayerService(this);

        getLogger().info("Core systems initialized.");
    }

    private void initializeHttp() throws Exception {

        httpServer = new HttpServer(this);

        httpServer.start();

        getLogger().info(
                "HTTP server started on port " +
                        configManager.getPort()
        );
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    private void shutdownHttp() {

        if (httpServer == null) {
            return;
        }

        try {

            httpServer.stop();

            getLogger().info("HTTP server stopped.");

        } catch (Exception exception) {

            getLogger().log(
                    Level.WARNING,
                    "Failed to stop HTTP server cleanly",
                    exception
            );
        }
    }

    private void shutdownExecutor() {

        if (executorService == null) {
            return;
        }

        executorService.shutdown();

        try {

            if (!executorService.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            )) {

                executorService.shutdownNow();
            }

        } catch (InterruptedException exception) {

            executorService.shutdownNow();

            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("statfyr")) {
            return false;
        }

        if (!sender.hasPermission("statfyr.admin")) {

            sender.sendMessage(
                    "§cYou do not have permission."
            );

            return true;
        }

        if (args.length == 0) {

            sender.sendMessage(
                    "§eUsage: /statfyr <reload|status>"
            );

            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload":
                reloadPlugin(sender);
                break;

            case "status":
                showStatus(sender);
                break;

            default:
                sender.sendMessage(
                        "§eUsage: /statfyr <reload|status>"
                );
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Reload
    // -------------------------------------------------------------------------

    private void reloadPlugin(CommandSender sender) {

        try {

            reloadConfig();

            configManager.reload();

            shutdownHttp();

            initializeHttp();

            sender.sendMessage(
                    "§aStatfyr reloaded successfully."
            );

        } catch (Exception exception) {

            getLogger().log(
                    Level.SEVERE,
                    "Failed to reload Statfyr",
                    exception
            );

            sender.sendMessage(
                    "§cFailed to reload Statfyr."
            );
        }
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    private void showStatus(CommandSender sender) {

        sender.sendMessage("§6=== Statfyr Status ===");

        sender.sendMessage(
                "§eHTTP Running: §f" +
                        (httpServer != null && httpServer.isRunning())
        );

        sender.sendMessage(
                "§eHTTP Port: §f" +
                        configManager.getPort()
        );

        sender.sendMessage(
                "§eStatsManager: §f" +
                        (statsManager != null)
        );

        sender.sendMessage(
                "§eExecutor Active: §f" +
                        (executorService != null
                                && !executorService.isShutdown())
        );

        sender.sendMessage(
                "§eOnline Players: §f" +
                        getServer()
                                .getOnlinePlayers()
                                .size()
        );
    }

    // -------------------------------------------------------------------------
    // Static Accessor
    // -------------------------------------------------------------------------

    public static Statfyr getInstance() {
        return instance;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StatsReader getStatsReader() {
        return statsReader;
    }

    public PlayerService getPlayerResolver() {
        return playerService;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }
}