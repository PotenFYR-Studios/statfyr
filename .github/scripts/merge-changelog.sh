#!/usr/bin/env bash
#
# =============================================================================
# StatFYR — changelog merger
# =============================================================================
#
# Merges a freshly generated changelog section into an existing release body.
#
# - If the same version already has a section it is *replaced* (so re-running a
#   release refreshes its notes instead of duplicating them).
# - All other version sections are preserved underneath.
#
# Usage:
#   merge-changelog.sh <existing-file> <new-section-file> <version> <output>
# =============================================================================
set -euo pipefail

EXISTING="${1:?existing file required}"
NEW_SECTION="${2:?new section file required}"
VERSION="${3:?version required}"
OUT="${4:?output file required}"

# Nothing to merge into — just use the new section.
if [ ! -f "${EXISTING}" ] || [ ! -s "${EXISTING}" ]; then
  cp "${NEW_SECTION}" "${OUT}"
  exit 0
fi

STRIPPED="$(mktemp)"

# Remove any existing section for this version (heading + body up to the next
# level-2 heading). Handles both "1.2.0" and "v1.2.0" headings.
awk -v ver="${VERSION}" '
  BEGIN { skip = 0 }
  /^## / {
    heading = $0
    if (index(heading, "## " ver) == 1 || index(heading, "## v" ver) == 1) {
      skip = 1
      next
    } else {
      skip = 0
    }
  }
  skip == 1 { next }
  { print }
' "${EXISTING}" > "${STRIPPED}"

{
  cat "${NEW_SECTION}"
  echo
  echo "---"
  echo
  cat "${STRIPPED}"
} > "${OUT}"

rm -f "${STRIPPED}"

echo "Merged changelog section for ${VERSION} into ${OUT}"
