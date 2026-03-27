package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {
    public OrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Not Found", "order-not-found", message);
    }
}
