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

| Type     |
|----------|
| playtime |
| kills    |
| deaths   |
| mined    |
| crafted  |

---

# Query Parameters

| Parameter | Type    | Description     |
|-----------|---------|-----------------|
| limit     | integer | Maximum entries |
| page      | integer | Page number     |
| order     | string  | asc / desc      |

---

## Example Request

```http
GET /api/leaderboard/playtime?limit=10
```

---

## Example Response

```json
{
  "type": "playtime",
  "entries": [
    {
      "rank": 1,
      "uuid": "uuid-here",
      "name": "Steve",
      "value": 500000
    }
  ]
}
```

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
