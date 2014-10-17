/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.query;

import static com.amalto.core.query.user.UserQueryBuilder.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.inmemory.MemoryStorage;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Expression;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.server.MockServerLifecycle;
import com.amalto.core.server.ServerContext;
import com.amalto.core.storage.SecuredStorage;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.hibernate.HibernateStorage;

@SuppressWarnings("nls")
public class StorageTestCase extends TestCase {

    private static Logger LOG = Logger.getLogger(StorageTestCase.class);

    protected static final Storage storage;

    protected static final MetadataRepository repository;

    protected static final ComplexTypeMetadata product;

    protected static final ComplexTypeMetadata ff;

    protected static final ComplexTypeMetadata productFamily;

    protected static final ComplexTypeMetadata store;

    protected static final ComplexTypeMetadata supplier;

    protected static final ComplexTypeMetadata type;

    protected static final ComplexTypeMetadata person;

    protected static final ComplexTypeMetadata address;

    protected static final ComplexTypeMetadata country;

    protected static final ComplexTypeMetadata a;

    protected static final ComplexTypeMetadata b;

    protected static final ComplexTypeMetadata c;

    protected static final ComplexTypeMetadata d;

    protected static final ComplexTypeMetadata e;

    protected static final ComplexTypeMetadata e2;

    protected static final ComplexTypeMetadata e1;

    protected static final ComplexTypeMetadata updateReport;

    protected static final ComplexTypeMetadata persons;

    protected static final ComplexTypeMetadata employee;

    protected static final ComplexTypeMetadata manager;

    protected static final ComplexTypeMetadata ss;

    protected static final ComplexTypeMetadata employee1;

    protected static final ComplexTypeMetadata manager1;

    protected static final ComplexTypeMetadata company;

    protected static final ComplexTypeMetadata entityA;

    protected static final ComplexTypeMetadata entityB;

    protected static final ComplexTypeMetadata ContainedEntityA;

    protected static final ComplexTypeMetadata ContainedEntityB;

    protected static TestUserDelegator userSecurity = new TestUserDelegator();

    public static final String DATABASE = "H2";

    static {
        LOG.info("Setting up MDM server environment...");
        ServerContext.INSTANCE.get(new MockServerLifecycle());
        LOG.info("MDM server environment set.");

        LOG.info("Preparing storage for tests...");
        storage = new SecuredStorage(new HibernateStorage("MDM"), userSecurity);
        repository = new MetadataRepository();
        repository.load(StorageQueryTest.class.getResourceAsStream("metadata.xsd"));

        type = repository.getComplexType("TypeA");
        person = repository.getComplexType("Person");
        address = repository.getComplexType("Address");
        country = repository.getComplexType("Country");
        product = repository.getComplexType("Product");
        productFamily = repository.getComplexType("ProductFamily");
        store = repository.getComplexType("Store");
        supplier = repository.getComplexType("Supplier");
        a = repository.getComplexType("A");
        b = repository.getComplexType("B");
        c = repository.getComplexType("C");
        d = repository.getComplexType("D");
        e = repository.getComplexType("E");
        updateReport = repository.getComplexType("Update");
        ff = repository.getComplexType("ff");
        e2 = repository.getComplexType("E2");
        e1 = repository.getComplexType("E1");
        ss = repository.getComplexType("SS");
        persons = repository.getComplexType("Persons");
        employee = repository.getComplexType("Employee");
        manager = repository.getComplexType("Manager");
        employee1 = repository.getComplexType("Employee1");
        manager1 = repository.getComplexType("Manager1");
        company = repository.getComplexType("Company");
        entityA = repository.getComplexType("EntityA");
        entityB = repository.getComplexType("EntityB");
        ContainedEntityA = repository.getComplexType("ContainedEntityA");
        ContainedEntityB = repository.getComplexType("ContainedEntityB");

        storage.init(getDatasource(DATABASE + "-Default"));
        // Indexed expressions
        List<Expression> indexedExpressions = new LinkedList<Expression>();
        indexedExpressions.add(UserQueryBuilder.from(person).where(isNull(person.getField("firstname"))).getExpression());
        indexedExpressions.add(UserQueryBuilder.from(person).where(isNull(person.getField("score"))).getExpression());
        indexedExpressions.add(UserQueryBuilder.from(person).where(isNull(address.getField("score"))).getExpression());
        indexedExpressions.add(UserQueryBuilder.from(person).where(isNull(product.getField("Stores/Store"))).getExpression());
        indexedExpressions.add(UserQueryBuilder.from(country).where(isNull(country.getField("notes/comment"))).getExpression());
        indexedExpressions.add(UserQueryBuilder.from(d).where(isNull(d.getField("commonText"))).getExpression());
        indexedExpressions.add(UserQueryBuilder.from(e).where(isNull(e.getField("commonText"))).getExpression());
        storage.prepare(repository, new HashSet<Expression>(indexedExpressions), true, true);
        LOG.info("Storage prepared.");
    }

    private ClassLoader previous;

    protected static DataSourceDefinition getDatasource(String dataSourceName) {
        return ServerContext.INSTANCE.get().getDefinition(dataSourceName, "MDM");
    }

    public void test() throws Exception {
        // Just there so JUnit does not complain about a test case that has no test.
    }

    @Override
    public void setUp() throws Exception {
        previous = Thread.currentThread().getContextClassLoader();
        storage.begin();
    }

    @Override
    public void tearDown() throws Exception {
        storage.commit();
        assertTrue(previous == Thread.currentThread().getContextClassLoader());
    }

    protected static class TestUserDelegator implements SecuredStorage.UserDelegator {

        boolean isActive = true;

        public void setActive(boolean active) {
            isActive = active;
        }

        @Override
        public boolean hide(FieldMetadata field) {
            return isActive && field.getHideUsers().contains("System_Users");
        }

        @Override
        public boolean hide(ComplexTypeMetadata type) {
            return isActive && type.getHideUsers().contains("System_Users");
        }
    }
}
