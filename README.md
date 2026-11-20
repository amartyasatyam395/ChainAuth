# ChainAuth

ChainAuth is a phased hybrid authentication project combining a Spring Boot API, MongoDB, a React frontend and Ethereum identity infrastructure.

## Current delivery

**Phases 1–7 source implementation checkpoints are present.** Phase 7 adds a repeatable CI pipeline, static/full verification runners, a cross-phase acceptance matrix and an operations runbook. See `PROJECT_STATUS.md`, `deployment/ACCEPTANCE-TESTS.md` and `deployment/RELEASE-RUNBOOK.md`. Runtime and end-to-end checks must still be executed in an equipped environment.

## Phase 3 wallet workflow

1. Register and sign in with email/password.
2. Open **Manage linked wallets** in the dashboard.
3. Connect MetaMask on the configured chain and sign the one-time SIWE challenge.
4. After linking, sign out and use **Sign in with MetaMask** on the login page.

A wallet cannot create an unrelated account or bypass the existing account model. The same JWT and refresh-cookie session service is used for both login methods.

## Requirements

- Java 21
- Maven 3.9+
- Node.js 20+
- MongoDB 7+
- Local Ethereum JSON-RPC node (Hardhat)

## Run locally

1. Copy `.env.example` to `.env` and adjust values. Do not commit `.env`.
2. Start MongoDB and a local Ethereum node.
3. Backend: `cd backend && mvn spring-boot:run`
4. Frontend: `cd frontend && npm install && npm run dev`
5. Blockchain tooling: `cd blockchain && npm install && npm run node`

The backend reads environment variables from the process environment. Export them before starting Spring Boot; `.env` is a template, not automatically loaded.

## Phase 2 authentication

See `docs/traditional-authentication.md` and `docs/api-reference.md` for endpoints, token lifecycle and security limitations. Configure `JWT_SECRET` before starting the backend. The frontend includes registration, login, dashboard, profile and security pages.

## Wallet authentication (Phase 3)

MetaMask + ethers.js wallet login and authenticated wallet linking use a server-issued SIWE challenge and Web3j EIP-191 signature recovery. Only previously linked wallets can log in; first link a wallet from an authenticated password session. Configure `SIWE_DOMAIN`, `SIWE_URI`, `SIWE_NONCE_TTL_SECONDS` and `ETHEREUM_CHAIN_ID`. ERC-1271 contract-wallet verification is deferred and not supported in this milestone.

## Health endpoints

- `GET /api/health`
- `GET /api/health/database`
- `GET /api/health/blockchain`

Database and blockchain endpoints perform live connectivity requests. An unavailable dependency returns HTTP 503. The root health endpoint reports API process health only.

## Security note

This is a development/academic implementation checkpoint, not a production security certification. Runtime builds and end-to-end flows remain unverified in this environment. Configure strong secrets, HTTPS and secure cookies before deployment. Do not expose the development RPC node publicly.


## Phase 4 — On-chain identity registry

The Solidity `IdentityRegistry` contract uses OpenZeppelin AccessControl and supports identity registration, role assignment, deactivation/reactivation, identity reads and events. Spring Boot integrates with the contract through Web3j, verifies the configured chain, estimates gas, submits signed transactions and waits for receipts. Only ChainAuth ADMIN users can use the identity administration API, and target wallets must already be linked to an account. See `docs/smart-contracts.md` and `docs/blockchain-guide.md`.

## Phase 5 — Hybrid identity synchronization

Phase 5 adds durable registration jobs between MongoDB and the Ethereum IdentityRegistry. Administrators enqueue registrations, workers submit transactions asynchronously, poll receipts and wait for the configured confirmation depth before a job becomes `CONFIRMED`. Idempotency keys protect request retries; failed/reorged jobs have an explicit retry endpoint. A scheduled indexer persists raw registry logs and a block cursor, with a bounded 12-block rewind when a reorg is detected. The reconciliation endpoint compares linked wallets to current chain state. Admin account suspension disables sign-in, invalidates access JWTs, revokes refresh sessions and queues deactivation of active linked on-chain identities; reactivation requires a fresh login and explicit wallet reactivation. **MongoDB remains the source of truth for Spring Security roles; on-chain role values do not grant application permissions.** See `docs/hybrid-synchronization.md`.


## Phase 6 — Security and administration

Phase 6 adds a MongoDB-backed hash-linked audit trail, administrator account/role management at `/admin`, configurable authentication endpoint rate limiting, security response headers and production startup checks. Audit records omit request bodies and credentials; client addresses are stored only as SHA-256 hashes. The in-memory rate limiter and audit append sequence are single-instance controls, and the audit chain is tamper-evident rather than immutable. See `docs/phase-6-security-administration.md`.


## Phase 7 — Final integration and release preparation

Run `./scripts/verify-project.sh --static` for dependency-light source checks. After installing the Java, Maven, Node/npm and project dependencies, run `./scripts/verify-project.sh --full`. GitHub Actions is configured in `.github/workflows/ci.yml`. Use `deployment/ACCEPTANCE-TESTS.md` for live integration acceptance and `deployment/RELEASE-RUNBOOK.md` for staged deployment and incident response. A successful static check is not a production-readiness or security certification.
