package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {
    public ProductNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Not Found", "product-not-found", message);
    }
}
