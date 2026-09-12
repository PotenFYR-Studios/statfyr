import { useSyncExternalStore } from "react";

/* ------------------------------------------------------------------- base */

/**
 * Base path the site is served under. Set at build time via VITE_BASE
 * (vite.config `base`); falls back through the runtime env so the
 * prerender script executed outside vite still resolves it.
 */
const envBase: unknown = import.meta.env?.BASE_URL;
const BASE: string =
  typeof envBase === "string" ? envBase
  : typeof process !== "undefined" ? process.env?.VITE_BASE ?? "/"
  : "/";

/** Prefix an absolute site path with the serving base (idempotent). */
export function withBase(p: string): string {
  if (BASE !== "/" && (p === BASE || p.startsWith(BASE))) return p;
  if (!p.startsWith("/")) return p;
  return `${BASE}${p.slice(1)}`;
}

/** Remove the serving base from a URL path (idempotent). */
export function stripBase(pathname: string): string {
  if (BASE === "/") return pathname;
  if (pathname === BASE) return "/";
  if (pathname.startsWith(BASE)) return pathname.slice(BASE.length - 1);
  return pathname;
}

/* ------------------------------------------------------------------ store */

const listeners = new Set<() => void>();

function emit() {
  for (const fn of listeners) fn();
}

function subscribe(fn: () => void) {
  listeners.add(fn);
  window.addEventListener("popstate", emit);
  return () => {
    listeners.delete(fn);
    window.removeEventListener("popstate", emit);
  };
}

const getPath = () => {
  const p = stripBase(window.location.pathname);
  /* Directory twins are served at "path/"; normalize so cold loads match. */
  let out = p.length > 1 && p.endsWith("/") ? p.slice(0, -1) : p;
  /* .html twins (examples.html) hydrate to the same page as the directory. */
  if (out.endsWith(".html")) out = out.slice(0, -5);
  return out;
};

/* Server snapshot: during prerender, window is stubbed per route, so read it.
   Outside prerender (and in the browser) this equals getPath(). */
const getServerPath = () =>
  typeof window !== "undefined" &&
  typeof window.location?.pathname === "string" &&
  window.location.pathname.length > 0
    ? getPath()
    : "/";

export function usePath(): string {
  return useSyncExternalStore(subscribe, getPath, getServerPath);
}

export function navigate(to: string, replace = false) {
  const url = withBase(to);
  if (replace) window.history.replaceState({}, "", url);
  else window.history.pushState({}, "", url);
  emit();
}

/* ------------------------------------------------------------------ Link */

export type LinkProps = {
  to: string;
  children: React.ReactNode;
  className?: string;
  ariaLabel?: string;
};

export function Link({ to, children, className, ariaLabel }: LinkProps) {
  return (
    <a
      href={withBase(to)}
      className={className}
      aria-label={ariaLabel}
      onClick={(e) => {
        if (
          e.metaKey ||
          e.ctrlKey ||
          e.shiftKey ||
          e.altKey ||
          e.button !== 0
        )
          return;
        e.preventDefault();
        navigate(to);
      }}
    >
      {children}
    </a>
  );
}

/* ------------------------------------------------------------ route table */

export type DocRoute = {
  path: string;
  nav: string;
  group: "Get started" | "Reference" | "Project";
  title: string;
  short: string;
  inDocsPager: boolean;
};

/** Ordered docs-reader routes (drives sidebar, prev/next, palette). */
export const DOC_ROUTES: DocRoute[] = [
  {
    path: "/docs/getting-started",
    nav: "Getting started",
    group: "Get started",
    title: "Getting Started",
    short: "Download, install, and verify your first API call.",
    inDocsPager: true,
  },
  {
    path: "/docs/configuration",
    nav: "Configuration",
    group: "Get started",
    title: "Configuration Reference",
    short: "Every config.yml key, default, and environment override.",
    inDocsPager: true,
  },
  {
    path: "/docs/api",
    nav: "API reference",
    group: "Reference",
    title: "API Reference",
    short: "All endpoints with real request/response examples.",
    inDocsPager: true,
  },
  {
    path: "/docs/examples",
    nav: "Examples",
    group: "Reference",
    title: "Integration Examples",
    short: "Dashboard, leaderboard, and Discord bot recipes.",
    inDocsPager: true,
  },
  {
    path: "/docs/faq",
    nav: "FAQ",
    group: "Project",
    title: "FAQ",
    short: "TPS impact, offline players, HTTPS, CORS, and more.",
    inDocsPager: true,
  },
];

export const ALL_NAV: { path: string; nav: string }[] = [
  { path: "/docs", nav: "Docs" },
  ...DOC_ROUTES.map((r) => ({ path: r.path, nav: r.nav })),
  { path: "/about", nav: "About" },
];

export function docIndex(path: string): number {
  return DOC_ROUTES.findIndex((r) => r.path === path);
}

/* ------------------------------------------------------------------- SEO */

export function applyMeta(opts: { title: string; description: string; path: string }) {
  document.title = opts.title;
  setMeta('meta[name="description"]', "content", opts.description);
  setMeta('meta[property="og:title"]', "content", opts.title);
  setMeta('meta[property="og:description"]', "content", opts.description);
  setMeta('meta[name="twitter:title"]', "content", opts.title);
  setMeta('meta[name="twitter:description"]', "content", opts.description);
  const canon = `https://statfyr.docs.potenfyr.in${opts.path === "/" ? "/" : opts.path}`;
  setMeta('link[rel="canonical"]', "href", canon);
  setMeta('meta[property="og:url"]', "content", canon);
}

function setMeta(selector: string, attr: string, value: string) {
  const el = document.head.querySelector(selector);
  if (el) el.setAttribute(attr, value);
}
