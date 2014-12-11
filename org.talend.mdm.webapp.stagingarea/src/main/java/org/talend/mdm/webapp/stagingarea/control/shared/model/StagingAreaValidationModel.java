// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.stagingarea.control.shared.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.talend.mdm.webapp.stagingarea.control.shared.event.ModelEvent;

import java.io.Serializable;
import java.util.Date;

public class StagingAreaValidationModel extends AbstractHasModelEvents implements IsSerializable, Serializable {

    public String getId() {
        return get("id"); //$NON-NLS-1$
    }

    public void setId(String id) {
        if (getId() == null && id != null) {
            notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_START, this));
        }
        set("id", id); //$NON-NLS-1$
        notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_MODEL_CHANGED, this));
        if (id == null) {
            notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_END, this));
        }
    }

    public Date getStartDate() {
        return get("start_date"); //$NON-NLS-1$
    }

    public void setStartDate(Date startDate) {
        set("start_date", startDate); //$NON-NLS-1$
        notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_MODEL_CHANGED, this));
    }

    public int getProcessedRecords() {
        return get("processed_records") == null ? 0 : (Integer) get("processed_records"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void setProcessedRecords(int processedRecords) {
        set("processed_records", processedRecords); //$NON-NLS-1$
        notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_MODEL_CHANGED, this));
    }

    public int getInvalidRecords() {
        return get("invalid_records") == null ? 0 : (Integer) get("invalid_records"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void setInvalidRecords(int invalidRecords) {
        set("invalid_records", invalidRecords); //$NON-NLS-1$
        notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_MODEL_CHANGED, this));
    }

    public int getTotalRecord() {
        return get("total_record") == null ? 0 : (Integer) get("total_record"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void setTotalRecord(int totalRecord) {
        set("total_record", totalRecord); //$NON-NLS-1$
        notifyHandlers(new ModelEvent(ModelEvent.Types.VALIDATION_MODEL_CHANGED, this));
    }

}
