# Contributing to Statfyr

Thanks for your interest in contributing! This file covers everything you need to build, test, and submit changes.

## Project Overview

| | |
|---|---|
| Language | Java 8 bytecode (compiled with a modern JDK via `--release 8`) |
| Build system | Gradle (Kotlin-free Groovy DSL, `build.gradle` + wrapper) |
| Platform API | Paper 1.16.5 (`com.destroystokyo.paper:paper-api`), compile-only |
| Runtime support | Bukkit · Spigot · Paper · Purpur · Folia — Minecraft **1.8.x – 26.x** |
| HTTP server | `com.sun.net.httpserver` (JDK built-in) |
| Dependencies | **None** bundled beyond the platform API; please keep it that way |

The plugin embeds a REST API directly inside a Minecraft server. The API surface (endpoints, JSON shapes, config keys) is documented in the docs site (`docs/`) and must stay in sync with the code.

StatfyR compiles against the **oldest supported API** and reaches newer/older API surface reflectively through the `in.potenfyr.statfyr.compat` package. Do not call version-specific Paper/Spigot APIs directly — add a compat helper instead.

## Prerequisites

- **JDK 17 or newer** to run Gradle ([Temurin](https://adoptium.net/) recommended, the same distribution CI uses)
- The **Gradle wrapper** is committed, so no Gradle install is required
- For manual testing: any Bukkit-family server on 1.8.x–26.x

## Building

```bash
./gradlew clean build
```

The compiled plugin lands at `build/libs/Statfyr-<version>.jar` (version from `gradle.properties`). This is exactly what CI runs, so if it builds locally it will build there.

Useful tasks:

```bash
./gradlew jar              # build only the plugin JAR
./gradlew buildRelease     # clean + build and print the artifact path
./gradlew publishToMavenLocal
```

To build inside Docker (no local JDK required):

```bash
docker run --rm -u "$(id -u):$(id -g)" \
  -e HOME=/tmp -e GRADLE_USER_HOME=/tmp/gradle-home \
  -v /tmp/gradle-home:/tmp/gradle-home \
  -v "$PWD":/workspace -w /workspace \
  eclipse-temurin:21-jdk ./gradlew --no-daemon clean build
```

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

- [ ] `./gradlew clean build` succeeds
- [ ] No new bundled dependencies added (or a strong case made for them in the PR description)
- [ ] Docs (`docs/`) and README updated if endpoints, config, commands, or behavior changed
- [ ] Manually tested on a local server, including error responses
- [ ] No secrets, keys, or real player data committed

## CI

- **`build.yml`**: builds the plugin with Gradle on every push and PR targeting `master`, verifies Java 8 bytecode, and uploads the jar as an artifact
- **`release.yml`**: builds and publishes a GitHub Release on `v*` tags (replacing assets and merging the changelog if the version already exists)
- **`docs-pages.yml`**: builds the docs site; pushes to `master` deploy it to GitHub Pages

## Reporting Issues

Use the [issue templates](https://github.com/PotenFYR-Studios/statfyr/issues/new/choose). For security vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
