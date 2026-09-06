# Ticket Purchase Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an authenticated `POST /api/v1/tickets/purchase` endpoint that atomically reserves tickets from a type under a DB row lock, creates one `PURCHASED` `Ticket` per requested unit, decrements `totalAvailable`, returns a base64 PNG QR code per ticket, and answers `409 Conflict` when sold out.

**Architecture:** Thin controller reading `currentUser` from the request attribute → `@Transactional` service → repositories. `TicketTypeRepository.getTicketTypeByIdWithLock(UUID)` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` so each purchase takes a `SELECT … FOR UPDATE` row lock; check, decrement, ticket creation, and QR generation all happen in that transaction, serializing concurrent purchases on the ticket type row. Errors use the existing `ProblemDetail` `GlobalExceptionHandler` plus a new `TicketSoldOutException` → 409.

**Tech Stack:** Spring Boot 4.1, Java 21, records, Lombok, MapStruct, ZXing (existing `QrCodeService`), Maven wrapper.

---

### Task 1: Branch

- [ ] **Step 1: Create and switch branch**

```bash
git checkout -b feat/purchase-tickets
```

---

### Task 2: Spec and plan documents

**Files:**
- Create: `docs/superpowers/specs/2026-09-06-purchase-tickets-design.md`
- Create: `docs/superpowers/plans/2026-09-06-purchase-tickets.md`

- [ ] **Step 1: Write the spec doc** — problem statement, endpoint `POST /api/v1/tickets/purchase`, batch quantity support, pessimistic-lock race-condition decision, availability-only validation, sold-out → 409 decision, request/response shapes, error contract table, components, files touched.
- [ ] **Step 2: Save this plan** to the path above.
- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-09-06-purchase-tickets-design.md docs/superpowers/plans/2026-09-06-purchase-tickets.md
git commit -m "docs(spec): add ticket purchase design"
```

---

### Task 3: Data layer (lock query, setter, repository)

**Files:**
- Create: `src/main/java/com/example/tickets/repository/TicketRepository.java`
- Modify: `src/main/java/com/example/tickets/repository/TicketTypeRepository.java`
- Modify: `src/main/java/com/example/tickets/domain/TicketType.java`

- [ ] **Step 1: Create `TicketRepository`**

```java
package com.example.tickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tickets.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
}
```

- [ ] **Step 2: Add the lock query to `TicketTypeRepository`** (after the existing contents)

```java
package com.example.tickets.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.TicketType;

import jakarta.persistence.LockModeType;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tt from TicketType tt where tt.id = :id")
    @Transactional
    Optional<TicketType> getTicketTypeByIdWithLock(@Param("id") UUID id);
}
```

(The `@Transactional` on the repository method lets the test transaction acquire and hold the lock.)
Compile before committing by running `./mvnw -q compile`.

- [ ] **Step 3: Add `@Setter` on `totalAvailable` in `TicketType`** (field already has `@Column(nullable = false)`; add the Lombok annotation `@Setter` above the `totalAvailable` declaration, matching the existing `@Setter private Event event;` style)

```java
    @Setter
    @Column(nullable = false)
    private Integer totalAvailable;
```

Add `import lombok.Setter;` to the imports.

- [ ] **Step 4: Compile and commit**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

```bash
git add src/main/java/com/example/tickets/repository/TicketRepository.java src/main/java/com/example/tickets/repository/TicketTypeRepository.java src/main/java/com/example/tickets/domain/TicketType.java
git commit -m "feat(data): add pessimistic lock query for ticket type"
```

---

### Task 4: DTOs and exception

**Files:**
- Create: `src/main/java/com/example/tickets/dto/request/PurchaseTicketsRequest.java`
- Create: `src/main/java/com/example/tickets/dto/response/PurchasedTicketResponse.java`
- Create: `src/main/java/com/example/tickets/dto/response/PurchaseTicketsResponse.java`
- Create: `src/main/java/com/example/tickets/exception/TicketSoldOutException.java`

- [ ] **Step 1: Create `PurchaseTicketsRequest`**

```java
package com.example.tickets.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseTicketsRequest(
    @NotNull UUID ticketTypeId,
    @NotNull @Min(1) Integer quantity) {
}
```

- [ ] **Step 2: Create `PurchasedTicketResponse`**

```java
package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchasedTicketResponse(
    UUID ticketId,
    UUID ticketTypeId,
    String ticketTypeName,
    BigDecimal price,
    String qrCode) {
}
```

- [ ] **Step 3: Create `PurchaseTicketsResponse`**

```java
package com.example.tickets.dto.response;

import java.util.List;

public record PurchaseTicketsResponse(
    List<PurchasedTicketResponse> tickets) {
}
```

- [ ] **Step 4: Create `TicketSoldOutException`**

```java
package com.example.tickets.exception;

public class TicketSoldOutException extends RuntimeException {

    public TicketSoldOutException(String message) {
        super(message);
    }
}
```

- [ ] **Step 5: Compile and commit**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

```bash
git add src/main/java/com/example/tickets/dto/request/PurchaseTicketsRequest.java src/main/java/com/example/tickets/dto/response/PurchasedTicketResponse.java src/main/java/com/example/tickets/dto/response/PurchaseTicketsResponse.java src/main/java/com/example/tickets/exception/TicketSoldOutException.java
git commit -m "feat(api): add purchase request and response dtos"
```

---

### Task 5: Service

**Files:**
- Create: `src/main/java/com/example/tickets/service/TicketService.java`

- [ ] **Step 1: Create `TicketService`**

```java
package com.example.tickets.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.QrCode;
import com.example.tickets.domain.Ticket;
import com.example.tickets.domain.TicketStatusEnum;
import com.example.tickets.domain.TicketType;
import com.example.tickets.domain.User;
import com.example.tickets.dto.request.PurchaseTicketsRequest;
import com.example.tickets.dto.response.PurchaseTicketsResponse;
import com.example.tickets.dto.response.PurchasedTicketResponse;
import com.example.tickets.exception.TicketSoldOutException;
import com.example.tickets.repository.TicketRepository;
import com.example.tickets.repository.TicketTypeRepository;
import com.example.tickets.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    @Transactional
    public PurchaseTicketsResponse purchase(UUID purchaserId, PurchaseTicketsRequest request) {
        TicketType ticketType = ticketTypeRepository.getTicketTypeByIdWithLock(request.ticketTypeId())
            .orElseThrow(() -> new NoSuchElementException("Ticket type not found"));
        int quantity = request.quantity();
        if (ticketType.getTotalAvailable() < quantity) {
            throw new TicketSoldOutException(
                "Requested %d tickets but only %d remaining".formatted(quantity, ticketType.getTotalAvailable()));
        }
        ticketType.setTotalAvailable(ticketType.getTotalAvailable() - quantity);
        User purchaser = userRepository.findById(purchaserId)
            .orElseThrow(() -> new IllegalStateException("User %s does not exist".formatted(purchaserId)));

        List<PurchasedTicketResponse> responses = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Ticket ticket = ticketRepository.save(Ticket.builder()
                .status(TicketStatusEnum.PURCHASED)
                .ticketType(ticketType)
                .purchaser(purchaser)
                .build());
            QrCode qrCode = qrCodeService.generate(ticket);
            responses.add(new PurchasedTicketResponse(
                ticket.getId(),
                ticketType.getId(),
                ticketType.getName(),
                ticketType.getPrice(),
                qrCode.getValue()));
        }
        return new PurchaseTicketsResponse(responses);
    }
}
```

- [ ] **Step 2: Compile and commit**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

```bash
git add src/main/java/com/example/tickets/service/TicketService.java
git commit -m "feat(api): add ticket purchase service method"
```

---

### Task 6: Controller and error handler

**Files:**
- Create: `src/main/java/com/example/tickets/controller/TicketController.java`
- Modify: `src/main/java/com/example/tickets/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create `TicketController`**

```java
package com.example.tickets.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tickets.domain.User;
import com.example.tickets.dto.request.PurchaseTicketsRequest;
import com.example.tickets.dto.response.PurchaseTicketsResponse;
import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.service.TicketService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseTicketsResponse> purchase(@Valid @RequestBody PurchaseTicketsRequest request,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        return ResponseEntity.ok(ticketService.purchase(currentUser.getId(), request));
    }
}
```

- [ ] **Step 2: Add the sold-out handler to `GlobalExceptionHandler`** (after `handleInvalidDates`)

```java
    @ExceptionHandler(TicketSoldOutException.class)
    public ProblemDetail handleSoldOut(TicketSoldOutException ex) {
        log.warn("Ticket type sold out: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Ticket sold out");
        problem.setDetail(ex.getMessage());
        return problem;
    }
```

`TicketSoldOutException` is in the same package as `GlobalExceptionHandler` (`com.example.tickets.exception`), so no import is needed.

- [ ] **Step 3: Compile and commit**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

```bash
git add src/main/java/com/example/tickets/controller/TicketController.java src/main/java/com/example/tickets/exception/GlobalExceptionHandler.java
git commit -m "feat(api): expose ticket purchase endpoint"
```

---

### Task 7: Regression check

- [ ] **Step 1: Run full suite**

Run: `./mvnw test`
Expected: all existing suites pass (behavior unchanged); BUILD SUCCESS