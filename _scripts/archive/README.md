# Archived Scripts

Scripts that are no longer used but kept for reference.

## prepare-merge-to-develop.sh

**Status:** Obsolete (2025-11-23)

**Reason:** Workflow clarification - our `develop` branch is just an inbox for upstream changes. We don't merge feature branches to develop. Instead:
- Work in `feature/*` branches
- Use `prepare-for-PR.sh` to create clean `pr/*` branches
- Create PRs from `pr/*` to upstream
- Pull upstream changes into `develop`
- Merge `develop` into `feature/*` to stay current

**Original Purpose:** Removed dev assets from feature branch before merging to develop.

**Kept for:** Reference implementation of git cleanup approach.
