#!/usr/bin/env bash
#
# Format classified commits into a CHANGELOG entry.
#
# Usage: format-changelog.sh <style> <version> [release-date]
#
#   style        "underline" — "Version X.Y.Z (date)" + hyphen rule, flat lines
#                              (iOS / Android trackers)
#                "markdown"  — "# X.Y.Z" + "* " bullets
#                              (Flutter tracker)
#   version      the new version string, e.g. 6.2.6
#   release-date YYYY-MM-DD; defaults to today (UTC). Only used by "underline".
#
# Reads the TSV produced by classify-commits.sh on stdin, writes the entry to
# stdout. Both styles are flat lists — no category headers — matching the
# existing files. Category ordering is still applied so breaking changes and
# features surface first.

set -euo pipefail

style="${1:?usage: format-changelog.sh <underline|markdown> <version> [release-date]}"
version="${2:?missing version}"
release_date="${3:-$(date -u +%Y-%m-%d)}"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# shellcheck disable=SC2034  # login/external are read to keep field alignment;
# contributor attribution belongs in the PR body, not the CHANGELOG.
while IFS=$'\t' read -r category description pr_ref login external; do
  [[ -z "${category:-}" ]] && continue
  # classify-commits.sh writes "-" for empty columns; see the note there.
  [[ "$pr_ref" == "-" ]] && pr_ref=""
  [[ "$login" == "-" ]] && login=""

  line="$description"
  [[ -n "$pr_ref" ]] && line="${line} (${pr_ref})"
  # Breaking changes are called out inline, since these lists have no headers.
  if [[ "$category" == "breaking" ]]; then
    line="**Breaking:** ${line}"
  fi
  printf '%s\n' "$line" >> "$work/$category"
done

collect() {
  for c in breaking feature improvement fix enhancement; do
    [[ -s "$work/$c" ]] && cat "$work/$c"
  done
  return 0
}

if ! collect | grep -q .; then
  printf '::error::No user-facing commits found; refusing to write an empty CHANGELOG entry.\n' >&2
  exit 1
fi

case "$style" in
  underline)
    header="Version ${version} (${release_date})"
    printf '%s\n' "$header"
    # Underline rule matches the header length, as in the existing CHANGELOG.
    printf '%s\n' "$(printf '%*s' "${#header}" '' | tr ' ' '-')"
    collect
    ;;
  markdown)
    printf '# %s\n\n' "$version"
    collect | sed 's/^/* /'
    ;;
  *)
    printf '::error::Unknown changelog style: %s\n' "$style" >&2
    exit 1
    ;;
esac
