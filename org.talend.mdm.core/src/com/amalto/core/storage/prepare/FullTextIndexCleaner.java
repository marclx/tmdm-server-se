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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class FullTextIndexCleaner implements StorageCleaner {

    private static final Logger LOGGER = Logger.getLogger(FullTextIndexCleaner.class);

    public void clean(Storage storage) {
        DataSource storageDataSource = storage.getDataSource();
        if (!(storageDataSource instanceof RDBMSDataSource)) {
            throw new IllegalArgumentException("Storage to clean does not seem to be a RDBMS storage.");
        }

        RDBMSDataSource dataSource = (RDBMSDataSource) storageDataSource;
        if (dataSource.supportFullText()) {
            String dataSourceIndexDirectory = dataSource.getIndexDirectory();
            File indexDirectory = new File(dataSourceIndexDirectory);
            if (indexDirectory.exists()) {
                try {
                    FileUtils.deleteDirectory(indexDirectory);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not successfully delete '" + indexDirectory.getAbsolutePath() + "'", e);
                }
            } else {
                LOGGER.warn("Directory '" + dataSourceIndexDirectory + "' does not exist. No need to clean full text indexes.");
            }
        }
    }
}