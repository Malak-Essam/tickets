# Design: Get Published Event by ID

Date: 2026-09-01
Status: Approved
Branch: `feat/get-published-event`

## Problem

Attendees can list the public catalog via `GET /api/v1/events/public`, but there is no way to
fetch a single published event. The only get-by-id endpoint, `GET /api/v1/events/{id}`, is scoped
to the authenticated organizer and returns full organizer-facing details (sales windows, status,
organizer id, and ticket availability) that are not appropriate for the public.

## Decisions

- **Public get-by-id:** a new endpoint returns a single `PUBLISHED` event by id, accessible without
  authentication. It is **not** scoped to the caller; it exposes only publish-safe fields.
- **Path & auth:** `GET /api/v1/events/public/{id}`, added to `SecurityConfig` as `permitAll()`.
  The literal `public` segment already resolves over the `{id}` path variable, and `{id}` is a
  UUID, so there is no conflict with the existing `/api/v1/events/public` list matcher (which is
  exact-match). A new matcher `/api/v1/events/public/*` is required for the id subpath.
- **Visibility rule:** returns the event **only if** `status = PUBLISHED`. If no event with that id
  is PUBLISHED (not found, or exists but not published), return 404 via `NoSuchElementException`,
  consistent with the organizer-scoped `getById`.
- **Response differs from organizer `getById`:** a new `PublishedEventResponse` omits `salesStart`,
  `salesEnd`, `status`, and `organizerId`. Each ticket type uses a new `PublishedTicketTypeResponse`
  that omits `totalAvailable`. Two new MapStruct mappings are added to `EventMapper`.
- **Layering:** thin controller → read-only transactional service → repository, consistent with
  existing event methods.
- **No new tests** (matches the deferred-testing precedent set for the rest of the event API).

## API contract

`GET /api/v1/events/public/{id}` — public, no authentication required.

| Status | Condition |
|---|---|
| 200 | `PublishedEventResponse` for the event with `status = PUBLISHED` |
| 404 | no event with that id is PUBLISHED |

The single-event response is the only consumer of `PublishedEventResponse`; the list endpoint
still returns `PageResponse<EventResponse>`.

## Components

| Layer | Unit | Responsibility |
|---|---|---|
| web | `controller.EventController` | `@GetMapping("/public/{id}")` — returns 200 + `PublishedEventResponse`; does not read `currentUser` |
| security | `config.SecurityConfig` | `permitAll()` for `/api/v1/events/public/*` |
| service | `service.EventService` | `@Transactional(readOnly = true) getPublishedEvent(UUID id)` |
| data | `repository.EventRepository` | `Optional<Event> findByIdAndStatus(UUID, EventStatusEnum)` |
| dto | `dto.response.PublishedEventResponse` | public event view (no sales/status/organizer) |
| dto | `dto.response.PublishedTicketTypeResponse` | public ticket type view (no totalAvailable) |
| mapper | `mapper.EventMapper` | `toPublishedResponse(Event)` and `toPublishedResponse(TicketType)` |

## Files

| Action | Path |
|---|---|
| create | `docs/superpowers/specs/2026-09-01-get-published-event-design.md` |
| create | `docs/superpowers/plans/2026-09-01-get-published-event.md` |
| create | `dto/response/PublishedEventResponse.java` |
| create | `dto/response/PublishedTicketTypeResponse.java` |
| modify | `repository/EventRepository.java` (add `findByIdAndStatus`) |
| modify | `service/EventService.java` (add `getPublishedEvent`) |
| modify | `mapper/EventMapper.java` (add published response mappings) |
| modify | `controller/EventController.java` (add `@GetMapping("/public/{id}")`) |
| modify | `config/SecurityConfig.java` (permitAll matcher) |

No tests added in this change; regression gate is the existing suite.
