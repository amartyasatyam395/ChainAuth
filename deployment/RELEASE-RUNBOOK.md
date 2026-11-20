# ChainAuth Release and Operations Runbook

## Environment separation

Maintain separate development, staging and production MongoDB databases, Ethereum RPC endpoints, contract deployments, JWT secrets, audit HMAC secrets and transaction signer keys. Never reuse local Hardhat accounts or development secrets in production.

## Required configuration

| Variable | Purpose |
|---|---|
| `MONGODB_URI` | MongoDB connection string with least-privilege credentials |
| `JWT_SECRET` | Unique high-entropy JWT signing secret (32+ bytes) |
| `AUDIT_HASH_SECRET` | Separate high-entropy HMAC key for audit source pseudonymization |
| `SECURE_COOKIE` | `true` in production |
| `FRONTEND_ORIGIN` | Exact HTTPS origin; no wildcard |
| `ETHEREUM_RPC_URL` | Trusted private/provider RPC endpoint |
| `ETHEREUM_CHAIN_ID` | Exact target chain ID |
| `IDENTITY_REGISTRY_ADDRESS` | Address deployed on that exact chain |
| `BLOCKCHAIN_SIGNER_PRIVATE_KEY` | Backend transaction signer secret; inject via secret manager only |
| `CHAINAUTH_BACKEND_SIGNER_ADDRESS` | Public signer address granted `IDENTITY_MANAGER_ROLE` at deployment |
| `BLOCKCHAIN_EVENT_START_BLOCK` | Contract deployment block, not an arbitrary historical zero on a mature chain |
| `BLOCKCHAIN_CONFIRMATIONS` | Network-appropriate finality threshold |
| `SIWE_DOMAIN`, `SIWE_URI` | Exact public frontend domain and URI used in signed challenges |

The `.env.example` files are templates. Spring Boot does not automatically load `.env`; inject variables into the process/container environment. The production profile additionally validates key settings at startup.

## Release sequence

1. Back up MongoDB and verify restore in staging.
2. Run `scripts/verify-project.sh --static` and the full test commands in `deployment/ACCEPTANCE-TESTS.md`.
3. Deploy the contract to the target chain and record chain ID, address, deployment block and transaction hash in a controlled release record.
4. Grant `IDENTITY_MANAGER_ROLE` to the backend signer; verify manager permissions and signer address.
5. Configure backend secrets from a secret manager; enable the `prod` profile, HTTPS, secure cookies and exact CORS origin.
6. Deploy backend and frontend; check `/api/health`, `/api/health/database` and `/api/health/blockchain`.
7. Run the staging acceptance suite before opening traffic.
8. Monitor failed sync jobs, event cursor lag, audit verification, rate-limit events and RPC errors.

## Incident response

- **Signer compromise:** stop the sync worker, revoke the compromised manager role from a separate trusted admin wallet, rotate/reissue signer credentials and inspect transactions from the compromise window.
- **JWT secret compromise:** rotate the secret and invalidate active sessions; expect users to authenticate again.
- **Audit integrity failure:** preserve database snapshots, export records to independent storage, restrict write access and investigate before attempting repair.
- **Chain reorganization:** pause identity mutations if finality is uncertain, preserve job/event evidence, compare canonical contract state and use the supported reconciliation/retry workflow.
- **MongoDB outage:** pause writes/workers, restore from verified backup, reconcile outstanding on-chain transactions before replaying jobs.

## Known release blockers / architectural limits

The current in-memory rate limiter and synchronized audit append are single-instance controls. The event indexer assumes one active instance per network/contract. Hash-linked MongoDB audit rows are tamper-evident, not immutable. ERC-1271 is not implemented. Do not scale horizontally or describe audit storage as immutable until these controls are replaced with distributed coordination, shared rate limiting and independently controlled retention.
