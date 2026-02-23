package com.mts.online_shop.exception;

import org.springframework.http.HttpStatus;

public class ProductNotInCartException extends ApiException {
    public ProductNotInCartException(String message) {
        super(HttpStatus.NOT_FOUND, "Not Found", "product-not-in-cart", message);
    }
}
