package com.bank_simulator.demo.controller;

import com.bank_simulator.demo.exception.CardNotFoundException;
import com.bank_simulator.demo.exception.InsufficientFundsException;
import com.bank_simulator.demo.exception.InvalidAmountException;
import com.bank_simulator.demo.model.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BankExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BankExceptionHandler.class);

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<PaymentResponse> handleInvalidAmount(InvalidAmountException ex) {
        log.warn("InvalidAmount: {}", ex.getMessage());
        PaymentResponse body = new PaymentResponse();
        body.setApproved(false);
        body.setMessage(ex.getMessage());
        body.setRemainingBalance(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<PaymentResponse> handleCardNotFound(CardNotFoundException ex) {
        log.warn("CardNotFound: {}", ex.getMessage());
        PaymentResponse body = new PaymentResponse();
        body.setApproved(false);
        body.setMessage(ex.getMessage());
        body.setRemainingBalance(null);
        // Для простоты возвращаем 400, как описано в OpenAPI.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<PaymentResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("InsufficientFunds: {}", ex.getMessage());
        PaymentResponse body = new PaymentResponse();
        body.setApproved(false);
        body.setMessage(ex.getMessage());
        body.setRemainingBalance(ex.getBalance() != null ? ex.getBalance().floatValue() : null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

