#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-}
if [[ -z "$VERSION" ]]; then
  echo "Usage: $0 <version>  (e.g. 0.4.0)" >&2
  exit 1
fi

ROOT=$(git rev-parse --show-toplevel)
CHANGELOG="$ROOT/CHANGELOG.md"
DATE=$(date +%Y-%m-%d)

if ! grep -q "## \[Unreleased\]" "$CHANGELOG"; then
  echo "No [Unreleased] section found in CHANGELOG.md" >&2
  exit 1
fi

if git tag | grep -q "^v$VERSION$"; then
  echo "Tag v$VERSION already exists" >&2
  exit 1
fi

python3 - "$CHANGELOG" "$VERSION" "$DATE" <<'EOF'
import sys

changelog, version, date = sys.argv[1], sys.argv[2], sys.argv[3]
with open(changelog) as f:
    content = f.read()

fresh_unreleased = (
    "## [Unreleased]\n"
    "### Added\n\n"
    "### Changed\n\n"
    "### Deprecated\n\n"
    "### Removed\n\n"
    "### Fixed\n\n"
    "### Security\n"
)
versioned = f"## [{version}] — {date}"
content = content.replace("## [Unreleased]", f"{fresh_unreleased}\n{versioned}", 1)

with open(changelog, "w") as f:
    f.write(content)
EOF

git add "$CHANGELOG"
git commit -m "chore(release): $VERSION"
git tag "v$VERSION"

echo "Released v$VERSION. Push with: git push && git push origin v$VERSION"
