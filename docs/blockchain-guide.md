# Blockchain Development Guide

## Local Hardhat node
Run `npm install` and `npm run node` inside `blockchain/`. The configured local network is chain ID `31337` and JSON-RPC URL `http://127.0.0.1:8545` by default.

## IdentityRegistry
Compile with `npm run compile`, run tests with `npm test`, and deploy with `npm run deploy`. The deployment script saves the ABI, address and chain ID under `blockchain/deployments/<chainId>.json`.

## Backend connectivity
Spring Boot reads `ETHEREUM_RPC_URL`, `ETHEREUM_CHAIN_ID`, `IDENTITY_REGISTRY_ADDRESS`, `BLOCKCHAIN_SIGNER_PRIVATE_KEY`, and `BLOCKCHAIN_GAS_LIMIT`. The backend reports connection and configuration state at `GET /api/admin/identities/network`.

The signer key is never stored in source or the generated deployment JSON. Use a disposable local-only key during development. For a production deployment, use a dedicated signer backed by a secrets manager/KMS/HSM and restrict its permissions.
