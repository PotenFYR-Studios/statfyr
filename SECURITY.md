# Security Policy

## Supported Versions

| Version        | Supported |
|----------------|-----------|
| 1.0.0-BETA (master) | ✅ |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Use GitHub's **private vulnerability reporting** for this repository:

1. Go to the **Security** tab of [PotenFYR-Studios/statfyr](https://github.com/PotenFYR-Studios/statfyr/security)
2. Click **Report a vulnerability**
3. Describe the issue and how to reproduce it

If private vulnerability reporting is not available, contact the maintainers directly through [PotenFYR Studios](https://potenfyr.in).

<!-- TODO(owner): replace/add a monitored security contact address here -->

Please include as much of the following as you can:

- The plugin version (`/statfyr status` or the jar name)
- Server software and version (Paper/Spigot/Purpur)
- A minimal reproduction (config, request, and response)
- Your assessment of severity and impact

## What to Expect

We will acknowledge reports as soon as possible, work with you to understand and reproduce the issue, and credit you in the fix release if you'd like.

## Scope Notes

Statfyr exposes an HTTP API on your server. Keep these in mind when assessing a report:

- An instance started with default settings on a publicly reachable host is **insecure by design**: the README and docs call out enabling `security.enable-api-key` and HTTPS as required hardening. Deployment mistakes like this are not plugin vulnerabilities, though documentation gaps about them are welcome reports.
- Reports about the plugin's own code (auth bypass, injection into responses, rate-limit or whitelist bypass, path traversal, etc.) are exactly what we want.
