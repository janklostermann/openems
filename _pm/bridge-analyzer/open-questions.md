---
project: Bridge Analyzer
---

# Open Questions & Blockers

## Active Questions

None currently - all tasks in refinement phase.

## Parking Lot

Ideas to consider later (from requirements.adoc):

### Packet Filtering in Phase 1?
- **Pro:** Users can focus on specific function codes
- **Con:** Adds complexity
- **Current Decision:** Defer to Phase 2, evaluate after user feedback
- **Revisit:** After Phase 1 deployment

### LAST_TRANSACTION as JSON vs String?
- **Pro (JSON):** Structured data, easier for UI parsing
- **Con (JSON):** More complex, may not align with other natures
- **Pro (String):** Simple, human-readable
- **Current Decision:** String for Phase 1, evaluate for Phase 2
- **Revisit:** If UI needs structured access

### Packet Export to File?
- **Pro:** Useful for offline analysis
- **Con:** Security concern (sensitive data), file management complexity
- **Current Decision:** Out of scope for Phase 1
- **Revisit:** If users request it

### High Traffic Bridge Handling?
- **Options:** Sampling (every Nth packet), dynamic buffer sizing
- **Current Decision:** Monitor in Phase 1, add if needed
- **Revisit:** After performance testing with real deployments

### Bridge Integration Mechanism?
- **Phase 1:** Log parsing (no bridge changes)
- **Phase 2:** Event listener interface (requires bridge PR)
- **Question:** Will log format remain stable enough?
- **Mitigation:** Document dependency, plan migration path
