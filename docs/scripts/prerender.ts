// Static prerender: after `vite build` (which emits per-route HTML shells via
// the multiPageEmit plugin), render every route with react-dom/server and
// inject the markup into the emitted pages so crawlers see full content.
// The client hydrates it: src/main.tsx uses hydrateRoot when the root div
// already has children, createRoot on the empty dev server.
//
// Routes are prerendered normalized (no trailing slash) to match the
// router's getPath() convention, so cold loads of "path/" hydrate cleanly.

import { readFile, writeFile } from "node:fs/promises";
import { renderToString } from "react-dom/server";
import { StrictMode, createElement } from "react";
import App from "../src/App";

type Emit = { path: string; files: string[] };

// path = normalized route; files = emitted pages that must show this route.
const EMITS: Emit[] = [
  { path: "/", files: ["index.html"] },
  { path: "/docs", files: ["docs/index.html"] },
  { path: "/docs/getting-started", files: ["docs/getting-started/index.html"] },
  { path: "/docs/configuration", files: ["docs/configuration/index.html"] },
  { path: "/docs/api", files: ["docs/api/index.html"] },
  { path: "/docs/examples", files: ["docs/examples/index.html"] },
  { path: "/docs/faq", files: ["docs/faq/index.html"] },
  { path: "/about", files: ["about/index.html"] },
  { path: "/examples", files: ["examples/index.html", "examples.html"] },
  { path: "/license", files: ["license/index.html"] },
  // The 404 fallback page renders the NotFound view.
  { path: "/definitely-not-a-route", files: ["404.html"] },
];

// Minimal window stub: the router's server snapshot reads
// window.location.pathname (see src/lib/router.tsx getServerPath).
const win = {
  location: { pathname: "/" },
  addEventListener() {},
  removeEventListener() {},
  history: { pushState() {}, replaceState() {} },
  scrollTo() {},
};
(globalThis as Record<string, unknown>).window = win;

const distDir = decodeURIComponent(new URL("../dist/", import.meta.url).pathname);
const MARKER = '<div id="root"></div>';

let pages = 0;
try {
  for (const emit of EMITS) {
    win.location.pathname = emit.path;
    const body = renderToString(
      createElement(StrictMode, null, createElement(App)),
    );
    for (const file of emit.files) {
      const target = `${distDir}${file}`;
      const html = await readFile(target, "utf8");
      if (!html.includes(MARKER)) {
        throw new Error(`root placeholder missing in ${file}`);
      }
      await writeFile(target, html.replace(MARKER, `<div id="root">${body}</div>`));
      pages++;
      console.log(`[prerender] ${file} <- ${emit.path} (${body.length} chars)`);
    }
  }
  console.log(`[prerender] ${pages} pages carry static content`);
} catch (err) {
  console.error("[prerender] ERROR:", err);
  process.exit(1);
}
