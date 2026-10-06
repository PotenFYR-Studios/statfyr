package in.potenfyr.statfyr.integrations;

import in.potenfyr.statfyr.Statfyr;
import org.bukkit.Bukkit;

/**
 * Registers the StatFYR PlaceholderAPI expansion when PlaceholderAPI is
 * installed.
 *
 * <p>The expansion class is loaded reflectively so that the plugin starts
 * normally when PlaceholderAPI is absent. PlaceholderAPI is never a hard
 * dependency.
 */
public final class PlaceholderIntegration {

    private final Statfyr plugin;

    private Object expansion;

    public PlaceholderIntegration(Statfyr plugin) {

        this.plugin = plugin;
    }

    /**
     * @return {@code true} when the expansion was registered
     */
    public boolean register() {

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }

        try {

            Class<?> type =
                    Class.forName(
                            "in.potenfyr.statfyr.integrations.StatfyrPlaceholderExpansion"
                    );

            Object instance =
                    type.getConstructor(Statfyr.class)
                            .newInstance(plugin);

            type.getMethod("register")
                    .invoke(instance);

            this.expansion = instance;

            plugin.getLogger().info(
                    "PlaceholderAPI expansion registered."
            );

            return true;

        } catch (Throwable throwable) {

            plugin.getLogger().warning(
                    "Failed to register PlaceholderAPI expansion: "
                            + throwable.getMessage()
            );

            return false;
        }
    }

    /**
     * @return {@code true} when the expansion is registered
     */
    public boolean isRegistered() {

        return expansion != null;
    }
}
