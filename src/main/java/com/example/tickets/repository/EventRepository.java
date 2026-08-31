package com.example.tickets.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.tickets.domain.Event;
import com.example.tickets.domain.EventStatusEnum;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByOrganizerId(UUID organizerId, Pageable pageable);

    Page<Event> findAllByStatus(EventStatusEnum status, Pageable pageable);

    Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);

    Optional<Event> findByIdAndStatus(UUID id, EventStatusEnum status);

    @Query(value = "SELECT * FROM events e WHERE status = 'PUBLISHED' AND to_tsvector('english', e.name || ' ' || e.venue) @@ plainto_tsquery('english', :query)", countQuery = "SELECT COUNT(*) FROM events e WHERE status = 'PUBLISHED' AND to_tsvector('english', e.name || ' ' || e.venue) @@ plainto_tsquery('english', :query)", nativeQuery = true)
    Page<Event> searchByNameOrVenue(@Param("query") String query, Pageable pageable);
}
