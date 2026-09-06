package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchasedTicketResponse(
    UUID ticketId,
    UUID ticketTypeId,
    String ticketTypeName,
    BigDecimal price,
    String qrCode) {
}