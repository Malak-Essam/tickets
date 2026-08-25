# Create-Event Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce field-level and cross-field validation on `POST /api/v1/events`, returning RFC 9457 ProblemDetail errors instead of raw 500s.

**Architecture:** Bean Validation annotations on the two request records, `@Valid` at the controller, three date-ordering rules checked explicitly in `EventService` throwing a dedicated exception, and a new `@RestControllerAdvice` mapping all failures to uniform ProblemDetail responses. No new tests (explicit user decision).

**Tech Stack:** Spring Boot 4.1 (`spring-boot-starter-validation`, `ProblemDetail`), Java 21 records, Lombok, Maven wrapper.

---

### Task 1: Spec and plan documents

**Files:**
- Create: `docs/superpowers/specs/2026-08-25-create-event-validation-design.md`
- Create: `docs/superpowers/plans/2026-08-25-create-event-validation.md`

- [x] **Step 1: Write the spec doc** — content: problem statement (deferred validation from `8d68170`), approved rule set (field table + 3 date rules), Approach B decision (service-side dates), ProblemDetail error contract table, files touched.
- [x] **Step 2: Save this plan** to the path above.
- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-08-25-create-event-validation-design.md docs/superpowers/plans/2026-08-25-create-event-validation.md
git commit -m "docs(spec): add create event validation design"
```

---

### Task 2: Add validation starter

**Files:**
- Modify: `pom.xml` (~line 58, after `spring-boot-starter-jackson`)

- [ ] **Step 1: Insert dependency**

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
```

- [ ] **Step 2: Verify it resolves**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml && git commit -m "feat(api): add bean validation starter"
```

---

### Task 3: Annotate request DTOs

**Files:**
- Modify: `src/main/java/com/example/tickets/dto/request/CreateEventRequest.java`
- Modify: `src/main/java/com/example/tickets/dto/request/CreateTicketTypeRequest.java`

- [ ] **Step 1: Replace `CreateEventRequest.java` body**

```java
package com.example.tickets.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.example.tickets.domain.EventStatusEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull LocalDateTime startDate,
    @NotNull LocalDateTime endDate,
    @NotBlank @Size(max = 255) String venue,
    @NotNull LocalDateTime salesStart,
    @NotNull LocalDateTime salesEnd,
    EventStatusEnum status,
    @NotEmpty List<@Valid CreateTicketTypeRequest> ticketTypes) {
}
```

(`status` stays unannotated — optional, defaults to `DRAFT`. `List<@Valid ...>` cascades into every element.)

- [ ] **Step 2: Replace `CreateTicketTypeRequest.java` body**

```java
package com.example.tickets.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTicketTypeRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @Positive BigDecimal price,
    @NotNull @Positive Integer totalAvailable,
    String description) {
}
```

- [ ] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/tickets/dto/request/ && git commit -m "feat(api): constrain create event request payloads"
```

---

### Task 4: Trigger validation in controller

**Files:**
- Modify: `src/main/java/com/example/tickets/controller/EventController.java`

- [ ] **Step 1: Add imports** `jakarta.validation.Valid`; change the signature line to:

```java
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
        HttpServletRequest servletRequest) {
```

- [ ] **Step 2: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/tickets/controller/EventController.java && git commit -m "feat(api): trigger validation on event creation"
```

---

### Task 5: Service-side date rules

**Files:**
- Create: `src/main/java/com/example/tickets/exception/InvalidEventDatesException.java`
- Modify: `src/main/java/com/example/tickets/service/EventService.java`

- [ ] **Step 1: Create the exception**

```java
package com.example.tickets.exception;

public class InvalidEventDatesException extends RuntimeException {

    public InvalidEventDatesException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: In `EventService.create()`, insert immediately after the organizer lookup (before `eventMapper.toEvent`)** — plus import `com.example.tickets.exception.InvalidEventDatesException`:

```java
        if (!request.endDate().isAfter(request.startDate())) {
            throw new InvalidEventDatesException("endDate must be after startDate");
        }
        if (!request.salesEnd().isAfter(request.salesStart())) {
            throw new InvalidEventDatesException("salesEnd must be after salesStart");
        }
        if (request.salesStart().isAfter(request.startDate())) {
            throw new InvalidEventDatesException("salesStart must not be after startDate");
        }
```

- [ ] **Step 3: Compile**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/tickets/exception/ src/main/java/com/example/tickets/service/EventService.java
git commit -m "feat(api): reject invalid event date ranges"
```

---

### Task 6: ProblemDetail exception handler

**Files:**
- Create: `src/main/java/com/example/tickets/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create handler**

```java
package com.example.tickets.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(InvalidEventDatesException.class)
    public ProblemDetail handleInvalidDates(InvalidEventDatesException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("Malformed request body");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal error");
        problem.setDetail("An unexpected error occurred");
        return problem;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/tickets/exception/GlobalExceptionHandler.java
git commit -m "feat(api): map validation errors to problem details"
```

---

### Task 7: Regression check

- [ ] **Step 1: Run full suite**

Run: `./mvnw test`
Expected: all existing suites pass (valid-request behavior unchanged); BUILD SUCCESS
