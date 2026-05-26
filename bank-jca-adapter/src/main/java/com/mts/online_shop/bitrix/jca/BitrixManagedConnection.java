package com.mts.online_shop.bitrix.jca;

import com.mts.online_shop.bank.jca.BankManagedConnectionMetaData;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BitrixManagedConnection implements ManagedConnection {

    private final BitrixManagedConnectionFactory mcf;
    private final HttpClient httpClient;
    private final CopyOnWriteArrayList<ConnectionEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile BitrixCciConnection cciHandle;

    BitrixManagedConnection(BitrixManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    BitrixManagedConnectionFactory getMcf() {
        return mcf;
    }

    HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        if (cciHandle != null) {
            throw new ResourceException("Logical connection already open");
        }
        cciHandle = new BitrixCciConnection(this);
        return cciHandle;
    }

    @Override
    public void destroy() throws ResourceException {
        cciHandle = null;
    }

    @Override
    public void cleanup() throws ResourceException {
        cciHandle = null;
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (connection instanceof BitrixCciConnection handle) {
            this.cciHandle = handle;
        } else {
            throw new ResourceException("Unsupported connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new ResourceException("LocalTransaction not supported");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new BankManagedConnectionMetaData();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        mcf.setLogWriter(out);
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return mcf.getLogWriter();
    }

    void logicalClosed(BitrixCciConnection handle) throws ResourceException {
        if (cciHandle == handle) {
            cciHandle = null;
        }
    }
}
