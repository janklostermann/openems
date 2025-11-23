---
project: Bridge Analyzer
branch: feature/bridge-analyzer
started: 2025-11-22
updated: 2025-11-22
---

# Tasks

## Current Focus

- [x] jk: Review requirements engineering approach
  - Requested TDD with BDD-style Given/When/Then format
  - Approved separation of requirements vs implementation

- [x] jk: Review collaboration.md skill and PM structure
  - Approved split file approach
  - Confirmed `_pm/` folder naming
  - Refined task state meanings

## Next Up

### API Bundle (bridgeanalyzer.api)

- [ ] ai: Create BridgeAnalyzer nature interface
  - Should follow EVSE patterns (minimalist, clean)
  - Channels: OPERATING_MODE, CAPTURE_ENABLED, PACKETS_CAPTURED, LAST_TRANSACTION, BUFFER_OVERRUNS, ANALYZER_NOT_READY
  - [ ] ai: Should we add helper methods like getAnalyzerState()?
  - [ ] ai: Any channel constraints (max length, range)?

- [ ] ai: Create OperatingMode enum
  - Values: UNDEFINED(-1), READ_ONLY(0), PASSTHROUGH(1), SIMULATION(2)
  - Implements OptionsEnum
  - [ ] ai: Should we add method to check if mode supports modification?

- [ ] ai: Create package-info.java for API bundle
  - [ ] ai: Package version 1.0.0 correct for initial release?
  - [ ] ai: Just @Version and @Export, or other annotations needed?

- [ ] jk: Review API bundle before proceeding to Modbus

### Modbus Bundle (bridgeanalyzer.modbus)

- [ ] ai: Create ModbusAnalyzer nature interface
  - Extends BridgeAnalyzer
  - Add Modbus-specific channels
  - [ ] ai: Should error counters be cumulative or resettable?

- [ ] ai: Create supporting enums
  - ModbusFunctionCode (0x01-0x17)
  - ModbusTransactionType (READ, WRITE)
  - ModbusExceptionCode (0x01-0x0B)
  - [ ] ai: Include diagnostic/vendor-specific codes or just standard?

- [ ] ai: Create ModbusPacket data class
  - Use Java record
  - [ ] ai: Which fields are mandatory vs optional?
  - [ ] ai: Should we split into separate Request/Response records?

- [ ] ai: Create PacketBuffer circular buffer
  - Thread-safe implementation
  - [ ] ai: Use synchronized methods or ReentrantLock?
  - [ ] ai: Should buffer support iteration while adding?

- [ ] ai: Create ModbusPacketFormatter
  - Human-readable formatting
  - [ ] ai: Configurable format (compact vs verbose)?
  - [ ] ai: Support different output targets (UI vs log)?

- [ ] ai: Create Config interface
  - @ObjectClassDefinition
  - [ ] ai: Should buffer size have min/max validation annotations?

- [ ] ai: Create ModbusAnalyzerImpl component
  - Main OSGi component
  - [ ] ai: How to integrate with bridge - logging or other mechanism?

- [ ] ai: Create package-info.java for Modbus bundle

- [ ] jk: Review Modbus bundle implementation

### Testing & Documentation

- [ ] .: Write unit tests
- [ ] .: Integration test with dummy bridge
- [ ] .: Update documentation
- [ ] jk: Final review and merge

## Waiting / Blocked

None currently.
