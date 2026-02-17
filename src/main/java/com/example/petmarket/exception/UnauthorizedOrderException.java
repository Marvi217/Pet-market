package com.example.petmarket.exception;

public class UnauthorizedOrderException extends RuntimeException {
    public UnauthorizedOrderException(String message) {
        super(message);
    }

    public UnauthorizedOrderException() {
        super("You are not authorized to access this order");
    }
}