package com.example.tickets.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.example.tickets.domain.EventStatusEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a new event with its ticket types")
public record CreateEventRequest(
    @Schema(description = "Name of the event", example = "Spring Music Festival", maxLength = 255)
    @NotBlank @Size(max = 255) String name,

    @Schema(description = "Event start date and time", example = "2026-06-15T18:00:00")
    @NotNull LocalDateTime startDate,

    @Schema(description = "Event end date and time", example = "2026-06-15T23:00:00")
    @NotNull LocalDateTime endDate,

    @Schema(description = "Venue where the event takes place", example = "City Arena", maxLength = 255)
    @NotBlank @Size(max = 255) String venue,

    @Schema(description = "When ticket sales open", example = "2026-03-01T00:00:00")
    @NotNull LocalDateTime salesStart,

    @Schema(description = "When ticket sales close", example = "2026-06-14T23:59:59")
    @NotNull LocalDateTime salesEnd,

    @Schema(description = "Initial status of the event")
    @NotNull EventStatusEnum status,

    @Schema(description = "At least one ticket type must be provided")
    @NotEmpty List<@Valid CreateTicketTypeRequest> ticketTypes) {
}
