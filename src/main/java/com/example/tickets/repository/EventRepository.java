package com.example.tickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tickets.domain.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {
}
