/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.adapt;

import java.io.File;
import java.sql.*;

import junit.framework.TestCase;

import org.h2.jdbc.JdbcSQLException;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.metadata.compare.CompareTest;
import com.amalto.core.server.MockServerLifecycle;
import com.amalto.core.server.ServerContext;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.hibernate.HibernateStorage;

public class StorageAdaptTest extends TestCase {

    protected static final String STORAGE_NAME = "Test";

    @Override
    public void setUp() throws Exception {
        ServerContext.INSTANCE.get(new MockServerLifecycle());
    }

    public void testParameter() throws Exception {
        DataSourceDefinition dataSource = ServerContext.INSTANCE.get().getDefinition("H2-DS3", STORAGE_NAME);
        Storage storage = new HibernateStorage("Test", StorageType.MASTER);
        storage.init(dataSource);
        MetadataRepository repository = new MetadataRepository();
        repository.load(CompareTest.class.getResourceAsStream("schema1_1.xsd"));
        storage.prepare(repository, true);
        try {
            storage.adapt(null, true);
            fail("Null is not supposed to be accepted as parameter for adapt()");
        } catch (IllegalArgumentException e) {
            // Expected
        } finally {
            storage.close(true);
        }
    }

    public void testNoUpdate() throws Exception {
        DataSourceDefinition dataSource = ServerContext.INSTANCE.get().getDefinition("H2-DS3", STORAGE_NAME);
        Storage storage = new HibernateStorage("Test", StorageType.MASTER);
        storage.init(dataSource);
        MetadataRepository repository = new MetadataRepository();
        repository.load(CompareTest.class.getResourceAsStream("schema1_1.xsd"));
        storage.prepare(repository, true);
        performAssert1(dataSource, true, true);
        storage.adapt(storage.getMetadataRepository(), true);
        performAssert1(dataSource, true, true);
        storage.close(true);
    }

    public void test1() throws Exception {
        // Test preparation
        DataSourceDefinition dataSource = ServerContext.INSTANCE.get().getDefinition("H2-DS3", STORAGE_NAME);
        Storage storage = new HibernateStorage("Test", StorageType.MASTER);
        storage.init(dataSource);
        MetadataRepository repository = new MetadataRepository();
        repository.load(StorageAdaptTest.class.getResourceAsStream("schema1_1.xsd"));
        storage.prepare(repository, true);
        // Ensure name column exists
        performAssert1(dataSource, true, true);
        // Actual test
        MetadataRepository newRepository = new MetadataRepository();
        newRepository.load(StorageAdaptTest.class.getResourceAsStream("schema1_2.xsd"));
        storage.adapt(newRepository, true);
        // Test expected schema update
        performAssert1(dataSource, false, false);
        storage.close(true);
    }

    private void performAssert1(DataSourceDefinition dataSource, boolean expectNameColumn, boolean expectSupplierTable)
            throws SQLException {
        DataSource master = dataSource.getMaster();
        assertTrue(master instanceof RDBMSDataSource);
        RDBMSDataSource rdbmsDataSource = (RDBMSDataSource) master;
        assertEquals(RDBMSDataSource.DataSourceDialect.H2, rdbmsDataSource.getDialectName());
        Connection connection = DriverManager.getConnection(rdbmsDataSource.getConnectionURL());
        Statement statement = connection.createStatement();
        try {
            // Product asserts
            ResultSet resultSet = statement.executeQuery("SELECT * FROM PRODUCT");
            ResultSetMetaData metaData = resultSet.getMetaData();
            boolean hasName = false;
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                hasName |= "x_name".equalsIgnoreCase(metaData.getColumnName(i));
            }
            assertSame(expectNameColumn, hasName);
            // Supplier asserts
            try {
                statement.executeQuery("SELECT * FROM SUPPLIER");
            } catch (SQLException e) {
                if (!expectSupplierTable) {
                    JdbcSQLException h2Exception = (JdbcSQLException) e;
                    assertEquals(42102, h2Exception.getErrorCode()); // 42102 is "table or view not found" error code
                                                                     // for H2.
                } else {
                    fail("Expected statement to succeed (table exist).");
                }
            }
            File ftDirectory = new File(rdbmsDataSource.getIndexDirectory() + '/' + STORAGE_NAME + "/org.talend.mdm.storage.hibernate.Supplier");
            if (!expectSupplierTable) {
                assertTrue(ftDirectory.list() == null || ftDirectory.list().length == 0);
            } else {
                assertTrue(ftDirectory.exists());
            }
        } finally {
            statement.close();
            connection.close();
        }
    }

    public void test2() throws Exception {
        // Test preparation
        DataSourceDefinition dataSource = ServerContext.INSTANCE.get().getDefinition("H2-DS3", STORAGE_NAME);
        Storage storage = new HibernateStorage("Test", StorageType.MASTER);
        storage.init(dataSource);
        MetadataRepository repository = new MetadataRepository();
        repository.load(StorageAdaptTest.class.getResourceAsStream("schema2_1.xsd"));
        storage.prepare(repository, true);
        // Ensure name column exists
        performAssert2(dataSource, true);
        // Actual test
        MetadataRepository newRepository = new MetadataRepository();
        newRepository.load(StorageAdaptTest.class.getResourceAsStream("schema2_2.xsd"));
        storage.adapt(newRepository, true);
        // Test expected schema update
        performAssert2(dataSource, false);
        storage.close(true);
    }

    private void performAssert2(DataSourceDefinition dataSource, boolean roadElement)
            throws SQLException {
        DataSource master = dataSource.getMaster();
        assertTrue(master instanceof RDBMSDataSource);
        RDBMSDataSource rdbmsDataSource = (RDBMSDataSource) master;
        assertEquals(RDBMSDataSource.DataSourceDialect.H2, rdbmsDataSource.getDialectName());
        Connection connection = DriverManager.getConnection(rdbmsDataSource.getConnectionURL());
        Statement statement = connection.createStatement();
        try {
            // Chinese address table asserts
            ResultSet resultSet = statement.executeQuery("SELECT * FROM X_ChineseAddress");
            ResultSetMetaData metaData = resultSet.getMetaData();
            boolean hasRoad = false;
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                hasRoad |= "x_road".equalsIgnoreCase(metaData.getColumnName(i));
            }
            assertSame(roadElement, hasRoad);
        } finally {
            statement.close();
            connection.close();
        }
    }

}
