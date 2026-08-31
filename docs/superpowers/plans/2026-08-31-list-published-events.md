# List Published Events Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a public `GET /api/v1/events/public` endpoint listing all `PUBLISHED` events (paginated, default `startDate asc`), accessible without authentication.

**Architecture:** Thin controller → read-only transactional service → repository. A new `@GetMapping("/public")` on `EventController` (does not read `currentUser`) calls `eventService.listPublished(pageable)`, which queries `eventRepository.findAllByStatus(PUBLISHED, pageable)`. `SecurityConfig` adds a `permitAll()` matcher for this path before the catch-all authenticated rule.

**Tech Stack:** Spring Boot 4.1, Java 21, records, Lombok, Maven wrapper.

---

### Task 1: Branch

- [ ] **Step 1: Create and switch branch**

```bash
git checkout -b feat/list-published-events
```

---

### Task 2: Spec and plan documents

**Files:**
- Create: `docs/superpowers/specs/2026-08-31-list-published-events-design.md`
- Create: `docs/superpowers/plans/2026-08-31-list-published-events.md`

- [ ] **Step 1: Write the spec doc** — problem statement, public-catalog decision, path `/api/v1/events/public` + `permitAll`, endpoint contract table, components, files touched.
- [ ] **Step 2: Save this plan** to the path above.
- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-08-31-list-published-events-design.md docs/superpowers/plans/2026-08-31-list-published-events.md
git commit -m "docs(spec): add list published events design"
```

---

### Task 3: Repository + service

**Files:**
- Modify: `src/main/java/com/example/tickets/repository/EventRepository.java`
- Modify: `src/main/java/com/example/tickets/service/EventService.java`

- [ ] **Step 1: Add the derived query to `EventRepository`** (import `com.example.tickets.domain.EventStatusEnum`)

```java
    Page<Event> findAllByStatus(EventStatusEnum status, Pageable pageable);
```

- [ ] **Step 2: Add `listPublished` to `EventService`** after `list`

```java
    @Transactional(readOnly = true)
    public Page<Event> listPublished(Pageable pageable) {
        return eventRepository.findAllByStatus(EventStatusEnum.PUBLISHED, pageable);
    }
```

(`EventStatusEnum` is already imported in `EventService`.)

- [ ] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/tickets/repository/EventRepository.java src/main/java/com/example/tickets/service/EventService.java
git commit -m "feat(api): query published events by status"
```

---

### Task 4: Controller + security

**Files:**
- Modify: `src/main/java/com/example/tickets/controller/EventController.java`
- Modify: `src/main/java/com/example/tickets/config/SecurityConfig.java`

- [ ] **Step 1: Add the public endpoint to `EventController`** after `list`

```java
    @GetMapping("/public")
    public ResponseEntity<PageResponse<EventResponse>> listPublished(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "startDate") String sort,
        @RequestParam(defaultValue = "asc") String direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new IllegalArgumentException(
                "sort must be one of: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Event> events = eventService.listPublished(pageable);
        return ResponseEntity.ok(eventMapper.toPageResponse(events));
    }
```

(No `HttpServletRequest`/`currentUser` read — this is public.)

- [ ] **Step 2: Add the `permitAll` matcher to `SecurityConfig`** before the catch-all `anyRequest()`

```java
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/events/public").permitAll()
                .anyRequest().authenticated())
```

- [ ] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/tickets/controller/EventController.java src/main/java/com/example/tickets/config/SecurityConfig.java
git commit -m "feat(api): expose public published events endpoint"
```

---

### Task 5: Regression check

- [ ] **Step 1: Run full suite**

Run: `./mvnw test`
Expected: all existing suites pass (behavior unchanged); BUILD SUCCESS
