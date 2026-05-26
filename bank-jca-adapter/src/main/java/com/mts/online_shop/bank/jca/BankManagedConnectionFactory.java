package com.mts.online_shop.bank.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

public final class BankManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    private static final long serialVersionUID = 1L;

    private String bankBaseUrl = "http://localhost:8081";
    private transient PrintWriter logWriter;

    public String getBankBaseUrl() {
        return bankBaseUrl;
    }

    public void setBankBaseUrl(String bankBaseUrl) {
        this.bankBaseUrl = bankBaseUrl != null ? bankBaseUrl : "http://localhost:8081";
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new LocalConnectionManager());
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new BankConnectionFactoryImpl(this, cxManager);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return new BankManagedConnection(this);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankManagedConnectionFactory that = (BankManagedConnectionFactory) o;
        return Objects.equals(bankBaseUrl, that.bankBaseUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bankBaseUrl);
    }
}
