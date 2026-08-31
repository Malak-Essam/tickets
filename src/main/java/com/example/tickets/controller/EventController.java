package com.example.tickets.controller;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.mapper.EventMapper;
import com.example.tickets.service.EventService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "startDate");

    private final EventService eventService;
    private final EventMapper eventMapper;

    @PostMapping
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

    @GetMapping
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
    public ResponseEntity<PageResponse<EventResponse>> listPublished(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "startDate") String sort,
        @RequestParam(defaultValue = "asc") String direction,
        @RequestParam(required = false) String q) {
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new IllegalArgumentException(
                "sort must be one of: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Event> events = (q != null && !q.isBlank())
            ? eventService.searchPublished(q.trim(), pageable)
            : eventService.listPublished(pageable);
        return ResponseEntity.ok(eventMapper.toPageResponse(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable UUID id,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        Event event = eventService.getById(id, currentUser.getId());
        return ResponseEntity.ok(eventMapper.toResponse(event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        eventService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
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
