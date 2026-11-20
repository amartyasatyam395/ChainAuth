# Identity Registry API and Role Mapping

| Contract enum | Numeric value | Meaning |
|---|---:|---|
| USER | 0 | Standard registered identity |
| MODERATOR | 1 | Moderation-level identity |
| ADMIN | 2 | Administrative identity |

ChainAuth API operations are protected by the existing Spring Security ADMIN application role. On-chain operations are submitted by the backend signer, which must hold `IDENTITY_MANAGER_ROLE`.

See `smart-contracts.md` for deployment and security requirements and `api-reference.md` for HTTP endpoint details.
