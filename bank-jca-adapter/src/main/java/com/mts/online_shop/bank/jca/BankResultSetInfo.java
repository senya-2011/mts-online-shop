package com.mts.online_shop.bank.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.ResultSetInfo;

import java.io.Serializable;

public final class BankResultSetInfo implements ResultSetInfo, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean supportsResultSetType(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean updatesAreDetected(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean deletesAreDetected(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean insertsAreDetected(int type) throws ResourceException {
        return false;
    }

    @Override
    public boolean supportsResultTypeConcurrency(int type, int concurrency) throws ResourceException {
        return false;
    }
}
