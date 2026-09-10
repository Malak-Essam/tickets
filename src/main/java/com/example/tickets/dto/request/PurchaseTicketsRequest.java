package com.example.tickets.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for purchasing tickets")
public record PurchaseTicketsRequest(
    @Schema(description = "ID of the ticket type to purchase", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull UUID ticketTypeId,

    @Schema(description = "Number of tickets to purchase", example = "2", minimum = "1")
    @NotNull @Min(1) Integer quantity) {
}
