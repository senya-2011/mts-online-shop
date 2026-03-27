package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {
    public UserAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "Conflict", "user-already-exists", message);
    }
}
