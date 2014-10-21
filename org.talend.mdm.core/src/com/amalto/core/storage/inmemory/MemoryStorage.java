package com.amalto.core.storage.inmemory;

import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Expression;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.datasource.RDBMSDataSourceBuilder;
import com.amalto.core.storage.hibernate.HibernateStorage;

public class MemoryStorage extends HibernateStorage {

    protected static final Logger LOGGER = Logger.getLogger(MemoryStorage.class);

    public MemoryStorage(String storageName, StorageType type) {
        super(storageName, type);
    }

    @Override
    public void init(DataSourceDefinition dataSourceDefinition) {
        RDBMSDataSourceBuilder builder = RDBMSDataSourceBuilder.newBuilder();
        builder.driverClassName("org.h2.Driver"); //$NON-NLS-1$
        builder.dialect(RDBMSDataSource.DataSourceDialect.H2).connectionURL("jdbc:h2:mem:" + getName() + ";DB_CLOSE_DELAY=-1"); //$NON-NLS-1$ //$NON-NLS-2$
        // Don't initialize a huge connection pool (not needed).
        builder.connectionPoolMinSize(1).connectionPoolMaxSize(1);
        builder.generateConstraints(false);
        dataSource = builder.build();
        internalInit();
    }

    @Override
    public synchronized void prepare(MetadataRepository repository, boolean dropExistingData) {
        if (dropExistingData) {
            LOGGER.debug("No need to drop existing data for a in-memory storage.");
        }
        super.prepare(repository, false);
    }

    @Override
    public synchronized void prepare(MetadataRepository repository, Set<Expression> optimizedExpressions, boolean force,
            boolean dropExistingData) {
        if (dropExistingData) {
            LOGGER.debug("No need to drop existing data for a in-memory storage.");
        }
        super.prepare(repository, optimizedExpressions, force, false);
    }
}
