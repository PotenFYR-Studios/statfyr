import { useEffect } from "react";
import { Link, DOC_ROUTES, applyMeta, type DocRoute } from "../lib/router";
import { Eyebrow } from "../components/ui";

const GROUP_ORDER: DocRoute["group"][] = ["Get started", "Reference", "Project"];

export default function DocsPortal() {
  useEffect(() => {
    applyMeta({
      title: "Statfyr Documentation",
      description:
        "Guides and reference for the Statfyr REST API plugin: installation, configuration, every endpoint, integration examples, and FAQ.",
      path: "/docs",
    });
  }, []);

  return (
    <div className="mx-auto max-w-6xl px-6 pb-24 pt-16">
      <Eyebrow>Documentation</Eyebrow>
      <h1 className="mt-3 text-[clamp(2rem,4.5vw,3rem)] font-extrabold tracking-[-0.02em] text-white">
        Everything statfyr can do, documented
      </h1>
      <p className="mt-4 max-w-2xl text-[1.02em] text-[#b9bfd4]">
        Start with installation, tune <code>config.yml</code>, then wire your first endpoint into a
        dashboard. Every example on these pages mirrors the plugin's actual responses.
      </p>

      <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {GROUP_ORDER.map((group) =>
          DOC_ROUTES.filter((r) => r.group === group).map((r) => (
            <Link key={r.path} to={r.path} className="doc-card group">
              <span className="mono-label">{group}</span>
              <span className="mt-1 text-[1.05em] font-semibold text-white group-hover:text-[#c4b5fd]">
                {r.title}
              </span>
              <span className="text-sm text-[#9aa0b4]">{r.short}</span>
              <span className="card-arrow" aria-hidden>→</span>
            </Link>
          )),
        )}
      </div>

      <div className="mt-10 flex flex-wrap gap-3">
        <a
          className="btn btn-ghost btn-sm"
          href="https://modrinth.com/plugin/statfyr"
          target="_blank"
          rel="noopener noreferrer"
        >
          Download from Modrinth
        </a>
        <a
          className="btn btn-ghost btn-sm"
          href="https://github.com/PotenFYR-Studios/statfyr/issues"
          target="_blank"
          rel="noopener noreferrer"
        >
          Report an issue
        </a>
      </div>
    </div>
  );
}
