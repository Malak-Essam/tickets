# Design: Create Event with Nested Ticket Types

Date: 2026-08-22
Status: Approved (scope revised 2026-08-23)
Branch: `feat/create-event`

## Problem

Organizers need to create events together with their ticket types in a single request. The API has
no web layer yet; this feature establishes the controller/service/DTO/mapper patterns the rest of
the API will follow.

## Scope revision

Validation (Bean Validation annotations and cross-field business rules) and feature tests are
explicitly **deferred** and will be added in follow-up work. The existing provisioning and context
suites must stay green.

## Decisions

- **Authorization:** any authenticated user may create events; the just-in-time provisioned
  `currentUser` is the organizer. Role checks are deferred.
- **Initial status:** client may send any `EventStatusEnum` value; omitted → `DRAFT`.
- **Response:** `201 Created`, `Location: /api/v1/events/{id}`, full `EventResponse` body.
- **Layering:** thin controller → transactional service → repositories. DTOs are Java records.
  MapStruct (`componentModel = "spring"`) maps DTO↔entity both ways.

## API contract

`POST /api/v1/events`

```json
{
  "name": "Summer Fest",
  "startDate": "2026-09-01T18:00:00",
  "endDate": "2026-09-01T23:00:00",
  "venue": "Central Park",
  "salesStart": "2026-08-01T10:00:00",
  "salesEnd": "2026-08-31T23:59:59",
  "status": "PUBLISHED",
  "ticketTypes": [
    { "name": "General", "price": 49.90, "totalAvailable": 500, "description": null }
  ]
}
```

`EventResponse`: `id`, `name`, `startDate`, `endDate`, `venue`, `salesStart`, `salesEnd`,
`status`, `organizerId`, `ticketTypes[]` (`id`, `name`, `price`, `totalAvailable`,
`description`).

## Components

| Layer | Unit | Responsibility |
|---|---|---|
| web | `controller.EventController` | Reads `currentUser` request attribute; returns 201 + Location |
| dto | `dto.request.CreateEventRequest`, `CreateTicketTypeRequest` | plain records |
| dto | `dto.response.EventResponse`, `TicketTypeResponse` | records |
| mapping | `mapper.EventMapper` | MapStruct: request→entities, entities→response |
| service | `service.EventService` | `@Transactional create(organizerId, request)` — organizer reload, aggregate save |
| data | `repository.EventRepository`, `TicketTypeRepository` | JPA interfaces |

### Entity adjustments

- `CascadeType.PERSIST` on `Event.ticketTypes` so one `save(event)` persists the aggregate.
- Targeted Lombok `@Setter` on `Event.organizer`, `Event.status`, and `TicketType.event`
  (same pattern as `User.name/email`) so the service can complete bidirectional references after
  mapping.
- Collection fields on `Event`/`TicketType` typed as `List` instead of concrete `ArrayList`
  (Hibernate injects `PersistentBag`; same fix as previously applied to `User`).
- Service re-loads the organizer by id inside its transaction instead of trusting the detached
  instance from the provisioning filter.

## Error handling

No custom handling yet; Boot's default error responses apply. The controller defensively throws
if the `currentUser` attribute is missing (filter guarantees it for authenticated requests).

## Dependencies

- Added `spring-boot-starter-jackson` — Boot 4 no longer ships Jackson with the web starter.
- `spring-boot-starter-validation` is intentionally absent until validation work resumes.
