package com.amalto.core.storage.hibernate;

import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.transaction.StorageTransaction;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.ContextHelper;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.util.List;

/**
 * This class is needed in case of clustering: default option can't handle well full text indexing. This class allows
 * JMS-based sharing. See <a
 * href="https://docs.jboss.org/hibernate/search/3.1/reference/en/html/jms-backend.html">here</a> for more configuration
 * details.
 */
public class JMSFullTextController extends AbstractJMSHibernateSearchController implements MessageDrivenBean {

    public static final Logger        LOGGER            = Logger.getLogger(JMSFullTextController.class);

    @Override
    protected Session getSession() {
        /*
         * Quick note: MDM holds *many* Session (and also one SessionFactory per MDM container). This means getSession()
         * needs a context information such as the class being updated). For this reason, it's better to throw exception
         * here and provide a different onMessage implementation.
         */
        throw new UnsupportedOperationException("Not supported (need additional context information to select correct Session.");
    }

    @Override
    protected void cleanSessionIfNeeded(Session session) {
        // Not needed (sessions are closed at back end level - in Hibernate code -).
    }

    @Override
    public void onMessage(Message message) {
        LOGGER.info("Processing work...");
        if (!(message instanceof ObjectMessage)) {
            LOGGER.error("Incorrect message type: '" + message.getClass() + "'.");
            return;
        }
        Runnable worker = getWorker((ObjectMessage) message);
        worker.run();
    }

    private Runnable getWorker(ObjectMessage message) {
        StorageAdmin admin = ServerContext.INSTANCE.get().getStorageAdmin();
        String[] containers = admin.getAll(null);
        Storage storage = null;
        StorageClassLoader classLoader = null;
        for (String container : containers) {
            storage = admin.get(container, StorageType.MASTER, null);
            if (storage.asInternal() instanceof HibernateStorage) {
                HibernateStorage hibernateStorage = (HibernateStorage) storage.asInternal();
                classLoader = hibernateStorage.getClassLoader();
                ClassLoader previous = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    message.getObject();
                    storage = hibernateStorage;
                    break; // Found storage and class loader, exit.
                } catch (Exception e) {
                    // Ignored
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Ignored exception during look up for storage.", e);
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(previous);
                }
            }
        }
        Runnable empty = new Runnable() {

            @Override
            public void run() {
                // Nothing to do.
            }
        };
        if (storage == null || classLoader == null) {
            LOGGER.error("Unable to find storage for class '" + message + "'. Discarding index update.");
            return empty;
        }
        StorageTransaction transaction = storage.newStorageTransaction();
        Session session = ((HibernateStorageTransaction) transaction).getSession();
        session.beginTransaction();
        final ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            SearchFactoryImplementor factory = ContextHelper.getSearchFactory(session);
            final Runnable processor = factory.getBackendQueueProcessorFactory().getProcessor((List<LuceneWork>) message.getObject());
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        processor.run();
                    } finally {
                        Thread.currentThread().setContextClassLoader(previous);
                    }
                }
            };
        } catch (Exception e) {
            LOGGER.error("Unable to process queue.", e);
        } finally {
            cleanSessionIfNeeded(session);
        }
        return empty;
    }

    @Override
    public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) throws EJBException {
    }

    public void ejbCreate() throws EJBException {
        LOGGER.info("Enabled JMS sharing for full text indexes.");
    }

    @Override
    public void ejbRemove() throws EJBException {
        LOGGER.info("Shutdown JMS sharing for full text indexes.");
    }
}
