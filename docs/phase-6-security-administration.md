# Phase 6 — Security, Auditing and Administration

## Delivered

1. **Tamper-evident audit trail**
   - MongoDB collection `security_audit_events`.
   - Sequential SHA-256 hash links with a genesis marker.
   - Captures mutating HTTP requests and auth operations, response status, actor, route, timestamp and a HMAC-SHA-256 hash of the source address.
   - Never captures request bodies, credentials, bearer tokens, cookies or signatures.
   - Admin listing and integrity verification endpoints.

2. **Administrator controls**
   - Admin UI at `/admin`.
   - User listing, role changes, account suspension and reactivation.
   - Backend rejects demotion or suspension of the last enabled administrator.
   - MongoDB remains the application authorization authority.

3. **Authentication abuse controls**
   - Fixed-window limits for login, registration, wallet challenge/verification and refresh endpoints.
   - Configurable request count and time window.
   - HTTP 429 with `Retry-After`.

4. **Security hardening**
   - Frame denial, MIME sniffing protection, referrer policy and restrictive permissions policy.
   - Production startup guard for distinct JWT/audit HMAC secrets, HTTPS frontend origin and secure refresh cookies.

## Known limitations

- Rate-limit state is in process memory and keyed by remote address + route. It is not shared between replicas. Do not horizontally scale until replaced with a shared limiter and trusted proxy strategy.
- Audit append ordering is synchronized within one JVM only. Use distributed append coordination if running multiple instances.
- The SHA-256 chain is tamper-evident, not immutable or non-repudiable. A privileged database operator can rewrite all records. Forward events to independently controlled WORM storage/SIEM for production assurance.
- The audit HMAC key must be kept separate from the JWT signing key. The audit UI shows a bounded recent event list; the backend verifies up to the latest 5,000 records.
- No production deployment, penetration test, external SIEM integration or runtime MongoDB test was performed in this phase.
