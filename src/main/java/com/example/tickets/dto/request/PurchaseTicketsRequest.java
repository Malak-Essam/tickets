package com.example.tickets.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseTicketsRequest(
    @NotNull UUID ticketTypeId,
    @NotNull @Min(1) Integer quantity) {
}