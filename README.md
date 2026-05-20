<div align="center">


<!-- Banner -->
<img src="src/main/resources/statfyrbanner.png" alt="Statfyr Banner" width="100%"/>

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
  "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "Steve",
  "online": true,
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

### Example Response — Leaderboard

```json
{
  "type": "playtime",
  "entries": [
    {
      "rank": 1,
      "uuid": "...",
      "name": "Steve",
      "value": 500000
    },
    {
      "rank": 2,
      "uuid": "...",
      "name": "Alex",
      "value": 380000
    }
  ]
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
