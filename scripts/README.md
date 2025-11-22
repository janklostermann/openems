# Development Scripts

Helper scripts for managing development workflows in the OpenEMS fork.

## Available Scripts

### `setup-feature-branch.sh`
Create a new feature branch with development assets automatically included.

```bash
./scripts/setup-feature-branch.sh <feature-name>
```

**Example:**
```bash
./scripts/setup-feature-branch.sh my-new-feature
```

This will:
1. Create a new branch `feature/my-new-feature` from `develop`
2. Pull in development assets from `dev-assets` branch
3. Commit the assets to the new branch

### `sync-dev-assets.sh`
Sync the latest development assets into your current feature branch.

```bash
./scripts/sync-dev-assets.sh
```

Use this when:
- Someone has updated skills or scripts in `dev-assets`
- You want to get the latest development tools
- You've been working on a branch for a while and want updates

**Safety:** Prevents running on `develop` or `master` branches.

### `update-dev-asset.sh`
Push changes you made to a development asset back to the `dev-assets` branch.

```bash
./scripts/update-dev-asset.sh <file-path>
```

**Example:**
```bash
./scripts/update-dev-asset.sh .claude/skills/edge-component.md
```

This will:
1. Switch to `dev-assets` branch
2. Copy your modified file
3. Commit and push to `dev-assets`
4. Switch back to your feature branch

## Workflow Examples

### Starting a New Feature

```bash
# Option 1: Use the helper script (recommended)
./scripts/setup-feature-branch.sh authentication

# Option 2: Manual setup
git checkout develop
git pull
git checkout -b feature/authentication
git checkout dev-assets -- .claude/ scripts/ doc/templates/
git commit -m "Add development assets"
```

### Updating Your Assets

```bash
# While working on feature/bridge-analyzer
# Someone updated the edge-component skill

./scripts/sync-dev-assets.sh
# Review changes
git status
# Commit if you want to keep them
git commit -m "Sync latest development assets"
```

### Improving a Skill

```bash
# While on feature/bridge-analyzer
# You improved edge-component.md

# Edit the file
vim .claude/skills/edge-component.md

# Push it back to dev-assets
./scripts/update-dev-asset.sh .claude/skills/edge-component.md
```

## See Also

- [Managing Development Assets](../doc/managing-dev-assets.md) - Complete documentation
- `.claude/skills/` - Available Claude skills
- `doc/templates/` - Document templates

## Troubleshooting

**Script not executable?**
```bash
chmod +x scripts/*.sh
```

**Assets not found?**
Make sure the `dev-assets` branch exists:
```bash
git fetch origin dev-assets
git branch -a | grep dev-assets
```

**Want to see what's in dev-assets?**
```bash
git ls-tree -r dev-assets --name-only
```
