package com.example.tickets.controller;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tickets.domain.Event;
import com.example.tickets.domain.User;
import com.example.tickets.dto.request.CreateEventRequest;
import com.example.tickets.dto.request.UpdateEventRequest;
import com.example.tickets.dto.response.EventResponse;
import com.example.tickets.dto.response.PageResponse;
import com.example.tickets.dto.response.PublishedEventResponse;
import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.mapper.EventMapper;
import com.example.tickets.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Validated
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "startDate");

    private final EventService eventService;
    private final EventMapper eventMapper;

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping
    @Operation(summary = "Create a new event", description = "Creates a new event with its ticket types. Requires ORGANIZER role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Event created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "403", description = "Not authorized — requires ORGANIZER role")
    })
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        Event created = eventService.create(currentUser.getId(), request);
        EventResponse response = eventMapper.toResponse(created);
        return ResponseEntity
            .created(URI.create("/api/v1/events/" + created.getId()))
            .body(response);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping
    @Operation(summary = "List events for the current organizer", description = "Returns a paginated, sortable list of events owned by the authenticated organizer.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized — requires ORGANIZER role")
    })
    public ResponseEntity<PageResponse<EventResponse>> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "desc") String direction,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new IllegalArgumentException(
                "sort must be one of: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Event> events = eventService.list(currentUser.getId(), pageable);
        return ResponseEntity.ok(eventMapper.toPageResponse(events));
    }

    @GetMapping("/public")
    @Operation(summary = "List published events", description = "Returns a paginated list of published events. Public — no authentication required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Published events retrieved successfully")
    })
    public ResponseEntity<PageResponse<EventResponse>> listPublished(
        @RequestParam(required = false) String q,
        Pageable pageable) {
        Page<Event> events = (q != null && !q.isBlank())
            ? eventService.searchPublished(q.trim(), pageable)
            : eventService.listPublished(pageable);
        return ResponseEntity.ok(eventMapper.toPageResponse(events));
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Get a published event by ID", description = "Returns details of a single published event. Public — no authentication required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event found"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<PublishedEventResponse> getPublishedById(@PathVariable UUID id) {
        Event event = eventService.getPublishedEvent(id);
        return ResponseEntity.ok(eventMapper.toPublishedResponse(event));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/{id}")
    @Operation(summary = "Get an event by ID", description = "Returns details of a single event owned by the authenticated organizer.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event found"),
        @ApiResponse(responseCode = "403", description = "Not authorized — requires ORGANIZER role"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventResponse> getById(@PathVariable UUID id,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        Event event = eventService.getById(id, currentUser.getId());
        return ResponseEntity.ok(eventMapper.toResponse(event));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event", description = "Deletes an event owned by the authenticated organizer.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized — requires ORGANIZER role"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        eventService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an event", description = "Updates an event owned by the authenticated organizer.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "403", description = "Not authorized — requires ORGANIZER role"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventResponse> update(@PathVariable UUID id,
        @Valid @RequestBody UpdateEventRequest request,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        Event event = eventService.update(id, currentUser.getId(), request);
        return ResponseEntity.ok(eventMapper.toResponse(event));
    }
}
