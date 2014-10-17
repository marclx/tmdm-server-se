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

package com.amalto.core.storage.hibernate;

import com.amalto.core.load.io.ResettableStringWriter;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.exception.ConstraintViolationException;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordXmlWriter;
import com.amalto.core.storage.record.ObjectDataRecordReader;
import com.amalto.core.storage.transaction.StorageTransaction;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.engine.EntityKey;
import org.hibernate.impl.SessionImpl;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;

import java.util.HashSet;
import java.util.Set;

class HibernateStorageTransaction extends StorageTransaction {

    private static final Logger LOGGER = Logger.getLogger(HibernateStorageTransaction.class);

    private static final int TRANSACTION_DUMP_MAX = 10;

    private final HibernateStorage storage;

    private final Session session;

    private final Thread initiatorThread;

    private boolean hasFailed;

    public HibernateStorageTransaction(HibernateStorage storage, Session session) {
        this.storage = storage;
        this.session = session;
        this.initiatorThread = Thread.currentThread();
    }

    public Thread getInitiatorThread() {
        return initiatorThread;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }

    @Override
    public void begin() {
        if (!session.isOpen()) {
            throw new IllegalStateException("Could not start transaction: provided session is not ready for use (session is closed).");
        }
        Transaction transaction = session.getTransaction();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transaction begin (session " + session.hashCode() + ")");
        }
        if (!transaction.isActive()) {
            session.beginTransaction();
        }
    }

    @Override
    public void commit() {
        if (isAutonomous) {
            Transaction transaction = session.getTransaction();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[" + storage + "] Transaction #" + transaction.hashCode() + " -> Commit includes "
                        + session.getStatistics().getEntityCount() + " not-flushed record(s)...");
            }
            if (!transaction.isActive()) {
                throw new IllegalStateException("Can not commit transaction, no transaction is active.");
            }
            try {
                if (!transaction.wasCommitted()) {
                    session.flush();
                    transaction.commit();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("[" + storage + "] Transaction #" + transaction.hashCode() + " -> Commit done.");
                    }
                } else {
                    LOGGER.warn("Transaction was already committed.");
                }
                if (session.isOpen()) {
                    /*
                     * Eviction is not <b>needed</b> (the session will not be reused), but evicts cache in case the session
                     * is reused.
                     */
                    if (session.getStatistics().getEntityKeys().size() > 0) {
                        session.clear();
                    }
                    session.close();
                }
            } catch (Exception e) {
                try {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Transaction failed, dumps transaction content for diagnostic.");
                        dumpTransactionContent(session, storage); // Dumps all the faulty session information.
                    }
                    processCommitException(e);
                } finally {
                    hasFailed = true; // Mark this storage transaction as "failed".
                }
            }
        } else {
            try {
                if (session.isDirty()) {
                    session.flush();
                }
            } catch (Exception e) {
                hasFailed = true; // Mark this storage transaction as "failed".
                processCommitException(e);
            }
        }
        super.commit();
        storage.getClassLoader().reset(Thread.currentThread());
    }

    private static void processCommitException(Exception e) {
        if (e instanceof org.hibernate.exception.ConstraintViolationException || e instanceof ObjectNotFoundException) {
            throw new ConstraintViolationException(e);
        } else {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dumps all current entities in <code>session</code> using data model information from <code>storage</code>.
     *
     * @param session The Hibernate session that failed to be committed.
     * @param storage A {@link com.amalto.core.storage.hibernate.HibernateStorage} that can be used to retrieve metadata information for all objects in
     *                <code>session</code>.
     */
    private static void dumpTransactionContent(Session session, HibernateStorage storage) {
        Level currentLevel = Level.INFO;
        if (LOGGER.isEnabledFor(currentLevel)) {
            Set<EntityKey> failedKeys = new HashSet<EntityKey>(session.getStatistics().getEntityKeys()); // Copy content to avoid concurrent modification issues.
            int i = 1;
            ObjectDataRecordReader reader = new ObjectDataRecordReader();
            MappingRepository mappingRepository = storage.getTypeEnhancer().getMappings();
            StorageClassLoader classLoader = storage.getClassLoader();
            DataRecordXmlWriter writer = new DataRecordXmlWriter();
            ResettableStringWriter xmlContent = new ResettableStringWriter();
            for (EntityKey failedKey : failedKeys) {
                String entityTypeName = StringUtils.substringAfterLast(failedKey.getEntityName(), "."); //$NON-NLS-1$
                LOGGER.log(currentLevel, "Entity #" + i++ + " (type=" + entityTypeName + ", id=" + failedKey.getIdentifier() + ")");
                try {
                    storage.getClassLoader().bind(Thread.currentThread());
                    Wrapper o = (Wrapper) ((SessionImpl) session).getPersistenceContext().getEntity(failedKey);
                    if (!session.isReadOnly(o)) {
                        if (o != null) {
                            ComplexTypeMetadata type = classLoader.getTypeFromClass(classLoader.loadClass(failedKey.getEntityName()));
                            if (type != null) {
                                DataRecord record = reader.read(mappingRepository.getMappingFromDatabase(type), o);
                                writer.write(record, xmlContent);
                                LOGGER.log(currentLevel, xmlContent + "\n(taskId='" + o.taskId() + "', timestamp='" + o.timestamp() + "')");
                            } else {
                                LOGGER.warn("Could not find data model type for object " + o);
                            }
                        } else {
                            LOGGER.warn("Could not find an object for entity " + failedKey);
                        }
                    }
                } catch (ObjectNotFoundException missingRefException) {
                    LOGGER.log(currentLevel, "Can not log entity: contains a unresolved reference to '"
                            + missingRefException.getEntityName() + "' with id '"
                            + missingRefException.getIdentifier() + "'");
                } catch (Exception serializationException) {
                    LOGGER.log(currentLevel, "Failed to log entity content for type " + entityTypeName + " (enable DEBUG for exception details).");
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Serialization exception occurred.", serializationException);
                    }
                } finally {
                    xmlContent.reset();
                    storage.getClassLoader().unbind(Thread.currentThread());
                }
                if (i > TRANSACTION_DUMP_MAX) {
                    if (!LOGGER.isDebugEnabled()) {
                        int more = failedKeys.size() - i;
                        if (more > 0) {
                            LOGGER.log(currentLevel, "and " + more + " more... (enable DEBUG for full dump)");
                        }
                        return;
                    } else {
                        currentLevel = Level.DEBUG; // Continue the dump but with a DEBUG level
                    }
                }
            }
        }
    }

    @Override
    public void rollback() {
        if (isAutonomous) {
            try {
                Transaction transaction = session.getTransaction();
                if (!transaction.isActive()) {
                    LOGGER.warn("Can not rollback transaction, no transaction is active.");
                    return;
                } else {
                    boolean dirty;
                    try {
                        dirty = session.isDirty();
                    } catch (HibernateException e) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Is dirty check during rollback threw exception.", e);
                        }
                        dirty = true; // Consider session is dirty (exception might occur when there's an integrity issue).
                    }
                    if (LOGGER.isInfoEnabled() && dirty) {
                        LOGGER.info("Transaction is being rollbacked. Transaction content:");
                        dumpTransactionContent(session, storage); // Dumps all content in the current transaction.
                    }
                }
                if (!transaction.wasRolledBack()) {
                    transaction.rollback();
                } else {
                    LOGGER.warn("Transaction was already rollbacked.");
                }
            } finally {
                try {
                    /*
                     * Eviction is not <b>needed</b> (the session will not be reused), but evicts cache in case the
                     * session is reused.
                     */
                    if (session.isOpen() && session.getStatistics().getEntityKeys().size() > 0) {
                        session.clear();
                        session.close();
                    }
                    hasFailed = false;
                } catch (HibernateException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Could not clean up session.", e);
                    }
                } finally {
                    // It is *very* important to ensure super.rollback() gets called (even if session close did not succeed).
                    super.rollback();
                    storage.getClassLoader().reset(Thread.currentThread());
                }
            }
        }
    }

    @Override
    public boolean hasFailed() {
        return hasFailed;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public String toString() {
        return "HibernateStorageTransaction {" +
                "storage=" + storage +
                ", session=" + session +
                ", initiatorThread=" + initiatorThread +
                '}';
    }
}
