#!/usr/bin/env bash
# Downloads the newest Google Sans Code and Google Sans Flex TTFs and installs
# them into app/src/main/res/font/, keeping the resource file names stable.
#
# Sources:
#   - Google Sans Code: google/fonts repo (OFL), main branch tip of the TTF file.
#   - Google Sans Flex: official Google Fonts download manifest, which always
#     references the latest variable-font build served by fonts.gstatic.com.
set -euo pipefail

FONT_DIR="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/res/font"
CODE_URL="https://raw.githubusercontent.com/google/fonts/main/ofl/googlesanscode/GoogleSansCode%5Bwght%5D.ttf"
FLEX_MANIFEST="https://fonts.google.com/download/list?family=Google+Sans+Flex"

fetch() { curl -fsSL --retry 3 --retry-delay 2 -o "$2" "$1"; }

assert_ttf() {
    local file="$1"
    if [[ ! -s "$file" ]]; then echo "error: $file is empty"; exit 1; fi
    local magic
    magic=$(head -c 4 "$file" | od -An -tx1 | tr -d ' \n')
    case "$magic" in
        00010000|4f54544f|"74727565") ;;          # TTF / OTTO / true
        *) echo "error: $file is not a TTF (magic=$magic)"; exit 1 ;;
    esac
}

flex_url() {
    # Manifest body starts with a XSSI prefix ")]}'" that must be stripped.
    curl -fsSL --retry 3 --retry-delay 2 "$FLEX_MANIFEST" \
        | tail -c +5 \
        | python3 -c '
import json, sys
data = json.load(sys.stdin)
for ref in data["manifest"]["fileRefs"]:
    if ref["filename"].startswith("GoogleSansFlex-VariableFont"):
        print(ref["url"])
        break
else:
    sys.exit("variable font entry not found in Google Fonts manifest")
'
}

tmp_code=$(mktemp)
tmp_flex=$(mktemp)
trap 'rm -f "$tmp_code" "$tmp_flex"' EXIT

echo "Downloading Google Sans Code..."
fetch "$CODE_URL" "$tmp_code"

echo "Resolving latest Google Sans Flex variable font..."
fetch "$(flex_url)" "$tmp_flex"

assert_ttf "$tmp_code"
assert_ttf "$tmp_flex"

mkdir -p "$FONT_DIR"
mv "$tmp_code" "$FONT_DIR/google_sans_code.ttf"
mv "$tmp_flex" "$FONT_DIR/google_sans_flex_variable.ttf"
ls -la "$FONT_DIR"
echo "Fonts updated."
