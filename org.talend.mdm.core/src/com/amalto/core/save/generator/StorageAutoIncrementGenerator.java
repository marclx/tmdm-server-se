package com.amalto.core.save.generator;

import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.server.Server;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.server.XmlServer;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.metadata.UnsupportedDataRecordMetadata;
import com.amalto.core.storage.transaction.Transaction;
import com.amalto.core.storage.transaction.TransactionManager;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import java.util.Iterator;
import java.util.List;

import static com.amalto.core.query.user.UserQueryBuilder.eq;
import static com.amalto.core.query.user.UserQueryBuilder.from;

/**
 * This {@link com.amalto.core.save.generator.AutoIdGenerator generator} is a more secure way to generate auto increment
 * values in case of concurrent access to the same underlying database.
 */
public class StorageAutoIncrementGenerator implements AutoIdGenerator {

    private static final Logger LOGGER = Logger.getLogger(StorageAutoIncrementGenerator.class);

    private final Storage system;

    /**
     * Build a {@link com.amalto.core.save.generator.AutoIdGenerator} using current available system storage (from current
     * context).
     */
    StorageAutoIncrementGenerator() {
        this(ServerContext.INSTANCE.get().getStorageAdmin().get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null));
    }

    /**
     * Build a {@link com.amalto.core.save.generator.AutoIdGenerator} using provided system
     * {@link com.amalto.core.storage.Storage storage}.
     * 
     * @param storage The {@link com.amalto.core.storage.Storage system} storage to use. This storage must be of
     * {@link com.amalto.core.storage.StorageType#SYSTEM} type.
     */
    public StorageAutoIncrementGenerator(Storage storage) {
        if (storage.getType() != StorageType.SYSTEM) {
            throw new IllegalArgumentException("Storage should be of " + StorageType.SYSTEM + " (was " + storage.getType() + ").");
        }
        this.system = storage;
        // Create auto increment if doesn't exist
        MetadataRepository repository = system.getMetadataRepository();
        ComplexTypeMetadata autoIncrementType = repository.getComplexType("AutoIncrement"); //$NON-NLS-1
        UserQueryBuilder qb = from(autoIncrementType).forUpdate();
        system.begin();
        try {
            StorageResults results = system.fetch(qb.getSelect());
            if (results.getCount() == 0) {
                DataRecord autoIncrementRecord = new DataRecord(autoIncrementType, UnsupportedDataRecordMetadata.INSTANCE);
                autoIncrementRecord.set(autoIncrementType.getField("id"), "AutoIncrement"); //$NON-NLS-1
                system.update(autoIncrementRecord);
            }
            system.commit();
        } catch (Exception e) {
            // There might be caused by concurrent access to the database, leading to "duplicate pk" issues. However,
            // all the code in this class cares about is a AutoIncrement instance in database, not by whom.
            system.rollback();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to create auto increment (might be caused by concurrent access to database).", e);
            }
        }
    }

    @Override
    public String generateId(String dataClusterName, String conceptName, String keyElementName) {
        Server server = ServerContext.INSTANCE.get();
        // Create or get current transaction
        TransactionManager manager = server.getTransactionManager();
        Transaction previousTransaction = null;
        if (manager.hasTransaction()) {
            previousTransaction = manager.currentTransaction(); // Remember existing transaction (re-set at the end).
        }
        // Query and update auto increment value using custom lock strategy
        Transaction transaction = manager.create(Transaction.Lifetime.LONG);
        manager.associate(transaction);
        try {
            // Set lock strategy
            transaction.setLockStrategy(Transaction.LockStrategy.LOCK_FOR_UPDATE);
            system.begin(); // Implicitly adds system in current transaction
            // Query and update auto increment record
            MetadataRepository repository = system.getMetadataRepository();
            ComplexTypeMetadata autoIncrementType = repository.getComplexType("AutoIncrement"); //$NON-NLS-1
            FieldMetadata entryField = autoIncrementType.getField("entry"); //$NON-NLS-1
            ComplexTypeMetadata entryType = (ComplexTypeMetadata) entryField.getType();
            FieldMetadata keyField = entryType.getField("key"); //$NON-NLS-1
            FieldMetadata valueField = entryType.getField("value"); //$NON-NLS-1
            // Get the auto increment record
            UserQueryBuilder qb = from(autoIncrementType)
                    .where(eq(autoIncrementType.getField("id"), "AutoIncrement")) //$NON-NLS-1 //$NON-NLS-2
                    .limit(1)
                    .forUpdate();
            DataRecord autoIncrementRecord = null;
            StorageResults autoIncrementRecordResults = system.fetch(qb.getSelect());
            Iterator<DataRecord> iterator = autoIncrementRecordResults.iterator();
            while (iterator.hasNext()) {
                autoIncrementRecord = iterator.next();
                if (iterator.hasNext()) {
                    throw new IllegalArgumentException("Expected only 1 auto increment to be returned.");
                }
            }
            // Update it
            String key = "[HEAD]." + dataClusterName + '.' + conceptName + '.' + keyElementName; //$NON-NLS-1
            List<DataRecord> entries = (List<DataRecord>) autoIncrementRecord.get(entryField);
            Integer value = null;
            if (entries != null) {
                for (DataRecord entry : entries) { // Find entry for type in database object
                    if (key.equals(String.valueOf(entry.get(keyField)))) {
                        Integer integer = (Integer) entry.get(valueField);
                        entry.set(valueField, integer + 1);
                        value = integer;
                    }
                }
            }
            if (value == null) { // No entry for current asked type, creates one
                DataRecord entry = new DataRecord(entryType, UnsupportedDataRecordMetadata.INSTANCE);
                entry.set(keyField, key);
                entry.set(valueField, 1);
                autoIncrementRecord.set(entryField, entry); // This add at end of collection is already present
            }
            // Update the DB record before leaving
            system.update(autoIncrementRecord);
            transaction.commit();
            if (value == null) {
                // Re-entry in order to re-acquire lock (for concurrent initialization of first id).
                return generateId(dataClusterName, conceptName, keyElementName);
            }
            return String.valueOf(value);
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException("Unable to get auto increment value.", e);
        } finally {
            if (previousTransaction != null) {
                manager.associate(previousTransaction); // Restore previous transaction (if any).
            }
        }
    }

    @Override
    public void saveState(XmlServer server) {
        // No need to do anything (implementation saves new values when generated).
    }

    @Override
    public void init() {
        // Nothing to do.
    }
}
