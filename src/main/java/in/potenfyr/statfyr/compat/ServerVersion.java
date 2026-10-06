package in.potenfyr.statfyr.compat;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;

/**
 * Runtime Minecraft version and server-flavour detection.
 *
 * <p>Statfyr supports every server type and version from 1.8.x through 26.x.
 * Because Minecraft switched to calendar versioning in 2026 (1.21.x -&gt; 26.1,
 * 26.2, 26.3, &hellip;) version parsing has to understand both numbering
 * schemes. Everything here is reached reflectively and defensively so the
 * plugin never hard-fails on a server that lacks a newer API.
 *
 * <p>Supported server flavours include Bukkit, Spigot, Paper, Purpur and
 * Folia (plus their forks).
 */
public final class ServerVersion {

    private ServerVersion() {
    }

    /**
     * Raw Minecraft version, for example {@code "1.8.8"}, {@code "1.16.5"},
     * {@code "1.21.4"} or {@code "26.3"}.
     */
    private static final String MINECRAFT_VERSION =
            detectMinecraftVersion();

    /**
     * Parsed numeric components of {@link #MINECRAFT_VERSION}.
     */
    private static final int[] PARSED_VERSION =
            VersionMath.parse(MINECRAFT_VERSION);

    /**
     * Every version before 1.17 is considered "legacy". This covers the
     * 1.8.x–1.16.x range requested for long-term legacy support.
     */
    private static final boolean LEGACY =
            VersionMath.compare(
                    PARSED_VERSION,
                    VersionMath.parse("1.17")
            ) < 0;

    private static final boolean FOLIA =
            hasClass("io.papermc.paper.threadedregions.RegionizedServer")
                    || brandContains("folia");

    private static final boolean PAPER =
            hasClass("com.destroystokyo.paper.PaperConfig")
                    || hasClass("io.papermc.paper.configuration.Configuration")
                    || brandContains("paper");

    private static final boolean PURPUR =
            hasClass("org.purpurmc.purpur.PurpurConfig")
                    || brandContains("purpur");

    private static final String SERVER_BRAND =
            detectBrand();

    // -------------------------------------------------------------------------
    // Version
    // -------------------------------------------------------------------------

    /**
     * @return the raw Minecraft version string, never {@code null}
     */
    public static String getMinecraftVersion() {

        return MINECRAFT_VERSION;
    }

    /**
     * @return parsed numeric version components
     */
    public static int[] getParsedVersion() {

        return PARSED_VERSION.clone();
    }

    /**
     * @param major major component (for example {@code 1} or {@code 26})
     * @param minor minor component (for example {@code 16} or {@code 3})
     * @return {@code true} when the running server is at least that version
     */
    public static boolean isAtLeast(int major, int minor) {

        return VersionMath.compare(
                PARSED_VERSION,
                new int[]{major, minor}
        ) >= 0;
    }

    /**
     * @param major major component
     * @param minor minor component
     * @param patch patch component
     * @return {@code true} when the running server is at least that version
     */
    public static boolean isAtLeast(int major, int minor, int patch) {

        return VersionMath.compare(
                PARSED_VERSION,
                new int[]{major, minor, patch}
        ) >= 0;
    }

    /**
     * @return {@code true} for 1.8.x through 1.16.x (legacy support range)
     */
    public static boolean isLegacy() {

        return LEGACY;
    }

    /**
     * @return {@code true} for the pre-flattening versions 1.8.x–1.12.x,
     *         where materials are still data-value based
     */
    public static boolean isPreFlattening() {

        return VersionMath.compare(
                PARSED_VERSION,
                VersionMath.parse("1.13")
        ) < 0;
    }

    // -------------------------------------------------------------------------
    // Flavour
    // -------------------------------------------------------------------------

    /**
     * @return the human readable server brand (for example {@code "Paper"})
     */
    public static String getServerBrand() {

        return SERVER_BRAND;
    }

    /**
     * @return {@code true} when running on Folia (regionised threading)
     */
    public static boolean isFolia() {

        return FOLIA;
    }

    /**
     * @return {@code true} when running on Paper or a Paper fork
     */
    public static boolean isPaper() {

        return PAPER;
    }

    /**
     * @return {@code true} when running on Purpur
     */
    public static boolean isPurpur() {

        return PURPUR;
    }

    /**
     * @return a short {@code "Brand MC"} label used in API responses and logs
     */
    public static String getPlatformLabel() {

        String brand = SERVER_BRAND;

        if (brand == null || brand.trim().isEmpty()) {
            brand = "Bukkit";
        }

        return brand + " " + MINECRAFT_VERSION;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static String detectMinecraftVersion() {

        // Modern Paper API (1.16.5+). Absent on Spigot, so reflect.
        try {

            Method method =
                    Bukkit.class.getMethod(
                            "getMinecraftVersion"
                    );

            Object value =
                    method.invoke(null);

            if (value != null) {

                String version =
                        String.valueOf(value).trim();

                if (!version.isEmpty()) {
                    return version;
                }
            }

        } catch (Throwable ignored) {
            // Not available — fall through to Bukkit version parsing.
        }

        try {

            String bukkitVersion =
                    Bukkit.getBukkitVersion();

            if (bukkitVersion != null) {

                int dash =
                        bukkitVersion.indexOf('-');

                if (dash > 0) {
                    bukkitVersion =
                            bukkitVersion.substring(0, dash);
                }

                bukkitVersion =
                        bukkitVersion.trim();

                if (!bukkitVersion.isEmpty()) {
                    return bukkitVersion;
                }
            }

        } catch (Throwable ignored) {
            // Nothing else we can do.
        }

        return "unknown";
    }

    private static String detectBrand() {

        try {

            String name =
                    Bukkit.getName();

            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }

        } catch (Throwable ignored) {
        }

        return "Bukkit";
    }

    private static boolean brandContains(String needle) {

        try {

            String name =
                    Bukkit.getName();

            return name != null
                    && name.toLowerCase()
                    .contains(needle);

        } catch (Throwable ignored) {

            return false;
        }
    }

    private static boolean hasClass(String className) {

        try {

            Class.forName(
                    className,
                    false,
                    ServerVersion.class.getClassLoader()
            );

            return true;

        } catch (Throwable ignored) {

            return false;
        }
    }
}
