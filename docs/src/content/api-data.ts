import type { EndpointParam } from "../components/ui";

/* All payloads below mirror the actual handler output in
   src/main/java/in/potenfyr/statfyr/http/handlers/, not invented. */

export type ApiEndpoint = {
  id: string;
  method: "GET";
  path: string;
  title: string;
  description: string;
  auth: string;
  params?: EndpointParam[];
  request: string;
  response: string;
  responseLang?: string;
  notes?: string[];
};

export const API_BASE_NOTE =
  "All routes are served from the plugin's embedded HTTP server at the host and port you configure (default 0.0.0.0:8080). Every response is application/json.";

export const ENDPOINTS: ApiEndpoint[] = [
  {
    id: "health",
    method: "GET",
    path: "/api/health",
    title: "Health check",
    description:
      "Liveness probe with server, memory, feature-flag, and executor snapshots. Ideal for uptime monitors and status pages.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/health \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "system": {
    "status": "ok",
    "timestamp": "2026-09-12T18:42:07.113Z",
    "uptime_seconds": 86412,
    "plugin_version": "1.0.0",
    "minecraft_version": "26.3",
    "server_version": "git-Paper-156",
    "bukkit_version": "26.3-R0.1-SNAPSHOT",
    "platform": "Paper 26.3",
    "legacy": false,
    "folia": false
  },
  "players": { "online": 42, "max": 100 },
  "memory": {
    "used_mb": 2048, "free_mb": 1024, "max_mb": 4096,
    "heap_used_mb": 1980, "heap_committed_mb": 3072, "heap_max_mb": 4096
  },
  "features": {
    "https_enabled": false,
    "compression_enabled": true,
    "async_enabled": true,
    "rate_limit_enabled": true,
    "api_auth_enabled": true,
    "docs_enabled": true
  },
  "executor": { "active": 2 }
}`,
  },
  {
    id: "root",
    method: "GET",
    path: "/api",
    title: "API index",
    description:
      "Minimal service descriptor. Handy as a smoke test that the HTTP server is up and which build is running.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api`,
    response: `{
  "name": "Statfyr",
  "version": "1.0.0",
  "supported_versions": "1.8.x - 26.x",
  "platform": "Paper 26.3",
  "docs": "/api/docs"
}`,
  },
  {
    id: "docs",
    method: "GET",
    path: "/api/docs",
    title: "Self-hosted route index",
    description:
      "Machine-readable index of the live API routes served by this instance. Consumed by generators and SDK scaffolding.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/docs`,
    response: `{
  "name": "Statfyr",
  "version": "1.0.0",
  "supported_versions": "1.8.x - 26.x",
  "platform": "Paper 26.3",
  "base_url": "/api",
  "endpoints": [
    { "path": "/api/health", "method": "GET" },
    { "path": "/api/players", "method": "GET" },
    { "path": "/api/player/{player}", "method": "GET" },
    { "path": "/api/player/{player}/summary", "method": "GET" },
    { "path": "/api/player/{player}/history", "method": "GET" },
    { "path": "/api/player/{player}/activity", "method": "GET" },
    { "path": "/api/player/{player}/sessions", "method": "GET" },
    { "path": "/api/leaderboard/{type}", "method": "GET" },
    { "path": "/api/server/summary", "method": "GET" },
    { "path": "/api/network", "method": "GET" },
    { "path": "/api/custom/{metric}", "method": "GET" },
    { "path": "/api/archive/{metric}", "method": "GET" }
  ]
}`,
  },
  {
    id: "players",
    method: "GET",
    path: "/api/players",
    title: "List players",
    description:
      "Paginated roster of tracked players, sorted by name, with live online status. Supports search by name substring and an online-only filter.",
    auth: "Auth applies",
    params: [
      { name: "limit", type: "int", def: "pagination.default-limit", desc: "Results per page (1 to pagination.max-limit)." },
      { name: "page", type: "int", def: "1", desc: "Zero-based page index." },
      { name: "order", type: "enum", def: "sorting.default-order", desc: "asc or desc name ordering." },
      { name: "online_only", type: "bool", def: "false", desc: "Only include players currently online." },
      { name: "search", type: "string", def: "n/a", desc: "Case-insensitive name substring filter." },
    ],
    request: `curl "http://localhost:8080/api/players?limit=2&page=0&search=ste" \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "total": 1,
  "limit": 2,
  "page": 0,
  "offset": 0,
  "players": [
    {
      "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "name": "Notch",
      "online": true
    }
  ],
  "metadata": {
    "generated_at": "2026-09-12T18:42:07.401Z",
    "execution_time_ms": 3,
    "ascending": true,
    "online_only": false,
    "search": "ste"
  }
}`,
  },
  {
    id: "player-stats",
    method: "GET",
    path: "/api/player/{id}",
    title: "Full player statistics",
    description:
      "Complete statistic profile for a player by UUID or exact current name: computed summary metrics plus raw per-category Minecraft statistic maps. Reads live Bukkit statistics for online players and the world/stats JSON files for offline players.",
    auth: "Auth applies",
    params: [
      { name: "summary", type: "bool", def: "true", desc: "Include the computed summary object." },
      { name: "raw", type: "bool", def: "true", desc: "Include raw minecraft:* category maps." },
      { name: "categories", type: "csv", def: "all", desc: "Comma-separated filter: crafted, mined, used, broken, pickedUp, dropped, killed, killedBy." },
    ],
    request: `curl http://localhost:8080/api/player/069a79f4-44e9-4726-a5be-fca90e38aaf5 \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "name": "Notch",
  "online": true,
  "readTimestamp": "2026-09-12T18:42:07.552Z",
  "summary": {
    "playTimeTicks": 21894400,
    "playTimeFormatted": "15d 4h 50m",
    "deaths": 128,
    "playerKills": 41,
    "mobKills": 3140,
    "damageDealt": 512094,
    "damageTaken": 388712,
    "jumps": 92044,
    "distanceCm": 482034118,
    "distanceKm": 4820.34,
    "chestsOpened": 7433,
    "itemsCrafted": 21843,
    "itemsBroken": 812,
    "itemsUsed": 104552,
    "itemsDropped": 4211,
    "itemsPickedUp": 88210,
    "blocksMined": 318402
  },
  "minecraft:mined": {
    "minecraft:sand": 12044,
    "minecraft:stone": 184022
  },
  "minecraft:custom": {
    "minecraft:deaths": 128,
    "minecraft:player_kills": 41
  },
  "stats": { "...": "full unmodified Bukkit statistic dump" },
  "metadata": {
    "generated_at": "2026-09-12T18:42:07.552Z",
    "execution_time_ms": 12,
    "summary_enabled": true,
    "raw_enabled": true,
    "categories_filter": null
  }
}`,
    notes: [
      "Unknown players return 404; players with no recorded stats return HTTP 200 with {\"error\":true,\"message\":\"Stats not found\"}.",
    ],
  },
  {
    id: "player-summary",
    method: "GET",
    path: "/api/player/{id}/summary",
    title: "Player summary",
    description:
      "Aggregated, snake_case view of a player optimized for dashboards and cards. Each section (combat, movement, activity) can be toggled off to slim the payload.",
    auth: "Auth applies",
    params: [
      { name: "movement", type: "bool", def: "true", desc: "Include the movement section." },
      { name: "combat", type: "bool", def: "true", desc: "Include the combat section." },
      { name: "activity", type: "bool", def: "true", desc: "Include the activity section." },
    ],
    request: `curl http://localhost:8080/api/player/Notch/summary \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "name": "Notch",
  "online": true,
  "playtime_ticks": 21894400,
  "playtime_seconds": 1094720,
  "playtime_formatted": "15d 4h 50m",
  "combat": {
    "deaths": 128,
    "player_kills": 41,
    "mob_kills": 3140,
    "damage_dealt": 512094,
    "damage_taken": 388712
  },
  "movement": {
    "distance_walked_cm": 221034511,
    "distance_walked_m": 2210345.11,
    "distance_sprinted_cm": 150223044,
    "distance_sprinted_m": 1502230.44,
    "distance_flown_cm": 4102299,
    "distance_flown_m": 41022.99,
    "distance_swum_cm": 8823011,
    "distance_swum_m": 88230.11,
    "total_distance_cm": 482034118,
    "total_distance_m": 4820341.18,
    "total_distance_km": 4820.34,
    "jumps": 92044
  },
  "activity": {
    "chests_opened": 7433,
    "items_crafted": 21843,
    "items_broken": 812,
    "items_used": 104552,
    "items_picked_up": 88210,
    "items_dropped": 4211,
    "blocks_mined": 318402
  },
  "metadata": {
    "generated_at": "2026-09-12T18:42:07.601Z",
    "execution_time_ms": 8
  }
}`,
  },
  {
    id: "leaderboard",
    method: "GET",
    path: "/api/leaderboard/{stat}",
    title: "Leaderboard",
    description:
      "Ranked standings for a statistic. Accepts built-in metric names, dotted category.key aliases, full vanilla stat keys, and custom stats registered by other plugins.",
    auth: "Auth applies",
    params: [
      { name: "limit", type: "int", def: "pagination.default-limit", desc: "Entries per page (1 to pagination.max-limit)." },
      { name: "page", type: "int", def: "1", desc: "Page index (1-based)." },
      { name: "order", type: "enum", def: "desc", desc: "asc or desc value ordering." },
      { name: "period", type: "enum", def: "leaderboards.default-period", desc: "daily, weekly, monthly or all_time." },
      { name: "online_only", type: "bool", def: "false", desc: "Rank only players currently online." },
    ],
    request: `curl "http://localhost:8080/api/leaderboard/playtime?limit=3" \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "stat": "playtime",
  "total": 1284,
  "limit": 3,
  "offset": 0,
  "page": 0,
  "entries": [
    {
      "rank": 1,
      "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "name": "Notch",
      "online": true,
      "value": 21894400,
      "formatted": "15d 4h 50m"
    },
    {
      "rank": 2,
      "uuid": "853c80ef-3c37-49fd-aa49-938b674adae6",
      "name": "jeb_",
      "online": false,
      "value": 19020044,
      "formatted": "13d 4h 20m"
    },
    {
      "rank": 3,
      "uuid": "61699b2e-d327-4a01-9f1e-0ea8c3f06bc6",
      "name": "Dinnerbone",
      "online": false,
      "value": 15411220,
      "formatted": "10d 17h 25m"
    }
  ],
  "metadata": {
    "generated_at": "2026-09-12T18:42:07.733Z",
    "execution_time_ms": 21,
    "ascending": false,
    "online_only": false
  }
}`,
    notes: [
      "rank is offset + index, so page-based pagination keeps continuous global ranks.",
      "formatted is present for time (playtime/active_time), KDR (2 decimals) and distance (metres).",
      "KDR requires leaderboards.minimums.kdr.kills kills before a player is ranked.",
    ],
  },
  {
    id: "player-history",
    method: "GET",
    path: "/api/player/{id}/history",
    title: "Player history",
    description:
      "Historical metric snapshots captured on the configured interval, streamed from append-only storage and filterable by time range.",
    auth: "Auth applies",
    params: [
      { name: "from", type: "time", def: "30d ago", desc: "Epoch millis or relative (7d, 24h, 30m)." },
      { name: "to", type: "time", def: "now", desc: "Epoch millis." },
      { name: "limit", type: "int", def: "500", desc: "Maximum snapshots returned." },
    ],
    request: `curl "http://localhost:8080/api/player/Notch/history?from=7d&limit=100" \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "name": "Notch",
  "from": 1759700000000,
  "to": 1760300000000,
  "count": 2,
  "history": [
    {
      "timestamp": 1760100000000,
      "metrics": { "kills": 320, "deaths": 128, "playtime": 1094720 }
    }
  ]
}`,
  },
  {
    id: "player-activity",
    method: "GET",
    path: "/api/player/{id}/activity",
    title: "Player activity timeline",
    description:
      "Meaningful, low-volume activity events: joins, leaves, session summaries, kills, deaths and milestones.",
    auth: "Auth applies",
    params: [
      { name: "from", type: "time", def: "7d ago", desc: "Epoch millis or relative." },
      { name: "to", type: "time", def: "now", desc: "Epoch millis." },
      { name: "limit", type: "int", def: "200", desc: "Maximum events." },
    ],
    request: `curl "http://localhost:8080/api/player/Notch/activity?from=7d" \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "name": "Notch",
  "count": 2,
  "activity": [
    { "timestamp": 1760100000000, "type": "join", "detail": "Notch", "value": 0 },
    { "timestamp": 1760103600000, "type": "session", "value": 3600 }
  ]
}`,
  },
  {
    id: "player-sessions",
    method: "GET",
    path: "/api/player/{id}/sessions",
    title: "Player sessions",
    description:
      "Session-level statistics: totals, averages, longest session, current session, first/last seen and online state.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/player/Notch/sessions \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "player": "Notch",
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "sessions": 127,
  "total_playtime": 98234,
  "active_time": 86320,
  "afk_time": 11914,
  "average_session": 773,
  "longest_session": 24100,
  "current_session": 0,
  "first_seen": 1710000000000,
  "last_seen": 1760103600000,
  "online": false
}`,
  },
  {
    id: "server-summary",
    method: "GET",
    path: "/api/server/summary",
    title: "Server analytics",
    description:
      "Aggregated server analytics: peaks, unique players, total playtime, average session, new vs returning players and average concurrency.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/server/summary \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "online": 24,
  "peak_today": 47,
  "peak_week": 61,
  "peak_month": 74,
  "peak_all_time": 128,
  "average_concurrent": 18.4,
  "unique_players_today": 73,
  "unique_players_week": 318,
  "unique_players_month": 812,
  "total_players": 4218,
  "total_playtime_seconds": 9823400,
  "average_session_seconds": 2280,
  "sessions_per_day": 42.1,
  "new_players_today": 6,
  "returning_players_today": 67,
  "sessions_total": 18234,
  "server_id": "server-1",
  "server_name": "Survival"
}`,
  },
  {
    id: "server-history",
    method: "GET",
    path: "/api/server/history",
    title: "Server history",
    description:
      "Concurrency time series captured alongside player snapshots. Useful for graphing player counts over time.",
    auth: "Auth applies",
    params: [
      { name: "from", type: "time", def: "30d ago", desc: "Epoch millis or relative." },
      { name: "to", type: "time", def: "now", desc: "Epoch millis." },
      { name: "limit", type: "int", def: "1000", desc: "Maximum snapshots." },
    ],
    request: `curl "http://localhost:8080/api/server/history?from=24h" \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "from": 1760213600000,
  "to": 1760300000000,
  "count": 2,
  "history": [
    { "timestamp": 1760296000000, "online": 21, "unique_today": 73 },
    { "timestamp": 1760299600000, "online": 24, "unique_today": 73 }
  ]
}`,
  },
  {
    id: "server-activity",
    method: "GET",
    path: "/api/server/activity",
    title: "Activity heatmap",
    description:
      "Aggregated activity data for heatmaps: sessions by hour, by weekday, peak by day, sessions by day and new players by day.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/server/activity \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "sessions_by_hour": { "18": 412, "19": 388, "20": 501 },
  "sessions_by_weekday": { "SATURDAY": 921, "SUNDAY": 874 },
  "peak_by_day": { "2026-10-05": 47 },
  "sessions_by_day": { "2026-10-05": 128 },
  "new_players_by_day": { "2026-10-05": 6 }
}`,
  },
  {
    id: "server-retention",
    method: "GET",
    path: "/api/server/retention",
    title: "Retention",
    description:
      "D1/D7/D14/D30 retention across first-seen cohorts, with the raw cohort and retained counts.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/server/retention \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "d1": 0.61,
  "d7": 0.38,
  "d14": 0.27,
  "d30": 0.19,
  "cohorts": { "d1": 420, "d7": 398, "d14": 351, "d30": 280 },
  "retained": { "d1": 256, "d7": 151, "d14": 95, "d30": 53 }
}`,
  },
  {
    id: "server-segments",
    method: "GET",
    path: "/api/server/segments",
    title: "Player segmentation",
    description:
      "Configurable player classifications (new, active, highly_active, at_risk, inactive, churned) plus a per-player list.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/server/segments \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "segments": { "new": 312, "active": 1840, "highly_active": 220, "at_risk": 640, "inactive": 900, "churned": 306 },
  "players": [
    { "uuid": "069a79f4-...", "name": "Notch", "segment": "active" }
  ]
}`,
  },
  {
    id: "server-economy",
    method: "GET",
    path: "/api/server/economy",
    title: "Economy analytics",
    description:
      "Read-only Vault economy analytics. Returns { enabled: false } when Vault or an economy provider is absent — StatFYR never implements an economy.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/server/economy \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "enabled": true,
  "players": 4218,
  "total_in_circulation": 91827364.5,
  "average": 21765.4,
  "median": 12000.0,
  "highest": 9123456.0,
  "lowest": 0.0
}`,
  },
  {
    id: "network",
    method: "GET",
    path: "/api/network",
    title: "Network aggregation",
    description:
      "Multi-server ready aggregation shape. A single-server install returns just this server; BungeeCord/Velocity aggregators can merge multiple.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/network \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "network_players": 24,
  "network_total_players": 4218,
  "servers": [
    { "id": "server-1", "name": "Survival", "online": 24, "total_players": 4218, "sessions": 18234 }
  ]
}`,
  },
  {
    id: "custom-metrics",
    method: "GET",
    path: "/api/custom/{metric}",
    title: "Custom metrics",
    description:
      "Metrics registered by other plugins through the StatFYR metrics API. Without a metric name, lists every known custom metric.",
    auth: "Auth applies",
    params: [],
    request: `curl http://localhost:8080/api/custom/economy.balance \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "metric": "economy.balance",
  "players": {
    "069a79f4-44e9-4726-a5be-fca90e38aaf5": 12500.0
  }
}`,
  },
  {
    id: "archive",
    method: "GET",
    path: "/api/archive/{metric}",
    title: "Archived leaderboards",
    description:
      "Leaderboard results archived when a daily/weekly/monthly window rolls over, so historical standings stay accessible after counters reset.",
    auth: "Auth applies",
    params: [
      { name: "period", type: "enum", def: "all", desc: "daily, weekly or monthly." },
      { name: "from", type: "time", def: "365d ago", desc: "Epoch millis or relative." },
      { name: "to", type: "time", def: "now", desc: "Epoch millis." },
      { name: "limit", type: "int", def: "25", desc: "Maximum archives." },
    ],
    request: `curl "http://localhost:8080/api/archive/kills?period=weekly&limit=10" \\
  -H "Authorization: Bearer YOUR_API_KEY"`,
    response: `{
  "metric": "kills",
  "period": "weekly",
  "count": 1,
  "archives": [
    {
      "period": "weekly",
      "metric": "kills",
      "start": 1759536000000,
      "end": 1760140800000,
      "generated_at": "2026-10-11T00:05:00Z",
      "entries": [
        { "rank": 1, "uuid": "069a79f4-...", "name": "Notch", "value": 328, "decimal": 328.0 }
      ]
    }
  ]
}`,
  },
  {
    id: "prometheus",
    method: "GET",
    path: "/metrics",
    title: "Prometheus metrics",
    description:
      "Prometheus text exposition format (when integrations.prometheus.enabled is true). Scrape directly into Prometheus → Grafana. No Prometheus dependency required.",
    auth: "No auth (separate context)",
    params: [],
    request: `curl http://localhost:8080/metrics`,
    response: `# HELP statfyr_online_players Players currently online
# TYPE statfyr_online_players gauge
statfyr_online_players 24
# HELP statfyr_peak_players_all_time Peak concurrent players all time
# TYPE statfyr_peak_players_all_time gauge
statfyr_peak_players_all_time 128
# HELP statfyr_player_sessions Total sessions recorded
# TYPE statfyr_player_sessions gauge
statfyr_player_sessions 18234`,
    responseLang: "text",
  },
];

export const LEADERBOARD_STATS: { key: string; meaning: string }[] = [
  { key: "kills", meaning: "Player + mob kills" },
  { key: "deaths", meaning: "Death count" },
  { key: "kdr", meaning: "Kill/death ratio (requires a minimum kills threshold)" },
  { key: "player_kills", meaning: "Players killed" },
  { key: "mob_kills", meaning: "Mobs killed" },
  { key: "damage_dealt", meaning: "Damage dealt" },
  { key: "damage_taken", meaning: "Damage taken" },
  { key: "playtime", meaning: "Total play time (human-readable formatted)" },
  { key: "active_time", meaning: "Non-AFK time" },
  { key: "afk_time", meaning: "AFK time" },
  { key: "sessions", meaning: "Number of sessions" },
  { key: "blocks_mined", meaning: "Blocks mined" },
  { key: "blocks_broken", meaning: "Tools/items broken" },
  { key: "items_crafted", meaning: "Items crafted" },
  { key: "items_used", meaning: "Items used" },
  { key: "items_picked_up", meaning: "Items picked up" },
  { key: "items_dropped", meaning: "Items dropped" },
  { key: "chests_opened", meaning: "Chests and shulker boxes opened" },
  { key: "jumps", meaning: "Jumps" },
  { key: "distance_traveled", meaning: "Total distance (metres in formatted)" },
  { key: "distance_walked", meaning: "Distance walked" },
  { key: "distance_sprinted", meaning: "Distance sprinted" },
  { key: "distance_swum", meaning: "Distance swum" },
  { key: "distance_flown", meaning: "Distance flown" },
  { key: "balance", meaning: "Vault balance (when Vault is present)" },
];

export const ERROR_SHAPE = `{
  "success": false,
  "status": 429,
  "error": "Rate limit exceeded",
  "timestamp": "2026-09-12T18:42:08.101Z"
}`;

export const STATUS_CODES: { code: number; meaning: string }[] = [
  { code: 200, meaning: "Success (including the \"Stats not found\" soft-error body)." },
  { code: 400, meaning: "Malformed query parameter or path argument." },
  { code: 401, meaning: "Missing/invalid Bearer key while security.enable-api-key is on." },
  { code: 403, meaning: "Requester IP not in security.allowed-ips while the whitelist is enabled." },
  { code: 404, meaning: "Unknown route, unknown player, or unknown leaderboard stat." },
  { code: 405, meaning: "Method other than GET (all routes are GET-only)." },
  { code: 429, meaning: "Per-IP fixed-window rate limit exceeded." },
  { code: 500, meaning: "Internal handler error." },
];
