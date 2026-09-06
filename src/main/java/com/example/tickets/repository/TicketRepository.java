package com.example.tickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tickets.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
}