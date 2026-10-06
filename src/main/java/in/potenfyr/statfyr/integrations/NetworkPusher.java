package in.potenfyr.statfyr.integrations;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.PeerReport;
import in.potenfyr.statfyr.analytics.ServerStats;
import in.potenfyr.statfyr.compat.SchedulerCompat;
import in.potenfyr.statfyr.storage.JsonIO;
import in.potenfyr.statfyr.util.Text;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Optional network client: periodically pushes this server's summary to a
 * StatFYR hub so {@code /api/network} can aggregate the whole network.
 *
 * <p>Disabled unless {@code network.hub-url} is configured. Uses plain{" "}
 * {@link HttpURLConnection}; no dependency is bundled.
 */
public final class NetworkPusher {

    private final Statfyr plugin;

    private volatile boolean enabled;
    private volatile String hubUrl = "";
    private volatile String reportKey = "";
    private volatile int intervalSeconds = 60;

    private boolean started;

    public NetworkPusher(Statfyr plugin) {

        this.plugin = plugin;
    }

    /**
     * Re-reads configuration. Safe to call on every reload.
     */
    public void reload() {

        String url =
                plugin.getConfig().getString("network.hub-url", "");

        this.hubUrl = url == null ? "" : url.trim();

        String key =
                plugin.getConfig().getString("network.report-key", "");

        this.reportKey = key == null ? "" : key.trim();

        this.intervalSeconds =
                Math.max(
                        15,
                        plugin.getConfig().getInt(
                                "network.report-interval-seconds",
                                60
                        )
                );

        this.enabled =
                !Text.isBlank(hubUrl);

        plugin.getNetworkRegistry().setTtlSeconds(
                plugin.getConfig().getLong(
                        "network.report-ttl-seconds",
                        180L
                )
        );
    }

    /**
     * Starts the push task. Call once, after the analytics engine.
     */
    public void start() {

        reload();

        if (started) {
            return;
        }

        started = true;

        long ticks =
                (long) intervalSeconds * 20L;

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::push,
                ticks,
                ticks
        );
    }

    private void push() {

        if (!enabled || Text.isBlank(hubUrl)) {
            return;
        }

        ServerStats stats =
                plugin.getAnalytics().serverStats();

        PeerReport report =
                new PeerReport(
                        plugin.getAnalytics().serverId(),
                        plugin.getAnalytics().serverName(),
                        stats.online,
                        stats.totalPlayers,
                        stats.sessionsTotal,
                        stats.peakAllTime
                );

        final String body =
                JsonIO.toJson(report);

        final String url =
                hubUrl.replaceAll("/+$", "") + "/api/network/report";

        try {

            plugin.getExecutorService().execute(
                    () -> post(url, body)
            );

        } catch (Throwable ignored) {
        }
    }

    private void post(String url, String body) {

        HttpURLConnection connection = null;

        try {

            connection =
                    (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );
            connection.setRequestProperty("User-Agent", "StatFYR");

            if (!Text.isBlank(reportKey)) {
                connection.setRequestProperty(
                        "X-StatFYR-Key",
                        reportKey
                );
            }

            try (OutputStream output =
                         connection.getOutputStream()) {

                output.write(
                        body.getBytes(StandardCharsets.UTF_8)
                );
            }

            int status =
                    connection.getResponseCode();

            if (status >= 300) {

                plugin.getLogger().fine(
                        "Network hub responded with HTTP " + status
                );
            }

        } catch (Throwable throwable) {

            plugin.getLogger().fine(
                    "Failed to push to network hub: "
                            + throwable.getMessage()
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
