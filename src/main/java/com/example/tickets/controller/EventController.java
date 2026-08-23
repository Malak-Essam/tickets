package com.example.tickets.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tickets.domain.Event;
import com.example.tickets.domain.User;
import com.example.tickets.dto.request.CreateEventRequest;
import com.example.tickets.dto.response.EventResponse;
import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.mapper.EventMapper;
import com.example.tickets.service.EventService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    @PostMapping
    public ResponseEntity<EventResponse> create(@RequestBody CreateEventRequest request,
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
}
