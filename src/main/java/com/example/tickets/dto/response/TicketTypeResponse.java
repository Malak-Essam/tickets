package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketTypeResponse(
    UUID id,
    String name,
    BigDecimal price,
    Integer totalAvailable,
    String description) {
}
