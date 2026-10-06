import { useEffect } from "react";
import { applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "optional", label: "All optional" },
  { id: "placeholderapi", label: "PlaceholderAPI" },
  { id: "vault", label: "Vault" },
  { id: "discord", label: "Discord" },
  { id: "prometheus", label: "Prometheus" },
  { id: "dashboard", label: "Web dashboard" },
  { id: "network", label: "Multi-server" },
  { id: "custom-metrics", label: "Custom metrics API" },
];

export default function Integrations() {
  useEffect(() => {
    applyMeta({
      title: "Integrations · StatFYR Docs",
      description:
        "Optional StatFYR integrations: PlaceholderAPI, Vault economy analytics, Discord webhooks, Prometheus metrics, the web dashboard, and the custom metrics API.",
      path: "/docs/integrations",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "Integrations" },
      ]}
      title="Integrations"
      lede="Every integration is optional and independently toggleable. StatFYR starts and runs normally with all of them absent."
      toc={TOC}
    >
      <h2 id="optional">All optional</h2>
      <p>
        Integrations are configured under <code>integrations:</code> in{" "}
        <code>plugins/statfyr/config.yml</code>. None of them are hard dependencies, and none add a
        bundled runtime dependency.
      </p>
      <CodeBlock
        lang="yaml"
        code={`integrations:
  placeholderapi:
    enabled: true
  vault:
    enabled: true
  discord:
    enabled: false
    webhook-url: ""
    peak-milestone: 100
    events:
      weekly-leaderboard: true
      monthly-leaderboard: true
      peak-milestone: true
      new-player: false
      server-status: false
  prometheus:
    enabled: false
  dashboard:
    enabled: false`}
      />

      <h2 id="placeholderapi">PlaceholderAPI</h2>
      <p>
        When PlaceholderAPI is installed the <code>statfyr</code> expansion registers
        automatically. See the <a href="/docs/placeholders">Placeholders</a> page for the full list.
      </p>

      <h2 id="vault">Vault</h2>
      <p>
        With Vault and a compatible economy provider, StatFYR reads balances for analytics — player
        balance, and server aggregates at <code>/api/server/economy</code> (total in circulation,
        average, median, highest, lowest). It also exposes a <code>balance</code> leaderboard.
      </p>
      <Callout kind="warn">
        StatFYR never implements an economy. It only reads from the existing Vault provider. When
        Vault is absent, economy endpoints return <code>{'{ "enabled": false }'}</code>.
      </Callout>

      <h2 id="discord">Discord</h2>
      <p>
        Optional Discord webhook notifications, sent asynchronously over plain{" "}
        <code>HttpURLConnection</code>. Configure the webhook URL and enable the events you want:
      </p>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Event</th><th>Trigger</th></tr></thead>
          <tbody>
            <tr><td><code>weekly-leaderboard</code></td><td>Shortly after the configured weekly reset.</td></tr>
            <tr><td><code>monthly-leaderboard</code></td><td>Shortly after the monthly reset.</td></tr>
            <tr><td><code>peak-milestone</code></td><td>When concurrency reaches <code>peak-milestone</code>.</td></tr>
            <tr><td><code>new-player</code></td><td>A player joins for the first time.</td></tr>
            <tr><td><code>server-status</code></td><td>Reserved for periodic status posts.</td></tr>
          </tbody>
        </table>
      </div>

      <h2 id="prometheus">Prometheus</h2>
      <p>
        Set <code>integrations.prometheus.enabled: true</code> to expose{" "}
        <code>/metrics</code> in Prometheus text exposition format. No Prometheus dependency is
        required — scrape it directly:
      </p>
      <CodeBlock
        lang="yaml"
        filename="prometheus.yml"
        code={`scrape_configs:
  - job_name: statfyr
    static_configs:
      - targets: ["your-server:8080"]`}
      />
      <p>Exported metrics include:</p>
      <CodeBlock
        lang="text"
        code={`statfyr_online_players
statfyr_unique_players_today
statfyr_peak_players_today
statfyr_peak_players_all_time
statfyr_player_sessions
statfyr_server_playtime_seconds
statfyr_new_players_today
statfyr_returning_players_today
statfyr_profiles_total`}
      />

      <h2 id="dashboard">Web dashboard</h2>
      <p>
        Set <code>integrations.dashboard.enabled: true</code> to serve a lightweight, API-first
        dashboard at <code>/dashboard</code>. It consumes the same public REST API — enter your API
        key in the page. There is no separate analytics implementation.
      </p>

      <h2 id="network">Multi-server</h2>
      <p>
        StatFYR can aggregate a whole BungeeCord/Velocity network without a shared database. Point
        each server at a hub and it will push its summary every interval; the hub stores reports
        (expiring after a TTL) and aggregates them at <code>/api/network</code>.
      </p>
      <CodeBlock
        lang="yaml"
        code={`network:
  server-id: "survival"
  server-name: "Survival"
  hub-url: "http://hub.example.com:8080"   # blank = don't push
  report-key: "shared-secret"              # X-StatFYR-Key
  accept-reports: true                     # accept POST /api/network/report
  report-interval-seconds: 60
  report-ttl-seconds: 180`}
      />
      <Callout kind="note">
        Multi-server support is optional. A single-server install simply returns itself from{" "}
        <code>/api/network</code> and needs no extra configuration.
      </Callout>

      <h2 id="custom-metrics">Custom metrics API</h2>
      <p>
        Other plugins can register arbitrary metrics (economy, quests, minigame wins, votes, jobs,
        skills, …). They are stored per player and exposed at{" "}
        <code>/api/custom/&lt;metric&gt;</code>.
      </p>
      <CodeBlock
        lang="java"
        filename="AnotherPlugin.java"
        code={`Statfyr.getInstance().getAnalytics().setCustomMetric(
    "economy.balance",
    player.getUniqueId(),
    12500.0
);

// Optional: register a metric for offline players by UUID
Statfyr.getInstance().getAnalytics().setCustomMetric(
    "quests.completed",
    uuid,
    42.0
);`}
      />
      <Callout kind="note">
        Custom metrics are only meaningful if your plugin pushes them; StatFYR does not tightly
        couple to any third-party plugin.
      </Callout>
    </DocsShell>
  );
}
