package com.example.tickets.dto.request;

import java.math.BigDecimal;

public record CreateTicketTypeRequest(
    String name,
    BigDecimal price,
    Integer totalAvailable,
    String description) {
}
