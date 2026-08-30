# Design: Delete Event

Date: 2026-08-31
Status: Approved
Branch: `feat/delete-event`

## Problem

Organizers need to remove an event. The API has create, list, get-by-id, and update for events but
no delete endpoint yet.

## Decisions

- **Endpoint:** `DELETE /api/v1/events/{id}` → `204 No Content` with an empty body.
- **Scoping:** the authenticated user's id (from the provisioning filter) scopes the lookup, matching
  `getById`/`update`. An event not found or not owned by the caller returns `404` via the existing
  `NoSuchElementException` handler — no new exception type.
- **Deletion is a hard delete that cascades the whole aggregate** (explicit user decision): the
  event, its ticket types, their tickets, QR codes, and ticket validations are all removed. The
  event's `attendees`/`staff` join rows are owned by the `Event` side and are removed automatically.
- **Cascade wiring:** change `Event.ticketTypes` from `CascadeType.PERSIST` to `CascadeType.ALL`.
  The existing `TicketType.tickets` `CascadeType.ALL` then cascades tickets → QR codes →
  validations. This treats ticket types as part of the event aggregate (mirroring the create side)
  and avoids imperative ordered deletes.
- **Layering:** thin controller → transactional service → repository, consistent with existing
  event methods. The service reuses `getById(id, organizerId)` for scoping + 404, then
  `eventRepository.delete(event)`.
- **No new tests** (matches the deferred-testing precedent set for create/update).

## API contract

```
DELETE /api/v1/events/{id}
```

| Status | Condition |
|---|---|
| 204 | event deleted |
| 404 | event not found or not owned by the caller |

No response body. Location/created semantics do not apply.

## Components

| Layer | Unit | Responsibility |
|---|---|---|
| web | `controller.EventController` | Reads `currentUser`; `@DeleteMapping("/{id}")` returns 204 |
| service | `service.EventService` | `@Transactional delete(id, organizerId)` — scoped load, then delete |
| domain | `domain.Event` | cascade `PERSIST` → `ALL` on `ticketTypes` |
| data | `repository.EventRepository` | inherited `delete(Event)` |

## Error handling

`NoSuchElementException` (event missing or not owned) already maps to `404 Not Found` through
`GlobalExceptionHandler`. No new handlers are required.

## Files

| Action | Path |
|---|---|
| create | `docs/superpowers/specs/2026-08-31-delete-event-design.md` |
| create | `docs/superpowers/plans/2026-08-31-delete-event.md` |
| modify | `domain/Event.java` (cascade on `ticketTypes`) |
| modify | `service/EventService.java` (add `delete`) |
| modify | `controller/EventController.java` (add `@DeleteMapping`) |

No tests added in this change; regression gate is the existing suite.
