package com.example.tickets.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.TicketType;

import jakarta.persistence.LockModeType;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tt from TicketType tt where tt.id = :id")
    @Transactional
    Optional<TicketType> getTicketTypeByIdWithLock(@Param("id") UUID id);
}