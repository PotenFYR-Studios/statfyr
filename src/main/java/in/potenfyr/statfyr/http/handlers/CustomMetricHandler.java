package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Custom metric endpoints registered by other plugins through the StatFYR
 * metrics API.
 *
 * <ul>
 *     <li>{@code GET /api/custom} — lists known custom metric names</li>
 *     <li>{@code GET /api/custom/{metric}} — all player values</li>
 * </ul>
 */
public final class CustomMetricHandler implements HttpHandler {

    private final Statfyr plugin;

    public CustomMetricHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        String path =
                exchange.getRequestURI().getPath();

        String metric =
                path.length() > "/api/custom".length()
                        ? path.substring("/api/custom".length())
                        : "";

        metric =
                metric.replace("/", "");

        if (metric.trim().isEmpty()) {

            TreeSet<String> names =
                    new TreeSet<>();

            for (PlayerProfile profile
                    : plugin.getAnalytics().allProfiles()) {

                names.addAll(profile.customMetrics.keySet());
            }

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "metrics",
                    new java.util.ArrayList<>(names)
            );

            ResponseUtil.sendOk(
                    exchange,
                    JsonBuilder.object(response)
            );

            return;
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("metric", metric);
        response.put(
                "players",
                plugin.getAnalytics().customMetricValues(metric)
        );

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(response)
        );
    }
}
