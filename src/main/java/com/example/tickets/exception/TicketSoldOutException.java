package com.example.tickets.exception;

public class TicketSoldOutException extends RuntimeException {

    public TicketSoldOutException(String message) {
        super(message);
    }
}