package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.PeerReport;
import in.potenfyr.statfyr.storage.JsonIO;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;
import in.potenfyr.statfyr.util.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * {@code POST /api/network/report} — accepts a {@link PeerReport} from another
 * StatFYR instance so a hub can aggregate the whole network.
 *
 * <p>Enabled with {@code network.accept-reports}. When{" "}
 * {@code network.report-key} is set, the sender must present it in the{" "}
 * {@code X-StatFYR-Key} header.
 */
public final class NetworkReportHandler implements HttpHandler {

    /**
     * Hard cap on accepted report bodies (512 KB). Reports are tiny JSON
     * documents; anything larger is hostile.
     */
    private static final int MAX_BODY_BYTES = 512 * 1024;

    private final Statfyr plugin;

    public NetworkReportHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {

            ResponseUtil.sendMethodNotAllowed(exchange);
            return;
        }

        String requiredKey =
                plugin.getConfig().getString("network.report-key", "");

        if (!Text.isBlank(requiredKey)) {

            String provided =
                    exchange.getRequestHeaders()
                            .getFirst("X-StatFYR-Key");

            byte[] providedBytes =
                    provided == null
                            ? new byte[0]
                            : provided.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8);

            byte[] expectedBytes =
                    requiredKey.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8);

            if (!java.security.MessageDigest.isEqual(
                    providedBytes,
                    expectedBytes
            )) {

                ResponseUtil.sendUnauthorized(exchange);
                return;
            }
        }

        try {

            String body =
                    readBody(exchange);

            PeerReport report =
                    JsonIO.fromJson(body, PeerReport.class);

            if (report == null
                    || Text.isBlank(report.serverId)) {

                ResponseUtil.sendBadRequest(
                        exchange,
                        "serverId is required"
                );

                return;
            }

            plugin.getNetworkRegistry().report(report);

            ResponseUtil.sendJson(
                    exchange,
                    200,
                    new JsonBuilder()
                            .add("ok", true)
                            .add("server_id", report.serverId)
                            .add(
                                    "peers",
                                    plugin.getNetworkRegistry().size()
                            )
                            .build()
            );

        } catch (Exception exception) {

            ResponseUtil.sendBadRequest(
                    exchange,
                    "Invalid report body"
            );
        }
    }

    private static String readBody(HttpExchange exchange)
            throws IOException {

        try (InputStream input = exchange.getRequestBody()) {

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            byte[] chunk =
                    new byte[4096];

            int read;

            while ((read = input.read(chunk)) != -1) {

                buffer.write(chunk, 0, read);

                if (buffer.size() > MAX_BODY_BYTES) {

                    throw new IOException(
                            "Report body exceeds limit"
                    );
                }
            }

            return new String(
                    buffer.toByteArray(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
