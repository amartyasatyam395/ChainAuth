# ChainAuth Phase 7 — Integration Acceptance Plan

This checklist is the release gate for Phases 1–7. A check is **not passed** until it is executed against the target environment and evidence (command output, test report, transaction hash or sanitized HTTP response) is retained.

## A. Reproducible build

- [ ] Java 21 and Maven 3.9+; `cd backend && mvn --batch-mode clean verify`.
- [ ] Node.js 20+; `cd frontend && npm ci && npm run build` (commit a lockfile before relying on `npm ci`; until then use `npm install`).
- [ ] `cd blockchain && npm install && npm run compile && npm test`.
- [ ] `./scripts/verify-project.sh --static`.
- [ ] Record exact Java, Maven, Node, npm, Solidity compiler and dependency versions.

## B. Traditional authentication

- [ ] Register a new user; duplicate email is rejected without disclosing password or hash.
- [ ] Correct password logs in; incorrect password is rejected.
- [ ] Password hash is BCrypt and never returned by any API.
- [ ] Access token expires; refresh rotates the refresh token; replay of an old refresh token is rejected.
- [ ] Logout revokes refresh session; subsequent refresh fails.
- [ ] CSRF is required for cookie-backed state-changing operations; CORS accepts only configured frontend origin.

## C. Wallet authentication

- [ ] Password-authenticated account links an EOA wallet using a fresh server nonce and valid EIP-191 signature.
- [ ] Invalid signature, altered message, wrong chain, expired nonce and reused nonce are rejected.
- [ ] Unlinked wallet cannot sign in; linked wallet can sign in and receives the standard ChainAuth session.
- [ ] Unlinking removes the wallet association; contract wallets are rejected unless ERC-1271 is separately implemented.

## D. Ethereum identity and hybrid sync

- [ ] Deploy `IdentityRegistry` to a disposable local chain; confirm deployment chain ID and address.
- [ ] Backend signer is a manager; unauthorized signer cannot mutate identities.
- [ ] Queue registration twice with the same idempotency key; only one job/transaction is created.
- [ ] Exercise QUEUED → SUBMITTED → CONFIRMED; verify receipt, configured confirmation depth and final contract state.
- [ ] Exercise failed transaction, timeout, bounded retry and explicit admin retry.
- [ ] Duplicate event logs do not create duplicate index records; simulated reorg rewinds the cursor safely.
- [ ] Reconciliation reports missing/inactive identities accurately.
- [ ] On-chain role changes never grant MongoDB/Spring Security application privileges.

## E. Security and administration

- [ ] Non-admin cannot access admin users, identity sync, registry, or audit APIs.
- [ ] Admin can suspend/reactivate accounts and update roles; last enabled admin cannot be removed.
- [ ] Suspension invalidates existing access JWTs and refresh sessions; reactivation does not revive old sessions.
- [ ] Rate limits return 429 and `Retry-After`; confirm shared storage is configured before multi-instance deployment.
- [ ] Audit chain verifies after normal operations; tampering/deletion is detected. Forward production audit to independently controlled WORM/SIEM storage.
- [ ] Production profile rejects weak/default secrets, non-HTTPS frontend origin and insecure cookies.
- [ ] Secrets, signatures, passwords, cookies and authorization headers are absent from logs and audit documents.

## F. Deployment and recovery

- [ ] Restore MongoDB backup in a staging environment and verify users, refresh tokens, wallets, sync jobs, event cursor and audit records.
- [ ] Confirm MongoDB indexes are created and backup/restore retention is documented.
- [ ] Confirm RPC endpoint, chain ID, contract address, deployment block, confirmation depth and signer permissions.
- [ ] Run one indexer per network/contract unless distributed leader election is implemented.
- [ ] Use KMS/HSM or isolated signer service for the backend transaction key; never store it in source control or deployment manifests.
- [ ] Verify TLS termination, secure cookies, CORS, firewall rules and restricted RPC access.
- [ ] Perform a staged rollback and document how to pause workers/indexers without losing queued jobs.

## Release decision

Do not call the project production-ready until all relevant checks above pass in a controlled staging environment, unresolved limitations are accepted, and an independent security review is complete. The repository's static check is a source sanity check, not runtime or security certification.
