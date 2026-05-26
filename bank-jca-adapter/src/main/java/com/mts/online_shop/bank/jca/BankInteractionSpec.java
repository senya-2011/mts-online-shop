package com.mts.online_shop.bank.jca;

import jakarta.resource.cci.InteractionSpec;

import java.io.Serializable;

public final class BankInteractionSpec implements InteractionSpec, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FUNCTION_PAYMENT = "PAYMENT";
    public static final String FUNCTION_REFUND = "REFUND";

    private final String functionName;

    public BankInteractionSpec(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }
}
