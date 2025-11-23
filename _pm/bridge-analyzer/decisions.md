---
project: Bridge Analyzer
---

# Decisions

## 2025-11-23: Git Workflow Clarification - Develop is Upstream Inbox Only
- **Requested by:** jk
- **Decision:** `develop` branch is just an inbox for upstream changes, NOT a merge target
- **Correct Workflow:**
  1. Pull upstream changes → `develop`
  2. Create `feature/*` from `develop` (with dev assets)
  3. Work in `feature/*` branches
  4. Create clean `pr/*` branch from `feature/*` (without dev assets)
  5. Submit PR from `pr/*` to upstream
  6. Keep `feature/*` updated: merge `develop` into `feature/*`
- **Rationale:** We're a fork contributing to upstream, not maintaining independent develop branch
- **Impact:** Obsoleted prepare-merge-to-develop.sh, focus on prepare-for-PR.sh

## 2025-11-23: Separate PR Branches from Feature Branches
- **Requested by:** jk
- **Decision:** Use separate `pr/*` branches (lowercase) for upstream PRs
- **Workflow:**
  - Work in `feature/bridge-analyzer` (with `_pm/`, `.claude/`, `_scripts/`, `_doc/`)
  - Run `_scripts/prepare-for-PR.sh` to create clean `pr/bridge-analyzer`
  - Create upstream PR from `pr/bridge-analyzer`
- **Rationale:** Clear separation between our work environment and upstream contributions
- **Impact:** Need prepare-for-PR.sh script, two-branch workflow for contributions

## 2025-11-23: Rename to Underscore Prefixes (_pm, _doc, _scripts)
- **Requested by:** jk
- **Decision:** Rename `scripts/` → `_scripts/`, `doc/managing-dev-assets.md` → `_doc/`, etc.
- **Rationale:** Clear visual distinction - underscore = our stuff, no underscore = project stuff
- **Impact:** All dev/management files easily identifiable, sort to top of file lists

## 2025-11-22: Task Refinement with Prompt Collaboration
- **Requested by:** jk
- **Decision:** Tasks start as `[ ]` (open for refinement) before `[>]` (ready to start)
- **Process:** AI adds prompts and questions, JK reviews/approves, then task marked `[>]`
- **Rationale:** Collaborative refinement leads to better prompts and clearer understanding
- **Impact:** Two-phase task workflow (refine → execute)

## 2025-11-22: Split PM Files by Concern
- **Requested by:** jk
- **Decision:** Use separate files: tasks.md, decisions.md, open-questions.md, notes.md
- **Structure:** `_pm/<project-name>/` with archive subfolder
- **Rationale:** Smaller files are easier to navigate and maintain
- **Impact:** Each file stays focused and accessible

## 2025-11-22: PM Folder Named `_pm/`
- **Requested by:** jk
- **Decision:** Use underscore prefix for project management folders
- **Naming:** `_pm/<branch-name>/` (e.g., `_pm/bridge-analyzer/`)
- **Rationale:** Sorts to top of file list, always visible, clear abbreviation
- **Impact:** Easy to find PM files in any project

## 2025-11-22: Dev Assets in Separate Branch
- **Requested by:** jk
- **Decision:** dev-assets branch for skills, scripts, templates
- **Workflow:** Pull into feature branches, never merge to develop
- **Rationale:** Keep production clean, version control dev helpers
- **Files:** .claude/, _scripts/, doc/templates/, _doc/managing-dev-assets.md

## 2025-11-22: Lightweight Task Management with Markdown
- **Requested by:** jk
- **Decision:** Use markdown task lists for project management
- **Format:** `- [ ] ai:` / `- [ ] jk:` / `- [ ] tbd:` / `- [ ] .:`
- **States:** `[ ]` refine, `[>]` start, `[/]` working, `[?]` blocked, `[x]` done, `[~]` skipped
- **Rationale:** Git-friendly, conversational, captures prompts and context
- **File:** .claude/skills/collaboration.md

## 2025-11-22: Separate Requirements from Implementation
- **Requested by:** jk
- **Decision:** requirements.adoc (WHAT/testable), implementation-notes.adoc (HOW/arch)
- **Format:** BDD Given/When/Then with explicit tests
- **Rationale:** TDD approach, clear acceptance criteria, proper requirements engineering
- **Files:** doc/requirements.adoc, doc/implementation-notes.adoc

## 2025-11-22: Follow EVSE Nature Patterns
- **Requested by:** jk
- **Decision:** Use EVSE as reference for modern OpenEMS patterns
- **Pattern:** Minimalist nature interfaces, Doc.of(OpenemsType.XXX) channels
- **Rationale:** Most recent development, cleanest patterns
- **Reference:** io.openems.edge.evse.api/

## 2025-11-22: Phase 1 Scope - READ_ONLY Mode Only
- **Decision:** No PASSTHROUGH or SIMULATION in Phase 1
- **Capture Mechanism:** Bridge logging (no bridge modifications)
- **Rationale:** Minimize risk, faster delivery, validate approach
- **Future:** Phase 2 may add event listener pattern

## 2025-11-22: Use Java Records for Data Classes
- **Decision:** ModbusPacket as Java record
- **Rationale:** Immutable, less boilerplate, modern Java 21
- **Pattern:** record ModbusPacket(...) with helper methods
