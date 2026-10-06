package in.potenfyr.statfyr.commands;

import in.potenfyr.statfyr.Statfyr;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * Loads and formats configurable messages from {@code messages.yml}.
 *
 * <p>Supports '&amp;' colour codes and {@code {placeholder}} substitution.
 */
public final class MessageService {

    private final Statfyr plugin;
    private FileConfiguration messages;
    private String prefix = "";

    public MessageService(Statfyr plugin) {

        this.plugin = plugin;
        reload();
    }

    /**
     * (Re)loads {@code messages.yml}, creating it from the bundled default on
     * first run.
     */
    public void reload() {

        File file =
                new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages =
                YamlConfiguration.loadConfiguration(file);

        // Fall back to bundled defaults for any missing key.
        try {

            messages.setDefaults(
                    YamlConfiguration.loadConfiguration(
                            new java.io.InputStreamReader(
                                    plugin.getResource("messages.yml"),
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    )
            );

        } catch (Exception ignored) {
        }

        prefix =
                color(
                        messages.getString(
                                "prefix",
                                "&8[&6StatFYR&8] &r"
                        )
                );
    }

    /**
     * @param key message key
     * @return raw formatted value
     */
    public String get(String key) {

        String value =
                messages.getString(key);

        return value == null ? key : color(value);
    }

    /**
     * Formats a message with placeholders.
     *
     * @param key        message key
     * @param placeholders alternating key/value pairs
     * @return formatted message
     */
    public String format(String key, String... placeholders) {

        String value =
                messages.getString(key, key);

        if (placeholders != null) {

            for (int i = 0; i + 1 < placeholders.length; i += 2) {

                value =
                        value.replace(
                                "{" + placeholders[i] + "}",
                                placeholders[i + 1]
                        );
            }
        }

        return color(value);
    }

    public void send(
            CommandSender sender,
            String key,
            String... placeholders
    ) {

        sender.sendMessage(
                prefix + format(key, placeholders)
        );
    }

    public void sendRaw(
            CommandSender sender,
            String key,
            String... placeholders
    ) {

        sender.sendMessage(format(key, placeholders));
    }

    public void sendMessage(CommandSender sender, String message) {

        sender.sendMessage(color(message));
    }

    public String prefix() {

        return prefix;
    }

    public List<String> list(String key) {

        return messages.getStringList(key);
    }

    /**
     * Translates '&amp;' colour codes.
     *
     * @param input raw input
     * @return colourised output
     */
    public static String color(String input) {

        if (input == null) {
            return "";
        }

        return input.replace('&', '\u00a7');
    }
}
