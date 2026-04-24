package com.mts.online_shop.bank.jca;

import jakarta.resource.cci.ResourceAdapterMetaData;

import java.io.Serializable;

public final class BankResourceAdapterMetaData implements ResourceAdapterMetaData, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String getAdapterVersion() {
        return "1.0";
    }

    @Override
    public String getAdapterVendorName() {
        return "mts-online-shop";
    }

    @Override
    public String getAdapterName() {
        return "BankJcaAdapter";
    }

    @Override
    public String getAdapterShortDescription() {
        return "Outbound CCI to bank simulator REST API";
    }

    @Override
    public String getSpecVersion() {
        return "2.1";
    }

    @Override
    public String[] getInteractionSpecsSupported() {
        return new String[]{BankInteractionSpec.class.getName()};
    }

    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return true;
    }

    @Override
    public boolean supportsExecuteWithInputRecordOnly() {
        return true;
    }

    @Override
    public boolean supportsLocalTransactionDemarcation() {
        return false;
    }
}
