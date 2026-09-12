import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";
import { Marquee, Meteors, DotPattern, GlowOrb } from "../components/magicui";
import { CodeBlock, Eyebrow, StatTile } from "../components/ui";

/* Demo numbers are plausible beta-stage values; field names are the real
   summary-response keys from PlayerSummaryHandler. */
const TILES = [
  { icon: "BLK", label: "Blocks mined", value: 318402, field: "summary.blocksMined" },
  { icon: "KIL", label: "Mob kills", value: 3140, field: "summary.mobKills" },
  { icon: "DTH", label: "Deaths", value: 128, field: "summary.deaths" },
  { icon: "KM", label: "Distance", value: 4820, suffix: "km", field: "summary.distanceKm" },
  { icon: "CRF", label: "Items crafted", value: 21843, field: "summary.itemsCrafted" },
  { icon: "CHST", label: "Chests opened", value: 7433, field: "summary.chestsOpened" },
];

const MARQUEE_KEYS = [
  "minecraft:custom",
  "minecraft:mined",
  "minecraft:crafted",
  "minecraft:used",
  "minecraft:broken",
  "minecraft:picked_up",
  "minecraft:dropped",
  "minecraft:killed",
  "minecraft:killed_by",
  "playtime",
  "player_kills",
  "mob_kills",
  "blocks_mined",
  "items_picked_up",
  "items_crafted",
];

const FEATURES = [
  {
    title: "Live and offline players",
    body: "Online players read straight from the Bukkit statistic API; offline players from the world's stats JSON files. One API covers both.",
  },
  {
    title: "Nine vanilla categories",
    body: "custom, mined, crafted, used, broken, picked_up, dropped, killed, killed_by, plus custom stats registered by other plugins.",
  },
  {
    title: "Safe to poll",
    body: "A 30-second statistic cache and per-IP rate limiting keep request costs flat even while dashboards poll aggressively.",
  },
];

export default function Landing() {
  useEffect(() => {
    applyMeta({
      title: "Statfyr · REST API plugin for Minecraft server statistics",
      description:
        "Blazing-fast REST API plugin for Minecraft exposing player statistics (kills, playtime, blocks mined and more) through a clean documented HTTP interface.",
      path: "/",
    });
  }, []);

  return (
    <>
      {/* ---------------------------------------------------------- hero */}
      <section className="relative overflow-hidden">
        <DotPattern className="[mask-image:radial-gradient(700px_circle_at_50%_18%,white,transparent_72%)]" />
        <div className="pointer-events-none absolute inset-0" aria-hidden>
          <GlowOrb className="-top-32 left-1/2 -translate-x-1/2" color="rgba(16,185,129,0.14)" />
          <GlowOrb className="right-[-140px] top-40" color="rgba(139,92,246,0.16)" size={440} />
          <Meteors number={12} />
        </div>

        <div className="relative mx-auto max-w-4xl px-6 pb-20 pt-20 text-center sm:pt-28">
          <div className="hero-enter">
            <span className="hero-badge">
              <span className="pulse-dot" aria-hidden />
              v1.0.0-BETA · public beta on Modrinth
            </span>
          </div>
          <h1
            className="hero-enter mt-6 text-[clamp(2.5rem,6vw,4.2rem)] font-extrabold leading-[1.06] tracking-[-0.03em]"
            style={{ animationDelay: "0.08s" }}
          >
            Your server's statistics, <span className="grad-text-mc">as one JSON API</span>
          </h1>
          <p
            className="hero-enter mx-auto mt-6 max-w-2xl text-[1.05em] leading-relaxed text-[#b9bfd4]"
            style={{ animationDelay: "0.16s" }}
          >
            Statfyr embeds an HTTP server inside your Minecraft server and serves every player
            statistic, live or offline, as JSON. Dashboards, leaderboards, and Discord bots read
            from one place instead of scraping logs or poking the database.
          </p>
          <div
            className="hero-enter mt-8 flex flex-wrap items-center justify-center gap-3"
            style={{ animationDelay: "0.24s" }}
          >
            <a
              className="btn btn-primary"
              href="https://modrinth.com/plugin/statfyr"
              target="_blank"
              rel="noopener noreferrer"
            >
              Get statfyr on Modrinth
            </a>
            <Link className="btn btn-ghost" to="/docs/getting-started">
              Read the quick start
            </Link>
          </div>
          <div
            className="hero-enter mt-10 flex flex-wrap items-center justify-center gap-x-4 gap-y-2 font-mono text-[11px] text-[#6a7089]"
            style={{ animationDelay: "0.3s" }}
          >
            <span className="flex items-center gap-1.5"><span className="text-[#34d399]">GET</span> /api/health</span>
            <span className="flex items-center gap-1.5"><span className="text-[#34d399]">GET</span> /api/players</span>
            <span className="flex items-center gap-1.5"><span className="text-[#34d399]">GET</span> /api/player/&lt;id&gt;</span>
            <span className="flex items-center gap-1.5"><span className="text-[#34d399]">GET</span> /api/leaderboard/&lt;stat&gt;</span>
          </div>
        </div>
      </section>

      {/* ---------------------------------------------------- stats demo */}
      <section className="relative mx-auto max-w-6xl px-6 py-16">
        <div className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
          <div>
            <Eyebrow>What one call returns</Eyebrow>
            <h2 className="mt-3 text-[clamp(1.6rem,3vw,2.2rem)] font-extrabold tracking-tight text-white">
              One GET, the whole profile
            </h2>
          </div>
          <p className="max-w-md text-sm text-[#9aa0b4]">
            <code>GET /api/player/&lt;id&gt;/summary</code> folds hundreds of raw Bukkit statistics
            into a dashboard-ready object. These tiles are rendered from its real field names.
          </p>
        </div>

        <div className="mt-10 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {TILES.map((t) => (
            <StatTile
              key={t.field}
              icon={t.icon}
              label={t.label}
              value={t.value}
              suffix={t.suffix}
              field={t.field}
            />
          ))}
        </div>

        <div className="mt-8 grid gap-4 lg:grid-cols-2">
          <div>
            <p className="mono-label mb-2">Request</p>
            <CodeBlock
              lang="bash"
              code={`curl http://localhost:8080/api/player/Notch/summary \\
  -H "Authorization: Bearer YOUR_API_KEY"`}
            />
          </div>
          <div>
            <p className="mono-label mb-2">Response</p>
            <CodeBlock
              lang="json"
              code={`{
  "playtime_formatted": "15d 4h 50m",
  "combat": { "deaths": 128, "player_kills": 41 },
  "movement": { "total_distance_km": 4820.34, "jumps": 92044 },
  "activity": { "blocks_mined": 318402, "items_crafted": 21843 }
}`}
            />
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------- marquee */}
      <section className="border-y border-line-light bg-white/[0.015] py-8">
        <Marquee>
          {MARQUEE_KEYS.map((k) => (
            <span key={k} className="chip">{k}</span>
          ))}
        </Marquee>
      </section>

      {/* ------------------------------------------------------ features */}
      <section className="mx-auto max-w-6xl px-6 py-16">
        <Eyebrow>Why statfyr</Eyebrow>
        <h2 className="mt-3 text-[clamp(1.6rem,3vw,2.2rem)] font-extrabold tracking-tight text-white">
          Built for owners who ship dashboards
        </h2>
        <div className="mt-10 grid gap-4 md:grid-cols-3">
          {FEATURES.map((f) => (
            <div key={f.title} className="doc-card">
              <h3 className="!mt-1 !text-[16px] !text-white">{f.title}</h3>
              <p className="!text-sm">{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ----------------------------------------------------------- CTA */}
      <section className="mx-auto max-w-6xl px-6 pb-24">
        <div className="doc-card items-center !gap-0 p-10 text-center">
          <h2 className="!mt-0 !text-[1.6em] font-extrabold !text-white">Point a dashboard at it</h2>
          <p className="mx-auto mt-3 max-w-lg text-sm text-[#9aa0b4]">
            Install the plugin, set an API key, and start polling. The full endpoint reference
            includes copy-paste curl for every route.
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Link className="btn btn-primary btn-sm" to="/docs/getting-started">Start in five minutes</Link>
            <Link className="btn btn-ghost btn-sm" to="/docs/api">API reference</Link>
          </div>
        </div>
      </section>
    </>
  );
}
