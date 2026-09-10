package com.example.tickets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.tickets.domain.EventStatusEnum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full event details returned to the organizer")
public record EventResponse(
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

    @Schema(description = "When ticket sales open", example = "2026-03-01T00:00:00")
    LocalDateTime salesStart,

    @Schema(description = "When ticket sales close", example = "2026-06-14T23:59:59")
    LocalDateTime salesEnd,

    @Schema(description = "Current status of the event")
    EventStatusEnum status,

    @Schema(description = "ID of the organizer who owns this event")
    UUID organizerId,

    @Schema(description = "Associated ticket types")
    List<TicketTypeResponse> ticketTypes) {
}
