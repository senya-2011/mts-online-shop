package com.mts.online_shop.exception;

public class InvalidPaymentDataException extends RuntimeException {

    public InvalidPaymentDataException(String message) {
        super(message);
    }
}
