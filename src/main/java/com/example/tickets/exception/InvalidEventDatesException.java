package com.example.tickets.exception;

public class InvalidEventDatesException extends RuntimeException {

    public InvalidEventDatesException(String message) {
        super(message);
    }
}
