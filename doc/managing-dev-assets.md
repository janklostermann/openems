# Managing Development Assets in OpenEMS Fork

This document describes how we manage development assets (Claude skills, scripts, templates) in our OpenEMS fork without polluting the upstream merge path.

## The Challenge

We want to maintain development helpers like:
- `.claude/skills/` - Claude Code skills for common development tasks
- `scripts/` - Development automation scripts
- `doc/templates/` - Document templates

But we **don't** want these to:
- Accidentally merge into `develop` or upstream
- Create conflicts with upstream updates
- Clutter production branches

## Solution: Dedicated `dev-assets` Branch

We maintain a separate `dev-assets` branch that contains all development helpers. When creating feature branches, we pull these assets in, but we're careful not to merge them back.

## Architecture

```
develop (clean, upstream-compatible)
  │
  ├── feature/bridge-analyzer (has dev assets)
  ├── feature/another-feature (has dev assets)
  └── ...

dev-assets (development helpers only)
  ├── .claude/skills/
  ├── scripts/
  └── doc/templates/
```

## Options Considered

### Option 1: Dedicated `dev-assets` Branch ✅ **CHOSEN**
**Approach:** Separate branch for development assets, merged into feature branches as needed

**Pros:**
- Clean separation of development assets from production code
- Easy to merge into any feature branch
- Won't conflict with upstream updates
- Can version control evolution of development practices

**Cons:**
- Extra step when creating feature branches
- Need to remember to pull in the assets

### Option 2: Local Template Repository
**Approach:** Keep development assets in a completely separate repo

**Pros:**
- Completely independent versioning
- Can be shared across multiple OpenEMS forks
- Zero risk of accidental upstream merge

**Cons:**
- More complex setup
- Not automatically in the context

### Option 3: `.gitignore` with Manual Sync
**Approach:** Add `.claude/` to `.gitignore`, maintain separately

**Pros:**
- Never risks upstream contamination
- Total flexibility

**Cons:**
- Manual process, error-prone
- Not version controlled with the code

### Option 4: Feature Branch Template
**Approach:** Maintain a `feature-template` branch with assets pre-merged

**Pros:**
- Consistent starting point for all features
- Assets always available

**Cons:**
- Extra branch to maintain
- Need discipline to not merge assets back

## Workflows

### Initial Setup (One-time)

Create the `dev-assets` branch:

```bash
# Start from develop
git checkout develop
git pull

# Create dev-assets branch
git checkout -b dev-assets

# Add development assets
mkdir -p .claude/skills
mkdir -p scripts
mkdir -p doc/templates

# Add your files (see structure below)
git add .claude/ scripts/ doc/templates/
git commit -m "Initialize development assets"
git push -u origin dev-assets
```

### Starting a New Feature Branch

**Manual approach:**
```bash
# Create feature branch from develop
git checkout develop
git pull
git checkout -b feature/my-feature

# Pull in dev assets
git checkout dev-assets -- .claude/
git checkout dev-assets -- scripts/
git checkout dev-assets -- doc/templates/

# Commit the assets
git commit -m "Add development assets to feature branch"
```

**Scripted approach (recommended):**
```bash
./scripts/setup-feature-branch.sh my-feature
```

### Updating Development Assets

When you improve a skill or add a new script:

```bash
# Switch to dev-assets
git checkout dev-assets

# Make your changes
vim .claude/skills/edge-component.md

# Commit to dev-assets
git add .claude/skills/edge-component.md
git commit -m "Improved edge-component skill with better examples"
git push

# Switch back to your feature branch
git checkout feature/bridge-analyzer

# Pull in the updates
git checkout dev-assets -- .claude/
git commit -m "Update development assets"
```

### Merging Feature Back to Develop

**Important:** Don't merge the development assets back!

**Option A: Unstage assets before commit**
```bash
git checkout develop
git merge --no-commit feature/bridge-analyzer

# Unstage development assets
git restore --staged .claude/
git restore --staged scripts/
git restore --staged doc/templates/

# Remove from working directory if they were added
git clean -fd .claude/ scripts/ doc/templates/

# Commit the merge (without assets)
git commit
```

**Option B: Merge only specific paths**
```bash
git checkout develop
git checkout feature/bridge-analyzer -- io.openems.edge.bridgeanalyzer.api/
git checkout feature/bridge-analyzer -- io.openems.edge.bridgeanalyzer.modbus/
# ... only the actual feature code

git commit -m "Merge feature/bridge-analyzer"
```

**Option C: Interactive rebase to drop asset commits**
```bash
git checkout develop
git merge feature/bridge-analyzer
# If assets got merged accidentally:
git rebase -i HEAD~N  # where N includes the merge
# Mark asset commits as 'drop'
```

### Syncing dev-assets Across Team Members

When someone else updates the dev-assets:

```bash
# In your feature branch
git fetch origin dev-assets

# Pull in latest assets
git checkout origin/dev-assets -- .claude/
git commit -m "Sync latest development assets"
```

## Development Assets Structure

```
openems/
├── .claude/
│   ├── commands/           # Custom slash commands
│   └── skills/             # Claude skills for common tasks
│       ├── edge-component.md
│       ├── requirements-doc.md
│       └── ...
├── scripts/
│   ├── setup-feature-branch.sh
│   ├── sync-dev-assets.sh
│   └── prepare-commit.sh   # Pre-commit checks
└── doc/
    └── templates/
        ├── requirements.adoc
        ├── implementation-notes.adoc
        └── ...
```

## Helper Scripts

### `scripts/setup-feature-branch.sh`

```bash
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
```

### `scripts/sync-dev-assets.sh`

```bash
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
git checkout origin/dev-assets -- scripts/ 2>/dev/null || echo "No scripts/ in dev-assets"
git checkout origin/dev-assets -- doc/templates/ 2>/dev/null || echo "No doc/templates/ in dev-assets"

if ! git diff --staged --quiet; then
    echo ""
    echo "Development assets updated. Review and commit:"
    git status --short
    echo ""
    echo "Run: git commit -m 'Sync latest development assets'"
else
    echo "✓ Already up to date"
fi
```

### `scripts/update-dev-asset.sh`

```bash
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
```

## Best Practices

### DO:
- ✓ Keep all development helpers in `dev-assets` branch
- ✓ Use scripts to automate pulling assets into feature branches
- ✓ Update assets in `dev-assets` when you improve them
- ✓ Pull asset updates regularly into your feature branch
- ✓ Document new skills/scripts in this file

### DON'T:
- ✗ Don't commit development assets to `develop` or `master`
- ✗ Don't merge `dev-assets` directly into `develop`
- ✗ Don't edit assets in feature branches without syncing back to `dev-assets`
- ✗ Don't use platform-specific scripts (keep them portable)

## Maintenance

### Adding a New Asset

1. Switch to `dev-assets`: `git checkout dev-assets`
2. Add your file: `vim .claude/skills/new-skill.md`
3. Commit and push: `git add ... && git commit -m "Add new-skill" && git push`
4. In feature branches: `git checkout dev-assets -- .claude/skills/new-skill.md`

### Removing an Obsolete Asset

1. Switch to `dev-assets`: `git checkout dev-assets`
2. Remove the file: `git rm .claude/skills/old-skill.md`
3. Commit and push: `git commit -m "Remove obsolete old-skill" && git push`
4. In feature branches: Next sync will remove it

### Reviewing Asset Changes

```bash
# See what's different between your branch and dev-assets
git diff dev-assets -- .claude/
```

## Troubleshooting

### "I accidentally committed assets to develop"

```bash
# If not yet pushed
git checkout develop
git rebase -i HEAD~N  # N = number of commits back
# Mark the asset commit as 'drop' or 'edit' and remove the files

# If already pushed (use with caution)
git checkout develop
git revert <commit-hash>
git push
```

### "Assets are out of sync across branches"

```bash
# Authoritative source is always dev-assets
git checkout dev-assets
git log --oneline -- .claude/  # See history

# Force sync to your branch
git checkout feature/my-feature
git checkout dev-assets -- .claude/ scripts/ doc/templates/
git commit -m "Force sync development assets"
```

### "Merge conflicts in asset files"

```bash
# Always prefer dev-assets version
git checkout --theirs .claude/
git add .claude/
```

## Future Enhancements

Potential improvements to this system:

1. **Git hooks**: Pre-commit hook to prevent committing assets to develop
2. **CI/CD check**: Verify develop branch doesn't contain .claude/ or scripts/
3. **Asset registry**: Maintain a manifest of available assets
4. **Shared asset repo**: Extract to separate repository for multi-project use
5. **Template instantiation**: Scripts to instantiate templates with placeholders

## References

- [Git Workflow Best Practices](https://git-scm.com/book/en/v2/Git-Branching-Branching-Workflows)
- [Fork and Pull Request Workflow](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
- [OpenEMS Contributing Guidelines](../CONTRIBUTING.md)

---

**Document Status:** Living Document
**Last Updated:** 2025-11-22
**Maintained By:** Development Team
