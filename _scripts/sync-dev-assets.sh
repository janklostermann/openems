#!/bin/bash
# Sync latest development assets into current branch

CURRENT_BRANCH=$(git branch --show-current)

if [ "$CURRENT_BRANCH" = "develop" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    echo "ERROR: Don't sync dev assets into develop or master!"
    exit 1
fi

echo "Syncing development assets from dev-assets branch..."
git fetch origin dev-assets

git checkout origin/dev-assets -- .claude/ 2>/dev/null || echo "No .claude/ in dev-assets"
git checkout origin/dev-assets -- _scripts/ 2>/dev/null || echo "No _scripts/ in dev-assets"
git checkout origin/dev-assets -- _doc/ 2>/dev/null || echo "No _doc/ in dev-assets"
# Note: _pm/ is project-specific and not synced from dev-assets

if ! git diff --staged --quiet; then
    echo ""
    echo "Development assets updated. Review and commit:"
    git status --short
    echo ""
    echo "Run: git commit -m 'Sync latest development assets'"
else
    echo "✓ Already up to date"
fi
