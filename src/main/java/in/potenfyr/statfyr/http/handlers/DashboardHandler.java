package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.config.DashboardConfig;
import in.potenfyr.statfyr.util.JsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the optional, API-first web dashboard at {@code /dashboard}.
 *
 * <p>The dashboard is a single self-contained HTML page that talks to the same
 * public REST API as every other client. Its appearance and module layout come
 * from {@code dashboard.yml}; that configuration (and, when explicitly allowed,
 * the API key) is injected into the page here so visitors are never asked to
 * type credentials into the browser.
 *
 * <p>Disabled unless {@code dashboard.enabled} is {@code true} in
 * {@code dashboard.yml}.
 */
public final class DashboardHandler implements HttpHandler {

    private final Statfyr plugin;
    private final byte[] template;

    public DashboardHandler(Statfyr plugin) {

        this.plugin = plugin;
        this.template = loadPage();
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        String configJson =
                buildConfigJson();

        // The template contains a single ${STATFYR_CONFIG} placeholder inside
        // a script tag; replace it with the server-generated configuration.
        String html =
                new String(template, StandardCharsets.UTF_8)
                        .replace("${STATFYR_CONFIG}", configJson);

        byte[] body =
                html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "X-Content-Type-Options",
                "nosniff"
        );

        exchange.getResponseHeaders().set(
                "Cache-Control",
                "no-cache, no-store, must-revalidate"
        );

        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(body);
        }
    }

    /**
     * Builds the JSON configuration object embedded into the page.
     *
     * <p>Every string value comes from {@link DashboardConfig}, which strips
     * quotes and angle brackets at load time, so the emitted script block is
     * safe even for a hand-edited dashboard.yml.
     */
    private String buildConfigJson() {

        DashboardConfig config =
                plugin.getDashboardConfig();

        String apiKey = "";

        if (config != null && config.isEmbedApiKey()) {

            ConfigManager security =
                    plugin.getConfigManager();

            // Only embed when API authentication is actually active;
            // otherwise the page needs no key at all.
            if (security != null && security.isAuthEnabled()) {

                apiKey = security.getApiKey();
            }
        }

        String theme =
                config == null ? "dark" : config.getTheme();

        String accent =
                config == null ? "#8b5cf6" : config.getAccent();

        String title =
                config == null ? "StatFYR" : config.getTitle();

        String subtitle =
                config == null ? "Server Analytics" : config.getSubtitle();

        int refreshSeconds =
                config == null ? 30 : config.getRefreshSeconds();

        String apiBase =
                config == null ? "" : config.getApiBase();

        String metric =
                config == null ? "kills" : config.getLeaderboardMetric();

        String period =
                config == null ? "all_time" : config.getLeaderboardPeriod();

        return new JsonBuilder()
                .add("title", title)
                .add("subtitle", subtitle)
                .add("theme", theme)
                .add("accent", accent)
                .add("refresh_seconds", refreshSeconds)
                .add("api_base", apiBase)
                .add("api_key", apiKey)
                .add("leaderboard_metric", metric)
                .add("leaderboard_period", period)
                .add("module_podium", moduleEnabled(config, "podium"))
                .add("module_board", moduleEnabled(config, "board"))
                .add("module_lookup", moduleEnabled(config, "lookup"))
                .add("module_live", moduleEnabled(config, "live"))
                .build();
    }

    private static boolean moduleEnabled(
            DashboardConfig config,
            String name
    ) {

        return config == null || config.isModuleEnabled(name);
    }

    private byte[] loadPage() {

        try (InputStream input =
                     plugin.getResource("dashboard/index.html")) {

            if (input == null) {
                return fallback();
            }

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            byte[] chunk =
                    new byte[8192];

            int read;

            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }

            return buffer.toByteArray();

        } catch (Exception exception) {

            return fallback();
        }
    }

    private static byte[] fallback() {

        String html =
                "<!doctype html><html><head><meta charset=\"utf-8\">"
                        + "<title>StatFYR</title></head><body>"
                        + "<h1>StatFYR</h1>"
                        + "<p>Dashboard asset missing. The REST API is "
                        + "available under <code>/api</code>.</p>"
                        + "</body></html>";

        return html.getBytes(StandardCharsets.UTF_8);
    }
}
