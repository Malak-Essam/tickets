package com.example.tickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tickets.domain.QrCode;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {
}