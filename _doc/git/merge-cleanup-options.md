# Git Merge Cleanup Options

How to exclude development assets (`_pm/`, `.claude/`, `_scripts/`, `_doc/`) when merging feature branches to `develop`.

## The Problem

Feature branches contain development/management files that should NOT be merged to `develop` or upstream:
- `_pm/` - Project management files
- `.claude/` - Development skills/commands
- `_scripts/` - Our helper scripts
- `_doc/` - Our workflow documentation

## Option 1: Pre-commit Hook ⭐ Recommended for Safety

Add check to `.git/hooks/pre-commit` or enhance existing `tools/prepare-commit.sh`:

```bash
# In tools/prepare-commit.sh or custom hook
# Prevent dev assets from being committed to develop/master

CURRENT_BRANCH=$(git branch --show-current)

if [ "$CURRENT_BRANCH" = "develop" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    # Strict check for main branches
    if git diff --cached --name-only | grep -E "^(_pm/|_doc/|_scripts/|\.claude/)"; then
        echo "ERROR: Development assets cannot be committed to $CURRENT_BRANCH"
        echo "Found:"
        git diff --cached --name-only | grep -E "^(_pm/|_doc/|_scripts/|\.claude/)"
        echo ""
        echo "These files should only exist in feature branches."
        exit 1
    fi
fi
```

**Pros:**
- Catches mistakes automatically
- Prevents accidental commits to main branches
- Still allows commits in feature branches

**Cons:**
- Only prevents, doesn't clean
- Need to remember to run prepare-commit.sh

## Option 2: Pre-merge Preparation Script ⭐ Recommended for Workflow

Create `_scripts/prepare-merge-to-develop.sh`:

```bash
#!/bin/bash
# Prepare feature branch for merge to develop
# Removes development assets from git tracking

CURRENT_BRANCH=$(git branch --show-current)

if [ "$CURRENT_BRANCH" = "develop" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    echo "ERROR: Run this FROM your feature branch BEFORE merging to develop"
    echo "Current branch: $CURRENT_BRANCH"
    exit 1
fi

echo "Preparing $CURRENT_BRANCH for merge to develop..."
echo ""

# Check for uncommitted changes
if ! git diff --quiet || ! git diff --staged --quiet; then
    echo "ERROR: You have uncommitted changes. Commit or stash them first."
    git status --short
    exit 1
fi

# List what will be removed
echo "Development assets that will be removed from git:"
git ls-files _pm/ _doc/ _scripts/ .claude/ 2>/dev/null | sed 's/^/  - /'
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

# Remove from git but keep in working directory
git rm --cached -r _pm/ _doc/ _scripts/ .claude/ 2>/dev/null

# Add to .gitignore for this branch
cat >> .gitignore << 'EOF'

# Development assets (added by prepare-merge-to-develop.sh)
# These should only exist in feature branches, not in develop
_pm/
_doc/
_scripts/
.claude/
EOF

# Commit the removal
git add .gitignore
git commit -m "Prepare for merge to develop - remove dev assets

Removed development/management assets from git tracking:
- _pm/ (project management files)
- _doc/ (workflow documentation)
- _scripts/ (development helper scripts)
- .claude/ (skills and commands)

Files remain in working directory but are gitignored.
Safe to merge to develop.
"

echo ""
echo "✓ Development assets removed from git history"
echo "✓ .gitignore updated to exclude them"
echo "✓ Files still exist in your working directory"
echo ""
echo "Ready to merge to develop:"
echo "  git checkout develop"
echo "  git merge --no-ff $CURRENT_BRANCH"
echo ""
echo "After merge, to restore dev assets in new feature:"
echo "  git checkout -b feature/new-feature develop"
echo "  git checkout dev-assets -- _pm/ _doc/ _scripts/ .claude/"
```

**Pros:**
- Explicit, controlled process
- Files stay in working directory
- Creates clean commit before merge
- Reversible if needed

**Cons:**
- Manual step (need to remember)
- Could be automated more

## Option 3: .gitignore in Feature Branch

Add to feature branch's `.gitignore` before first commit:

```gitignore
_pm/
_doc/
_scripts/
.claude/
```

**Pros:**
- Simple, declarative
- Files never get tracked

**Cons:**
- Only works if added BEFORE first commit
- Doesn't help with already-tracked files
- We WANT them tracked in feature branches for collaboration

**Verdict:** Not suitable for our workflow

## Option 4: Manual Merge with Path Exclusion

Merge command excluding specific paths:

```bash
# Checkout develop
git checkout develop

# Start merge but don't commit
git merge --no-commit --no-ff feature/bridge-analyzer

# Unstage dev assets
git restore --staged _pm/ _doc/ _scripts/ .claude/

# Remove from working tree
git clean -fd _pm/ _doc/ _scripts/ .claude/

# Complete merge
git commit -m "Merge feature/bridge-analyzer"
```

**Pros:**
- Full control over what gets merged
- No script needed

**Cons:**
- Easy to forget steps
- Manual, error-prone
- Need to remember all paths

**Verdict:** Too manual, use script instead

## Option 5: Git Attributes + Merge Driver

Create custom merge driver that ignores specific paths:

**.gitattributes:**
```
_pm/** merge=ours
_doc/** merge=ours
_scripts/** merge=ours
.claude/** merge=ours
```

**.git/config:**
```ini
[merge "ours"]
    name = Keep ours merge strategy
    driver = true
```

**Pros:**
- Automatic during merge
- Git-native solution

**Cons:**
- Complex to set up
- Strategy keeps "ours" (develop's version, which is nothing)
- May cause confusion
- Doesn't prevent initial commit to develop

**Verdict:** Over-engineered for our needs

## Option 6: Stash/Unstash Pattern

Before merging, stash dev assets:

```bash
# In feature branch
git stash push -m "Dev assets before merge" \
    _pm/ _doc/ _scripts/ .claude/

# Now merge to develop (assets not in history)
git checkout develop
git merge feature/bridge-analyzer

# Later, restore in new feature
git checkout -b feature/new-thing develop
git stash pop
```

**Pros:**
- Quick and simple
- Reversible

**Cons:**
- Stash is local (not pushed)
- Loses assets if stash is dropped
- Assets not in any branch (risky)

**Verdict:** Too risky, assets could be lost

## Recommended Approach: **Combination of Option 1 + 2**

### For Safety (Option 1):
Enhance `tools/prepare-commit.sh` to prevent commits to develop/master:

```bash
# Add safety check to existing script
_scripts/update-prepare-commit.sh
```

### For Workflow (Option 2):
Create cleanup script for controlled preparation:

```bash
# Before merging to develop, run:
_scripts/prepare-merge-to-develop.sh

# Then merge normally:
git checkout develop
git merge --no-ff feature/bridge-analyzer
```

### Recovery Process:
After merge, to start new feature with dev assets:

```bash
# Create new feature from clean develop
git checkout -b feature/new-feature develop

# Pull dev assets from dev-assets branch
git checkout dev-assets -- _pm/ _doc/ _scripts/ .claude/

# Commit them to this feature branch
git commit -m "Add development assets to feature branch"
```

Or use helper script:
```bash
_scripts/setup-feature-branch.sh new-feature
```

## Implementation Checklist

- [ ] Create `_scripts/prepare-merge-to-develop.sh`
- [ ] Update `tools/prepare-commit.sh` with dev-asset check
- [ ] Update `_doc/managing-dev-assets.md` with merge process
- [ ] Update `_scripts/setup-feature-branch.sh` to pull from correct paths
- [ ] Test workflow:
  - [ ] Create dummy feature branch
  - [ ] Add dev assets
  - [ ] Run prepare-merge script
  - [ ] Verify clean merge to develop
  - [ ] Verify assets excluded
- [ ] Document in collaboration.md skill

## References

- [Git Merge Strategies](https://git-scm.com/docs/merge-strategies)
- [Git Attributes](https://git-scm.com/docs/gitattributes)
- [Managing Dev Assets](_doc/managing-dev-assets.md)

---

**Document Status:** Decision Guide
**Created:** 2025-11-23
**Recommended:** Option 1 + 2 (Safety check + Preparation script)
