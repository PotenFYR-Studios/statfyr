package in.potenfyr.statfyr.config;

import in.potenfyr.statfyr.Statfyr;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and validates {@code dashboard.yml}, the dedicated configuration file
 * for the web dashboard.
 *
 * <p>The dashboard is intentionally configured separately from the API config
 * so server owners can rebrand and restructure the page without touching the
 * security-sensitive settings.
 */
public final class DashboardConfig {

    private final Statfyr plugin;

    private File configFile;

    private volatile boolean enabled;

    private volatile String title = "StatFYR";

    private volatile String subtitle = "Server Analytics";

    private volatile String theme = "dark";

    private volatile String accent = "#8b5cf6";

    private volatile int refreshSeconds = 30;

    private volatile String apiBase = "";

    private volatile boolean embedApiKey = true;

    private volatile String leaderboardMetric = "kills";

    private volatile String leaderboardPeriod = "all_time";

    /**
     * Module name ("overview", "charts", ...) -> enabled. Defaults live here
     * so unknown or missing module keys resolve to {@code true}.
     */
    private final Map<String, Boolean> modules =
            new ConcurrentHashMap<>();

    public DashboardConfig(Statfyr plugin) {

        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    /**
     * Creates {@code dashboard.yml} on first run and loads every value.
     */
    public void load() {

        configFile = new File(
                plugin.getDataFolder(),
                "dashboard.yml"
        );

        if (!configFile.exists()) {

            try {

                plugin.saveResource("dashboard.yml", false);

            } catch (Exception exception) {

                plugin.getLogger().warning(
                        "Could not create dashboard.yml: "
                                + exception.getMessage()
                );
            }
        }

        YamlConfiguration yaml =
                YamlConfiguration.loadConfiguration(configFile);

        YamlConfiguration defaults =
                YamlConfiguration.loadConfiguration(
                        new InputStreamReader(
                                plugin.getResource("dashboard.yml")
                        )
                );

        enabled =
                yaml.getBoolean("enabled", false);

        title =
                sanitize(yaml.getString("title", defaults.getString("title", "StatFYR")));

        subtitle =
                sanitize(yaml.getString("subtitle", defaults.getString("subtitle", "Server Analytics")));

        theme =
                "light".equalsIgnoreCase(
                        yaml.getString("theme", "dark")
                ) ? "light" : "dark";

        accent =
                sanitizeColor(
                        yaml.getString("accent", "#8b5cf6"),
                        "#8b5cf6"
                );

        refreshSeconds =
                Math.max(
                        5,
                        yaml.getInt("refresh-seconds", 30)
                );

        String base =
                yaml.getString("api-base", "");

        apiBase =
                base == null ? "" : base.trim();

        embedApiKey =
                yaml.getBoolean("embed-api-key", true);

        leaderboardMetric =
                sanitize(
                        yaml.getString(
                                "leaderboard.metric",
                                defaults.getString("leaderboard.metric", "kills")
                        )
                );

        leaderboardPeriod =
                sanitize(
                        yaml.getString(
                                "leaderboard.period",
                                defaults.getString("leaderboard.period", "all_time")
                        )
                );

        modules.clear();

        for (String key : defaults.getConfigurationSection("modules")
                .getKeys(false)) {

            modules.put(
                    key,
                    yaml.getBoolean(
                            "modules." + key,
                            true
                    )
            );
        }

        plugin.getLogger().info(
                "Dashboard config loaded (enabled: " + enabled + ")."
        );
    }

    // -------------------------------------------------------------------------
    // Sanitization
    // -------------------------------------------------------------------------
    //
    // Every value below ends up inside the served HTML page, so angle
    // brackets and quotes are stripped to keep the page injection-free even
    // when dashboard.yml is edited carelessly.

    private static String sanitize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("<", "")
                .replace(">", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("\\", "")
                .trim();
    }

    private static String sanitizeColor(String value, String fallback) {

        String cleaned =
                sanitize(value);

        if (!cleaned.matches("^#[0-9a-fA-F]{3,8}$")) {

            // Also accept a bare hex body without the leading hash.
            if (cleaned.matches("^[0-9a-fA-F]{3,8}$")) {
                return "#" + cleaned;
            }

            return fallback;
        }

        return cleaned;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public boolean isEnabled() {

        return enabled;
    }

    public String getTitle() {

        return title;
    }

    public String getSubtitle() {

        return subtitle;
    }

    public String getTheme() {

        return theme.toLowerCase(Locale.ROOT);
    }

    public String getAccent() {

        return accent;
    }

    public int getRefreshSeconds() {

        return refreshSeconds;
    }

    public String getApiBase() {

        return apiBase;
    }

    public boolean isEmbedApiKey() {

        return embedApiKey;
    }

    public String getLeaderboardMetric() {

        return leaderboardMetric;
    }

    public String getLeaderboardPeriod() {

        return leaderboardPeriod;
    }

    /**
     * @param name module name from the {@code modules} section
     * @return whether the module should render (defaults to {@code true})
     */
    public boolean isModuleEnabled(String name) {

        Boolean value =
                modules.get(name);

        return value == null || value;
    }
}
