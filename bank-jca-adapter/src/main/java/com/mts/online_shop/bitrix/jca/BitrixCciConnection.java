package com.mts.online_shop.bitrix.jca;

import com.mts.online_shop.bank.jca.BankConnectionMetaData;
import com.mts.online_shop.bank.jca.BankResultSetInfo;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;

public final class BitrixCciConnection implements Connection {

    private final BitrixManagedConnection mc;
    private volatile boolean closed;

    BitrixCciConnection(BitrixManagedConnection mc) {
        this.mc = mc;
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        if (closed) {
            throw new ResourceException("Connection closed");
        }
        return new BitrixCciInteraction(mc, this);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local transactions are not supported");
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return new BankConnectionMetaData();
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        return new BankResultSetInfo();
    }

    @Override
    public void close() throws ResourceException {
        if (closed) {
            return;
        }
        closed = true;
        mc.logicalClosed(this);
    }
}
