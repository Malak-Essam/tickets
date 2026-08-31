# Get Published Event by ID Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a public `GET /api/v1/events/public/{id}` endpoint returning a single `PUBLISHED` event (and its ticket types) with publish-safe fields, accessible without authentication. Response differs from organizer `getById` via two new DTOs.

**Architecture:** Thin controller → read-only transactional service → repository. A new `@GetMapping("/public/{id}")` on `EventController` (does not read `currentUser`) calls `eventService.getPublishedEvent(id)`, which queries `eventRepository.findByIdAndStatus(id, PUBLISHED)` and throws `NoSuchElementException` when absent. `EventMapper` gains `toPublishedResponse(Event)` and `toPublishedResponse(TicketType)` mapping to new `PublishedEventResponse` / `PublishedTicketTypeResponse` DTOs. `SecurityConfig` adds a `permitAll()` matcher for `/api/v1/events/public/*`.

**Tech Stack:** Spring Boot 4.1, Java 21, records, Lombok, MapStruct, Maven wrapper.

---

### Task 1: Branch

- [ ] **Step 1: Create and switch branch**

```bash
git checkout -b feat/get-published-event
```

---

### Task 2: Spec and plan documents

**Files:**
- Create: `docs/superpowers/specs/2026-09-01-get-published-event-design.md`
- Create: `docs/superpowers/plans/2026-09-01-get-published-event.md`

- [ ] **Step 1: Write the spec doc** — problem statement, public get-by-id decision, path `/api/v1/events/public/{id}` + `permitAll`, visibility rule (PUBLISHED-only, 404 otherwise), response DTO differences, endpoint contract table, components, files touched.
- [ ] **Step 2: Save this plan** to the path above.
- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-09-01-get-published-event-design.md docs/superpowers/plans/2026-09-01-get-published-event.md
git commit -m "docs(spec): add get published event design"
```

---

### Task 3: Response DTOs

**Files:**
- Create: `src/main/java/com/example/tickets/dto/response/PublishedEventResponse.java`
- Create: `src/main/java/com/example/tickets/dto/response/PublishedTicketTypeResponse.java`

- [ ] **Step 1: Create `PublishedTicketTypeResponse`** (public view, no `totalAvailable`)

```java
package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PublishedTicketTypeResponse(
    UUID id,
    String name,
    BigDecimal price,
    String description) {
}
```

- [ ] **Step 2: Create `PublishedEventResponse`** (public view, no sales/status/organizer)

```java
package com.example.tickets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublishedEventResponse(
    UUID id,
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String venue,
    List<PublishedTicketTypeResponse> ticketTypes) {
}
```

---

### Task 4: Repository + service + mapper

**Files:**
- Modify: `src/main/java/com/example/tickets/repository/EventRepository.java`
- Modify: `src/main/java/com/example/tickets/service/EventService.java`
- Modify: `src/main/java/com/example/tickets/mapper/EventMapper.java`

- [ ] **Step 1: Add the derived query to `EventRepository`** (after `findByIdAndOrganizerId`)

```java
    Optional<Event> findByIdAndStatus(UUID id, EventStatusEnum status);
```

(`EventStatusEnum` and `Optional` are already imported.)

- [ ] **Step 2: Add `getPublishedEvent` to `EventService`** after `searchPublished`

```java
    @Transactional(readOnly = true)
    public Event getPublishedEvent(UUID id) {
        return eventRepository.findByIdAndStatus(id, EventStatusEnum.PUBLISHED)
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    }
```

- [ ] **Step 3: Add published mappings to `EventMapper`**

```java
    PublishedEventResponse toPublishedResponse(Event event);

    PublishedTicketTypeResponse toPublishedResponse(TicketType ticketType);
```

Add imports for `PublishedEventResponse` and `PublishedTicketTypeResponse`. MapStruct maps the `ticketTypes` collection (target element `PublishedTicketTypeResponse`) automatically from the `TicketType` source.

- [ ] **Step 4: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/tickets/dto/response/PublishedEventResponse.java src/main/java/com/example/tickets/dto/response/PublishedTicketTypeResponse.java src/main/java/com/example/tickets/repository/EventRepository.java src/main/java/com/example/tickets/service/EventService.java src/main/java/com/example/tickets/mapper/EventMapper.java
git commit -m "feat(api): add published event response dto and lookup"
```

---

### Task 5: Controller + security

**Files:**
- Modify: `src/main/java/com/example/tickets/controller/EventController.java`
- Modify: `src/main/java/com/example/tickets/config/SecurityConfig.java`

- [ ] **Step 1: Add the public endpoint to `EventController`** after `listPublished`

```java
    @GetMapping("/public/{id}")
    public ResponseEntity<PublishedEventResponse> getPublishedById(@PathVariable UUID id) {
        Event event = eventService.getPublishedEvent(id);
        return ResponseEntity.ok(eventMapper.toPublishedResponse(event));
    }
```

(No `HttpServletRequest`/`currentUser` read — this is public.)

- [ ] **Step 2: Add the `permitAll` matcher to `SecurityConfig`** before the catch-all `anyRequest()`

```java
                .requestMatchers("/api/v1/events/public").permitAll()
                .requestMatchers("/api/v1/events/public/*").permitAll()
                .anyRequest().authenticated())
```

- [ ] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/tickets/controller/EventController.java src/main/java/com/example/tickets/config/SecurityConfig.java
git commit -m "feat(api): expose public get published event endpoint"
```

---

### Task 6: Regression check

- [ ] **Step 1: Run full suite**

Run: `./mvnw test`
Expected: all existing suites pass (behavior unchanged); BUILD SUCCESS
