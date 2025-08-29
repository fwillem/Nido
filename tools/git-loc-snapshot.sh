#!/usr/bin/env bash
set -euo pipefail

DEBUG="${DEBUG:-1}"
log() { [ "$DEBUG" = "1" ] && echo "[DEBUG] $*"; }

# Resolve paths (Windows → POSIX if needed)
to_posix() { if [[ "$1" == /* ]]; then echo "$1"; elif command -v cygpath >/dev/null 2>&1; then cygpath -u "$1"; else echo "$1"; fi; }

REPO_ROOT_RAW="$(git rev-parse --show-toplevel)"
REPO_ROOT="$(to_posix "$REPO_ROOT_RAW")"

TARGET_PATH_RAW="${1:-"$REPO_ROOT_RAW/app/src"}"
HISTORY_FILE_RAW="${2:-"$REPO_ROOT_RAW/metrics/loc_history.csv"}"

TARGET_PATH="$(to_posix "$TARGET_PATH_RAW")"
HISTORY_FILE="$(to_posix "$HISTORY_FILE_RAW")"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CLOC_WIN="$SCRIPT_DIR/cloc-2.06.exe"

log "REPO_ROOT_RAW = $REPO_ROOT_RAW"
log "REPO_ROOT     = $REPO_ROOT"
log "TARGET_PATH   = $TARGET_PATH"
log "HISTORY_FILE  = $HISTORY_FILE"
log "CLOC exe      = $CLOC_WIN"

[ -f "$CLOC_WIN" ] || { echo "ERROR: cloc not found at $CLOC_WIN"; exit 1; }

# Git meta
branch="$(git rev-parse --abbrev-ref HEAD | tr -d '\r\n')"
commit="$(git rev-parse --short HEAD | tr -d '\r\n')"
if tag=$(git describe --tags --exact-match 2>/dev/null); then tag="${tag//$'\r'/}"; else tag=""; fi
when="$(date '+%Y-%m-%d %H:%M:%S')"

# Run cloc inside target dir on '.'
tmp_csv="$(mktemp)"
log "cd -> $TARGET_PATH"
pushd "$TARGET_PATH" >/dev/null || { echo "ERROR: cd failed: $TARGET_PATH"; exit 1; }
log "pwd = $(pwd)"
log "RUN: \"$CLOC_WIN\" --include-ext=kt,xml --csv --quiet . > $tmp_csv"
"$CLOC_WIN" --include-ext=kt,xml --csv --quiet . > "$tmp_csv"
popd >/dev/null

[ "$DEBUG" = "1" ] && { echo "[DEBUG] CLOC CSV content:"; cat "$tmp_csv"; echo; }

# ----- Robust CSV parse: detect header positions -----
# Header looks like: files,language,blank,comment,code,"github..."
# We detect the index of 'language' and 'code' and use those columns.
get_code_for_lang () {
  local lang="$1"
  awk -F, -v L="$lang" '
    NR==1 {
      # map header names -> indices
      for (i=1;i<=NF;i++){ gsub(/"/,"",$i); h[$i]=i }
      lc=h["language"]; cc=h["code"];
      next
    }
    {
      for (i=1;i<=NF;i++) gsub(/[\r"]/,"",$i);
      if (lc && cc && $(lc)==L) { print $(cc); found=1 }
    }
    END { if (!found) print 0 }
  ' "$tmp_csv"
}

get_total_code () {
  awk -F, '
    NR==1 {
      for (i=1;i<=NF;i++){ gsub(/"/,"",$i); h[$i]=i }
      lc=h["language"]; cc=h["code"];
      next
    }
    {
      for (i=1;i<=NF;i++) gsub(/[\r"]/,"",$i);
      if (lc && cc && $(lc)=="SUM") { print $(cc); found=1 }
    }
    END { if (!found) print 0 }
  ' "$tmp_csv"
}

kotlin="$(get_code_for_lang "Kotlin")"
xml="$(get_code_for_lang "XML")"
total="$(get_total_code)"

log "Parsed -> Kotlin=$kotlin XML=$xml TOTAL=$total"
rm -f "$tmp_csv"

# Ensure history + header
mkdir -p "$(dirname "$HISTORY_FILE")"
[ -f "$HISTORY_FILE" ] || printf "datetime,branch,tag,commit,kotlin_loc,xml_loc,total_loc\n" > "$HISTORY_FILE"

printf "%s,%s,%s,%s,%s,%s,%s\n" \
  "$when" "$branch" "$tag" "$commit" "$kotlin" "$xml" "$total" >> "$HISTORY_FILE"

echo "LOC snapshot -> $HISTORY_FILE"
echo "  Kotlin=$kotlin  XML=$xml  TOTAL=$total  [$branch $commit $tag]"
