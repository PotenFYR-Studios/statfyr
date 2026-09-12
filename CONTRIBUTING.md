# Contributing to Statfyr

Thanks for your interest in contributing! This file covers everything you need to build, test, and submit changes.

## Project Overview

| | |
|---|---|
| Language | Java 16 |
| Build system | Maven (`pom.xml`) |
| Platform API | Paper 1.16.5 (`io.papermc.paper:paper-api`) |
| HTTP server | `com.sun.net.httpserver` (JDK built-in) |
| Dependencies | **None** beyond the platform API; please keep it that way |

The plugin embeds a REST API directly inside a Minecraft server. The API surface (endpoints, JSON shapes, config keys) is documented in the docs site (`docs/`) and must stay in sync with the code.

## Prerequisites

- **JDK 16 or newer** ([Temurin](https://adoptium.net/) recommended, the same distribution CI uses)
- **Maven 3.8+** (or use your IDE's bundled Maven)
- For manual testing: any Paper, Spigot, or Purpur server on 1.16.5+

## Building

```bash
mvn clean package
```

The compiled plugin lands at `target/statfyr-1.0.0-BETA.jar` (the version follows `pom.xml`). This is exactly what CI runs, so if it builds locally it will build there.

## Testing Your Changes

There is no automated test suite yet, so verify manually against a local server:

1. Copy the built jar into a test server's `plugins/` folder
2. Start the server, then check `plugins/statfyr/config.yml` was generated
3. Hit the API:

   ```bash
   curl http://localhost:8080/api/health
   curl http://localhost:8080/api/players
   ```

4. Exercise the endpoints you touched, including error paths (bad params, unknown players)

## Documentation

The docs site lives in `docs/` (Vite + React + TypeScript, built with Bun):

```bash
cd docs
bun install
bun run build   # outputs to docs/dist
```

To preview the built site locally:

```bash
python3 -m http.server 4178 --directory docs/dist
```

If your change affects endpoints, JSON shapes, config keys, or commands, update the matching page under `docs/src/` (and the README) in the same PR. The docs must reflect **real** behavior: if something is configurable but not yet enforced by the code, it should be marked that way rather than documented as working.

## Pull Requests

1. Fork the repo and create your branch from `master`
2. Keep PRs focused: one fix or feature per PR
3. Use [conventional commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`)
4. Fill out the pull request template

**Checklist before opening:**

- [ ] `mvn clean package` succeeds
- [ ] No new dependencies added (or a strong case made for them in the PR description)
- [ ] Docs (`docs/`) and README updated if endpoints, config, commands, or behavior changed
- [ ] Manually tested on a local server, including error responses
- [ ] No secrets, keys, or real player data committed

## CI

- **`build.yml`**: builds the plugin with Maven on every push and PR targeting `master`, and uploads the jar as an artifact
- **`docs-pages.yml`**: builds the docs site; pushes to `master` deploy it to GitHub Pages

## Reporting Issues

Use the [issue templates](https://github.com/PotenFYR-Studios/statfyr/issues/new/choose). For security vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
