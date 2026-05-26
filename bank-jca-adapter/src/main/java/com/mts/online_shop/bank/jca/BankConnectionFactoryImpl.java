package com.mts.online_shop.bank.jca;

import javax.naming.Reference;
import javax.naming.Referenceable;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

import java.io.Serializable;

public final class BankConnectionFactoryImpl implements ConnectionFactory, Serializable, Referenceable {

    private static final long serialVersionUID = 1L;

    private final BankManagedConnectionFactory mcf;
    private final ConnectionManager cm;
    private final BankRecordFactory recordFactory = new BankRecordFactory();
    private transient Reference reference;

    BankConnectionFactoryImpl(BankManagedConnectionFactory mcf, ConnectionManager cm) {
        this.mcf = mcf;
        this.cm = cm;
    }

    @Override
    public jakarta.resource.cci.Connection getConnection() throws ResourceException {
        return (jakarta.resource.cci.Connection) cm.allocateConnection(mcf, null);
    }

    @Override
    public jakarta.resource.cci.Connection getConnection(ConnectionSpec properties) throws ResourceException {
        return getConnection();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return recordFactory;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return new BankResourceAdapterMetaData();
    }

    @Override
    public Reference getReference() {
        return reference;
    }

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }
}
