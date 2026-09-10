package com.example.tickets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Published event details visible to attendees")
public record PublishedEventResponse(
    @Schema(description = "Unique identifier of the event", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Name of the event", example = "Spring Music Festival")
    String name,

    @Schema(description = "Event start date and time", example = "2026-06-15T18:00:00")
    LocalDateTime startDate,

    @Schema(description = "Event end date and time", example = "2026-06-15T23:00:00")
    LocalDateTime endDate,

    @Schema(description = "Venue where the event takes place", example = "City Arena")
    String venue,

    @Schema(description = "Available ticket types for this event")
    List<PublishedTicketTypeResponse> ticketTypes) {
}
