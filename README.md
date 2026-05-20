<div align="center">


<!-- Banner -->
<img src="https://i.imgur.com/9IwT9gW.png" alt="Statfyr Banner" width="100%"/>

<br/>

# Statfyr

### A blazing-fast REST API for Minecraft player statistics

</div>

---

**Statfyr** is a lightweight Paper plugin that exposes your server's player statistics through a clean, documented REST
API. Dashboards, bots, leaderboard websites, Discord integrations — build anything, in any language, against a single
HTTP endpoint.

---

## ✨ Features

- 📊 **Full stat access** — playtime, kills, deaths, blocks mined, items crafted, movement, and more
- 🏆 **Leaderboards** — ranked endpoints for playtime, kills, deaths, mined, and crafted
- ⚡ **Async loading** — stats are fetched off the main thread, zero TPS impact
- 🗜️ **gzip compression** — optional response compression for bandwidth efficiency
- 🔐 **Bearer authentication** — lock your API behind a secret key
- 🛡️ **Rate limiting** — per-IP request throttling out of the box
- 🌐 **CORS support** — configurable allowed origins for browser-based clients
- 🔒 **IP whitelist** — restrict access to specific trusted addresses
- 🔑 **HTTPS / SSL** — optional TLS via Java keystore
- 📄 **Built-in API docs** — browsable documentation at `/api/docs`
- 🧩 **Pagination & sorting** — standard `limit`, `page`, and `order` params across all list endpoints
- ⚙️ **Response caching** — configurable TTL to reduce repeated stat lookups

---

## 📋 Requirements

| Requirement  | Version |
|--------------|---------|
| Java         | 16+     |
| Paper/Spigot | 1.16.5+ |

---

## 🚀 Installation

1. Download the latest `.jar` from the [Releases](https://modrinth.com/plugin/statfyr/versions) tab
2. Drop it into your server's `/plugins/` folder
3. Start (or restart) your server
4. Edit `plugins/statfyr/config.yml` to your liking
5. Use `/statfyr reload` to apply changes without a full restart

The API will be live at `http://your-server-ip:8080/api` by default.

---

## 🔌 API Overview

All responses are JSON. The base URL for all endpoints is:

```
http://your-server:8080/api
```

### Endpoints at a Glance

| Method | Endpoint                       | Description                            |
|--------|--------------------------------|----------------------------------------|
| `GET`  | `/api/health`                  | Server health and version info         |
| `GET`  | `/api/players`                 | List all known players                 |
| `GET`  | `/api/player/{player}`         | Full stats for a player (UUID or name) |
| `GET`  | `/api/player/{player}/summary` | Lightweight summarized stats           |
| `GET`  | `/api/leaderboard/{type}`      | Ranked leaderboard                     |

### Leaderboard Types

`playtime` · `kills` · `deaths` · `mined` · `crafted`

---

## 📖 Usage Examples

**Get all online players:**

```http
GET /api/players?online_only=true&limit=25
```

**Get a player's full stats, filtered to mined and crafted categories:**

```http
GET /api/player/Steve?summary=true&categories=mined,crafted
```

**Get the top 10 playtime leaderboard:**

```http
GET /api/leaderboard/playtime?limit=10&order=desc
```

**Authenticated request:**

```http
GET /api/players
Authorization: Bearer your-secret-key
```

---

### Example Response — Player Summary

```json
{
  "uuid": "xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "Alex",
  "online": false,
  "playtime_ticks": 125,
  "playtime_seconds": 6,
  "playtime_formatted": "6s",
  "combat": {
    "deaths": 0,
    "player_kills": 0,
    "mob_kills": 0,
    "damage_dealt": 0,
    "damage_taken": 0
  },
  "movement": {
    "distance_walked_cm": 0,
    "distance_walked_m": 0,
    "distance_sprinted_cm": 0,
    "distance_sprinted_m": 0,
    "distance_flown_cm": 0,
    "distance_flown_m": 0,
    "distance_swum_cm": 0,
    "distance_swum_m": 0,
    "total_distance_cm": 0,
    "total_distance_m": 0,
    "total_distance_km": 0,
    "jumps": 0
  },
  "activity": {
    "chests_opened": 0,
    "items_crafted": 0,
    "items_broken": 0,
    "items_used": 0,
    "items_picked_up": 0,
    "items_dropped": 0,
    "blocks_mined": 0
  },
  "metadata": {
    "generated_at": "2026-05-20T11:17:16.252462500Z",
    "execution_time_ms": 34,
    "movement_enabled": true,
    "combat_enabled": true,
    "activity_enabled": true
  }
}
```

### Example Response — Leaderboard

```json
{
  "stat": "playtime",
  "total": 1,
  "limit": 25,
  "offset": 0,
  "page": 1,
  "entries": [
    {
      "rank": 1,
      "uuid": "xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "name": "Steve",
      "online": false,
      "value": 125,
      "formatted": "6s"
    }
  ],
  "metadata": {
    "generated_at": "2026-05-20T11:18:07.625703400Z",
    "execution_time_ms": 3,
    "ascending": false,
    "online_only": false
  }
}
```

---

## ⚙️ Configuration

The full config file is generated at `plugins/statfyr/config.yml` on first run.

```yaml
http:
  port: 8080
  bind-address: "0.0.0.0"   # Use 127.0.0.1 to restrict to localhost

security:
  enable-api-key: false      # Enable Bearer token auth
  api-key: ""                # Set your key here, or via STATFYR_API_KEY env var

  enable-rate-limit: true
  rate-limit-requests: 120   # Requests per window
  rate-limit-window-seconds: 60

  enable-cors: true
  allowed-origins:
    - "*"                    # Lock this down in production

cache:
  ttl-seconds: 60
  refresh-seconds: 10
```

**Environment variable overrides:**

| Variable                    | Config Key                |
|-----------------------------|---------------------------|
| `STATFYR_API_KEY`           | `security.api-key`        |
| `STATFYR_KEYSTORE_PASSWORD` | `https.keystore-password` |

---

## 🔒 Securing Your API

For production servers, it is strongly recommended to:

1. **Enable API key authentication** — set `enable-api-key: true` and generate a strong random key
2. **Restrict CORS origins** — replace `"*"` with your dashboard's actual domain
3. **Enable IP whitelisting** — if only known services need access, lock it to those IPs
4. **Enable HTTPS** — generate a keystore and set `https.enabled: true`

**Generate a keystore for HTTPS:**

```bash
keytool -genkeypair -alias statfyr \
  -keyalg RSA -keysize 2048 \
  -storetype JKS \
  -keystore plugins/statfyr/keystore.jks \
  -validity 3650
```

---

## 📡 API Documentation

Interactive API documentation is available at:

```
http://your-server:8080/api/docs
```

Disable it in production with `docs.enabled: false`.

---

## 🗂️ HTTP Status Codes

| Code  | Meaning                                   |
|-------|-------------------------------------------|
| `200` | Success                                   |
| `400` | Bad request (invalid parameters)          |
| `401` | Unauthorized (missing or invalid API key) |
| `403` | Forbidden (IP not whitelisted)            |
| `404` | Player or resource not found              |
| `429` | Too many requests (rate limit exceeded)   |
| `500` | Internal server error                     |

---

## 🛠️ Commands & Permissions

| Command           | Permission       | Description              |
|-------------------|------------------|--------------------------|
| `/statfyr reload` | `statfyr.reload` | Reload the configuration |
| `/statfyr status` | `statfyr.status` | Show API status and port |

---

## 💬 Support & Community

- 🐛 **Bug reports** — [GitHub Issues](https://github.com/your-repo/statfyr/issues)
- 💡 **Feature requests** — [GitHub Discussions](https://github.com/your-repo/statfyr/discussions)
- 📖 **Full documentation** — [Wiki](https://github.com/your-repo/statfyr/wiki)

---

## 📜 License

Statfyr is released under the [MIT License](LICENSE).

---
