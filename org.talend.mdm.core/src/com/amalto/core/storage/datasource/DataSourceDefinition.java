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

package com.amalto.core.storage.datasource;

public class DataSourceDefinition {

    private final String name;

    private final DataSource master;

    private DataSource staging;

    public DataSourceDefinition(String name, DataSource master, DataSource staging) {
        this.name = name;
        this.master = master;
        this.staging = staging;
    }

    public String getName() {
        return name;
    }

    public boolean hasStaging() {
        return staging != null;
    }

    public DataSource getMaster() {
        return master;
    }

    public DataSource getStaging() {
        return staging;
    }

}