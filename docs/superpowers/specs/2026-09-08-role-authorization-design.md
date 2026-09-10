# Design: Role-based Authorization (Organizer / Staff / Attendee)

Date: 2026-09-08
Status: Approved

## Problem

Authenticated users (Keycloak JWTs) can all reach every endpoint that isn't public. Event
management endpoints only restrict by *ownership* (`EventService.findBy…OrganizerId`), never by
*role*, so any authenticated user could call `POST/PUT/DELETE /api/v1/events`. Ticket purchase is
open to any authenticated user. We need coarse role rules enforced centrally plus method-level
checks as defense-in-depth.

## Decisions

- **Roles live in Keycloak, not the DB.** Realm roles `ROLE_ORGANIZER`, `ROLE_STAFF`, `ROLE_ATTENDEE`
  are assigned to users in Keycloak and arrive in the JWT under `realm_access.roles`. This keeps the
  existing JIT-provisioning decision ("no local roles") intact — the `users` row stays identity-only.
- **Custom `JwtAuthenticationConverter`.** Spring's default converter only maps `scope`/`scp` claims
  to `SCOPE_*` authorities and never reads `realm_access.roles`. A `JwtAuthoritiesConverter`
  extracts `realm_access.roles` (fallback: `roles` claim) and emits each entry as a
  `SimpleGrantedAuthority`, prefixing `ROLE_` only when absent. `hasRole("ORGANIZER")` etc. then work
  whether the Keycloak role is named `ROLE_ORGANIZER` or `organizer`.
- **URL rules first, method security second.** `SecurityFilterChain` matchers gate whole route
  groups; `@PreAuthorize` on controller methods re-checks the same role in depth. The per-event
  `organizer_id` ownership checks in `EventService` remain as object-level authorization.
- **`ROLE_STAFF` currently has no endpoints** — reserved for future ticket-validation/check-in
  flows; it is simply rejected from event management and ticket purchase.

## Access matrix

| Route | Access |
|---|---|
| `/error` | permitAll |
| `GET /api/v1/events/public`, `/api/v1/events/public/**` | permitAll |
| `/api/v1/events/**` (create, list own, get own, update, delete) | `hasRole('ORGANIZER')` |
| `POST /api/v1/tickets/purchase` | `hasRole('ATTENDEE')` |
| anything else | authenticated |

Authorization failures produce standard Spring Security responses (401 unauthenticated,
403 forbidden); no handler changes are needed.

## Architecture

| Unit | Package | Purpose |
|---|---|---|
| `JwtAuthoritiesConverter` | `com.example.tickets.config` | JWT `realm_access.roles` → `ROLE_*` `GrantedAuthority`s |
| `SecurityConfig` | `com.example.tickets.config` | Registers converter bean, URL rules, `@EnableMethodSecurity` |
| `EventController` / `TicketController` | `com.example.tickets.controller` | `@PreAuthorize` per management/purchase method |

## Testing

- `JwtAuthoritiesConverterTest`: maps realm roles verbatim; prefixes missing `ROLE_`; falls back to
  the `roles` claim; empty when no role claims.
- `RoleAuthorizationIntegrationTest` (`@SpringBootTest` + MockMvc): organizer can create/list own
  events; attendee/staff/role-less/anon cannot; attendee passes the purchase gate; organizer cannot
  purchase; public event browsing stays anonymous-accessible.