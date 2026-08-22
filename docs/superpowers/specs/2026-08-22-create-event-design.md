# Design: Create Event with Nested Ticket Types

Date: 2026-08-22
Status: Approved
Branch: `feat/create-event`

## Problem

Organizers need to create events together with their ticket types in a single request. The API has
no web layer yet; this feature establishes the controller/service/DTO/mapper patterns the rest of
the API will follow.

## Decisions

- **Authorization:** any authenticated user may create events; the just-in-time provisioned
  `currentUser` is the organizer. Role checks are deferred.
- **Initial status:** client chooses; only `DRAFT` and `PUBLISHED` accepted. Omitted → `DRAFT`.
- **Validation:** strict — field-level Bean Validation plus cross-field business rules enforced in
  the service layer (single enforcement point).
- **Response:** `201 Created`, `Location: /api/v1/events/{id}`, full `EventResponse` body.
- **Layering:** thin controller → transactional service → repositories. DTOs are Java records.
  MapStruct (`componentModel = "spring"`) maps DTO↔entity both ways. Entities never leave the
  service layer boundary except as mapped responses.

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
| web | `controller.EventController` | `@Valid @RequestBody`; reads `currentUser` request attribute; returns 201 + Location |
| dto | `dto.request.CreateEventRequest`, `CreateTicketTypeRequest` | records with Jakarta validation annotations |
| dto | `dto.response.EventResponse`, `TicketTypeResponse` | records |
| mapping | `mapper.EventMapper` | MapStruct: request→entities, entities→response |
| service | `service.EventService` | `@Transactional create(organizerId, request)` — business rules, organizer reload, aggregate save |
| data | `repository.EventRepository`, `TicketTypeRepository` | JPA interfaces |
| errors | `exception.BusinessRuleException`, `api.GlobalExceptionHandler` | RFC 7807 problem+json responses |

### Entity adjustments (required by implementation)

- `CascadeType.PERSIST` on `Event.ticketTypes` so one `save(event)` persists the aggregate.
- Targeted Lombok `@Setter` on `Event.organizer`, `Event.status`, and `TicketType.event`
  (same pattern as `User.name/email`) so the service can complete bidirectional references after
  mapping.
- Service re-loads the organizer by id inside its transaction instead of trusting the detached
  instance from the provisioning filter.

## Validation

Field-level (annotations): `name`/`venue` `@NotBlank`; all datetimes `@NotNull`;
`price` `@DecimalMin("0.0")` (free ticket types allowed); `totalAvailable` `@Positive`;
`ticketTypes` `@NotEmpty @Valid`.

Cross-field (service, throws `BusinessRuleException`):

1. `endDate` strictly after `startDate`
2. `salesEnd` strictly after `salesStart`, and sales must close by event start (`salesEnd <= startDate`)
3. Ticket-type names unique within the request (case-insensitive)
4. Initial status limited to `DRAFT`/`PUBLISHED`

## Error handling

All errors use Spring's `ProblemDetail` (`application/problem+json`):

- Bean validation failures → **400**, field error list
- Malformed JSON / unknown enum value → **400**
- `BusinessRuleException` → **422**
- Missing `currentUser` attribute (defensive; filter guarantees it) → **500**

## Data flow

JWT auth → provisioning filter creates/syncs user + sets attribute → controller extracts
organizer id → mapper builds entities from request → service validates rules, re-loads organizer,
sets status/organizer/back-references → single `save(event)` (cascade) → mapper builds response →
201 + Location.

## Testing

- `EventServiceTest`: unit (Mockito repositories, real generated MapStruct impl); one test per
  business rule plus happy path asserting persisted values.
- `EventControllerTest`: `@WebMvcTest` slice with mocked service; contract tests for 201 +
  Location + body JSON, 400 field violations, 422 business-rule propagation.
- `CreateEventIntegrationTest`: `@SpringBootTest` + MockMvc `.jwt()`; asserts rows in
  `events`/`ticket_types` and the response body end-to-end through the real provisioning filter.

New dependency: `spring-boot-starter-validation`.
