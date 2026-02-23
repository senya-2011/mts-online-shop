package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class EmptyCartException extends ApiException {
    public EmptyCartException(String message) {
        super(HttpStatus.BAD_REQUEST, "Bad Request", "empty-cart", message);
    }
}
