import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";
import { DocsShell } from "../components/layout";
import { Callout, CodeBlock } from "../components/ui";

const TOC = [
  { id: "file-location", label: "File location" },
  { id: "full-example", label: "Full example" },
  { id: "key-reference", label: "Key reference" },
  { id: "environment-overrides", label: "Environment overrides" },
  { id: "https-setup", label: "HTTPS setup" },
  { id: "pending-keys", label: "Reserved keys" },
  { id: "applying-changes", label: "Applying changes" },
];

export default function Configuration() {
  useEffect(() => {
    applyMeta({
      title: "Configuration Reference · Statfyr Docs",
      description:
        "Every plugins/statfyr/config.yml key with defaults and behavior: HTTP server, HTTPS, auth, rate limiting, CORS, IP whitelist, pagination, and cache.",
      path: "/docs/configuration",
    });
  }, []);

  return (
    <DocsShell
      crumbs={[
        { label: "Docs", to: "/docs" },
        { label: "v1.0.0-BETA" },
        { label: "Configuration" },
      ]}
      title="Configuration Reference"
      lede="statfyr is configured entirely through plugins/statfyr/config.yml. This page documents every key in the shipped default file and what each one actually does in v1.0.0-BETA."
      toc={TOC}
    >
      <h2 id="file-location">File location</h2>
      <p>
        <code>plugins/statfyr/config.yml</code>, created automatically on first boot with the
        defaults shown below. <code>config-version</code> is managed by the plugin; do not edit it.
      </p>

      <h2 id="full-example">Full example</h2>
      <p>This is the file the plugin ships, verbatim:</p>
      <CodeBlock
        lang="yaml"
        filename="plugins/statfyr/config.yml (default)"
        code={`# Statfyr Configuration
config-version: 1

http:
  port: 8080
  bind-address: "0.0.0.0"     # 0.0.0.0 = public, 127.0.0.1 = localhost only
  request-timeout-seconds: 15
  max-request-body-kb: 512

https:
  enabled: false
  keystore-path: "plugins/statfyr/keystore.jks"
  keystore-password: "changeit"

compression:
  enabled: true

async:
  enabled: true

docs:
  enabled: true

debug: false

security:
  enable-api-key: false
  api-key: ""

  enable-rate-limit: true
  rate-limit-requests: 120
  rate-limit-window-seconds: 60

  enable-cors: true
  allowed-origins:
    - "*"

  enable-ip-whitelist: false
  allowed-ips: []

query:
  max-limit: 100

pagination:
  default-limit: 25
  max-limit: 100

sorting:
  default-order: "desc"       # asc | desc

cache:
  ttl-seconds: 60
  refresh-seconds: 10`}
      />

      <h2 id="key-reference">Key reference</h2>
      <p>
        Status reflects v1.0.0-BETA behavior: <strong>enforced</strong> keys change runtime
        behavior today; <strong>reserved</strong> keys are read into the config but not yet wired
        to the feature they name.
      </p>
      <div className="table-scroll">
        <table className="doc-table">
          <thead>
            <tr><th>Key</th><th>Default</th><th>Status</th><th>Behavior</th></tr>
          </thead>
          <tbody>
            <tr><td><code>http.port</code></td><td>8080</td><td>Enforced</td><td>Port the embedded HTTP server binds (clamped to 1–65535).</td></tr>
            <tr><td><code>http.bind-address</code></td><td>0.0.0.0</td><td>Enforced</td><td>Bind address. Use 127.0.0.1 to keep the API local-only.</td></tr>
            <tr><td><code>http.request-timeout-seconds</code></td><td>15</td><td>Reserved</td><td>Loaded but not applied to exchanges yet.</td></tr>
            <tr><td><code>http.max-request-body-kb</code></td><td>512</td><td>Reserved</td><td>Loaded but not applied (all routes are GET with no body).</td></tr>
            <tr><td><code>https.enabled</code></td><td>false</td><td>Enforced</td><td>Serve over HTTPS using the JKS keystore instead of plain HTTP.</td></tr>
            <tr><td><code>https.keystore-path</code></td><td>plugins/statfyr/keystore.jks</td><td>Enforced</td><td>Path to the JKS keystore.</td></tr>
            <tr><td><code>https.keystore-password</code></td><td>changeit</td><td>Enforced</td><td>Keystore password; <code>STATFYR_KEYSTORE_PASSWORD</code> wins if set.</td></tr>
            <tr><td><code>compression.enabled</code></td><td>true</td><td>Reserved</td><td>Reported in <code>/api/health</code> features. Gzip is actually driven by each client's <code>Accept-Encoding</code>.</td></tr>
            <tr><td><code>async.enabled</code></td><td>true</td><td>Reserved</td><td>Reported in <code>/api/health</code> features. Request handling is always asynchronous.</td></tr>
            <tr><td><code>docs.enabled</code></td><td>true</td><td>Reserved</td><td>Reported in <code>/api/health</code> features. <code>/api</code> and <code>/api/docs</code> are always registered.</td></tr>
            <tr><td><code>debug</code></td><td>false</td><td>Enforced</td><td>Verbose request logging (method, path, status) to the server console.</td></tr>
            <tr><td><code>security.enable-api-key</code></td><td>false</td><td>Enforced</td><td>Require <code>Authorization: Bearer &lt;key&gt;</code>. Auth only activates when a non-blank key is configured.</td></tr>
            <tr><td><code>security.api-key</code></td><td>(empty)</td><td>Enforced</td><td>The Bearer key. <code>STATFYR_API_KEY</code> wins if set.</td></tr>
            <tr><td><code>security.enable-rate-limit</code></td><td>true</td><td>Enforced</td><td>Per-IP fixed-window rate limiting; excess requests get HTTP 429.</td></tr>
            <tr><td><code>security.rate-limit-requests</code></td><td>120</td><td>Enforced</td><td>Requests allowed per window per IP.</td></tr>
            <tr><td><code>security.rate-limit-window-seconds</code></td><td>60</td><td>Enforced</td><td>Window length in seconds.</td></tr>
            <tr><td><code>security.enable-cors</code></td><td>true</td><td>Enforced</td><td>Sends CORS headers and answers <code>OPTIONS</code> preflights with 204.</td></tr>
            <tr><td><code>security.allowed-origins</code></td><td>["*"]</td><td>Reserved</td><td>Loaded but not used for matching: when CORS is on, <code>Access-Control-Allow-Origin: *</code> is always sent.</td></tr>
            <tr><td><code>security.enable-ip-whitelist</code></td><td>false</td><td>Enforced</td><td>Reject clients whose IP is not in <code>allowed-ips</code> with HTTP 403.</td></tr>
            <tr><td><code>security.allowed-ips</code></td><td>[]</td><td>Enforced</td><td>Whitelist entries (exact IPs).</td></tr>
            <tr><td><code>query.max-limit</code></td><td>100</td><td>Reserved</td><td>Loaded but unused; <code>pagination.max-limit</code> is what clamps <code>limit</code>.</td></tr>
            <tr><td><code>pagination.default-limit</code></td><td>25</td><td>Enforced</td><td>Default page size for <code>/api/players</code> and leaderboards.</td></tr>
            <tr><td><code>pagination.max-limit</code></td><td>100</td><td>Enforced</td><td>Maximum accepted <code>limit</code>; larger values are clamped.</td></tr>
            <tr><td><code>sorting.default-order</code></td><td>desc</td><td>Enforced</td><td>Default sort direction when <code>order</code> is omitted.</td></tr>
            <tr><td><code>cache.ttl-seconds</code></td><td>60</td><td>Reserved</td><td>Loaded but unused; the statistic cache TTL is hard-coded at 30 seconds.</td></tr>
            <tr><td><code>cache.refresh-seconds</code></td><td>10</td><td>Reserved</td><td>Loaded but unused; the background refresh runs every 5 seconds.</td></tr>
          </tbody>
        </table>
      </div>

      <h2 id="environment-overrides">Environment overrides</h2>
      <p>Two secrets can be supplied through the environment instead of the config file:</p>
      <div className="table-scroll">
        <table className="doc-table">
          <thead><tr><th>Variable</th><th>Overrides</th><th>Use</th></tr></thead>
          <tbody>
            <tr><td><code>STATFYR_API_KEY</code></td><td><code>security.api-key</code></td><td>Keep the Bearer key out of config.yml and backups.</td></tr>
            <tr><td><code>STATFYR_KEYSTORE_PASSWORD</code></td><td><code>https.keystore-password</code></td><td>Keep the keystore password out of config.yml and backups.</td></tr>
          </tbody>
        </table>
      </div>

      <h2 id="https-setup">HTTPS setup</h2>
      <p>
        statfyr serves HTTPS from a Java JKS keystore. Generate one with <code>keytool</code>
        (ships with your JDK), then flip <code>https.enabled</code> to <code>true</code> and
        restart:
      </p>
      <CodeBlock
        lang="bash"
        code={`keytool -genkeypair -alias statfyr \\
  -keyalg RSA -keysize 2048 \\
  -storetype JKS \\
  -keystore keystore.jks \\
  -validity 3650`}
      />
      <Callout kind="note">
        For public deployments, a reverse proxy (nginx, Caddy) with a real certificate is usually
        simpler than a self-signed JKS: run statfyr on <code>127.0.0.1</code> and let the proxy
        terminate TLS.
      </Callout>

      <h2 id="pending-keys">Reserved keys</h2>
      <Callout kind="todo">
        Keys marked <strong>reserved</strong> are read by <code>ConfigManager</code> but not yet
        enforced: <code>http.request-timeout-seconds</code>, <code>http.max-request-body-kb</code>,{" "}
        <code>compression.enabled</code>, <code>async.enabled</code>, <code>docs.enabled</code>,{" "}
        <code>security.allowed-origins</code>, <code>query.max-limit</code>,{" "}
        <code>cache.ttl-seconds</code>, <code>cache.refresh-seconds</code>. TODO(owner): either wire
        these up or drop them from the file; this page will track whichever way the plugin goes.
      </Callout>

      <h2 id="applying-changes">Applying changes</h2>
      <p>
        Run <code>/statfyr reload</code> (permission <code>statfyr.admin</code>) to re-read the
        file without a restart, or <code>/statfyr status</code> to confirm what the server picked
        up: bind address, port, HTTPS, auth, rate limiting, and cache state.
      </p>
    </DocsShell>
  );
}
