# Phase 5 — Hybrid identity synchronization

## Trust model
MongoDB remains the source of truth for ChainAuth accounts and Spring Security application roles. Ethereum is an independently verifiable identity registry. A pending, submitted, failed, or reorged chain transaction never grants application privileges. This phase deliberately does not promote an on-chain ADMIN role into Spring Security ADMIN.

## Durable registration workflow
1. An authenticated ChainAuth administrator submits `POST /api/admin/identity-sync/registrations` with the existing `IdentityRegistrationRequest` body and optional `Idempotency-Key` header.
2. The API verifies that the wallet is linked to a MongoDB user, then persists an `identity_sync_jobs` record and returns HTTP 202. Repeating the same key with the same wallet/operation/role returns the same job; reusing a key with a different payload is rejected.
3. `IdentitySyncWorker` submits queued jobs without blocking the request thread. Submission attempts and retry timestamps are persisted. Exponential backoff is bounded; the configured attempt limit moves a job to FAILED.
4. The worker polls the transaction receipt, checks its canonical block hash, waits for the configured confirmation depth, and then reads the contract state. A job becomes CONFIRMED only if registration, active state and requested role match.
5. Reorged or failed jobs require explicit administrator retry via `POST /api/admin/identity-sync/{id}/retry`. A retry clears the old transaction reference and starts a new submission.

## Account suspension and sessions
`POST /api/admin/users/{userId}/suspend` disables the MongoDB account, increments the JWT token version (immediately invalidating prior access tokens), revokes all refresh-token records, and queues deactivation jobs for linked wallets that are currently registered and active on-chain. RPC failures are reported in `walletsNotQueued`; they do not undo the account suspension. `POST /api/admin/users/{userId}/reactivate` re-enables the account but does not restore old sessions or automatically reactivate wallets. An administrator must explicitly queue wallet reactivation. This avoids accidentally restoring an old wallet identity when an account is reopened.

## API
- `POST /api/admin/identity-sync/registrations` — enqueue an idempotent registration. Optional `Idempotency-Key` header (max 128 characters).
- `POST /api/admin/identity-sync/wallets/{address}/role` — enqueue on-chain role assignment.
- `POST /api/admin/identity-sync/wallets/{address}/deactivate` and `/reactivate` — enqueue identity status changes.
- `GET /api/admin/identity-sync` — latest 100 jobs.
- `GET /api/admin/identity-sync/{id}` — job status.
- `POST /api/admin/identity-sync/{id}/retry` — retry FAILED or REORGED jobs.
All endpoints require the ChainAuth ADMIN application role.

## Event indexing and reorg handling
`IdentityRegistryEventIndexer` reads bounded block ranges and stores raw contract log records in `identity_registry_events`, using transaction hash + log index as an idempotency key. The persisted `identity-registry` cursor advances only after the range is processed. On a cursor block-hash mismatch, the indexer rewinds up to 12 blocks and removes indexed events from the rewind point before replay. Configure `BLOCKCHAIN_EVENT_START_BLOCK` to the contract deployment block to avoid scanning unrelated history. This is a bounded best-effort reorg strategy, not a full archive-node canonical-chain indexer.

## Configuration
- `BLOCKCHAIN_CONFIRMATIONS` (default 3)
- `BLOCKCHAIN_MAX_SYNC_ATTEMPTS` (default 5)
- `BLOCKCHAIN_TRANSACTION_TIMEOUT_SECONDS` (default 600; minimum effective value 60)
- `BLOCKCHAIN_SYNC_POLL_MS` (default 5000)
- `BLOCKCHAIN_EVENT_START_BLOCK` (default 0; set to deployment block)
- `BLOCKCHAIN_EVENT_POLL_MS` (default 15000)

## Operational caveats
MongoDB and Ethereum cannot participate in one atomic transaction. The durable job is the recovery boundary. MongoDB unique indexes must be enabled (`spring.data.mongodb.auto-index-creation=true`, already configured). Run only one indexer instance per contract/network unless a distributed lease is added; the current cursor does not implement multi-instance leader election. A job stuck in SUBMITTED is retried by receipt polling, not blindly re-submitted. RPC errors leave the cursor unchanged. The raw event payload is retained for later decoding; this phase does not infer app permissions from event contents.

The backend signer remains a high-value secret. Use a KMS/HSM or dedicated signing service in production. Never commit the private key.
