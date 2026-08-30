# Delete Event Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `DELETE /api/v1/events/{id}` that hard-deletes an event and its aggregate (ticket types → tickets → QR codes → validations), scoped to the organizer, returning 204.

**Architecture:** Thin controller → transactional service → repository. A `@DeleteMapping` reads the `currentUser` attribute, calls `eventService.delete(id, organizerId)` which reuses `getById` (enforcing scoping + 404 via `NoSuchElementException`) then `eventRepository.delete(event)`. Cascade is enabled by changing `Event.ticketTypes` cascade from `PERSIST` to `ALL`.

**Tech Stack:** Spring Boot 4.1, Java 21, records, Lombok, Maven wrapper.

---

### Task 1: Branch

- [ ] **Step 1: Create and switch branch**

```bash
git checkout -b feat/delete-event
```

---

### Task 2: Spec and plan documents

**Files:**
- Create: `docs/superpowers/specs/2026-08-31-delete-event-design.md`
- Create: `docs/superpowers/plans/2026-08-31-delete-event.md`

- [ ] **Step 1: Write the spec doc** — problem statement, endpoint contract (204/404), hard-delete cascade decision, cascade wiring change, files touched.
- [ ] **Step 2: Save this plan** to the path above.
- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-08-31-delete-event-design.md docs/superpowers/plans/2026-08-31-delete-event.md
git commit -m "docs(spec): add delete event design"
```

---

### Task 3: Enable cascade delete on the event aggregate

**Files:**
- Modify: `src/main/java/com/example/tickets/domain/Event.java`

- [ ] **Step 1: Change the `ticketTypes` cascade** from `CascadeType.PERSIST` to `CascadeType.ALL`

```java
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TicketType> ticketTypes = new ArrayList<>();
```

- [ ] **Step 2: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/tickets/domain/Event.java && git commit -m "feat(api): cascade delete event aggregate"
```

---

### Task 4: Service delete method

**Files:**
- Modify: `src/main/java/com/example/tickets/service/EventService.java`

- [ ] **Step 1: Add `delete` after `getById`**

```java
    @Transactional
    public void delete(UUID id, UUID organizerId) {
        Event event = getById(id, organizerId);
        eventRepository.delete(event);
    }
```

- [ ] **Step 2: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/tickets/service/EventService.java && git commit -m "feat(api): delete event by id"
```

---

### Task 5: Controller delete endpoint

**Files:**
- Modify: `src/main/java/com/example/tickets/controller/EventController.java`

- [ ] **Step 1: Add `DeleteMapping` import** and the endpoint method

Import: `org.springframework.web.bind.annotation.DeleteMapping`

```java
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        eventService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 2: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/tickets/controller/EventController.java && git commit -m "feat(api): add delete event endpoint"
```

---

### Task 6: Regression check

- [ ] **Step 1: Run full suite**

Run: `./mvnw test`
Expected: all existing suites pass (behavior unchanged); BUILD SUCCESS
