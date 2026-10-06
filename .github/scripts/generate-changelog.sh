#!/usr/bin/env bash
#
# =============================================================================
# StatFYR — changelog generator
# =============================================================================
#
# Builds a Markdown changelog section for a release from git history.
#
# Usage:
#   generate-changelog.sh <tag> <output-file> [version] [date]
#
# Commits are grouped from Conventional Commit prefixes. If no previous tag
# exists the whole history of the tag is used.
# =============================================================================
set -euo pipefail

TAG="${1:?tag required}"
OUT="${2:?output file required}"
VERSION="${3:-${TAG#v}}"
DATE="${4:-$(date -u +%Y-%m-%d)}"

REPO="${GITHUB_REPOSITORY:-PotenFYR-Studios/statfyr}"
SERVER="${GITHUB_SERVER_URL:-https://github.com}"

# -----------------------------------------------------------------------------
# Find the previous tag (excluding the tag being released).
# -----------------------------------------------------------------------------
PREV=""
if git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null 2>&1; then
  PREV="$(git tag --sort=-v:refname | grep -v -x "${TAG}" | head -n1 || true)"
fi

if [ -n "${PREV}" ]; then
  RANGE="${PREV}..${TAG}"
  COMPARE="${SERVER}/${REPO}/compare/${PREV}...${TAG}"
else
  RANGE="${TAG}"
  COMPARE="${SERVER}/${REPO}/commits/${TAG}"
fi

# -----------------------------------------------------------------------------
# Collect commits.
# -----------------------------------------------------------------------------
COMMITS="$(git log --no-merges --pretty=format:'%h%x09%s' "${RANGE}" 2>/dev/null || true)"

declare -A BUCKETS
BUCKET_ORDER=(
  "feat:Features"
  "fix:Bug Fixes"
  "perf:Performance"
  "refactor:Refactors"
  "docs:Documentation"
  "build:Build System"
  "ci:Continuous Integration"
  "test:Tests"
  "chore:Chores"
  "other:Other Changes"
)

for key in "${BUCKET_ORDER[@]}"; do
  BUCKETS["${key%%:*}"]=""
done

if [ -n "${COMMITS}" ]; then
  while IFS=$'\t' read -r hash subject; do
    [ -z "${hash}" ] && continue

    # Determine conventional-commit type.
    type="other"
    if printf '%s' "${subject}" | grep -qE '^(feat|feature)(\(.+\))?!?:'; then
      type="feat"
    elif printf '%s' "${subject}" | grep -qE '^fix(\(.+\))?!?:'; then
      type="fix"
    elif printf '%s' "${subject}" | grep -qE '^perf(\(.+\))?!?:'; then
      type="perf"
    elif printf '%s' "${subject}" | grep -qE '^refactor(\(.+\))?!?:'; then
      type="refactor"
    elif printf '%s' "${subject}" | grep -qE '^docs?(\(.+\))?!?:'; then
      type="docs"
    elif printf '%s' "${subject}" | grep -qE '^build(\(.+\))?!?:'; then
      type="build"
    elif printf '%s' "${subject}" | grep -qE '^(ci)(\(.+\))?!?:'; then
      type="ci"
    elif printf '%s' "${subject}" | grep -qE '^test(\(.+\))?!?:'; then
      type="test"
    elif printf '%s' "${subject}" | grep -qE '^chore(\(.+\))?!?:'; then
      type="chore"
    fi

    # Strip the conventional prefix for readability.
    clean="$(printf '%s' "${subject}" | sed -E 's/^(feat|feature|fix|perf|refactor|docs?|build|ci|test|chore)(\(.+\))?!?:[[:space:]]*//')"

    line="- ${clean} ([${hash}](${SERVER}/${REPO}/commit/${hash}))"
    BUCKETS["${type}"]="${BUCKETS[${type}]:-}${line}"$'\n'
  done <<< "${COMMITS}"
fi

# -----------------------------------------------------------------------------
# Write section.
# -----------------------------------------------------------------------------
{
  echo "## ${VERSION} — ${DATE}"
  echo
  echo "[Full changelog](${COMPARE})"
  echo

  any=false
  for key in "${BUCKET_ORDER[@]}"; do
    bucket="${key%%:*}"
    title="${key#*:}"
    content="${BUCKETS[${bucket}]:-}"

    if [ -n "${content}" ]; then
      any=true
      echo "### ${title}"
      echo
      printf '%s' "${content}"
      echo
    fi
  done

  if [ "${any}" = false ]; then
    echo "No user-facing changes in this release."
    echo
  fi
} > "${OUT}"

echo "Generated changelog for ${VERSION} at ${OUT}"
