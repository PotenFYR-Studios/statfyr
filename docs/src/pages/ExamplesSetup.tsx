import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "config", label: "The config.yml" },
  { id: "endpoints", label: "Endpoint walkthrough" },
  { id: "fetch", label: "Fetch it from JavaScript" },
  { id: "commands", label: "Manage it in game" },
  { id: "next", label: "Go deeper" },
];

export default function ExamplesSetup() {
  useEffect(() => {
    applyMeta({
      title: "Examples · Statfyr",
      description:
        "Ready-to-paste Statfyr examples: a hardened config.yml (API key, HTTPS, CORS) and curl walkthroughs of every REST endpoint.",
      path: "/examples",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "Examples" },
      ]}
      title="Examples"
      lede="Two copy-paste blocks to go from install to a working, authenticated API: a hardened config.yml and a curl walkthrough of every endpoint. For full integrations (dashboards, leaderboards, a Discord bot), see the integration examples."
      toc={TOC}
    >
      <h2 id="config">The config.yml</h2>
      <p>
        On first run the plugin writes <code>plugins/statfyr/config.yml</code>. This excerpt sets
        the values you almost certainly want to change before exposing the API beyond localhost;
        all keys and defaults are straight from the shipped config file:
      </p>
      <CodeBlock
        lang="yaml"
        filename="plugins/statfyr/config.yml"
        code={`https:
  enabled: true
  keystore-path: "plugins/statfyr/keystore.jks"
  keystore-password: "changeit"       # set your own, or env STATFYR_KEYSTORE_PASSWORD

security:
  # Bearer auth: Authorization: Bearer <your-key>
  enable-api-key: true
  api-key: "GENERATE_ME"              # e.g. openssl rand -hex 32, or env STATFYR_API_KEY

  # Rate limiting (per client IP)
  enable-rate-limit: true
  rate-limit-requests: 120
  rate-limit-window-seconds: 60

  # CORS
  enable-cors: true
  allowed-origins:
    - "https://dashboard.example.com"

http:
  port: 8080
  bind-address: "0.0.0.0"             # 127.0.0.1 keeps the API localhost-only`}
      />
      <p>
        After editing, apply changes without a restart: <code>/statfyr reload</code>. Generate the
        key once, hand it only to trusted clients; it grants full read access to every statistic.
      </p>
      <Callout kind="warn">
        Authentication is only active while the key is non-blank: with{" "}
        <code>enable-api-key: true</code> and an empty <code>api-key</code>, the plugin treats auth
        as disabled and serves every endpoint openly. Set the key, or set{" "}
        <code>enable-api-key: false</code> knowingly on a trusted network.
      </Callout>
      <Callout kind="warn">
        <code>allowed-origins</code> is not enforced yet: with <code>enable-cors: true</code> the
        API answers <code>Access-Control-Allow-Origin: *</code> regardless of the list. Don't rely
        on it as a security boundary: the API key is the boundary. The list stays in the config
        for the day it is wired up.
      </Callout>

      <h2 id="endpoints">Endpoint walkthrough</h2>
      <p>
        With <code>enable-api-key: true</code>, every route requires the header, including{" "}
        <code>/api/health</code> included. Requests are GET-only (anything else answers{" "}
        <code>405</code>; CORS preflight <code>OPTIONS</code> answers <code>204</code>).
      </p>
      <CodeBlock
        lang="bash"
        filename="first-requests.sh"
        code={`BASE="http://127.0.0.1:8080"
KEY="GENERATE_ME"

# API index and self-describing endpoint list
curl -s -H "Authorization: Bearer $KEY" "$BASE/api"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/docs"

# Health and player roster
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/health"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/players?online_only=true"

# Full stats and computed summary for one player
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/Notch"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/Notch/summary"

# Sessions, history and activity timeline
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/Notch/sessions"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/Notch/history?from=7d"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/Notch/activity?from=7d"

# Leaderboards (all-time and weekly)
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/leaderboard/kills?limit=5"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/leaderboard/kills?period=weekly&limit=5"

# Server analytics, retention and heatmaps
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/server/summary"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/server/retention"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/server/activity"

# Archived weekly leaderboards and network shape
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/archive/kills?period=weekly"
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/network"`}
      />
      <Callout kind="note">
        A missing or wrong key answers <code>401</code>; tripping the 120-requests-per-60-seconds
        limit answers <code>429</code>. Full request/response shapes live in the{" "}
        <Link to="/docs/api">API reference</Link>.
      </Callout>

      <h2 id="fetch">Fetch it from JavaScript</h2>
      <p>The whole client is one wrapper: base URL plus Bearer header:</p>
      <CodeBlock
        lang="javascript"
        filename="statfyr.js"
        code={`const BASE = "http://127.0.0.1:8080";

export async function statfyr(path) {
  const res = await fetch(BASE + path, {
    headers: { Authorization: "Bearer " + process.env.STATFYR_API_KEY },
  });
  if (!res.ok) throw new Error("statfyr " + res.status);
  return res.json();
}

const health = await statfyr("/api/health");
const top = await statfyr("/api/leaderboard/playtime?limit=10");`}
      />

      <h2 id="commands">Manage it in game</h2>
      <p>
        Admin commands need <code>statfyr.admin</code> (or the granular{" "}
        <code>statfyr.reload</code> / <code>statfyr.status</code>); player commands are available to
        everyone by default:
      </p>
      <CodeBlock
        lang="text"
        code={`/statfyr stats [player]   # view statistics
/statfyr top [stat] [period]  # leaderboards
/statfyr help             # list commands
/statfyr reload           # re-read config and restart the HTTP server
/statfyr status           # current runtime status
/statfyr database         # storage statistics`}
      />
      <p>See <Link to="/docs/commands">Commands &amp; Permissions</Link> for the full list.</p>

      <h2 id="next">Go deeper</h2>
      <ul>
        <li>
          <Link to="/docs/api">API reference</Link>: every endpoint's parameters, response fields,
          and error shapes.
        </li>
        <li>
          <Link to="/docs/commands">Commands &amp; Permissions</Link>: the full command and
          permission surface, plus LuckPerms examples.
        </li>
        <li>
          <Link to="/docs/placeholders">PlaceholderAPI</Link>: player and server placeholders.
        </li>
        <li>
          <Link to="/docs/integrations">Integrations</Link>: Vault, Discord, Prometheus, the
          dashboard and the custom metrics API.
        </li>
        <li>
          <Link to="/docs/analytics">Analytics &amp; History</Link>: sessions, history, retention
          and segmentation.
        </li>
        <li>
          <Link to="/docs/examples">Integration examples</Link>: a live dashboard, a paginated
          leaderboard, and a Discord bot built on this API.
        </li>
        <li>
          <Link to="/docs/configuration">Configuration reference</Link>: every key in config.yml,
          with enforced/reserved status flagged per key.
        </li>
      </ul>
    </DocsShell>
  );
}
