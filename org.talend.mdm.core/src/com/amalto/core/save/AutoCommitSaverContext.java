/*
 * Copyright (C) 2006-2013 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.save;

import com.amalto.core.history.Action;
import com.amalto.core.history.MutableDocument;
import com.amalto.core.save.context.AutoCommit;
import com.amalto.core.save.context.DocumentSaver;
import org.apache.commons.lang.StringUtils;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;

import java.util.List;

public class AutoCommitSaverContext extends AbstractDocumentSaverContext {

    private final DocumentSaverContext delegate;

    private AutoCommitSaverContext(DocumentSaverContext delegate) {
        this.delegate = delegate;
    }

    public static AutoCommitSaverContext decorate(DocumentSaverContext context) {
        return new AutoCommitSaverContext(context);
    }

    @Override
    public MutableDocument getUpdateReportDocument() {
        return delegate.getUpdateReportDocument();
    }

    @Override
    public void setUpdateReportDocument(MutableDocument updateReportDocument) {
        delegate.setUpdateReportDocument(updateReportDocument);
    }

    @Override
    public UserAction getUserAction() {
        return delegate.getUserAction();
    }

    @Override
    public void setUserAction(UserAction userAction) {
        delegate.setUserAction(userAction);
    }

    @Override
    public String getPartialUpdatePivot() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getPartialUpdateKey() {
        return StringUtils.EMPTY;
    }

    @Override
    public DocumentSaver createSaver() {
        return new AutoCommit(delegate.createSaver());
    }

    @Override
    public MutableDocument getDatabaseDocument() {
        return delegate.getDatabaseDocument();
    }

    @Override
    public MutableDocument getDatabaseValidationDocument() {
        return delegate.getDatabaseValidationDocument();
    }

    @Override
    public MutableDocument getUserDocument() {
        return delegate.getUserDocument();
    }

    @Override
    public void setUserDocument(MutableDocument document) {
        delegate.setUserDocument(document);
    }

    @Override
    public List<Action> getActions() {
        return delegate.getActions();
    }

    @Override
    public void setActions(List<Action> actions) {
        delegate.setActions(actions);
    }

    @Override
    public ComplexTypeMetadata getType() {
        return delegate.getType();
    }

    @Override
    public String getDataCluster() {
        return delegate.getDataCluster();
    }

    @Override
    public String getDataModelName() {
        return delegate.getDataModelName();
    }

    @Override
    public String getRevisionID() {
        return delegate.getRevisionID();
    }

    @Override
    public void setDatabaseDocument(MutableDocument databaseDocument) {
        delegate.setDatabaseDocument(databaseDocument);
    }

    @Override
    public void setDatabaseValidationDocument(MutableDocument databaseValidationDocument) {
        delegate.setDatabaseValidationDocument(databaseValidationDocument);
    }

    @Override
    public void setRevisionId(String revisionID) {
        delegate.setRevisionId(revisionID);
    }

    @Override
    public void setType(ComplexTypeMetadata type) {
        delegate.setType(type);
    }

    @Override
    public boolean hasMetAutoIncrement() {
        return delegate.hasMetAutoIncrement();
    }

    @Override
    public void setHasMetAutoIncrement(boolean hasMetAutoIncrement) {
        delegate.setHasMetAutoIncrement(hasMetAutoIncrement);
    }

    @Override
    public void setTaskId(String taskId) {
        delegate.setTaskId(taskId);
    }

    @Override
    public String getTaskId() {
        return delegate.getTaskId();
    }

    @Override
    public boolean preserveOldCollectionValues() {
        return delegate.preserveOldCollectionValues();
    }

    @Override
    public String[] getId() {
        return delegate.getId();
    }

    @Override
    public void setId(String[] id) {
        delegate.setId(id);
    }
}