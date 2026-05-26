package com.mts.online_shop.bank.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

import java.io.Serializable;

public final class BankManagedConnectionMetaData implements ManagedConnectionMetaData, Serializable {

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
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    @Override
    public String getUserName() throws ResourceException {
        return "";
    }
}
