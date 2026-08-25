package com.example.tickets.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.example.tickets.domain.EventStatusEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull LocalDateTime startDate,
    @NotNull LocalDateTime endDate,
    @NotBlank @Size(max = 255) String venue,
    @NotNull LocalDateTime salesStart,
    @NotNull LocalDateTime salesEnd,
    EventStatusEnum status,
    @NotEmpty List<@Valid CreateTicketTypeRequest> ticketTypes) {
}
