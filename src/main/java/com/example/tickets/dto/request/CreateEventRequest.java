package com.example.tickets.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.example.tickets.domain.EventStatusEnum;

public record CreateEventRequest(
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String venue,
    LocalDateTime salesStart,
    LocalDateTime salesEnd,
    EventStatusEnum status,
    List<CreateTicketTypeRequest> ticketTypes) {
}
