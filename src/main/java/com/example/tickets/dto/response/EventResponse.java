package com.example.tickets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.tickets.domain.EventStatusEnum;

public record EventResponse(
    UUID id,
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String venue,
    LocalDateTime salesStart,
    LocalDateTime salesEnd,
    EventStatusEnum status,
    UUID organizerId,
    List<TicketTypeResponse> ticketTypes) {
}
