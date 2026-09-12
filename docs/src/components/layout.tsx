import { useEffect, useMemo, useRef, useState } from "react";
import { Link, usePath, navigate, DOC_ROUTES, docIndex, type DocRoute } from "../lib/router";
import { Eyebrow } from "./ui";

/* ------------------------------------------------------------------ brand */

function BrandMark() {
  return (
    <img
      src="/favicon.png"
      alt=""
      width={22}
      height={22}
      className="brand-mark"
    />
  );
}

export function Brand() {
  return (
    <Link to="/" className="flex items-center gap-2.5 no-underline" ariaLabel="Statfyr home">
      <BrandMark />
      <span className="text-[17px] font-bold tracking-tight">
        statfyr<span className="grad-text">.</span>
      </span>
      <span className="chip hidden sm:inline-block">v1.0.0-BETA</span>
    </Link>
  );
}

/* ----------------------------------------------------------------- header */

const NAV = [
  { to: "/docs", label: "Docs" },
  { to: "/docs/api", label: "API" },
  { to: "/examples", label: "Examples" },
  { to: "/docs/faq", label: "FAQ" },
  { to: "/about", label: "About" },
];

function Header({ onOpenPalette }: { onOpenPalette: () => void }) {
  const path = usePath();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(false);
  }, [path]);

  return (
    <header className="site-header">
      <Brand />
      <nav className="ml-4 hidden items-center gap-1 md:flex" aria-label="Primary">
        {NAV.map((n) => {
          const active =
            n.to === "/docs" ? path === "/docs" : path.startsWith(n.to);
          return (
            <Link
              key={n.to}
              to={n.to}
              className={`nav-link ${active ? "active" : ""}`}
            >
              {n.label}
            </Link>
          );
        })}
      </nav>
      <div className="ml-auto flex items-center gap-3">
        <button
          type="button"
          onClick={onOpenPalette}
          className="hidden items-center gap-2 rounded-full border border-line-light bg-white/[0.03] px-3 py-1 font-mono text-[11px] text-[#9aa0b4] transition-colors hover:border-[rgba(139,92,246,0.5)] hover:text-white md:flex"
          aria-label="Open command palette"
        >
          <span>Search</span>
          <kbd className="rounded border border-line-light bg-white/[0.04] px-1.5 py-0.5 text-[10px]">⌘K</kbd>
        </button>
        <a
          href="https://modrinth.com/plugin/statfyr"
          target="_blank"
          rel="noopener noreferrer"
          className="header-ext hidden sm:inline"
        >
          Modrinth ↗
        </a>
        <a
          href="https://github.com/PotenFYR-Studios/statfyr"
          target="_blank"
          rel="noopener noreferrer"
          className="header-ext hidden sm:inline"
        >
          GitHub ↗
        </a>
        <button
          type="button"
          className="md:hidden rounded-md border border-line-light bg-white/[0.03] px-2.5 py-1.5 text-sm"
          aria-label="Toggle navigation menu"
          aria-expanded={open}
          onClick={() => setOpen(!open)}
        >
          ☰
        </button>
      </div>
      {open && (
        <nav
          className="absolute inset-x-0 top-full z-40 border-b border-line-light bg-[#0e1120]/95 px-4 py-3 backdrop-blur md:hidden"
          aria-label="Mobile"
        >
          {NAV.map((n) => (
            <Link key={n.to} to={n.to} className="block rounded-md px-2 py-2 text-sm text-[#b9bfd4] hover:bg-white/5 hover:text-white">
              {n.label}
            </Link>
          ))}
        </nav>
      )}
    </header>
  );
}

/* ----------------------------------------------------------------- footer */

function Footer() {
  return (
    <footer className="site-footer">
      <div className="sf-inner">
        <div className="flex flex-col gap-8 md:flex-row md:items-start md:justify-between">
          <div className="max-w-sm">
            <Brand />
            <p className="mt-3 text-sm text-[#9aa0b4]">
              REST API plugin for Minecraft servers. Every statistic, one clean JSON API.
            </p>
          </div>
          <div className="flex flex-col items-start gap-2.5 md:items-end">
            <div className="flex flex-wrap gap-x-5 gap-y-2 md:justify-end">
              <a className="footer-link" href="https://github.com/PotenFYR-Studios" target="_blank" rel="noopener noreferrer">GitHub Org</a>
              <a className="footer-link" href="https://potenfyr.in" target="_blank" rel="noopener noreferrer">potenfyr.in</a>
              <a className="footer-link" href="https://modrinth.com/plugin/statfyr" target="_blank" rel="noopener noreferrer">Modrinth</a>
              <a className="footer-link" href="/docs" onClick={(e) => { e.preventDefault(); navigate("/docs"); }}>Docs</a>
              <a className="footer-link" href="/examples" onClick={(e) => { e.preventDefault(); navigate("/examples"); }}>Examples</a>
              <a className="footer-link" href="/license" onClick={(e) => { e.preventDefault(); navigate("/license"); }}>License</a>
            </div>
            <p className="text-xs text-[#6a7089]">
              © 2026 PotenFYR Studios.{" "}
              <a className="underline decoration-[#3a3f55] underline-offset-2 hover:decoration-[#8b5cf6]" href="/license" onClick={(e) => { e.preventDefault(); navigate("/license"); }}>Apache-2.0 with the Commons Clause</a>
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}

/* ------------------------------------------------------------------- TOC */

export type TocItem = { id: string; label: string; h3?: boolean };

function TocRail({ items }: { items: TocItem[] }) {
  const [active, setActive] = useState(items[0]?.id ?? "");

  useEffect(() => {
    setActive(items[0]?.id ?? "");
    const headings = items
      .map((t) => document.getElementById(t.id))
      .filter((el): el is HTMLElement => Boolean(el));
    if (headings.length === 0) return;
    const obs = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting) setActive(e.target.id);
        }
      },
      { rootMargin: "-72px 0px -66% 0px" },
    );
    headings.forEach((h) => obs.observe(h));
    return () => obs.disconnect();
  }, [items.map((i) => i.id).join("|")]);

  return (
    <nav className="toc-rail" aria-label="On this page">
      <p className="toc-title mb-2 text-[#6a7089]">On this page</p>
      {items.map((t) => (
        <a
          key={t.id}
          href={`#${t.id}`}
          className={`toc-item ${t.h3 ? "toc-h3" : ""} ${active === t.id ? "active" : ""}`}
          onClick={(e) => {
            e.preventDefault();
            document.getElementById(t.id)?.scrollIntoView({ behavior: "smooth" });
          }}
        >
          {t.label}
          {t.h3 ? "" : ""}
        </a>
      ))}
    </nav>
  );
}

/* ------------------------------------------------------------- DocsShell */

function SideGroup({ label, children }: { label: string; children: React.ReactNode }) {
  const [open, setOpen] = useState(true);
  return (
    <div className="side-group">
      <button
        type="button"
        className="side-group-btn"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="side-section">{label}</span>
        <span className={`side-chevron${open ? " open" : ""}`} aria-hidden="true">
          ›
        </span>
      </button>
      {open && <div className="side-group-body">{children}</div>}
    </div>
  );
}

function Sidebar({ path }: { path: string }) {
  const groups: DocRoute["group"][] = ["Get started", "Reference", "Project"];
  return (
    <aside className="sidebar-rail">
      {groups.map((g) => (
        <SideGroup key={g} label={g}>
          {DOC_ROUTES.filter((r) => r.group === g).map((r) => (
            <Link
              key={r.path}
              to={r.path}
              className={`side-link ${path === r.path ? "active" : ""}`}
            >
              {r.nav}
            </Link>
          ))}
        </SideGroup>
        ))}
      <p className="side-section">Resources</p>
      <a className="side-link" href="https://modrinth.com/plugin/statfyr" target="_blank" rel="noopener noreferrer">
        Modrinth ↗
      </a>
      <a className="side-link" href="https://github.com/PotenFYR-Studios/statfyr" target="_blank" rel="noopener noreferrer">
        GitHub ↗
      </a>
    </aside>
  );
}

function PrevNext({ path }: { path: string }) {
  const i = docIndex(path);
  const prev = i > 0 ? DOC_ROUTES[i - 1] : undefined;
  const next = i >= 0 && i < DOC_ROUTES.length - 1 ? DOC_ROUTES[i + 1] : undefined;
  if (i < 0) return null;
  return (
    <div className="mt-12 flex flex-col gap-3 sm:flex-row">
      {prev ? (
        <Link to={prev.path} className="page-nav-card prev">
          <span className="pn-label">← Previous</span>
          <span className="pn-title">{prev.title}</span>
        </Link>
      ) : (
        <span className="flex-1" />
      )}
      {next ? (
        <Link to={next.path} className="page-nav-card next sm:items-end">
          <span className="pn-label text-right">Next →</span>
          <span className="pn-title sm:text-right">{next.title}</span>
        </Link>
      ) : (
        <span className="flex-1" />
      )}
    </div>
  );
}

export function Crumb({ label, to }: { label: string; to?: string }) {
  if (to) {
    return (
      <Link to={to} className="breadcrumb">
        {label}
      </Link>
    );
  }
  return <span className="breadcrumb">{label}</span>;
}

export function DocsShell({
  breadcrumb,
  crumbs,
  title,
  lede,
  toc,
  children,
}: {
  breadcrumb?: string;
  crumbs?: { label: string; to?: string }[];
  title: string;
  lede?: string;
  toc?: TocItem[];
  children: React.ReactNode;
}) {
  const path = usePath();
  return (
    <div className="layout">
      <Sidebar path={path} />
      <article className="doc-article min-w-0">
        {crumbs ? (
          <nav className="crumb-trail" aria-label="Breadcrumb">
            {crumbs.map((c, i) => (
              <span key={i} className="crumb-item">
                <Crumb label={c.label} to={c.to} />
                {i < crumbs.length - 1 && (
                  <span className="breadcrumb" aria-hidden="true">
                    {" "}
                    /{" "}
                  </span>
                )}
              </span>
            ))}
          </nav>
        ) : (
          breadcrumb && <p className="breadcrumb">{breadcrumb}</p>
        )}
        <h1 className="mt-3">{title}</h1>
        {lede && <p className="mt-4 !text-[1.02em] leading-relaxed text-[#b9bfd4]">{lede}</p>}
        <div className="mt-6">{children}</div>
        <PrevNext path={path} />
      </article>
      {toc && toc.length > 0 && <TocRail items={toc} />}
    </div>
  );
}

/* ---------------------------------------------------------------- palette */

export function CommandPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [q, setQ] = useState("");
  const [sel, setSel] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const items = useMemo(() => {
    const base = [
      { path: "/", nav: "Home", group: "Site" },
      { path: "/examples", nav: "Examples", group: "Site" },
      { path: "/about", nav: "About", group: "Site" },
      { path: "/license", nav: "License", group: "Site" },
      ...DOC_ROUTES.map((r) => ({ path: r.path, nav: r.nav, group: r.group })),
    ];
    if (!q.trim()) return base;
    const needle = q.toLowerCase();
    return base.filter((i) => `${i.nav} ${i.group} ${i.path}`.toLowerCase().includes(needle));
  }, [q]);

  useEffect(() => {
    if (open) {
      setQ("");
      setSel(0);
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      else if (e.key === "ArrowDown") {
        e.preventDefault();
        setSel((s) => Math.min(s + 1, items.length - 1));
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setSel((s) => Math.max(s - 1, 0));
      } else if (e.key === "Enter" && items[sel]) {
        navigate(items[sel].path);
        onClose();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, items, sel, onClose]);

  if (!open) return null;
  return (
    <div className="cmdk-overlay" onClick={onClose}>
      <div
        className="cmdk-panel"
        role="dialog"
        aria-modal="true"
        aria-label="Search documentation"
        onClick={(e) => e.stopPropagation()}
      >
        <input
          ref={inputRef}
          className="cmdk-input"
          placeholder="Jump to… (Esc to close)"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setSel(0);
          }}
        />
        <div className="max-h-[320px] overflow-y-auto p-2">
          {items.length === 0 && (
            <p className="px-3 py-6 text-center text-sm text-[#6a7089]">No matches</p>
          )}
          {items.map((it, idx) => (
            <div
              key={it.path}
              className={`cmdk-row ${idx === sel ? "selected" : ""}`}
              onClick={() => {
                navigate(it.path);
                onClose();
                window.scrollTo({ top: 0 });
              }}
              onMouseEnter={() => setSel(idx)}
            >
              <span className="cmdk-glyph">↳</span>
              <span>{it.nav}</span>
              <span className="ml-auto font-mono text-[10px] text-[#6a7089]">{it.group}</span>
            </div>
            ))}
        </div>
      </div>
    </div>
  );
}

export { Header, Footer, Eyebrow };
