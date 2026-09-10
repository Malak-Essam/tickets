package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Published ticket type visible to attendees (no inventory info)")
public record PublishedTicketTypeResponse(
    @Schema(description = "Unique identifier of the ticket type", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Display name of the ticket type", example = "General Admission")
    String name,

    @Schema(description = "Price per ticket", example = "25.00")
    BigDecimal price,

    @Schema(description = "Optional description of the ticket type", example = "Includes access to all main stages")
    String description) {
}
