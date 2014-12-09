package com.amalto.core.save.generator;

public class AutoIncrementGenerator {

    private static final boolean                        ENABLE_SAFE_CONCURRENT_INCREMENT = false;

    private static final InMemoryAutoIncrementGenerator inMemoryAutoIncrementGenerator   = new InMemoryAutoIncrementGenerator();

    private static final StorageAutoIncrementGenerator  storageAutoIncrementGenerator    = new StorageAutoIncrementGenerator();

    public static AutoIdGenerator get() {
        if (ENABLE_SAFE_CONCURRENT_INCREMENT) {
            return storageAutoIncrementGenerator;
        }
        return inMemoryAutoIncrementGenerator;
    }

}
