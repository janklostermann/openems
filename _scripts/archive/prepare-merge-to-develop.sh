#!/bin/bash
# Prepare feature branch for merge to develop
# Removes development assets from git tracking while keeping them in working directory

set -e  # Exit on error

CURRENT_BRANCH=$(git branch --show-current)

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "Prepare Merge to Develop"
echo "========================================="
echo ""

# Safety check: Must run from feature branch
if [ "$CURRENT_BRANCH" = "develop" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    echo -e "${RED}ERROR: Run this FROM your feature branch BEFORE merging to develop${NC}"
    echo "Current branch: $CURRENT_BRANCH"
    echo ""
    echo "Correct workflow:"
    echo "  1. Be on feature branch (e.g., feature/bridge-analyzer)"
    echo "  2. Run this script"
    echo "  3. Then: git checkout develop && git merge feature/bridge-analyzer"
    exit 1
fi

# Safety check: No uncommitted changes
if ! git diff --quiet || ! git diff --staged --quiet; then
    echo -e "${RED}ERROR: You have uncommitted changes${NC}"
    echo "Commit or stash them first, then run this script again."
    echo ""
    git status --short
    exit 1
fi

# Safety check: In git repository root
if [ ! -d ".git" ]; then
    echo -e "${RED}ERROR: Must run from repository root${NC}"
    exit 1
fi

echo "Current branch: $CURRENT_BRANCH"
echo ""

# List dev assets that will be removed from git
DEV_ASSETS=("_pm/" "_doc/" "_scripts/" ".claude/")
FOUND_ASSETS=()

echo "Checking for development assets..."
for asset in "${DEV_ASSETS[@]}"; do
    if git ls-files "$asset" 2>/dev/null | grep -q .; then
        FOUND_ASSETS+=("$asset")
    fi
done

if [ ${#FOUND_ASSETS[@]} -eq 0 ]; then
    echo -e "${YELLOW}No development assets found in git tracking.${NC}"
    echo "Nothing to do. Branch is already clean."
    exit 0
fi

echo ""
echo -e "${YELLOW}The following development assets will be removed from git:${NC}"
for asset in "${FOUND_ASSETS[@]}"; do
    count=$(git ls-files "$asset" 2>/dev/null | wc -l)
    echo "  - $asset ($count files)"
done
echo ""
echo -e "${GREEN}These files will remain in your working directory${NC}"
echo "They will be added to .gitignore to prevent re-tracking"
echo ""

# Confirmation
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted. No changes made."
    exit 0
fi

echo ""
echo "Removing development assets from git tracking..."

# Remove from git but keep in working directory
for asset in "${FOUND_ASSETS[@]}"; do
    if git ls-files "$asset" 2>/dev/null | grep -q .; then
        echo "  Removing $asset from git..."
        git rm --cached -r "$asset" 2>/dev/null || true
    fi
done

# Add or update .gitignore
echo ""
echo "Updating .gitignore..."

# Check if .gitignore exists and if it already has our markers
if [ -f ".gitignore" ] && grep -q "# Development assets (managed by prepare-merge-to-develop.sh)" .gitignore 2>/dev/null; then
    echo "  .gitignore already contains development asset exclusions"
else
    # Add development asset exclusions to .gitignore
    cat >> .gitignore << 'EOF'

# Development assets (managed by prepare-merge-to-develop.sh)
# These should only exist in feature branches, not in develop
_pm/
_doc/
_scripts/
.claude/
EOF
    echo "  Added development asset exclusions to .gitignore"
fi

# Stage .gitignore
git add .gitignore

# Create commit
echo ""
echo "Creating cleanup commit..."
git commit -m "Prepare for merge to develop - remove dev assets

Removed development/management assets from git tracking:
- _pm/ (project management files)
- _doc/ (workflow documentation)
- _scripts/ (development helper scripts)
- .claude/ (skills and commands)

Files remain in working directory but are now gitignored.
Safe to merge to develop without polluting production branches.

Part of our fork's development workflow - see dev-assets branch.
"

echo ""
echo -e "${GREEN}✓ Development assets removed from git history${NC}"
echo -e "${GREEN}✓ .gitignore updated to exclude them${NC}"
echo -e "${GREEN}✓ Files still exist in your working directory${NC}"
echo ""
echo "========================================="
echo "Ready to merge to develop!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. git checkout develop"
echo "  2. git pull  # Get latest develop"
echo "  3. git merge --no-ff $CURRENT_BRANCH"
echo "  4. git push"
echo ""
echo "After merge, to create new feature with dev assets:"
echo "  _scripts/setup-feature-branch.sh new-feature-name"
echo ""
