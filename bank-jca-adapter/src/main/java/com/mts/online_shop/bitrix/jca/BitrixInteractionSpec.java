package com.mts.online_shop.bitrix.jca;

import jakarta.resource.cci.InteractionSpec;

import java.io.Serializable;

public final class BitrixInteractionSpec implements InteractionSpec, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FUNCTION_BLOGPOST_ADD = "BLOGPOST_ADD";

    private final String functionName;

    public BitrixInteractionSpec(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }
}
