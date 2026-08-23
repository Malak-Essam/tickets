package com.example.tickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tickets.domain.TicketType;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
}
