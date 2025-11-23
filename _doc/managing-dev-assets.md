# Managing Development Assets in OpenEMS Fork

This document describes how we manage development assets (Claude skills, scripts, templates, project management files) in our OpenEMS fork without polluting upstream contributions.

## The Challenge

We want to maintain development helpers like:
- `.claude/skills/` - Claude Code skills for common development tasks
- `_scripts/` - Development automation scripts
- `_doc/` - Our workflow documentation and templates
- `_pm/` - Project management files (per-feature)

But we **don't** want these to:
- Accidentally get included in upstream PRs
- Create conflicts with upstream updates
- Clutter the upstream repository

## Git Workflow - Understanding Branch Roles

**Critical insight:** Our `develop` branch is just an **inbox for upstream changes**, NOT a merge target.

```
upstream/develop (OpenEMS upstream repository)
  ↓ (we pull from upstream)
develop (our fork - tracks upstream, rarely touch directly)
  ↓ (create feature branches from here)
feature/bridge-analyzer (our work + dev assets)
  ↓ (prepare-for-PR.sh removes dev assets)
pr/bridge-analyzer (clean, for upstream PR)
  ↓ (create PR to upstream)
upstream/develop (our contribution merged back)
```

### Branch Purposes

- **`develop`** - Mirrors upstream, our inbox for upstream changes
- **`dev-assets`** - Contains reusable development helpers (`.claude/`, `_scripts/`, `_doc/`)
- **`feature/*`** - Our working branches (code + dev assets + `_pm/`)
- **`pr/*`** - Clean branches for upstream PRs (code only, no dev assets)

## Solution: Multi-Branch Strategy

### 1. `dev-assets` Branch - Reusable Development Helpers

Contains shared development assets that are useful across multiple features:
- `.claude/skills/` - Claude skills for OpenEMS development
- `_scripts/` - Helper scripts for git workflow, branch setup, etc.
- `_doc/` - Workflow documentation and document templates

**Does NOT contain:**
- `_pm/` folders (these are project-specific, live only in feature branches)

### 2. `feature/*` Branches - Working Branches

Where actual development happens:
- Production code (bundles, components, etc.)
- Dev assets pulled from `dev-assets` branch
- Project-specific `_pm/<project-name>/` folder

### 3. `pr/*` Branches - Clean Branches for Upstream

Created from `feature/*` branches with dev assets removed:
- Production code only
- No `_pm/`, `_doc/`, `_scripts/`, `.claude/`
- Ready for upstream pull request

## Workflows

### Initial Setup (One-time)

Create the `dev-assets` branch:

```bash
# Start from develop
git checkout develop
git pull upstream develop

# Create dev-assets branch
git checkout -b dev-assets

# Add development assets
mkdir -p .claude/skills
mkdir -p _scripts
mkdir -p _doc/git
mkdir -p _doc/templates

# Add your files (skills, scripts, docs)
git add .claude/ _scripts/ _doc/
git commit -m "Initialize development assets"
git push -u origin dev-assets
```

### Starting a New Feature

**Manual approach:**
```bash
# Get latest upstream changes
git checkout develop
git pull upstream develop
git push origin develop

# Create feature branch from develop
git checkout -b feature/my-feature

# Pull in dev assets from dev-assets branch
git checkout dev-assets -- .claude/
git checkout dev-assets -- _scripts/
git checkout dev-assets -- _doc/

# Commit the assets
git commit -m "Add development assets to feature branch"

# Create project management folder
mkdir -p _pm/my-feature
# (add tasks.md, decisions.md, etc. - see collaboration.md skill)
```

**Scripted approach (recommended):**
```bash
# Get latest upstream
git checkout develop
git pull upstream develop

# Create feature with dev assets
_scripts/setup-feature-branch.sh my-feature

# Manually create _pm/my-feature/ folder and files
```

### Working in Feature Branch

1. **Make changes** to production code and dev assets as needed
2. **Commit regularly** - dev assets can be committed in feature branches
3. **Keep current** with upstream:
   ```bash
   git checkout develop
   git pull upstream develop
   git checkout feature/my-feature
   git merge develop
   ```

### Updating Development Assets

When you improve a skill or add a new script that should be shared:

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
git checkout feature/my-feature

# Pull in the updates
git checkout dev-assets -- .claude/
git commit -m "Sync latest development assets"
```

**Or use the helper script:**
```bash
# From feature branch, update a file in dev-assets
_scripts/update-dev-asset.sh .claude/skills/edge-component.md

# This automatically commits to dev-assets and returns to feature branch
```

### Creating Upstream PR

**Important:** Never create PR directly from `feature/*` branches!

**Use the preparation script:**
```bash
# In your feature branch (e.g., feature/bridge-analyzer)
_scripts/prepare-for-PR.sh

# This creates a clean pr/bridge-analyzer branch without dev assets
# Then create PR from pr/bridge-analyzer to upstream
```

**Manual approach (if script not ready):**
```bash
# In feature branch
git checkout -b pr/bridge-analyzer

# Remove dev assets from git tracking
git rm -r --cached _pm/ _doc/ _scripts/ .claude/

# Add to .gitignore
cat >> .gitignore << 'EOF'

# Development assets (not for upstream)
_pm/
_doc/
_scripts/
.claude/
EOF

# Commit cleanup
git add .gitignore
git commit -m "Remove dev assets for upstream PR"

# Push and create PR
git push -u origin pr/bridge-analyzer
# Create PR from pr/bridge-analyzer to upstream/develop
```

### Syncing Dev Assets Across Team Members

When someone else updates the dev-assets:

```bash
# In your feature branch
_scripts/sync-dev-assets.sh

# Reviews changes and commits if needed
```

**Or manually:**
```bash
git fetch origin dev-assets
git checkout origin/dev-assets -- .claude/ _scripts/ _doc/
git commit -m "Sync latest development assets"
```

## Development Assets Structure

**Underscore-prefixed folders** = our development/management assets (not for upstream):

```
openems/
├── .claude/                     # Claude Code skills and commands
│   ├── commands/                # Custom slash commands
│   └── skills/                  # Claude skills for common tasks
│       ├── edge-component.md
│       ├── collaboration.md
│       └── ...
│
├── _scripts/                    # Development helper scripts
│   ├── setup-feature-branch.sh
│   ├── sync-dev-assets.sh
│   ├── update-dev-asset.sh
│   ├── prepare-for-PR.sh
│   └── archive/                 # Obsolete scripts kept for reference
│
├── _doc/                        # Our workflow documentation
│   ├── managing-dev-assets.md   # This file
│   ├── git/
│   │   └── merge-cleanup-options.md
│   └── templates/
│       ├── requirements.adoc
│       └── implementation-notes.adoc
│
└── _pm/                         # Project management (per-feature)
    └── bridge-analyzer/         # Example: matches feature branch name
        ├── tasks.md             # Current tasks, next up
        ├── decisions.md         # Architectural decisions
        ├── open-questions.md    # Blockers, parking lot
        ├── notes.md             # Context, references
        └── archive/             # Completed work
```

**Convention:** Files/folders with underscore prefix (`_*`) are ours, everything else is project/upstream code.

## Helper Scripts

We provide several helper scripts in `_scripts/` to automate common workflow tasks.

**See [_scripts/README.md](../_scripts/README.md) for detailed documentation of all available scripts.**

**Quick reference:**
- `setup-feature-branch.sh` - Create new feature with dev assets
- `sync-dev-assets.sh` - Pull latest dev assets into current branch
- `update-dev-asset.sh` - Push improved asset back to dev-assets branch
- `prepare-for-PR.sh` - Create clean pr/* branch for upstream (in development)

## Best Practices

### DO:
- ✓ Keep shared development helpers in `dev-assets` branch
- ✓ Pull dev assets into every feature branch
- ✓ Commit dev assets freely in `feature/*` branches
- ✓ Use `_pm/<project-name>/` for project-specific management files
- ✓ Always create upstream PRs from clean `pr/*` branches
- ✓ Pull upstream changes into `develop`, then merge `develop` → `feature/*`
- ✓ Update assets in `dev-assets` when you improve them
- ✓ Use underscore prefix for all our management files/folders

### DON'T:
- ✗ Don't commit dev assets to `develop` branch
- ✗ Don't create PRs directly from `feature/*` branches
- ✗ Don't merge `feature/*` branches to `develop` (develop tracks upstream only!)
- ✗ Don't merge `dev-assets` directly into `develop`
- ✗ Don't edit assets in feature branches without syncing back to `dev-assets`
- ✗ Don't use platform-specific scripts (keep them portable)

## Common Scenarios

### Scenario 1: Starting New Feature

```bash
git checkout develop
git pull upstream develop
_scripts/setup-feature-branch.sh my-feature
# Manually create _pm/my-feature/ with tasks.md, decisions.md, etc.
```

### Scenario 2: Contributing to Upstream

```bash
# In feature/my-feature after work is done
_scripts/prepare-for-PR.sh
# Creates pr/my-feature
# Create PR from pr/my-feature → upstream/develop
```

### Scenario 3: Upstream Merged My PR

```bash
# Update develop with latest upstream (includes your merged PR)
git checkout develop
git pull upstream develop

# Update or delete your branches
git branch -d pr/my-feature      # Delete PR branch (no longer needed)
git branch -d feature/my-feature # Or keep for future work

# Start next feature
_scripts/setup-feature-branch.sh next-feature
```

### Scenario 4: Keep Feature Branch Current

```bash
# Get latest upstream changes
git checkout develop
git pull upstream develop

# Merge into your feature
git checkout feature/my-feature
git merge develop

# Resolve any conflicts, commit
```

### Scenario 5: Improve a Skill

```bash
# From feature branch
vim .claude/skills/edge-component.md

# Push improvement to dev-assets
_scripts/update-dev-asset.sh .claude/skills/edge-component.md

# Other team members can now sync it
```

## Maintenance

### Adding a New Asset

1. Switch to `dev-assets`: `git checkout dev-assets`
2. Add your file: `vim .claude/skills/new-skill.md`
3. Commit and push: `git add ... && git commit -m "Add new-skill" && git push`
4. In feature branches: `_scripts/sync-dev-assets.sh`

### Removing an Obsolete Asset

1. Switch to `dev-assets`: `git checkout dev-assets`
2. Remove the file: `git rm .claude/skills/old-skill.md`
3. Commit and push: `git commit -m "Remove obsolete old-skill" && git push`
4. In feature branches: Next sync will remove it

### Reviewing Asset Changes

```bash
# See what's different between your branch and dev-assets
git diff dev-assets -- .claude/
git diff dev-assets -- _scripts/
git diff dev-assets -- _doc/
```

## Troubleshooting

### "I accidentally committed assets to develop"

```bash
# If not yet pushed
git checkout develop
git rebase -i HEAD~N  # N = number of commits back
# Mark the asset commit as 'drop' or 'edit' and remove the files

# If already pushed (use with caution - coordinate with team)
git checkout develop
git revert <commit-hash>
git push
```

### "I created a PR from feature/* instead of pr/*"

```bash
# Close the PR on GitHub
# Create correct PR branch
_scripts/prepare-for-PR.sh
# Create new PR from pr/* branch
```

### "Assets are out of sync across branches"

```bash
# Authoritative source is always dev-assets
git checkout dev-assets
git log --oneline -- .claude/  # See history

# Force sync to your branch
git checkout feature/my-feature
git checkout dev-assets -- .claude/ _scripts/ _doc/
git commit -m "Force sync development assets"
```

### "Merge conflicts in asset files"

```bash
# Always prefer dev-assets version
git checkout --theirs .claude/
git checkout --theirs _scripts/
git checkout --theirs _doc/
git add .claude/ _scripts/ _doc/
```

### "How do I keep develop updated?"

```bash
# Simple: just pull from upstream
git checkout develop
git pull upstream develop
git push origin develop  # Update our fork's develop on GitHub
```

## Future Enhancements

Potential improvements to this system:

1. **Pre-commit hook**: Prevent committing assets to `pr/*` or `develop` branches
2. **CI/CD check**: Verify `pr/*` branches don't contain dev assets before merge
3. **Asset registry**: Maintain a manifest of available assets
4. **Shared asset repo**: Extract to separate repository for multi-project use
5. **Template instantiation**: Scripts to instantiate templates with placeholders

## References

- [Git Workflow Best Practices](https://git-scm.com/book/en/v2/Git-Branching-Branching-Workflows)
- [Fork and Pull Request Workflow](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
- [OpenEMS Contributing Guidelines](../CONTRIBUTING.md)
- [Merge Cleanup Options](_doc/git/merge-cleanup-options.md)
- [Collaboration Workflow](.claude/skills/collaboration.md)

---

**Document Status:** Living Document
**Last Updated:** 2025-11-23
**Maintained By:** Development Team
