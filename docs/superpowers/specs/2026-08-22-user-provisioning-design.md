# Design: Just-in-Time User Provisioning from JWT

Date: 2026-08-22
Status: Approved

## Problem

The API authenticates requests as Keycloak-issued JWTs (OAuth2 resource server), but the local
`users` table has no way of learning about those identities. Any feature that references a local
`User` (ticket purchaser, event organizer, staff) needs a row that corresponds to the caller.

## Decisions

- **Just-in-time provisioning:** users are provisioned from validated JWT claims; there is no
  signup API and no Keycloak Admin API integration.
- **Upsert on every request:** name/email are kept in sync with the token on each authenticated
  request (update only when changed).
- **Identity link:** the local `User.id` is the JWT `sub` claim. No extra provider-id column.
  All users are assumed to come from this Keycloak realm.
- **No local roles:** authorization keeps using Keycloak roles from the token directly. The local
  row stores identity/profile only (`id`, `name`, `email`).

## Architecture

| Unit | Package | Purpose |
|---|---|---|
| `UserRepository` | `com.example.tickets.repository` | `JpaRepository<User, UUID>`; PK lookup only |
| `UserProvisioningService` | `com.example.tickets.service` | `User upsert(Jwt jwt)` — all provisioning logic |
| `UserProvisioningFilter` | `com.example.tickets.filter` | Thin `OncePerRequestFilter`; calls service, exposes result |
| `SecurityConfig` | `com.example.tickets.config` | `SecurityFilterChain` preserving resource-server defaults + filter registration after `BearerTokenAuthenticationFilter` |

The filter is not a Spring bean; it is constructed by `SecurityConfig`, so it runs exactly once,
inside the security chain (a `@Component` filter would also be auto-registered as a servlet
filter).

The service deliberately has no class-level `@Transactional`: the insert-race recovery needs a
fresh read after a constraint violation, which would fail in the same rolled-back Hibernate
transaction. Each repository call runs in its own short transaction instead.

## Data flow

```
Request + Bearer token
→ BearerTokenAuthenticationFilter (validates JWT, builds JwtAuthenticationToken)
→ UserProvisioningFilter:
    no JwtAuthenticationToken in SecurityContext? → skip (pass through)
    otherwise: user = upsert(jwt); request.setAttribute("currentUser", user)
→ downstream handlers can read the provisioned User
```

Claim mapping:

- `id` = `sub`
- `name` = `preferred_username` (always present in default Keycloak tokens)
- `email` = `email`

Upsert logic: find by id → insert if missing / update fields only if changed / return existing
untouched otherwise.

Entity changes required by implementation (found via tests): targeted Lombok `@Setter` on
`User.name`/`User.email` so the sync can update them, and collection fields typed as `List`
instead of concrete `ArrayList` — Hibernate injects `PersistentBag` implementations, which
cannot be assigned to an `ArrayList`-typed field, so users could not be loaded at all before.

## Error handling

- **Missing/unparseable `sub`:** log a warning and skip provisioning; the request continues.
  Authentication remains valid at the IdP level.
- **Concurrent first-login race** (two parallel requests for an unknown user): catch
  `DataIntegrityViolationException` from the insert, re-fetch by id, return the winner's row.
- **Database failure during upsert:** let the exception propagate (500). Fail closed rather than
  serving requests with unprovisioned identity.

## Testing

- Service unit tests: creates new user; updates changed name/email; no-op when unchanged;
  race path re-fetches instead of throwing.
- Filter unit tests (mocked service): skips when not authenticated with a JWT; sets request
  attribute when provisioning succeeds.
- Integration test: `@SpringBootTest` + MockMvc with `.jwt()` post-processor and a minimal
  test-only controller; asserts one authenticated request results in a `users` row.
