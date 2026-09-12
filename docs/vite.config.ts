import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));
const CANON = "https://statfyr.docs.potenfyr.in";

type RouteMeta = {
  path: string;
  title: string;
  description: string;
  jsonLd?: Record<string, unknown>;
};

const ROUTES: RouteMeta[] = [
  {
    path: "/",
    title: "Statfyr · Minecraft REST API for Player Statistics",
    description:
      "Blazing-fast REST API plugin for Minecraft exposing player statistics (kills, playtime, blocks mined and more) through a clean documented HTTP interface.",
    jsonLd: {
      "@context": "https://schema.org",
      "@graph": [
        {
          "@type": "SoftwareApplication",
          name: "Statfyr",
          applicationCategory: "GameApplication",
          operatingSystem: "Minecraft Server (Paper/Spigot 1.16.5+)",
          description:
            "REST API plugin for Minecraft exposing player statistics through a clean documented HTTP interface.",
          url: CANON,
          downloadUrl: "https://modrinth.com/plugin/statfyr",
          license: "https://github.com/PotenFYR-Studios/statfyr/blob/master/LICENSE",
          author: { "@id": `${CANON}/#org` },
          offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
        },
        {
          "@type": "WebSite",
          name: "Statfyr Docs",
          url: `${CANON}/`,
          publisher: { "@id": `${CANON}/#org` },
        },
        {
          "@type": "Organization",
          "@id": `${CANON}/#org`,
          name: "PotenFYR Studios",
          url: "https://potenfyr.in",
          sameAs: [
            "https://github.com/PotenFYR-Studios",
            "https://modrinth.com/organization/potenfyr",
          ],
        },
      ],
    },
  },
  {
    path: "/docs",
    title: "Documentation Portal · Statfyr",
    description:
      "Complete documentation for Statfyr: install, configure, secure, and integrate the REST API for Minecraft player statistics.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "WebPage",
      name: "Statfyr Documentation",
      description:
        "Complete documentation for Statfyr: install, configure, secure, and integrate the REST API.",
      url: `${CANON}/docs`,
      isPartOf: { "@type": "WebSite", name: "Statfyr Docs", url: CANON },
    },
  },
  {
    path: "/docs/getting-started",
    title: "Getting Started · Statfyr Docs",
    description:
      "Download and install Statfyr on your Paper/Spigot server in under 5 minutes. First API call, verify health, and explore the endpoints.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "TechArticle",
      headline: "Getting Started with Statfyr",
      description:
        "Download and install Statfyr on your Paper/Spigot server in under 5 minutes.",
      url: `${CANON}/docs/getting-started`,
    },
  },
  {
    path: "/docs/configuration",
    title: "Configuration Reference · Statfyr Docs",
    description:
      "Every config.yml key in Statfyr explained: HTTP, HTTPS, security, rate limiting, CORS, pagination, caching, and environment variable overrides.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "TechArticle",
      headline: "Statfyr Configuration Reference",
      description:
        "Every config.yml key in Statfyr explained with defaults and advisory notes.",
      url: `${CANON}/docs/configuration`,
    },
  },
  {
    path: "/docs/api",
    title: "API Reference · Statfyr Docs",
    description:
      "Complete REST API reference for Statfyr: all 7 endpoints, request/response JSON examples, authentication, error shapes, leaderboard stat name formats.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "TechArticle",
      headline: "Statfyr API Reference",
      description:
        "Complete REST API reference for Statfyr with request/response examples.",
      url: `${CANON}/docs/api`,
    },
  },
  {
    path: "/docs/examples",
    title: "Integration Examples · Statfyr Docs",
    description:
      "Real integration examples: JavaScript dashboard, leaderboard widget, Discord bot with discord.js, curl one-liners, all against the Statfyr REST API.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "TechArticle",
      headline: "Statfyr Integration Examples",
      description:
        "JavaScript dashboard, leaderboard widget, Discord bot, curl examples using the Statfyr API.",
      url: `${CANON}/docs/examples`,
    },
  },
  {
    path: "/docs/faq",
    title: "FAQ · Statfyr Docs",
    description:
      "Frequently asked questions about Statfyr: TPS impact, offline players, port conflicts, HTTPS setup, CORS, rate limiting, and supported versions.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "FAQPage",
      mainEntity: [
        {
          "@type": "Question",
          name: "Does Statfyr impact server TPS?",
          acceptedAnswer: {
            "@type": "Answer",
            text: "No. Stats are read asynchronously on a background thread pool. The HTTP server uses com.sun.net.httpserver with its own executor. The main server thread is never blocked by API requests.",
          },
        },
        {
          "@type": "Question",
          name: "Does Statfyr support offline players?",
          acceptedAnswer: {
            "@type": "Answer",
            text: "Yes. For online players stats are read live via the Bukkit Statistic API. For offline players stats are parsed from the vanilla world/stats/<uuid>.json files.",
          },
        },
        {
          "@type": "Question",
          name: "What Minecraft versions are supported?",
          acceptedAnswer: {
            "@type": "Answer",
            text: "Paper, Spigot, Purpur, and compatible forks running 1.16.5 through 1.21.x. The plugin compiles against Paper 1.16.5 API and uses only stable Bukkit API surface.",
          },
        },
      ],
    },
  },
  {
    path: "/about",
    title: "About · Statfyr",
    description:
      "About Statfyr: a zero-dependency REST API plugin for Minecraft by PotenFYR Studios, built on the JDK HttpServer with Paper API.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "AboutPage",
      name: "About Statfyr",
      description:
        "Statfyr is a zero-dependency REST API plugin for Minecraft by PotenFYR Studios.",
      url: `${CANON}/about`,
    },
  },
  {
    path: "/examples",
    title: "Examples · Statfyr",
    description:
      "Ready-to-paste Statfyr examples: a hardened config.yml (API key, HTTPS, CORS) and curl walkthroughs of all 7 REST endpoints.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "TechArticle",
      headline: "Statfyr Examples",
      description:
        "A hardened config.yml and curl walkthroughs of every Statfyr REST endpoint.",
      url: `${CANON}/examples`,
    },
  },
  {
    path: "/license",
    title: "License · Statfyr",
    description:
      "Statfyr is free software under Apache-2.0 with the Commons Clause: use, modify, fork, and self-host it for anything, including commercial use. Not for resale.",
    jsonLd: {
      "@context": "https://schema.org",
      "@type": "WebPage",
      name: "Statfyr License",
      description:
        "What you can and cannot do with Statfyr: Apache-2.0 with the Commons Clause in plain terms.",
      url: `${CANON}/license`,
    },
  },
];

function multiPageEmit(): Plugin {
  return {
    name: "statfyr-multi-page",
    apply: "build",
    writeBundle() {
      const outDir = resolve(__dir, "dist");
      const shell = readFileSync(resolve(outDir, "index.html"), "utf8");

      const emit = (
        meta: RouteMeta,
        file: string,
        extraHead = "",
      ) => {
        const canon = `${CANON}${meta.path === "/" ? "/" : meta.path}`;
        const ld =
          meta.jsonLd
            ? `<script type="application/ld+json">${JSON.stringify(meta.jsonLd)}</script>`
            : "";
        const html = shell
          .replace(
            /<title>.*?<\/title>/,
            `<title>${meta.title}</title>`,
          )
          .replace(
            "</head>",
            `  <meta name="description" content="${meta.description.replace(/"/g, "&quot;")}">\n` +
              `  <link rel="canonical" href="${canon}">\n` +
              `  <meta property="og:type" content="website">\n  <meta property="og:site_name" content="Statfyr Docs">\n  <meta property="og:title" content="${meta.title.replace(/"/g, "&quot;")}">\n  <meta property="og:description" content="${meta.description.replace(/"/g, "&quot;")}">\n  <meta property="og:url" content="${canon}">\n` +
              `  <meta property="og:image" content="${CANON}/og.png">\n` +
              `  <meta property="og:image:alt" content="Statfyr · REST API plugin for Minecraft by PotenFYR Studios">\n` +
              `  <meta property="og:image:width" content="1200">\n  <meta property="og:image:height" content="630">\n` +
              `  <meta name="twitter:card" content="summary_large_image">\n  <meta name="twitter:title" content="${meta.title.replace(/"/g, "&quot;")}">\n  <meta name="twitter:description" content="${meta.description.replace(/"/g, "&quot;")}">\n` +
              `  <meta name="twitter:image" content="${CANON}/og.png">\n` +
              ld + extraHead + "\n</head>",
          );
        const dir = resolve(outDir, file, "..");
        mkdirSync(dir, { recursive: true });
        writeFileSync(resolve(outDir, file), html);
      };

      for (const r of ROUTES) {
        const file =
          r.path === "/" ? "index.html" : `${r.path.replace(/^\//, "")}/index.html`;
        emit(r, file);
        // Clean-URL twin for extensionless requests (examples.html)
        if (r.path === "/examples") emit(r, "examples.html");
      }

      // 404 fallback
      emit(
        {
          path: "/404",
          title: "Not Found · Statfyr",
          description: "Page not found.",
        },
        "404.html",
        '<meta name="robots" content="noindex">',
      );
    },
  };
}

export default defineConfig({
  root: __dirname,
  base: "/",
  plugins: [react(), tailwindcss(), multiPageEmit()],
  build: {
    outDir: "dist",
    emptyOutDir: true,
    sourcemap: false,
  },
  server: { port: 4179 },
});
