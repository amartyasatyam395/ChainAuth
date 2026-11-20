# Phase 2 — Traditional Authentication

## Implemented API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create a USER account |
| POST | `/api/auth/login` | Verify credentials and issue access/refresh tokens |
| POST | `/api/auth/refresh` | Rotate the refresh token and issue a new access token |
| POST | `/api/auth/logout` | Revoke the current refresh token and clear its cookie |
| GET | `/api/auth/csrf` | Obtain the CSRF token for state-changing requests |
| GET | `/api/auth/me` | Current authenticated user |
| GET | `/api/users/me` | Current profile |
| PUT | `/api/users/me` | Update display name |
| POST | `/api/users/me/change-password` | Change password and revoke refresh sessions |
| GET | `/api/users/me/security` | Account security summary |
| GET | `/api/admin/status` | ADMIN-only authorization probe |

## Security design

- Email is trimmed and lowercased before lookup/storage. MongoDB unique index enforces uniqueness under concurrent registration attempts.
- Passwords are hashed with BCrypt (work factor 12); raw passwords are never persisted. Registration requires at least 12 characters and a maximum of 72 UTF-8 characters (BCrypt input limit).
- Access JWT lifetime defaults to 900 seconds (15 minutes). The JWT signing secret must be supplied via `JWT_SECRET` and be at least 32 bytes.
- Refresh tokens are 512-bit random opaque values. Only SHA-256 hashes are stored in MongoDB. The browser receives the raw token only in an HttpOnly, SameSite=Strict cookie scoped to `/api/auth`.
- Refresh rotation atomically marks the previous token used. Reuse of a consumed token revokes its token family. MongoDB TTL removes expired refresh-token documents.
- CSRF protection uses Spring Security's cookie repository and `X-XSRF-TOKEN` request header. The frontend obtains a token from `/api/auth/csrf`.
- The backend is stateless. Authorization uses roles loaded from MongoDB, not client-provided role claims.
- Password change revokes all refresh tokens for the user. Existing access JWTs remain valid until their short expiry (maximum 15 minutes); clients should discard the in-memory access token and sign in again.

## Local development

Set `JWT_SECRET` to a random value at least 32 bytes long. `SECURE_COOKIE=false` is suitable only for local HTTP development. Set `SECURE_COOKIE=true` behind HTTPS in production. The frontend origin must exactly match `FRONTEND_ORIGIN`.

The `ADMIN` role is intentionally not self-assignable. For local demonstrations, assign it through a trusted database/admin bootstrap procedure; do not expose a public role-escalation endpoint.

## Known limitations

- No email verification, password reset, MFA, account lockout policy or admin management UI in this phase.
- Refresh-token reuse detection is family-wide; simultaneous refresh requests from multiple tabs can be interpreted as reuse and revoke that family. Coordinate refreshes in a multi-tab production client.
- Access JWT revocation is bounded by access-token expiry; immediate denylisting is not implemented.
- Runtime integration tests require a reachable MongoDB instance.
