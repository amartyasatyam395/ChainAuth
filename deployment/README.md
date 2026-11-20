# Deployment Preparation

Phase 4 supports local Hardhat deployment only. Follow `docs/smart-contracts.md` and `docs/blockchain-guide.md` to deploy `IdentityRegistry` to the local chain and configure Spring Boot.

No mainnet deployment is performed. Sepolia deployment instructions and production secrets management will be completed in Phase 7. Never store private keys in this directory or commit real `.env` files.

## Phase 7 release materials

- `ACCEPTANCE-TESTS.md` is the required cross-phase integration and release checklist.
- `RELEASE-RUNBOOK.md` covers environment separation, deployment sequencing and incident response.
- `../.github/workflows/ci.yml` defines automated build/test jobs.
- `../scripts/verify-project.sh --static` runs source sanity checks; `--full` runs the full build/test suite when prerequisites and dependencies are installed.

Do not interpret a successful static check as a passing runtime test or production security certification. See `PROJECT_STATUS.md` for the checks actually executed in this environment.
