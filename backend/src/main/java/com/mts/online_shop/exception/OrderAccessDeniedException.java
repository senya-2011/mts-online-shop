package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class OrderAccessDeniedException extends ApiException {
    public OrderAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "Forbidden", "order-access-denied", message);
    }
}
