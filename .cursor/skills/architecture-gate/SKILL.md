---
name: architecture-gate
description: Use when planning architecture, new module boundaries, ADRs, or reviewing a proposed stack change.
---
# Architecture Gate
1. Read approved architecture, previous analysis and current constraints.
2. State the business capability, in/out of scope, dependencies and key risks.
3. Compare at most 2–3 realistic alternatives, including operational cost for one developer.
4. Confirm compatibility (Java, Oracle JDBC, Spring, frontend, 1C adapter); unknowns become checks, not guesses.
5. Output proposed diagram, contracts, ADR draft, tests and reversible rollout plan.
6. No implementation until explicit approval.
