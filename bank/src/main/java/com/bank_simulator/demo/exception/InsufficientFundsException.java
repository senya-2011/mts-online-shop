package com.bank_simulator.demo.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    private final BigDecimal balance;

    public InsufficientFundsException(String message, BigDecimal balance) {
        super(message);
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}

