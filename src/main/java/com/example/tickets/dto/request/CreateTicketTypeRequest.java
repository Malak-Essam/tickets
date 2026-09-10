package com.example.tickets.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket type to include when creating an event")
public record CreateTicketTypeRequest(
    @Schema(description = "Display name of the ticket type", example = "General Admission", maxLength = 255)
    @NotBlank @Size(max = 255) String name,

    @Schema(description = "Price per ticket", example = "25.00")
    @NotNull @Positive BigDecimal price,

    @Schema(description = "Total number of tickets available for this type", example = "100")
    @NotNull @Positive Integer totalAvailable,

    @Schema(description = "Optional description of the ticket type", example = "Includes access to all main stages")
    String description) {
}
