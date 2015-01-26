/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.save.context;

import com.amalto.core.ejb.UpdateReportPOJO;
import com.amalto.core.history.Action;
import com.amalto.core.history.MutableDocument;
import com.amalto.core.history.accessor.Accessor;
import com.amalto.core.save.DocumentSaverContext;
import com.amalto.core.save.ReportDocumentSaverContext;
import com.amalto.core.save.SaverSession;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.w3c.dom.Document;

import java.util.List;

class UpdateReport implements DocumentSaver {

    public static final String UPDATE_REPORT_DATA_MODEL = "UpdateReport"; //$NON-NLS-1$

    public static final String UPDATE_REPORT_TYPE = "Update"; //$NON-NLS-1$

    private final DocumentSaver next;

    UpdateReport(DocumentSaver next) {
        this.next = next;
    }

    @Override
    public void save(SaverSession session, DocumentSaverContext context) {
        UpdateReportDocument updateReportDocument;
        Document updateReportAsDOM = (Document) SaverContextFactory.EMPTY_UPDATE_REPORT.cloneNode(true);
        if(isInvokeBeforeSaving(context)) {
            if(context.getUpdateReportDocument() != null){
                updateReportAsDOM = context.getUpdateReportDocument().asDOM();
            }
        }       
        updateReportDocument = new UpdateReportDocument(updateReportAsDOM);
        
        StringBuilder key = new StringBuilder();
        String[] id = context.getId();
        for (int i = 0; i < id.length; i++) {
            key.append(id[i]);
            if (i < id.length - 1) {
                key.append('.');
            }
        }

        ComplexTypeMetadata type = context.getUserDocument().getType();
        List<Action> actions = context.getActions();
        boolean hasHeader = false;
        for (Action action : actions) {
            if (!hasHeader) {
                setHeader(updateReportDocument, "UserName", session.getSaverSource().getLegitimateUser()); //$NON-NLS-1$
                setHeader(updateReportDocument, "Source", String.valueOf(action.getSource())); //$NON-NLS-1$
                setHeader(updateReportDocument, "TimeInMillis", String.valueOf(action.getDate().getTime())); //$NON-NLS-1$
                setHeader(updateReportDocument, "RevisionID", String.valueOf(context.getRevisionID())); //$NON-NLS-1$
                setHeader(updateReportDocument, "DataCluster", String.valueOf(context.getDataCluster())); //$NON-NLS-1$
                setHeader(updateReportDocument, "DataModel", String.valueOf(context.getDataModelName())); //$NON-NLS-1$
                setHeader(updateReportDocument, "Concept", String.valueOf(type.getName())); //$NON-NLS-1$
                setHeader(updateReportDocument, "Key", key.toString()); //$NON-NLS-1$
                hasHeader = true;
                updateReportDocument.enableRecordFieldChange();
            }
            action.perform(updateReportDocument);
            action.undo(updateReportDocument);
        }
        if (context.getUpdateReportDocument() == null) {
            updateReportDocument.setOperationType(UpdateReportPOJO.OPERATION_TYPE_UPDATE);
        }
        updateReportDocument.disableRecordFieldChange();

        context.setUpdateReportDocument(updateReportDocument);
        next.save(session, context);
    }

    private void setHeader(MutableDocument updateReportDocument, String fieldName, String value) {
        Accessor accessor = updateReportDocument.createAccessor(fieldName);
        accessor.set(value);
    }
    
    private boolean isInvokeBeforeSaving(DocumentSaverContext context) {
        if (context instanceof ReportDocumentSaverContext) {
            if(((ReportDocumentSaverContext)context).getDelegate() instanceof StorageSaver){
                StorageSaver saver = (StorageSaver) ((ReportDocumentSaverContext)context).getDelegate();
                return saver.isInvokeBeforeSaving();
            } else if (((ReportDocumentSaverContext)context).getDelegate() instanceof UserContext){
                UserContext saver = (UserContext) ((ReportDocumentSaverContext)context).getDelegate();
                return saver.isInvokeBeforeSaving();
            }
        }
        return false;
    }

    @Override
    public String[] getSavedId() {
        return next.getSavedId();
    }

    @Override
    public String getSavedConceptName() {
        return next.getSavedConceptName();
    }

    @Override
    public String getBeforeSavingMessage() {
        return next.getBeforeSavingMessage();
    }
}
