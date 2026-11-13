---
name: integration-1c
description: Use when designing or implementing the future 1C integration.
---
# Integration 1C
1. Confirm actual 1C configuration, supported exchange interfaces and ownership of financial truth.
2. Define document IDs, schemas, status mapping, versioning and authentication.
3. Use an anti-corruption adapter with idempotency keys, outbox/inbox or equivalent, retry/backoff and reconciliation.
4. Mock 1C for initial tests; require explicit approval before live endpoints.
5. Document failure recovery and audit trails; never infer financial/accounting rules.
