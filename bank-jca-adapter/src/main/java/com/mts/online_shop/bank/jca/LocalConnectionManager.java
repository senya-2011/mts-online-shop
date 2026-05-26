package com.mts.online_shop.bank.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import java.io.Serializable;

/**
 * Minimal {@link ConnectionManager} for outbound-only usage outside an application server.
 */
public final class LocalConnectionManager implements ConnectionManager, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        return mc.getConnection(null, cxRequestInfo);
    }
}
