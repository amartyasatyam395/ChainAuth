# ChainAuth Project Status

Last updated: 2026-11-20

## Delivery status

| Phase | Status | Notes |
|---|---|---|
| 1 — Foundation | Source checkpoint | Spring Boot, React/Vite, MongoDB and Ethereum health checks, Hardhat config |
| 2 — Traditional authentication | Source checkpoint | Registration/login, BCrypt, JWT, hashed rotating refresh tokens, CSRF, profiles and roles |
| 3 — Ethereum wallet authentication | Source checkpoint | MetaMask/ethers.js, SIWE challenge, EIP-191 EOA recovery, MongoDB one-time nonce, wallet linking and shared sessions |
| 4 — Smart contract identity registry | Source implementation checkpoint | Solidity AccessControl registry, deploy script, Hardhat tests, Web3j reads/writes, gas estimation, receipts, event retrieval, admin API |
| 5 — Hybrid synchronization | Source implementation checkpoint | Idempotent MongoDB sync jobs, asynchronous tx submission/confirmation, retries, bounded event indexer, reorg rewind, reconciliation API |
| 6 — Security and administration | Source implementation checkpoint | Hash-linked audit trail, admin UI/role management, auth rate limiting, security headers and production guard |
| 7 — Final integration | Source checkpoint | CI workflow, static/full verification runners, cross-phase acceptance matrix and release/operations runbook; runtime release gate remains open |

## Phase 5 implementation details

- `identity_sync_jobs` persists idempotency key, wallet/user association, operation, desired role, tx hash, status, attempts, confirmation count, submitted block/hash, error and retry timestamps.
- Admin API enqueues linked-wallet registration and returns HTTP 202; repeated `Idempotency-Key` values resolve to the same job.
- Scheduled worker submits transactions without holding the HTTP request open, polls receipts, checks canonical block hashes, waits for configured confirmations and verifies contract state before marking CONFIRMED.
- Retry uses bounded exponential backoff. FAILED and REORGED jobs can be explicitly retried by an administrator.
- `IdentityRegistryEventIndexer` stores raw contract logs idempotently and persists a block cursor. It rewinds up to 12 blocks if the cursor's canonical block hash changes.
- Reconciliation endpoint compares up to 500 linked MongoDB wallets with on-chain registration and active status.
- Admin account suspension disables the account, increments JWT token version, revokes refresh sessions and queues on-chain deactivation for active linked wallets; reactivation does not restore old sessions or automatically reactivate wallets.
- MongoDB remains the authoritative source for Spring Security roles. On-chain roles never independently grant application privileges.
- Documentation: `docs/hybrid-synchronization.md`.

## Important trust boundaries and limitations

- MongoDB and Ethereum are not a distributed transaction; durable sync jobs are the recovery boundary.
- The event indexer uses a bounded 12-block rewind and does not implement multi-instance leader election. Run one indexer instance per network/contract.
- Event payloads are retained as raw logs; typed event decoding and application-role promotion are intentionally not implemented.
- ERC-1271 contract-wallet signature verification remains unimplemented from Phase 3.
- The backend signer is a high-value secret; production should use a KMS/HSM.
- No mainnet deployment is performed.

## Phase 6 implementation details

- `security_audit_events` stores sequential SHA-256 hash-linked audit events for mutating requests and authentication operations. Request bodies, passwords, bearer tokens, cookies and signatures are never persisted; source IP is HMAC-SHA-256 hashed.
- `GET /api/admin/audit` lists recent events and validates the latest chain; `GET /api/admin/audit/verify` verifies up to the latest 5,000 events.
- Admin UI is available at `/admin`; admins can list accounts, suspend/reactivate accounts, and change MongoDB application roles.
- `PUT /api/admin/users/{userId}/roles` prevents demoting or suspending the last enabled administrator.
- Fixed-window in-memory rate limiting protects login, registration, wallet challenge/verification and refresh endpoints; rejected requests receive 429 and `Retry-After`.
- Security headers deny framing, disable MIME sniffing, restrict referrer leakage and disable camera/microphone/geolocation permissions.
- Activating Spring profile `prod` requires distinct unique JWT and audit HMAC secrets of at least 32 bytes, `SECURE_COOKIE=true`, and an HTTPS frontend origin.
- Known limitations: the limiter and serialized audit append are single-instance only; the hash chain is tamper-evident rather than immutable. Production should use shared rate-limit storage, distributed append coordination and independently controlled WORM/SIEM audit retention.
- Documentation: `docs/phase-6-security-administration.md` and `docs/security-guide.md`.

## Verification

- POM XML parse: PASSED.
- package.json manifests parse: PASSED.
- application.yml parse: PASSED.
- Java source delimiter/structure check: PASSED across 75 source files (not a Java compilation).
- JavaScript `.js` syntax check: PASSED via TypeScript parser/transpiler.
- JavaScript/JSX parser check using the installed TypeScript parser: PASSED across 9 frontend files.
- Maven compile/tests: NOT RUN (Maven is not installed in this execution environment); Phase 5 unit tests are authored but unexecuted.
- Hardhat compile/tests: NOT RUN (dependencies/compiler are not installed in this execution environment).
- MongoDB/Hardhat node/MetaMask end-to-end integration: NOT RUN.
- Frontend production build: NOT RUN.
- Phase 5 and Phase 6 JUnit tests are authored but not executed. Do not treat source-created tests as passing until run.


## Phase 7 implementation details

- Added `scripts/verify-static.py` and `scripts/verify-project.sh --static` for repeatable manifest/YAML parsing, required-file checks, Java/Solidity delimiter checks and frontend JS/JSX syntax parsing when the global TypeScript parser is available.
- Added `scripts/verify-project.sh --full` to check prerequisites and run Maven verification, frontend production build, Hardhat compile and contract tests.
- Added `.github/workflows/ci.yml` with separate backend, frontend, smart-contract and static-source jobs. The workflow is authored but has not been executed in this environment.
- Added `deployment/ACCEPTANCE-TESTS.md` for cross-phase auth, wallet, blockchain sync, admin/security and recovery scenarios.
- Added `deployment/RELEASE-RUNBOOK.md` for secret management, release sequence, incident response and known deployment blockers.
- Updated test strategy, README, changelog and version to 0.7.0.

## Phase 7 verification performed in this environment

- `python3 scripts/verify-static.py`: PASSED — 5 check groups; parsed Maven POM, npm manifests, application YAML, required files, 75 Java files, Solidity contract and 9 frontend JS/JSX files.
- `./scripts/verify-project.sh --full`: NOT COMPLETED — Maven is not installed; frontend and blockchain dependency installation also timed out/unavailable.
- Maven compilation/JUnit, Vite production build, Hardhat compile/tests, live MongoDB + Ethereum node + MetaMask flows, CI workflow execution and deployment tests: NOT RUN.
- Final archive integrity and required-file checks: see Phase 7 ZIP checkpoint.

## Final release blockers and known limitations

- Runtime build/test and full E2E acceptance must pass in staging before release.
- The rate limiter and serialized audit append are single-instance only; the event indexer assumes one active instance per network/contract.
- Audit chain is tamper-evident, not immutable; independently controlled WORM/SIEM retention is required for stronger guarantees.
- ERC-1271 contract-wallet verification is not implemented.
- Backend signing key should be managed by KMS/HSM or an isolated signer service.
- No public/mainnet deployment or security certification is included.
