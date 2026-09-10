package com.example.tickets.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tickets.domain.User;
import com.example.tickets.dto.request.PurchaseTicketsRequest;
import com.example.tickets.dto.response.PurchaseTicketsResponse;
import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tickets", description = "Ticket purchase endpoints")
public class TicketController {

    private final TicketService ticketService;

    @PreAuthorize("hasRole('ATTENDEE')")
    @PostMapping("/purchase")
    @Operation(summary = "Purchase tickets", description = "Purchases tickets for a given ticket type. Requires ATTENDEE role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tickets purchased successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "403", description = "Not authorized — requires ATTENDEE role"),
        @ApiResponse(responseCode = "409", description = "Tickets sold out")
    })
    public ResponseEntity<PurchaseTicketsResponse> purchase(@Valid @RequestBody PurchaseTicketsRequest request,
        HttpServletRequest servletRequest) {
        User currentUser = (User) servletRequest.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user is missing from the request");
        }
        return ResponseEntity.ok(ticketService.purchase(currentUser.getId(), request));
    }
}
