---
skill: collaboration
description: Lightweight project management and collaboration workflow between developer (jk) and AI assistant (ai)
version: 1.0
---

# Collaboration Workflow

This skill defines how we work together on OpenEMS features using lightweight, conversation-driven project management.

## Core Principles

1. **Transparency** - All work is visible in markdown task lists
2. **Ownership** - Clear assignment (jk/ai/tbd)
3. **Context** - Prompts and decisions captured inline
4. **Flexibility** - Easy to adjust as we learn
5. **Git-friendly** - Everything in version control

## Task Assignment Tags

- `jk` - Task assigned to developer (Jan)
- `ai` - Task assigned to AI assistant
- `tbd` - Not yet assigned
- `.` - Placeholder for future breakdown

## Task States

- `[ ]` - Open for refinement (all sides invited to comment, improve, add prompts)
- `[>]` - Please start (refined and ready to begin)
- `[/]` - In progress (actively working on it)
- `[?]` - Blocked or awaiting decision
- `[x]` - Completed
- `[~]` - Skipped/deferred

### Special: Prompt Refinement

When a task has `- [>] ai: Prompt: ...` in its substructure:
- **Invitation:** AI should improve/refine the prompt before executing
- **Process:** Add questions one level deeper if clarification needed
- **Goal:** Get the best possible prompt through collaborative refinement

## Task Lifecycle & Refinement

### Phase 1: Open for Refinement `[ ]`

Task is created but not finalized. **Everyone invited to:**
- Add comments and suggestions as subtasks
- Propose or refine prompts
- Ask clarifying questions
- Improve task description

**AI's role when seeing `[ ]` tasks:**
- Review task description
- Add detailed prompt as `- [>] ai: Prompt: "..."`
- Add questions about unclear aspects
- Suggest improvements learned from previous tasks
- **Do NOT execute yet** - wait for `[>]` state

**JK's role:**
- Review AI's proposed prompts and questions
- Answer questions
- Approve by changing task to `[>]` state
- Or add more refinements

### Phase 2: Ready to Start `[>]`

Task is refined and approved. AI should begin work.

**AI's role:**
- Change to `[/]` when starting
- Execute using the refined prompt
- Add progress updates as subtasks

### Phase 3: In Progress `[/]`

Work is actively happening.

**AI's role:**
- Keep task marked as `[/]` while working
- Add "Done! <DateTime>" subtask when complete (e.g., "Done! 2025-11-23 14:30")
- Add review request for JK
- **Keep as `[/]` until approved**

### Phase 4: Complete `[x]`

Work is done and approved.

**JK's role:**
- Review the work
- Either approve (mark `[x]`) or request changes (back to `[>]` or `[/]`)

## Project File Structure

Each feature/project has project management files in `_pm/<project-name>/`:

```
_pm/
└── bridge-analyzer/              ← Matches branch name (without feature/ prefix)
    ├── tasks.md                  ← Current Focus + Next Up
    ├── decisions.md              ← Architectural/approach decisions
    ├── open-questions.md         ← Questions, blockers, parking lot
    ├── notes.md                  ← Misc notes, links, context
    └── archive/
        ├── tasks-done.md         ← Completed tasks (by date/milestone)
        └── README.md             ← Index of archived items
```

**Rationale for `_pm/`:**
- Underscore prefix sorts to top of file list (always visible)
- Clear abbreviation for "project management"
- Folder name matches branch name for easy reference
- Split files keep each document small and focused

## File Templates

### tasks.md
```markdown
---
project: Bridge Analyzer
branch: feature/bridge-analyzer
started: 2025-11-22
updated: 2025-11-22
---

# Tasks

## Current Focus

What we're actively working on right now.

- [/] ai: Implement BridgeAnalyzer nature interface
  - [>] ai: Prompt: "Create BridgeAnalyzer.java following EvseChargePoint pattern.
    Include channels: OPERATING_MODE, CAPTURE_ENABLED, PACKETS_CAPTURED,
    LAST_TRANSACTION, BUFFER_OVERRUNS, ANALYZER_NOT_READY.
    Use Doc.of(OpenemsType.XXX) pattern. Keep interface minimal."
  - [x] ai: Done! → bridgeanalyzer.api/src/.../BridgeAnalyzer.java:1
  - [ ] jk: Review interface

## Next Up

Prioritized backlog in order.

- [>] ai: Create OperatingMode enum
  - [>] ai: Prompt: "Create OperatingMode enum implementing OptionsEnum.
    Values: UNDEFINED(-1), READ_ONLY(0), PASSTHROUGH(1), SIMULATION(2).
    Follow pattern from evse.api Mode.java."
  - Ready to start after BridgeAnalyzer review

- [ ] ai: Create package-info.java
  - Need OSGi export annotations for the package
  - [ ] ai: Which package version should we use? 1.0.0?
  - [ ] ai: Any additional annotations needed beyond @Export and @Version?

- [ ] jk: Review API bundle before moving to Modbus bundle
```

### decisions.md
```markdown
---
project: Bridge Analyzer
---

# Decisions

## 2025-11-22: Use TDD with Given/When/Then Requirements
- **Requested by:** jk
- **Decision:** Use BDD-style testable requirements
- **Rationale:** Enables test-driven development, clear acceptance criteria
- **Impact:** All requirements in requirements.adoc must have explicit tests

## 2025-11-22: Separate Requirements from Implementation
- **Requested by:** jk
- **Decision:** requirements.adoc (WHAT), implementation-notes.adoc (HOW)
- **Rationale:** Cleaner separation of concerns, proper requirements engineering
- **Impact:** Keep architectural decisions out of requirements
```

### open-questions.md
```markdown
---
project: Bridge Analyzer
---

# Open Questions & Blockers

## Active Questions

- [?] ai: How to handle bridge logging format changes?
  - Context: Phase 1 uses log parsing which depends on format
  - Discussion: Document dependency, plan Phase 2 event listener migration
  - Next: Add risk to implementation-notes.adoc

## Parking Lot

Ideas to consider later:

- Should we add packet filtering in Phase 1?
  - Pro: Focus on specific function codes
  - Con: Adds complexity
  - Current: Defer to Phase 2

- Should LAST_TRANSACTION be JSON instead of String?
  - Pro: Structured, easier for UI
  - Con: More complex
  - Current: String for Phase 1
```

### notes.md
```markdown
---
project: Bridge Analyzer
---

# Notes & Context

## Useful References

- EVSE nature: `io.openems.edge.evse.api/src/.../EvseChargePoint.java`
- ElectricityMeter nature: `io.openems.edge.meter.api/src/.../ElectricityMeter.java`
- Modbus bridge: `io.openems.edge.bridge.modbus/`

## Tips & Patterns

- Use `Doc.of(OpenemsType.XXX)` for channel definitions
- Follow process image pattern (nextValue/value)
- Keep interfaces minimal (like EVSE, not like ElectricityMeter)

## Random Thoughts

- Consider adding debug logging level option
- UI widget for packet visualization (Phase 2?)
```

## Workflow Examples

### Task Refinement Process

**JK creates initial task (open for refinement):**
```markdown
## Next Up
- [ ] ai: Create BridgeAnalyzer nature interface
  - Should follow EVSE patterns
  - Needs channels for capture control and statistics
```

**AI adds/refines prompt:**
```markdown
- [ ] ai: Create BridgeAnalyzer nature interface
  - [>] ai: Prompt: "Create BridgeAnalyzer.java following EvseChargePoint pattern.
    Include channels: OPERATING_MODE (String), CAPTURE_ENABLED (Boolean),
    PACKETS_CAPTURED (Integer), LAST_TRANSACTION (String), BUFFER_OVERRUNS (Integer),
    ANALYZER_NOT_READY (StateChannel). Use Doc.of(OpenemsType.XXX) pattern.
    Keep interface minimal - just channel definitions and accessors."
    - [ ] ai: Should we add getAnalyzerState() helper method?
    - [ ] ai: LAST_TRANSACTION max length constraint?
```

**JK reviews refinement and approves:**
```markdown
- [>] ai: Create BridgeAnalyzer nature interface
  - [>] ai: Prompt: "Create BridgeAnalyzer.java following EvseChargePoint pattern..."
    - [x] ai: Should we add getAnalyzerState() helper method?
      - jk: No, keep it minimal for Phase 1
    - [x] ai: LAST_TRANSACTION max length constraint?
      - jk: No constraint, formatter handles it
```

**AI executes:**
```markdown
- [/] ai: Create BridgeAnalyzer nature interface
  - [>] ai: Prompt: "Create BridgeAnalyzer.java following EvseChargePoint pattern..."
  - Working on implementation...
```

**AI completes:**
```markdown
- [/] ai: Create BridgeAnalyzer nature interface
  - [>] ai: Prompt: "Create BridgeAnalyzer.java following EvseChargePoint pattern..."
  - [x] ai: Done! 2025-11-22 15:45 → bridgeanalyzer.api/src/.../BridgeAnalyzer.java:1
  - [ ] jk: Review and approve
```

**JK approves:**
```markdown
- [x] ai: Create BridgeAnalyzer nature interface
  - [x] ai: Prompt: "Create BridgeAnalyzer.java following EvseChargePoint pattern..."
  - [x] ai: Done! → bridgeanalyzer.api/src/.../BridgeAnalyzer.java:1
  - [x] jk: Reviewed - approved!
```

### Handling Blockers

**AI encounters question:**
```markdown
- [?] ai: Implement ModbusPacket capture
  - Question: Should we use immutable record or traditional class?
  - jk: Use record for simplicity
  - [x] ai: Updated to use record
```

### Making Decisions

```markdown
## Decisions Made

### 2025-11-22: Use Records for Data Classes
- Question: Record vs traditional class for ModbusPacket?
- Decision: Use Java records
- Rationale: Immutable by default, less boilerplate, modern Java
- Decided by: jk
```

## Task Breakdown Guidelines

### When to Create Subtasks

1. **Complexity** - Task has multiple distinct steps
2. **Collaboration** - Task requires back-and-forth
3. **Blocking** - Part of task can proceed while other part waits
4. **Review** - Implementation needs approval before continuing

### Good Task Granularity

**Too coarse:**
```markdown
- [ ] ai: Implement entire Modbus analyzer
```

**Too fine:**
```markdown
- [ ] ai: Import OpenemsComponent
- [ ] ai: Create interface declaration
- [ ] ai: Add first channel
- [ ] ai: Add second channel
```

**Just right:**
```markdown
- [ ] ai: Create BridgeAnalyzer nature interface
  - Channels: OPERATING_MODE, CAPTURE_ENABLED, PACKETS_CAPTURED, LAST_TRANSACTION
  - Follow EVSE minimalist pattern
  - [ ] ai: Done! 2025-11-22 15:45 → BridgeAnalyzer.java:1
  - [ ] jk: Review
```

## Prompt Capture

When AI starts a task, capture the effective prompt:

```markdown
- [>] ai: Create ModbusPacketFormatter class
  - Prompt: "Create formatter class that converts ModbusPacket to human-readable string.
    Include: function code name, unit ID, register address/count, hex dump of data,
    error codes if present. Follow OpenEMS patterns. Max 80 chars line width."
  - Context: See ModbusPacket.java and requirements FR-2.3
```

This helps:
- Reproduce results if needed
- Understand what was requested
- Improve prompts over time
- Onboard others to the project

## Integration with Git

### Commit Messages Reference Tasks

```bash
git commit -m "Implement BridgeAnalyzer nature interface

See collaboration.md: 'Create BridgeAnalyzer nature interface'
Implements channels for operating mode, capture control, statistics.
Follows EVSE minimalist pattern."
```

### Branch Per Major Task (Optional)

For larger tasks, consider branches:
```markdown
- [ ] ai: Implement entire ModbusAnalyzer bundle
  - Branch: feature/modbus-analyzer-impl
  - [ ] ai: Create nature interface
  - [ ] ai: Create supporting enums
  - [ ] ai: Create data structures
  - [ ] ai: Create main component
```

## Keeping It Lightweight

### Don't Overthink It

- ✓ Update as you work
- ✓ Keep it conversational
- ✓ Move to "Done" when complete
- ✓ Archive old stuff
- ✗ Don't spend more time managing than doing
- ✗ Don't create tasks for every tiny thing
- ✗ Don't leave it stale - sync frequently

### Regular Cleanup

Every few days:
1. Move completed tasks to "Done"
2. Archive old "Done" items (collapse in `<details>`)
3. Re-prioritize "Next Up"
4. Review "Decisions Made" - still relevant?

## Example Real Usage

Here's what our current bridge-analyzer collaboration.md might look like:

```markdown
---
project: Bridge Analyzer
bundles: bridgeanalyzer.api, bridgeanalyzer.modbus
started: 2025-11-22
status: Requirements done, starting implementation
phase: Phase 1 - READ_ONLY mode
---

# Bridge Analyzer - Collaboration

## Current Focus

- [>] jk: Review requirements.adoc
  - [x] ai: Rewrote with BDD Given/When/Then format
  - [x] jk: Approved - good separation of concerns

## Next Up

### API Bundle (bridgeanalyzer.api)

- [ ] ai: Create BridgeAnalyzer nature interface
  - Channels: OPERATING_MODE, CAPTURE_ENABLED, PACKETS_CAPTURED, LAST_TRANSACTION, BUFFER_OVERRUNS, ANALYZER_NOT_READY
  - Follow EVSE minimalist pattern
  - See: EvseChargePoint.java for reference

- [ ] ai: Create OperatingMode enum
  - Values: UNDEFINED, READ_ONLY, PASSTHROUGH, SIMULATION
  - Implements OptionsEnum

- [ ] ai: Create package-info.java
  - OSGi export annotations
  - Version 1.0.0

- [ ] jk: Review API bundle before proceeding to Modbus

### Modbus Bundle (bridgeanalyzer.modbus)

- [ ] ai: Create ModbusAnalyzer nature interface
  - Extends BridgeAnalyzer
  - Add Modbus-specific channels

- [ ] ai: Create supporting enums
  - ModbusFunctionCode
  - ModbusTransactionType
  - ModbusExceptionCode

- [ ] ai: Create ModbusPacket data class
  - Use Java record
  - Immutable packet representation

- [ ] ai: Create PacketBuffer circular buffer
  - Thread-safe
  - Configurable size
  - Eviction tracking

- [ ] ai: Create ModbusPacketFormatter
  - Human-readable formatting
  - Hex dump support

- [ ] ai: Create Config interface
  - @ObjectClassDefinition
  - Required and optional params

- [ ] ai: Create ModbusAnalyzerImpl component
  - Main OSGi component
  - Implements both natures
  - Lifecycle management

- [ ] ai: Create package-info.java

- [ ] jk: Review Modbus bundle

### Testing & Integration

- [ ] .: Write unit tests
- [ ] .: Integration test with dummy bridge
- [ ] .: Manual test with real device
- [ ] jk: Final review and merge

## Blocked / Questions

None currently.

## Decisions Made

### 2025-11-22: Proper Requirements Engineering
- Requested by: jk
- Change: Separate requirements from implementation, use BDD format
- Files: requirements.adoc (WHAT), implementation-notes.adoc (HOW)
- Rationale: TDD approach, testable requirements

### 2025-11-22: Use EVSE Patterns
- Requested by: jk
- Change: Follow modern EVSE nature patterns instead of ElectricityMeter
- Rationale: Most recent OpenEMS development practices

### 2025-11-22: Dev Assets Management
- Requested by: jk
- Change: Created dev-assets branch with skills and scripts
- Rationale: Keep dev helpers separate from production code

## Done

- [x] ai: Create managing-dev-assets.md documentation
- [x] ai: Set up dev-assets branch infrastructure
- [x] ai: Create requirements.adoc for bridgeanalyzer.modbus
- [x] jk: Review and approve requirements approach

## Notes

- Remember: All requirements must be testable
- Keep implementation details in implementation-notes.adoc
- Consult edge-component.md skill for OpenEMS patterns
```

## Best Practices

1. **Start of work session** - Review "Next Up", pick highest priority
2. **During work** - Mark task `[>]`, update with progress
3. **On completion** - Mark `[x]`, add "Done!" subtask
4. **On questions** - Mark `[?]`, add question as subtask
5. **End of session** - Move completed to "Done", update "Next Up"
6. **Regular reviews** - Archive old items, reassess priorities

## Tools Integration

### Future Enhancements

Could integrate with:
- GitHub Issues (bi-directional sync)
- Task automation scripts
- Progress visualization
- Time tracking (optional)

But start simple - markdown is enough!

## References

- [edge-component.md](./edge-component.md) - OpenEMS component patterns
- [managing-dev-assets.md](../../_doc/managing-dev-assets.md) - Dev workflow
- [GitHub Markdown Task Lists](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/about-task-lists)

---

**Skill Status:** Active
**Last Updated:** 2025-11-22
**Maintained By:** jk + ai
