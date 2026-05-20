package in.potenfyr.statfyr.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP response utilities.
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    // -------------------------------------------------------------------------
    // JSON
    // -------------------------------------------------------------------------

    public static void sendJson(
            HttpExchange exchange,
            String jsonBody
    ) throws IOException {

        sendJson(
                exchange,
                200,
                jsonBody
        );
    }

    public static void sendJson(
            HttpExchange exchange,
            int statusCode,
            String jsonBody
    ) throws IOException {

        if (exchange == null) {
            return;
        }

        byte[] bodyBytes =
                jsonBody.getBytes(
                        StandardCharsets.UTF_8
                );

        boolean gzip =
                supportsGzip(exchange);

        // Headers
        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "X-Content-Type-Options",
                "nosniff"
        );

        exchange.getResponseHeaders().set(
                "Cache-Control",
                "no-cache, no-store, must-revalidate"
        );

        exchange.getResponseHeaders().set(
                "Pragma",
                "no-cache"
        );

        exchange.getResponseHeaders().set(
                "Expires",
                "0"
        );

        exchange.getResponseHeaders().set(
                "Server",
                "Statfyr"
        );

        // Compression
        if (gzip) {

            exchange.getResponseHeaders().set(
                    "Content-Encoding",
                    "gzip"
            );

            exchange.sendResponseHeaders(
                    statusCode,
                    0
            );

            try (
                    OutputStream outputStream =
                            new GZIPOutputStream(
                                    exchange.getResponseBody()
                            )
            ) {

                outputStream.write(bodyBytes);
            }

            return;
        }

        // Normal response
        exchange.sendResponseHeaders(
                statusCode,
                bodyBytes.length
        );

        try (
                OutputStream outputStream =
                        exchange.getResponseBody()
        ) {

            outputStream.write(bodyBytes);
        }
    }

    // -------------------------------------------------------------------------
    // Success
    // -------------------------------------------------------------------------

    public static void sendOk(
            HttpExchange exchange,
            String jsonBody
    ) throws IOException {

        sendJson(
                exchange,
                200,
                jsonBody
        );
    }

    // -------------------------------------------------------------------------
    // Errors
    // -------------------------------------------------------------------------

    public static void sendBadRequest(
            HttpExchange exchange,
            String message
    ) throws IOException {

        sendJson(
                exchange,
                400,
                JsonBuilder.error(
                        400,
                        message
                )
        );
    }

    public static void sendUnauthorized(
            HttpExchange exchange
    ) throws IOException {

        sendJson(
                exchange,
                401,
                JsonBuilder.error(
                        401,
                        "Unauthorized"
                )
        );
    }

    public static void sendForbidden(
            HttpExchange exchange,
            String message
    ) throws IOException {
        sendJson(
                exchange,
                403,
                JsonBuilder.error(message)
        );
    }

    public static void sendNotFound(
            HttpExchange exchange,
            String message
    ) throws IOException {

        sendJson(
                exchange,
                404,
                JsonBuilder.error(
                        404,
                        message
                )
        );
    }

    public static void sendMethodNotAllowed(
            HttpExchange exchange
    ) throws IOException {

        sendJson(
                exchange,
                405,
                JsonBuilder.error(
                        405,
                        "Method not allowed"
                )
        );
    }

    public static void sendTooManyRequests(
            HttpExchange exchange,
            String message
    ) throws IOException {

        sendJson(
                exchange,
                429,
                JsonBuilder.error(
                        429,
                        message
                )
        );
    }

    public static void sendInternalError(
            HttpExchange exchange,
            String message
    ) throws IOException {

        sendJson(
                exchange,
                500,
                JsonBuilder.error(
                        500,
                        message
                )
        );
    }

    // -------------------------------------------------------------------------
    // Compression
    // -------------------------------------------------------------------------

    private static boolean supportsGzip(
            HttpExchange exchange
    ) {

        String encoding =
                exchange.getRequestHeaders()
                        .getFirst(
                                "Accept-Encoding"
                        );

        return encoding != null
                && encoding.contains("gzip");
    }
}