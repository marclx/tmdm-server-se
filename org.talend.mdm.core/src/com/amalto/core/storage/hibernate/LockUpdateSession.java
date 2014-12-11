package com.amalto.core.storage.hibernate;

import org.hibernate.*;
import org.hibernate.classic.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A implementation of {@link org.hibernate.Session} that ensures all read operations are performed using pessimistic
 * locks.
 * @see com.amalto.core.storage.transaction.Transaction.LockStrategy
 * @see com.amalto.core.storage.transaction.StorageTransaction#getLockStrategy()
 * @see HibernateStorageTransaction#getLockStrategy()
 */
class LockUpdateSession implements Session {

    private static final LockOptions lockOptions = LockOptions.UPGRADE.setLockMode(LockMode.PESSIMISTIC_WRITE);

    private static final LockMode lockMode = LockMode.PESSIMISTIC_WRITE;

    private final Session delegate;

    LockUpdateSession(Session delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object saveOrUpdateCopy(Object object) throws HibernateException {
        return delegate.saveOrUpdateCopy(object);
    }

    @Override
    public Object saveOrUpdateCopy(Object object, Serializable id) throws HibernateException {
        return delegate.saveOrUpdateCopy(object, id);
    }

    @Override
    public Object saveOrUpdateCopy(String entityName, Object object) throws HibernateException {
        return delegate.saveOrUpdateCopy(entityName, object);
    }

    @Override
    public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) throws HibernateException {
        return delegate.saveOrUpdateCopy(entityName, object, id);
    }

    @Override
    public List find(String query) throws HibernateException {
        return delegate.find(query);
    }

    @Override
    public List find(String query, Object value, Type type) throws HibernateException {
        return delegate.find(query, value, type);
    }

    @Override
    public List find(String query, Object[] values, Type[] types) throws HibernateException {
        return delegate.find(query, values, types);
    }

    @Override
    public Iterator iterate(String query) throws HibernateException {
        return delegate.iterate(query);
    }

    @Override
    public Iterator iterate(String query, Object value, Type type) throws HibernateException {
        return delegate.iterate(query, value, type);
    }

    @Override
    public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException {
        return delegate.iterate(query, values, types);
    }

    @Override
    public Collection filter(Object collection, String filter) throws HibernateException {
        return delegate.filter(collection, filter);
    }

    @Override
    public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException {
        return delegate.filter(collection, filter, value, type);
    }

    @Override
    public Collection filter(Object collection, String filter, Object[] values, Type[] types) throws HibernateException {
        return delegate.filter(collection, filter, values, types);
    }

    @Override
    public int delete(String query) throws HibernateException {
        return delegate.delete(query);
    }

    @Override
    public int delete(String query, Object value, Type type) throws HibernateException {
        return delegate.delete(query, value, type);
    }

    @Override
    public int delete(String query, Object[] values, Type[] types) throws HibernateException {
        return delegate.delete(query, values, types);
    }

    @Override
    public Query createSQLQuery(String sql, String returnAlias, Class returnClass) {
        return delegate.createSQLQuery(sql, returnAlias, returnClass);
    }

    @Override
    public Query createSQLQuery(String sql, String[] returnAliases, Class[] returnClasses) {
        return delegate.createSQLQuery(sql, returnAliases, returnClasses);
    }

    @Override
    public void save(Object object, Serializable id) throws HibernateException {
        delegate.save(object, id);
    }

    @Override
    public void save(String entityName, Object object, Serializable id) throws HibernateException {
        delegate.save(entityName, object, id);
    }

    @Override
    public void update(Object object, Serializable id) throws HibernateException {
        delegate.update(object, id);
    }

    @Override
    public void update(String entityName, Object object, Serializable id) throws HibernateException {
        delegate.update(entityName, object, id);
    }

    @Override
    public EntityMode getEntityMode() {
        return delegate.getEntityMode();
    }

    @Override
    public org.hibernate.Session getSession(EntityMode entityMode) {
        return delegate.getSession(entityMode);
    }

    @Override
    public void flush() throws HibernateException {
        delegate.flush();
    }

    @Override
    public void setFlushMode(FlushMode flushMode) {
        delegate.setFlushMode(flushMode);
    }

    @Override
    public FlushMode getFlushMode() {
        return delegate.getFlushMode();
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        delegate.setCacheMode(cacheMode);
    }

    @Override
    public CacheMode getCacheMode() {
        return delegate.getCacheMode();
    }

    @Override
    public SessionFactory getSessionFactory() {
        return delegate.getSessionFactory();
    }

    @Override
    public Connection connection() throws HibernateException {
        return delegate.connection();
    }

    @Override
    public Connection close() throws HibernateException {
        return delegate.close();
    }

    @Override
    public void cancelQuery() throws HibernateException {
        delegate.cancelQuery();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        return delegate.isDirty();
    }

    @Override
    public boolean isDefaultReadOnly() {
        return delegate.isDefaultReadOnly();
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {
        delegate.setDefaultReadOnly(readOnly);
    }

    @Override
    public Serializable getIdentifier(Object object) throws HibernateException {
        return delegate.getIdentifier(object);
    }

    @Override
    public boolean contains(Object object) {
        return delegate.contains(object);
    }

    @Override
    public void evict(Object object) throws HibernateException {
        delegate.evict(object);
    }

    @Override
    public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
        return delegate.load(theClass, id, LockUpdateSession.lockMode);
    }

    @Override
    public Object load(Class theClass, Serializable id, LockOptions lockOptions) throws HibernateException {
        return delegate.load(theClass, id, LockUpdateSession.lockOptions);
    }

    @Override
    public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        return delegate.load(entityName, id, LockUpdateSession.lockMode);
    }

    @Override
    public Object load(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
        return delegate.load(entityName, id, LockUpdateSession.lockOptions);
    }

    @Override
    public Object load(Class theClass, Serializable id) throws HibernateException {
        return delegate.load(theClass, id, lockOptions);
    }

    @Override
    public Object load(String entityName, Serializable id) throws HibernateException {
        return delegate.load(entityName, id, lockOptions);
    }

    @Override
    public void load(Object object, Serializable id) throws HibernateException {
        delegate.load(object.getClass(), id, lockOptions);
    }

    @Override
    public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {
        delegate.replicate(object, replicationMode);
    }

    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {
        delegate.replicate(entityName, object, replicationMode);
    }

    @Override
    public Serializable save(Object object) throws HibernateException {
        return delegate.save(object);
    }

    @Override
    public Serializable save(String entityName, Object object) throws HibernateException {
        return delegate.save(entityName, object);
    }

    @Override
    public void saveOrUpdate(Object object) throws HibernateException {
        delegate.saveOrUpdate(object);
    }

    @Override
    public void saveOrUpdate(String entityName, Object object) throws HibernateException {
        delegate.saveOrUpdate(entityName, object);
    }

    @Override
    public void update(Object object) throws HibernateException {
        delegate.update(object);
    }

    @Override
    public void update(String entityName, Object object) throws HibernateException {
        delegate.update(entityName, object);
    }

    @Override
    public Object merge(Object object) throws HibernateException {
        return delegate.merge(object);
    }

    @Override
    public Object merge(String entityName, Object object) throws HibernateException {
        return delegate.merge(entityName, object);
    }

    @Override
    public void persist(Object object) throws HibernateException {
        delegate.persist(object);
    }

    @Override
    public void persist(String entityName, Object object) throws HibernateException {
        delegate.persist(entityName, object);
    }

    @Override
    public void delete(Object object) throws HibernateException {
        delegate.delete(object);
    }

    @Override
    public void delete(String entityName, Object object) throws HibernateException {
        delegate.delete(entityName, object);
    }

    @Override
    public void lock(Object object, LockMode lockMode) throws HibernateException {
        delegate.lock(object, lockMode);
    }

    @Override
    public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
        delegate.lock(entityName, object, lockMode);
    }

    @Override
    public LockRequest buildLockRequest(LockOptions lockOptions) {
        return delegate.buildLockRequest(lockOptions);
    }

    @Override
    public void refresh(Object object) throws HibernateException {
        delegate.refresh(object);
    }

    @Override
    public void refresh(Object object, LockMode lockMode) throws HibernateException {
        delegate.refresh(object, LockUpdateSession.lockMode);
    }

    @Override
    public void refresh(Object object, LockOptions lockOptions) throws HibernateException {
        delegate.refresh(object, LockUpdateSession.lockOptions);
    }

    @Override
    public LockMode getCurrentLockMode(Object object) throws HibernateException {
        return delegate.getCurrentLockMode(object);
    }

    @Override
    public Transaction beginTransaction() throws HibernateException {
        return delegate.beginTransaction();
    }

    @Override
    public Transaction getTransaction() {
        return delegate.getTransaction();
    }

    @Override
    public Criteria createCriteria(Class persistentClass) {
        Criteria criteria = delegate.createCriteria(persistentClass);
        criteria.setLockMode(LockMode.PESSIMISTIC_READ);
        return criteria;
    }

    @Override
    public Criteria createCriteria(Class persistentClass, String alias) {
        Criteria criteria = delegate.createCriteria(persistentClass, alias);
        criteria.setLockMode(LockMode.PESSIMISTIC_READ);
        return criteria;
    }

    @Override
    public Criteria createCriteria(String entityName) {
        Criteria criteria = delegate.createCriteria(entityName);
        criteria.setLockMode(LockMode.PESSIMISTIC_READ);
        return criteria;
    }

    @Override
    public Criteria createCriteria(String entityName, String alias) {
        Criteria criteria = delegate.createCriteria(entityName, alias);
        criteria.setLockMode(LockMode.PESSIMISTIC_READ);
        return criteria;
    }

    @Override
    public Query createQuery(String queryString) throws HibernateException {
        return delegate.createQuery(queryString);
    }

    @Override
    public SQLQuery createSQLQuery(String queryString) throws HibernateException {
        return delegate.createSQLQuery(queryString);
    }

    @Override
    public Query createFilter(Object collection, String queryString) throws HibernateException {
        return delegate.createFilter(collection, queryString);
    }

    @Override
    public Query getNamedQuery(String queryName) throws HibernateException {
        return delegate.getNamedQuery(queryName);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Object get(Class clazz, Serializable id) throws HibernateException {
        return delegate.get(clazz, id, lockOptions);
    }

    @Override
    public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
        return delegate.get(clazz, id, lockOptions);
    }

    @Override
    public Object get(Class clazz, Serializable id, LockOptions lockOptions) throws HibernateException {
        return delegate.get(clazz, id, LockUpdateSession.lockOptions);
    }

    @Override
    public Object get(String entityName, Serializable id) throws HibernateException {
        return delegate.get(entityName, id, lockOptions);
    }

    @Override
    public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        return delegate.get(entityName, id, lockOptions);
    }

    @Override
    public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
        return delegate.get(entityName, id, LockUpdateSession.lockOptions);
    }

    @Override
    public String getEntityName(Object object) throws HibernateException {
        return delegate.getEntityName(object);
    }

    @Override
    public Filter enableFilter(String filterName) {
        return delegate.enableFilter(filterName);
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        return delegate.getEnabledFilter(filterName);
    }

    @Override
    public void disableFilter(String filterName) {
        delegate.disableFilter(filterName);
    }

    @Override
    public SessionStatistics getStatistics() {
        return delegate.getStatistics();
    }

    @Override
    public boolean isReadOnly(Object entityOrProxy) {
        return delegate.isReadOnly(entityOrProxy);
    }

    @Override
    public void setReadOnly(Object entityOrProxy, boolean readOnly) {
        delegate.setReadOnly(entityOrProxy, readOnly);
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        delegate.doWork(work);
    }

    @Override
    public Connection disconnect() throws HibernateException {
        return delegate.disconnect();
    }

    @Override
    public void reconnect() throws HibernateException {
        delegate.reconnect();
    }

    @Override
    public void reconnect(Connection connection) throws HibernateException {
        delegate.reconnect(connection);
    }

    @Override
    public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
        return delegate.isFetchProfileEnabled(name);
    }

    @Override
    public void enableFetchProfile(String name) throws UnknownProfileException {
        delegate.enableFetchProfile(name);
    }

    @Override
    public void disableFetchProfile(String name) throws UnknownProfileException {
        delegate.disableFetchProfile(name);
    }
}
