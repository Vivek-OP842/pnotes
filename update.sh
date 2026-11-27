#!/usr/bin/env bash
# Push app changes to GitHub. Usage: ./update.sh "what you changed"
# Only the pnotes/ app folder (and .gitignore) is ever staged.
set -euo pipefail
cd "$(dirname "$0")"

MSG="${1:-update app}"
BRANCH="master"

git add pnotes .gitignore

if git diff --cached --quiet; then
  echo "Koi app change nahi mila — kuch push karne ko nahi hai."
  exit 0
fi

echo "--- Staged ---"
git diff --cached --stat

git commit -m "$MSG"
git push origin "$BRANCH"
echo "Done: https://github.com/Vivek-OP842/pnotes"
