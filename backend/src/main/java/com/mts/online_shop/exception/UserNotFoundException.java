package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Not Found", "user-not-found", message);
    }
}
