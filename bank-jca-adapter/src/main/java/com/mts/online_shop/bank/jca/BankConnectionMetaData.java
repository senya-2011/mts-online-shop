package com.mts.online_shop.bank.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionMetaData;

import java.io.Serializable;

public final class BankConnectionMetaData implements ConnectionMetaData, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String getEISProductName() throws ResourceException {
        return "BankSimulator";
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        return "1.0";
    }

    @Override
    public String getUserName() throws ResourceException {
        return "";
    }
}
