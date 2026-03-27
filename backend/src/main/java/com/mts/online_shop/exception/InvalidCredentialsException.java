package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", "invalid-credentials", message);
    }
}
