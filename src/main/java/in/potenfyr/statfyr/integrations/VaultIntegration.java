package in.potenfyr.statfyr.integrations;

import in.potenfyr.statfyr.Statfyr;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Read-only Vault economy integration.
 *
 * <p>Vault is accessed entirely through reflection, so it is never a hard
 * dependency. When Vault (or an economy provider) is absent the plugin simply
 * skips economy analytics.
 */
public final class VaultIntegration {

    private final Statfyr plugin;

    private Object economy;
    private Method getBalanceOffline;
    private Method getBalanceName;

    public VaultIntegration(Statfyr plugin) {

        this.plugin = plugin;
    }

    /**
     * @return {@code true} when a Vault economy provider was found
     */
    public boolean register() {

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        try {

            Class<?> economyClass =
                    Class.forName("net.milkbowl.vault.economy.Economy");

            Object registration =
                    Bukkit.getServicesManager()
                            .getRegistration(economyClass);

            if (registration == null) {
                return false;
            }

            Object provider =
                    registration.getClass()
                            .getMethod("getProvider")
                            .invoke(registration);

            if (provider == null) {
                return false;
            }

            this.economy = provider;

            this.getBalanceOffline =
                    findMethod(
                            provider.getClass(),
                            "getBalance",
                            OfflinePlayer.class
                    );

            this.getBalanceName =
                    findMethod(
                            provider.getClass(),
                            "getBalance",
                            String.class
                    );

            plugin.getAnalytics().setBalanceProvider(
                    this::balance
            );

            plugin.getLogger().info(
                    "Vault economy integration enabled."
            );

            return true;

        } catch (Throwable throwable) {

            plugin.getLogger().warning(
                    "Failed to hook into Vault: " + throwable.getMessage()
            );

            return false;
        }
    }

    private double balance(UUID uuid, String name) {

        if (economy == null) {
            return 0.0;
        }

        try {

            if (getBalanceOffline != null) {

                Object value =
                        getBalanceOffline.invoke(
                                economy,
                                Bukkit.getOfflinePlayer(uuid)
                        );

                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            }

            if (getBalanceName != null && name != null) {

                Object value =
                        getBalanceName.invoke(economy, name);

                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            }

        } catch (Throwable ignored) {
        }

        return 0.0;
    }

    private static Method findMethod(
            Class<?> owner,
            String name,
            Class<?> parameter
    ) {

        try {

            return owner.getMethod(name, parameter);

        } catch (Throwable ignored) {

            return null;
        }
    }
}
