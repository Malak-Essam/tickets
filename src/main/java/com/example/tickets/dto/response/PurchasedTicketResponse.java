package com.example.tickets.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Individual purchased ticket with QR code")
public record PurchasedTicketResponse(
    @Schema(description = "Unique identifier of the ticket", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID ticketId,

    @Schema(description = "ID of the ticket type", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID ticketTypeId,

    @Schema(description = "Name of the ticket type", example = "VIP")
    String ticketTypeName,

    @Schema(description = "Price paid for this ticket", example = "50.00")
    BigDecimal price,

    @Schema(description = "Base64-encoded QR code image for ticket validation")
    String qrCode) {
}
