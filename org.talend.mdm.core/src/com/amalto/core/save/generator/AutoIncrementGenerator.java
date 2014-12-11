package com.amalto.core.save.generator;

import com.amalto.core.server.Server;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.StorageType;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.core.MDMConfiguration;

import java.util.Properties;

public class AutoIncrementGenerator {

    private static final Logger                         LOGGER                         = Logger.getLogger(AutoIncrementGenerator.class);

    private static final boolean                        ENABLE_SAFE_CONCURRENT_INCREMENT;

    private static final InMemoryAutoIncrementGenerator inMemoryAutoIncrementGenerator = new InMemoryAutoIncrementGenerator();

    private static final StorageAutoIncrementGenerator  storageAutoIncrementGenerator;

    static {
        // Initialize Storage auto increment generator (only if system SQL is available).
        StorageAdmin admin = ServerContext.INSTANCE.get().getStorageAdmin();
        if (admin.exist(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null)) {
            storageAutoIncrementGenerator = new StorageAutoIncrementGenerator();
        } else {
            storageAutoIncrementGenerator = null;
        }
        // Enable concurrent auto increment generator
        Properties properties = MDMConfiguration.getConfiguration();
        boolean enableConcurrentIncrement = Boolean.parseBoolean(properties.getProperty(Server.SYSTEM_CLUSTER, Boolean.FALSE.toString()));
        if (enableConcurrentIncrement) {
            if (MDMConfiguration.isSqlDataBase()) {
                if (storageAutoIncrementGenerator == null) {
                    LOGGER.error("Unable to enable concurrent access support for auto increment generator (system database must use SQL).");
                    enableConcurrentIncrement = false;
                } else {
                    LOGGER.info("Enable concurrent access support for auto increment generator.");
                }
            } else {
                LOGGER.error("Unable to enable concurrent access support for auto increment generator (must use SQL databases).");
                enableConcurrentIncrement = false;
            }
        }
        ENABLE_SAFE_CONCURRENT_INCREMENT = enableConcurrentIncrement;
    } 

    public static AutoIdGenerator get() {
        if (ENABLE_SAFE_CONCURRENT_INCREMENT) {
            return storageAutoIncrementGenerator;
        }
        return inMemoryAutoIncrementGenerator;
    }

}
