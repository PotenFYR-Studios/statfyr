import { useEffect } from "react";
import { Link, applyMeta } from "../lib/router";
import { Eyebrow } from "../components/ui";

export default function About() {
  useEffect(() => {
    applyMeta({
      title: "About · Statfyr",
      description:
        "Statfyr is a REST API plugin for Minecraft by PotenFYR Studios, Apache-2.0 licensed with the Commons Clause and in public beta.",
      path: "/about",
    });
  }, []);

  return (
    <div className="mx-auto max-w-3xl px-6 pb-24 pt-16">
      <Eyebrow>About</Eyebrow>
      <h1 className="mt-3 text-[clamp(2rem,4.5vw,3rem)] font-extrabold tracking-[-0.02em] text-white">
        Statistics shouldn't be trapped in a stats.json
      </h1>
      <div className="doc-article mt-8">
        <p>
          Statfyr is a plugin by <strong>PotenFYR Studios</strong> that embeds a REST API directly
          inside your Minecraft server. It exists because player data has always been awkward to
          reach: buried in <code>world/stats/&lt;uuid&gt;.json</code> files, or locked behind
          plugins that only write to databases you'd have to mirror.
        </p>
        <p>
          Instead of asking you to export anything, statfyr serves your server's statistics over
          HTTP the moment the plugin loads: live for online players, parsed from the vanilla stat
          files for offline ones. Nine vanilla statistic categories, computed summaries,
          leaderboards, Bearer auth, rate limiting, and gzip compression are all in the box.
        </p>

        <h2>Project status</h2>
        <p>
          Statfyr is a <strong>public beta</strong> (v1.0.0-BETA). The API surface is stable enough
          to build on, but endpoints and config keys can still change before 1.0. A handful of
          config keys are read but not yet enforced; each is flagged in the
          <Link to="/docs/configuration"> configuration reference</Link>.
        </p>

        <h2>License</h2>
        <p>
          Apache-2.0 with the Commons Clause: free to use, modify, and embed in your own projects,
          including commercial ones; the only thing you cannot do is sell Statfyr itself. Read the
          plain-terms summary on the <Link to="/license">license page</Link>. The license text in
          the <a href="https://github.com/PotenFYR-Studios/statfyr/blob/master/LICENSE" target="_blank" rel="noopener noreferrer">repository</a>{" "}
          is authoritative.
        </p>

        <h2>The studio</h2>
        <p>
          PotenFYR Studios builds tools for Minecraft communities: servers, sites, and the
          plumbing in between. Statfyr is part of a growing set of projects; more at
          <a href="https://potenfyr.in" target="_blank" rel="noopener noreferrer"> potenfyr.in</a>.
        </p>

        <h2>Talk to us</h2>
        <ul>
          <li>Bugs and features: <a href="https://github.com/PotenFYR-Studios/statfyr/issues" target="_blank" rel="noopener noreferrer">GitHub Issues</a></li>
          <li>Security: the confidential channel in <a href="https://github.com/PotenFYR-Studios/statfyr/blob/master/SECURITY.md" target="_blank" rel="noopener noreferrer">SECURITY.md</a></li>
          <li>Downloads: <a href="https://modrinth.com/plugin/statfyr" target="_blank" rel="noopener noreferrer">Modrinth</a></li>
        </ul>
      </div>
    </div>
  );
}
