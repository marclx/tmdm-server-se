package com.amalto.core.storage.hibernate;

import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.transaction.StorageTransaction;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.ContextHelper;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
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

    private static final Logger   LOGGER = Logger.getLogger(JMSFullTextController.class);

    private static final Runnable EMPTY  = new Runnable() {

                                             @Override
                                             public void run() {
                                                 // Nothing to do.
                                             }
                                         };

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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processing work (message = '" + message + "')...");
        }
        if (!(message instanceof ObjectMessage)) {
            LOGGER.error("Incorrect message type: '" + message.getClass() + "' (expected: " + ObjectMessage.class.getName()
                    + ").");
            return;
        }
        getWorker((ObjectMessage) message).run();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Done processing work (message = '" + message + "').");
        }
    }

    private static Runnable getWorker(ObjectMessage message) {
        StorageAdmin admin = ServerContext.INSTANCE.get().getStorageAdmin();
        String[] containers = admin.getAll(null);
        for (String container : containers) {
            Storage storage = admin.get(container, StorageType.MASTER, null);
            // Storage might be hidden, call asInternal() to get actual storage.
            Storage internal = storage.asInternal();
            if (internal instanceof HibernateStorage) {
                final HibernateStorage hibernateStorage = (HibernateStorage) internal;
                StorageClassLoader classLoader = hibernateStorage.getClassLoader();
                final ClassLoader previous = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(classLoader); // Need dynamically generated classes.
                    message.getObject(); // Throw no class found if storage isn't correct
                    // Found storage, run the full text update
                    StorageTransaction transaction = storage.newStorageTransaction();
                    Session session = ((HibernateStorageTransaction) transaction).getSession();
                    SearchFactoryImplementor factory = ContextHelper.getSearchFactory(session);
                    BackendQueueProcessorFactory queueProcessorFactory = factory.getBackendQueueProcessorFactory();
                    final Transaction hibernateTransaction = session.beginTransaction();
                    final Runnable processor = queueProcessorFactory.getProcessor((List<LuceneWork>) message.getObject());
                    return new Runnable() {

                        @Override
                        public void run() {
                            try {
                                processor.run();
                                if (!hibernateTransaction.wasCommitted() && !hibernateTransaction.wasRolledBack()) {
                                    hibernateTransaction.commit();
                                }
                            } finally {
                                Thread.currentThread().setContextClassLoader(previous);
                            }
                        }
                    };
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
        // Unable to find the storage, discard unit of work.
        try {
            LOGGER.info("Unable to find storage for message '" + message + "'.");
            message.acknowledge(); // Prevent JMS queue fill up.
        } catch (JMSException e) {
            // Ignored
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to acknowledge message.", e);
            }
        }
        return EMPTY;
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
