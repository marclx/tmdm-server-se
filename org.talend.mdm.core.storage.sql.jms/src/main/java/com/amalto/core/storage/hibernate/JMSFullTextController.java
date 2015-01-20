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
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is needed in case of clustering: default option can't handle well full text indexing. This class allows
 * JMS-based sharing. See <a
 * href="https://docs.jboss.org/hibernate/search/3.1/reference/en/html/jms-backend.html">here</a> for more configuration
 * details.
 */
public class JMSFullTextController extends AbstractJMSHibernateSearchController implements MessageDrivenBean {

    public static final Logger        LOGGER            = Logger.getLogger(JMSFullTextController.class);

    private final Map<Class, Storage> classStorageCache = new HashMap<Class, Storage>();

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
        if (!(message instanceof ObjectMessage)) {
            LOGGER.error("Incorrect message type: '" + message.getClass() + "'.");
            return;
        }
        ObjectMessage objectMessage = (ObjectMessage) message;
        List<LuceneWork> queue;
        try {
            queue = (List<LuceneWork>) objectMessage.getObject();
        } catch (JMSException e) {
            LOGGER.error("Unable to retrieve object from message: " + message.getClass(), e);
            return;
        } catch (ClassCastException e) {
            LOGGER.error("Illegal object retrieved from message", e);
            return;
        }
        Runnable worker = getWorker(queue);
        worker.run();
    }

    private Runnable getWorker(List<LuceneWork> queue) {
        LuceneWork luceneWork = queue.get(0);
        Class entityClass = luceneWork.getEntityClass();
        StorageAdmin admin = ServerContext.INSTANCE.get().getStorageAdmin();
        String[] containers = admin.getAll(null);
        Storage storage = null;
        Storage cachedStorage = classStorageCache.get(entityClass);
        if (cachedStorage != null) {
            // Redo a lookup by name in case storage was restarted in the mean time
            storage = admin.get(cachedStorage.getName(), StorageType.MASTER, null);
        }
        for (String container : containers) {
            storage = admin.get(container, StorageType.MASTER, null);
            if (storage instanceof HibernateStorage) {
                try {
                    HibernateStorage hibernateStorage = (HibernateStorage) storage;
                    Class<?> aClass = hibernateStorage.getClassLoader().findClass(entityClass.getName());
                    classStorageCache.put(aClass, hibernateStorage);
                    storage = hibernateStorage;
                    break;
                } catch (ClassNotFoundException e) {
                    // Ignored, but log it in debug anyway
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Discard storage '" + storage.getName() + "'.", e);
                    }
                }
            }
        }
        if (storage == null) {
            LOGGER.error("Unable to find storage for class '" + entityClass + "'. Discarding index update.");
            return new Runnable() {

                @Override
                public void run() {
                    // Nothing to do.
                }
            };
        }
        StorageTransaction transaction = storage.newStorageTransaction();
        Session session = ((HibernateStorageTransaction) transaction).getSession();
        session.beginTransaction();
        Runnable processor = null;
        try {
            SearchFactoryImplementor factory = ContextHelper.getSearchFactory(session);
            processor = factory.getBackendQueueProcessorFactory().getProcessor(queue);
        } finally {
            cleanSessionIfNeeded(session);
        }
        return processor;
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
