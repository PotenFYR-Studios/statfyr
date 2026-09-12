import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "requirements", label: "Requirements" },
  { id: "install", label: "Install" },
  { id: "secure", label: "Secure the API" },
  { id: "verify", label: "Verify" },
  { id: "whats-next", label: "What's next" },
];

export default function GettingStarted() {
  useEffect(() => {
    applyMeta({
      title: "Getting Started · Statfyr Docs",
      description:
        "Install the Statfyr REST API plugin on a Paper/Spigot server and verify your first API call in five minutes.",
      path: "/docs/getting-started",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "Getting Started" },
      ]}
      title="Getting Started"
      lede="Get the plugin on your server, set an API key, and make your first API call, about five minutes end to end."
      toc={TOC}
    >
      <h2 id="requirements">Requirements</h2>
      <ul>
        <li>A Paper, Spigot, or Purpur server running Minecraft 1.16 or newer (built against Paper 1.16.5).</li>
        <li>Java 16 or newer on the server host.</li>
        <li>A free TCP port reachable from wherever your clients run (default <code>8080</code>).</li>
      </ul>

      <h2 id="install">Install</h2>
      <p>
        Download the latest <code>statfyr-1.0.0-BETA.jar</code> from
        <a href="https://modrinth.com/plugin/statfyr" target="_blank" rel="noopener noreferrer"> Modrinth </a>
        and drop it into your server's <code>plugins/</code> folder, then restart the server. On
        first boot statfyr writes its default <code>plugins/statfyr/config.yml</code> and starts
        listening immediately.
      </p>
      <Callout kind="note">
        Commands: <code>/statfyr reload</code> re-reads config.yml, <code>/statfyr status</code> shows
        bind address, port, HTTPS state, auth, and cache status. Both need the{" "}
        <code>statfyr.admin</code> permission (granted to ops by default).
      </Callout>

      <h2 id="secure">Secure the API</h2>
      <p>
        The API is open by default (anyone who can reach the port can read stats). Before exposing
        it beyond localhost, set a Bearer key in <code>plugins/statfyr/config.yml</code>:
      </p>
      <CodeBlock
        lang="yaml"
        filename="plugins/statfyr/config.yml"
        code={`security:
  enable-api-key: true
  api-key: "pick-a-long-random-string"`}
      />
      <Callout kind="warn">
        Do not commit this key anywhere public. Clients send it as{" "}
        <code>Authorization: Bearer &lt;key&gt;</code> on every request. The key can also be
        supplied (or overridden) with the <code>STATFYR_API_KEY</code> environment variable.
      </Callout>

      <h2 id="verify">Verify</h2>
      <p>From the server host, confirm the API answers:</p>
      <CodeBlock
        lang="bash"
        code={`curl http://localhost:8080/api/health \\
  -H "Authorization: Bearer pick-a-long-random-string"`}
      />
      <p>
        A JSON object with <code>system.status</code> set to <code>"ok"</code> means statfyr is up.
        Then try your first player lookup: substitute a real player name:
      </p>
      <CodeBlock
        lang="bash"
        code={`curl http://localhost:8080/api/player/Notch/summary \\
  -H "Authorization: Bearer pick-a-long-random-string"`}
      />
      <p>
        The response shape is documented route by route on the
        <Link to="/docs/api"> API reference</Link> page.
      </p>

      <h2 id="whats-next">What's next</h2>
      <ul>
        <li>Tune ports, HTTPS, caching, and rate limits in the <Link to="/docs/configuration">configuration reference</Link>.</li>
        <li>Browse every route with real payloads in the <Link to="/docs/api">API reference</Link>.</li>
        <li>Copy a dashboard, leaderboard, or Discord bot recipe from <Link to="/docs/examples">examples</Link>.</li>
      </ul>
    </DocsShell>
  );
}
