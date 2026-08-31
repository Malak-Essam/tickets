package com.example.tickets.service;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.Event;
import com.example.tickets.domain.EventStatusEnum;
import com.example.tickets.domain.User;
import com.example.tickets.dto.request.CreateEventRequest;
import com.example.tickets.dto.request.UpdateEventRequest;
import com.example.tickets.exception.InvalidEventDatesException;
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
        if (!request.endDate().isAfter(request.startDate())) {
            throw new InvalidEventDatesException("endDate must be after startDate");
        }
        if (!request.salesEnd().isAfter(request.salesStart())) {
            throw new InvalidEventDatesException("salesEnd must be after salesStart");
        }
        if (request.salesStart().isAfter(request.startDate())) {
            throw new InvalidEventDatesException("salesStart must not be after startDate");
        }
        Event event = eventMapper.toEvent(request);

        request.ticketTypes()
                .stream()
                .map(eventMapper::toTicketType)
                .forEach(event::addTicketType);
        event.setOrganizer(organizer);
        event.setStatus(request.status() == null ? EventStatusEnum.DRAFT : request.status());
        if (event.getTicketTypes() != null) {
            event.getTicketTypes().forEach(type -> type.setEvent(event));
        }
        Event saved = eventRepository.save(event);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Event> list(UUID organizerId, Pageable pageable) {
        return eventRepository.findAllByOrganizerId(organizerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> listPublished(Pageable pageable) {
        return eventRepository.findAllByStatus(EventStatusEnum.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Event getById(UUID id, UUID organizerId) {
        return eventRepository.findByIdAndOrganizerId(id, organizerId)
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    }

    @Transactional
    public void delete(UUID id, UUID organizerId) {
        Event event = getById(id, organizerId);
        eventRepository.delete(event);
    }

    @Transactional
    public Event update(UUID id, UUID organizerId, UpdateEventRequest request) {
        Event event = getById(id, organizerId);
        if (!request.endDate().isAfter(request.startDate())) {
            throw new InvalidEventDatesException("endDate must be after startDate");
        }
        if (!request.salesEnd().isAfter(request.salesStart())) {
            throw new InvalidEventDatesException("salesEnd must be after salesStart");
        }
        if (request.salesStart().isAfter(request.startDate())) {
            throw new InvalidEventDatesException("salesStart must not be after startDate");
        }
        event.setName(request.name());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setVenue(request.venue());
        event.setSalesStart(request.salesStart());
        event.setSalesEnd(request.salesEnd());
        event.setStatus(request.status());
        return eventRepository.save(event);
    }
}
