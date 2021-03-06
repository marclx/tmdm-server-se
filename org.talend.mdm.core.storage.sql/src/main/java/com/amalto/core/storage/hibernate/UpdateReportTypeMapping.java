package com.amalto.core.storage.hibernate;

import org.talend.mdm.commmon.metadata.*;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordReader;
import com.amalto.core.storage.record.DataRecordXmlWriter;
import com.amalto.core.storage.record.XmlStringDataRecordReader;
import org.hibernate.Session;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
*
*/
class UpdateReportTypeMapping extends TypeMapping {

    private static final String MAPPING_NAME = "FIXED UPDATE REPORT MAPPING"; //$NON-NLS-1$

    private static final String NO_SOURCE = "none"; //$NON-NLS-1$

    private final ComplexTypeMetadata updateReportType;

    private final ComplexTypeMetadata databaseUpdateReportType;

    private final MetadataRepository repository;

    private Map<String, FieldMetadata> userToDatabase = new HashMap<String, FieldMetadata>();

    private Map<String, FieldMetadata> databaseToUser = new HashMap<String, FieldMetadata>();

    public UpdateReportTypeMapping(ComplexTypeMetadata updateReportType, ComplexTypeMetadata databaseUpdateReportType, MappingRepository mappings, MetadataRepository repository) {
        super(updateReportType, mappings);
        this.updateReportType = updateReportType;
        this.databaseUpdateReportType = databaseUpdateReportType;
        this.repository = repository;

        map(updateReportType.getField("UserName"), databaseUpdateReportType.getField("x_user_name")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("Source"), databaseUpdateReportType.getField("x_source")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("TimeInMillis"), databaseUpdateReportType.getField("x_time_in_millis")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("OperationType"), databaseUpdateReportType.getField("x_operation_type")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("RevisionID"), databaseUpdateReportType.getField("x_revision_id")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("DataCluster"), databaseUpdateReportType.getField("x_data_cluster")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("DataModel"), databaseUpdateReportType.getField("x_data_model")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("Concept"), databaseUpdateReportType.getField("x_concept")); //$NON-NLS-1$ //$NON-NLS-2$
        map(updateReportType.getField("Key"), databaseUpdateReportType.getField("x_key")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public ComplexTypeMetadata getUser() {
        return updateReportType;
    }

    @Override
    public ComplexTypeMetadata getDatabase() {
        return databaseUpdateReportType;
    }

    @Override
    public void setValues(Session session, DataRecord from, Wrapper to) {
        to.set("x_user_name", from.get("UserName")); //$NON-NLS-1$ //$NON-NLS-2$
        Object source = from.get("Source");
        if (source == null) {
            // TMDM-4856: In case source is null, put "none" as the source.
            source = NO_SOURCE;
        }
        to.set("x_source", source); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_time_in_millis", Long.parseLong(String.valueOf(from.get("TimeInMillis")))); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_operation_type", from.get("OperationType")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_revision_id", from.get("RevisionID")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_data_cluster", from.get("DataCluster")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_data_model", from.get("DataModel")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_concept", from.get("Concept")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set("x_key", from.get("Key")); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            List<DataRecord> dataRecord = (List<DataRecord>) from.get("Item"); //$NON-NLS-1$
            if (dataRecord != null) { // this might be null if there is no 'Item' element in update report.
                DataRecordXmlWriter writer = new DataRecordXmlWriter("Item"); //$NON-NLS-1$
                StringWriter stringWriter = new StringWriter();
                for (DataRecord record : dataRecord) {
                    writer.write(record, new BufferedWriter(stringWriter));
                }
                to.set("x_items_xml", stringWriter.toString()); //$NON-NLS-1$
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not set Items XML value", e);
        }
    }

    @Override
    public DataRecord setValues(Wrapper from, DataRecord to) {
        String itemXmlContent = (String) from.get("x_items_xml"); //$NON-NLS-1$
        DataRecordReader<String> itemReader = new XmlStringDataRecordReader();
        DataRecord items = itemReader.read(repository, updateReportType, "<Update>" + itemXmlContent + "</Update>");  //$NON-NLS-1$ //$NON-NLS-2$

        to.set(updateReportType.getField("UserName"), from.get("x_user_name")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("Source"), from.get("x_source")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("TimeInMillis"), from.get("x_time_in_millis")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("OperationType"), from.get("x_operation_type")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("RevisionID"), from.get("x_revision_id")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("DataCluster"), from.get("x_data_cluster")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("DataModel"), from.get("x_data_model")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("Concept"), from.get("x_concept")); //$NON-NLS-1$ //$NON-NLS-2$
        to.set(updateReportType.getField("Key"), from.get("x_key")); //$NON-NLS-1$ //$NON-NLS-2$
        List<DataRecord> itemList = (List<DataRecord>) items.get("Item"); //$NON-NLS-1$
        if (itemList != null) { // Might be null for create update report for instance.
            for (DataRecord dataRecord : itemList) {
                to.set(updateReportType.getField("Item"), dataRecord); //$NON-NLS-1$
            }
        }

        return to;
    }

    @Override
    public String getDatabaseTimestamp() {
        return "x_time_in_millis"; //$NON-NLS-1$
    }

    @Override
    public String getDatabaseTaskId() {
        return null;
    }

    @Override
    public String toString() {
        return MAPPING_NAME;
    }

    protected void map(FieldMetadata user, FieldMetadata database) {
        if (isFrozen) {
            throw new IllegalStateException("Mapping is frozen.");
        }
        userToDatabase.put(user.getName(), database);
        databaseToUser.put(database.getName(), user);
    }

    public FieldMetadata getDatabase(FieldMetadata from) {
        if (!from.getContainingType().equals(updateReportType)) {
            throw new IllegalArgumentException("Field '" + from.getName() + "' does not exist in database.");
        }
        return userToDatabase.get(from.getName());
    }

    public FieldMetadata getUser(FieldMetadata to) {
        return databaseToUser.get(to.getName());
    }

    /**
     * "Freeze" both database and internal types.
     * @see TypeMetadata#freeze()
     */
    public void freeze() {
        if (!isFrozen) {
            // Ensure mapped type are frozen.
            try {
                database.freeze();
            } catch (Exception e) {
                throw new RuntimeException("Could not process internal type '" + database.getName() + "'.", e);
            }
            try {
                user.freeze();
            } catch (Exception e) {
                throw new RuntimeException("Could not process user type '" + user.getName() + "'.", e);
            }

            // Freeze field mappings.
            Map<String, FieldMetadata> frozen = new HashMap<String, FieldMetadata>();
            for (Map.Entry<String, FieldMetadata> entry : userToDatabase.entrySet()) {
                frozen.put(entry.getKey(), entry.getValue().freeze());
            }
            userToDatabase = frozen;
            frozen = new HashMap<String, FieldMetadata>();
            for (Map.Entry<String, FieldMetadata> entry : databaseToUser.entrySet()) {
                frozen.put(entry.getKey(), entry.getValue().freeze());
            }
            databaseToUser = frozen;
            isFrozen = true;
        }
    }
}
