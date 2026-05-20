package in.potenfyr.statfyr.config;

import in.potenfyr.statfyr.Statfyr;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Handles everything related to Statfyr configuration management.
 *
 * <p>This class is responsible for:
 * <ul>
 *     <li>Creating the config file if it does not exist</li>
 *     <li>Validating required configuration keys</li>
 *     <li>Repairing missing keys automatically</li>
 *     <li>Loading configuration values into memory</li>
 *     <li>Backing up invalid/broken configuration files</li>
 *     <li>Providing easy getter access for other classes</li>
 * </ul>
 *
 * <p>The configuration is loaded once during startup and can also
 * be reloaded dynamically using {@link #reload()}.
 */
public final class ConfigManager {

    // =========================================================================
    // CONFIG VERSIONING
    // =========================================================================

    /**
     * Current supported configuration version.
     *
     * <p>If the user's config version does not match this version,
     * the config will be considered outdated or incompatible.
     */
    private static final int CURRENT_CONFIG_VERSION = 1;

    // =========================================================================
    // REQUIRED CONFIG PATHS
    // =========================================================================

    /**
     * All mandatory configuration paths.
     *
     * <p>If any of these keys are missing, the config is considered invalid.
     */
    private static final List<String> REQUIRED_PATHS = List.of(

            "config-version",

            "http.port",
            "http.bind-address",

            "https.enabled",

            "security.enable-api-key",
            "security.enable-rate-limit",

            "compression.enabled",

            "pagination.default-limit",

            "async.enabled"
    );

    // =========================================================================
    // CORE
    // =========================================================================

    /**
     * Main plugin instance.
     */
    private final Statfyr plugin;

    /**
     * Reference to config.yml.
     */
    private File configFile;

    // =========================================================================
    // HTTP SETTINGS
    // =========================================================================

    /**
     * HTTP server port.
     */
    private int apiPort;

    /**
     * HTTP bind address.
     */
    private String apiHost;

    /**
     * Maximum request timeout in seconds.
     */
    private int requestTimeoutSeconds;

    /**
     * Maximum request body size in KB.
     */
    private int maxRequestBodySizeKb;

    // =========================================================================
    // HTTPS SETTINGS
    // =========================================================================

    /**
     * Whether HTTPS is enabled.
     */
    private boolean httpsEnabled;

    /**
     * Path to SSL keystore.
     */
    private String keystorePath;

    /**
     * SSL keystore password.
     */
    private String keystorePassword;

    // =========================================================================
    // DEBUG SETTINGS
    // =========================================================================

    /**
     * Enables verbose debug logging.
     */
    private boolean debug;

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    /**
     * Whether API key authentication is enabled.
     */
    private boolean enableApiKey;

    /**
     * API key used for authentication.
     */
    private String apiKey;

    // =========================================================================
    // RATE LIMITING
    // =========================================================================

    /**
     * Whether rate limiting is enabled.
     */
    private boolean enableRateLimit;

    /**
     * Maximum requests allowed in the window.
     */
    private int rateLimitRequests;

    /**
     * Time window for rate limiting.
     */
    private int rateLimitWindowSeconds;

    // =========================================================================
    // COMPRESSION
    // =========================================================================

    /**
     * Whether gzip compression is enabled.
     */
    private boolean compressionEnabled;

    // =========================================================================
    // ASYNC SETTINGS
    // =========================================================================

    /**
     * Whether async processing is enabled.
     */
    private boolean asyncEnabled;

    // =========================================================================
    // DOCUMENTATION SETTINGS
    // =========================================================================

    /**
     * Whether API documentation routes are enabled.
     */
    private boolean docsEnabled;

    // =========================================================================
    // QUERY SETTINGS
    // =========================================================================

    /**
     * Maximum query limit allowed.
     */
    private int maxQueryLimit;

    // =========================================================================
    // PAGINATION SETTINGS
    // =========================================================================

    /**
     * Default page size.
     */
    private int defaultPageLimit;

    /**
     * Maximum page size allowed.
     */
    private int maxPageLimit;

    // =========================================================================
    // SORTING SETTINGS
    // =========================================================================

    /**
     * Default sorting order.
     *
     * <p>Usually "asc" or "desc".
     */
    private String defaultSortOrder;

    // =========================================================================
    // CORS SETTINGS
    // =========================================================================

    /**
     * Whether CORS support is enabled.
     */
    private boolean enableCors;

    /**
     * Allowed CORS origins.
     */
    private List<String> allowedOrigins;

    // =========================================================================
    // IP WHITELIST SETTINGS
    // =========================================================================

    /**
     * Whether IP whitelist protection is enabled.
     */
    private boolean enableIpWhitelist;

    /**
     * List of allowed IP addresses.
     */
    private List<String> allowedIps;

    // =========================================================================
    // CACHE SETTINGS
    // =========================================================================

    /**
     * Cache lifetime in seconds.
     */
    private int cacheTtlSeconds;

    /**
     * Cache refresh interval in seconds.
     */
    private int cacheRefreshSeconds;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Creates a new ConfigManager instance.
     *
     * @param plugin Main plugin instance
     */
    public ConfigManager(Statfyr plugin) {

        this.plugin = plugin;

        initialize();
    }

    // =========================================================================
    // RELOAD
    // =========================================================================

    /**
     * Reloads the configuration from disk.
     *
     * <p>This re-validates and re-loads all configuration values.
     */
    public void reload() {

        initialize();

        plugin.getLogger().info(
                "Configuration reloaded."
        );
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    /**
     * Initializes the configuration system.
     *
     * <p>This method:
     * <ul>
     *     <li>Creates config.yml if missing</li>
     *     <li>Validates the configuration</li>
     *     <li>Repairs missing keys</li>
     *     <li>Backs up broken configs</li>
     *     <li>Loads all values into memory</li>
     * </ul>
     */
    private void initialize() {

        configFile = new File(
                plugin.getDataFolder(),
                "config.yml"
        );

        // Create plugin folder if it does not exist
        if (!plugin.getDataFolder().exists()) {
            if (plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().info(
                        "Created plugin data folder."
                );
            }
        }

        boolean regenerate = false;

        // If config file does not exist, create a new one
        if (!configFile.exists()) {

            regenerate = true;

        } else {

            try {

                // Validate existing config
                validateConfig(configFile);

                // Automatically repair missing keys
                repairMissingKeys();

            } catch (Exception exception) {

                plugin.getLogger().severe(
                        "Invalid config.yml detected."
                );

                // Backup broken config before replacing
                backupBrokenConfig();

                regenerate = true;
            }
        }

        // Generate fresh config if needed
        if (regenerate) {

            plugin.saveResource(
                    "config.yml",
                    true
            );

            plugin.getLogger().info(
                    "Generated new config.yml"
            );
        }

        // Reload Bukkit config instance
        plugin.reloadConfig();

        // Load all values
        load();
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    /**
     * Validates the configuration file.
     *
     * @param file Config file to validate
     * @throws Exception if validation fails
     */
    private void validateConfig(File file)
            throws Exception {

        YamlConfiguration yaml =
                new YamlConfiguration();

        yaml.load(file);

        int version =
                yaml.getInt(
                        "config-version",
                        -1
                );

        // Validate config version
        if (version != CURRENT_CONFIG_VERSION) {

            throw new IllegalStateException(
                    "Unsupported config version"
            );
        }

        // Validate required keys
        for (String path : REQUIRED_PATHS) {

            validatePath(
                    yaml,
                    path
            );
        }
    }

    /**
     * Ensures a configuration path exists.
     *
     * @param config Configuration object
     * @param path   Path to validate
     */
    private void validatePath(
            FileConfiguration config,
            String path
    ) {

        if (!config.contains(path)) {

            throw new IllegalStateException(
                    "Missing config path: " + path
            );
        }
    }

    // =========================================================================
    // AUTO REPAIR
    // =========================================================================

    /**
     * Repairs missing config keys automatically.
     *
     * <p>Missing keys are copied from the default bundled config.yml.
     */
    private void repairMissingKeys() {

        FileConfiguration current =
                YamlConfiguration.loadConfiguration(
                        configFile
                );


        //TODO: Will have to see what to do
        YamlConfiguration defaults =
                YamlConfiguration.loadConfiguration(
                        new InputStreamReader(
                                plugin.getResource(
                                        "config.yml"
                                )
                        )
                );

        boolean changed = false;

        // Check every key from defaults
        for (String key : defaults.getKeys(true)) {

            if (!current.contains(key)) {

                current.set(
                        key,
                        defaults.get(key)
                );

                changed = true;

                plugin.getLogger().warning(
                        "Added missing config key: " + key
                );
            }
        }

        // Save repaired config
        if (changed) {

            atomicSave(current);

            plugin.getLogger().info(
                    "Config repaired automatically."
            );
        }
    }

    // =========================================================================
    // CONFIG BACKUP
    // =========================================================================

    /**
     * Creates a backup of a broken configuration file.
     *
     * <p>The backup file includes a timestamp to avoid overwriting.
     */
    private void backupBrokenConfig() {

        try {

            String timestamp =
                    new SimpleDateFormat(
                            "yyyy-MM-dd_HH-mm-ss"
                    ).format(
                            new Date()
                    );

            File backup =
                    new File(
                            plugin.getDataFolder(),
                            "config-old-" +
                                    timestamp +
                                    ".yml"
                    );

            Files.move(
                    configFile.toPath(),
                    backup.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            plugin.getLogger().warning(
                    "Backed up broken config to " +
                            backup.getName()
            );

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Failed to backup config."
            );

            plugin.getLogger().severe(
                    "Please check the plugin folder for a file named config.yml and rename it manually to prevent data loss."
            );

            if (isDebug()) {
                plugin.getLogger().severe(
                        "Exception details:" + exception
                );

            }

        }
    }

    // =========================================================================
    // ATOMIC SAVE
    // =========================================================================

    /**
     * Saves configuration safely using atomic file replacement.
     *
     * <p>This reduces the chance of corruption if the server crashes
     * during saving.
     *
     * @param configuration Configuration to save
     */
    private void atomicSave(
            FileConfiguration configuration
    ) {

        File tempFile =
                new File(
                        plugin.getDataFolder(),
                        "config-temp.yml"
                );

        try {

            configuration.save(tempFile);

            Files.move(
                    tempFile.toPath(),
                    configFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Failed to save config atomically."
            );

            if (isDebug()) {
                plugin.getLogger().severe(
                        "Exception details:" + exception
                );
            }

        }
    }

    // =========================================================================
    // LOAD CONFIG VALUES
    // =========================================================================

    /**
     * Loads all configuration values into memory.
     *
     * <p>This method also sanitizes and clamps values where needed.
     */
    private void load() {

        FileConfiguration config =
                plugin.getConfig();

        // ---------------------------------------------------------------------
        // HTTP
        // ---------------------------------------------------------------------

        apiPort =
                Math.max(
                        1,
                        Math.min(
                                65535,
                                config.getInt(
                                        "http.port",
                                        8080
                                )
                        )
                );

        apiHost =
                config.getString(
                        "http.bind-address",
                        "0.0.0.0"
                );

        requestTimeoutSeconds =
                Math.max(
                        1,
                        config.getInt(
                                "http.request-timeout-seconds",
                                15
                        )
                );

        maxRequestBodySizeKb =
                Math.max(
                        64,
                        config.getInt(
                                "http.max-request-body-kb",
                                512
                        )
                );

        // ---------------------------------------------------------------------
        // HTTPS
        // ---------------------------------------------------------------------

        httpsEnabled =
                config.getBoolean(
                        "https.enabled",
                        false
                );

        keystorePath =
                config.getString(
                        "https.keystore-path",
                        "plugins/statfyr/keystore.jks"
                );

        /*
         * Environment variable takes priority over config value.
         * This is safer because secrets should not be stored in plain text.
         */
        keystorePassword =
                System.getenv()
                        .getOrDefault(
                                "STATFYR_KEYSTORE_PASSWORD",
                                config.getString(
                                        "https.keystore-password",
                                        "changeit"
                                )
                        );

        // ---------------------------------------------------------------------
        // DEBUG
        // ---------------------------------------------------------------------

        debug =
                config.getBoolean(
                        "debug",
                        false
                );

        // ---------------------------------------------------------------------
        // AUTHENTICATION
        // ---------------------------------------------------------------------

        enableApiKey =
                config.getBoolean(
                        "security.enable-api-key",
                        false
                );

        /*
         * Environment variable is preferred for security.
         */
        apiKey =
                System.getenv()
                        .getOrDefault(
                                "STATFYR_API_KEY",
                                config.getString(
                                        "security.api-key",
                                        ""
                                )
                        );

        // ---------------------------------------------------------------------
        // RATE LIMITING
        // ---------------------------------------------------------------------

        enableRateLimit =
                config.getBoolean(
                        "security.enable-rate-limit",
                        true
                );

        rateLimitRequests =
                Math.max(
                        1,
                        config.getInt(
                                "security.rate-limit-requests",
                                120
                        )
                );

        rateLimitWindowSeconds =
                Math.max(
                        1,
                        config.getInt(
                                "security.rate-limit-window-seconds",
                                60
                        )
                );

        // ---------------------------------------------------------------------
        // COMPRESSION
        // ---------------------------------------------------------------------

        compressionEnabled =
                config.getBoolean(
                        "compression.enabled",
                        true
                );

        // ---------------------------------------------------------------------
        // ASYNC
        // ---------------------------------------------------------------------

        asyncEnabled =
                config.getBoolean(
                        "async.enabled",
                        true
                );

        // ---------------------------------------------------------------------
        // DOCS
        // ---------------------------------------------------------------------

        docsEnabled =
                config.getBoolean(
                        "docs.enabled",
                        true
                );

        // ---------------------------------------------------------------------
        // QUERY LIMITS
        // ---------------------------------------------------------------------

        maxQueryLimit =
                Math.max(
                        1,
                        config.getInt(
                                "query.max-limit",
                                100
                        )
                );

        // ---------------------------------------------------------------------
        // PAGINATION
        // ---------------------------------------------------------------------

        defaultPageLimit =
                Math.max(
                        1,
                        config.getInt(
                                "pagination.default-limit",
                                25
                        )
                );

        maxPageLimit =
                Math.max(
                        defaultPageLimit,
                        config.getInt(
                                "pagination.max-limit",
                                100
                        )
                );

        // ---------------------------------------------------------------------
        // SORTING
        // ---------------------------------------------------------------------

        defaultSortOrder =
                config.getString(
                        "sorting.default-order",
                        "desc"
                );

        // ---------------------------------------------------------------------
        // CORS
        // ---------------------------------------------------------------------

        enableCors =
                config.getBoolean(
                        "security.enable-cors",
                        true
                );

        allowedOrigins =
                new ArrayList<>(
                        config.getStringList(
                                "security.allowed-origins"
                        )
                );

        /*
         * If no origins are defined,
         * allow all origins by default.
         */
        if (allowedOrigins.isEmpty()) {
            allowedOrigins.add("*");
        }

        // ---------------------------------------------------------------------
        // IP WHITELIST
        // ---------------------------------------------------------------------

        enableIpWhitelist =
                config.getBoolean(
                        "security.enable-ip-whitelist",
                        false
                );

        allowedIps =
                new ArrayList<>(
                        config.getStringList(
                                "security.allowed-ips"
                        )
                );

        // ---------------------------------------------------------------------
        // CACHE
        // ---------------------------------------------------------------------

        cacheTtlSeconds =
                Math.max(
                        1,
                        config.getInt(
                                "cache.ttl-seconds",
                                60
                        )
                );

        cacheRefreshSeconds =
                Math.max(
                        1,
                        config.getInt(
                                "cache.refresh-seconds",
                                10
                        )
                );
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /**
     * @return HTTP server port
     */
    public int getPort() {
        return apiPort;
    }

    /**
     * @return HTTP bind address
     */
    public String getBindAddress() {
        return apiHost;
    }

    /**
     * FUTURE: Not in use currently, but can be used in the future to set request timeouts.
     *
     * @return Request timeout in seconds
     */
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    /**
     * FUTURE: Not in use currently, but can be used in the future to limit the size of incoming requests.
     *
     * @return Maximum request body size in KB
     */
    public int getMaxRequestBodySizeKb() {
        return maxRequestBodySizeKb;
    }

    /**
     * @return true if HTTPS is enabled
     */
    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    /**
     * @return Path to SSL keystore
     */
    public String getKeystorePath() {
        return keystorePath;
    }

    /**
     * @return SSL keystore password
     */
    public String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * @return true if debug mode is enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @return true if API key authentication is enabled
     */
    public boolean isEnableApiKey() {
        return enableApiKey;
    }

    /**
     * @return Configured API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Checks whether authentication is actually active.
     *
     * <p>Authentication is only considered enabled if:
     * <ul>
     *     <li>API keys are enabled</li>
     *     <li>The API key is not null</li>
     *     <li>The API key is not blank</li>
     * </ul>
     *
     * @return true if authentication is active
     */
    public boolean isAuthEnabled() {

        return enableApiKey
                && apiKey != null
                && !apiKey.isBlank();
    }

    /**
     * @return true if rate limiting is enabled
     */
    public boolean isEnableRateLimit() {
        return enableRateLimit;
    }

    /**
     * @return Maximum allowed requests
     */
    public int getRateLimitRequests() {
        return rateLimitRequests;
    }

    /**
     * @return Rate limit time window in seconds
     */
    public int getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    /**
     * @return true if compression is enabled
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * @return true if async processing is enabled
     */
    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    /**
     * @return true if API docs are enabled
     */
    public boolean isDocsEnabled() {
        return docsEnabled;
    }

    /**
     * @return Maximum query limit
     */
    public int getMaxQueryLimit() {
        return maxQueryLimit;
    }

    /**
     * @return Default pagination size
     */
    public int getDefaultPageLimit() {
        return defaultPageLimit;
    }

    /**
     * @return Maximum pagination size
     */
    public int getMaxPageLimit() {
        return maxPageLimit;
    }

    /**
     * @return Default sorting order
     */
    public String getDefaultSortOrder() {
        return defaultSortOrder;
    }

    /**
     * @return true if CORS is enabled
     */
    public boolean isEnableCors() {
        return enableCors;
    }

    /**
     * Returns allowed CORS origins.
     *
     * <p>The returned list is immutable to prevent accidental modification.
     *
     * @return Allowed origins list
     */
    public List<String> getAllowedOrigins() {

        return Collections.unmodifiableList(
                allowedOrigins
        );
    }

    /**
     * @return true if IP whitelist is enabled
     */
    public boolean isEnableIpWhitelist() {
        return enableIpWhitelist;
    }

    /**
     * Returns allowed IP addresses.
     *
     * @return Immutable list of allowed IPs
     */
    public List<String> getAllowedIps() {

        return Collections.unmodifiableList(
                allowedIps
        );
    }

    /**
     * @return Cache TTL in seconds
     */
    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    /**
     * @return Cache refresh interval in seconds
     */
    public int getCacheRefreshSeconds() {
        return cacheRefreshSeconds;
    }

    // =========================================================================
    // DEBUG DUMP
    // =========================================================================

    /**
     * Prints important configuration values to the console.
     *
     * <p>Useful for debugging startup issues.
     */
    public void dumpConfigState() {

        plugin.getLogger().info(
                "===== Statfyr Config ====="
        );

        plugin.getLogger().info(
                "HTTP: " +
                        apiHost +
                        ":" +
                        apiPort
        );

        plugin.getLogger().info(
                "HTTPS: " +
                        httpsEnabled
        );

        plugin.getLogger().info(
                "Compression: " +
                        compressionEnabled
        );

        plugin.getLogger().info(
                "Async: " +
                        asyncEnabled
        );

        plugin.getLogger().info(
                "API Auth: " +
                        enableApiKey
        );

        plugin.getLogger().info(
                "Rate Limit: " +
                        enableRateLimit
        );

        plugin.getLogger().info(
                "Pagination Default: " +
                        defaultPageLimit
        );

        plugin.getLogger().info(
                "Pagination Max: " +
                        maxPageLimit
        );

        plugin.getLogger().info(
                "Docs Enabled: " +
                        docsEnabled
        );
    }
}