package com.mts.online_shop.bank.jca;

import jakarta.resource.cci.MappedRecord;

import java.util.LinkedHashMap;

/**
 * {@link MappedRecord} backed by a {@link LinkedHashMap}; satisfies Map + cci Record methods.
 */
public final class SimpleMappedRecord extends LinkedHashMap<String, Object> implements MappedRecord<String, Object> {

    private static final long serialVersionUID = 1L;

    private String recordName = "record";
    private String description = "";

    public SimpleMappedRecord() {
    }

    public SimpleMappedRecord(String recordName) {
        this.recordName = recordName;
    }

    @Override
    public String getRecordName() {
        return recordName;
    }

    @Override
    public void setRecordName(String name) {
        this.recordName = name;
    }

    @Override
    public String getRecordShortDescription() {
        return description;
    }

    @Override
    public void setRecordShortDescription(String description) {
        this.description = description;
    }

    @Override
    public Object clone() {
        SimpleMappedRecord copy = new SimpleMappedRecord(recordName);
        copy.description = this.description;
        copy.putAll(this);
        return copy;
    }
}
