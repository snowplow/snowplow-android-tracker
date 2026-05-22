#!/usr/bin/env bash
# Bump the tracker version across all files that the "Prepare for release" commit touches.
# Usage: bump-version.sh <X.Y.Z>
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <X.Y.Z>" >&2
  exit 2
fi

NEW_VERSION="$1"

if [[ ! "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version '$NEW_VERSION' (expected X.Y.Z)" >&2
  exit 2
fi

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

# 1. VERSION
echo "$NEW_VERSION" > VERSION

# 2. gradle.properties — line: VERSION_NAME=X.Y.Z
sed -i.bak -E "s/^(VERSION_NAME=)[0-9]+\.[0-9]+\.[0-9]+$/\1$NEW_VERSION/" gradle.properties
rm gradle.properties.bak

# 3. build.gradle — line: version = 'X.Y.Z' (single quotes, inside subprojects block)
sed -i.bak -E "s/(^[[:space:]]*version[[:space:]]*=[[:space:]]*)'[0-9]+\.[0-9]+\.[0-9]+'$/\1'$NEW_VERSION'/" build.gradle
rm build.gradle.bak

# Sanity check: each file must now contain the new version on the expected line.
grep -q "^$NEW_VERSION\$" VERSION
grep -q "^VERSION_NAME=$NEW_VERSION\$" gradle.properties
grep -qE "^[[:space:]]*version[[:space:]]*=[[:space:]]*'$NEW_VERSION'\$" build.gradle

echo "Bumped to $NEW_VERSION:"
echo "  VERSION"
echo "  gradle.properties"
echo "  build.gradle"
