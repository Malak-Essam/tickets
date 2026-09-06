package com.example.tickets.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.QrCode;
import com.example.tickets.domain.Ticket;
import com.example.tickets.domain.TicketStatusEnum;
import com.example.tickets.domain.TicketType;
import com.example.tickets.domain.User;
import com.example.tickets.dto.request.PurchaseTicketsRequest;
import com.example.tickets.dto.response.PurchaseTicketsResponse;
import com.example.tickets.dto.response.PurchasedTicketResponse;
import com.example.tickets.exception.TicketSoldOutException;
import com.example.tickets.repository.TicketRepository;
import com.example.tickets.repository.TicketTypeRepository;
import com.example.tickets.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    @Transactional
    public PurchaseTicketsResponse purchase(UUID purchaserId, PurchaseTicketsRequest request) {
        TicketType ticketType = ticketTypeRepository.getTicketTypeByIdWithLock(request.ticketTypeId())
            .orElseThrow(() -> new NoSuchElementException("Ticket type not found"));
        int quantity = request.quantity();
        if (ticketType.getTotalAvailable() < quantity) {
            throw new TicketSoldOutException(
                "Requested %d tickets but only %d remaining".formatted(quantity, ticketType.getTotalAvailable()));
        }
        ticketType.setTotalAvailable(ticketType.getTotalAvailable() - quantity);
        User purchaser = userRepository.findById(purchaserId)
            .orElseThrow(() -> new IllegalStateException("User %s does not exist".formatted(purchaserId)));

        List<PurchasedTicketResponse> responses = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Ticket ticket = ticketRepository.save(Ticket.builder()
                .status(TicketStatusEnum.PURCHASED)
                .ticketType(ticketType)
                .purchaser(purchaser)
                .build());
            QrCode qrCode = qrCodeService.generate(ticket);
            responses.add(new PurchasedTicketResponse(
                ticket.getId(),
                ticketType.getId(),
                ticketType.getName(),
                ticketType.getPrice(),
                qrCode.getValue()));
        }
        return new PurchaseTicketsResponse(responses);
    }
}