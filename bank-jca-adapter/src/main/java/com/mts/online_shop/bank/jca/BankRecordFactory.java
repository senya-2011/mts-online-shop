package com.mts.online_shop.bank.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.RecordFactory;

import java.io.Serializable;

public final class BankRecordFactory implements RecordFactory, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public MappedRecord createMappedRecord(String recordName) throws ResourceException {
        return new SimpleMappedRecord(recordName);
    }

    @Override
    public IndexedRecord createIndexedRecord(String recordName) throws ResourceException {
        throw new ResourceException("IndexedRecord not supported");
    }
}
