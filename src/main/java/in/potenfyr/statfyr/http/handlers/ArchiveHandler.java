package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.Metrics;
import in.potenfyr.statfyr.http.HttpQuery;
import in.potenfyr.statfyr.storage.PeriodArchive;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Archived leaderboard results for completed period windows.
 *
 * <p>{@code GET /api/archive/{metric}?period=weekly&from=90d&limit=10}
 */
public final class ArchiveHandler implements HttpHandler {

    private final Statfyr plugin;

    public ArchiveHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        try {

            String path =
                    exchange.getRequestURI().getPath();

            String metric =
                    path.length() > "/api/archive/".length()
                            ? path.substring("/api/archive/".length())
                            : "";

            metric =
                    metric.replace("/", "");

            Map<String, String> params =
                    HttpQuery.parse(exchange.getRequestURI());

            long to =
                    HttpQuery.parseTime(
                            params,
                            "to",
                            System.currentTimeMillis()
                    );

            long from =
                    HttpQuery.parseTime(
                            params,
                            "from",
                            to - 365L * 24L * 60L * 60L * 1000L
                    );

            int limit =
                    HttpQuery.parseInt(params, "limit", 25, 1, 500);

            String period =
                    params.get("period");

            List<PeriodArchive> archives =
                    plugin.getAnalytics()
                            .archives(period, from, to, limit);

            List<Object> list =
                    new ArrayList<>();

            for (PeriodArchive archive : archives) {

                if (!metric.isEmpty()
                        && !Metrics.canonical(metric)
                        .equals(archive.metric)) {
                    continue;
                }

                Map<String, Object> entry =
                        new LinkedHashMap<>();

                entry.put("period", archive.period);
                entry.put("metric", archive.metric);
                entry.put("start", archive.start);
                entry.put("end", archive.end);
                entry.put(
                        "generated_at",
                        Instant.ofEpochMilli(archive.generatedAt).toString()
                );

                List<Object> rows =
                        new ArrayList<>();

                int rank = 1;

                for (PeriodArchive.Entry row : archive.entries) {

                    Map<String, Object> map =
                            new LinkedHashMap<>();

                    map.put("rank", rank);
                    map.put("uuid", row.uuid);
                    map.put("name", row.name);
                    map.put("value", row.value);
                    map.put("decimal", row.decimal);

                    rows.add(map);
                    rank++;
                }

                entry.put("entries", rows);

                list.add(entry);
            }

            JsonBuilder builder =
                    new JsonBuilder()
                            .add("metric", metric)
                            .add("period", period == null ? "all" : period)
                            .add("count", list.size())
                            .add("archives", list);

            ResponseUtil.sendOk(exchange, builder.build());

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "Archive endpoint failure: " + exception.getMessage()
            );

            ResponseUtil.sendInternalError(
                    exchange,
                    "Failed to read archives"
            );
        }
    }
}
