package com.example.tickets.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTicketTypeRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @Positive BigDecimal price,
    @NotNull @Positive Integer totalAvailable,
    String description) {
}
