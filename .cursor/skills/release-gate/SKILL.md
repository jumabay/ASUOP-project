---
name: release-gate
description: Use before proposing release, deployment, schema changes or cutover.
---
# Release Gate
1. Require explicit release authorization and target environment.
2. Verify builds, tests, dependency/security checks, log redaction and DB permission model.
3. Validate backup/restore, versioned artifacts, rollback process and health checks.
4. Confirm no duplicate importers and no simultaneous legacy/new writers without designed coordination.
5. Present GO/NO-GO checklist, unresolved risks and required approvals.
6. Do not deploy or execute cutover automatically.
