package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "Bad Request", "bad-request", message);
    }
}
