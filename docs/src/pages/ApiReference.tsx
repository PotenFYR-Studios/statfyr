import { useEffect } from "react";
import { applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { EndpointCard, CodeBlock, Callout } from "../components/ui";
import { ENDPOINTS, LEADERBOARD_STATS, ERROR_SHAPE, STATUS_CODES, API_BASE_NOTE } from "../content/api-data";

const TOC = [
  { id: "base-note", label: "Base URL & auth" },
  ...ENDPOINTS.map((e) => ({ id: e.id, label: e.path })),
  { id: "leaderboard-stats", label: "Leaderboard stats" },
  { id: "error-shape", label: "Error shape" },
  { id: "status-codes", label: "Status codes" },
];

export default function ApiReference() {
  useEffect(() => {
    applyMeta({
      title: "API Reference · Statfyr Docs",
      description:
        "Every Statfyr REST endpoint documented with real request/response JSON: health, players, player stats, summaries, and leaderboards.",
      path: "/docs/api",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "API Reference" },
      ]}
      title="API Reference"
      lede="Seven GET endpoints, all returning application/json. Each card below mirrors the handler as it ships."
      toc={TOC}
    >
      <h2 id="base-note">Base URL & auth</h2>
      <p>{API_BASE_NOTE}</p>
      <ul>
        <li>All routes are <strong>GET-only</strong>; any other method gets 405.</li>
        <li>When <code>security.enable-api-key</code> is on with a non-blank key, every route requires <code>Authorization: Bearer &lt;key&gt;</code>.</li>
        <li>When <code>security.enable-cors</code> is on, <code>OPTIONS</code> preflights return 204.</li>
      </ul>

      {ENDPOINTS.map((ep) => (
        <div key={ep.id} id={ep.id} className="mb-6">
          <EndpointCard
            method={ep.method}
            path={ep.path}
            title={ep.title}
            description={ep.description}
            auth={ep.auth}
            params={ep.params && ep.params.length > 0 ? ep.params : undefined}
          >
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <div>
                <p className="mono-label mb-2">Request</p>
                <CodeBlock lang="bash" code={ep.request} />
              </div>
              <div>
                <p className="mono-label mb-2">Response</p>
                <CodeBlock lang="json" code={ep.response} />
              </div>
            </div>
            {ep.notes && ep.notes.length > 0 && (
              <Callout kind="note">
                <ul className="!mt-0 !mb-0">
                  {ep.notes.map((n, i) => <li key={i}>{n}</li>)}
                </ul>
              </Callout>
            )}
          </EndpointCard>
        </div>
      ))}

      <h2 id="leaderboard-stats">Leaderboard stat keys</h2>
      <p>
        Built-in stat names (bare words), dotted <code>category.key</code> aliases, full vanilla
        keys like <code>minecraft:mined:minecraft:sand</code>, and custom plugin stats are all
        accepted. These seven are the hard-coded entries:
      </p>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Key</th><th>Measures</th></tr></thead>
          <tbody>
            {LEADERBOARD_STATS.map((s) => (
              <tr key={s.key}><td><code>{s.key}</code></td><td>{s.meaning}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
      <Callout kind="note">
        <code>playtime</code> is the only leaderboard that appends a <code>formatted</code> field
        (human-readable duration). All other stats return raw integer <code>value</code>.
      </Callout>

      <h2 id="error-shape">Error shape</h2>
      <p>Every error response (except the "Stats not found" soft error) follows this schema:</p>
      <CodeBlock lang="json" code={ERROR_SHAPE} />

      <h2 id="status-codes">Status codes</h2>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Code</th><th>Meaning</th></tr></thead>
          <tbody>
            {STATUS_CODES.map((s) => (
              <tr key={s.code}><td><code>{s.code}</code></td><td>{s.meaning}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </DocsShell>
  );
}
