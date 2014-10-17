package com.amalto.core.storage;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;

import com.amalto.core.storage.record.DataRecord;

public class CachedResults implements StorageResults {

    private final List<DataRecord> records;

    public CachedResults(List<DataRecord> records) {
        this.records = records;
    }

    public static StorageResults from(StorageResults otherResults) {
        return new CachedResults(IteratorUtils.toList(otherResults.iterator()));
    }

    @Override
    public int getSize() {
        return records.size();
    }

    @Override
    public int getCount() {
        return records.size();
    }

    @Override
    public void close() {
    }

    @Override
    public Iterator<DataRecord> iterator() {
        return records.iterator();
    }
}
