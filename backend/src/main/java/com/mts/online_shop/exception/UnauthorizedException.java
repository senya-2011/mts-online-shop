package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", "unauthorized", message);
    }
}
