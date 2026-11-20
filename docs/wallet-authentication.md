# Phase 3 — Ethereum Wallet Authentication

## Scope
- MetaMask browser-wallet connection through ethers.js v6.
- Server-issued SIWE-style challenge with domain, URI, address, chain ID, random nonce, issued-at and expiration.
- EIP-191 `personal_sign` signature recovery in the Spring Boot backend using Web3j.
- MongoDB nonce persistence, five-minute expiry and atomic one-time consumption.
- Wallet-to-existing-user association; no separate wallet-only account creation.
- Wallet link/unlink APIs and shared Phase 2 access/refresh session issuance.

## API
- `POST /api/auth/wallet/nonce` — body `{ address, chainId, purpose }`; purpose is `LOGIN` or `LINK`.
- `POST /api/auth/wallet/verify` — body `{ address, message, signature }`; verifies a linked wallet and issues the existing ChainAuth session cookies/tokens.
- `POST /api/auth/wallet/link/verify` — same body, requires a valid Phase 2 access token and links wallet to that account.
- `GET /api/users/me/wallets` — list wallets for the authenticated account.
- `DELETE /api/users/me/wallets/{walletId}` — unlink a wallet belonging to the authenticated account.

## SIWE validation model
The backend creates and stores the complete message and compares the submitted message byte-for-byte to the challenge stored for that address. This binds the signed message to the configured domain, URI, chain ID, nonce, issued-at and expiry fields; the client cannot substitute any of them. The backend then recovers the EOA address from the EIP-191 personal-sign signature and compares it to the challenge address. Nonce consumption uses one conditional MongoDB `findAndModify` operation, so only one concurrent verification can consume a challenge.

Configure `SIWE_DOMAIN`, `SIWE_URI`, `SIWE_NONCE_TTL_SECONDS` and `ETHEREUM_CHAIN_ID`. The default local chain ID is 31337. Frontend and backend SIWE domain/URI must match the origin used by the browser.

## Limitations / security notes
- This phase verifies externally owned accounts (EOAs) with EIP-191. ERC-1271 contract-wallet verification is deferred because it requires querying the configured chain and handling contract-wallet semantics; it is not silently treated as supported.
- Wallet login is available only for wallets already linked to an existing ChainAuth account. First link a wallet from an authenticated account.
- Unlinking is blocked if it would remove the account's only authentication method.
- No gas is needed for SIWE signing; signing a message is not an on-chain transaction.
- Never request a seed phrase or private key.
- Use HTTPS and set `SECURE_COOKIE=true` outside local development.
