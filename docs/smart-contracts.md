# Phase 4 — IdentityRegistry Smart Contract

## Purpose
`IdentityRegistry.sol` stores the minimal on-chain identity state for wallets already verified and linked by ChainAuth. MongoDB remains the primary application user database. The contract is a verifiable registry, not a replacement user database.

## Contract data model
- `registered`: whether the wallet has an identity record.
- `active`: whether the identity is currently active.
- `role`: `USER = 0`, `MODERATOR = 1`, `ADMIN = 2`.
- `registeredAt`: block timestamp of initial registration.

The contract does not store names, emails, passwords, or other personal data. Phase 4 on-chain roles are not yet synchronized into MongoDB or Spring Security; they are registry data only until Phase 5 implements a confirmation-aware synchronization policy.

## Access control
OpenZeppelin `AccessControl` is used:
- `DEFAULT_ADMIN_ROLE`: controls manager grants/revocations.
- `IDENTITY_MANAGER_ROLE`: can register, deactivate, reactivate, and assign application roles.
- The deployer receives both roles at deployment.
- The deployment script can grant `IDENTITY_MANAGER_ROLE` to the backend signer address.

The Spring Boot admin endpoints also require the ChainAuth application `ADMIN` role. The backend signer is a privileged on-chain identity manager; protect it as a production secret (use a KMS/HSM or dedicated signing service in production). Its on-chain role does not automatically grant or revoke a ChainAuth application role.

## Contract functions
- `registerIdentity(address, Role)`
- `assignRole(address, Role)`
- `deactivateIdentity(address)`
- `reactivateIdentity(address)`
- `isRegistered(address)`
- `isActive(address)`
- `getRole(address)`
- `getIdentity(address)`

Events:
- `IdentityRegistered`
- `IdentityRoleUpdated`
- `IdentityDeactivated`
- `IdentityReactivated`

## Deploy locally
From `blockchain/`:
1. `npm install`
2. Terminal A: `npm run node`
3. Terminal B: set `ETHEREUM_CHAIN_ID=31337`, `ETHEREUM_RPC_URL=http://127.0.0.1:8545`.
4. Set `CHAINAUTH_BACKEND_SIGNER_ADDRESS` to the address corresponding to the backend signer key (if using a separate signer).
5. `npm run compile`
6. `npm run deploy`

The deploy script writes `deployments/31337.json` with the deployed address and ABI. Set that address as `IDENTITY_REGISTRY_ADDRESS` for Spring Boot.

## Backend configuration
- `ETHEREUM_RPC_URL`: JSON-RPC endpoint.
- `ETHEREUM_CHAIN_ID`: expected network chain ID.
- `IDENTITY_REGISTRY_ADDRESS`: deployed contract address.
- `BLOCKCHAIN_SIGNER_PRIVATE_KEY`: private key of the backend transaction signer (never commit it).
- `BLOCKCHAIN_GAS_LIMIT`: maximum gas limit accepted after a 20% estimate buffer.

The backend verifies the connected chain before reads and writes. It estimates gas, sends a signed transaction, waits for a receipt, checks receipt status, and can query contract logs.

## Admin API
All `/api/admin/identities/**` endpoints require the ChainAuth `ADMIN` role. Registration and mutations also require the wallet address to already exist in MongoDB's linked-wallet collection. This prevents the admin API from creating on-chain identities for arbitrary unlinked addresses.

## Security and trust boundaries
- No personal information is written to Ethereum.
- The frontend cannot call contract mutation functions through a frontend-supplied claim.
- Contract access control is enforced independently of Spring Security.
- A compromised backend signer can exercise its on-chain manager permissions; use a dedicated key and rotate/revoke its role if compromised.
- The backend signer is configured through an environment variable only.
- Do not use a production wallet private key in local development.
- Do not deploy to mainnet automatically.

## Limitations
- MongoDB and Ethereum are not a distributed transaction. A transaction can be pending or fail after the API request; later phases add durable synchronization and retries.
- Phase 4 does not yet implement event indexing/reorg recovery or automatic synchronization. The API can retrieve raw event logs from a supplied block.
- Phase 4 uses a backend manager signer for on-chain writes. End-user wallet-signed contract transactions are not used for these admin workflows.
