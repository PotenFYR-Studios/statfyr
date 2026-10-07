# Statfyr API Documentation

## Base URL

```http
http://localhost:8080/api
```

HTTPS:

```http
https://localhost:8080/api
```

---

# Authentication

If API authentication is enabled:

```http
Authorization: Bearer YOUR_API_KEY
```

---

# Response Format

All responses are JSON.

Example:

```json
{
  "success": true
}
```

Error Example:

```json
{
  "success": false,
  "status": 404,
  "error": "Player not found"
}
```

---

# Endpoints

---

# Health Endpoint

## GET `/api/health`

Returns API and server health information.

---

## Example Request

```http
GET /api/health
```

---

## Example Response

```json
{
  "status": "ok",
  "plugin_version": "1.0.0",
  "server_version": "git-Paper-790",
  "bukkit_version": "1.16.5-R0.1-SNAPSHOT",
  "online_players": 3,
  "minecraft_version": "1.16.5",
  "uptime_seconds": 5400
}
```

---

# Players Endpoint

## GET `/api/players`

Returns all known players.

---

# Query Parameters

| Parameter   | Type    | Description              |
|-------------|---------|--------------------------|
| limit       | integer | Maximum players returned |
| page        | integer | Page number              |
| order       | string  | asc / desc               |
| online_only | boolean | Only online players      |
| search      | string  | Search by player name    |

---

## Example Request

```http
GET /api/players?limit=10&page=1&order=asc
```

---

## Example Response

```json
{
  "total": 2,
  "limit": 10,
  "page": 1,
  "offset": 0,
  "players": [
    {
      "uuid": "uuid-here",
      "name": "Steve",
      "online": true
    }
  ]
}
```

---

# Player Stats Endpoint

## GET `/api/player/{player}`

Returns complete player statistics.

`{player}` can be:

- UUID
- player name

---

# Query Parameters

| Parameter  | Type    | Description                |
|------------|---------|----------------------------|
| summary    | boolean | Include summary            |
| raw        | boolean | Include raw stats          |
| categories | string  | Comma-separated categories |

---

## Example Request

```http
GET /api/player/Steve
```

---

## Example Request With Filters

```http
GET /api/player/Steve?summary=true&raw=false&categories=mined,crafted
```

---

## Example Response

```json
{
  "uuid": "uuid-here",
  "name": "Steve",
  "online": true,
  "summary": {
    "playTimeTicks": 123456,
    "playTimeFormatted": "1d 2h 30m",
    "deaths": 5,
    "playerKills": 10,
    "mobKills": 250
  },
  "crafted": {
    "minecraft:diamond_pickaxe": 2
  },
  "mined": {
    "minecraft:diamond_ore": 120
  }
}
```

---

# Player Summary Endpoint

## GET `/api/player/{player}/summary`

Returns lightweight summarized stats.

---

# Query Parameters

| Parameter | Type    | Description            |
|-----------|---------|------------------------|
| movement  | boolean | Include movement stats |
| combat    | boolean | Include combat stats   |
| activity  | boolean | Include activity stats |

---

## Example Request

```http
GET /api/player/Steve/summary
```

---

## Example Response

```json
{
  "uuid": "uuid-here",
  "name": "Steve",
  "online": false,
  "playtime_ticks": 500000,
  "playtime_formatted": "6h 55m",
  "combat": {
    "deaths": 4,
    "mob_kills": 120
  },
  "movement": {
    "total_distance_km": 25.3,
    "jumps": 1200
  },
  "activity": {
    "blocks_mined": 4200,
    "items_crafted": 150
  }
}
```

---

# Leaderboard Endpoint

## GET `/api/leaderboard/{type}`

Returns leaderboard data.

---

# Supported Types

`kills`, `deaths`, `kdr`, `player_kills`, `mob_kills`, `damage_dealt`, `damage_taken`,
`playtime`, `active_time`, `sessions`, `blocks_mined`, `blocks_broken`, `items_crafted`,
`items_used`, `items_picked_up`, `items_dropped`, `chests_opened`, `jumps`,
`distance_traveled`, `distance_walked`, `distance_sprinted`, `distance_swum`, `distance_flown`, `balance`.

---

# Query Parameters

| Parameter   | Type    | Description                                |
|-------------|---------|--------------------------------------------|
| limit       | integer | Maximum entries                            |
| page        | integer | Page number                                |
| order       | string  | asc / desc                                 |
| period      | string  | daily / weekly / monthly / all_time        |
| online_only | boolean | Only currently online players              |

---

## Example Request

```http
GET /api/leaderboard/kills?period=weekly&limit=10
```

---

## Example Response

```json
{
  "stat": "kills",
  "period": "weekly",
  "total": 128,
  "limit": 10,
  "page": 1,
  "offset": 0,
  "entries": [
    {
      "rank": 1,
      "uuid": "uuid-here",
      "name": "Steve",
      "online": true,
      "value": 328,
      "formatted": "328"
    }
  ]
}
```

---

# Player History Endpoint

## GET `/api/player/{player}/history`

Returns historical metric snapshots.

| Parameter | Type    | Description                                  |
|-----------|---------|----------------------------------------------|
| from      | string  | Epoch millis or relative (`7d`, `24h`, `30m`) |
| to        | string  | Epoch millis (defaults to now)               |
| limit     | integer | Maximum snapshots                            |

```http
GET /api/player/Steve/history?from=7d&limit=100
```

---

# Player Activity Endpoint

## GET `/api/player/{player}/activity`

Returns the player's activity timeline (joins, leaves, sessions, kills, deaths, milestones).

```http
GET /api/player/Steve/activity?from=7d
```

---

# Player Sessions Endpoint

## GET `/api/player/{player}/sessions`

```json
{
  "player": "Steve",
  "uuid": "uuid-here",
  "sessions": 127,
  "total_playtime": 98234,
  "average_session": 46,
  "longest_session": 241,
  "current_session": 0,
  "first_seen": 1710000000000,
  "last_seen": 1720000000000,
  "online": false
}
```

---

# Server Analytics

## GET `/api/server` · `/api/server/summary`

```json
{
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
}
```

## GET `/api/server/history`

Concurrency time series (`from`, `to`, `limit`).

## GET `/api/server/activity`

Activity heatmap data: `sessions_by_hour`, `sessions_by_weekday`, `peak_by_day`, `sessions_by_day`, `new_players_by_day`.

## GET `/api/server/retention`

```json
{
  "d1": 0.61,
  "d7": 0.38,
  "d14": 0.27,
  "d30": 0.19,
  "cohorts": { "d1": 420, "d7": 398, "d14": 351, "d30": 280 },
  "retained": { "d1": 256, "d7": 151, "d14": 95, "d30": 53 }
}
```

## GET `/api/server/segments`

Player segmentation counts (`new`, `active`, `highly_active`, `at_risk`, `inactive`, `churned`).

---

# Network Endpoint

## GET `/api/network`

Aggregated view of this server plus every peer that has pushed a report (or to
which this server pushes). A single-server install returns just itself.

```json
{
  "network_players": 190,
  "network_total_players": 4100,
  "network_sessions": 1800,
  "server_count": 3,
  "servers": [
    { "id": "survival", "name": "Survival", "online": 82, "total_players": 2000, "sessions": 900, "peak_all_time": 128, "local": true },
    { "id": "skyblock", "name": "Skyblock", "online": 61, "total_players": 1200, "sessions": 500, "peak_all_time": 90, "local": false }
  ]
}
```

## POST `/api/network/report`

Accepts a peer summary from another StatFYR instance (enabled with
`network.accept-reports`). When `network.report-key` is set, send it in the
`X-StatFYR-Key` header. Reports expire after `network.report-ttl-seconds`.

```http
POST /api/network/report
X-StatFYR-Key: shared-secret
Content-Type: application/json

{
  "serverId": "skyblock",
  "serverName": "Skyblock",
  "online": 61,
  "totalPlayers": 1200,
  "sessions": 500,
  "peakAllTime": 90
}
```

A node with `network.hub-url` configured pushes its own report to the hub
automatically every `network.report-interval-seconds`.

---

# Custom Metrics Endpoint

## GET `/api/custom` · `/api/custom/{metric}`

Metrics registered by other plugins via the StatFYR metrics API.

```http
GET /api/custom/economy.balance
```

---

# Prometheus Endpoint

## GET `/metrics`

When `integrations.prometheus.enabled: true`, exposes Prometheus-format metrics:

```text
statfyr_online_players
statfyr_unique_players_today
statfyr_peak_players_today
statfyr_peak_players_all_time
statfyr_player_sessions
statfyr_server_playtime_seconds
statfyr_new_players_today
statfyr_returning_players_today
```

---

# Archive Endpoint

## GET `/api/archive/{metric}`

Archived leaderboard results for completed daily/weekly/monthly windows.

| Parameter | Type    | Description                          |
|-----------|---------|--------------------------------------|
| period    | string  | daily / weekly / monthly             |
| from      | string  | Epoch millis or relative (`90d`)     |
| to        | string  | Epoch millis                         |
| limit     | integer | Maximum archives                     |

```http
GET /api/archive/kills?period=weekly&limit=10
```

---

# Economy Endpoint

## GET `/api/server/economy`

Read-only Vault economy analytics (returns `{ "enabled": false }` when Vault is absent).

```json
{
  "enabled": true,
  "players": 4218,
  "total_in_circulation": 91827364.5,
  "average": 21765.4,
  "median": 12000.0,
  "highest": 9123456.0,
  "lowest": 0.0
}
```

---

# Web Dashboard

## GET `/dashboard`

Optional, API-first dashboard. Enabled with `enabled: true` in `plugins/statfyr/dashboard.yml`.

The dashboard consumes the same public REST API as every other client. It is configured entirely
from `dashboard.yml` (title, subtitle, theme, accent color, refresh interval, default leaderboard
view) and visitors are never asked to type an API key into the page: the server injects its
configuration on load, including the API key when both of these hold:

- `embed-api-key: true` in `dashboard.yml` (default)
- `security.enable-api-key: true` with a non-empty `api-key` in `config.yml`

Only leave `embed-api-key` enabled where `/dashboard` is not publicly reachable (LAN, reverse
proxy with its own authentication).

### Modules

Each section of the page can be toggled independently in `dashboard.yml`:

| Key                  | Section                                    |
|----------------------|--------------------------------------------|
| `modules.overview`   | Headline counters                          |
| `modules.charts`     | Players-online and sessions-by-hour charts |
| `modules.leaderboards` | Leaderboard table with metric/period controls |
| `modules.players`    | Player directory with search               |
| `modules.retention`  | New player retention bars                  |
| `modules.segments`   | Activity segments                          |
| `modules.network`    | Multi-server network table                 |

### dashboard.yml example

```yaml
enabled: true
title: "My Server"
subtitle: "Live Stats"
theme: "dark"            # dark | light
accent: "#8b5cf6"
refresh-seconds: 30
api-base: ""             # empty = same origin
embed-api-key: true
leaderboard:
  metric: "kills"
  period: "all_time"
modules:
  overview: true
  charts: true
  leaderboards: true
  players: true
  retention: true
  segments: true
  network: true
```

Changes require a server restart or `/statfyr reload`.

---

# Status Codes

| Code | Meaning               |
|------|-----------------------|
| 200  | Success               |
| 400  | Bad Request           |
| 401  | Unauthorized          |
| 403  | Forbidden             |
| 404  | Not Found             |
| 405  | Method Not Allowed    |
| 429  | Too Many Requests     |
| 500  | Internal Server Error |

---

# Compression

If enabled:

```http
Accept-Encoding: gzip
```

Responses may be gzip compressed.

---

# Rate Limiting

If enabled:

- requests are rate limited per IP
- exceeding limit returns:

```http
429 Too Many Requests
```

---
