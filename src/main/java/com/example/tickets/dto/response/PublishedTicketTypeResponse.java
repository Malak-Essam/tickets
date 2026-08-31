package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PublishedTicketTypeResponse(
    UUID id,
    String name,
    BigDecimal price,
    String description) {
}
