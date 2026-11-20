# ChainAuth Test Strategy and Release Gate

## Automated checks

- Backend: `cd backend && mvn --batch-mode clean verify` (Java 21, Maven 3.9+).
- Frontend: `cd frontend && npm install && npm run build` (Node.js 20+).
- Solidity: `cd blockchain && npm install && npm run compile && npm test` (Hardhat and Solidity 0.8.28).
- Dependency-light source checks: `./scripts/verify-project.sh --static` (Python 3; PyYAML recommended; Node.js/TypeScript parser used for frontend syntax when available).
- Full local verification: `./scripts/verify-project.sh --full` after installing dependencies.

GitHub Actions workflow `.github/workflows/ci.yml` runs backend tests, frontend build, smart-contract compile/tests and static checks on pushes and pull requests. CI results must be observed before claiming those checks passed; adding the workflow does not itself execute it.

## Existing automated tests

- Solidity: `blockchain/test/IdentityRegistry.test.js`.
- Backend wallet signature and SIWE: `backend/src/test/java/com/chainauth/wallet/`.
- Backend authentication/JWT: `backend/src/test/java/com/chainauth/auth/` and `security/`.
- Backend blockchain role and synchronization: `backend/src/test/java/com/chainauth/blockchain/`.
- Backend audit and rate limiting: `backend/src/test/java/com/chainauth/audit/` and `security/`.
- Backend account suspension/role protections: `backend/src/test/java/com/chainauth/user/`.

## Manual integration acceptance

Follow `deployment/ACCEPTANCE-TESTS.md` for MetaMask, MongoDB, local Ethereum, sync lifecycle, reorg, authorization, security and recovery scenarios. These flows require a live staging environment and are not proven by static source checks or unit tests alone.

## Evidence discipline

Record command, environment/version, exit code and test report for each check. Do not mark unexecuted tests as passed. The project is not production-certified; complete a separate security review and accept the single-instance/audit limitations documented in the release runbook.
