Super 👍
Voici la version **Markdown (`GitCmd.md`)** de ton pense-bête, plus lisible dans un éditeur moderne (Android Studio, VS Code, GitHub…) :

````markdown
# Git Command Notes

⚠️ This file is **just a note for myself**.  
Git does **not** read this file automatically.  

If I want to make an alias real, I must run the line in a terminal once:

```bash
git config --global alias.<name> '<command>'
````

Aliases live in my global `~/.gitconfig` (Linux/Mac)
or `C:\Users\<me>\.gitconfig` (Windows).

They persist until I delete them with:

```bash
git config --global --unset alias.<name>
```

They are **not tied to a specific repo**: all repos share them.
They only disappear if I:

* uninstall Git and delete my `.gitconfig`
* or move to another machine (unless I copy `.gitconfig`)

---

## 🚀 Aliases I currently use

### 1. `git savepoint` — Auto-save with timestamp tag

Creates an auto-save commit + timestamp tag, then pushes branch + tag.

```bash
git config --global alias.savepoint '!t=$(date +%Y-%m-%d_%H-%M-%S) && git add . && git commit -m "Auto-save" && git tag -a "savepoint-$t" -m "Archived version" && git push origin HEAD && git push origin "refs/tags/savepoint-$t"'
```

---

### 2. `git release` — Create a release branch with tag

Creates & switches to a new branch, commits, tags, and pushes branch + tag.
Prompts for a description.

```bash
git config --global alias.release '!f() { \
  branch=$1; \
  shift; \
  if [ -z "$branch" ]; then \
    echo "Usage: git release <branch-name>"; \
    return 1; \
  fi; \
  read -p "Enter release description: " desc; \
  git checkout -b "$branch" && \
  git add . && \
  git commit --allow-empty -m "Release $branch: $desc" && \
  git tag -a "$branch" -m "$desc" && \
  git push origin "refs/heads/$branch:refs/heads/$branch" && \
  git push origin "refs/tags/$branch:refs/tags/$branch"; \
}; f'
```

---

### 3. `git cleanup` — Delete branch/tag safely

Deletes a branch or tag locally and remotely, **with confirmation**.
Prevents deletion if you are currently on the branch.

```bash
git config --global alias.cleanup '!f() { \
  if [ -z "$1" ]; then \
    echo "Usage: git cleanup <branch-or-tag>"; \
    return 1; \
  fi; \
  target=$1; \
  current=$(git rev-parse --abbrev-ref HEAD); \
  if [ "$target" = "$current" ]; then \
    echo "⛔ You are currently on [$target]. Switch to another branch before deleting it."; \
    return 1; \
  fi; \
  read -p "⚠️  Delete local and remote [$target]? (Y/N) " ans; \
  case "$ans" in \
    [Yy]) \
      git branch -D "$target" 2>/dev/null || true; \
      git tag -d "$target" 2>/dev/null || true; \
      git push origin --delete "$target" 2>/dev/null || true; \
      echo "✅ [$target] deleted (local + remote if existed).";; \
    *) echo "❌ Cancelled.";; \
  esac; \
}; f'
```

---

## 📖 Quick Reference

* **`git release <branch>`**

  * Creates a new branch with given name
  * Prompts for a release description
  * Commits (empty if nothing staged), tags, pushes both

* **`git savepoint`**

  * Auto-saves all changes
  * Commits with `"Auto-save"`
  * Creates timestamped tag (`savepoint-YYYY-MM-DD_HH-MM-SS`)
  * Pushes branch + tag

* **`git cleanup <branch-or-tag>`**

  * Deletes branch or tag both local and remote
  * Warns if you’re currently on it
  * Asks for confirmation (Y/N)

---

## 🔧 Useful Commands

* Show all global aliases:

  ```bash
  git config --get-regexp '^alias\.'
  ```

* Remove a specific alias:

  ```bash
  git config --global --unset alias.<name>
  ```

* Check where config is stored (global/system/local):

  ```bash
  git config --list --show-origin
  ```

* Show all refs (branches + tags):

  ```bash
  git show-ref --heads --tags
  ```

```
