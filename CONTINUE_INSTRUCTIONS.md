# Continue ChainAuth Development

Current source checkpoint includes Phases 1–7. The final implementation checkpoint is complete; runtime build, tests and live acceptance remain release gates and must be executed in a staging environment.

## First actions when continuing
1. Install Java 21, Maven 3.9+, Node.js 20+, MongoDB 7+ and npm dependencies.
2. Run `cd backend && mvn test package`; resolve Java/Web3j API compatibility errors and execute Phase 5 `IdentitySyncServiceTests`.
3. Run `cd frontend && npm install && npm run build`.
4. Run `cd blockchain && npm install && npm run compile && npm test`.
5. Start MongoDB and a Hardhat local node; deploy `IdentityRegistry` and configure `IDENTITY_REGISTRY_ADDRESS`.
6. Configure the backend signer key and ensure its public address holds `IDENTITY_MANAGER_ROLE`.
7. Set `BLOCKCHAIN_TRANSACTION_TIMEOUT_SECONDS` according to the target network and `BLOCKCHAIN_EVENT_START_BLOCK` to the actual contract deployment block (do not leave at zero for a long-lived chain).
8. Exercise `POST /api/admin/identity-sync/registrations` with an `Idempotency-Key`, observe QUEUED → SUBMITTED → CONFIRMED, and test receipt timeout, failed/reorged retry.
9. Verify event cursor advancement, duplicate-log suppression and reorg rewind using a local chain that can mine and reorganize blocks.
10. Exercise `GET /api/admin/identity-sync/reconcile` and verify that on-chain role data never changes Spring Security privileges.
11. Update PROJECT_STATUS.md with actual executed results before Phase 7.

## Phase 6 files
- `audit/AuditEvent.java`, `AuditEventRepository.java`, `AuditService.java`, `AuditCaptureFilter.java`, `AdminAuditController.java`.
- `security/AuthenticationRateLimitFilter.java` and `config/ProductionSecurityGuard.java`.
- Admin role update API and `/admin` frontend page.
- `docs/phase-6-security-administration.md`.

## Phase 5 files
- `blockchain/IdentitySyncJob.java`, `IdentitySyncJobRepository.java`, `IdentitySyncService.java`, `IdentitySyncWorker.java`, `IdentitySyncController.java`.
- `blockchain/IdentityRegistryEventIndexer.java`, `IndexedChainEvent.java`, `IndexedChainEventRepository.java`, `ChainEventCursor.java`, `ChainEventCursorRepository.java`.
- `blockchain/IdentityReconciliationService.java`.
- `user/AdminAccountService.java`, `user/AdminAccountController.java`; JWT token-version invalidation in `security/JwtService.java` and `security/JwtAuthenticationFilter.java`.
- `docs/hybrid-synchronization.md`.

## Phase 5 APIs
- `GET /api/admin/users` — list accounts without credential hashes.
- `POST /api/admin/users/{userId}/suspend` — disable account, invalidate access JWTs, revoke refresh sessions and queue on-chain deactivation.
- `POST /api/admin/users/{userId}/reactivate` — re-enable account without restoring old sessions/wallet identity.
- `POST /api/admin/identity-sync/registrations` — queue a registration; send `Idempotency-Key` for safe request retries.
- `GET /api/admin/identity-sync` — latest jobs.
- `GET /api/admin/identity-sync/{id}` — job details.
- `POST /api/admin/identity-sync/{id}/retry` — retry FAILED/REORGED jobs.
- `POST /api/admin/identity-sync/wallets/{address}/role` — enqueue role assignment.
- `POST /api/admin/identity-sync/wallets/{address}/deactivate` or `/reactivate` — enqueue status changes.
- `GET /api/admin/identity-sync/reconcile` — compare up to 500 linked wallets against chain state.

All these APIs require the ChainAuth ADMIN role. MongoDB application roles remain authoritative. The event indexer currently assumes one application instance per contract/network; add a distributed lease before horizontal scaling.

## Prior phase notes
- Contract: `blockchain/contracts/IdentityRegistry.sol`.
- Backend signer private key comes from `BLOCKCHAIN_SIGNER_PRIVATE_KEY`. Never commit it or put it in deployment manifests.
- ERC-1271 remains deferred.

## Next milestones
- Phase 7: end-to-end tests, complete docs, deployment preparation and final security review.

Preserve existing modules and APIs. Never store plaintext passwords, private keys, seed phrases or real secrets. Create and verify a full ZIP after each phase.


## Phase 7 files
- `.github/workflows/ci.yml` — CI jobs for backend, frontend, Solidity and static checks.
- `scripts/verify-static.py` — dependency-light static source checks.
- `scripts/verify-project.sh --static|--full` — static or full build/test runner.
- `deployment/ACCEPTANCE-TESTS.md` — cross-phase integration/release checklist.
- `deployment/RELEASE-RUNBOOK.md` — release and incident-response runbook.

Before production, run the full commands in the acceptance checklist and resolve the documented single-instance and audit-retention limitations.
