/*
 * Copyright (C) 2006-2012 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.storage.prepare;

import com.amalto.core.storage.Storage;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.prepare.StorageCleaner;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.Properties;

class H2StorageCleaner implements StorageCleaner {

    private final StorageCleaner next;

    public H2StorageCleaner(StorageCleaner next) {
        this.next = next;
    }

    public void clean(Storage storage) {
        try {
            DataSource storageDataSource = storage.getDataSource();
            if (!(storageDataSource instanceof RDBMSDataSource)) {
                throw new IllegalArgumentException("Storage to clean does not seem to be a RDBMS storage.");
            }

            RDBMSDataSource dataSource = (RDBMSDataSource) storageDataSource;
            Driver driver = (Driver) Class.forName(dataSource.getDriverClassName()).newInstance();
            Connection connection = driver.connect(dataSource.getConnectionURL(), new Properties());
            try {
                Statement statement = connection.createStatement();
                try {
                    statement.execute("drop all objects delete files;"); //$NON-NLS-1$
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }

            next.clean(storage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}