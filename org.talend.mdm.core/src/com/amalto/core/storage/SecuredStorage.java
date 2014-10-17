/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage;

import com.amalto.core.query.user.Expression;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.transaction.StorageTransaction;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.metadata.compare.ImpactAnalyzer;

import java.util.Set;

public class SecuredStorage implements Storage {

    private final Storage delegate;

    private final UserDelegator delegator;

    /**
     * Interface to handle user visibility rules.
     */
    public static interface UserDelegator {

        /**
         * @param field A field in data model.
         * @return <code>true</code> if user should not see the <code>field</code>, <code>false</code> otherwise.
         */
        boolean hide(FieldMetadata field);

        /**
         * @param type A entity type in data model.
         * @return <code>true</code> if user should not see the <code>type</code>, <code>false</code> otherwise.
         */
        boolean hide(ComplexTypeMetadata type);
    }

    public SecuredStorage(Storage delegate, UserDelegator delegator) {
        this.delegate = delegate;
        this.delegator = delegator;
    }

    @Override
    public Storage asInternal() {
        return delegate.asInternal();
    }

    @Override
    public int getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public StorageTransaction newStorageTransaction() {
        return delegate.newStorageTransaction();
    }

    public void init(DataSourceDefinition dataSource) {
        delegate.init(dataSource);
    }

    public void prepare(MetadataRepository repository, Set<Expression> optimizedExpressions, boolean force,
            boolean dropExistingData) {
        delegate.prepare(repository, optimizedExpressions, force, dropExistingData);
    }

    public void prepare(MetadataRepository repository, boolean dropExistingData) {
        delegate.prepare(repository, dropExistingData);
    }

    @Override
    public MetadataRepository getMetadataRepository() {
        return delegate.getMetadataRepository();
    }

    public StorageResults fetch(Expression userQuery) {
        Expression cleanedExpression = userQuery.accept(new SecurityQueryCleaner(delegator));
        return delegate.fetch(cleanedExpression);
    }

    public void update(DataRecord record) {
        delegate.update(record);
    }

    public void update(Iterable<DataRecord> records) {
        delegate.update(records);
    }

    public void delete(Expression userQuery) {
        delegate.delete(userQuery);
    }

    @Override
    public void delete(DataRecord record) {
        delegate.delete(record);
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

    @Override
    public ImpactAnalyzer getImpactAnalyzer() {
        return delegate.getImpactAnalyzer();
    }

    @Override
    public void adapt(MetadataRepository newRepository, boolean force) {
        delegate.adapt(newRepository, force);
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

}
