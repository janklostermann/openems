#!/bin/bash
# Setup a new feature branch with development assets

FEATURE_NAME=$1
if [ -z "$FEATURE_NAME" ]; then
    echo "Usage: $0 <feature-name>"
    echo "Example: $0 bridge-analyzer"
    exit 1
fi

# Ensure we're up to date
git fetch origin

# Create feature branch from develop
git checkout develop
git pull
git checkout -b "feature/$FEATURE_NAME"

# Pull in dev assets without switching branches
echo "Pulling development assets..."
git checkout dev-assets -- .claude/ 2>/dev/null || echo "No .claude/ in dev-assets"
git checkout dev-assets -- scripts/ 2>/dev/null || echo "No scripts/ in dev-assets"
git checkout dev-assets -- doc/templates/ 2>/dev/null || echo "No doc/templates/ in dev-assets"

# Commit the assets if anything was added
if ! git diff --staged --quiet; then
    git commit -m "Add development assets to feature branch"
    echo "✓ Development assets added"
fi

echo ""
echo "Feature branch 'feature/$FEATURE_NAME' ready!"
echo "Development assets are available in .claude/, scripts/, and doc/templates/"
