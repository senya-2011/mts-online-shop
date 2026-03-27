package com.bank_simulator.demo.exception;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String message) {
        super(message);
    }
}

