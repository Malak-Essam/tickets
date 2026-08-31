package com.example.tickets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublishedEventResponse(
    UUID id,
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String venue,
    List<PublishedTicketTypeResponse> ticketTypes) {
}
