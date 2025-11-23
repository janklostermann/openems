---
project: Bridge Analyzer
---

# Notes & Context

## Project Overview

**Goal:** Create protocol bridge analyzer infrastructure for OpenEMS Edge, starting with Modbus support.

**Bundles:**
- `io.openems.edge.bridgeanalyzer.api` - Protocol-agnostic nature definitions
- `io.openems.edge.bridgeanalyzer.modbus` - Modbus-specific implementation

**Phase 1 Scope:** READ_ONLY mode, passive observation, no bridge modifications

## Useful References

### OpenEMS Patterns
- **EVSE nature:** `io.openems.edge.evse.api/src/.../EvseChargePoint.java` - Modern minimalist pattern
- **ElectricityMeter nature:** `io.openems.edge.meter.api/src/.../ElectricityMeter.java` - Full-featured example
- **Modbus bridge:** `io.openems.edge.bridge.modbus/` - Target for integration
- **Edge component skill:** `.claude/skills/edge-component.md` - Component development guide

### Documentation
- **Requirements:** `io.openems.edge.bridgeanalyzer.modbus/doc/requirements.adoc` - BDD-style testable requirements
- **Implementation notes:** `io.openems.edge.bridgeanalyzer.modbus/doc/implementation-notes.adoc` - Technical decisions
- **Integration guide:** `io.openems.edge.bridgeanalyzer.modbus/doc/integration-guide.adoc` - Usage examples
- **Collaboration guide:** `doc/managing-dev-assets.md` - Our dev workflow

## Key Patterns to Follow

### Channel Definitions
```java
// Use Doc.of(OpenemsType.XXX) pattern (EVSE style)
OPERATING_MODE(Doc.of(OpenemsType.STRING))

// Not the verbose pattern (ElectricityMeter style)
OPERATING_MODE(new StringDoc().text("Operating mode"))
```

### Nature Interfaces
- Keep minimal (like EVSE, not like ElectricityMeter)
- Just channel definitions and accessor methods
- No complex helper methods in Phase 1

### Process Image Pattern
- Use `_setChannelName()` for internal updates
- Respect nextValue/value semantics
- Update channels only during cycle events

### OSGi Component Lifecycle
- `@Activate` - Initialize, validate config
- `@Modified` - Handle runtime config changes
- `@Deactivate` - Clean up resources

## Tips & Reminders

- All requirements must be testable (BDD Given/When/Then format)
- Keep implementation details in implementation-notes.adoc, not requirements.adoc
- Use `[ ]` tasks for refinement before `[>]` for execution
- Capture prompts in task structure for reproducibility
- Java 21 features available (records, pattern matching, etc.)
- Follow OpenEMS coding guidelines from CLAUDE.md

## Random Thoughts / Future Ideas

- Debug logging level option for verbose output
- UI widget for packet visualization (Phase 2?)
- Packet replay capability for testing
- Statistical analysis of traffic patterns
- Integration with OpenEMS alerting system
- Support for other protocols (HTTP, M-Bus, OneWire)

## Session Notes

### 2025-11-22 PM
- Established dev-assets branch workflow
- Created collaboration skill and PM structure
- Refined task states and prompt collaboration process
- Created split PM files (tasks, decisions, questions, notes)
- Ready to begin API bundle implementation
