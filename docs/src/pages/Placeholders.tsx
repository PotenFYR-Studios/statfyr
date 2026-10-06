import { useEffect } from "react";
import { applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "overview", label: "Overview" },
  { id: "player", label: "Player placeholders" },
  { id: "parameterised", label: "Other players" },
  { id: "server", label: "Server placeholders" },
  { id: "examples", label: "Examples" },
];

export default function Placeholders() {
  useEffect(() => {
    applyMeta({
      title: "PlaceholderAPI · StatFYR Docs",
      description:
        "Every StatFYR PlaceholderAPI placeholder: player statistics, K/D, playtime, sessions, rank, segments and server analytics.",
      path: "/docs/placeholders",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "Placeholders" },
      ]}
      title="PlaceholderAPI"
      lede="When PlaceholderAPI is installed the statfyr expansion registers automatically. Without it, StatFYR works exactly the same — PlaceholderAPI is never a hard dependency."
      toc={TOC}
    >
      <h2 id="overview">Overview</h2>
      <p>
        Self placeholders (no suffix) resolve against the requesting player. Append a player name to
        query someone else. All values come from the same analytics engine that powers the REST API.
      </p>

      <h2 id="player">Player placeholders</h2>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Placeholder</th><th>Value</th></tr></thead>
          <tbody>
            <tr><td><code>%statfyr_kills%</code></td><td>Player + mob kills.</td></tr>
            <tr><td><code>%statfyr_deaths%</code></td><td>Deaths.</td></tr>
            <tr><td><code>%statfyr_kdr%</code></td><td>Kill/death ratio (2 decimals).</td></tr>
            <tr><td><code>%statfyr_player_kills%</code></td><td>Player kills.</td></tr>
            <tr><td><code>%statfyr_mob_kills%</code></td><td>Mob kills.</td></tr>
            <tr><td><code>%statfyr_playtime%</code></td><td>Total playtime, human readable (e.g. <code>82h 14m</code>).</td></tr>
            <tr><td><code>%statfyr_active_time%</code></td><td>Non-AFK time.</td></tr>
            <tr><td><code>%statfyr_afk_time%</code></td><td>AFK time.</td></tr>
            <tr><td><code>%statfyr_afk%</code></td><td><code>yes</code> / <code>no</code>.</td></tr>
            <tr><td><code>%statfyr_sessions%</code></td><td>Number of sessions.</td></tr>
            <tr><td><code>%statfyr_blocks_mined%</code></td><td>Blocks mined.</td></tr>
            <tr><td><code>%statfyr_blocks_broken%</code></td><td>Tools/items broken.</td></tr>
            <tr><td><code>%statfyr_items_crafted%</code></td><td>Items crafted.</td></tr>
            <tr><td><code>%statfyr_distance_traveled%</code></td><td>Total distance in metres.</td></tr>
            <tr><td><code>%statfyr_rank%</code></td><td>All-time kills rank (<code>#12</code> or <code>unranked</code>).</td></tr>
            <tr><td><code>%statfyr_segment%</code></td><td>Player segment (<code>new</code>, <code>active</code>, <code>highly_active</code>, …).</td></tr>
            <tr><td><code>%statfyr_first_seen%</code></td><td>Relative first-seen time.</td></tr>
            <tr><td><code>%statfyr_last_seen%</code></td><td>Relative last-seen time.</td></tr>
          </tbody>
        </table>
      </div>

      <h2 id="parameterised">Other players</h2>
      <p>
        Append <code>_&lt;player&gt;</code> to any player placeholder. Metric names containing
        underscores (such as <code>active_time</code>) are matched longest-first, so they resolve
        correctly.
      </p>
      <CodeBlock
        lang="text"
        code={`%statfyr_kills_Steve%
%statfyr_playtime_Steve%
%statfyr_rank_Steve%
%statfyr_kdr_Steve%`}
      />

      <h2 id="server">Server placeholders</h2>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Placeholder</th><th>Value</th></tr></thead>
          <tbody>
            <tr><td><code>%statfyr_server_online%</code></td><td>Players online.</td></tr>
            <tr><td><code>%statfyr_server_peak_today%</code></td><td>Peak concurrent players today.</td></tr>
            <tr><td><code>%statfyr_server_peak_week%</code></td><td>Peak this week.</td></tr>
            <tr><td><code>%statfyr_server_peak_month%</code></td><td>Peak this month.</td></tr>
            <tr><td><code>%statfyr_server_peak_all_time%</code></td><td>All-time peak.</td></tr>
            <tr><td><code>%statfyr_server_unique_today%</code></td><td>Unique players today.</td></tr>
            <tr><td><code>%statfyr_server_unique_week%</code></td><td>Unique players this week.</td></tr>
            <tr><td><code>%statfyr_server_unique_month%</code></td><td>Unique players this month.</td></tr>
            <tr><td><code>%statfyr_server_total_players%</code></td><td>Total tracked players.</td></tr>
            <tr><td><code>%statfyr_server_sessions%</code></td><td>Total sessions recorded.</td></tr>
            <tr><td><code>%statfyr_server_playtime%</code></td><td>Total server playtime, human readable.</td></tr>
          </tbody>
        </table>
      </div>

      <h2 id="examples">Examples</h2>
      <CodeBlock
        lang="yaml"
        filename="Scoreboard / tab plugin"
        code={`lines:
  - "&6Kills: &f%statfyr_kills%"
  - "&6K/D: &f%statfyr_kdr%"
  - "&6Rank: &f%statfyr_rank%"
  - "&7Online: &a%statfyr_server_online% / &f%statfyr_server_peak_all_time% peak"`}
      />
      <Callout kind="note">
        PlaceholderAPI is optional. If it is not installed, these placeholders simply do not exist
        and StatFYR keeps running normally.
      </Callout>
    </DocsShell>
  );
}
