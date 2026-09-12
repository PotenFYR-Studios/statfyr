<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:8b5cf6,50:ec4899,100:f97316&height=220&section=header&text=Statfyr&fontSize=52&fontColor=ffffff&fontAlignY=34&desc=A%20blazing-fast%20REST%20API%20for%20Minecraft%20player%20statistics&descSize=20&descAlignY=55&animation=twinkling" width="100%" alt="Statfyr banner"/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=20&pause=1200&color=8B5CF6&center=true&vCenter=true&width=800&lines=Every+player+statistic%2C+one+JSON+API;Live+reads+with+zero+TPS+impact;Bearer+auth%2C+rate+limits%2C+gzip+built+in;Paper+%C2%B7+Spigot+%C2%B7+Purpur+1.16.5+to+1.21.x)](https://statfyr.docs.potenfyr.in)

[![Modrinth](https://img.shields.io/badge/Modrinth-statfyr-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white&labelColor=1c1e26)](https://modrinth.com/plugin/statfyr)
[![Docs](https://img.shields.io/badge/Docs-statfyr.docs.potenfyr.in-8b5cf6?style=for-the-badge&logo=githubpages&logoColor=white&labelColor=1c1e26)](https://statfyr.docs.potenfyr.in)
[![Build](https://img.shields.io/github/actions/workflow/status/PotenFYR-Studios/statfyr/build.yml?branch=master&style=for-the-badge&logo=githubactions&label=Build&labelColor=1c1e26&color=2ea043)](https://github.com/PotenFYR-Studios/statfyr/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache--2.0%20%2B%20Commons%20Clause-f97316?style=for-the-badge&labelColor=1c1e26)](LICENSE)
[![Profile views](https://komarev.com/ghpvc/?username=PotenFYR-Studios-statfyr&color=ec4899&style=for-the-badge&label=PROFILE+VIEWS&labelColor=1c1e26)](https://github.com/PotenFYR-Studios/statfyr)

</div>

**Statfyr** is a blazing-fast REST API plugin for Minecraft that exposes player statistics (playtime, kills, deaths, blocks mined, items crafted, movement, and more) through a clean, documented HTTP interface. Built for **Paper, Spigot, and Purpur 1.16.5–1.21.x** with zero external dependencies: dashboards, Discord bots, leaderboard sites, and analytics tools all talk to your server over plain JSON.

Stats are read **asynchronously, off the main thread**, and served from an in-memory cache, so API traffic never touches your TPS.

📚 **Full documentation → [statfyr.docs.potenfyr.in](https://statfyr.docs.potenfyr.in)**

---

## ✨ Features

- 📊 **Full stat access**: all nine vanilla statistic categories, live for online players and parsed from vanilla stat files for offline ones
- 🏆 **Leaderboards**: `playtime` · `deaths` · `player_kills` · `mob_kills` · `blocks_mined` · `items_picked_up` · `items_crafted`
- ⚡ **Async loading**: stats are fetched on background threads, zero TPS impact
- 🗃️ **Response caching**: short-lived in-memory cache keeps repeated lookups cheap
- 🔐 **Bearer authentication**: lock the API behind a secret key
- 🛡️ **Rate limiting**: per-IP throttling out of the box (120 requests / 60 s by default)
- 🌐 **CORS support**: browser-based dashboards work without a proxy
- 🧱 **IP whitelist**: restrict access to trusted addresses
- 🔑 **HTTPS / TLS**: optional, via a Java keystore
- 📄 **Built-in API docs**: browsable endpoint reference at `/api/docs`
- 🧩 **Pagination & sorting**: standard `limit`, `page`, and `order` params across list endpoints
- 🗜️ **gzip compression**: automatic for clients that send `Accept-Encoding: gzip`

## 📋 Requirements

| Requirement   | Version                 |
|---------------|-------------------------|
| Java          | 16+                     |
| Paper/Spigot/Purpur | 1.16.5 – 1.21.x   |

## 🚀 Quick Start

1. Download the latest `.jar` from [Modrinth](https://modrinth.com/plugin/statfyr)
2. Drop it into your server's `/plugins/` folder and restart
3. Verify it's live:

   ```bash
   curl http://localhost:8080/api/health
   ```

4. For anything beyond localhost, **enable authentication**: set `security.enable-api-key: true`, generate a strong random key into `security.api-key`, then call the API with `Authorization: Bearer <key>` (or set the `STATFYR_API_KEY` environment variable)

Configuration changes apply with `/statfyr reload`; no restart needed.

## 🔌 API Overview

All endpoints are `GET`-only and return JSON. The base URL is:

```
http://your-server:8080/api
```

### Endpoints

| Method | Endpoint                        | Description                                        |
|--------|---------------------------------|----------------------------------------------------|
| `GET`  | `/api`                          | API index and version                               |
| `GET`  | `/api/health`                   | Server health, memory, and feature flags            |
| `GET`  | `/api/docs`                     | Built-in endpoint reference                         |
| `GET`  | `/api/players`                  | Paginated player list (`limit`, `page`, `online_only`, `search`) |
| `GET`  | `/api/player/{uuid\|name}`      | Full stats for one player                           |
| `GET`  | `/api/player/{uuid\|name}/summary` | Lightweight movement/combat/activity summary     |
| `GET`  | `/api/leaderboard/{stat}`       | Ranked leaderboard (`limit`, `offset`, `order`)     |

Valid `{stat}` values: `playtime`, `deaths`, `player_kills`, `mob_kills`, `blocks_mined`, `items_picked_up`, `items_crafted`.

### Examples

```bash
# Server health
curl http://localhost:8080/api/health

# Five most recently online players
curl "http://localhost:8080/api/players?limit=5&online_only=true"

# A player's summary (by name or UUID)
curl -H "Authorization: Bearer $STATFYR_API_KEY" \
  "http://localhost:8080/api/player/Notch/summary"

# Top 10 by playtime
curl "http://localhost:8080/api/leaderboard/playtime?limit=10"
```

<details>
<summary><strong>Example response: leaderboard</strong></summary>

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

</details>

Every request shape, parameter, and response field is documented in the [API Reference](https://statfyr.docs.potenfyr.in/docs/api).

## ⚙️ Configuration

The config file is generated at `plugins/statfyr/config.yml` on first run. The core, enforced keys:

```yaml
http:
  port: 8080
  bind-address: "0.0.0.0"    # use 127.0.0.1 to keep the API localhost-only

https:
  enabled: false
  keystore-path: "plugins/statfyr/keystore.jks"

security:
  enable-api-key: false
  api-key: ""                # or set the STATFYR_API_KEY env var
  enable-rate-limit: true
  rate-limit-requests: 120
  rate-limit-window-seconds: 60
  enable-cors: true
  enable-ip-whitelist: false
  allowed-ips: []

pagination:
  default-limit: 25
  max-limit: 100
```

**Environment variable overrides** (take precedence over the config file):

| Variable                    | Config key                 |
|-----------------------------|----------------------------|
| `STATFYR_API_KEY`           | `security.api-key`         |
| `STATFYR_KEYSTORE_PASSWORD` | `https.keystore-password`  |

> Some keys shipped in `config.yml` (compression, async, cache, docs, and a few others) are read but **not yet enforced** in the 1.0.0-BETA build; the [configuration reference](https://statfyr.docs.potenfyr.in/docs/configuration) marks every key as enforced or reserved.

## 🔒 Securing Your API

For any server reachable beyond localhost:

1. **Enable API key authentication**: `security.enable-api-key: true` with a long random key
2. **Enable HTTPS**: generate a keystore and set `https.enabled: true`
3. **Restrict the bind address**: `http.bind-address: "127.0.0.1"` if only local services (e.g. a reverse proxy) need access
4. **Enable IP whitelisting**: `security.enable-ip-whitelist: true` with your dashboard's IPs

**Generate a keystore for HTTPS:**

```bash
keytool -genkeypair -alias statfyr \
  -keyalg RSA -keysize 2048 \
  -storetype JKS \
  -keystore plugins/statfyr/keystore.jks \
  -validity 3650
```

## 🛠️ Commands & Permissions

| Command           | Permission       | Default | Description              |
|-------------------|------------------|---------|--------------------------|
| `/statfyr reload` | `statfyr.admin`  | op      | Reload the configuration |
| `/statfyr status` | `statfyr.admin`  | op      | Show API status and port |

## 📡 Status Codes

| Code  | Meaning                                   |
|-------|-------------------------------------------|
| `200` | Success                                   |
| `400` | Bad request (invalid parameters)          |
| `401` | Unauthorized (missing or invalid API key) |
| `403` | Forbidden (IP not whitelisted)            |
| `404` | Player or resource not found              |
| `405` | Method not allowed (the API is GET-only)  |
| `429` | Too many requests (rate limit exceeded)   |
| `500` | Internal server error                     |

## 💬 Support

- **Bugs, ideas, questions** → [GitHub Issues](https://github.com/PotenFYR-Studios/statfyr/issues); issue templates are provided, so pick the closest fit
- **Documentation** → [statfyr.docs.potenfyr.in](https://statfyr.docs.potenfyr.in)

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for how to build the plugin locally and what to include in a pull request.

## 🔐 Security Policy

Found a security vulnerability? Please **do not open a public issue**, and follow the confidential disclosure process in [SECURITY.md](SECURITY.md).

## 📜 License

Statfyr is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) with the [Commons Clause](https://github.com/PotenFYR-Studios/statfyr/blob/master/LICENSE) condition: use, modify, fork, self-host, and redistribute it freely, including in commercial projects, but do not sell Statfyr itself. The license text in this repository is the authoritative version.

## Contributing

Contributions make the open-source community such an amazing place to learn, inspire and create. Any contributions you make are **greatly appreciated** - see [CONTRIBUTING.md](CONTRIBUTING.md) and the [good first issues](https://github.com/PotenFYR-Studios/statfyr/labels/good%20first%20issue). Security concerns: please use [SECURITY.md](SECURITY.md) (private vulnerability reporting), not public issues.

<a href="https://github.com/PotenFYR-Studios/statfyr/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=PotenFYR-Studios/statfyr" alt="statfyr contributors" />
</a>
<a href="https://github.com/PotenFYR-Studios/statfyr/stargazers">
  <img src="https://img.shields.io/github/stars/PotenFYR-Studios/statfyr?style=social&label=Stars" alt="Live star count" />
</a>
<a href="https://github.com/PotenFYR-Studios/statfyr/network/members">
  <img src="https://img.shields.io/github/forks/PotenFYR-Studios/statfyr?style=social&label=Forks" alt="Live fork count" />
</a>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/PotenFYR-Studios/FYRwall/output/github-snake-dark.svg" />
  <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/PotenFYR-Studios/FYRwall/output/github-snake.svg" />
  <img alt="Contribution snake animation" src="https://raw.githubusercontent.com/PotenFYR-Studios/FYRwall/output/github-snake.svg" width="100%" />
</picture>


---

<!-- markdownlint-disable -->


<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:f97316,50:ec4899,100:8b5cf6&height=120&section=footer&text=Made%20with%20%E2%9D%A4%EF%B8%8F%20by%20PotenFYR%20Studios&fontSize=22&fontColor=ffffff&animation=twinkling" width="100%" alt="footer"/>

</div>
<!-- markdownlint-enable -->

---

## ⭐ Star History

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=potenfyr-studios/authcore,potenfyr-studios/statfyr,potenfyr-studios/discord-botlists,potenfyr-studios/vigilfyr,potenfyr-studios/shell-eggs,potenfyr-studios/prog-language-eggs,potenfyr-studios/minecraft-eggs,potenfyr-studios/database-eggs,potenfyr-studios/apicordon,potenfyr-studios/ojaj,potenfyr-studios/fyrwall,potenfyr-studios/echoingdeaths&type=Date&theme=dark" />
  <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=potenfyr-studios/authcore,potenfyr-studios/statfyr,potenfyr-studios/discord-botlists,potenfyr-studios/vigilfyr,potenfyr-studios/shell-eggs,potenfyr-studios/prog-language-eggs,potenfyr-studios/minecraft-eggs,potenfyr-studios/database-eggs,potenfyr-studios/apicordon,potenfyr-studios/ojaj,potenfyr-studios/fyrwall,potenfyr-studios/echoingdeaths&type=Date" />
  <img alt="Star history chart for all PotenFYR Studios public repositories" src="https://api.star-history.com/svg?repos=potenfyr-studios/authcore,potenfyr-studios/statfyr,potenfyr-studios/discord-botlists,potenfyr-studios/vigilfyr,potenfyr-studios/shell-eggs,potenfyr-studios/prog-language-eggs,potenfyr-studios/minecraft-eggs,potenfyr-studios/database-eggs,potenfyr-studios/apicordon,potenfyr-studios/ojaj,potenfyr-studios/fyrwall,potenfyr-studios/echoingdeaths&type=Date" width="80%" />
</picture>

Every public PotenFYR Studios repository on one live chart, served by [star-history.com](https://star-history.com).
