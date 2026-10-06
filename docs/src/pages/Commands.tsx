import { useEffect } from "react";
import { applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "player-commands", label: "Player commands" },
  { id: "admin-commands", label: "Admin commands" },
  { id: "permissions", label: "Permissions" },
  { id: "luckperms", label: "LuckPerms" },
  { id: "tab-completion", label: "Tab completion" },
  { id: "aliases", label: "Aliases" },
];

export default function Commands() {
  useEffect(() => {
    applyMeta({
      title: "Commands & Permissions · StatFYR Docs",
      description:
        "Every StatFYR command, permission node, LuckPerms example, tab-completion behaviour, and the /sf alias.",
      path: "/docs/commands",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "Commands & Permissions" },
      ]}
      title="Commands & Permissions"
      lede="StatFYR exposes a focused, permission-gated command surface. Everything works with vanilla Bukkit permissions and is fully compatible with LuckPerms."
      toc={TOC}
    >
      <h2 id="player-commands">Player commands</h2>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Command</th><th>Description</th></tr></thead>
          <tbody>
            <tr><td><code>/statfyr stats</code></td><td>Your own statistics, including derived values (K/D, active time, sessions, segment).</td></tr>
            <tr><td><code>/statfyr stats &lt;player&gt;</code></td><td>Another player's statistics. Offline players are supported.</td></tr>
            <tr><td><code>/statfyr top</code></td><td>Default leaderboard (kills, all-time).</td></tr>
            <tr><td><code>/statfyr top &lt;stat&gt;</code></td><td>Leaderboard for a metric, e.g. <code>kills</code>, <code>playtime</code>, <code>mined</code>, <code>kdr</code>, <code>sessions</code>.</td></tr>
            <tr><td><code>/statfyr top &lt;stat&gt; &lt;period&gt;</code></td><td>Time-scoped leaderboard: <code>daily</code>, <code>weekly</code>, <code>monthly</code>, <code>alltime</code>.</td></tr>
            <tr><td><code>/statfyr help</code></td><td>Lists every command you have permission to run.</td></tr>
          </tbody>
        </table>
      </div>
      <Callout kind="note">
        Output is fully configurable in <code>plugins/statfyr/messages.yml</code> using{" "}
        <code>&amp;</code> colour codes and <code>{"{placeholder}"}</code> tokens.
      </Callout>

      <h2 id="admin-commands">Admin commands</h2>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Command</th><th>Description</th></tr></thead>
          <tbody>
            <tr><td><code>/statfyr reload</code></td><td>Reload config, messages, analytics settings and the HTTP server in place.</td></tr>
            <tr><td><code>/statfyr status</code></td><td>HTTP state, port, platform, tracked profiles and online players.</td></tr>
            <tr><td><code>/statfyr database</code></td><td>Storage statistics: profiles, retention, known players, sessions.</td></tr>
            <tr><td><code>/statfyr purge &lt;player&gt;</code></td><td>Delete a player's analytics data (profile, history, activity).</td></tr>
            <tr><td><code>/statfyr purge-history</code></td><td>Delete all historical snapshots and activity (profiles are kept).</td></tr>
            <tr><td><code>/statfyr debug</code></td><td>Dump analytics diagnostics and integration state.</td></tr>
          </tbody>
        </table>
      </div>
      <Callout kind="warn">
        Destructive commands (<code>purge</code>, <code>purge-history</code>) are protected by{" "}
        <code>statfyr.admin</code> and never exposed to normal users.
      </Callout>

      <h2 id="permissions">Permissions</h2>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Permission</th><th>Default</th><th>Grants</th></tr></thead>
          <tbody>
            <tr><td><code>statfyr.*</code></td><td>op</td><td>Every StatFYR permission.</td></tr>
            <tr><td><code>statfyr.use</code></td><td>everyone</td><td>Basic StatFYR usage.</td></tr>
            <tr><td><code>statfyr.stats</code></td><td>everyone</td><td>View statistics (implies <code>statfyr.stats.self</code>).</td></tr>
            <tr><td><code>statfyr.stats.self</code></td><td>everyone</td><td><code>/statfyr stats</code> for yourself.</td></tr>
            <tr><td><code>statfyr.stats.others</code></td><td>op</td><td><code>/statfyr stats &lt;player&gt;</code>.</td></tr>
            <tr><td><code>statfyr.top</code></td><td>everyone</td><td>All leaderboards.</td></tr>
            <tr><td><code>statfyr.top.&lt;stat&gt;</code></td><td>op</td><td>A single leaderboard, e.g. <code>statfyr.top.kills</code>.</td></tr>
            <tr><td><code>statfyr.help</code></td><td>everyone</td><td>The help menu.</td></tr>
            <tr><td><code>statfyr.admin</code></td><td>op</td><td>Administrative commands.</td></tr>
            <tr><td><code>statfyr.reload</code></td><td>op</td><td><code>/statfyr reload</code>.</td></tr>
            <tr><td><code>statfyr.status</code></td><td>op</td><td><code>/statfyr status</code>.</td></tr>
            <tr><td><code>statfyr.database</code></td><td>op</td><td><code>/statfyr database</code>.</td></tr>
            <tr><td><code>statfyr.purge</code></td><td>op</td><td>Purge commands.</td></tr>
            <tr><td><code>statfyr.debug</code></td><td>op</td><td><code>/statfyr debug</code>.</td></tr>
          </tbody>
        </table>
      </div>

      <h2 id="luckperms">LuckPerms</h2>
      <p>
        StatFYR uses the standard Bukkit permission API, so LuckPerms manages it naturally with no
        plugin dependency.
      </p>
      <CodeBlock
        lang="bash"
        filename="LuckPerms"
        code={`/lp group default permission set statfyr.use true
/lp group default permission set statfyr.stats true
/lp group default permission set statfyr.top true
/lp group moderator permission set statfyr.stats.others true
/lp group moderator permission set statfyr.admin true`}
      />

      <h2 id="tab-completion">Tab completion</h2>
      <p>Suggestions are permission-aware and never reveal commands a sender cannot run:</p>
      <ul>
        <li><code>/statfyr &lt;TAB&gt;</code> → subcommands the sender may use.</li>
        <li><code>/statfyr top &lt;TAB&gt;</code> → metric keys.</li>
        <li><code>/statfyr top kills &lt;TAB&gt;</code> → periods (<code>daily</code>, <code>weekly</code>, <code>monthly</code>, <code>alltime</code>).</li>
        <li><code>/statfyr stats &lt;TAB&gt;</code> / <code>/statfyr purge &lt;TAB&gt;</code> → known player names.</li>
      </ul>

      <h2 id="aliases">Aliases</h2>
      <p>
        <code>/sf</code> is registered in <code>plugin.yml</code> and can be extended in{" "}
        <code>config.yml</code>:
      </p>
      <CodeBlock
        lang="yaml"
        code={`commands:
  aliases: [ sf ]`}
      />
    </DocsShell>
  );
}
