package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the optional, API-first web dashboard at {@code /dashboard}.
 *
 * <p>The dashboard is a single self-contained HTML page that talks to the same
 * public REST API as every other client — there is no separate analytics
 * implementation. It is disabled unless {@code integrations.dashboard.enabled}
 * is {@code true}.
 */
public final class DashboardHandler implements HttpHandler {

    private final Statfyr plugin;
    private final byte[] page;

    public DashboardHandler(Statfyr plugin) {

        this.plugin = plugin;
        this.page = loadPage();
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        byte[] body =
                page;

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
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
