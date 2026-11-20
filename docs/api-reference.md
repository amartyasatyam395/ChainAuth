# API Reference — Phases 1 and 2

All JSON APIs are served from the Spring Boot backend on port 8080 by default. State-changing requests require the CSRF header returned by `GET /api/auth/csrf`. Authenticated requests use `Authorization: Bearer <accessToken>`. The refresh token is cookie-only.

## Registration
`POST /api/auth/register` body: `{ "name": "Example User", "email": "user@example.com", "password": "at-least-12-chars" }`. Returns 201 with a safe user view.

## Login
`POST /api/auth/login` body: `{ "email": "user@example.com", "password": "..." }`. Returns an access token and user view; sets the refresh cookie.

## Refresh and logout
`POST /api/auth/refresh` rotates the HttpOnly refresh cookie and returns a new access token. `POST /api/auth/logout` revokes the supplied refresh token and clears the cookie.

## User endpoints
`GET /api/auth/me`, `GET /api/users/me`, `PUT /api/users/me` with `{ "name": "New Name" }`, `POST /api/users/me/change-password` with `{ "currentPassword": "...", "newPassword": "..." }`, and `GET /api/users/me/security`.

## Admin probe
`GET /api/admin/status` requires `ROLE_ADMIN`; ordinary USER accounts receive 403.


## Ethereum wallet authentication (Phase 3)

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/wallet/nonce` | Public for LOGIN; authenticated for LINK | Create a one-time SIWE challenge |
| POST | `/api/auth/wallet/verify` | Public | Verify linked wallet EIP-191 signature and issue shared session |
| POST | `/api/auth/wallet/link/verify` | Authenticated | Verify ownership and link wallet to current account |
| GET | `/api/users/me/wallets` | Authenticated | List current user's linked wallets |
| DELETE | `/api/users/me/wallets/{walletId}` | Authenticated | Unlink a wallet owned by current user |

Nonce request JSON: `{"address":"0x...","chainId":31337,"purpose":"LOGIN"}`. `purpose` may be `LOGIN` or `LINK`. Verification JSON: `{"address":"0x...","message":"<exact server-issued SIWE message>","signature":"0x..."}`. All state-changing requests require the existing CSRF token.


## Identity registry administration (Phase 4)

All endpoints below require an authenticated ChainAuth user with the `ADMIN` application role. The backend additionally verifies that a wallet is linked to a ChainAuth account before attempting identity registration or mutation.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/admin/identities/network` | Report RPC chain, configured chain, contract and signer readiness |
| GET | `/api/admin/identities/{address}` | Read on-chain identity state |
| POST | `/api/admin/identities/register` | Register a linked wallet `{ "address": "0x...", "role": "USER" }` |
| POST | `/api/admin/identities/{address}/role` | Assign `{ "role": "MODERATOR" }` |
| POST | `/api/admin/identities/{address}/deactivate` | Deactivate identity |
| POST | `/api/admin/identities/{address}/reactivate` | Reactivate identity |
| GET | `/api/admin/identities/events?fromBlock=0` | Read contract logs from a block onward |

The backend signer must be granted `IDENTITY_MANAGER_ROLE` on the deployed contract. It is configured only through `BLOCKCHAIN_SIGNER_PRIVATE_KEY`; never commit the key. The application ADMIN role gates these API operations, while the contract enforces manager access on-chain.


Phase 4 identity-role mutations update the Ethereum registry only. They do not automatically mutate the MongoDB `User.roles` collection or grant application API privileges. That synchronization is intentionally deferred to Phase 5.

## Hybrid synchronization (Phase 5)

All endpoints below require an authenticated ChainAuth ADMIN application role.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/admin/identity-sync/registrations` | Queue linked wallet registration; accepts optional `Idempotency-Key` header and the same `{ "address": "0x...", "role": "USER" }` JSON body |
| POST | `/api/admin/identity-sync/wallets/{address}/role` | Queue on-chain role assignment with `{ "role": "MODERATOR" }` |
| POST | `/api/admin/identity-sync/wallets/{address}/deactivate` or `/reactivate` | Queue on-chain identity status change |
| GET | `/api/admin/identity-sync` | List latest 100 synchronization jobs |
| GET | `/api/admin/identity-sync/{id}` | Read durable job state and transaction/confirmation metadata |
| POST | `/api/admin/identity-sync/{id}/retry` | Retry only FAILED or REORGED jobs |
| GET | `/api/admin/identity-sync/reconcile` | Compare up to 500 MongoDB-linked wallets against on-chain registration/active status |

The enqueue endpoint returns HTTP 202. Job states are QUEUED, SUBMITTED, CONFIRMED, FAILED and REORGED. Only CONFIRMED means the configured confirmation threshold was reached and the contract state matched the requested registration. No blockchain state grants Spring Security roles; MongoDB remains authoritative. See `docs/hybrid-synchronization.md` for retry and reorg behavior.


## Account suspension (Phase 5)

All routes require the ChainAuth ADMIN application role.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/admin/users` | List users without password hashes |
| POST | `/api/admin/users/{userId}/suspend` | Disable account, increment JWT token version, revoke refresh sessions and queue on-chain deactivation for active linked identities |
| POST | `/api/admin/users/{userId}/reactivate` | Re-enable account without restoring old sessions or automatically reactivating wallets |

Suspended accounts are rejected by the JWT filter. Reactivated users must sign in again; linked wallet identities remain inactive until explicitly reactivated. See `docs/hybrid-synchronization.md`.

## Phase 6 — Security administration

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| PUT | `/api/admin/users/{userId}/roles` | ADMIN | Replace MongoDB application roles; cannot demote the final enabled administrator |
| GET | `/api/admin/audit?limit=50` | ADMIN | List recent audit events and current hash-chain integrity result |
| GET | `/api/admin/audit/verify` | ADMIN | Verify the latest 5,000 audit events |

Authentication endpoints are protected by a configurable in-memory fixed-window rate limiter. A rejected request receives HTTP 429 and a `Retry-After` header.
