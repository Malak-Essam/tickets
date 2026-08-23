package com.example.tickets.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.Event;
import com.example.tickets.domain.EventStatusEnum;
import com.example.tickets.domain.User;
import com.example.tickets.dto.request.CreateEventRequest;
import com.example.tickets.mapper.EventMapper;
import com.example.tickets.repository.EventRepository;
import com.example.tickets.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    @Transactional
    public Event create(UUID organizerId, CreateEventRequest request) {
        User organizer = userRepository.findById(organizerId)
            .orElseThrow(() -> new IllegalStateException("Organizer %s does not exist".formatted(organizerId)));
        Event event = eventMapper.toEvent(request);
        event.setOrganizer(organizer);
        event.setStatus(request.status() == null ? EventStatusEnum.DRAFT : request.status());
        if (event.getTicketTypes() != null) {
            event.getTicketTypes().forEach(type -> type.setEvent(event));
        }
        return eventRepository.save(event);
    }
}
