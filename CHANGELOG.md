## 0.7.0 — Phase 7 — 2026-11-20
- Added a dependency-light static verification runner and a full build/test runner with explicit prerequisite checks.
- Added GitHub Actions CI jobs for backend Maven verification, frontend production build, Solidity compilation/tests and static source checks.
- Added a phase-wide integration acceptance matrix covering traditional auth, wallet auth, hybrid chain sync, security/admin controls and recovery.
- Added production release/operations runbook with environment separation, signer incident response and documented release blockers.
- Runtime builds, CI workflow execution and live MongoDB/Ethereum/MetaMask acceptance remain unverified in this environment.

# Changelog

## 0.6.0 — Phase 6 — 2026-11-20
- Added MongoDB security audit events linked by sequential SHA-256 hashes.
- Added admin-only audit listing and hash-chain verification endpoints.
- Added administrator account/role management UI and role-update API with last-enabled-admin protection.
- Added configurable fixed-window rate limiting for login, registration, wallet challenge/verification and refresh endpoints.
- Added security response headers and a production profile guard for JWT secret, HTTPS origin and secure cookies.
- Added audit-chain and rate-limit tests (authored, not executed).
- Documented single-instance limitations and production WORM/SIEM recommendations.
- Runtime compilation, unit tests and end-to-end verification remain unexecuted.

## 0.5.0 — Phase 5 — 2026-11-20
- Added durable MongoDB identity synchronization jobs with unique idempotency keys and explicit lifecycle states.
- Added asynchronous Web3j transaction submission, receipt polling, confirmation-depth checks and canonical block hash validation.
- Added bounded retry/backoff, transaction receipt timeout and administrator retry for failed or reorged jobs.
- Added a persistent IdentityRegistry event indexer with idempotent log storage, block cursor and bounded reorg rewind.
- Added administrator reconciliation API comparing linked wallets with on-chain identity state.
- Added account suspension/reactivation, refresh-session revocation, JWT token-version invalidation and queued wallet deactivation.
- Kept MongoDB as the sole authority for Spring Security application roles; chain roles do not grant app privileges.
- Added hybrid synchronization documentation and JUnit tests (not executed in this environment).
- Maven/runtime integration tests remain unexecuted.

## 0.4.0 — Phase 4 — 2026-11-20
- Added OpenZeppelin AccessControl-based IdentityRegistry Solidity contract.
- Added wallet identity registration, application roles, status changes, reads and events.
- Added Hardhat deployment script with optional backend manager-role grant and deployment manifest output.
- Added nine Hardhat contract tests.
- Added Web3j service for network checks, identity reads, gas estimation, transaction signing, receipt polling and log retrieval.
- Added admin-only identity registry API with MongoDB linked-wallet checks.
- Added backend role-mapping tests and smart-contract/deployment documentation.
- Runtime compilation and integration checks remain unexecuted in this environment.

## 0.3.0 — Phase 3
- Added MetaMask + SIWE wallet authentication, EIP-191 verification, one-time MongoDB nonce and wallet management.

## 0.2.0 — Phase 2
- Traditional authentication source checkpoint.

## 0.1.0 — Phase 1
- Initial foundation scaffold.
