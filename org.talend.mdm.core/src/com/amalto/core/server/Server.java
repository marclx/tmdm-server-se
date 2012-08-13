// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package com.amalto.core.server;

import com.amalto.core.storage.datasource.DataSource;

import javax.management.MBeanServer;

public interface Server {

    DataSource getDataSource(String dataSourceName, String container);
    
    StorageAdmin getStorageAdmin();

    MetadataRepositoryAdmin getMetadataRepositoryAdmin();

    MBeanServer getMBeanServer();

    void close();

    void init();
}