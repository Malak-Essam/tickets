package com.example.tickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tickets.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
}
