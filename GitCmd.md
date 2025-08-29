# Git Command Notes

⚠️ This file is **just a note for myself**.  
Git does **not** read this file automatically.

To create an alias (global, for all repos):

```bash
git config --global alias.<name> '<command>'
```

Aliases live in my global config:
- Linux/Mac: `~/.gitconfig`
- Windows: `C:\Users\<me>\.gitconfig`

Remove an alias:
```bash
git config --global --unset alias.<name>
```

They are **not tied to a specific repo**.

---

## 📦 Repo utilities for LOC tracking

Recommended layout (self-contained, no PATH change):

```
<repo>/
 ├─ app/
 │   └─ src/
 ├─ tools/
 │   ├─ cloc-2.06.exe
 │   └─ git-loc-snapshot.sh
 └─ metrics/
     └─ loc_history.csv   (auto-created)
```

> Under Windows, if `cloc-2.06.exe` was downloaded from the Internet and is blocked, unblock it once (PowerShell):
> `Unblock-File .\tools\cloc-2.06.exe`

---

## 🚀 Aliases I use

### 1) `git savepoint` — Auto-save + timestamp tag + LOC snapshot

```bash
git config --global alias.savepoint '!f(){ \
  t=$(date +%Y-%m-%d_%H-%M-%S); \
  git add . && \
  git commit -m "Auto-save" && \
  git tag -a "savepoint-$t" -m "Archived version" && \
  git push origin HEAD && \
  git push origin "refs/tags/savepoint-$t"; \
  repo_root="$(git rev-parse --show-toplevel)"; \
  DEBUG=0 bash "$repo_root/tools/git-loc-snapshot.sh" "$repo_root/app/src" "$repo_root/metrics/loc_history.csv"; \
}; f'
```

### 2) `git release <branch>` — Create branch + tag + push + LOC snapshot

```bash
git config --global alias.release '!f(){ \
  branch="$1"; \
  if [ -z "$branch" ]; then echo "Usage: git release <branch-name>"; return 1; fi; \
  read -p "Enter release description: " desc; \
  git checkout -b "$branch" && \
  git add . && \
  git commit --allow-empty -m "Release $branch: $desc" && \
  git tag -a "$branch" -m "$desc" && \
  git push origin "refs/heads/$branch:refs/heads/$branch" && \
  git push origin "refs/tags/$branch:refs/tags/$branch"; \
  repo_root="$(git rev-parse --show-toplevel)"; \
  DEBUG=0 bash "$repo_root/tools/git-loc-snapshot.sh" "$repo_root/app/src" "$repo_root/metrics/loc_history.csv"; \
}; f'
```

### 3) `git cleanup <branch-or-tag>` — Delete safely (local + remote)

```bash
git config --global alias.cleanup '!f(){ \
  if [ -z "$1" ]; then echo "Usage: git cleanup <branch-or-tag>"; return 1; fi; \
  target="$1"; \
  current="$(git rev-parse --abbrev-ref HEAD)"; \
  if [ "$target" = "$current" ]; then echo "⛔ You are on [$target]. Switch first."; return 1; fi; \
  read -p "⚠️  Delete local+remote [$target]? (Y/N) " ans; \
  case "$ans" in \
    [Yy]) git branch -D "$target" 2>/dev/null || true; \
          git tag -d "$target" 2>/dev/null || true; \
          git push origin --delete "$target" 2>/dev/null || true; \
          echo "✅ [$target] deleted."; ;; \
    *) echo "❌ Cancelled." ;; \
  esac; \
}; f'
```

### 4) `git loc` — Manual LOC snapshot anytime

```bash
git config --global alias.loc '!f(){ \
  repo_root="$(git rev-parse --show-toplevel)"; \
  DEBUG=0 bash "$repo_root/tools/git-loc-snapshot.sh" "$repo_root/app/src" "$repo_root/metrics/loc_history.csv"; \
}; f'
```

---

## 🧰 Script: `tools/git-loc-snapshot.sh`

Counts Kotlin + XML under `app/src`, appends one CSV row with:
`datetime,branch,tag,commit,kotlin_loc,xml_loc,total_loc`.

> Tip: set `DEBUG=1` before running to see traces.

```bash
#!/usr/bin/env bash
set -euo pipefail

DEBUG="${DEBUG:-0}"
log(){ [ "$DEBUG" = "1" ] && echo "[DEBUG] $*"; }

# Windows → POSIX path helper (Git Bash)
to_posix(){ if [[ "$1" == /* ]]; then echo "$1"; elif command -v cygpath >/dev/null 2>&1; then cygpath -u "$1"; else echo "$1"; fi; }

REPO_ROOT_RAW="$(git rev-parse --show-toplevel)"
REPO_ROOT="$(to_posix "$REPO_ROOT_RAW")"
TARGET_PATH="$(to_posix "${1:-"$REPO_ROOT_RAW/app/src"}")"
HISTORY_FILE="$(to_posix "${2:-"$REPO_ROOT_RAW/metrics/loc_history.csv"}")"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CLOC_WIN="$SCRIPT_DIR/cloc-2.06.exe"

log "REPO_ROOT=$REPO_ROOT"
log "TARGET_PATH=$TARGET_PATH"
log "HISTORY_FILE=$HISTORY_FILE"
log "CLOC=$CLOC_WIN"

[ -f "$CLOC_WIN" ] || { echo "ERROR: cloc not found at $CLOC_WIN"; exit 1; }

branch="$(git rev-parse --abbrev-ref HEAD | tr -d '\r\n')"
commit="$(git rev-parse --short HEAD | tr -d '\r\n')"
if tag=$(git describe --tags --exact-match 2>/dev/null); then tag="${tag//$'\r'/}"; else tag=""; fi
when="$(date '+%Y-%m-%d %H:%M:%S')"

tmp_csv="$(mktemp)"
pushd "$TARGET_PATH" >/dev/null
"$CLOC_WIN" --include-ext=kt,xml --csv --quiet . > "$tmp_csv"
popd >/dev/null

# Detect header columns and extract code counts
get_code_for_lang(){ local lang="$1"; awk -F, -v L="$lang" '
  NR==1{ for(i=1;i<=NF;i++){gsub(/"/,"",$i); h[$i]=i }; lc=h["language"]; cc=h["code"]; next }
  { for(i=1;i<=NF;i++) gsub(/[\r"]/,"",$i); if(lc&&cc&&$(lc)==L){ print $(cc); found=1 } }
  END{ if(!found) print 0 }
' "$tmp_csv"; }

get_total_code(){ awk -F, '
  NR==1{ for(i=1;i<=NF;i++){gsub(/"/,"",$i); h[$i]=i }; lc=h["language"]; cc=h["code"]; next }
  { for(i=1;i<=NF;i++) gsub(/[\r"]/,"",$i); if(lc&&cc&&$(lc)=="SUM"){ print $(cc); found=1 } }
  END{ if(!found) print 0 }
' "$tmp_csv"; }

kotlin="$(get_code_for_lang "Kotlin")"
xml="$(get_code_for_lang "XML")"
total="$(get_total_code)"
rm -f "$tmp_csv"

mkdir -p "$(dirname "$HISTORY_FILE")"
[ -f "$HISTORY_FILE" ] || printf "datetime,branch,tag,commit,kotlin_loc,xml_loc,total_loc\n" > "$HISTORY_FILE"

printf "%s,%s,%s,%s,%s,%s,%s\n" "$when" "$branch" "$tag" "$commit" "$kotlin" "$xml" "$total" >> "$HISTORY_FILE"

echo "LOC snapshot -> $HISTORY_FILE"
echo "  Kotlin=$kotlin  XML=$xml  TOTAL=$total  [$branch $commit $tag]"
```

> 🔧 Pour compter plus d’extensions, modifie `--include-ext=kt,xml` (ex: `kt,xml,txt,md,json`).

---

## 🗂 Quick reference

* **`git release <branch>`**  
  Creates a new branch, asks for a description, commits (allow-empty), tags, pushes both, then writes a LOC snapshot.

* **`git savepoint`**  
  Auto-save commit `"Auto-save"`, timestamped tag `savepoint-YYYY-MM-DD_HH-MM-SS`, push branch+tag, then LOC snapshot.

* **`git cleanup <branch-or-tag>`**  
  Deletes local+remote (with confirmation). Prevents deleting current branch.

* **`git loc`**  
  Appends a LOC snapshot manually.

---

## 👀 Viewing the history

**Bash (quick):**
```bash
tail -n 10 metrics/loc_history.csv
```

**PowerShell (format as table):**
```powershell
Import-Csv metrics/loc_history.csv | Sort-Object datetime | Format-Table -AutoSize
```

**Optional chart (if `tools/plot-loc.sh` is present):**
```bash
bash tools/plot-loc.sh   # generates metrics/loc_history.html and opens it
```

---

## 🔧 Useful Git commands

Show all global aliases:
```bash
git config --get-regexp '^alias\.'
```

Where configs are stored:
```bash
git config --list --show-origin
```

Show all refs (branches + tags):
```bash
git show-ref --heads --tags
```
