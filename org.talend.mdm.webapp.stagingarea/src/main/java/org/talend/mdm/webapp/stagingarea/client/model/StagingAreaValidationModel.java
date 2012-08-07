package org.talend.mdm.webapp.stagingarea.client.model;

import java.io.Serializable;
import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.gwt.user.client.rpc.IsSerializable;

public class StagingAreaValidationModel extends BaseModelData implements IsSerializable, Serializable {

    private static final long serialVersionUID = 3782403942844388546L;

    public StagingAreaValidationModel() {

    }

    public String getId() {
        return get("id"); //$NON-NLS-1$
    }

    public void setId(String id) {
        set("id", id); //$NON-NLS-1$
    }

    public Date getStartDate() {
        return get("start_date"); //$NON-NLS-1$
    }

    public void setStartDate(Date startDate) {
        set("start_date", startDate); //$NON-NLS-1$
    }

    public int getProcessedRecords() {
        return get("processed_records"); //$NON-NLS-1$
    }

    public void setProcessedRecords(int processedRecords) {
        set("processed_records", processedRecords); //$NON-NLS-1$
    }

    public int getInvalidRecords() {
        return get("invalid_records"); //$NON-NLS-1$
    }

    public void setInvalidRecords(int invalidRecords) {
        set("invalid_records", invalidRecords); //$NON-NLS-1$
    }

    public int getTotalRecord() {
        return get("total_record"); //$NON-NLS-1$
    }

    public void setTotalRecord(int totalRecord) {
        set("total_record", totalRecord); //$NON-NLS-1$
    }

}