package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentDataException extends ApiException {
    public InvalidPaymentDataException(String message) {
        super(HttpStatus.BAD_REQUEST, "Bad Request", "invalid-payment-data", message);
    }
}
