package com.example.tickets.dto.request;

import java.time.LocalDateTime;

import com.example.tickets.domain.EventStatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for updating an existing event")
public record UpdateEventRequest(
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

    @Schema(description = "Updated status of the event")
    @NotNull EventStatusEnum status) {
}
