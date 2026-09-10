package com.example.tickets.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after successfully purchasing tickets")
public record PurchaseTicketsResponse(
    @Schema(description = "List of purchased tickets with QR codes")
    List<PurchasedTicketResponse> tickets) {
}
