# Design: List Published Events

Date: 2026-08-31
Status: Approved
Branch: `feat/list-published-events`

## Problem

Attendees need a public, browsable catalog of published events. The only list endpoint today,
`GET /api/v1/events`, is scoped to the authenticated organizer's own events. There is no way for
the general public to discover events that organizers have published.

## Decisions

- **Public catalog:** a new endpoint lists all events with `status = PUBLISHED`, regardless of
  date, paginated. It is **not** scoped to the caller (explicit user decision).
- **Path & auth:** `GET /api/v1/events/public`, added to `SecurityConfig` as `permitAll()`. The
  literal `public` segment resolves over the `{id}` path variable, so there is no route conflict.
  The existing organizer-scoped `GET /api/v1/events` is untouched.
- **Sorting:** defaults to `startDate asc` (soonest first) for a catalog feel. Allowed sort fields
  match the existing list: `name`, `createdAt`, `startDate`.
- **Response:** same `PageResponse<EventResponse>` shape and `EventMapper.toPageResponse` as the
  existing list, so the response contract is identical (includes ticket types).
- **Layering:** thin controller → transactional (read-only) service → repository, consistent with
  existing event methods.
- **No new tests** (matches the deferred-testing precedent set for the rest of the event API).

## API contract

`GET /api/v1/events/public` — public, no authentication required.

| Param | Default | Rules |
|---|---|---|
| `page` | 0 | `@Min(0)` |
| `size` | 20 | `@Min(1) @Max(100)` |
| `sort` | `startDate` | `name`, `createdAt`, `startDate` |
| `direction` | `asc` | `asc` / `desc` |

| Status | Condition |
|---|---|
| 200 | page of events with `status = PUBLISHED` |

Invalid `sort` returns 400 via the existing `IllegalArgumentException` handler.

## Components

| Layer | Unit | Responsibility |
|---|---|---|
| web | `controller.EventController` | `@GetMapping("/public")` — returns 200 + `PageResponse<EventResponse>`; does not read `currentUser` |
| security | `config.SecurityConfig` | `permitAll()` for `/api/v1/events/public` |
| service | `service.EventService` | `@Transactional(readOnly = true) listPublished(pageable)` |
| data | `repository.EventRepository` | `Page<Event> findAllByStatus(EventStatusEnum, Pageable)` |

No changes to `EventResponse`, `EventMapper`, or error handling.

## Files

| Action | Path |
|---|---|
| create | `docs/superpowers/specs/2026-08-31-list-published-events-design.md` |
| create | `docs/superpowers/plans/2026-08-31-list-published-events.md` |
| modify | `repository/EventRepository.java` (add `findAllByStatus`) |
| modify | `service/EventService.java` (add `listPublished`) |
| modify | `controller/EventController.java` (add `@GetMapping("/public")`) |
| modify | `config/SecurityConfig.java` (permitAll matcher) |

No tests added in this change; regression gate is the existing suite.
