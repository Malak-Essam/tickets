package com.example.tickets.dto.response;

import java.util.List;

public record PurchaseTicketsResponse(
    List<PurchasedTicketResponse> tickets) {
}