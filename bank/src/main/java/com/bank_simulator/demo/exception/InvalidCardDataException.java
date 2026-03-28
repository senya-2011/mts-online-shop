package com.bank_simulator.demo.exception;

public class InvalidCardDataException extends RuntimeException {

    public InvalidCardDataException(String message) {
        super(message);
    }
}
