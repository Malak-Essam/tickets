# Design: Ticket Purchase

Date: 2026-09-06
Status: Approved
Branch: `feat/purchase-tickets`

## Problem

Attendees can browse published events and their ticket types, but there is no way to buy
tickets. The platform needs a purchase feature: callers supply a ticket type (and quantity),
the system reserves availability atomically, creates the tickets, and returns a QR code per
purchased ticket. No payment service integration in this iteration. The core risk is a race
condition — two concurrent purchases must not oversell a ticket type's `totalAvailable`.

## Decisions

- **Endpoint (authenticated):** `POST /api/v1/tickets/purchase`. Security requires no change:
  `anyRequest().authenticated()` already covers it. The caller is the `User` set on the request
  by `UserProvisioningFilter` (same pattern as `EventController`).
- **Request:** `ticketTypeId` (required `UUID`) + `quantity` (required `@Min(1)`). Optional
  quantity is rejected; batch purchase is supported and requested.
- **Response:** `200 OK` with a `PurchaseTicketsResponse` containing one `PurchasedTicketResponse`
  per ticket (ticketId, ticketTypeId, ticketTypeName, price, base64 PNG `qrCode`). `201` is not
  used because a batch creates many resources with no single `Location`.
- **Race condition:** `TicketTypeRepository.getTicketTypeByIdWithLock(UUID)` uses
  `@Lock(LockModeType.PESSIMISTIC_WRITE)` with a JPQL query, so the database acquires a row-level
  `SELECT … FOR UPDATE` on the ticket type. The availability check, decrement, ticket creation,
  and QR generation all execute inside one `@Transactional` service method. Concurrent purchases
  on the same ticket type serialize on that row lock; after the first commits, the second reads
  the decremented availability and fails with sold-out when exhausted. Pessimistic locking was
  chosen over an atomic conditional `UPDATE` (because the requested `getTicketTypeByIdWithLock`
  DB-lock approach was preferred) and over optimistic `@Version` (would add a version column and
  retry logic, and is not a DB-level lock).
- **Availability-only validation:** no event status or sales-window checks in this iteration
  (explicitly requested). A purchase only fails when the requested quantity exceeds remaining
  `totalAvailable`.
- **Sold out error:** new `TicketSoldOutException` mapped in `GlobalExceptionHandler` to
  `409 Conflict` with a `ProblemDetail`. Ticket type not found maps to `404` via the existing
  `NoSuchElementException` handler.
- **Decrement:** `TicketType.totalAvailable` gains a per-field `@Setter` (consistent with how
  `Event` exposes mutable fields). The service checks availability then decrements under the lock.
  Total availability must never go negative: the sold-out check runs while holding the lock.
- **Purchaser:** the service re-fetches the `User` by id inside its own transaction (`EventService`
  pattern) rather than trusting the detached `User` from the request attribute, to avoid
  `detached entity` issues when saving the `Ticket`.
- **QR codes:** one per ticket via the existing `QrCodeService.generate(Ticket)`. Each QR encodes
  its own id (`QrCode.value` is the base64 PNG). `ticket.getQrCodes()` is populated through the
  `Ticket` ↔ `QrCode` bi-directional mapping (ticket-side `qrCodes` collection + FK on `QrCode`).
- **Ticket status:** each created ticket is `PURCHASED` (`TicketStatusEnum`).
- **Layering:** thin controller (reads `currentUser` from the request attribute) → transactional
  service → repositories, consistent with the rest of the API.
- **Testing:** no new tests in this change (deferred per decision; existing suite is the regression
  gate). A concurrent-purchase integration test is the recommended follow-up to prove the lock
  prevents overselling.

## API contract

`POST /api/v1/tickets/purchase` — requires authentication.

**Request body**
```json
{ "ticketTypeId": "…", "quantity": 2 }
```

**Response body (200)**
```json
{
  "tickets": [
    { "ticketId": "…", "ticketTypeId": "…", "ticketTypeName": "General", "price": 50.00, "qrCode": "<base64 PNG>" }
  ]
}
```

| Status | Condition |
|---|---|
| 200 | `quantity` tickets created, totalAvailable decremented by `quantity`, one QR per ticket returned |
| 400 | invalid body (missing/invalid `ticketTypeId` or `quantity < 1`) — existing validation handler |
| 404 | no ticket type with that id — existing `NoSuchElementException` handler |
| 409 | `quantity` exceeds remaining `totalAvailable` — new `TicketSoldOutException` handler |

## Components

| Layer | Unit | Responsibility |
|---|---|---|
| web | `controller.TicketController` | `@PostMapping("/api/v1/tickets/purchase")`, reads `currentUser`, validates, returns 200 |
| service | `service.TicketService` | `@Transactional purchase(UUID purchaserId, PurchaseTicketsRequest)` — lock, check, decrement, create tickets, generate QRs, assemble response |
| data | `repository.TicketTypeRepository` | `@Lock(PESSIMISTIC_WRITE) getTicketTypeByIdWithLock(UUID)` — `SELECT … FOR UPDATE` |
| data | `repository.TicketRepository` | `JpaRepository<Ticket, UUID>` |
| data | `domain.TicketType` | per-field `@Setter` on `totalAvailable` |
| dto | `dto.request.PurchaseTicketsRequest` | `ticketTypeId`, `quantity` (validated) |
| dto | `dto.response.PurchasedTicketResponse` | one purchased ticket + its base64 QR |
| dto | `dto.response.PurchaseTicketsResponse` | list of purchased tickets |
| exception | `exception.TicketSoldOutException` | sold-out signal |
| exception | `exception.GlobalExceptionHandler` | 409 handler for `TicketSoldOutException` |

## Files

| Action | Path |
|---|---|
| create | `docs/superpowers/specs/2026-09-06-purchase-tickets-design.md` |
| create | `docs/superpowers/plans/2026-09-06-purchase-tickets.md` |
| create | `repository/TicketRepository.java` |
| modify | `repository/TicketTypeRepository.java` (add lock query) |
| modify | `domain/TicketType.java` (add `@Setter` on `totalAvailable`) |
| create | `service/TicketService.java` |
| create | `controller/TicketController.java` |
| create | `dto/request/PurchaseTicketsRequest.java` |
| create | `dto/response/PurchasedTicketResponse.java` |
| create | `dto/response/PurchaseTicketsResponse.java` |
| create | `exception/TicketSoldOutException.java` |
| modify | `exception/GlobalExceptionHandler.java` (sold-out handler) |

No tests added in this change; regression gate is the existing suite.