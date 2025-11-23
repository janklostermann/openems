---
project: Bridge Analyzer
---

# Decisions

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
- **Files:** .claude/, scripts/, doc/templates/, doc/managing-dev-assets.md

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
