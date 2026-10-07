package in.potenfyr.statfyr;

import in.potenfyr.statfyr.analytics.AnalyticsManager;
import in.potenfyr.statfyr.analytics.NetworkRegistry;
import in.potenfyr.statfyr.commands.MessageService;
import in.potenfyr.statfyr.commands.StatfyrCommand;
import in.potenfyr.statfyr.compat.SchedulerCompat;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.config.DashboardConfig;
import in.potenfyr.statfyr.http.HttpServer;
import in.potenfyr.statfyr.integrations.DiscordIntegration;
import in.potenfyr.statfyr.integrations.NetworkPusher;
import in.potenfyr.statfyr.integrations.PlaceholderIntegration;
import in.potenfyr.statfyr.integrations.VaultIntegration;
import in.potenfyr.statfyr.listeners.PlayerListener;
import in.potenfyr.statfyr.player.PlayerService;
import in.potenfyr.statfyr.stats.StatsManager;
import in.potenfyr.statfyr.stats.StatsReader;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Main plugin entry-point.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Bootstrap core systems</li>
 *     <li>Manage lifecycle</li>
 *     <li>Hold shared services</li>
 *     <li>Start/stop HTTP server</li>
 *     <li>Wire optional integrations</li>
 * </ul>
 */
public final class Statfyr extends JavaPlugin {

    private static Statfyr instance;

    // -------------------------------------------------------------------------
    // Core Services
    // -------------------------------------------------------------------------

    private ConfigManager configManager;
    private DashboardConfig dashboardConfig;
    private StatsReader statsReader;
    private PlayerService playerService;
    private StatsManager statsManager;
    private AnalyticsManager analytics;
    private MessageService messages;
    private NetworkRegistry networkRegistry;

    // -------------------------------------------------------------------------
    // HTTP
    // -------------------------------------------------------------------------

    private HttpServer httpServer;

    // -------------------------------------------------------------------------
    // Integrations
    // -------------------------------------------------------------------------

    private PlaceholderIntegration placeholderIntegration;
    private VaultIntegration vaultIntegration;
    private DiscordIntegration discordIntegration;
    private NetworkPusher networkPusher;

    // -------------------------------------------------------------------------
    // Async Systems
    // -------------------------------------------------------------------------

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

            initializeIntegrations();

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

        if (analytics != null) {

            try {
                analytics.shutdown();

            } catch (Exception exception) {

                getLogger().log(
                        Level.WARNING,
                        "Failed to shut down analytics cleanly",
                        exception
                );
            }
        }

        shutdownHttp();

        shutdownExecutor();

        getLogger().info("Statfyr disabled.");
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    private void initializeCore() throws Exception {

        // Config
        configManager = new ConfigManager(this);

        // Dashboard config (separate dashboard.yml)
        dashboardConfig = new DashboardConfig(this);

        dashboardConfig.load();

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

        // Analytics
        messages = new MessageService(this);

        networkRegistry = new NetworkRegistry();

        analytics = new AnalyticsManager(this);
        analytics.setMilestoneListener(this::onMilestone);
        analytics.start();

        // Commands
        StatfyrCommand command =
                new StatfyrCommand(this, messages);

        PluginCommand pluginCommand =
                getCommand("statfyr");

        if (pluginCommand != null) {

            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);

            applyCommandAliases(pluginCommand);
        }

        // Listeners
        getServer()
                .getPluginManager()
                .registerEvents(
                        new PlayerListener(this),
                        this
                );

        getLogger().info("Core systems initialized.");
    }

    private void initializeIntegrations() {

        placeholderIntegration =
                new PlaceholderIntegration(this);

        placeholderIntegration.register();

        vaultIntegration =
                new VaultIntegration(this);

        vaultIntegration.register();

        discordIntegration =
                new DiscordIntegration(this);

        discordIntegration.start();

        networkPusher =
                new NetworkPusher(this);

        networkPusher.start();
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
    // Reload
    // -------------------------------------------------------------------------

    /**
     * Reloads configuration, messages, analytics settings and the HTTP server.
     *
     * @throws Exception when the HTTP server cannot be restarted
     */
    public void reloadStatfyr() throws Exception {

        reloadConfig();
        configManager.reload();
        dashboardConfig.load();
        messages.reload();
        analytics.reloadSettings();

        if (discordIntegration != null) {
            discordIntegration.reload();
        }

        if (networkPusher != null) {
            networkPusher.reload();
        }

        shutdownHttp();
        initializeHttp();

        PluginCommand pluginCommand =
                getCommand("statfyr");

        if (pluginCommand != null) {
            applyCommandAliases(pluginCommand);
        }

        getLogger().info("Statfyr reloaded.");
    }

    private void applyCommandAliases(PluginCommand command) {

        List<String> aliases =
                getConfig().getStringList("commands.aliases");

        if (aliases != null && !aliases.isEmpty()) {

            command.setAliases(aliases);

        } else {

            command.setAliases(
                    java.util.Collections.singletonList("sf")
            );
        }
    }

    // -------------------------------------------------------------------------
    // Milestones
    // -------------------------------------------------------------------------

    private void onMilestone(
            java.util.UUID uuid,
            String key,
            String message
    ) {

        if (discordIntegration != null) {
            discordIntegration.announceMilestone(message);
        }

        if (getConfig().getBoolean("milestones.broadcast", true)) {

            final String formatted =
                    MessageService.color("&6[StatFYR] &f" + message);

            SchedulerCompat.runGlobalSync(
                    this,
                    () -> Bukkit.broadcastMessage(formatted)
            );
        }
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

        // Commands are dispatched by StatfyrCommand; this is a fallback.
        return false;
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

    public DashboardConfig getDashboardConfig() {
        return dashboardConfig;
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

    public AnalyticsManager getAnalytics() {
        return analytics;
    }

    public NetworkRegistry getNetworkRegistry() {
        return networkRegistry;
    }

    public MessageService getMessages() {
        return messages;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

    public boolean isPlaceholderApiPresent() {

        return getServer()
                .getPluginManager()
                .getPlugin("PlaceholderAPI") != null;
    }

    public boolean isVaultPresent() {

        return getServer()
                .getPluginManager()
                .getPlugin("Vault") != null;
    }
}
