#!/bin/bash
# Update a development asset and push to dev-assets branch

FILE_PATH=$1
if [ -z "$FILE_PATH" ]; then
    echo "Usage: $0 <file-path>"
    echo "Example: $0 .claude/skills/edge-component.md"
    exit 1
fi

if [ ! -f "$FILE_PATH" ]; then
    echo "ERROR: File $FILE_PATH does not exist"
    exit 1
fi

CURRENT_BRANCH=$(git branch --show-current)

# Stash current work if any
if ! git diff --quiet || ! git diff --staged --quiet; then
    echo "Stashing current changes..."
    git stash push -m "Auto-stash before updating dev asset"
    STASHED=1
fi

# Switch to dev-assets
git checkout dev-assets

# Copy the file from the feature branch
git checkout "$CURRENT_BRANCH" -- "$FILE_PATH"

# Commit
git add "$FILE_PATH"
git commit -m "Update $(basename $FILE_PATH)"
git push

# Switch back
git checkout "$CURRENT_BRANCH"

# Restore stash if we created one
if [ "$STASHED" = "1" ]; then
    echo "Restoring stashed changes..."
    git stash pop
fi

echo "✓ $FILE_PATH updated in dev-assets branch"
