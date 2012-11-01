/*
 * Copyright (C) 2006-2012 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.storage;

import com.amalto.core.metadata.ConsoleDumpMetadataVisitor;
import com.amalto.core.metadata.FieldMetadata;
import com.amalto.core.metadata.MetadataRepository;
import com.amalto.core.query.user.Expression;
import com.amalto.core.query.user.UserQueryDumpConsole;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.record.DataRecord;
import org.apache.log4j.Logger;

import java.util.Set;

public class StorageLogger implements Storage {

    private static final Logger LOGGER = Logger.getLogger(StorageLogger.class);

    private final Storage delegate;

    public StorageLogger(Storage delegate) {
        this.delegate = delegate;
    }

    public void init(DataSource dataSource) {
        delegate.init(dataSource);
    }

    private void handlePrepareError(MetadataRepository repository, boolean force, Exception e) {
        // Dumps types to console
        LOGGER.error("##### Error during storage preparation (force = " + force + ") #####");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Types:");
            repository.accept(new ConsoleDumpMetadataVisitor());
        }
        // TODO Dumps hibernate mapping (if possible).
        // TODO Dumps hibernate configuration
        // Re-throw exception
        throw new RuntimeException(e);
    }

    public void prepare(MetadataRepository repository, Set<FieldMetadata> indexedFields, boolean force, boolean dropExistingData) {
        try {
            delegate.prepare(repository, indexedFields, force, dropExistingData);
        } catch (Exception e) {
            handlePrepareError(repository, force, e);
        }
    }

    public void prepare(MetadataRepository repository, boolean dropExistingData) {
        try {
            delegate.prepare(repository, dropExistingData);
        } catch (Exception e) {
            handlePrepareError(repository, false, e);
        }
    }

    @Override
    public MetadataRepository getMetadataRepository() {
        return delegate.getMetadataRepository();
    }

    public StorageResults fetch(Expression userQuery) {
        if (LOGGER.isDebugEnabled()) {
            userQuery.accept(new UserQueryDumpConsole(LOGGER));
        }
        try {
            return delegate.fetch(userQuery);
        } catch (Exception e) {
            // Dumps query to console
            LOGGER.error("##### Error during fetch operation #####");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Query:");
                userQuery.accept(new UserQueryDumpConsole(LOGGER));
            }
            // Re-throw exception
            throw new RuntimeException(e);
        }
    }

    public void update(DataRecord record) {
        delegate.update(record);
    }

    public void update(Iterable<DataRecord> records) {
        delegate.update(records);
    }

    public void delete(Expression userQuery) {
        if (LOGGER.isDebugEnabled()) {
            userQuery.accept(new UserQueryDumpConsole(LOGGER));
        }
        try {
            delegate.delete(userQuery);
        } catch (Exception e) {
            // Dumps query to console
            LOGGER.error("##### Error during delete operation #####");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Query:");
                userQuery.accept(new UserQueryDumpConsole(LOGGER));
            }
            // Re-throw exception
            throw new RuntimeException(e);
        }
    }

    public void close() {
        delegate.close();
    }

    @Override
    public void close(boolean dropExistingData) {
        delegate.close(dropExistingData);
    }

    public void begin() {
        delegate.begin();
    }

    public void commit() {
        delegate.commit();
    }

    public void rollback() {
        delegate.rollback();
    }

    public void end() {
        delegate.end();
    }

    public void reindex() {
        delegate.reindex();
    }

    public Set<String> getFullTextSuggestion(String keyword, FullTextSuggestion mode, int suggestionSize) {
        return delegate.getFullTextSuggestion(keyword, mode, suggestionSize);
    }

    public String getName() {
        return delegate.getName();
    }

    public DataSource getDataSource() {
        return delegate.getDataSource();
    }

    @Override
    public StorageType getType() {
        return delegate.getType();
    }
}