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
    "minecraft_version": "1.16.5",
    "server_version": "git-Paper-794",
    "bukkit_version": "1.16.5-R0.1-SNAPSHOT"
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
  "routes": [
    "/api/health",
    "/api/players",
    "/api/player/{uuid}",
    "/api/player/{name}",
    "/api/player/{id}/summary",
    "/api/leaderboard/{stat}",
    "/api/docs"
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
      { name: "page", type: "int", def: "1", desc: "Zero-based page index." },
      { name: "order", type: "enum", def: "sorting.default-order", desc: "asc or desc value ordering." },
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
      "formatted appears only for the playtime leaderboard (human-readable duration).",
    ],
  },
];

export const LEADERBOARD_STATS: { key: string; meaning: string }[] = [
  { key: "playtime", meaning: "Total play time (adds human-readable formatted)" },
  { key: "deaths", meaning: "Death count" },
  { key: "player_kills", meaning: "Players killed" },
  { key: "mob_kills", meaning: "Mobs killed" },
  { key: "blocks_mined", meaning: "Blocks mined" },
  { key: "items_picked_up", meaning: "Items picked up" },
  { key: "items_crafted", meaning: "Items crafted" },
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
