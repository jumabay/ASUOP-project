---
name: oracle-parity
description: Use when validating the new implementation against legacy Oracle-backed behavior.
---
# Oracle Parity
1. Confirm TEST connection and read-only authorization; never use production.
2. Select anonymized representative wagon IDs and a bounded hourly time range.
3. Compare current state, every hourly snapshot, ordering, timestamps and null/edge cases.
4. Run bounded parameterized SELECT queries only; no PL/SQL calls with unknown effects.
5. Record mismatches with query, expected, actual and source evidence; never auto-fix data.
