# Security baseline

Phase 1 has no user authentication yet. Health endpoints expose only connectivity metadata. CORS is restricted to one configured frontend origin. All other API paths are denied by Spring Security. CSRF is currently disabled because only stateless, read-only health routes exist; Phase 2 must revisit this before introducing cookie-based authentication. Do not expose RPC endpoints or development signing keys to the public internet.


## Wallet authentication controls (Phase 3)
SIWE challenges are generated server-side and stored with wallet address, purpose, account binding (for linking), chain ID, issued-at and expiry. The submitted message must match the stored challenge exactly. EIP-191 signature recovery is performed on the backend; the frontend is not trusted to assert ownership. MongoDB conditionally marks the nonce consumed once, preventing replay and concurrent reuse. Wallet addresses have a unique index. Use HTTPS and secure cookies outside local development. EOA signatures are supported; ERC-1271 contract-wallet signatures are not implemented in Phase 3.

## Security operations and administration (Phase 6)

- `AuthenticationRateLimitFilter` applies a configurable fixed-window limit to login, registration, wallet nonce/verification and refresh endpoints. Defaults are 10 requests per minute per remote address and route. It returns HTTP 429 with `Retry-After`. This in-memory limiter is per application instance; deployments with multiple replicas must move rate-limit counters to a shared store (for example Redis) and configure trusted proxy handling before using forwarded client IP headers.
- `AuditCaptureFilter` records mutating API requests and authentication actions without reading request bodies. Passwords, authorization headers, cookies, signatures and request payloads are not stored. The remote address is HMAC-SHA-256 hashed before persistence.
- `AuditService` creates a sequential SHA-256 hash chain. Administrators can inspect `GET /api/admin/audit` and verify it with `GET /api/admin/audit/verify`. This is tamper-evident, not immutable: a database administrator able to rewrite every event and hash can forge the chain. Production should stream events to independently controlled append-only/WORM storage or a SIEM, and use a distributed sequence/append mechanism when running multiple backend instances.
- `PUT /api/admin/users/{userId}/roles` changes MongoDB application roles. The final enabled administrator cannot be demoted or suspended. Ethereum roles do not grant application privileges.
- Security headers deny framing, disable MIME sniffing, restrict referrers and disable camera, microphone and geolocation browser permissions.
- Activate the `prod` Spring profile in production. Startup then rejects a weak/default JWT secret, a non-secure refresh cookie setting, or a non-HTTPS frontend origin. Set `JWT_SECRET` and `AUDIT_HASH_SECRET` to distinct unique random values of at least 32 bytes and `SECURE_COOKIE=true`.
- The current fixed-window rate limiter and audit append serialization are single-instance mechanisms. Do not horizontally scale without replacing them with distributed coordination/storage.

## Phase 6 audit API

- `GET /api/admin/audit?limit=50` — recent security events and a hash-chain validity result.
- `GET /api/admin/audit/verify` — verify the latest 5,000 stored events.
- `PUT /api/admin/users/{userId}/roles` — JSON body `{ "roles": ["USER"] }` or `{ "roles": ["USER", "ADMIN"] }`.
