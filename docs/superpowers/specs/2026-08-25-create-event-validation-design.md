# Design: Create Event Validation

Date: 2026-08-25
Status: Approved
Branch: `feat/create-event`

## Problem

Validation for `POST /api/v1/events` was explicitly deferred when the feature was built
(`8d68170`). Invalid requests currently fail late and ugly: missing fields surface as Hibernate
constraint violations (HTTP 500), a missing `ticketTypes` list throws an NPE in the service,
and there is no error contract beyond Boot's default JSON.

## Scope

Validation only. Feature tests remain deferred (explicit user decision); existing suites must
stay green.

## Decisions

- **Field-level rules** live as Bean Validation annotations on the request records; enabled via
  `@Valid` at the controller and `spring-boot-starter-validation`.
- **Cross-field date rules** live in `EventService.create()` (Approach B): explicit checks that
  throw a dedicated `InvalidEventDatesException`. Chosen over a class-level custom constraint to
  keep validation logic plainly readable; field rules stay declarative.
- **Error contract:** new `GlobalExceptionHandler` (`@RestControllerAdvice`) maps failures to RFC
  9457 ProblemDetail responses. This becomes the API-wide error format going forward.
- **Status stays client-settable:** any `EventStatusEnum` value is accepted; omitted → `DRAFT`
  (unchanged from the original design).

## Validation rules

| Request | Field | Rules |
|---|---|---|
| `CreateEventRequest` | `name`, `venue` | `@NotBlank`, `@Size(max = 255)` |
| | `startDate`, `endDate`, `salesStart`, `salesEnd` | `@NotNull` |
| | `ticketTypes` | `@NotEmpty`; elements cascaded via `List<@Valid ...>` |
| | `status` | none (optional) |
| `CreateTicketTypeRequest` | `name` | `@NotBlank`, `@Size(max = 255)` |
| | `price`, `totalAvailable` | `@NotNull`, `@Positive` |
| | `description` | none (nullable) |

Cross-field date rules (service-side, fail fast before any persistence):

- `endDate` > `startDate`
- `salesEnd` > `salesStart`
- `salesStart` ≤ `startDate`

## Error handling

All handled exceptions return `application/problem+json`.

| Exception | Status | Body extras |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `errors`: map of field path → message (e.g. `ticketTypes[0].price`) |
| `InvalidEventDatesException` | 400 | violated rule in `detail` |
| `HttpMessageNotReadableException` | 400 | malformed JSON / unknown enum value |
| `IllegalStateException` | 500 | generic detail (organizer missing / filter failure) |

Example 400:

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/events",
  "errors": { "name": "must not be blank" }
}
```

Valid requests are unaffected: still `201 Created` + `Location: /api/v1/events/{id}`.

## Dependencies

- Added `spring-boot-starter-validation` (was intentionally absent until this work resumed).

## Files

| Action | Path |
|---|---|
| modify | `pom.xml` |
| modify | `dto/request/CreateEventRequest.java`, `dto/request/CreateTicketTypeRequest.java` |
| modify | `controller/EventController.java` (`@Valid`) |
| create | `exception/InvalidEventDatesException.java` |
| create | `exception/GlobalExceptionHandler.java` |
| modify | `service/EventService.java` (date checks) |

No tests added in this change; regression gate is the existing suite.
