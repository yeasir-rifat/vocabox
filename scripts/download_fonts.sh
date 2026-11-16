#!/usr/bin/env bash
# Downloads DM Sans (SIL OFL) from the official Google Fonts repository
# into app/src/main/res/font. Run once locally; GitHub Actions runs it automatically.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=app/src/main/res/font
mkdir -p "$OUT"

fetch() { # <google/fonts folder> <output file name> <file name prefix>
  local dir="$1" out="$2" prefix="$3" url
  if [ -s "$OUT/$out" ]; then echo "$out already present"; return; fi
  local auth=()
  if [ -n "${GITHUB_TOKEN:-}" ]; then auth=(-H "Authorization: Bearer ${GITHUB_TOKEN}"); fi
  url=$(curl -fsSL "${auth[@]}" "https://api.github.com/repos/google/fonts/contents/ofl/${dir}" \
    | jq -r --arg p "$prefix" '[.[] | select(.name | endswith(".ttf")) | select(.name | startswith($p)) | select(.name | test("Italic") | not)][0].download_url')
  if [ -z "$url" ] || [ "$url" = "null" ]; then echo "No TTF found in ofl/${dir}" >&2; exit 1; fi
  echo "Downloading $url"
  curl -fsSL "$url" -o "$OUT/$out"
  local size; size=$(wc -c < "$OUT/$out")
  if [ "$size" -lt 50000 ]; then echo "$out looks wrong (${size} bytes)" >&2; exit 1; fi
}

fetch dmsans dm_sans.ttf DMSans
echo "Fonts ready in $OUT"
