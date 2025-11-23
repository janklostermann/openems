---
project: Bridge Analyzer
branch: feature/bridge-analyzer
started: 2025-11-22
updated: 2025-11-22
---

# Tasks

## Current Focus

Currently working on API Bundle implementation.

### Testing & Documentation

- [>] ai : set up Integration test to check all external requirements and run them (for now (no implementation yet) the expected result is: failed)
- [>] ai : Write unit tests and run them for all functionality you are going to implement 
- [>] at : Update documentation

### API Bundle (bridgeanalyzer.api)

- [ ] ai: Create BridgeAnalyzer nature interface
  - Should follow EVSE patterns (minimalist, clean)
  - Channels: OPERATING_MODE, CAPTURE_ENABLED, PACKETS_CAPTURED, LAST_TRANSACTION, BUFFER_OVERRUNS, ANALYZER_NOT_READY
  - [ ] ai: Should we add helper methods like getAnalyzerState()?
    - [>] sounds reasonable, but please have a look on what they are useful for in other APIs
  - [ ] ai: Any channel constraints (max length, range)?
    - [>]not that I know of, have a look into other APIs to understand better

- [ ] ai: Create OperatingMode enum
  - Values: UNDEFINED(-1), READ_ONLY(0), PASSTHROUGH(1), SIMULATION(2)
  - Implements OptionsEnum
  - [ ] ai: Should we add method to check if mode supports modification?
      - [>] @ai: What would that be useful for?

- [ ] ai: Create package-info.java for API bundle
  - [ ] ai: Package version 1.0.0 correct for initial release?
    - make it 0.1.0
  - [ ] ai: Just @Version and @Export, or other annotations needed?
    - [>] please have a look at the other to understand what's needed, and maybe into the documentation or comments for what they are helpful in the first place

- [ ] jk: Review API bundle before proceeding to Modbus

### Modbus Bundle (bridgeanalyzer.modbus)

- [ ] ai: Create ModbusAnalyzer nature interface
  - Extends BridgeAnalyzer
  - Add Modbus-specific channels
  - [ ] ai: Should error counters be cumulative or resettable?
    - [>] ai: when would you reset them?

- [ ] ai: Create supporting enums
  - ModbusFunctionCode (0x01-0x17)
  - ModbusTransactionType (READ, WRITE)
  - ModbusExceptionCode (0x01-0x0B)
  - [ ] ai: Include diagnostic/vendor-specific codes or just standard?
    - diagnostic/ vendor-specific stuff should be included, but clearly separated and marked as such, maybe even bundled in separate files collecting the specific stuff of the respective vendors/models

- [ ] ai: Create ModbusPacket data class
  - Use Java record
  - [ ] ai: Which fields are mandatory vs optional?
    - [>] Could you please try and find that out by investigating different modbus specification from various modbus devices?
  - [ ] ai: Should we split into separate Request/Response records?
    - [>] what would that be helpful for?

- [ ] ai: Create PacketBuffer circular buffer
  - Thread-safe implementation
  - [ ] ai: Use synchronized methods or ReentrantLock?
    - [>] what would you recommend and why?
  - [ ] ai: Should buffer support iteration while adding?
    - [>] what is this about? If you have a clear opinion just go for it, if in doubt let's revisit

- [ ] ai: Create ModbusPacketFormatter
  - Human-readable formatting
  - [ ] ai: Configurable format (compact vs verbose)?
    - [>] what would be the difference?
  - [ ] ai: Support different output targets (UI vs log)?

- [ ] ai: Create Config interface
  - @ObjectClassDefinition
  - [ ] ai: Should buffer size have min/max validation annotations?
    - [>] do the most reasonable

- [ ] ai: Create ModbusAnalyzerImpl component
  - Main OSGi component
  - [ ] ai: How to integrate with bridge - logging or other mechanism?
    - [>] what is this about?

- [>] ai: Create package-info.java for Modbus bundle

- [ ] jk: Review Modbus bundle implementation



## Next Up

- [ ] jk: Final review and merge

## Waiting / Blocked

### Infrastructure & Git Workflow

- [ ] ai: Create _scripts/prepare-for-PR.sh
  - **Blocked on:** Design decisions (can implement when needed for first PR)
  - [>] ai: Prompt: "Create script that prepares a clean pr/* branch from current feature/* branch for upstream PRs.
    Workflow:
    1. Verify current branch is feature/*
    2. Determine target pr/* branch name (e.g., feature/bridge-analyzer → pr/bridge-analyzer)
    3. Create or reset pr/* branch from current feature/* branch
    4. Remove _pm/, _doc/, _scripts/, .claude/ from git tracking in pr/* branch
    5. Add .gitignore entries for dev assets in pr/* branch
    6. Result: Clean pr/* branch ready for upstream PR (without dev assets)"
  - [ ] ai: Should we use git-filter-branch, or simpler: checkout feature → remove files → commit?
  - [ ] ai: Incremental updates or always rebuild pr/* branch from scratch?
  - [ ] ai: Delete pr/* branches after upstream merge, or keep for reference?
  - [x] jk: Confirmed lowercase pr/* naming convention
  - **Workaround:** Manual process documented in _doc/managing-dev-assets.md
  - **Impact if skipped:** Medium - manual process works, but error-prone

## Done

### Infrastructure & Git Workflow

- [x] jk: Review requirements engineering approach
  - Done! 2025-11-22
  - Requested TDD with BDD-style Given/When/Then format
  - Approved separation of requirements vs implementation

- [x] jk: Review collaboration.md skill and PM structure
  - Done! 2025-11-22
  - Approved split file approach
  - Confirmed `_pm/` folder naming
  - Refined task state meanings

- [~] ai: Create prepare-merge-to-develop.sh script
  - Done! 2025-11-23 (Skipped with explanation)
  - Skipped: Workflow clarification - develop is just upstream inbox, we don't merge to it
  - [x] ai: Archived script to _scripts/archive/ with explanation

- [x] ai: Update existing helper scripts for renamed paths
  - Done! 2025-11-23
  - [x] ai: Updated _scripts/setup-feature-branch.sh (scripts→_scripts, doc→_doc, added _pm)
  - [x] ai: Updated _scripts/sync-dev-assets.sh (scripts→_scripts, doc→_doc)
  - Note: update-dev-asset.sh works with any path, no changes needed

- [x] ai: Update _doc/managing-dev-assets.md
  - Done! 2025-11-23 → _doc/managing-dev-assets.md
  - [x] ai: Removed obsolete "Merging to Develop" workflow
  - [x] ai: Added corrected workflow: develop = upstream inbox only
  - [x] ai: Documented prepare-for-PR.sh workflow (feature/* → pr/* → upstream)
  - [x] ai: Updated all path references (scripts→_scripts, doc→_doc)
  - [x] ai: Added git workflow diagram showing correct branch relationships
  - [x] ai: Added common scenarios and troubleshooting sections
  - [x] ai: Separated helper scripts section → _scripts/README.md

- [x] ai: Update .claude/skills/collaboration.md
  - Done! 2025-11-23 → .claude/skills/collaboration.md
  - [x] ai: Added DateTime requirement for "Done!" markers (e.g., "Done! 2025-11-23 14:30")
  - [x] ai: Updated all examples to show DateTime format
