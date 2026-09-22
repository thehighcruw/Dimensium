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
import re, sys

changelog, version, date = sys.argv[1], sys.argv[2], sys.argv[3]
with open(changelog) as f:
    content = f.read()

# Extract the [Unreleased] section body
match = re.search(r'## \[Unreleased\](.*?)(?=^## \[|\Z)', content, re.DOTALL | re.MULTILINE)
unreleased_body = match.group(1) if match else ""

# Strip subsections that have no entries
def strip_empty_sections(body):
    parts = re.split(r'(?=^### )', body, flags=re.MULTILINE)
    result = []
    for part in parts:
        if part.startswith("### "):
            # Keep only if it has non-blank, non-heading content
            body_lines = re.sub(r'^### [^\n]*\n', '', part).strip()
            if body_lines:
                result.append(part)
        else:
            result.append(part)
    return "".join(result)

versioned_body = strip_empty_sections(unreleased_body)

fresh_unreleased = (
    "## [Unreleased]\n"
    "### Added\n\n"
    "### Changed\n\n"
    "### Deprecated\n\n"
    "### Removed\n\n"
    "### Fixed\n\n"
    "### Security\n"
)
versioned = f"## [{version}] — {date}{versioned_body}"
content = re.sub(
    r'## \[Unreleased\].*?(?=^## \[|\Z)',
    f"{fresh_unreleased}\n{versioned}",
    content, count=1, flags=re.DOTALL | re.MULTILINE
)

with open(changelog, "w") as f:
    f.write(content)
EOF

git add "$CHANGELOG"
git commit -m "chore(release): $VERSION"
git tag "v$VERSION"

echo "Released v$VERSION. Push with: git push && git push origin v$VERSION"
