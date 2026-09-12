import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "config", label: "The config.yml" },
  { id: "endpoints", label: "All 7 endpoints, in order" },
  { id: "fetch", label: "Fetch it from JavaScript" },
  { id: "commands", label: "Manage it in game" },
  { id: "next", label: "Go deeper" },
];

export default function ExamplesSetup() {
  useEffect(() => {
    applyMeta({
      title: "Examples · Statfyr",
      description:
        "Ready-to-paste Statfyr examples: a hardened config.yml (API key, HTTPS, CORS) and curl walkthroughs of all 7 REST endpoints.",
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

      <h2 id="endpoints">All 7 endpoints, in order</h2>
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

# 1) API index: name, version, docs pointer
curl -s -H "Authorization: Bearer $KEY" "$BASE/api"

# 2) Self-describing endpoint list
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/docs"

# 3) Health check
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/health"

# 4) Every tracked player (online ones only)
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/players?online_only=true"

# 5) Full stats for one player, by name or UUID
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/Notch"

# 6) The same player as a computed summary
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/player/069a79f4-44e9-4726-a5be-fca90e38aaf5/summary"

# 7) Leaderboard: playtime, deaths, player_kills, mob_kills, blocks_mined, items_picked_up, items_crafted
curl -s -H "Authorization: Bearer $KEY" "$BASE/api/leaderboard/playtime?limit=5"`}
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
        Both commands need the <code>statfyr.admin</code> permission (default: operator):
      </p>
      <CodeBlock
        lang="text"
        code={`/statfyr reload   # re-read config.yml and restart the embedded HTTP server
/statfyr status   # current runtime status`}
      />

      <h2 id="next">Go deeper</h2>
      <ul>
        <li>
          <Link to="/docs/api">API reference</Link>: every endpoint's parameters, response fields,
          and error shapes.
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
