# Role-Based Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce Keycloak realm roles (`ROLE_ORGANIZER`, `ROLE_STAFF`, `ROLE_ATTENDEE`) on API access. Organizer manages event endpoints, attendee buys tickets, staff has no endpoints yet. Roles stay in the JWT; no DB schema change.

**Architecture:** `JwtAuthoritiesConverter` (reads `realm_access.roles`, normalizes to `ROLE_*`) wired into `SecurityConfig` as a `JwtAuthenticationConverter` bean + URL rules + `@EnableMethodSecurity`; `@PreAuthorize` on management/purchase methods as defense-in-depth. Existing `EventService` ownership checks remain object-level authorization.

**Tech Stack:** Spring Boot 4.1 (Spring Security 6.x OAuth2 resource server), Java 21, Keycloak realm roles.

---

### Task 1: Branch

- [x] **Step 1: Create and switch branch**

```bash
git checkout -b feat/role-authorization
```

---

### Task 2: Authorities converter

**Files:**
- Create: `src/main/java/com/example/tickets/config/JwtAuthoritiesConverter.java`

- [x] **Step 1: Add the converter** — `Converter<Jwt, Collection<GrantedAuthority>>` reading `realm_access.roles` (fallback claim `roles`); each entry becomes a `SimpleGrantedAuthority`, prefixed with `ROLE_` only if absent.

- [x] **Step 2: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

---

### Task 3: Security configuration

**Files:**
- Modify: `src/main/java/com/example/tickets/config/SecurityConfig.java`

- [x] **Step 1:** Add `@EnableMethodSecurity`, a `JwtAuthenticationConverter` bean delegating to `JwtAuthoritiesConverter`, and wire it via `resourceServer.jwt(...).jwtAuthenticationConverter(...)`.
- [x] **Step 2:** URL rules in order: `/error` permitAll; `GET /api/v1/events/public` + `/public/**` permitAll; `/api/v1/events/**` `hasRole("ORGANIZER")`; `POST /api/v1/tickets/purchase` `hasRole("ATTENDEE")`; `anyRequest().authenticated()`.

- [x] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

---

### Task 4: Method security

**Files:**
- Modify: `src/main/java/com/example/tickets/controller/EventController.java`
- Modify: `src/main/java/com/example/tickets/controller/TicketController.java`

- [x] **Step 1:** `@PreAuthorize("hasRole('ORGANIZER')")` on `create`, `list`, `getById`, `update`, `delete` (not on the `/public` methods, which stay public).
- [x] **Step 2:** `@PreAuthorize("hasRole('ATTENDEE')")` on `TicketController.purchase`.

- [x] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

---

### Task 5: Tests

**Files:**
- Create: `src/test/java/com/example/tickets/config/JwtAuthoritiesConverterTest.java`
- Create: `src/test/java/com/example/tickets/RoleAuthorizationIntegrationTest.java`

- [x] **Step 1: Converter unit tests** — verbatim realm roles; prefix-if-missing; `roles` fallback; no role claims → empty.
- [x] **Step 2: Integration tests** (`@SpringBootTest` + MockMvc, distinct email per subject to satisfy the `users.email` unique constraint):
  - anonymous browses `/events/public` (200) and gets 404 (not 401) on an unknown public event;
  - unauthenticated → 401 on `/events`;
  - role-less / attendee / staff → 403 on `POST /events`;
  - organizer → 201 create + 200 list;
  - organizer → 403 on purchase; attendee reaches purchase (service returns 404 for unknown ticket type).

- [x] **Step 3: Run suite**

Run: `./mvnw test`
Expected: all suites pass (24 tests); BUILD SUCCESS.

---

### Task 6: Docs

**Files:**
- Create: `docs/superpowers/specs/2026-09-08-role-authorization-design.md`
- Create: `docs/superpowers/plans/2026-09-08-role-authorization.md`

- [x] **Step 1: Write the spec doc** — problem, decisions (Keycloak roles, custom converter, URL + method security, STAFF reserved), access matrix, architecture, testing.
- [x] **Step 2: Save this plan** to the path above.

---

### Task 7: Verification

- [x] **Step 1: Run full suite**

Run: `./mvnw test`
Expected: `Tests run: 24, Failures: 0, Errors: 0, BUILD SUCCESS`.